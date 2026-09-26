#!/usr/bin/env python3
"""Uretilen ikonlari TEK bir PNG'de yan yana dizer — ikonlara BAKMAK icin.

    python3 bedrock/tools/icon_sheet.py            # esya ikonlari
    python3 bedrock/tools/icon_sheet.py --blocks   # blok dokulari
    python3 bedrock/tools/icon_sheet.py --out tmp/x.png --scale 8

Neden var: bir ikonun "yanlis" oldugu hicbir kimlik/dosya denetiminde
gorunmez. 16x16'lik dosyayi tek tek acmak yerine hepsini olceklenmis bir
tabakada gormek, hatali olani saniyeler icinde gosterir. Saydam pikseller
DAMA TAHTASI olarak cizilir; boylece "arkaplani boyanmis" ikon ile saydam
olan bir bakista ayirt edilir.

Cikti dosyasinin yani sira, hucre sirasina karsilik gelen ad listesi
stdout'a basilir (tabakada yazi yoktur — piksel font tasimamak icin).
"""
from __future__ import annotations

import argparse
import os
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))


def _build():
    """build.py'yi modul olarak yukler (tools/ paket degil, yol ile)."""
    import importlib.util
    spec = importlib.util.spec_from_file_location(
        "build", os.path.join(os.path.dirname(HERE), "build.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod
RP = os.path.join(os.path.dirname(HERE), "super_tnt_RP")

# Dama tahtasi: saydam pikselin arkasinda gorunen iki gri
CHECK_A = (150, 150, 155)
CHECK_B = (110, 110, 115)
GAP = (30, 30, 34)


# PNG okuyucu build.py'de: iki kopya kacinilmaz olarak birbirinden ayrilir.
read_png = _build().read_png


def write_png(path, rows):
    wd, ht = len(rows[0]), len(rows)
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in r) for r in rows)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff))

    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    open(path, "wb").write(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", wd, ht, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def sheet(paths, scale, cols):
    cell = 16 * scale
    pad = 6
    rows_n = (len(paths) + cols - 1) // cols
    W = cols * (cell + pad) + pad
    H = rows_n * (cell + pad) + pad
    canvas = [[list(GAP) for _ in range(W)] for _ in range(H)]
    for n, p in enumerate(paths):
        w, h, px = read_png(p)
        ox = pad + (n % cols) * (cell + pad)
        oy = pad + (n // cols) * (cell + pad)
        for y in range(cell):
            for x in range(cell):
                sx, sy = x * w // cell, y * h // cell
                r, g, b, a = px[sy][sx]
                bg = CHECK_A if ((x // (scale * 2)) + (y // (scale * 2))) % 2 == 0 else CHECK_B
                canvas[oy + y][ox + x] = [
                    (r * a + bg[k] * (255 - a)) // 255 for k, r in enumerate((r, g, b))]
    return canvas


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blocks", action="store_true", help="blok dokulari")
    ap.add_argument("--dir", help="dogrudan bir klasor")
    ap.add_argument("--out", default="tmp/icon_sheet.png")
    ap.add_argument("--scale", type=int, default=6)
    ap.add_argument("--cols", type=int, default=8)
    a = ap.parse_args()
    src = a.dir or os.path.join(RP, "textures", "blocks" if a.blocks else "items")
    names = sorted(f for f in os.listdir(src) if f.endswith(".png"))
    if not names:
        print(f"{src} icinde png yok", file=sys.stderr)
        return 1
    write_png(a.out, sheet([os.path.join(src, f) for f in names], a.scale, a.cols))
    for i, f in enumerate(names):
        end = "\n" if i % a.cols == a.cols - 1 else "  "
        print(f"{i:3d} {f[:-4].replace('stnt_', ''):26s}", end=end)
    print(f"\n{len(names)} ikon -> {os.path.abspath(a.out)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
