#!/usr/bin/env python3
"""Install the checksum-pinned Linux Flutter SDK in the ignored workspace tool directory."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import hashlib,json,tarfile,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1];lock=json.loads((root/'toolchain.lock.json').read_text())['flutter']
folder=root/'.tools';folder.mkdir(exist_ok=True);target=folder/'flutter'
if (target/'bin/flutter').exists():
    version=json.loads((target/'bin/cache/flutter.version.json').read_text())['frameworkVersion']
    if version!=lock['version']:raise SystemExit('Existing Flutter SDK does not match toolchain.lock.json')
    print(target);raise SystemExit(0)
archive=folder/'flutter-sdk.tar.xz'
with urllib.request.urlopen(lock['base_url']+'/'+lock['archive'],timeout=90) as source,archive.open('wb') as output:
    while chunk:=source.read(2**20):output.write(chunk)
with archive.open('rb') as source:
    digest=hashlib.file_digest(source,'sha256').hexdigest()
if digest!=lock['sha256']:raise SystemExit('Flutter archive checksum mismatch')
with tarfile.open(archive) as package:package.extractall(folder,filter='data')
print(target)
