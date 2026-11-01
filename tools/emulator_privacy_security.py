#!/usr/bin/env python3
"""Observe a release candidate from a separate unprivileged app and with strace.
Requires a rooted AOSP emulator and a previously configured SMS-Alert smoke-test state.
Never installs anything on a physical phone; no SMS data is changed from the host.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import argparse,hashlib,json,os,re,subprocess,tempfile,time,sqlite3
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--probe',required=True,type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
if not a.serial.startswith('emulator-'):raise SystemExit('Emulator only')
P='ch.snadretto.smsalert';Q=P+'.probe'
def adb(*cmd,binary=False,check=True):
 r=subprocess.run([a.adb,'-s',a.serial,*cmd],capture_output=True,timeout=40,check=check)
 return r.stdout if binary else (r.stdout+r.stderr).decode(errors='replace').strip()
def state():
 data=adb('exec-out','cat',f'/data/user/0/{P}/databases/alerts.db',binary=True)
 with tempfile.NamedTemporaryFile() as file:
  file.write(data);file.flush()
  with sqlite3.connect(file.name) as db:return json.loads(db.execute('SELECT payload FROM settings').fetchone()[0]),db.execute("SELECT count(*) FROM events WHERE status='PENDING'").fetchone()[0]
assert adb('shell','getprop','ro.kernel.qemu')=='1' and adb('shell','id','-u')=='0'
assert state()[0]['enabled'],'Activate the configured test app through its UI before starting'
adb('install','-r',str(a.probe.resolve()))
# Root attaches to app threads, but app keeps its original UID and permissions.
pid=adb('shell','pidof',P);assert pid.isdecimal()
trace='/data/local/tmp/sms-alert-network.trace'
process=subprocess.Popen([a.adb,'-s',a.serial,'shell','strace','-f','-e','trace=network,ioctl','-s','256','-p',pid,'-o',trace],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
time.sleep(1)
try:
 before=state()
 adb('shell','am','start','-W','-n',Q+'/.ProbeActivity');time.sleep(2)
 probe=json.loads(adb('shell','run-as',Q,'cat','files/result.json'))
 assert probe['protectedSmsRejected'] is True and probe['pauseIntentSent'] is True
 assert state()==before,'Foreign pause intent modified target state'
 # Include a genuine emulator SMS through the protected system delivery path during tracing.
 adb('emu','sms','send','15551234567','ALARM_PRIVACY_SECURITY_TRACE')
 end=time.monotonic()+15
 while time.monotonic()<end and state()[1]==before[1]:time.sleep(.3)
 assert state()[1]==before[1]+1
 adb('shell','am','start','-W','-n',P+'/.MainActivity');time.sleep(2)
finally:
 tracer=adb('shell','pidof','strace',check=False)
 if tracer and all(part.isdecimal() for part in tracer.split()):adb('shell','kill','-INT',*tracer.split())
 try:process.communicate(timeout=10)
 except subprocess.TimeoutExpired:process.terminate();process.communicate(timeout=5)
raw=adb('shell','cat',trace)
assert raw.strip(),'No strace evidence captured'
# Binder and local AF_UNIX traffic are ordinary Android IPC, not external IP connections.
network=[line for line in raw.splitlines() if re.search(r'\b(?:AF_INET6?|sin6?_family=AF_INET6?)\b',line)]
assert not network,'IP socket attempt observed: '+str(network[:5])
result={'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'platform_api':adb('shell','getprop','ro.build.version.sdk'),'target_uid':adb('shell','stat','-c','%u',f'/data/user/0/{P}'),'probe_uid':probe['uid'],'cases':[{'case':'protected_sms_intent_rejected_for_unprivileged_installed_app','status':'PASS'},{'case':'private_pause_intent_does_not_change_target_state','status':'PASS'},{'case':'attached_network_syscalls_during_ui_and_synthetic_sms','status':'PASS','ip_socket_attempts':len(network),'trace_lines':len(raw.splitlines())}],'limits':'Trace covers already running app threads, UI resume and synthetic SMS, not every possible cold-start path. IPC via Binder/AF_UNIX is allowed. Packaged release lacks INTERNET independently.'}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');a.output.with_suffix('.trace').write_text(raw)
print(json.dumps(result,ensure_ascii=False,indent=2))
