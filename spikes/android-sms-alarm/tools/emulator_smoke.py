#!/usr/bin/env python3
"""Exercise the real receiver/service path with synthetic SMS on an emulator only."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "ch.snadretto.smsalert.spike"
parser = argparse.ArgumentParser()
parser.add_argument("--adb", required=True)
parser.add_argument("--serial", required=True)
parser.add_argument("--apk", required=True, type=Path)
parser.add_argument("--reset", action="store_true", help="Reset only this test app's emulator data")
parser.add_argument("--output", type=Path, default=Path("app/build/reports/emulator-smoke.json"))
args = parser.parse_args()
if not args.serial.startswith("emulator-") or not args.reset:
    raise SystemExit("Nur für einen expliziten Emulator und mit --reset ausführen; keine echten Telefone.")
results = []


def adb(*command, check=True):
    result = subprocess.run(
        [args.adb, "-s", args.serial, *command], check=check,
        capture_output=True, text=True, timeout=30,
    )
    return result.stdout.strip()


def preferences():
    raw = adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/spike.xml", check=False)
    if not raw.startswith("<?xml"):
        return {}
    values = {}
    for item in ET.fromstring(raw):
        value = item.text if item.tag == "string" else item.attrib.get("value")
        if item.tag == "boolean":
            value = value == "true"
        elif item.tag in ("long", "int"):
            value = int(value)
        values[item.attrib["name"]] = value
    return values


def wait(predicate, timeout=12):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        value = predicate()
        if value:
            return value
        time.sleep(0.25)
    raise RuntimeError("Zeitlimit: " + getattr(predicate, "__name__", "Bedingung"))


def record(name, status="PASS", **details):
    results.append({"case": name, "status": status, **details})
    print(status + ": " + name, flush=True)


def nodes():
    adb("shell", "uiautomator", "dump", "/data/local/tmp/sms-alert-ui.xml")
    return list(ET.fromstring(adb("shell", "cat", "/data/local/tmp/sms-alert-ui.xml")).iter("node"))


def tap(node):
    left, top, right, bottom = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(0.25)


def button(label):
    for _ in range(5):
        for node in nodes():
            if node.attrib.get("text", "").casefold() == label.casefold() and node.attrib.get("enabled") == "true":
                tap(node)
                return
        adb("shell", "input", "swipe", "540", "1800", "540", "700", "300")
    raise RuntimeError("Schaltfläche nicht gefunden: " + label)


def open_app():
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "input", "keyevent", "82")  # Emulator has no secure PIN.
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")


def playback_started():
    state = preferences()
    if state.get("alarm_id") and state.get("result") == "playback_started_not_audibility_verified":
        return state
    return None


try:
    if adb("shell", "getprop", "ro.kernel.qemu") != "1":
        raise RuntimeError("Gerät ist kein verifizierter Android-Emulator; keine Daten werden zurückgesetzt.")
    adb("install", "-r", str(args.apk.resolve()))
    adb("shell", "pm", "clear", PACKAGE)
    for permission in ("RECEIVE_SMS", "POST_NOTIFICATIONS"):
        adb("shell", "pm", "grant", PACKAGE, "android.permission." + permission)
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    adb("shell", "cmd", "appops", "set", PACKAGE, "SCHEDULE_EXACT_ALARM", "deny")
    adb("shell", "cmd", "appops", "set", PACKAGE, "USE_FULL_SCREEN_INTENT", "deny")
    open_app()
    assert not preferences().get("enabled", False)
    record("fresh_install_paused")

    for index, value in enumerate(("15551234567", "ALARM")):
        fields = [node for node in nodes() if node.attrib.get("class") == "android.widget.EditText"]
        tap(fields[index])
        adb("shell", "input", "text", value)
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
    button("SMS-Alarm aktivieren")
    wait(lambda: preferences().get("enabled"))

    before = preferences()
    adb("emu", "sms", "send", "15559999999", "ALARM_WRONG_SENDER")
    time.sleep(2)
    assert preferences() == before
    record("non_matching_sms_ignored")

    button("Lokalen Testalarm starten")
    simulated = wait(playback_started)
    assert simulated["simulated"] is True
    button("Quittieren")
    wait(lambda: "alarm_id" not in preferences())
    wait(lambda: "isForeground=true" not in adb("shell", "dumpsys", "activity", "services", PACKAGE))
    record("local_test_and_acknowledgement", latency_ms=simulated["latency_ms"])

    # Observe the missing-access profile; other lifecycle exemptions may also apply.
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    pid = adb("shell", "pidof", PACKAGE, check=False)
    if pid:
        if not pid.isdecimal():
            raise RuntimeError("Unerwartete Prozess-ID: " + pid)
        adb("shell", "run-as", PACKAGE, "kill", "-9", pid)
    wait(lambda: not adb("shell", "pidof", PACKAGE, check=False))
    adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
    adb("emu", "sms", "send", "15551234567", "ALARM_AUDIO_ACCESS_DENIED")
    outcome = wait(lambda: preferences().get("result") in (
        "alarm_audio_access_required", "audio_focus_denied", "playback_started_not_audibility_verified",
    ) and preferences())
    # Other legitimate lifecycle exemptions can survive recent foreground interaction.
    # Observe the result instead of claiming permission denial must always silence audio.
    record("background_audio_without_exact_alarm_access", status="OBSERVED", outcome=outcome["result"])
    open_app()
    button("Quittieren")
    wait(lambda: "alarm_id" not in preferences())
    wait(lambda: "isForeground=true" not in adb("shell", "dumpsys", "activity", "services", PACKAGE))
    adb("shell", "cmd", "appops", "set", PACKAGE, "SCHEDULE_EXACT_ALARM", "allow")

    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    pid = adb("shell", "pidof", PACKAGE, check=False)
    if pid:
        if not pid.isdecimal():
            raise RuntimeError("Unerwartete Prozess-ID: " + pid)
        # Simulate process death, without setting the package's force-stopped flag.
        adb("shell", "run-as", PACKAGE, "kill", "-9", pid)
    wait(lambda: not adb("shell", "pidof", PACKAGE, check=False))
    adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
    body = "ALARM_BODY_NOT_PERSISTED_A01"
    adb("emu", "sms", "send", "15551234567", body)
    cold = wait(playback_started)
    assert cold["simulated"] is False
    assert cold["latency_ms"] <= 5000
    assert body not in json.dumps(cold)
    services = adb("shell", "dumpsys", "activity", "services", PACKAGE)
    assert "isForeground=true" in services
    record("cold_process_sms_starts_foreground_alarm", latency_ms=cold["latency_ms"], display="off before SMS")

    open_app()
    button("Quittieren")
    wait(lambda: "alarm_id" not in preferences())
    wait(lambda: "isForeground=true" not in adb("shell", "dumpsys", "activity", "services", PACKAGE))
    assert preferences().get("last_ack_wall_ms", -1) > 0
    record("sms_alarm_acknowledgement_stops_service")

    button("App pausieren und aktuellen Alarm beenden")
    paused = preferences()
    assert paused["enabled"] is False
    adb("emu", "sms", "send", "15551234567", "ALARM_WHILE_PAUSED")
    time.sleep(2)
    assert preferences() == paused
    record("paused_sms_has_no_alarm_or_diagnostic_write")

    button("SMS-Alarm aktivieren")
    wait(lambda: preferences().get("enabled"))
    assert "alarm_id" not in preferences()
    adb("shell", "am", "force-stop", PACKAGE)
    adb("emu", "sms", "send", "15551234567", "ALARM_FORCE_STOPPED")
    time.sleep(2)
    assert "alarm_id" not in preferences()
    assert not adb("shell", "pidof", PACKAGE, check=False)
    record("force_stop_is_explicit_negative_case")
    open_app()
    button("App pausieren und aktuellen Alarm beenden")
except Exception as error:
    results.append({"case": "smoke_test_failure", "status": "FAIL", "error": str(error)})
    print("FAIL: " + str(error), flush=True)
    raise
finally:
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({
        "device": args.serial,
        "timestamp_utc": datetime.now(timezone.utc).isoformat(),
        "apk_sha256": hashlib.sha256(args.apk.read_bytes()).hexdigest(),
        "configuration": "exact-alarm access denied (comparison), then granted; full-screen denied; Wi-Fi and mobile data disabled",
        "platform": "Android 17 / API 37 AOSP emulator",
        "scope": "synthetic SMS; no physical sound/vibration or cellular delivery proof",
        "results": results,
    }, ensure_ascii=False, indent=2) + "\n")
