/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var permissionResult: MethodChannel.Result? = null
    private var toneResult: MethodChannel.Result? = null
    private var requestedPermissions = emptyList<String>()
    private var streamListener: (() -> Unit)? = null
    private var methods: MethodChannel? = null
    private var events: EventChannel? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        AlarmNotifications.ensureChannels(this)
        AppRuntime.worker.execute {
            try {
                val store = AppRuntime.store(this)
                store.maintain()
                if (!AlarmService.running) AlarmAudio.restoreVolume(this, store)
                ToneAssets.prune(applicationContext)
            } catch (_: RuntimeException) { /* The channel reports storage failures to the UI. */ }
        }
        methods = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "ch.snadretto.smsalert/methods_v1").also { it.setMethodCallHandler(::handleMethod) }
        events = EventChannel(flutterEngine.dartExecutor.binaryMessenger, "ch.snadretto.smsalert/events_v1").also { channel ->
            channel.setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
                    val app = applicationContext
                    val listener = {
                        try { sink.success(AppRuntime.snapshot(app)) }
                        catch (_: RuntimeException) { sink.error("storage", "Lokale Daten konnten nicht gelesen werden.", null) }
                    }
                    streamListener = listener
                    AppRuntime.listeners.add(listener)
                    listener()
                }
                override fun onCancel(arguments: Any?) { streamListener?.let { AppRuntime.listeners.remove(it) }; streamListener = null }
            })
        }
    }

    private fun handleMethod(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "pickDeviceTone") {
            pickDeviceTone(call.arguments as? Map<*, *>, result)
            return
        }
        if (call.method in setOf("requestCorePermissions", "requestSmsPermission")) {
            requestRuntimePermissions(call.method, result)
            return
        }
        if (call.method == "openSettings") {
            try { openSetting((call.arguments as? Map<*, *>)?.string("kind", "") ?: ""); result.success(null) }
            catch (failure: AppFailure) { result.error(failure.code, failure.message, null) }
            return
        }
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any, Any>()
        AppRuntime.worker.execute {
            try {
                val store = AppRuntime.store(this)
                var response: Any? = null
                when (call.method) {
                    "getSnapshot" -> response = AppRuntime.snapshot(this)
                    "saveRule" -> store.saveRule(store.ruleFromArguments(args))
                    "deleteRule" -> store.deleteRule(args.string("id", ""))
                    "setEnabled" -> { store.setEnabled(args.boolean("enabled", false)); AppRuntime.reconcile(this) }
                    "updateSettings" -> { store.updateSettings(args); ToneAssets.prune(applicationContext); AppRuntime.reconcile(this) }
                    "previewRule" -> {
                        val ruleArgs = args["rule"] as? Map<*, *> ?: throw AppFailure("invalid_argument", "Regel fehlt.")
                        val rule = store.ruleFromArguments(ruleArgs)
                        response = rule.copy(enabled = true).matches(args.string("sender", ""), args.string("body", ""), (args["simSlot"] as? Number)?.toInt(), store.settings().country)
                    }
                    "startTestAlarm" -> { store.addTest(System.currentTimeMillis(), SystemClock.elapsedRealtime(), AppRuntime.bootCount(this)); AppRuntime.reconcile(this) }
                    "acknowledge" -> {
                        val id = args.string("id", "")
                        val test = store.pending().any { it["id"] == id && it["source"] == "TEST" }
                        if (store.acknowledge(id) && test) store.setMeta("lastTestAckAt", System.currentTimeMillis().toString())
                        AppRuntime.reconcile(this)
                    }
                    "acknowledgeAll" -> {
                        val hadTest = store.pending().any { it["source"] == "TEST" }
                        store.acknowledgeAll()
                        if (hadTest) store.setMeta("lastTestAckAt", System.currentTimeMillis().toString())
                        AppRuntime.reconcile(this)
                    }
                    "resumeAlarm" -> AppRuntime.reconcile(this)
                    "clearHistory" -> store.clearHistory()
                    else -> { AppRuntime.main.post { result.notImplemented() }; return@execute }
                }
                AppRuntime.main.post { result.success(response) }
            } catch (failure: AppFailure) { AppRuntime.main.post { result.error(failure.code, failure.message, null) } }
            catch (_: Exception) { AppRuntime.main.post { result.error("storage", "Lokale Daten konnten nicht verarbeitet werden. Bitte die App erneut öffnen.", null) } }
        }
    }

    private fun pickDeviceTone(args: Map<*, *>?, result: MethodChannel.Result) {
        if (toneResult != null || permissionResult != null) { result.error("busy", "Eine Systemauswahl ist bereits offen.", null); return }
        try {
            val existing = args?.string("existingUri", "").orEmpty()
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarmton auswählen")
                if (existing.startsWith("content://") || existing.startsWith("android.resource://")) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existing))
                }
            }
            val handler = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY)
                ?: throw AppFailure("tone_picker_unavailable", "Dieses Gerät stellt keine Android-Klingeltonauswahl bereit. Signal und Puls bleiben verfügbar.")
            intent.component = ComponentName(handler.activityInfo.packageName, handler.activityInfo.name)
            toneResult = result
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 72)
        } catch (failure: AppFailure) { toneResult = null; result.error(failure.code, failure.message, null) }
        catch (_: RuntimeException) { toneResult = null; result.error("tone_picker_unavailable", "Die Klingeltonauswahl konnte nicht geöffnet werden. Signal und Puls bleiben verfügbar.", null) }
    }

    @Deprecated("Android activity-result bridge for FlutterActivity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 72) return
        val pending = toneResult ?: return
        if (resultCode != RESULT_OK) { toneResult = null; pending.success(null); return }
        val uri = if (Build.VERSION.SDK_INT >= 33) data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else { @Suppress("DEPRECATION") data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) }
        if (uri == null) { toneResult = null; pending.error("invalid_tone", "Bitte einen hörbaren Klingelton auswählen; Lautlos ist kein Alarmton.", null); return }
        val app = applicationContext
        ToneAssets.io.execute {
            try {
                val selected = ToneAssets.import(app, uri)
                AppRuntime.main.post { if (toneResult === pending) { toneResult = null; pending.success(selected.toMap()) } }
            } catch (failure: AppFailure) {
                AppRuntime.main.post { if (toneResult === pending) { toneResult = null; pending.error(failure.code, failure.message, null) } }
            }
        }
    }

    private fun requestRuntimePermissions(method: String, result: MethodChannel.Result) {
        if (permissionResult != null || toneResult != null) { result.error("busy", "Eine Systemanfrage ist bereits offen.", null); return }
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (method == "requestCorePermissions" && Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { result.success(null); AppRuntime.publish(); return }
        permissionResult = result
        requestedPermissions = missing
        try { requestPermissions(missing.toTypedArray(), 71) }
        catch (_: RuntimeException) {
            permissionResult = null; requestedPermissions = emptyList()
            result.error("permission_request_unavailable", "Android konnte die Freigabe nicht öffnen. Bitte unter Einrichtung die App-Berechtigungen öffnen.", null)
            AppRuntime.publish()
        }
    }

    private fun openSetting(kind: String) {
        val packageUri = Uri.parse("package:$packageName")
        val intent = when (kind) {
            "dnd" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            "channel" -> Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).putExtra(Settings.EXTRA_CHANNEL_ID, AlarmNotifications.ALARM_CHANNEL)
            "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            "alarmAccess" -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
            "fullscreen" -> if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
            "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "app" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
            else -> throw AppFailure("invalid_argument", "Unbekannte Systemeinstellung.")
        }
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { throw AppFailure("settings_unavailable", "Diese Einstellung ist auf diesem Gerät nicht verfügbar.") }
        catch (_: SecurityException) { throw AppFailure("settings_unavailable", "Diese Einstellung ist durch das Gerät eingeschränkt.") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 71) {
            val pendingResult = permissionResult
            val requested = requestedPermissions
            permissionResult = null; requestedPermissions = emptyList()
            // The callback may be empty or automatically denied. Read the effective grants.
            when {
                Manifest.permission.RECEIVE_SMS in requested && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ->
                    pendingResult?.error("sms_permission_denied", "SMS-Zugriff wurde nicht freigegeben. Bei eingeschränkten Einstellungen öffne unter Einrichtung die hervorgehobene Hilfe: App-Info → ⋮ → Eingeschränkte Einstellungen zulassen, danach Berechtigungen → SMS.", null)
                Build.VERSION.SDK_INT >= 33 && Manifest.permission.POST_NOTIFICATIONS in requested && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                    pendingResult?.error("notifications_permission_denied", "Benachrichtigungen wurden nicht freigegeben. Bitte unter Einrichtung die Alarmbenachrichtigungen öffnen und erlauben.", null)
                else -> pendingResult?.success(null)
            }
            AppRuntime.publish()
        }
    }
    override fun onResume() { super.onResume(); AppRuntime.uiVisible = true; AlarmNotifications.ensureChannels(this); AppRuntime.publish() }
    override fun onPause() { AppRuntime.uiVisible = false; super.onPause() }
    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        streamListener?.let { AppRuntime.listeners.remove(it) }; streamListener = null
        methods?.setMethodCallHandler(null); events?.setStreamHandler(null)
        permissionResult?.error("cancelled", "Berechtigungsanfrage wurde beendet.", null); permissionResult = null; requestedPermissions = emptyList()
        toneResult?.error("cancelled", "Klingeltonauswahl wurde beendet.", null); toneResult = null
        super.cleanUpFlutterEngine(flutterEngine)
    }
}
