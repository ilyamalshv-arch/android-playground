#!/usr/bin/env python3
"""Synthesize the app's ambient loop and UI sounds (no samples, no licenses).

Requires: pip install numpy scipy soundfile
Writes OGG Vorbis files to app/src/main/res/raw/.
"""
import pathlib
import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve, butter, sosfilt

SR = 44100
OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res/raw"
rng = np.random.default_rng(7)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def reverb_ir(seconds=4.5, decay=3.2):
    n = int(SR * seconds)
    t = np.arange(n) / SR
    ir = np.stack([rng.standard_normal(n), rng.standard_normal(n)]) * np.exp(-decay * t)
    sos = butter(2, 5000, "low", fs=SR, output="sos")
    ir = sosfilt(sos, ir, axis=1)
    return ir / np.abs(ir).max()


def pad_voice(freq, length, detune=0.004):
    t = np.arange(length) / SR
    sig = np.zeros(length)
    for d in (-detune, 0.0, detune):
        f = freq * (1 + d)
        ph = rng.uniform(0, 2 * np.pi)
        sig += np.sin(2 * np.pi * f * t + ph) + 0.18 * np.sin(2 * np.pi * 2 * f * t + ph) + 0.06 * np.sin(2 * np.pi * 3 * f * t)
    trem = 1 + 0.08 * np.sin(2 * np.pi * rng.uniform(0.05, 0.12) * t + rng.uniform(0, 6.28))
    return sig * trem / 3


def env(length, attack, release):
    e = np.ones(length)
    a, r = int(attack * SR), int(release * SR)
    e[:a] = np.sin(np.linspace(0, np.pi / 2, a)) ** 2
    e[-r:] = np.cos(np.linspace(0, np.pi / 2, r)) ** 2
    return e


def bell(freq, seconds=3.0, decay=1.6):
    t = np.arange(int(SR * seconds)) / SR
    partials = [(1, 1.0), (2.0, 0.25), (3.01, 0.08), (4.2, 0.03)]
    s = sum(a * np.sin(2 * np.pi * freq * m * t) * np.exp(-decay * m ** 0.7 * t) for m, a in partials)
    s *= np.minimum(1, t / 0.008)
    return s


def ambient(seconds=64):
    """Slow D-lydian chord pad with sparse bells; the reverb tail wraps so the loop is seamless."""
    n = SR * seconds
    tail = SR * 8
    mix = np.zeros((2, n + tail))
    chords = [[50, 57, 62, 64, 69], [47, 54, 59, 62, 66], [43, 50, 55, 59, 66], [45, 52, 57, 61, 64]]
    seg = n // len(chords)
    for i, chord in enumerate(chords):
        start = i * seg
        length = seg + SR * 6
        e = env(length, 4.5, 5.5)
        for j, note in enumerate(chord):
            v = pad_voice(midi(note), length) * e * (0.20 if note < 55 else 0.12)
            pan = 0.5 + 0.35 * np.sin(j * 1.7)
            mix[0, start:start + length] += v * (1 - pan)
            mix[1, start:start + length] += v * pan
    scale = [69, 71, 74, 76, 78, 81, 83, 86]
    t = 2.0
    while t < seconds - 2:
        b = bell(midi(rng.choice(scale))) * rng.uniform(0.05, 0.09)
        s = int(t * SR)
        pan = rng.uniform(0.2, 0.8)
        mix[0, s:s + len(b)] += b * (1 - pan)
        mix[1, s:s + len(b)] += b * pan
        t += rng.uniform(2.5, 6.0)
    ir = reverb_ir()
    wet = np.stack([fftconvolve(mix[c], ir[c])[: n + tail] for c in range(2)])
    out = 0.55 * mix + 0.45 * wet / np.abs(wet).max() * np.abs(mix).max()
    looped = out[:, :n].copy()
    looped[:, :tail] += out[:, n:n + tail]
    sos = butter(2, 60, "high", fs=SR, output="sos")
    looped = sosfilt(sos, looped, axis=1)
    return (looped / np.abs(looped).max() * 0.5).T


def ui_sound(freqs, seconds, decay, gain):
    t = np.arange(int(SR * seconds)) / SR
    s = np.zeros_like(t)
    for k, (f, delay) in enumerate(freqs):
        d = int(delay * SR)
        tt = t[: len(t) - d]
        s[d:] += np.sin(2 * np.pi * f * tt) * np.exp(-decay * tt) * (0.7 ** k) + 0.15 * np.sin(4 * np.pi * f * tt) * np.exp(-decay * 2 * tt)
    s *= np.minimum(1, t / 0.004)
    fade = int(0.02 * SR)
    s[-fade:] *= np.linspace(1, 0, fade)
    return s / np.abs(s).max() * gain


def write(name, data):
    OUT.mkdir(parents=True, exist_ok=True)
    data = np.ascontiguousarray(data, dtype=np.float32)
    channels = 1 if data.ndim == 1 else data.shape[1]
    # libsndfile's Vorbis encoder crashes on very large single writes, so stream in blocks.
    with sf.SoundFile(OUT / f"{name}.ogg", "w", SR, channels, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(data), 8192):
            f.write(data[i:i + 8192])
    print(name, round((OUT / f"{name}.ogg").stat().st_size / 1024), "KB")


if __name__ == "__main__":
    write("ambient", ambient())
    write("tap", ui_sound([(1318.5, 0)], 0.18, 38, 0.35))              # soft glass tick
    write("select", ui_sound([(880.0, 0), (1318.5, 0.0)], 0.6, 9, 0.45))  # small bell
    write("confirm", ui_sound([(659.3, 0), (987.8, 0.12)], 1.2, 5, 0.5))  # rising two-note chime
