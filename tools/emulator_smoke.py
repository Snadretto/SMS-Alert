#!/usr/bin/env python3
"""Release APK black-box smoke test. Synthetic data on an explicitly reset AOSP emulator only.
Root is used solely for read-only state verification / process-death injection, never to invoke
private release APIs, seed settings, bypass audio hardening, or simulate successful receipt.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse, hashlib, json, re, sqlite3, subprocess, tempfile, time
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET
P='ch.snadretto.smsalert'
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--reset',action='store_true');p.add_argument('--soak-seconds',type=int,default=0);p.add_argument('--output',type=Path,default=Path('app/build/reports/emulator-smoke.json'));a=p.parse_args()
if not a.serial.startswith('emulator-') or not a.reset:raise SystemExit('Explicit emulator serial and --reset required.')
results=[]
platform='Android emulator'
def adb(*cmd,check=True,binary=False,timeout=40):
 r=subprocess.run([a.adb,'-s',a.serial,*cmd],check=check,capture_output=True,timeout=timeout)
 return r.stdout if binary else (r.stdout+r.stderr).decode(errors='replace').strip()
def wait(pred,timeout=18):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  value=pred()
  if value:return value
  time.sleep(.3)
 raise RuntimeError('Timed out: '+getattr(pred,'__name__','condition'))
def record(case,**kw):
 results.append({'case':case,'status':'PASS',**kw});print('PASS: '+case,flush=True);save()
def save():
 a.output.parent.mkdir(parents=True,exist_ok=True)
 a.output.write_text(json.dumps({'timestamp_utc':datetime.now(timezone.utc).isoformat(),'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'platform':platform,'scope':'Synthetic SMS, release-mode APK; no real carrier, physical vibration, audibility or OEM proof. IP interfaces disabled.','results':results},ensure_ascii=False,indent=2)+'\n')
def db():
 data=adb('exec-out','cat',f'/data/user/0/{P}/databases/alerts.db',binary=True)
 with tempfile.NamedTemporaryFile() as file:
  file.write(data);file.flush()
  with sqlite3.connect(file.name) as conn:
   return {'settings':json.loads(conn.execute('SELECT payload FROM settings').fetchone()[0]),'rules':[json.loads(r[0]) for r in conn.execute('SELECT payload FROM rules')],'events':[dict(json.loads(r[0]),status=r[1]) for r in conn.execute('SELECT payload,status FROM events ORDER BY rowid')],'meta':dict(conn.execute('SELECT key,value FROM meta'))}
def pending():return [e for e in db()['events'] if e['status']=='PENDING']
def foreground():return 'isForeground=true' in adb('shell','dumpsys','activity','services',P)
def playing():return db()['meta'].get('lastOutcome')=='playback_started' and foreground()
def nodes():
 adb('shell','uiautomator','dump','/data/local/tmp/sms-alert-ui.xml')
 return list(ET.fromstring(adb('shell','cat','/data/local/tmp/sms-alert-ui.xml')).iter('node'))
def tap(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.2)
def label(n):return n.attrib.get('text') or n.attrib.get('content-desc','')
def swipe(up=True):adb('shell','input','swipe','540','1700' if up else '600','540','650' if up else '1800','250')
def button(text,top=False):
 if top:
  for _ in range(3):swipe(False)
 for _ in range(9):
  for n in nodes():
   if label(n).split('\n')[0]==text and n.attrib.get('enabled')=='true':tap(n);return
  swipe()
 raise RuntimeError('Button not found: '+text)
def field(identifier,value):
 for _ in range(6):
  for n in nodes():
   if n.attrib.get('resource-id')==identifier:
    tap(n);adb('shell','input','text',value);adb('shell','input','keyevent','KEYCODE_BACK');return
  swipe()
 raise RuntimeError('Field not found: '+identifier)
def openapp():
 adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','input','keyevent','82');adb('shell','am','start','-W','-n',P+'/.MainActivity')
def killprocess():
 pid=adb('shell','pidof',P,check=False)
 if pid:
  assert pid.isdecimal();adb('shell','kill','-9',pid)
 wait(lambda:not adb('shell','pidof',P,check=False))
def sms(body,sender='15551234567'):adb('emu','sms','send',sender,body)
try:
 assert adb('shell','getprop','ro.kernel.qemu')=='1'
 assert adb('shell','id','-u')=='0','AOSP adb root required for read-only verification'
 adb('install','-r',str(a.apk.resolve()),timeout=90);adb('shell','pm','clear',P)
 api=int(adb('shell','getprop','ro.build.version.sdk'));platform=f'Android API {api} / AOSP x86_64 emulator'
 for right in (('RECEIVE_SMS','POST_NOTIFICATIONS') if api>=33 else ('RECEIVE_SMS',)):adb('shell','pm','grant',P,'android.permission.'+right)
 adb('shell','cmd','appops','set',P,'SCHEDULE_EXACT_ALARM','allow')
 if api>=34:adb('shell','cmd','appops','set',P,'USE_FULL_SCREEN_INTENT','deny')
 adb('shell','svc','wifi','disable');adb('shell','svc','data','disable');openapp();wait(lambda:db())
 assert not db()['settings']['enabled'];record('fresh_install_paused')
 button('Erste Regel anlegen');field('rule_name','Pikett');field('rule_sender','15551234567');field('rule_text','ALARM');button('Regel speichern')
 wait(lambda:len(db()['rules'])==1);record('rule_created_through_flutter_ui')
 button('Alarmierung aktivieren',top=True);wait(lambda:db()['settings']['enabled'])
 before=db();sms('ALARM_WRONG_SENDER','15559999999');time.sleep(2);assert db()==before;record('non_matching_sms_ignored')
 button('Testalarm starten');wait(playing);assert pending()[0]['source']=='TEST';button('Diesen Alarm quittieren',top=True);wait(lambda:not pending());wait(lambda:not foreground());record('local_test_and_acknowledgement')
 adb('shell','input','keyevent','KEYCODE_HOME');killprocess();adb('shell','input','keyevent','KEYCODE_SLEEP')
 secret='ALARM_PRIVATE_BODY_RELEASE';sms(secret);wait(playing);assert pending()[0]['source']=='SMS';assert secret not in json.dumps(db());latency=int(db()['meta']['lastLatencyMs']);assert latency<=5000
 record('cold_process_sms_screen_off_starts_native_alarm',latency_ms=latency)
 sms('ALARM_SECOND_NEW_EVENT');wait(lambda:len(pending())==2);openapp();first=pending()[0]['id'];button('Diesen Alarm quittieren',top=True);wait(lambda:len(pending())==1);assert pending()[0]['id']!=first;wait(playing);record('two_events_individual_ack_preserves_second')
 if a.soak_seconds:
  adb('shell','input','keyevent','KEYCODE_HOME');adb('shell','input','keyevent','KEYCODE_SLEEP');started=time.monotonic();samples=0
  while time.monotonic()-started<a.soak_seconds:
   assert len(pending())==1 and playing();samples+=1
   print('SOAK sample '+str(samples),flush=True);time.sleep(min(30,a.soak_seconds-(time.monotonic()-started)))
  record('continuous_alarm_soak',duration_seconds=round(time.monotonic()-started),samples=samples,physical_audio_verified=False);openapp()
 button('App pausieren',top=True);button('Pausieren');wait(lambda:not db()['settings']['enabled']);wait(lambda:not foreground());assert not pending()
 paused=db();sms('ALARM_WHILE_PAUSED');time.sleep(2);assert db()==paused;record('pause_resolves_queue_and_paused_sms_not_stored')
 adb('install','-r',str(a.apk.resolve()),timeout=90);time.sleep(2);assert not db()['settings']['enabled'];assert len(db()['rules'])==1;record('signed_update_preserves_rule_and_pause')
 openapp();button('Alarmierung aktivieren',top=True);wait(lambda:db()['settings']['enabled']);assert not pending()
 adb('shell','am','force-stop',P);sms('ALARM_FORCE_STOPPED');time.sleep(2);assert not pending();assert not adb('shell','pidof',P,check=False);record('force_stop_negative_case_no_false_delivery')
 openapp();button('App pausieren',top=True);button('Pausieren');wait(lambda:not db()['settings']['enabled'])
 # Component export and permission protection are audited in the packaged manifest.
 # A shell UID is privileged; it is not a valid proxy for an untrusted installed app.
 pid=adb('shell','pidof',P);log=adb('logcat','-d','-v','brief','--pid',pid);assert secret not in log;record('received_sms_body_absent_from_app_storage_and_logcat')
except Exception as error:
 results.append({'case':'smoke_test_failure','status':'FAIL','error':str(error)});save();raise
finally:save()
