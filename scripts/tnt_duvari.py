#!/usr/bin/env python3
"""TNT duvarı: bedrock/build.py içindeki TNTS listesinden, paketin kendi blok
dokularıyla (super_tnt_RP/textures/blocks/stnt_<id>_{top,side}.png) her TNT'yi
izometrik bir küp olarak çizen SVG üretir. Dokular base64 gömülü olduğu için
dosya tek başına açılır; GitHub ve oku aynı dosyayı gösterir.

    python3 bedrock/build.py            # önce paket (dokular üretilir)
    python3 scripts/tnt_duvari.py       # -> docs/guide/tnt-duvari.svg
"""
import base64
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RP = os.path.join(ROOT, "bedrock", "super_tnt_RP", "textures", "blocks")
OUT = os.path.join(ROOT, "docs", "guide", "tnt-duvari.svg")

COLS, TILE, W = 10, 124, 30          # sütun, hücre genişliği, küp yarı genişliği
LABEL_H, PAD = 34, 24


def load_tnts():
    src = open(os.path.join(ROOT, "bedrock", "build.py"), encoding="utf-8").read()
    i = src.index("\nTNTS = [")
    j = src.index("\n]\n", i)
    ns = {}
    exec(src[i + 1:j + 2], ns)  # yalnız sabit liste; kod çalıştırmaz
    return ns["TNTS"]


def data_uri(path):
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def cube(x, y, top, side, w=W):
    """(x, y) üst köşe; w yarı genişlik; küp yüksekliği w."""
    h = w
    faces = [
        # top: birim kareyi baklavaya eşler
        (f"matrix({w/16},{w/32},{-w/16},{w/32},{x},{y})", top, 0),
        # sol yüz
        (f"matrix({w/16},{w/32},0,{h/16},{x-w},{y+w/2})", side, 0.18),
        # sağ yüz
        (f"matrix({w/16},{-w/32},0,{h/16},{x},{y+w})", side, 0.36),
    ]
    out = []
    for m, uri, shade in faces:
        out.append(f'<g transform="{m}"><image href="{uri}" width="16" height="16" '
                   f'preserveAspectRatio="none" style="image-rendering:pixelated"/>')
        if shade:
            out.append(f'<rect width="16" height="16" fill="#000" opacity="{shade}"/>')
        out.append("</g>")
    return "".join(out)


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;")


def main():
    tnts = load_tnts()
    rows = -(-len(tnts) // COLS)
    width = PAD * 2 + COLS * TILE
    height = PAD * 2 + 44 + rows * (TILE + LABEL_H)
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
             f'viewBox="0 0 {width} {height}" font-family="ui-sans-serif, system-ui, -apple-system, '
             f"'Segoe UI', Roboto, sans-serif\">",
             f'<rect width="{width}" height="{height}" rx="14" fill="#ffffff" stroke="#e5e7eb"/>',
             f'<text x="{PAD}" y="{PAD + 20}" font-size="16" font-weight="700" fill="#111827">'
             f'{len(tnts)} TNT — paketin kendi dokularıyla</text>']
    cache = {}
    for n, t in enumerate(tnts):
        tid = t["id"]
        faces = {}
        for face in ("top", "side"):
            p = os.path.join(RP, f"stnt_{tid}_{face}.png")
            if not os.path.exists(p):
                sys.exit(f"doku yok: {p} — önce python3 bedrock/build.py")
            faces[face] = cache.setdefault(p, data_uri(p))
        col, row = n % COLS, n // COLS
        cx = PAD + col * TILE + TILE / 2
        cy = PAD + 44 + row * (TILE + LABEL_H)
        parts.append(cube(cx, cy + 14, faces["top"], faces["side"]))
        parts.append(f'<text x="{cx}" y="{cy + TILE + 8}" font-size="11" fill="#374151" '
                     f'text-anchor="middle">{esc(t["tr"])}</text>')
    parts.append("</svg>")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w", encoding="utf-8").write("\n".join(parts))
    print(f"{OUT}: {len(tnts)} TNT, {os.path.getsize(OUT)//1024} KB")


if __name__ == "__main__":
    main()
