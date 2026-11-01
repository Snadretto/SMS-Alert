/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.content.Context
import android.os.SystemClock
import java.util.UUID

class SpikeStore(context: Context) {
    private val preferences = context.getSharedPreferences("spike", Context.MODE_PRIVATE)

    companion object {
        // All Android components run in one process. Serialize state changes in that process.
        private val lock = Any()
    }

    fun rule(): Rule = synchronized(lock) {
        Rule(preferences.getString("sender", "")!!, preferences.getString("text", "")!!)
    }

    fun saveRule(rule: Rule) = synchronized(lock) {
        require(rule.valid)
        check(preferences.edit().putString("sender", rule.sender.trim())
            .putString("text", rule.text.trim()).commit())
    }

    fun state(): AlarmState = synchronized(lock) { readState() }

    fun enable() = synchronized(lock) {
        require(rule().valid)
        writeState(readState().copy(enabled = true))
    }

    fun pause() = synchronized(lock) {
        writeState(readState().pause())
        record("paused")
    }

    fun receive(simulated: Boolean, receivedElapsedMs: Long): PendingAlarm? = synchronized(lock) {
        val next = readState().receive(UUID.randomUUID().toString(), receivedElapsedMs, simulated)
        writeState(next)
        if (next.pending != null) record("alarm_requested")
        next.pending
    }

    fun acknowledge(expectedId: String): Boolean = synchronized(lock) {
        val current = readState()
        val next = current.acknowledge(expectedId)
        if (next == current) return false
        writeState(next)
        check(preferences.edit().putLong("last_ack_wall_ms", System.currentTimeMillis()).commit())
        record("acknowledged")
        true
    }

    fun fail(expectedId: String, code: String) = synchronized(lock) {
        val current = readState()
        if (current.pending?.id == expectedId) {
            writeState(current.copy(pending = null))
            record(code)
            true
        } else {
            false
        }
    }

    fun record(code: String, latencyMs: Long? = null) = synchronized(lock) {
        val edit = preferences.edit().putString("result", code)
        if (latencyMs != null) edit.putLong("latency_ms", latencyMs)
        check(edit.commit())
    }

    fun result(): String = preferences.getString("result", "not_tested")!!
    fun latencyMs(): Long = preferences.getLong("latency_ms", -1)
    fun lastAcknowledgedMs(): Long = preferences.getLong("last_ack_wall_ms", -1)

    private fun readState(): AlarmState {
        val id = preferences.getString("alarm_id", null)
        return AlarmState(
            enabled = preferences.getBoolean("enabled", false),
            pending = id?.let {
                PendingAlarm(
                    it, preferences.getLong("received_elapsed_ms", SystemClock.elapsedRealtime()),
                    preferences.getBoolean("simulated", false),
                    preferences.getInt("matches", 1),
                )
            },
        )
    }

    private fun writeState(state: AlarmState) {
        val edit = preferences.edit().putBoolean("enabled", state.enabled)
        val pending = state.pending
        if (pending == null) {
            edit.remove("alarm_id").remove("received_elapsed_ms").remove("simulated").remove("matches")
        } else {
            edit.putString("alarm_id", pending.id).putLong("received_elapsed_ms", pending.receivedElapsedMs)
                .putBoolean("simulated", pending.simulated).putInt("matches", pending.matches)
        }
        check(edit.commit())
    }
}
