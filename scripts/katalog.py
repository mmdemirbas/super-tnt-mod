#!/usr/bin/env python3
"""Katalog: bedrock/build.py içindeki listelerden (TNTS, BLOCKS, ITEMS,
MONSTERS, MORPHS_*) Türkçe katalog tablolarını üretir. İki hedef:

  docs/guide/katalog.md             — belge sitesindeki tam katalog sayfası
  README.md  (<!-- katalog --> arası) — README'deki özet sayılar

    python3 scripts/katalog.py

Listeler ürünün kaynağı; bu betik yalnız okur. Elle yazılmış tablo README'de
kalmasın diye vardır: bir TNT eklenince katalog kendini yeniler.
"""
import contextlib
import importlib.util
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MONSTER_TR = {"ender_send": "Ender Send", "dev_zombi": "Dev Zombi",
              "dev_creeper": "Dev Creeper", "mutant_warden": "Mutant Warden"}


def load_build():
    spec = importlib.util.spec_from_file_location("build", os.path.join(ROOT, "bedrock", "build.py"))
    m = importlib.util.module_from_spec(spec)
    with contextlib.redirect_stdout(io.StringIO()):
        spec.loader.exec_module(m)  # yalnız tanımlar; build() __main__ korumasında
    return m


def cell(s):
    return (s or "").replace("|", "\\|").replace("\n", " ").strip()


def mat_name(mat):
    """minecraft:slime_ball -> slime ball (tarif malzemesi, kısa)."""
    if not mat:
        return "—"
    return mat.split(":")[-1].replace("_", " ")


def table(headers, rows):
    out = ["| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(cell(c) for c in r) + " |" for r in rows]
    return "\n".join(out)


def main():
    m = load_build()
    version = ".".join(map(str, m.VERSION))
    tnts = m.TNTS
    blocks = [b for b in m.BLOCKS if not b.get("hidden") and not re.match(r"(lego_|mini_|portal_\d)", b["id"])]
    lego = [b for b in m.BLOCKS if b["id"].startswith("lego_")]
    items = m.ITEMS
    monsters = m.MONSTERS
    morphs = m.MORPHS_VANILLA + m.MORPHS_OWN
    block_morphs = m.BLOCK_MORPHS

    counts = dict(tnt=len(tnts), blok=len(m.BLOCKS), esya=len(items), canavar=len(monsters),
                  kilik=len(morphs) + len(block_morphs))

    md = [f"""---
title: Katalog
order: 20
summary: Paketteki her TNT, blok, eşya, canavar ve kılık — bedrock/build.py listelerinden üretilir.
---

> [!TLDR]
> Sürüm {version}: {counts['tnt']} TNT, {counts['blok']} blok, {counts['esya']} eşya, {counts['canavar']} canavar, {counts['kilik']} kılık. Tarifte `M` malzeme, ortadaki `T` normal TNT: 8 malzeme + 1 TNT = 1 özel TNT.

## TNT'ler {{#tnt}}

{table(["TNT", "Ne yapar", "Tarif malzemesi"],
       [(t["tr"], t.get("trtip", ""), mat_name(t.get("mat"))) for t in tnts])}

## Bloklar {{#bloklar}}

{table(["Blok", "Ne yapar"], [(b["tr"], b.get("trtip", "")) for b in blocks])}

Lego tuğlaları {len(lego)} renkte gelir ({", ".join(b["tr"].replace(" Lego Tuğla", "") for b in lego)}); Lego TNT ve Çizim Eşyası bunlardan yapı kurar.

## Eşyalar {{#esyalar}}

{table(["Eşya", "Ne yapar"], [(i["tr"], i.get("trtip", "")) for i in items])}

## Canavarlar {{#canavarlar}}

{table(["Canavar", "Can", "Hasar"], [(MONSTER_TR.get(x["id"], x["id"]), str(x.get("hp", "")), str(x.get("dmg", ""))) for x in monsters])}

## Kılıklar {{#kiliklar}}

Dönüşüm Asası ile girilen kılıklar; her biri o canlının hareketini ve gücünü verir.

{table(["Grup", "Kılıklar"],
       [(c, ", ".join(x["tr"] for x in morphs if x["cat"] == c) or ", ".join(x["tr"] for x in block_morphs if x["cat"] == c))
        for c in m.MORPH_CATS])}
"""]
    out = os.path.join(ROOT, "docs", "guide", "katalog.md")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, "w", encoding="utf-8").write("".join(md))

    # README özeti: işaretler arasını yenile
    readme = os.path.join(ROOT, "README.md")
    summary = (f"**v{version} · {counts['tnt']} TNT · {counts['blok']} blok · {counts['esya']} eşya · "
               f"{counts['canavar']} canavar · {counts['kilik']} kılık** — tam liste "
               f"[katalogda](https://mmdemirbas.github.io/super-tnt-mod/katalog.html).")
    s = open(readme, encoding="utf-8").read()
    s2 = re.sub(r"(<!-- katalog -->).*?(<!-- /katalog -->)", lambda mm: f"{mm.group(1)}\n{summary}\n{mm.group(2)}", s, flags=re.S)
    if s2 != s:
        open(readme, "w", encoding="utf-8").write(s2)
    print(f"{out}: {counts}")


if __name__ == "__main__":
    main()
