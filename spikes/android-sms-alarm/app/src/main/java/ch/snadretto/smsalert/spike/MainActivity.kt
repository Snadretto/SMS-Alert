/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var store: SpikeStore
    private lateinit var sender: EditText
    private lateinit var text: EditText
    private lateinit var status: TextView
    private lateinit var test: Button
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SpikeStore(this)
        AlarmNotifications.ensureChannel(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val padding = (16 * resources.displayMetrics.density).toInt()
        column.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        fun label(id: Int) {
            column.addView(TextView(this).apply { setText(id); textSize = 16f })
        }
        fun button(id: Int, action: () -> Unit): Button = Button(this).apply {
            setText(id)
            setOnClickListener { action(); refreshStatus() }
            column.addView(this)
        }
        label(R.string.prototype_notice)
        val rule = store.rule()
        sender = EditText(this).apply {
            setHint(R.string.sender_hint)
            setText(rule.sender)
            isSingleLine = true
        }
        text = EditText(this).apply {
            setHint(R.string.text_hint)
            setText(rule.text)
            isSingleLine = true
        }
        column.addView(sender)
        column.addView(text)
        label(R.string.rule_help)
        button(R.string.save) { if (saveRule()) toast(R.string.saved) }
        button(R.string.grant_permissions) {
            val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
            requestPermissions(permissions.toTypedArray(), 1)
        }
        button(R.string.enable) { if (saveRule()) store.enable() }
        button(R.string.pause) {
            store.pause()
            stopService(Intent(this, AlarmService::class.java))
        }
        test = button(R.string.test_alarm) {
            AlarmLauncher.start(this, SystemClock.elapsedRealtime(), simulated = true)
        }
        button(R.string.acknowledge) {
            store.state().pending?.let { sendBroadcast(AlarmNotifications.acknowledgeIntent(this, it.id)) }
        }
        button(R.string.dnd_settings) { openSettings(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        button(R.string.channel_settings) {
            openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, AlarmNotifications.CHANNEL))
        }
        button(R.string.fullscreen_settings) {
            val action = if (Build.VERSION.SDK_INT >= 34) Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                else Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            openSettings(Intent(action, Uri.parse("package:$packageName")))
        }
        button(R.string.battery_settings) { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        button(R.string.sound_settings) { openSettings(Intent(Settings.ACTION_SOUND_SETTINGS)) }
        if (Build.VERSION.SDK_INT >= 37) {
            label(R.string.alarm_audio_help)
            button(R.string.alarm_audio_settings) {
                openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
        status = TextView(this).apply { textSize = 14f; setTextIsSelectable(true) }
        column.addView(status)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun saveRule(): Boolean {
        val rule = Rule(sender.text.toString().trim(), text.text.toString().trim())
        if (!rule.valid) {
            toast(R.string.invalid_rule)
            return false
        }
        store.saveRule(rule)
        return true
    }

    private fun refreshStatus() {
        AlarmNotifications.ensureChannel(this)
        val state = store.state()
        val manager = getSystemService(NotificationManager::class.java)
        val audio = getSystemService(AudioManager::class.java)
        fun yes(value: Boolean): String = getString(if (value) R.string.yes else R.string.no)
        val latency = store.latencyMs()
        val ack = store.lastAcknowledgedMs()
        status.text = getString(
            R.string.status_format,
            getString(if (state.enabled) R.string.active else R.string.paused),
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT, applicationInfo.targetSdkVersion,
            yes(checkSelfPermission(Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED),
            yes(AlarmNotifications.notificationsAllowed(this)),
            yes(manager.isNotificationPolicyAccessGranted),
            yes(manager.getNotificationChannel(AlarmNotifications.CHANNEL)?.canBypassDnd() == true),
            manager.currentInterruptionFilter,
            audio.getStreamVolume(AudioManager.STREAM_ALARM),
            audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            yes(getSystemService(VibratorManager::class.java).defaultVibrator.hasVibrator()),
            yes(AlarmNotifications.fullScreenAllowed(this)),
            yes(getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)),
            state.pending?.let { "${it.id.take(8)}, ${it.matches} Treffer" } ?: "—",
            store.result(),
            if (latency >= 0) "$latency ms" else "—",
            if (ack >= 0) DateFormat.getDateTimeInstance().format(Date(ack)) else "—",
        )
        status.append("\n" + getString(R.string.status_alarm_audio, yes(AlarmNotifications.alarmAudioAccess(this))))
        test.isEnabled = state.enabled && state.pending == null
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.settings_unavailable)
        } catch (_: SecurityException) {
            toast(R.string.settings_unavailable)
        }
    }

    private fun toast(id: Int) = Toast.makeText(this, id, Toast.LENGTH_SHORT).show()

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }
}
