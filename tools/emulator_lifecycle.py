#!/usr/bin/env python3
"""Notification acknowledgement, repeat SMS, actual reboot, update, and revoked permission.
Explicit rooted AOSP emulator with smoke-test rule required; no physical devices.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse,hashlib,json,re,sqlite3,subprocess,tempfile,time
from pathlib import Path
import xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();P='ch.snadretto.smsalert';results=[]
if not a.serial.startswith('emulator-'):raise SystemExit('Emulator only')
def adb(*cmd,check=True,binary=False,timeout=40):
 r=subprocess.run([a.adb,'-s',a.serial,*cmd],check=check,capture_output=True,timeout=timeout)
 return r.stdout if binary else (r.stdout+r.stderr).decode(errors='replace').strip()
def state():
 data=adb('exec-out','cat',f'/data/user/0/{P}/databases/alerts.db',binary=True)
 with tempfile.NamedTemporaryFile() as file:
  file.write(data);file.flush()
  with sqlite3.connect(file.name) as db:return {'settings':json.loads(db.execute('SELECT payload FROM settings').fetchone()[0]),'pending':db.execute("SELECT count(*) FROM events WHERE status='PENDING'").fetchone()[0],'meta':dict(db.execute('SELECT key,value FROM meta'))}
def wait(f,timeout=20):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  try:
   if f():return
  except subprocess.CalledProcessError:pass
  time.sleep(.4)
 raise RuntimeError('Timed out: '+str(f))
def foreground():return 'isForeground=true' in adb('shell','dumpsys','activity','services',P)
def nodes():
 for attempt in range(4):
  output=adb('shell','uiautomator','dump','/data/local/tmp/lifecycle-ui.xml')
  if 'dumped to:' in output:return list(ET.fromstring(adb('shell','cat','/data/local/tmp/lifecycle-ui.xml')).iter('node'))
  adb('shell','input','keyevent','KEYCODE_WAKEUP');time.sleep(.5)
 raise RuntimeError('No fresh UI hierarchy: '+output)
def button(label,top=False):
 if top:
  for _ in range(3):adb('shell','input','swipe','540','600','540','1800','200')
 for _ in range(8):
  for n in nodes():
   text=n.attrib.get('text') or n.attrib.get('content-desc','')
   if text.split('\n')[0].casefold()==label.casefold() and n.attrib.get('enabled')=='true':
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.2);return
  if label in ('Alle quittieren', 'Offenen Alarm fortsetzen', 'Übersicht', 'Einrichtung'):openapp()
  else:adb('shell','input','swipe','540','1700','540','650','200')
 raise RuntimeError('Button not found: '+label)
def openapp():
 adb('shell','cmd','statusbar','collapse')
 adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','input','keyevent','82');adb('shell','am','start','-W','-n',P+'/.MainActivity')
def sms(text):adb('emu','sms','send','15551234567',text)
def record(case):results.append({'case':case,'status':'PASS'});print('PASS: '+case,flush=True)
def reboot():
 adb('reboot');time.sleep(2);wait(lambda:adb('shell','getprop','sys.boot_completed',check=False).strip()=='1',120)
 adb('root');time.sleep(1);adb('wait-for-device');adb('shell','input','keyevent','82');time.sleep(2)
try:
 assert adb('shell','getprop','ro.kernel.qemu')=='1' and adb('shell','id','-u')=='0'
 assert state()['settings']['enabled'] and state()['pending']==1
 adb('shell','cmd','statusbar','expand-notifications');button('Diesen Alarm quittieren');wait(lambda:state()['pending']==0);wait(lambda:not foreground());record('notification_action_acknowledges_and_stops_service')
 openapp();sms('ALARM_IDENTICAL_REPEAT');wait(lambda:state()['pending']==1);time.sleep(2.1);sms('ALARM_IDENTICAL_REPEAT');wait(lambda:state()['pending']==2);record('new_sms_with_identical_text_is_not_body_cooldown')
 openapp();button('Alle quittieren',top=True);button('Alle quittieren');wait(lambda:state()['pending']==0);wait(lambda:not foreground())
 sms('ALARM_PENDING_BEFORE_REBOOT');wait(lambda:state()['pending']==1 and foreground());reboot()
 assert state()['settings']['enabled'] and state()['pending']==1
 wait(lambda:state()['meta'].get('lastOutcome')=='device_restarted');assert not foreground();record('active_reboot_preserves_pending_without_boot_media_service')
 adb('install','-r',str(a.apk.resolve()),timeout=90);time.sleep(2);assert state()['settings']['enabled'] and state()['pending']==1;assert not foreground();record('active_update_preserves_unacknowledged_event')
 openapp();button('Offenen Alarm fortsetzen',top=True);wait(foreground);button('Diesen Alarm quittieren',top=True);wait(lambda:state()['pending']==0);wait(lambda:not foreground());record('manual_resume_after_boot_and_ack_no_resurrection')
 button('App pausieren',top=True);button('Pausieren');wait(lambda:not state()['settings']['enabled']);reboot();assert not state()['settings']['enabled'] and state()['pending']==0 and not foreground();record('paused_reboot_does_not_reactivate')
 openapp();button('Alarmierung aktivieren',top=True);wait(lambda:state()['settings']['enabled'])
 adb('shell','pm','revoke',P,'android.permission.RECEIVE_SMS');sms('ALARM_REVOKED_SMS_PERMISSION');time.sleep(2);assert state()['pending']==0
 openapp();button('Einrichtung');assert any('SMS empfangen' in (n.attrib.get('text') or n.attrib.get('content-desc','')) for n in nodes());record('revoked_sms_permission_blocks_receive_without_changing_desired_activation')
 adb('shell','pm','grant',P,'android.permission.RECEIVE_SMS');sms('ALARM_RESTORED_SMS_PERMISSION');wait(lambda:state()['pending']==1 and foreground())
 button('Übersicht');button('App pausieren',top=True);button('Pausieren');wait(lambda:state()['pending']==0 and not state()['settings']['enabled']);wait(lambda:not foreground());record('restored_permission_accepts_new_sms_pause_still_stops')
except Exception as error:
 results.append({'case':'lifecycle_test_failure','status':'FAIL','error':str(error)});raise
finally:
 a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps({'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'scope':'Rooted AOSP emulator with synthetic SMS, no physical-device proof; reboot has no secure PIN.','results':results},ensure_ascii=False,indent=2)+'\n')
