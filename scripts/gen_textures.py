#!/usr/bin/env python3
"""Ender Çakmağı / Altın Flint ve ateşlerinin 16x16 dokularını üretir.

Dokular elle boyanmak yerine burada piksel ızgarası olarak tutuluyor: küçük bir
düzeltme için PNG editörü açmak gerekmesin. Harici bağımlılık yok — PNG yazımı
zlib + struct ile yapılıyor (Pillow kurulu olmayabilir).

Kullanım:  python3 scripts/gen_textures.py
"""

import os
import struct
import zlib

SIZE = 16
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "supertntmod", "textures")


def write_png(path, rows, palette):
    """rows: 16 adet 16 karakterlik satır; palette: karakter -> (r,g,b,a)."""
    assert len(rows) == SIZE, f"{path}: {len(rows)} satır, {SIZE} olmalı"
    raw = bytearray()
    for y, row in enumerate(rows):
        assert len(row) == SIZE, f"{path}: satır {y} uzunluğu {len(row)}, {SIZE} olmalı"
        raw.append(0)  # filter type: None
        for ch in row:
            raw.extend(palette[ch])

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)  # 8-bit RGBA
    blob = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(blob)
    print(f"yazıldı: {os.path.relpath(path, ROOT)}")


TRANSPARENT = (0, 0, 0, 0)

# Çakmak silueti: sol üstte metal kol, sağ altta çakmaktaşı, aralarında kıvılcım.
# d/m/l = metalin koyu/orta/açık tonu, k/n/h = taşın koyu/orta/açık tonu, * = kıvılcım
FLINT = [
    "................",
    "....ddddd.......",
    "...dmmmmmd......",
    "..dmlllllmd.....",
    "..dml....lmd....",
    "..dml.....dm.*..",
    "..dml......d....",
    "..dmd........*..",
    "...dd...kkk.....",
    ".......knnnhk...",
    "......knnhnnnk..",
    "......knnnnnnk..",
    ".......knnnnk...",
    "........kkkk....",
    "................",
    "................",
]

# Alev silueti: o = dış hale, m = gövde, c = çekirdek
FLAME = [
    "................",
    "................",
    ".......oo.......",
    "......ooo.......",
    "......omo.......",
    ".....oommo......",
    ".....ommmo......",
    "....oommmoo.....",
    "....ommcmmo.....",
    "...oommcmmoo....",
    "...ommcccmmo....",
    "..oommcccmmoo...",
    "..ommcccccmmo...",
    "..ommcccccmmo...",
    "..oommcccmmoo...",
    "...oommmmmoo....",
]

TEXTURES = [
    ("item/ender_flint.png", FLINT, {
        ".": TRANSPARENT,
        "d": (42, 20, 74, 255), "m": (90, 48, 150, 255), "l": (152, 108, 224, 255),
        "k": (18, 48, 46, 255), "n": (38, 96, 90, 255), "h": (92, 168, 158, 255),
        "*": (198, 160, 255, 255),
    }),
    ("item/golden_flint.png", FLINT, {
        ".": TRANSPARENT,
        "d": (110, 76, 12, 255), "m": (196, 148, 32, 255), "l": (255, 214, 96, 255),
        "k": (58, 58, 62, 255), "n": (104, 104, 110, 255), "h": (162, 162, 168, 255),
        "*": (255, 244, 176, 255),
    }),
    ("block/ender_fire.png", FLAME, {
        ".": TRANSPARENT,
        "o": (74, 34, 160, 255), "m": (58, 108, 235, 255), "c": (150, 236, 255, 255),
    }),
    ("block/gold_fire.png", FLAME, {
        ".": TRANSPARENT,
        "o": (150, 92, 8, 255), "m": (248, 184, 32, 255), "c": (255, 248, 190, 255),
    }),
]


def main():
    for rel, rows, palette in TEXTURES:
        write_png(os.path.join(ASSETS, *rel.split("/")), rows, palette)


if __name__ == "__main__":
    main()
