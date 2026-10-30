/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmService : Service() {
    private lateinit var store: SpikeStore
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var runningId: String? = null
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override fun onCreate() {
        super.onCreate()
        store = SpikeStore(this)
        audioManager = getSystemService(AudioManager::class.java)
        vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = store.state()
        val alarm = state.pending
        if (!state.enabled || alarm == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            AlarmNotifications.ensureChannel(this)
            if (!AlarmNotifications.notificationsAllowed(this)) {
                store.fail(alarm.id, "notifications_blocked")
                stopSelf(startId)
                return START_NOT_STICKY
            }
            startForeground(
                AlarmNotifications.ID, AlarmNotifications.build(this, alarm),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (error: RuntimeException) {
            store.fail(alarm.id, "foreground_failed:${error.javaClass.simpleName}")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (runningId == alarm.id) return START_STICKY
        releaseSignals()
        runningId = alarm.id
        try {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attributes)
        } catch (error: RuntimeException) {
            store.record("vibration_failed:${error.javaClass.simpleName}")
        }
        try {
            val media = MediaPlayer()
            player = media // Retain ownership even if setup/prepare fails, so the catch releases it.
            media.also {
                media.setAudioAttributes(attributes)
                media.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                resources.openRawResourceFd(R.raw.alarm).use { asset ->
                    media.setDataSource(asset.fileDescriptor, asset.startOffset, asset.declaredLength)
                }
                media.isLooping = true
                media.setOnErrorListener { _, _, _ ->
                    store.record("audio_playback_error")
                    true // Keep the visible alarm/vibration available for acknowledgement.
                }
                media.prepare()
            }
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener({ change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> startAudio()
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            try {
                                player?.pause()
                            } catch (_: IllegalStateException) {
                                // Playback may already have failed; preserve the quittable alarm.
                            }
                            store.record("audio_focus_lost")
                        }
                    }
                }, Handler(Looper.getMainLooper()))
                .build()
            focus = request
            // Android 15+ requires a visible app or foreground service before requesting focus.
            if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startAudio()
            } else {
                store.record(if (AlarmNotifications.alarmAudioAccess(this)) "audio_focus_denied"
                    else "alarm_audio_access_required")
            }
        } catch (error: Exception) {
            player?.release()
            player = null
            store.record("audio_failed:${error.javaClass.simpleName}")
        }
        return START_STICKY
    }

    private fun startAudio() {
        val media = player ?: return
        val pending = store.state().pending ?: return
        if (pending.id != runningId) return
        try {
            media.start()
            val latency = SystemClock.elapsedRealtime() - pending.receivedElapsedMs
            store.record("playback_started_not_audibility_verified", if (latency >= 0) latency else -1)
        } catch (error: RuntimeException) {
            store.record("audio_failed:${error.javaClass.simpleName}")
        }
    }

    private fun releaseSignals() {
        runningId = null
        player?.release()
        player = null
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
        vibrator.cancel()
    }

    override fun onDestroy() {
        releaseSignals()
        if (store.state().pending != null) store.record("service_stopped_with_open_alarm")
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(AlarmNotifications.ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
