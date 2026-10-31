/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Private immutable copies: no storage permission or future provider grant is needed. */
object ToneAssets {
    const val MAX_BYTES = 20L * 1024 * 1024
    val io = Executors.newSingleThreadExecutor()
    @Volatile private var lastPicked: String? = null

    private fun folder(context: Context) = File(context.filesDir, "alarm-tones").apply { mkdirs() }
    fun file(context: Context, id: String): File {
        if (!id.matches(Regex("[a-f0-9]{64}"))) throw AppFailure("invalid_tone", "Ungültiger Smartphone-Klingelton.")
        return File(folder(context), "$id.audio")
    }

    fun requireAvailable(context: Context, tone: DeviceTone) {
        tone.validate()
        val file = file(context, tone.id)
        if (!file.isFile || file.length() == 0L) throw AppFailure("tone_unavailable", "Der gespeicherte Klingelton fehlt. Bitte erneut auswählen.")
    }

    fun import(context: Context, uri: Uri): DeviceTone {
        if (uri.scheme !in setOf("content", "android.resource") || uri.toString().length > 2048) {
            throw AppFailure("invalid_tone", "Bitte einen lokal auf dem Smartphone vorhandenen Klingelton auswählen.")
        }
        val temporary = File.createTempFile("tone-import-", ".tmp", context.cacheDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                if (descriptor.declaredLength > MAX_BYTES) throw AppFailure("tone_too_large", "Der Klingelton ist zu groß. Höchstens 20 MB sind möglich.")
                descriptor.createInputStream().use { source ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_BYTES) throw AppFailure("tone_too_large", "Der Klingelton ist zu groß. Höchstens 20 MB sind möglich.")
                            digest.update(buffer, 0, count); output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
            } ?: throw AppFailure("tone_unavailable", "Android konnte den ausgewählten Klingelton nicht öffnen.")
            val probe = MediaPlayer()
            try {
                probe.setDataSource(temporary.absolutePath); probe.prepare()
                if (probe.duration <= 0) throw AppFailure("invalid_tone", "Der ausgewählte Ton enthält keine abspielbare Audiodatei.")
            } finally { probe.release() }
            val id = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            val destination = file(context, id)
            if (!temporary.renameTo(destination)) throw AppFailure("tone_unavailable", "Der Klingelton konnte nicht lokal gespeichert werden.")
            val selected = DeviceTone(id, title(context, uri), uri.toString()).also { it.validate() }
            lastPicked = id
            pruneNow(context)
            return selected
        } catch (failure: AppFailure) { throw failure }
        catch (_: Exception) { throw AppFailure("tone_unavailable", "Android hat den Klingelton nicht zum Lesen freigegeben oder das Audioformat ist nicht lesbar. Bitte einen anderen vorhandenen Ton auswählen.") }
        finally { temporary.delete() }
    }

    private fun title(context: Context, uri: Uri): String {
        for (column in listOf(MediaStore.Audio.Media.TITLE, OpenableColumns.DISPLAY_NAME)) {
            try {
                val value = context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                if (!value.isNullOrBlank()) return value.replace(Regex("[\\p{Cntrl}]"), " ").trim().take(120)
            } catch (_: RuntimeException) { /* OEM providers need not expose both columns. */ }
        }
        return "Ausgewählter Smartphone-Ton"
    }

    fun prune(context: Context) { io.execute { try { pruneNow(context) } catch (_: RuntimeException) { } } }
    private fun pruneNow(context: Context) {
        val active = AppRuntime.store(context).settings().deviceTone?.id
        // Retain the saved selection and at most one unsaved picker result.
        val keep = setOfNotNull(active, lastPicked)
        folder(context).listFiles()?.filter { it.name.matches(Regex("[a-f0-9]{64}\\.audio")) && it.name.removeSuffix(".audio") !in keep }?.forEach { it.delete() }
    }
}
