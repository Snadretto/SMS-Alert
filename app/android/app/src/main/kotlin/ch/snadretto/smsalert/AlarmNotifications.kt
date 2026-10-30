/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AlarmNotifications {
    const val ALARM_CHANNEL = "sms_alarm_v1"
    const val STATUS_CHANNEL = "sms_status_v1"
    const val ALARM_ID = 101
    const val STATUS_ID = 102
    const val ACTION_ACK = "ch.snadretto.smsalert.ACK"
    const val ACTION_PAUSE = "ch.snadretto.smsalert.PAUSE"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL, "Pikett-Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Offene SMS-Alarme mit eigener Ton- und Vibrationssteuerung"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            if (manager.isNotificationPolicyAccessGranted) setBypassDnd(true)
        })
        manager.createNotificationChannel(NotificationChannel(STATUS_CHANNEL, "Bereitschaft und Hinweise", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }

    fun allowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(ALARM_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun fullScreenAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun actionIntent(context: Context, action: String, id: String = ""): Intent = Intent(context, AlarmActionReceiver::class.java).apply {
        this.action = action
        data = Uri.parse("smsalert://action/${action.substringAfterLast('.')}/$id")
        putExtra("eventId", id)
    }

    private fun pendingAction(context: Context, action: String, id: String = ""): PendingIntent = PendingIntent.getBroadcast(
        context, 0, actionIntent(context, action, id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun alarm(context: Context, event: Map<String, Any?>, count: Int): Notification {
        val id = event["id"] as String
        val openAlarm = PendingIntent.getActivity(context, 0, Intent(context, AlarmActivity::class.java).apply {
            data = Uri.parse("smsalert://alarm/$id")
            putExtra("eventId", id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(context, ALARM_CHANNEL).apply {
            setSmallIcon(R.drawable.ic_alarm)
            setContentTitle(if (event["source"] == "TEST") "TESTALARM · keine SMS" else "Pikett-Alarm")
            setContentText(if (count == 1) "Ein Alarm wartet auf Quittierung." else "$count Alarme warten auf Quittierung.")
            setCategory(Notification.CATEGORY_ALARM)
            setVisibility(Notification.VISIBILITY_PRIVATE)
            setPublicVersion(Notification.Builder(context, ALARM_CHANNEL).setSmallIcon(R.drawable.ic_alarm).setContentTitle("SMS-Alert: Alarm aktiv").build())
            setOngoing(true)
            setOnlyAlertOnce(true)
            setContentIntent(openAlarm)
            addAction(Notification.Action.Builder(null, "Diesen Alarm quittieren", pendingAction(context, ACTION_ACK, id)).build())
            if (fullScreenAllowed(context)) setFullScreenIntent(openAlarm, true)
        }.build()
    }

    fun updateStatus(context: Context, afterBoot: Boolean = false) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val enabled = AppRuntime.store(context).settings().enabled
        if (!enabled || !allowed(context)) { manager.cancel(STATUS_ID); return }
        manager.notify(STATUS_ID, Notification.Builder(context, STATUS_CHANNEL).apply {
            setSmallIcon(R.drawable.ic_alarm)
            setContentTitle(if (afterBoot) "SMS-Alert nach Neustart prüfen" else "SMS-Alarmierung aktiviert")
            setContentText(if (afterBoot) "App öffnen und Einrichtung kontrollieren." else "Bereitschaft gilt innerhalb Ihrer freigegebenen Systemeinstellungen.")
            setContentIntent(openApp(context))
            setOngoing(true)
            setOnlyAlertOnce(true)
            setVisibility(Notification.VISIBILITY_PRIVATE)
            addAction(Notification.Action.Builder(null, "App pausieren", pendingAction(context, ACTION_PAUSE)).build())
        }.build())
    }
}
