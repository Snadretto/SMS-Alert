/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibratorManager
import android.provider.Settings
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

object AppRuntime {
    val worker = Executors.newSingleThreadExecutor()
    val main = Handler(Looper.getMainLooper())
    val listeners = CopyOnWriteArraySet<() -> Unit>()
    @Volatile private var database: AppStore? = null

    fun store(context: Context): AppStore = database ?: synchronized(this) {
        database ?: AppStore(context.applicationContext, changed = ::publish).also { database = it }
    }

    fun publish() { main.post { listeners.forEach { it() } } }
    fun bootCount(context: Context): Int = try { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) } catch (_: Exception) { -1 }

    fun reconcile(context: Context) {
        val app = context.applicationContext
        main.post {
            val store = store(app)
            try {
                AlarmNotifications.ensureChannels(app)
                val enabled = store.settings().enabled
                if (!enabled || store.nextPending() == null) {
                    app.stopService(Intent(app, AlarmService::class.java))
                    if (!AlarmService.running) AlarmAudio.restoreVolume(app, store)
                } else if (AlarmNotifications.allowed(app)) {
                    app.startForegroundService(Intent(app, AlarmService::class.java))
                } else {
                    app.stopService(Intent(app, AlarmService::class.java))
                    store.setMeta("lastOutcome", "notifications_blocked")
                }
                AlarmNotifications.updateStatus(app)
            } catch (_: RuntimeException) {
                store.setMeta("lastOutcome", "background_start_blocked")
                publish()
            }
        }
    }

    fun snapshot(context: Context): Map<String, Any?> {
        val store = store(context)
        val settings = store.settings()
        val rules = store.rules()
        val manager = context.getSystemService(NotificationManager::class.java)
        val audio = context.getSystemService(AudioManager::class.java)
        val notificationPermission = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val exact = Build.VERSION.SDK_INT < 37 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val diagnostics = linkedMapOf<String, Any?>(
            "api" to Build.VERSION.SDK_INT,
            "androidVersion" to Build.VERSION.RELEASE,
            "smsPermission" to (context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED),
            "notificationPermission" to notificationPermission,
            "notificationsEnabled" to AlarmNotifications.allowed(context),
            "dndAccess" to manager.isNotificationPolicyAccessGranted,
            "channelBypassDnd" to (manager.getNotificationChannel(AlarmNotifications.ALARM_CHANNEL)?.canBypassDnd() == true),
            "interruptionFilter" to manager.currentInterruptionFilter,
            "alarmVolume" to audio.getStreamVolume(AudioManager.STREAM_ALARM),
            "maxAlarmVolume" to audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            "fixedVolume" to audio.isVolumeFixed,
            "vibratorAvailable" to context.getSystemService(VibratorManager::class.java).defaultVibrator.hasVibrator(),
            "fullScreenAllowed" to AlarmNotifications.fullScreenAllowed(context),
            "alarmAudioAccess" to exact,
            "batteryExempt" to context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            "telephonyAvailable" to context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            "serviceRunning" to AlarmService.running,
            "audioStarted" to AlarmService.audioStarted,
            "lastOutcome" to (store.meta("lastOutcome") ?: "not_tested"),
            "audioWarning" to (store.meta("audioWarning") ?: ""),
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "lastTestAckAt" to store.meta("lastTestAckAt")?.toLongOrNull(),
            "lastLatencyMs" to store.meta("lastLatencyMs")?.toLongOrNull(),
            "directBootSupported" to false,
        )
        return linkedMapOf(
            "schemaVersion" to 1, "supported" to true, "platform" to "android",
            "settings" to settings.toMap(), "rules" to rules.map { it.toMap() },
            "pending" to store.pending(), "pendingCount" to store.pendingCount(),
            "history" to store.history(), "diagnostics" to diagnostics,
        )
    }
}
