/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.Manifest
import android.app.Notification
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object AlarmNotifications {
    const val CHANNEL = "sms_alarm_spike_v1"
    const val ID = 1
    const val ALARM_ID = "alarm_id"
    const val ACK = "ch.snadretto.smsalert.spike.ACK"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_description)
            setSound(null, null) // The service owns continuous audio/vibration; never play twice.
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            if (manager.isNotificationPolicyAccessGranted) setBypassDnd(true)
        }
        manager.createNotificationChannel(channel) // User channel choices remain authoritative.
    }

    fun notificationsAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun fullScreenAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 34 ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun alarmAudioAccess(context: Context): Boolean = Build.VERSION.SDK_INT < 37 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun acknowledgeIntent(context: Context, id: String): Intent =
        Intent(context, AcknowledgeReceiver::class.java).apply {
            action = ACK
            data = Uri.parse("smsalert-spike://ack/$id")
            putExtra(ALARM_ID, id)
        }

    fun build(context: Context, alarm: PendingAlarm): Notification {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java).apply {
                data = Uri.parse("smsalert-spike://alarm/${alarm.id}")
                putExtra(ALARM_ID, alarm.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ack = PendingIntent.getBroadcast(
            context, 0, acknowledgeIntent(context, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, CHANNEL).apply {
            setSmallIcon(R.drawable.ic_alarm)
            setContentTitle(context.getString(if (alarm.simulated) R.string.test_title else R.string.alarm_title))
            setContentText(context.getString(R.string.notification_body))
            setContentIntent(open)
            setCategory(Notification.CATEGORY_ALARM)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setVisibility(Notification.VISIBILITY_PRIVATE)
            addAction(Notification.Action.Builder(null, context.getString(R.string.acknowledge), ack).build())
            if (fullScreenAllowed(context)) setFullScreenIntent(open, true)
        }.build()
    }
}
