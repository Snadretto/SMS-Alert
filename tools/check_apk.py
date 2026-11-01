#!/usr/bin/env python3
"""Check the packaged manifest, not just the source manifest."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("apk", type=Path)
parser.add_argument("--aapt2", required=True, type=Path)
args = parser.parse_args()


def dump(kind, *extra):
    return subprocess.run(
        [str(args.aapt2), "dump", kind, *extra, str(args.apk)],
        check=True, capture_output=True, text=True,
    ).stdout


permissions = set(re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'", dump("permissions")))
expected = {
    "ch.snadretto.smsalert.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    "android.permission.RECEIVE_SMS",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.VIBRATE",
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    "android.permission.ACCESS_NOTIFICATION_POLICY",
    "android.permission.USE_FULL_SCREEN_INTENT",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.MODIFY_AUDIO_SETTINGS",
    "android.permission.RECEIVE_BOOT_COMPLETED",
}
if permissions != expected:
    raise SystemExit(f"Unerwartete Berechtigungen: zusätzlich={permissions - expected}, fehlend={expected - permissions}")
manifest = dump("xmltree", "--file", "AndroidManifest.xml")
if not re.search(r"android:allowBackup[^\n]*=(?:false|\(type 0x12\)0x0)\b", manifest):
    raise SystemExit("allowBackup=false fehlt im gepackten Manifest")
if "android.permission.BROADCAST_SMS" not in manifest:
    raise SystemExit("Berechtigungsschutz des SMS-Receivers fehlt")
lines = manifest.splitlines()
components = {}
for index, line in enumerate(lines):
    element = re.match(r"(\s*)E: (activity|receiver|service|provider)\b", line)
    if element is None:
        continue
    indent = len(element.group(1))
    attributes = []
    for child in lines[index + 1:]:
        if child.strip() and len(child) - len(child.lstrip()) <= indent:
            break
        attributes.append(child)
    attributes = "\n".join(attributes)
    name = re.search(r'android:name[^\n]*?="([^"]+)"', attributes)
    exported = re.search(r"android:exported[^\n]*=(true|false)", attributes)
    if name and name.group(1).endswith("ProfileInstallReceiver") and 'android.permission.DUMP' not in attributes:
        raise SystemExit("ProfileInstallReceiver muss durch DUMP geschützt sein")
    if name and exported:
        components[name.group(1).split(".")[-1]] = exported.group(1)
expected_components = {
    "InitializationProvider": "false", "ProfileInstallReceiver": "true", "MainActivity": "true", "SmsReceiver": "true", "AlarmActivity": "false",
    "AlarmActionReceiver": "false", "AlarmService": "false", "BootReceiver": "false",
}
if components != expected_components:
    raise SystemExit(f"Unerwartete exportierte Komponenten: {components}")
badging = dump("badging")
if not re.search(r"(?:minSdkVersion|sdkVersion):'31'", badging) or "targetSdkVersion:'37'" not in badging:
    raise SystemExit("Unerwartete SDK-Konfiguration")
print(f"{args.apk.name}: 11 Systemrechte und ein app-eigenes Signaturrecht, kein INTERNET/READ_SMS/SEND_SMS, Backup deaktiviert, SMS-Receiver geschützt, minSdk 31 / targetSdk 37.")
print("Dies ist eine Artefaktprüfung, kein Nachweis für hörbaren Alarm oder echte SMS-Zustellung.")

if re.search(r"android:debuggable[^\n]*=(?:true|\(type 0x12\)0xffffffff)", manifest):
    raise SystemExit("Release-APK darf nicht debuggable sein")
