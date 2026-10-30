#!/usr/bin/env python3
"""Build an offline release-mode test APK with a persistent, local test certificate."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse, hashlib, json, os, secrets, shutil, subprocess
from pathlib import Path
args=argparse.ArgumentParser();args.add_argument('--output-dir',type=Path,default=Path('dist'));args=args.parse_args()
root=Path(__file__).resolve().parents[1]
env=os.environ.copy()
env.setdefault('JAVA_HOME', str(root/'.tools/jdk'))
env.setdefault('ANDROID_HOME', str(root/'.tools/android-sdk'))
env.setdefault('ANDROID_SDK_ROOT', env['ANDROID_HOME'])
env.setdefault('GRADLE_USER_HOME', str(root/'.tools/gradle'))
env.setdefault('PUB_CACHE', str(root/'.tools/pub-cache'))
env['PATH']=str(Path(env['JAVA_HOME'])/'bin')+os.pathsep+env.get('PATH','')
flutter=env.get('FLUTTER_BIN',str(root/'.tools/flutter/bin/flutter'))
folder=root/'.tools/signing';folder.mkdir(parents=True,exist_ok=True);folder.chmod(0o700)
properties=folder/'signing.properties'
if not properties.exists():
    key=folder/'sms-alert-test.jks'
    if key.exists():raise SystemExit('Existing key without properties: restore its credentials instead of replacing it.')
    password=secrets.token_urlsafe(32);env['SMS_ALERT_KEY_PASSWORD']=password
    subprocess.run([str(Path(env['JAVA_HOME'])/'bin/keytool'),'-genkeypair','-noprompt','-keystore',str(key),'-alias','sms-alert-test','-keyalg','RSA','-keysize','3072','-validity','3650','-dname','CN=Snadretto SMS-Alert Test, O=SMS-Alert','-storepass:env','SMS_ALERT_KEY_PASSWORD','-keypass:env','SMS_ALERT_KEY_PASSWORD'],env=env,check=True)
    key.chmod(0o600)
    properties.write_text(f'storeFile={key}\nstorePassword={password}\nkeyAlias=sms-alert-test\nkeyPassword={password}\n');properties.chmod(0o600)
env['SMS_ALERT_SIGNING_PROPERTIES']=str(properties)
subprocess.run([flutter,'build','apk','--release','--target-platform','android-arm64,android-arm,android-x64'],cwd=root/'app',env=env,check=True)
dist=args.output_dir.resolve();dist.mkdir(parents=True,exist_ok=True)
version=(root/'app/pubspec.yaml').read_text().split('version: ')[1].splitlines()[0].split('+')[0]
target=dist/f'sms-alert-{version}-test.apk';shutil.copy2(root/'app/build/app/outputs/flutter-apk/app-release.apk',target)
subprocess.run(['python3',str(root/'tools/check_apk.py'),str(target),'--aapt2',str(Path(env['ANDROID_HOME'])/'build-tools/36.0.0/aapt2')],check=True)
subprocess.run([str(Path(env['ANDROID_HOME'])/'build-tools/36.0.0/apksigner'),'verify','--verbose',str(target)],env=env,check=True)
(dist/'SHA256SUMS').write_text(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n')
print('Test APK: '+str(target))
