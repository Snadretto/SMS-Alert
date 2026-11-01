/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

data class Rule(val sender: String = "", val text: String = "") {
    val valid: Boolean get() = sender.isNotBlank() || text.isNotBlank()

    fun matches(incomingSender: String?, incomingText: String): Boolean =
        valid &&
            (sender.isBlank() || sender.trim() == incomingSender?.trim()) &&
            (text.isBlank() || incomingText.contains(text.trim(), ignoreCase = true))
}

data class PendingAlarm(
    val id: String,
    val receivedElapsedMs: Long,
    val simulated: Boolean,
    val matches: Int = 1,
)

/** No sender or message body is kept in alarm state. */
data class AlarmState(val enabled: Boolean = false, val pending: PendingAlarm? = null) {
    fun receive(id: String, elapsedMs: Long, simulated: Boolean): AlarmState {
        if (!enabled) return this
        return copy(
            pending = pending?.copy(
                matches = pending.matches + 1,
                simulated = pending.simulated && simulated,
            )
                ?: PendingAlarm(id, elapsedMs, simulated),
        )
    }

    fun acknowledge(expectedId: String): AlarmState =
        if (pending?.id == expectedId) copy(pending = null) else this

    fun pause(): AlarmState = AlarmState(enabled = false)
}
