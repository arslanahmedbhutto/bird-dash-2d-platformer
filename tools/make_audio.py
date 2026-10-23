"""Generates the game's original music (MIDI) and sound effects (WAV).

Everything here is synthesised from scratch, so the audio is free of third-party copyright.
Usage: python tools/make_audio.py   (writes into sounds/)
"""
import math
import random
import struct
import wave
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "sounds"
RATE = 22050


# ---------------------------------------------------------------- MIDI ---
def _varlen(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.insert(0, (n & 0x7F) | 0x80)
        n >>= 7
    return bytes(out)


def write_midi(path, tracks, tempo_bpm=120, ppq=480):
    """tracks: list of (program, channel, [(start_beats, length_beats, note, velocity), ...])."""
    chunks = []
    for program, ch, notes in tracks:
        events = [(0, bytes([0xC0 | ch, program]))]
        for start, length, note, vel in notes:
            events.append((int(start * ppq), bytes([0x90 | ch, note, vel])))
            events.append((int((start + length) * ppq) - 1, bytes([0x80 | ch, note, 0])))
        events.sort(key=lambda e: (e[0], e[1][0] & 0xF0 == 0x90))
        data, last = b"", 0
        if not chunks:  # tempo in the first track
            data += _varlen(0) + b"\xFF\x51\x03" + struct.pack(">I", int(60_000_000 / tempo_bpm))[1:]
        for t, ev in events:
            data += _varlen(t - last) + ev
            last = t
        data += _varlen(0) + b"\xFF\x2F\x00"
        chunks.append(b"MTrk" + struct.pack(">I", len(data)) + data)
    header = b"MThd" + struct.pack(">IHHH", 6, 1, len(chunks), ppq)
    path.write_bytes(header + b"".join(chunks))


def note(name):
    names = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}
    return 12 * (int(name[-1]) + 1) + names[name[:-1]]


def melody(seq, start=0.0):
    """seq: list of (note_name or None, beats)."""
    out, t = [], start
    for n, d in seq:
        if n:
            out.append((t, d * 0.9, note(n), 92))
        t += d
    return out


def bass(roots, beats_each=4, start=0.0):
    out, t = [], start
    for r in roots:
        for k in range(beats_each):
            out.append((t + k, 0.45, note(r), 78 if k % 2 == 0 else 64))
        t += beats_each
    return out


def make_menu_theme():
    # Calm I-vi-IV-V in C major, 96 bpm.
    mel = melody([("E5", 1), ("G5", 1), ("C6", 1.5), ("B5", 0.5),
                  ("A5", 1), ("E5", 1), ("A5", 2),
                  ("F5", 1), ("A5", 1), ("C6", 1), ("A5", 1),
                  ("G5", 1.5), ("F5", 0.5), ("D5", 2)] * 2)
    bas = bass(["C3", "A2", "F2", "G2"] * 2)
    write_midi(OUT / "start.mid", [(80, 0, mel), (38, 1, bas)], tempo_bpm=96)


def make_level_theme():
    # Upbeat G major loop, 132 bpm, with arpeggiated lead.
    arp = []
    t = 0.0
    for chord in [("G4", "B4", "D5"), ("E4", "G4", "B4"), ("C4", "E4", "G4"), ("D4", "F#4", "A4")] * 2:
        for _ in range(2):
            for n in chord + (chord[1],):
                arp.append((t, 0.4, note(n), 70))
                t += 0.5
    lead = melody([("D5", 0.5), ("G5", 0.5), ("B5", 1), ("A5", 0.5), ("G5", 0.5), ("E5", 1),
                   ("E5", 0.5), ("G5", 0.5), ("B5", 1), ("C6", 1), ("B5", 1),
                   ("C6", 0.5), ("B5", 0.5), ("A5", 1), ("G5", 1), ("E5", 1),
                   ("F#5", 0.5), ("G5", 0.5), ("A5", 1), ("D5", 2)] * 2)
    bas = bass(["G2", "E2", "C2", "D2"] * 2)
    write_midi(OUT / "background.midi", [(81, 0, lead), (11, 2, arp), (33, 1, bas)], tempo_bpm=132)


# ----------------------------------------------------------------- WAV ---
def write_wav(path, samples):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 30000)) for s in samples))


def tone(freq_fn, dur, wave_shape="square", vol=0.5, attack=0.005, release=0.05):
    n = int(dur * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        phase += freq_fn(t) / RATE
        if wave_shape == "square":
            s = 1.0 if (phase % 1.0) < 0.5 else -1.0
        elif wave_shape == "triangle":
            s = 4 * abs((phase % 1.0) - 0.5) - 1
        else:
            s = math.sin(2 * math.pi * phase)
        env = min(1.0, t / attack) * min(1.0, (dur - t) / release)
        out.append(s * vol * env)
    return out


def make_sfx():
    random.seed(7)
    # Jump: quick rising chirp.
    write_wav(OUT / "jump.wav", tone(lambda t: 320 + 2600 * t, 0.16, "square", 0.32, release=0.06))
    # Death: falling pitch with a little noise.
    death = tone(lambda t: 700 * math.exp(-4 * t), 0.6, "square", 0.35, release=0.2)
    death = [s + random.uniform(-0.06, 0.06) * (1 - i / len(death)) for i, s in enumerate(death)]
    write_wav(OUT / "death.wav", death)
    # Level complete: rising major arpeggio fanfare.
    fan = []
    for f, d in [(523.25, 0.12), (659.25, 0.12), (783.99, 0.12), (1046.5, 0.45)]:
        fan += tone(lambda t, f=f: f, d, "triangle", 0.5, release=0.04)
    write_wav(OUT / "complete.wav", fan)
    # Game over: slow descending minor motif.
    over = []
    for f, d in [(392.0, 0.25), (311.13, 0.25), (261.63, 0.6)]:
        over += tone(lambda t, f=f: f * (1 - 0.03 * t), d, "triangle", 0.45, release=0.08)
    write_wav(OUT / "gameover.wav", over)


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    make_menu_theme()
    make_level_theme()
    make_sfx()
    for f in sorted(OUT.iterdir()):
        print(f"{f.name:<18} {f.stat().st_size / 1024:6.1f} KB")
