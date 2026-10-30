/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.nio.ByteBuffer
import java.text.Normalizer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AppFailure(val code: String, override val message: String) : IllegalArgumentException(message)

data class AlarmRule(
    val id: String,
    val name: String,
    val sender: String = "",
    val text: String = "",
    val logic: String = "AND",
    val enabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val simSlot: Int? = null,
) {
    fun validate() {
        if (name.isBlank() || name.length > 60) throw AppFailure("invalid_rule", "Bitte einen Regelnamen mit höchstens 60 Zeichen eingeben.")
        if (sender.isBlank() && text.isBlank()) throw AppFailure("invalid_rule", "Mindestens Absender oder Textbestandteil angeben.")
        if (sender.length > 80 || text.length > 1024) throw AppFailure("invalid_rule", "Absender oder Suchtext ist zu lang.")
        if (logic !in setOf("AND", "OR")) throw AppFailure("invalid_rule", "Unbekannte Verknüpfung.")
        if (simSlot != null && simSlot !in 0..1) throw AppFailure("invalid_rule", "SIM muss 1, 2 oder alle sein.")
    }

    fun matches(from: String?, body: String, incomingSlot: Int?, country: String): Boolean {
        if (!enabled || (simSlot != null && simSlot != incomingSlot)) return false
        if (sender.isBlank() && text.isBlank()) return false
        val tests = mutableListOf<Boolean>()
        if (sender.isNotBlank()) tests += from != null && SenderIdentity.canonical(sender, country) == SenderIdentity.canonical(from, country)
        if (text.isNotBlank()) {
            val normalizedBody = Normalizer.normalize(body, Normalizer.Form.NFC)
            val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFC)
            tests += if (caseSensitive) normalizedBody.contains(normalizedText)
                else normalizedBody.lowercase(Locale.ROOT).contains(normalizedText.lowercase(Locale.ROOT))
        }
        return when (logic) { "AND" -> tests.all { it }; "OR" -> tests.any { it }; else -> false }
    }

    fun toMap(): Map<String, Any?> = linkedMapOf(
        "id" to id, "name" to name, "sender" to sender, "text" to text, "logic" to logic,
        "enabled" to enabled, "caseSensitive" to caseSensitive, "simSlot" to simSlot,
    )
}

object SenderIdentity {
    private val phones = PhoneNumberUtil.getInstance()

    fun canonical(raw: String, country: String): String {
        val value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
        if (!value.matches(Regex("[+0-9() ./-]+"))) return "text:$value"
        var compact = value.replace(Regex("[() ./-]"), "")
        if (compact.startsWith("00")) compact = "+" + compact.drop(2)
        if (compact.removePrefix("+").length <= 6) return "short:$compact"
        return try {
            val phone = phones.parse(compact, country)
            if (phones.isPossibleNumber(phone)) "phone:" + phones.format(phone, PhoneNumberUtil.PhoneNumberFormat.E164)
            else "number:$compact"
        } catch (_: Exception) { "number:$compact" }
    }

    fun validCountry(country: String): Boolean = country in phones.supportedRegions

    fun masked(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unbekannter Absender"
        val cleaned = raw.trim()
        return if (cleaned.length <= 4) "••••" else "••••" + cleaned.takeLast(4)
    }
}

data class AlarmSettings(
    val enabled: Boolean = false,
    val country: String = "CH",
    val volume: Int = 80,
    val vibration: Boolean = true,
    val tone: String = "beacon",
    val raiseSystemVolume: Boolean = false,
    val retentionDays: Int = 30,
    val historyLimit: Int = 200,
) {
    fun validate() {
        if (!SenderIdentity.validCountry(country)) throw AppFailure("invalid_settings", "Unbekannter Ländercode.")
        if (volume !in 10..100) throw AppFailure("invalid_settings", "Alarmtonlautstärke muss zwischen 10 und 100 Prozent liegen.")
        if (tone !in setOf("beacon", "pulse")) throw AppFailure("invalid_settings", "Unbekannter Alarmton.")
        if (retentionDays !in setOf(7, 30, 90) || historyLimit !in 50..1000) throw AppFailure("invalid_settings", "Ungültige Aufbewahrungsgrenze.")
    }

    val audioKey: String get() = "$volume:$vibration:$tone:$raiseSystemVolume"

    fun toMap(): Map<String, Any?> = linkedMapOf(
        "enabled" to enabled, "country" to country, "volume" to volume, "vibration" to vibration,
        "tone" to tone, "raiseSystemVolume" to raiseSystemVolume,
        "retentionDays" to retentionDays, "historyLimit" to historyLimit,
    )
}

data class SmsInput(
    val sender: String?, val body: String, val sentAt: Long, val receivedAt: Long,
    val elapsedMs: Long, val bootCount: Int, val simSlot: Int?, val subscription: Int,
    val pdus: List<ByteArray>,
)

object TransportIdentity {
    fun fingerprint(secret: ByteArray, sms: SmsInput): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        fun add(data: ByteArray) { mac.update(ByteBuffer.allocate(4).putInt(data.size).array()); mac.update(data) }
        listOf(sms.sender.orEmpty(), sms.body, sms.sentAt.toString(), sms.subscription.toString(), sms.simSlot.toString()).forEach { add(it.toByteArray(Charsets.UTF_8)) }
        sms.pdus.forEach { add(it) }
        return mac.doFinal().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
