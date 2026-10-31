#!/usr/bin/env python3
"""Real system picker, saved local audio, cold SMS, fallback and guided app-info.
Only an explicitly reset AOSP emulator is accepted. Root is used for read-only
state checks/process death plus an explicit missing-file fault injection.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse,hashlib,json,re,sqlite3,subprocess,tempfile,time
from datetime import datetime,timezone
from pathlib import Path
import xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--reset',action='store_true');p.add_argument('--output',required=True,type=Path);a=p.parse_args();P='ch.snadretto.smsalert';results=[]
if not a.reset or not a.serial.startswith('emulator-'):raise SystemExit('Explicit emulator and --reset required')
def adb(*cmd,check=True,binary=False,timeout=40):
 r=subprocess.run([a.adb,'-s',a.serial,*cmd],check=check,capture_output=True,timeout=timeout)
 return r.stdout if binary else (r.stdout+r.stderr).decode(errors='replace').strip()
def nodes():
 for _ in range(4):
  out=adb('shell','uiautomator','dump','/data/local/tmp/tones-ui.xml')
  if 'dumped to:' in out:return list(ET.fromstring(adb('shell','cat','/data/local/tmp/tones-ui.xml')).iter('node'))
  adb('shell','input','keyevent','KEYCODE_WAKEUP');time.sleep(.4)
 raise RuntimeError('No fresh UI: '+out)
def label(n):return n.attrib.get('text') or n.attrib.get('content-desc','')
def tap(n):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3)
def swipe(up=True):adb('shell','input','swipe','540','1700' if up else '600','540','650' if up else '1800','250')
def button(text,top=False):
 if top:
  for _ in range(3):swipe(False)
 for _ in range(10):
  for n in nodes():
   if label(n).split('\n')[0].casefold()==text.casefold() and n.attrib.get('enabled')=='true':tap(n);return
  swipe()
 raise RuntimeError('Control not found: '+text)
def field(identifier,text):
 for _ in range(7):
  for n in nodes():
   if n.attrib.get('resource-id')==identifier:
    tap(n);adb('shell','input','text',text);adb('shell','input','keyevent','KEYCODE_BACK');return
  swipe()
 raise RuntimeError(identifier)
def db():
 raw=adb('exec-out','cat',f'/data/user/0/{P}/databases/alerts.db',binary=True)
 with tempfile.NamedTemporaryFile() as f:
  f.write(raw);f.flush()
  with sqlite3.connect(f.name) as conn:
   return {'settings':json.loads(conn.execute('SELECT payload FROM settings').fetchone()[0]),'pending':conn.execute("SELECT count(*) FROM events WHERE status='PENDING'").fetchone()[0],'meta':dict(conn.execute('SELECT key,value FROM meta'))}
def wait(f,timeout=20):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  if f():return
  time.sleep(.3)
 raise RuntimeError('Condition timed out: '+str(f))
def openapp():
 adb('shell','cmd','statusbar','collapse');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','input','keyevent','82');adb('shell','am','start','-W','--activity-clear-top','-n',P+'/.MainActivity')
def foreground():return 'isForeground=true' in adb('shell','dumpsys','activity','services',P)
def playing():return db()['meta'].get('lastOutcome')=='playback_started' and foreground()
def sms_permission():return re.search(r'android\.permission\.RECEIVE_SMS: granted=true',adb('shell','dumpsys','package',P)) is not None
def record(case,**details):results.append({'case':case,'status':'PASS',**details});print('PASS: '+case,flush=True)
def pick(index=0):
 button('Klingelton vom Smartphone auswählen',top=True)
 choices=[n for n in nodes() if n.attrib.get('class')=='android.widget.CheckedTextView' and label(n) and not label(n).lower().startswith(('default','silent','none'))]
 assert choices,'No system sound rows'
 chosen=label(choices[index]);tap(choices[index]);button('OK');wait(lambda:bool([n for n in nodes() if label(n).startswith('Ausgewählt: ')]))
 return chosen
try:
 assert adb('shell','getprop','ro.kernel.qemu')=='1' and adb('shell','id','-u')=='0'
 adb('install','-r',str(a.apk.resolve()),timeout=90);adb('shell','pm','clear',P);adb('shell','settings','put','system','screen_off_timeout','2147483647');openapp();wait(lambda:db())
 api=int(adb('shell','getprop','ro.build.version.sdk'))
 button('Einrichtung');button('Hilfe bei eingeschränkten Einstellungen')
 assert any('Oben rechts' in label(n) for n in nodes())
 assert not sms_permission();button('App-Info öffnen');wait(lambda:any(n.attrib.get('package')=='com.android.settings' for n in nodes()));assert not sms_permission()
 record('highlighted_restricted_help_opens_app_info_without_grant')
 adb('shell','input','keyevent','KEYCODE_BACK');openapp()
 for permission in (('RECEIVE_SMS','POST_NOTIFICATIONS') if api>=33 else ('RECEIVE_SMS',)):adb('shell','pm','grant',P,'android.permission.'+permission)
 adb('shell','cmd','appops','set',P,'SCHEDULE_EXACT_ALARM','allow')
 if api>=34:adb('shell','cmd','appops','set',P,'USE_FULL_SCREEN_INTENT','deny')
 adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
 button('Übersicht');button('Erste Regel anlegen');field('rule_name','ToneTest');field('rule_sender','15551234567');field('rule_text','ALARM');button('Regel speichern')
 button('Einstellungen');before=db()['settings'];chosen=pick();assert db()['settings']==before
 record('real_system_picker_stages_choice_until_save',selected_label=chosen)
 button('Klingelton vom Smartphone auswählen',top=True);button('CANCEL');assert db()['settings']==before
 assert any(label(n).startswith('Ausgewählt: ') for n in nodes());record('cancel_picker_keeps_previous_draft')
 button('Einstellungen speichern');wait(lambda:db()['settings']['tone']=='device')
 selected=db()['settings']['deviceTone'];assert selected and not db()['settings']['enabled']
 private=f'/data/user/0/{P}/files/alarm-tones/{selected["id"]}.audio'
 raw=adb('exec-out','cat',private,binary=True);assert hashlib.sha256(raw).hexdigest()==selected['id']
 record('selected_audio_copied_privately_and_saved_without_activating',bytes=len(raw),title=selected['title'])
 # Cancel the editor after choosing a different draft: the saved selection must stay unchanged.
 button('Einstellungen');pick(1);adb('shell','input','keyevent','KEYCODE_BACK');assert db()['settings']['deviceTone']==selected
 record('leaving_settings_without_save_preserves_active_choice')
 button('Alarmierung aktivieren',top=True);wait(lambda:db()['settings']['enabled']);button('Testalarm starten');wait(playing)
 assert not db()['meta'].get('audioWarning');button('Diesen Alarm quittieren',top=True);wait(lambda:db()['pending']==0);wait(lambda:not foreground());record('local_alarm_uses_saved_device_audio_and_stops')
 adb('shell','input','keyevent','KEYCODE_HOME');pid=adb('shell','pidof',P);assert pid.isdecimal();adb('shell','kill','-9',pid);wait(lambda:not adb('shell','pidof',P,check=False))
 adb('shell','input','keyevent','KEYCODE_SLEEP');adb('emu','sms','send','15551234567','ALARM_DEVICE_TONE_COLD');wait(playing)
 assert db()['settings']['deviceTone']==selected and not db()['meta'].get('audioWarning');openapp();button('Diesen Alarm quittieren',top=True);wait(lambda:db()['pending']==0);wait(lambda:not foreground());record('cold_sms_plays_private_device_tone_without_picker_grant')
 # Fault injection only on this explicitly reset emulator; there is no production test hook.
 adb('shell','rm',private);button('Testalarm starten',top=True);wait(playing)
 assert db()['meta'].get('audioWarning')=='device_tone_fallback';button('Diesen Alarm quittieren',top=True);wait(lambda:db()['pending']==0);wait(lambda:not foreground());record('missing_saved_audio_falls_back_to_signal')
 button('App pausieren',top=True);button('Pausieren');wait(lambda:not db()['settings']['enabled']);record('pause_stops_signals_after_device_tone_use')
except Exception as error:
 results.append({'case':'ringtone_test_failure','status':'FAIL','error':str(error),'visible_controls':[label(n) for n in nodes() if label(n)]});raise
finally:
 a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps({'timestamp_utc':datetime.now(timezone.utc).isoformat(),'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'platform_api':adb('shell','getprop','ro.build.version.sdk',check=False),'scope':'AOSP emulator, real system picker, private audio copy, synthetic SMS, no host audio/physical audibility proof. Root reads app state and deletes only its selected tone for fault injection. Permission grants are test setup, not performed by app/help.','results':results},ensure_ascii=False,indent=2)+'\n')
