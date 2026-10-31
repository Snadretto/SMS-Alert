/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    private fun rule(sender: String = "", text: String = "", logic: String = "AND") = AlarmRule("r", "Pikett", sender, text, logic)
    @Test fun nationalAndInternationalSenderAreEquivalent() {
        val r = rule(sender = "079 123 45 67")
        assertTrue(r.matches("+41791234567", "", null, "CH"))
        assertTrue(r.matches("0041 79 123 45 67", "", null, "CH"))
        assertFalse(r.matches("+41791234568", "", null, "CH"))
    }
    @Test fun countryContextChangesIdentity() {
        assertNotEquals(SenderIdentity.canonical("0791234567", "CH"), SenderIdentity.canonical("0791234567", "GB"))
    }
    @Test fun shortAndAlphanumericSendersRemainExact() {
        assertTrue(rule(sender="1234").matches("1234", "", null, "CH"))
        assertFalse(rule(sender="1234").matches("+411234", "", null, "CH"))
        assertFalse(rule(sender="Pikett").matches("pikett", "", null, "CH"))
        assertTrue(rule(sender="Pikett").matches("Pikett", "", null, "CH"))
    }
    @Test fun logicAndMissingCriteria() {
        assertFalse(rule("1234", "Alarm").matches("5678", "Alarm", null, "CH"))
        assertTrue(rule("1234", "Alarm", "OR").matches("5678", "Alarm", null, "CH"))
        assertTrue(rule(text="Alarm").matches(null, "Alarm", null, "CH"))
        assertFalse(rule().matches("1234", "Alarm", null, "CH"))
        assertFalse(rule(text="Alarm").copy(enabled=false).matches(null, "Alarm", null, "CH"))
    }
    @Test fun literalUnicodeTextAndCaseSensitivity() {
        val r=rule(text="Système [A].*")
        assertTrue(r.matches(null, "SYSTE\u0300ME [A].* ist gestört", null, "CH"))
        assertFalse(r.matches(null, "Système AX", null, "CH"))
        assertFalse(r.copy(caseSensitive=true).matches(null, "SYSTÈME [A].*", null, "CH"))
    }
    @Test fun unknownSimNeverMatchesSpecificSim() {
        val r=rule(text="Alarm").copy(simSlot=1)
        assertFalse(r.matches(null,"Alarm",null,"CH"))
        assertFalse(r.matches(null,"Alarm",0,"CH"))
        assertTrue(r.matches(null,"Alarm",1,"CH"))
    }
    @Test fun invalidRulesAndSettingsAreRejected() {
        for(r in listOf(rule(),rule(text="x").copy(name=""),rule(text="x").copy(simSlot=2),rule(text="x",logic="X"))) {
            assertThrows(AppFailure::class.java) { r.validate() }
        }
        assertThrows(AppFailure::class.java) { AlarmSettings(volume=0).validate() }
        assertThrows(AppFailure::class.java) { AlarmSettings(country="ZZ").validate() }
    }
    @Test fun transportReplayDiffersFromNewSmsAndSecretIsPrivate() {
        val sms=SmsInput("1234","Alarm",100,101,12,1,null,1,listOf(byteArrayOf(1,2)))
        val key=ByteArray(32){1}
        val fp=TransportIdentity.fingerprint(key,sms)
        assertEquals(fp,TransportIdentity.fingerprint(key,sms.copy(receivedAt=999,elapsedMs=900)))
        assertNotEquals(fp,TransportIdentity.fingerprint(key,sms.copy(sentAt=102)))
        assertNotEquals(fp,TransportIdentity.fingerprint(key,sms.copy(pdus=listOf(byteArrayOf(1,3)))))
        assertNotEquals(fp,TransportIdentity.fingerprint(key,sms.copy(subscription=2)))
        assertNotEquals(fp,TransportIdentity.fingerprint(ByteArray(32){2},sms))
        assertEquals(64,fp.length)
    }
    @Test fun deviceToneRequiresLocalSelectionAndSafeAssetIdentifier() {
        val selected = DeviceTone("a".repeat(64), "Telefon-Ton", "content://media/internal/audio/media/5")
        AlarmSettings(tone="device", deviceTone=selected).validate()
        assertThrows(AppFailure::class.java) { AlarmSettings(tone="device").validate() }
        assertThrows(AppFailure::class.java) { selected.copy(id="../../secret").validate() }
        assertThrows(AppFailure::class.java) { selected.copy(uri="https://example.org/sound").validate() }
        assertThrows(AppFailure::class.java) { selected.copy(title="").validate() }
        assertNotEquals(AlarmSettings(tone="device", deviceTone=selected).audioKey, AlarmSettings(tone="device", deviceTone=selected.copy(id="b".repeat(64))).audioKey)
    }
}
