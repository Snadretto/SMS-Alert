#!/usr/bin/env python3
"""Install the pinned official NDK; unzip supports its internal symlinks.
Android CLI 1.1's NDK extraction rejects those valid links. Other SDK packages
can be installed normally with sdkmanager. Only Google's checksum-verified archive is used.
"""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import hashlib,json,os,subprocess,urllib.request,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
version=json.loads((root/'toolchain.lock.json').read_text())['android_ndk']
sdk=Path(os.environ.get('ANDROID_HOME',root/'.tools/android-sdk'))
dest=sdk/'ndk'/version
if (dest/'source.properties').exists():raise SystemExit(0)
with urllib.request.urlopen('https://dl.google.com/android/repository/repository2-3.xml',timeout=60) as source:tree=ET.fromstring(source.read())
p=next(p for p in tree.findall('remotePackage') if p.attrib['path']=='ndk;'+version)
a=next(a for a in p.findall('./archives/archive') if a.findtext('host-os')=='linux').find('complete')
folder=root/'.tools/ndk-install';folder.mkdir(parents=True,exist_ok=True)
archive=folder/'ndk.zip'
with urllib.request.urlopen('https://dl.google.com/android/repository/'+a.findtext('url'),timeout=90) as source,archive.open('wb') as output:
    while chunk:=source.read(2**20):output.write(chunk)
with archive.open('rb') as source:digest=hashlib.file_digest(source,'sha1').hexdigest()
if digest!=a.findtext('checksum'):raise SystemExit('NDK checksum mismatch')
subprocess.run(['unzip','-q',str(archive),'-d',str(folder)],check=True)
unpacked=next(d for d in folder.iterdir() if d.is_dir() and d.name.startswith('android-ndk-'))
dest.parent.mkdir(parents=True,exist_ok=True);unpacked.rename(dest)
print('Installed checksum-verified official NDK '+version)
