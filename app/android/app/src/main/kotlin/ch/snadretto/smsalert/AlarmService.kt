/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.json.JSONObject

object AlarmAudio {
    fun applyVolume(context: Context, store: AppStore, settings: AlarmSettings) {
        if (!settings.raiseSystemVolume) { restoreVolume(context, store); return }
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio.isVolumeFixed) { store.setMeta("audioWarning", "fixed_volume"); return }
        val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val target = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val lease = store.meta("volumeLease")?.let { JSONObject(it) }
        if (lease != null && (lease.optInt("applied") != current || lease.optInt("boot") != AppRuntime.bootCount(context))) {
            store.setMeta("audioWarning", "volume_changed_by_user")
            return
        }
        if (current >= target) return
        // Persist before touching the shared stream, so process death does not lose restoration data.
        if (lease == null) store.setMeta("volumeLease", JSONObject(mapOf("original" to current, "applied" to target, "boot" to AppRuntime.bootCount(context))).toString())
        try {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != target) store.setMeta("audioWarning", "system_volume_blocked")
        } catch (_: RuntimeException) { store.setMeta("audioWarning", "system_volume_blocked") }
    }

    fun restoreVolume(context: Context, store: AppStore) {
        val raw = store.meta("volumeLease") ?: return
        try {
            val lease = JSONObject(raw)
            val audio = context.getSystemService(AudioManager::class.java)
            if (lease.optInt("boot") == AppRuntime.bootCount(context) && audio.getStreamVolume(AudioManager.STREAM_ALARM) == lease.getInt("applied")) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, lease.getInt("original"), 0)
            }
        } catch (_: RuntimeException) { store.setMeta("audioWarning", "volume_restore_unavailable") }
        finally { store.removeMeta("volumeLease") }
    }
}

class AlarmService : Service() {
    companion object {
        @Volatile var running = false; private set
        @Volatile var audioStarted = false; private set
    }
    private lateinit var store: AppStore
    private lateinit var audio: AudioManager
    private lateinit var vibrator: Vibrator
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var activeId: String? = null
    private var audioKey: String? = null
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    private fun outcome(code: String) {
        store.setMeta("lastOutcome", code)
        activeId?.let { store.recordOutcome(it, code) }
    }

    override fun onCreate() {
        super.onCreate()
        store = AppRuntime.store(this)
        audio = getSystemService(AudioManager::class.java)
        vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = store.settings()
        val event = store.nextPending()
        if (!settings.enabled || event == null) { stopSelf(startId); return START_NOT_STICKY }
        try {
            AlarmNotifications.ensureChannels(this)
            if (!AlarmNotifications.allowed(this)) { store.setMeta("lastOutcome", "notifications_blocked"); stopSelf(startId); return START_NOT_STICKY }
            startForeground(AlarmNotifications.ALARM_ID, AlarmNotifications.alarm(this, event, store.pendingCount(), activeId != event["id"]), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            running = true
        } catch (_: RuntimeException) {
            store.setMeta("lastOutcome", "background_start_blocked")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val id = event["id"] as String
        if (activeId == id && audioKey == settings.audioKey && audioStarted) return START_STICKY
        releaseSignals()
        activeId = id
        audioKey = settings.audioKey
        store.setMeta("audioWarning", "")
        AlarmAudio.applyVolume(this, store, settings)
        if (settings.vibration) {
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 650, 450), 0), attributes)
            } catch (_: RuntimeException) { store.setMeta("audioWarning", "vibration_failed") }
        }
        try {
            val media = if (settings.tone == "device") {
                try { preparedPlayer(settings, useDevice = true) }
                catch (_: Exception) {
                    store.setMeta("audioWarning", "device_tone_fallback")
                    preparedPlayer(settings.copy(tone = "beacon"), useDevice = false)
                }
            } else preparedPlayer(settings, useDevice = false)
            player = media
            media.setOnErrorListener { failed, _, _ ->
                if (player === failed) { player = null; audioStarted = false }
                failed.release()
                outcome("audio_failed")
                true
            }
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener({ change ->
                    if (change == AudioManager.AUDIOFOCUS_GAIN) startAudio()
                    else {
                        try { player?.pause() } catch (_: IllegalStateException) { }
                        audioStarted = false
                        outcome("audio_focus_lost")
                    }
                }, Handler(Looper.getMainLooper())).build()
            focus = request
            if (audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) startAudio()
            else outcome(if (Build.VERSION.SDK_INT >= 37 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) "alarm_audio_access_required" else "audio_focus_denied")
        } catch (_: Exception) {
            player?.release(); player = null; audioStarted = false
            outcome("audio_failed")
        }
        AppRuntime.publish()
        return START_STICKY
    }

    private fun preparedPlayer(settings: AlarmSettings, useDevice: Boolean): MediaPlayer {
        val media = MediaPlayer()
        try {
            media.setAudioAttributes(attributes)
            media.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            if (useDevice) {
                val selected = settings.deviceTone ?: throw AppFailure("tone_unavailable", "Smartphone-Klingelton fehlt.")
                ToneAssets.requireAvailable(this, selected)
                java.io.FileInputStream(ToneAssets.file(this, selected.id)).use { media.setDataSource(it.fd) }
            } else {
                resources.openRawResourceFd(if (settings.tone == "pulse") R.raw.pulse else R.raw.beacon).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.declaredLength) }
            }
            media.isLooping = true
            val gain = settings.volume / 100f
            media.setVolume(gain, gain)
            media.prepare()
            return media
        } catch (failure: Exception) { media.release(); throw failure }
    }

    private fun startAudio() {
        val current = store.nextPending() ?: return
        if (!store.settings().enabled || current["id"] != activeId) return
        val media = player ?: return
        try {
            media.start()
            audioStarted = true
            val boot = (current["bootCount"] as? Number)?.toInt()
            val received = (current["receivedElapsedMs"] as? Number)?.toLong()
            if (boot != null && boot >= 0 && boot == AppRuntime.bootCount(this) && received != null) {
                val latency = (SystemClock.elapsedRealtime() - received).coerceAtLeast(0)
                store.setMeta("lastLatencyMs", latency.toString())
                store.recordOutcome(activeId!!, "playback_started", latency)
            }
            outcome("playback_started")
        } catch (_: RuntimeException) { audioStarted = false; outcome("audio_failed") }
    }

    private fun releaseSignals() {
        activeId = null; audioKey = null; audioStarted = false
        try { player?.release() } catch (_: RuntimeException) { }
        player = null
        try { focus?.let { audio.abandonAudioFocusRequest(it) } } catch (_: RuntimeException) { }
        focus = null
        try { vibrator.cancel() } catch (_: RuntimeException) { }
    }

    override fun onDestroy() {
        releaseSignals()
        running = false
        AlarmAudio.restoreVolume(this, store)
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(AlarmNotifications.ALARM_ID)
        if (store.nextPending() != null && store.settings().enabled) store.setMeta("lastOutcome", "alarm_interrupted")
        AppRuntime.publish()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
