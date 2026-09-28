#!/usr/bin/env python3
"""
Generates the sounds and small textures that aren't part of the 3D model.
Everything is synthesized/drawn here, so the mod contains no third-party assets.

Run from the project root:  python3 tools/gen_assets.py   (needs numpy, Pillow and ffmpeg)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/huey")
RATE = 44100
rng = np.random.default_rng(1965)


def periodic_noise(n, lo, hi):
    """Band-limited noise that loops perfectly (built in the frequency domain)."""
    spec = np.zeros(n // 2 + 1, dtype=complex)
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    band = (freqs >= lo) & (freqs <= hi)
    spec[band] = np.exp(2j * np.pi * rng.random(band.sum()))
    sig = np.fft.irfft(spec, n)
    return sig / np.max(np.abs(sig))


def write_ogg(path, samples):
    samples = samples / np.max(np.abs(samples)) * 0.89
    pcm = (samples * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())
    os.makedirs(os.path.dirname(path), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5", path], check=True)
    os.remove(wav_path)


def rotor_loop():
    """Two seconds of the Huey's two-blade thump (~11 blade passes per second) plus turbine whine."""
    seconds = 2.0
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    blade_hz = 11.0  # 22 thumps in 2 s, so the loop point is seamless
    phase = (t * blade_hz) % 1.0
    env = np.exp(-phase / 0.07)                 # sharp attack, quick decay: the "whop"
    thump = np.sin(2 * np.pi * 62 * t + 1.5 * np.sin(2 * np.pi * blade_hz * t)) * env
    slap = periodic_noise(n, 150, 900) * np.exp(-phase / 0.025)  # blade slap crack
    rumble = periodic_noise(n, 25, 180) * 0.35
    whine = (np.sin(2 * np.pi * 1320 * t) * 0.05 + np.sin(2 * np.pi * 2640 * t) * 0.02)
    hiss = periodic_noise(n, 2000, 7000) * 0.05
    return thump * 1.0 + slap * 0.55 + rumble + whine + hiss


def gun_shot(seed):
    """One M60 round: a sharp crack, a chesty thump, and a short tail."""
    local = np.random.default_rng(seed)
    n = int(RATE * 0.32)
    t = np.arange(n) / RATE
    crack = local.standard_normal(n) * np.exp(-t / 0.012)
    body = np.sin(2 * np.pi * (95 + 40 * np.exp(-t / 0.02)) * t) * np.exp(-t / 0.05)
    tail = np.convolve(local.standard_normal(n), np.ones(40) / 40, mode="same") * np.exp(-t / 0.09) * 0.5
    attack = np.minimum(1.0, t / 0.0008)
    return (crack * 0.8 + body * 1.2 + tail) * attack


def item_icon():
    """16x16 inventory icon: a side-view Huey silhouette."""
    art = [
        "................",
        "BBBBBBBBBBBBBBB.",
        ".......M........",
        ".....OOOOO......",
        "..GGOOOOOOOOOOF.",
        ".GGGOOOOOOOOOOF.",
        ".GGOOOOOOO....F.",
        "..OOOOOOO.......",
        "...OOOOO........",
        "...S...S........",
        "..SSSSSSSS......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    colors = {
        "B": (40, 40, 38), "M": (110, 110, 104), "O": (84, 86, 52), "G": (110, 150, 138),
        "F": (70, 72, 44), "S": (120, 120, 112),
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # shift down so the icon is vertically centred
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in colors:
                shade = 1.0 if ch != "O" or y > 4 else 1.12
                c = tuple(min(255, int(v * shade)) for v in colors[ch])
                img.putpixel((x, y + 2), c + (255,))
    return img


if __name__ == "__main__":
    write_ogg(os.path.join(ASSETS, "sounds/rotor.ogg"), rotor_loop())
    for i in range(1, 4):
        write_ogg(os.path.join(ASSETS, f"sounds/door_gun_{i}.ogg"), gun_shot(i))
    icon = item_icon()
    icon.save(os.path.join(ASSETS, "textures/item/huey.png"))
    icon.resize((128, 128), Image.NEAREST).save(os.path.join(ASSETS, "icon.png"))
    print("sounds and icons written")
