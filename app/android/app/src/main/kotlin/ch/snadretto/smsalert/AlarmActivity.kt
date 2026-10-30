/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Only generic alarm information is displayed over the keyguard. */
class AlarmActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var expectedId: String? = null
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private val update = object : Runnable {
        override fun run() {
            val store = AppRuntime.store(this@AlarmActivity)
            val next = store.nextPending()
            if (next == null || next["id"] != expectedId || !store.settings().enabled) { finish(); return }
            title.text = if (next["source"] == "TEST") "TESTALARM" else "Pikett-Alarm"
            subtitle.text = "${store.pendingCount()} offene Alarme\nQuittierung gilt nur auf diesem Gerät."
            handler.postDelayed(this, 500)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expectedId = intent.getStringExtra("eventId")
        if (expectedId == null) { finish(); return }
        setShowWhenLocked(true); setTurnScreenOn(true)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        val padding = (24 * resources.displayMetrics.density).toInt()
        column.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        title = TextView(this).apply { textSize = 34f }
        subtitle = TextView(this).apply { textSize = 18f }
        column.addView(title); column.addView(subtitle)
        column.addView(Button(this).apply {
            text = "Diesen Alarm quittieren"
            setOnClickListener {
                expectedId?.let { sendBroadcast(AlarmNotifications.actionIntent(this@AlarmActivity, AlarmNotifications.ACTION_ACK, it)) }
                finish()
            }
        })
        column.addView(Button(this).apply {
            text = "App pausieren"
            setOnClickListener {
                AlertDialog.Builder(this@AlarmActivity).setTitle("Alarmierung pausieren?")
                    .setMessage("Alle offenen Alarme werden beendet. Neue SMS lösen bis zur manuellen Reaktivierung keinen Alarm aus.")
                    .setNegativeButton("Zurück", null).setPositiveButton("Pausieren") { _, _ ->
                        sendBroadcast(AlarmNotifications.actionIntent(this@AlarmActivity, AlarmNotifications.ACTION_PAUSE)); finish()
                    }.show()
            }
        })
        setContentView(column)
    }
    override fun onStart() { super.onStart(); handler.post(update) }
    override fun onStop() { handler.removeCallbacks(update); super.onStop() }
}
