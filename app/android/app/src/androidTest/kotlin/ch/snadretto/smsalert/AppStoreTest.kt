/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AppStoreTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="instrumentation-only.db"
    private lateinit var store:AppStore
    @Before fun setup(){context.deleteDatabase(name);store=AppStore(context,name)}
    @After fun cleanup(){store.close();context.deleteDatabase(name)}
    private fun enable(){store.saveRule(AlarmRule("r","Pikett",text="Alarm"));store.setEnabled(true)}
    private fun sms(index:Int=0)=SmsInput("0791234567","Alarm PRIVATE_BODY_$index",100L+index,System.currentTimeMillis()+index,1,1,0,1,listOf(byteArrayOf(index.toByte())))

    @Test fun activationRequiresRuleAndSurvivesReopening(){
        assertFalse(store.settings().enabled)
        assertThrows(AppFailure::class.java){store.setEnabled(true)}
        enable();store.close();store=AppStore(context,name)
        assertTrue(store.settings().enabled);assertEquals("Pikett",store.rules().single().name)
    }
    @Test fun replayDedupAndQueueNeverDropAnotherEvent(){
        enable();val input=sms();val first=store.receive(input)!!
        assertNull(store.receive(input.copy(receivedAt=input.receivedAt+1)))
        val second=store.receive(sms(1))!!
        assertEquals(2,store.pendingCount());assertTrue(store.acknowledge(first))
        assertFalse(store.acknowledge(first));assertEquals(second,store.nextPending()!!["id"])
        store.deleteRule("r");assertEquals(1,store.pendingCount())
    }
    @Test fun pauseResolvesAllAndIncomingHasNoPersistentEffect(){
        enable();store.receive(sms());store.addTest(System.currentTimeMillis(),1,1)
        store.setEnabled(false);assertEquals(0,store.pendingCount());assertEquals(2,store.history().size)
        assertTrue(store.history().all{it["status"]=="PAUSED"})
        val before=store.meta("lastOutcome");assertNull(store.receive(sms(2)))
        assertEquals(before,store.meta("lastOutcome"));assertEquals(2,store.history().size)
        store.close();store=AppStore(context,name);assertFalse(store.settings().enabled)
        store.setEnabled(true);assertEquals(0,store.pendingCount())
    }
    @Test fun multipleMatchingRulesProduceOneEventWithoutRawMessage(){
        enable();store.saveRule(AlarmRule("r2","Zweite",sender="+41791234567"))
        store.receive(sms());val event=store.nextPending()!!
        assertEquals(2,(event["ruleIds"] as List<*>).size)
        val payload=event.toString();assertFalse(payload.contains("PRIVATE_BODY"));assertFalse(payload.contains("0791234567"))
        assertFalse(payload.contains("pdus"));assertEquals("SMS",event["source"])
    }
    @Test fun historyLimitsAndClearNeverDeletePending(){
        enable();store.updateSettings(mapOf("historyLimit" to 50))
        val pending=store.receive(sms())!!
        repeat(55){store.acknowledge(store.receive(sms(it+1))!!)}
        assertEquals(50,store.history().size);assertEquals(pending,store.nextPending()!!["id"])
        store.clearHistory();assertTrue(store.history().isEmpty());assertEquals(1,store.pendingCount())
        store.acknowledgeAll();assertEquals(0,store.pendingCount())
    }
    @Test fun migrationPreservesRulesPauseAndPending(){
        enable();val id=store.receive(sms())!!;store.close()
        context.openOrCreateDatabase(name,0,null).use{db->db.execSQL("DROP TABLE seen");db.version=1}
        store=AppStore(context,name)
        assertTrue(store.settings().enabled);assertEquals("r",store.rules().single().id)
        assertEquals(id,store.nextPending()!!["id"]);assertNotNull(store.receive(sms(3)))
        assertEquals(2,store.readableDatabase.version)
    }
    @Test fun invalidSettingsRollBackAtomically(){
        enable();assertThrows(AppFailure::class.java){store.updateSettings(mapOf("country" to "ZZ","volume" to 10))}
        assertEquals("CH",store.settings().country);assertEquals(80,store.settings().volume)
        assertThrows(AppFailure::class.java){store.updateSettings(mapOf("volume" to 10.5))}
    }
    @Test fun eventDiagnosticsSurviveAckAndCannotReviveResolvedEvent(){
        enable();val id=store.receive(sms())!!
        store.recordOutcome(id,"playback_started",25)
        store.acknowledge(id);store.recordOutcome(id,"audio_failed")
        val event=store.history().single()
        assertEquals("playback_started",event["lastOutcome"])
        assertEquals(25,(event["latencyMs"] as Number).toInt())
        assertNotNull(event["startedAt"]);assertEquals(0,store.pendingCount())
    }
    @Test fun pausedMigrationAndRetentionPreservePending(){
        enable();val open=store.receive(sms())!!
        val old=store.receive(sms(1))!!;store.acknowledge(old)
        store.writableDatabase.execSQL("UPDATE events SET resolved_at=1 WHERE id=?", arrayOf(old))
        store.maintain();assertTrue(store.history().isEmpty());assertEquals(open,store.nextPending()!!["id"])
        store.setEnabled(false);store.close()
        context.openOrCreateDatabase(name,0,null).use{db->db.execSQL("DROP TABLE seen");db.version=1}
        store=AppStore(context,name);assertFalse(store.settings().enabled);assertEquals(1,store.history().size)
    }
}
