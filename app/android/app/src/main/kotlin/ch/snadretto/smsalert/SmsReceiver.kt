/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Telephony
import android.telephony.SubscriptionManager

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val receivedAt = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val pending = goAsync()
        AppRuntime.worker.execute {
            val store = AppRuntime.store(context)
            try {
                if (!store.settings().enabled) return@execute
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (parts.isNullOrEmpty() || parts.size > 128 || parts.any { it.messageBody == null }) return@execute
                val sender = parts.first().originatingAddress
                if (parts.any { it.originatingAddress != sender }) return@execute
                val body = parts.joinToString("") { it.messageBody }
                val subscription = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, -1)
                @Suppress("DEPRECATION")
                val slot = try { SubscriptionManager.getSlotIndex(subscription).takeIf { it >= 0 } }
                    catch (_: RuntimeException) { null }
                val reportedSlot = slot ?: intent.getIntExtra("slot", -1).takeIf { it >= 0 }
                @Suppress("DEPRECATION")
                val pdus = (intent.extras?.get("pdus") as? Array<*>)?.mapNotNull { it as? ByteArray } ?: emptyList()
                val id = store.receive(SmsInput(sender, body, parts.first().timestampMillis, receivedAt, elapsed, AppRuntime.bootCount(context), reportedSlot, subscription, pdus))
                if (id != null) AppRuntime.reconcile(context)
            } catch (_: RuntimeException) {
                try { store.setMeta("lastOutcome", "sms_processing_failed") } catch (_: RuntimeException) { /* No message data goes to system logs. */ }
            } finally { pending.finish() }
        }
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(AlarmNotifications.ACTION_ACK, AlarmNotifications.ACTION_PAUSE)) return
        val pending = goAsync()
        AppRuntime.worker.execute {
            try {
                val store = AppRuntime.store(context)
                when (intent.action) {
                    AlarmNotifications.ACTION_ACK -> {
                        val id = intent.getStringExtra("eventId") ?: return@execute
                        val wasTest = store.pending().any { it["id"] == id && it["source"] == "TEST" }
                        if (store.acknowledge(id) && wasTest) store.setMeta("lastTestAckAt", System.currentTimeMillis().toString())
                    }
                    AlarmNotifications.ACTION_PAUSE -> store.setEnabled(false)
                }
                AppRuntime.reconcile(context)
            } catch (_: RuntimeException) {
                AppRuntime.publish()
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        AppRuntime.worker.execute {
            try {
                val store = AppRuntime.store(context)
                store.maintain()
                store.setMeta("lastOutcome", if (intent.action == Intent.ACTION_BOOT_COMPLETED) "device_restarted" else "app_updated")
                AlarmNotifications.ensureChannels(context)
                // Never launch a mediaPlayback FGS from BOOT_COMPLETED on modern Android.
                AlarmNotifications.updateStatus(context, afterBoot = true)
            } catch (_: RuntimeException) { AppRuntime.publish() } finally { pending.finish() }
        }
    }
}
