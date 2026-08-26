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
RP = os.path.join(os.path.dirname(HERE), "super_tnt_RP")

# Dama tahtasi: saydam pikselin arkasinda gorunen iki gri
CHECK_A = (150, 150, 155)
CHECK_B = (110, 110, 115)
GAP = (30, 30, 34)


def read_png(path):
    """Her bit derinligi + palet destekli PNG okuyucu. (genislik, yukseklik,
    satirlar) doner; her piksel (r, g, b, a)."""
    d = open(path, "rb").read()
    i, idat, plte, trns = 8, b"", None, None
    w = h = bd = ct = None
    while i < len(d):
        ln = struct.unpack(">I", d[i:i + 4])[0]
        tag, data = d[i + 4:i + 8], d[i + 8:i + 8 + ln]
        if tag == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", data[:10])
        elif tag == b"IDAT":
            idat += data
        elif tag == b"PLTE":
            plte = data
        elif tag == b"tRNS":
            trns = data
        i += 12 + ln
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    bpc = max(1, bd // 8)
    ch = nch * bpc
    stride = w * ch if bd >= 8 else (w * nch * bd + 7) // 8
    bpp = max(1, (nch * bd) // 8)
    rows, prev, pos = [], bytearray(stride), 0
    for _y in range(h):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        for x in range(stride):                      # PNG satir filtreleri
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        row = []
        for x in range(w):
            if bd < 8:
                per = 8 // bd
                v = [(line[x // per] >> (8 - bd * (x % per + 1))) & ((1 << bd) - 1)]
            else:
                rv = line[x * ch:(x + 1) * ch]
                v = [rv[k * bpc] for k in range(nch)]
            if ct == 3:
                idx = v[0]
                al = trns[idx] if trns and idx < len(trns) else 255
                row.append(tuple(plte[idx * 3:idx * 3 + 3]) + (al,))
            elif ct == 2:
                row.append(tuple(v) + (255,))
            elif ct == 6:
                row.append(tuple(v))
            elif ct == 0:
                g = v[0] * (255 // ((1 << bd) - 1)) if bd < 8 else v[0]
                row.append((g, g, g, 255))
            else:
                row.append((v[0],) * 3 + (v[1],))
        rows.append(row)
        prev = line
    return w, h, rows


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
