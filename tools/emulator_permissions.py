#!/usr/bin/env python3
"""Real permission dialogs and installer restriction; no pm grant is used.
Only an explicitly reset AOSP emulator is accepted. Test data for SMS-Alert is erased.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse, hashlib, json, re, subprocess, time
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--reset',action='store_true');p.add_argument('--output',required=True,type=Path);a=p.parse_args()
P='ch.snadretto.smsalert';results=[]
if not a.reset or not a.serial.startswith('emulator-'):raise SystemExit('Explicit emulator and --reset required')
def adb(*cmd,check=True,timeout=45):
 r=subprocess.run([a.adb,'-s',a.serial,*cmd],check=check,capture_output=True,text=True,timeout=timeout)
 return (r.stdout+r.stderr).strip()
def nodes():
 for _ in range(4):
  out=adb('shell','uiautomator','dump','/data/local/tmp/permissions-ui.xml')
  if 'dumped to:' in out:return list(ET.fromstring(adb('shell','cat','/data/local/tmp/permissions-ui.xml')).iter('node'))
  adb('shell','input','keyevent','KEYCODE_WAKEUP');time.sleep(.4)
 raise RuntimeError('No fresh UI: '+out)
def label(n):return n.attrib.get('text') or n.attrib.get('content-desc','')
def tap(n):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.35)
def button(text=None,resource=None,scroll=True):
 for _ in range(7 if scroll else 2):
  for n in nodes():
   match=label(n).split('\n')[0].casefold()==text.casefold() if text is not None else resource in n.attrib.get('resource-id','')
   if match and n.attrib.get('enabled')=='true':tap(n);return
  if scroll:adb('shell','input','swipe','540','1700','540','700','200')
  else:time.sleep(.4)
 raise RuntimeError('Control not found: '+str(text or resource))
def text_visible(part):return any(part in label(n) for n in nodes())
def wait(f,seconds=15):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if f():return
  time.sleep(.3)
 raise RuntimeError('Condition timed out: '+str(f))
def sms_granted():
 data=adb('shell','dumpsys','package',P)
 return re.search(r'android\.permission\.RECEIVE_SMS: granted=true',data) is not None
def openapp():
 adb('shell','cmd','statusbar','collapse');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','input','keyevent','82')
 adb('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER','-n',P+'/.MainActivity')
def install(restricted=False):
 adb('uninstall',P,check=False)
 adb('install',*(['--restrict-permissions'] if restricted else []),str(a.apk.resolve()),timeout=90)
 openapp();button('Einrichtung');wait(lambda:text_visible('SMS empfangen'))
def record(case,**kwargs):
 results.append({'case':case,'status':'PASS',**kwargs});print('PASS: '+case,flush=True)
try:
 assert adb('shell','getprop','ro.kernel.qemu')=='1'
 adb('shell','settings','put','system','screen_off_timeout','2147483647')
 install()
 assert not sms_granted()
 button('SMS-Zugriff anfragen');button(resource=':id/permission_deny',scroll=False)
 wait(lambda:text_visible('SMS-Zugriff wurde nicht freigegeben.'));assert not sms_granted();record('sms_dialog_denial_reported_and_check_stays_missing')
 button('SMS-Zugriff anfragen');button(resource=':id/permission_deny',scroll=False)
 wait(lambda:text_visible('SMS-Zugriff wurde nicht freigegeben.'))
 button('SMS-Zugriff anfragen');time.sleep(1)
 assert not any('permission_allow_button' in n.attrib.get('resource-id','') for n in nodes())
 assert text_visible('SMS-Zugriff wurde nicht freigegeben.') and not sms_granted();record('permanent_denial_without_dialog_has_recovery_message')
 button('App-Berechtigungen öffnen')
 assert any(n.attrib.get('package')=='com.android.settings' for n in nodes());record('app_permission_settings_open_from_sms_card')
 button('Permissions');button('SMS');button('Allow',scroll=False);wait(sms_granted)
 # Return through the Settings stack just as a user does after granting access.
 for _ in range(3):adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.3)
 openapp();button('Einrichtung');wait(lambda:text_visible('SMS-Empfang ist freigegeben.'))
 assert not text_visible('SMS-Zugriff anfragen');record('manual_settings_grant_refreshes_sms_check')
 install();button('SMS-Zugriff anfragen');button(resource=':id/permission_allow_button',scroll=False);wait(sms_granted)
 wait(lambda:text_visible('SMS-Empfang ist freigegeben.'));record('first_sms_dialog_allow_refreshes_check_without_notification_request')
 install(restricted=True);button('SMS-Zugriff anfragen')
 wait(lambda:text_visible('SMS-Zugriff wurde nicht freigegeben.'));assert not sms_granted()
 raw=adb('shell','dumpsys','package',P)
 flags=next((line.strip() for line in raw.splitlines() if 'android.permission.RECEIVE_SMS: granted=' in line),'')
 assert 'APPLY_RESTRICTION' in flags
 assert text_visible('App-Berechtigungen öffnen');record('installer_restricted_sms_is_not_reported_as_granted',permission_flags=flags)
except Exception as error:
 results.append({'case':'permission_flow_failure','status':'FAIL','error':str(error),'visible_controls':[label(n) for n in nodes() if label(n)]});raise
finally:
 a.output.parent.mkdir(parents=True,exist_ok=True)
 a.output.write_text(json.dumps({'timestamp_utc':datetime.now(timezone.utc).isoformat(),'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'platform_api':adb('shell','getprop','ro.build.version.sdk',check=False),'scope':'AOSP emulator, real permission-controller dialogs and real install --restrict-permissions. No pm grant used. No claim about the user phone installer or OEM.','results':results},ensure_ascii=False,indent=2)+'\n')
 # Restore a normal, fresh, paused test installation for subsequent emulator work.
 if adb('shell','getprop','ro.kernel.qemu',check=False)=='1':
  adb('uninstall',P,check=False);adb('install',str(a.apk.resolve()),timeout=90)
