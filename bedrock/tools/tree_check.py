#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Mega Gubre'nin agac geometrisini oyuna girmeden olcer.

    python3 bedrock/tools/tree_check.py

NEDEN. Dev agac betikten blok blok oruluyor; kac blok ettigi ve ne kadar surdugu
ancak sayilarak bilinir. Tablette denemek her tur icin build + adb push +
elle tiklama demek, uc dakika. Bu betik ayni geometriyi Python'da kurup blok
sayisini, en yogun katmani, tepe genisligini ve tick butcesine gore sureyi
yazar. PORT-DURUMU.md'deki sayilar buradan geliyor.

DIKKAT: build.py icindeki megaTree() fonksiyonunun AYNASIDIR. Olculer
(TREE_H, TREE_TRUNK, TREE_CROWN) build.py'den okunur, ama sekil formulleri
burada tekrar yazilmistir — megaTree'nin govde/dal/tepe formullerini
degistirirsen buradakini de degistir, yoksa olctugun sey artik oyundaki agac
degildir.
"""
import importlib.util
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("stnt_build", os.path.join(HERE, "..", "build.py"))
build = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build)          # build() sadece __main__'de calisir

H = build.TREE_H
R0 = build.TREE_TRUNK
CR = build.TREE_CROWN
PER = [it for it in build.ITEMS if it["id"] == "mega_gubre"][0]["action"]["perTick"]

CY0 = round(H * 0.55)                   # tepenin baslangici
CY1 = H + round(CR * 0.6)               # en ust yaprak katmani


def trunk_r(y):
    return max(1, R0 * (1 - 0.72 * min(1, y / H)))


def crown_r(y):
    if y < CY0 or y > CY1:
        return 0
    return CR * math.pow(math.sin(math.pi * (y - CY0) / (CY1 - CY0)), 0.55)


def branch_cells():
    """y -> [(dx, dz), ...]"""
    out = {}
    for i in range(4):
        y0 = round(H * (0.60 + i * 0.09))
        n = 4 + (i % 2)
        length = round(CR * (0.75 - i * 0.12))
        for k in range(n):
            ang = (k / n) * 2 * math.pi + i * 0.7
            ux, uz = math.cos(ang), math.sin(ang)
            for d in range(1, length + 1):
                x, z = round(ux * d), round(uz * d)
                y = y0 + round(d * 0.45)
                out.setdefault(y, []).append((x, z))
                if d > 2:
                    out.setdefault(y - 1, []).append((x, z))
    return out


def measure(seed=1):
    random.seed(seed)
    branches = branch_cells()
    cells = set()
    n_trunk = n_branch = n_leaf = 0
    per_layer = {}
    for y in range(-3, CY1 + 1):
        before = len(cells)
        if y <= H:                                          # govde + kok
            r = R0 + 1 if y < 0 else trunk_r(y)
            ri = math.ceil(r)
            for dx in range(-ri, ri + 1):
                for dz in range(-ri, ri + 1):
                    if dx * dx + dz * dz > r * r:
                        continue
                    cells.add((dx, y, dz))
                    n_trunk += 1
        for (dx, dz) in branches.get(y, []):                # dallar
            if (dx, y, dz) not in cells:
                cells.add((dx, y, dz))
                n_branch += 1
        cr = crown_r(y)                                     # tepe
        if cr >= 1:
            ri = math.ceil(cr)
            inner = cr - 2.2
            for dx in range(-ri, ri + 1):
                for dz in range(-ri, ri + 1):
                    dist = math.hypot(dx, dz)
                    if dist > cr:
                        continue
                    if dist < inner and random.random() > 0.1:
                        continue
                    if (dx, y, dz) in cells:
                        continue
                    cells.add((dx, y, dz))
                    n_leaf += 1
        per_layer[y] = len(cells) - before
    return n_trunk, n_branch, n_leaf, per_layer


def main():
    n_trunk, n_branch, n_leaf, per_layer = measure()
    total = n_trunk + n_branch + n_leaf
    rmax = max(crown_r(y) for y in range(CY0, CY1 + 1))
    busiest = max(per_layer, key=per_layer.get)
    print(f"govde {n_trunk}  dal {n_branch}  yaprak {n_leaf}  TOPLAM {total} blok")
    print(f"yukseklik: govde {H}, yapraklarla {CY1}; tepe y={CY0}..{CY1}")
    print(f"tepe yaricapi {rmax:.1f} -> genislik {2 * round(rmax) + 1} blok")
    print(f"en yogun katman y={busiest} -> {per_layer[busiest]} blok")
    print(f"butce {PER}/tick -> ~{math.ceil(total / PER)} tick = {total / PER / 20:.1f} saniye")
    print(f"govde yaricapi y=0:{trunk_r(0):.1f} y={H // 2}:{trunk_r(H // 2):.1f} y={H}:{trunk_r(H):.1f}")
    print("siluet (5 katmanda bir, cubuk = yaricap):")
    for y in range(CY1, -4, -5):
        w = max(crown_r(y), trunk_r(y) if 0 <= y <= H else (R0 + 1 if y < 0 else 0))
        print(f"  y={y:4d} |{'#' * int(round(w))}")


if __name__ == "__main__":
    sys.exit(main())
