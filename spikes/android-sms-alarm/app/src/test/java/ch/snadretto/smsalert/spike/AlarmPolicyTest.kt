/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import org.junit.Assert.*
import org.junit.Test

class AlarmPolicyTest {
    @Test fun emptyRuleNeverMatches() {
        assertFalse(Rule("  ", " ").matches("Pikett", "Alarm"))
    }

    @Test fun senderOnlyRequiresExactIdentity() {
        val rule = Rule(sender = "Pikett")
        assertTrue(rule.matches("Pikett", "anything"))
        assertFalse(rule.matches("OtherPikett", "anything"))
        assertFalse(rule.matches(null, "anything"))
    }

    @Test fun textOnlyMatchesCaseInsensitiveUnicodeLiteral() {
        assertTrue(Rule(text = " Störung [1] ").matches(null, "ALARM: STÖRUNG [1]"))
        assertFalse(Rule(text = "[1]").matches("Pikett", "1"))
    }

    @Test fun bothCriteriaRequireBothMatches() {
        val rule = Rule("+41791234567", "alarm")
        assertTrue(rule.matches("+41791234567", "ALARM"))
        assertFalse(rule.matches("0791234567", "ALARM"))
        assertFalse(rule.matches("+41791234567", "test"))
    }

    @Test fun disabledStateRejectsRealAndSimulatedAlarms() {
        val paused = AlarmState()
        assertEquals(paused, paused.receive("first", 1, false))
        assertEquals(paused, paused.receive("test", 2, true))
    }

    @Test fun pauseClearsAlarmAndPreventsNewEvents() {
        val paused = AlarmState(true).receive("a", 1, false).pause()
        assertFalse(paused.enabled)
        assertNull(paused.pending)
        assertEquals(paused, paused.receive("b", 2, false))
    }

    @Test fun staleAcknowledgementCannotStopNewAlarm() {
        val current = AlarmState(true).receive("old", 1, false)
            .acknowledge("old").receive("new", 2, false)
        assertEquals(current, current.acknowledge("old"))
        assertNull(current.acknowledge("new").pending)
    }

    @Test fun repeatedNewMessagesAfterAcknowledgementCanAlarmAgain() {
        val first = AlarmState(true).receive("a", 1, false)
        val cleared = first.acknowledge("a")
        assertNull(cleared.pending)
        assertEquals(cleared, cleared.acknowledge("a"))
        assertEquals("b", cleared.receive("b", 2, false).pending?.id)
    }

    @Test fun concurrentMatchesDoNotCreateCompetingAudioSessions() {
        val state = AlarmState(true).receive("a", 1, false).receive("b", 2, false)
        assertEquals("a", state.pending?.id)
        assertEquals(2, state.pending?.matches)
        assertEquals(1L, state.pending?.receivedElapsedMs)
    }

    @Test fun realSmsCannotStayLabelledAsOnlyATest() {
        val state = AlarmState(true).receive("test", 1, true).receive("sms", 2, false)
        assertFalse(state.pending!!.simulated)
    }
}
