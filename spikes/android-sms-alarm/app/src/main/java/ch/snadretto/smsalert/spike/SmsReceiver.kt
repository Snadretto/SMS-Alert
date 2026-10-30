/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val store = SpikeStore(context)
        if (!store.state().enabled) return // No parsing or diagnostics for incoming SMS while paused.
        val received = SystemClock.elapsedRealtime()
        try {
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (parts.isNullOrEmpty() || parts.any { it.messageBody == null }) {
                store.record("invalid_sms")
                return
            }
            val sender = parts.first().originatingAddress
            if (parts.any { it.originatingAddress != sender }) {
                store.record("inconsistent_sms_parts")
                return
            }
            val body = parts.joinToString("") { it.messageBody }
            if (store.rule().matches(sender, body)) {
                AlarmLauncher.start(context, received, simulated = false)
            }
        } catch (error: RuntimeException) {
            store.record("sms_error:${error.javaClass.simpleName}")
        }
    }
}

object AlarmLauncher {
    fun start(context: Context, receivedElapsedMs: Long, simulated: Boolean) {
        val store = SpikeStore(context)
        if (!store.state().enabled) return
        val alarm = store.receive(simulated, receivedElapsedMs) ?: return
        try {
            AlarmNotifications.ensureChannel(context)
            if (!AlarmNotifications.notificationsAllowed(context)) {
                if (store.fail(alarm.id, "notifications_blocked")) {
                    context.stopService(Intent(context, AlarmService::class.java))
                }
                return
            }
            context.startForegroundService(Intent(context, AlarmService::class.java))
        } catch (error: RuntimeException) {
            if (store.fail(alarm.id, "foreground_start_failed:${error.javaClass.simpleName}")) {
                context.stopService(Intent(context, AlarmService::class.java))
            }
        }
    }
}

class AcknowledgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmNotifications.ACK) return
        val id = intent.getStringExtra(AlarmNotifications.ALARM_ID) ?: return
        if (SpikeStore(context).acknowledge(id)) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
