#!/usr/bin/env python3
"""Generate the project's own alarm asset; no downloaded sound or runtime synthesis."""
# Copyright 2026 Snadretto. SPDX-License-Identifier: Apache-2.0
import math
from pathlib import Path
import struct
import wave

destination = Path(__file__).resolve().parents[1] / "app/src/main/res/raw/alarm.wav"
destination.parent.mkdir(parents=True, exist_ok=True)
sample_rate = 22050
with wave.open(str(destination), "wb") as output:
    output.setparams((1, 2, sample_rate, 0, "NONE", "not compressed"))
    samples = bytearray()
    for index in range(sample_rate * 2):
        time = index / sample_rate
        position = time % 1
        envelope = max(0, min(1, position / 0.01, (0.65 - position) / 0.01))
        signal = (math.sin(2 * math.pi * 660 * time) + math.sin(2 * math.pi * 880 * time)) / 2
        samples.extend(struct.pack("<h", int(32767 * 0.3 * envelope * signal)))
    output.writeframes(samples)
print(destination)
