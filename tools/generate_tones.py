#!/usr/bin/env python3
"""Project-owned PCM tones; deterministic, offline, no sound-library license."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import math,struct,wave
from pathlib import Path
folder=Path(__file__).resolve().parents[1]/'app/android/app/src/main/res/raw';folder.mkdir(parents=True,exist_ok=True)
rate=22050
for name in ('beacon','pulse'):
    data=bytearray()
    for i in range(rate*2):
        t=i/rate;p=t%1
        envelope=max(0,min(1,p/.01,(.65-p)/.01))
        if name=='beacon':signal=(math.sin(2*math.pi*660*t)+math.sin(2*math.pi*880*t))/2
        else:signal=math.sin(2*math.pi*(660 if p<.325 else 880)*t)
        data.extend(struct.pack('<h',int(32767*.3*envelope*signal)))
    with wave.open(str(folder/(name+'.wav')),'wb') as output:
        output.setparams((1,2,rate,0,'NONE','not compressed'));output.writeframes(data)
