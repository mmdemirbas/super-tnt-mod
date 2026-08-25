#!/usr/bin/env python3
"""Bir .geo.json'i PNG'ye cizer — modeli oyuna gitmeden GORMEK icin.

    python3 bedrock/tools/pose_render.py bedrock/custom/mutant_warden.geo.json \
        --out tmp/warden.png [--rot right_arm=0,0,-20 --rot left_arm=0,0,20]

Neden var: bir modelin "nasil durdugu" hicbir statik denetimle yakalanmaz. Poz
hatalari (kollar govdeye giriyor, bacak arasinda birlesiyor) ancak GORULEREK
bulunur ve tablete gidip gelmek pahali. Bu betik kutulari kendi donusleriyle
birlikte on / yan / ust goruntuye dusurup boyar; bone basina ayri renk verir ki
hangi parcanin nerede oldugu okunabilsin.

--rot ile bir kemige ek donus verilip sonuc ANINDA gorulebilir; begenilen deger
build.py'ye pose animasyonu olarak yazilir.

Yalniz stdlib. Boyama: ressam algoritmasi (uzaktan yakina), z-derinlige gore.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import struct
import sys
import zlib

# Kemik basina renk — parcalari birbirinden ayirmak icin.
COLORS = [
    (220, 80, 80), (80, 180, 220), (120, 210, 120), (240, 200, 90),
    (200, 130, 230), (240, 150, 90), (120, 140, 240), (230, 120, 180),
    (140, 220, 200), (190, 190, 110), (160, 120, 90), (200, 200, 210),
]


def _mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3))
                 for i in range(3))


def rot_matrix(rx, ry, rz):
    """Bedrock .geo.json donusunu matrise cevirir: M = Rx(-x) * Ry(y) * Rz(-z).

    Isaretler ve sira TAHMIN DEGIL, olculdu. Bir .geo.json'daki her kutu ya
    kendi kemigindeki baska bir kutuya ya da govdeye DEGMEK zorundadir; model
    parcalari havada asili duramaz. Alti sira x sekiz isaret = 48 kombinasyon
    denendiginde mutant_warden'da yalnizca x ve z'si ters cevrilmis olanlar
    "toplam kopukluk 0.0" veriyor; duz (+,+,+) sira Rz*Ry*Rx ise 34.8 birim
    kopukluk, yani uzuvlar govdeden ayri havada duruyor. Y isareti bu modelde
    ayirt edilemiyor (yalnizca iki ince antende buyuk Y donusu var); +1 secildi.

    Yanlis konvansiyon sessiz bir hatadir: render yine bir sey cizer, sadece
    yanlis seyi cizer. Bu yuzden yeni bir modelde poz olcerken once ayni
    kopukluk testini kosmak gerekir.
    """
    rx, ry, rz = math.radians(-rx), math.radians(ry), math.radians(-rz)
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    my = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    mz = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))
    return _mul(_mul(mx, my), mz)


def apply(m, v):
    return tuple(sum(m[i][j] * v[j] for j in range(3)) for i in range(3))


def rot_about(p, pivot, m):
    d = [p[i] - pivot[i] for i in range(3)]
    r = apply(m, d)
    return [r[i] + pivot[i] for i in range(3)]


def cube_faces(origin, size, pivot, m):
    """Kutunun sekiz kosesini dondurup alti yuzunu (kose dizisi) dondurur."""
    x0, y0, z0 = origin
    x1, y1, z1 = (origin[i] + size[i] for i in range(3))
    corners = [[x, y, z] for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    if m is not None:
        corners = [rot_about(c, pivot, m) for c in corners]
    #      0:(x0,y0,z0) 1:(x0,y0,z1) 2:(x0,y1,z0) 3:(x0,y1,z1)
    #      4:(x1,y0,z0) 5:(x1,y0,z1) 6:(x1,y1,z0) 7:(x1,y1,z1)
    quads = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4),
             (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
    return [[corners[i] for i in q] for q in quads]


CUBE_PIVOT = "bone"


def collect(geo, extra_rot):
    """Her kutuyu (kemik adi, dunya koordinatli yuzler) olarak dondurur."""
    bones = {b["name"]: b for b in geo["bones"]}
    out = []

    def chain(name):
        seq, seen = [], set()
        while name and name in bones and name not in seen:
            seen.add(name)
            seq.append(bones[name])
            name = bones[name].get("parent")
        return seq                              # cocuktan koke

    for b in geo["bones"]:
        for c in b.get("cubes", []):
            origin = c["origin"]
            size = c["size"]
            pivot = c.get("pivot") or (b.get("pivot", [0, 0, 0]) if CUBE_PIVOT == "bone"
                                       else ([0, 0, 0] if CUBE_PIVOT == "zero" else
                                             [c["origin"][i] + c["size"][i] / 2 for i in range(3)]))
            m = rot_matrix(*c["rotation"]) if c.get("rotation") else None
            faces = cube_faces(origin, size, pivot, m)
            # Kemik zinciri: her kemigin kendi donusu + komut satirindan gelen ek
            for bone in chain(b["name"]):
                rot = list(bone.get("rotation") or [0, 0, 0])
                ex = extra_rot.get(bone["name"])
                if ex:
                    rot = [rot[i] + ex[i] for i in range(3)]
                if any(rot):
                    bm = rot_matrix(*rot)
                    bp = bone.get("pivot", [0, 0, 0])
                    faces = [[rot_about(p, bp, bm) for p in f] for f in faces]
            out.append((b["name"], faces))
    return out


def draw(cubes, view, w, h, scale, cx, cy):
    """Ressam algoritmasi: derinlige gore siralayip poligonlari doldurur."""
    grid = [[(24, 26, 32)] * w for _ in range(h)]
    names = sorted({n for n, _ in cubes})
    color_of = {n: COLORS[i % len(COLORS)] for i, n in enumerate(names)}

    def project(p):
        if view == "front":                     # +x saga, +y yukari, derinlik z
            return p[0], p[1], p[2]
        if view == "side":                      # +z saga, +y yukari, derinlik x
            return p[2], p[1], -p[0]
        return p[0], -p[2], -p[1]               # ust: +x saga, -z asagi

    polys = []
    for name, faces in cubes:
        for f in faces:
            pr = [project(p) for p in f]
            depth = sum(q[2] for q in pr) / len(pr)
            polys.append((depth, name, [(cx + q[0] * scale, cy - q[1] * scale) for q in pr]))
    polys.sort(key=lambda t: t[0])              # uzaktan yakina

    for depth, name, pts in polys:
        base = color_of[name]
        shade = max(0.45, min(1.15, 0.75 + depth / 90.0))
        col = tuple(max(0, min(255, int(c * shade))) for c in base)
        ys = [p[1] for p in pts]
        for y in range(max(0, int(min(ys))), min(h, int(max(ys)) + 1)):
            xs = []
            for i in range(len(pts)):
                x1, y1 = pts[i]
                x2, y2 = pts[(i + 1) % len(pts)]
                if (y1 <= y < y2) or (y2 <= y < y1):
                    xs.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(max(0, int(xs[i])), min(w, int(xs[i + 1]) + 1)):
                    grid[y][x] = col
    return grid, color_of


def write_png(path, grid):
    h, w = len(grid), len(grid[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in grid)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    hdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    blob = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", hdr)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    open(path, "wb").write(blob)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("geo", help=".geo.json yolu")
    ap.add_argument("--out", default="tmp/pose.png")
    ap.add_argument("--rot", action="append", default=[],
                    help="kemige ek donus: --rot right_arm=0,0,-20")
    ap.add_argument("--scale", type=float, default=5.0, help="piksel / model birimi")
    ap.add_argument("--cube-pivot", choices=("bone", "zero", "center"), default="bone",
                    help="kutu donusunun merkezi (pivot alani yoksa)")
    args = ap.parse_args()
    global CUBE_PIVOT
    CUBE_PIVOT = args.cube_pivot

    extra = {}
    for r in args.rot:
        name, _, vals = r.partition("=")
        parts = [float(v) for v in vals.split(",")]
        if len(parts) != 3:
            print(f"--rot {r}: uc sayi bekleniyor (x,y,z)", file=sys.stderr)
            return 2
        extra[name] = parts

    geo = json.load(open(args.geo, encoding="utf-8"))["minecraft:geometry"][0]
    cubes = collect(geo, extra)

    pad, s = 20, args.scale
    xs = [p[0] for _, fs in cubes for f in fs for p in f]
    ys = [p[1] for _, fs in cubes for f in fs for p in f]
    zs = [p[2] for _, fs in cubes for f in fs for p in f]
    vw = int((max(xs) - min(xs)) * s) + pad * 2
    vh = int((max(ys) - min(ys)) * s) + pad * 2
    dw = int((max(zs) - min(zs)) * s) + pad * 2

    views = [("front", vw, vh, pad - min(xs) * s, vh - pad + min(ys) * s),
             ("side", dw, vh, pad - min(zs) * s, vh - pad + min(ys) * s),
             ("top", vw, dw, pad - min(xs) * s, dw - pad + min(zs) * s)]

    panels = []
    for view, w, h, cx, cy in views:
        grid, colors = draw(cubes, view, w, h, s, cx, cy)
        panels.append(grid)

    H = max(len(p) for p in panels)
    W = sum(len(p[0]) for p in panels) + 8 * (len(panels) - 1)
    out = [[(12, 12, 16)] * W for _ in range(H)]
    x0 = 0
    for p in panels:
        for y, row in enumerate(p):
            out[y][x0:x0 + len(row)] = row
        x0 += len(p[0]) + 8
    write_png(args.out, out)

    print(f"{args.out}  ({W}x{H})  gorunum: on | yan | ust")
    print("kemik renkleri:")
    for name, col in sorted(colors.items()):
        print(f"  {name:16s} rgb{col}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
