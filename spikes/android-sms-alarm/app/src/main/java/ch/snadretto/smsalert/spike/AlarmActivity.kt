/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.spike

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** The lock-screen activity never exposes rule fields or message contents. */
class AlarmActivity : Activity() {
    private lateinit var title: TextView
    private var expectedId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val pending = SpikeStore(this@AlarmActivity).state().pending
            if (pending == null || pending.id != expectedId) {
                finish()
                return
            }
            title.setText(if (pending.simulated) R.string.test_title else R.string.alarm_title)
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expectedId = intent.getStringExtra(AlarmNotifications.ALARM_ID)
        if (expectedId == null) {
            finish()
            return
        }
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(32 + bars.left, 32 + bars.top, 32 + bars.right, 32 + bars.bottom)
            insets
        }
        title = TextView(this).apply { textSize = 30f }
        column.addView(title)
        column.addView(TextView(this).apply { setText(R.string.alarm_description); textSize = 18f })
        column.addView(Button(this).apply {
            setText(R.string.acknowledge)
            setOnClickListener {
                expectedId?.let { sendBroadcast(AlarmNotifications.acknowledgeIntent(this@AlarmActivity, it)) }
                finish()
            }
        })
        setContentView(column)
    }

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }
}
