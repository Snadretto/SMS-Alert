/* Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0 */
package ch.snadretto.smsalert.probe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/** Separate UID, no permissions, no code/dependencies included in the real APK. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        JSONObject report = new JSONObject();
        try {
            report.put("uid", android.os.Process.myUid());
            try {
                sendBroadcast(new Intent("android.provider.Telephony.SMS_RECEIVED")
                    .setComponent(new ComponentName("ch.snadretto.smsalert", "ch.snadretto.smsalert.SmsReceiver")));
                report.put("protectedSmsRejected", false);
            } catch (SecurityException expected) { report.put("protectedSmsRejected", true); }
            // Nonexported delivery is dropped by Android; external observation must confirm no state change.
            sendBroadcast(new Intent("ch.snadretto.smsalert.PAUSE")
                .setComponent(new ComponentName("ch.snadretto.smsalert", "ch.snadretto.smsalert.AlarmActionReceiver")));
            report.put("pauseIntentSent", true);
            try (java.io.FileOutputStream output = openFileOutput("result.json", MODE_PRIVATE)) {
                output.write(report.toString().getBytes(StandardCharsets.UTF_8));
            }
            TextView text = new TextView(this); text.setText(report.toString()); setContentView(text);
        } catch (Exception error) { throw new IllegalStateException("Probe failed", error); }
    }
}
