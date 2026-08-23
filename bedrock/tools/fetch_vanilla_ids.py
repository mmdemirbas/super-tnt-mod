#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Mojang/bedrock-samples'tan vanilla kimlik listelerini cekip
bedrock/tools/vanilla_ids.json dosyasina yazar.

    python3 bedrock/tools/fetch_vanilla_ids.py

NEDEN AYRI DOSYA. check_pack.py paketi bu listelere karsi dogrular: yazdigimiz
her blok / varlik / parcacik / ses / etki kimliginin vanilla'da GERCEKTEN var
oldugunu. Liste ag uzerinden her calistirmada cekilseydi dogrulama internete
bagimli olurdu ve hangi surume karsi dogrulandigi belirsiz kalirdi. Dosya
repoda durur, ne zaman yenilendigi icindeki 'fetched' alanindan bellidir.

Yenilemek gerektiginde (yeni Minecraft surumu, yeni mob/blok): bu betigi
calistir, ciktiyi commit'le, check_pack.py'yi tekrar kosur.
"""
import json
import os
import re
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

API = "https://api.github.com/repos/Mojang/bedrock-samples/git/trees/main?recursive=1"
RAW = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vanilla_ids.json")


def get_json(path):
    """Vanilla json'lari // yorumlari icerir; kati JSON'a cevirip okur."""
    text = urllib.request.urlopen(RAW + path, timeout=60).read().decode("utf-8", "replace")
    return json.loads(re.sub(r"^\s*//.*$", "", text, flags=re.M))


def main():
    tree = json.load(urllib.request.urlopen(API, timeout=60))["tree"]
    paths = [t["path"] for t in tree]

    blocks = sorted(b["name"] for b in get_json("metadata/vanilladata_modules/mojang-blocks.json")["data_items"])
    entities = sorted(e["name"] for e in get_json("metadata/vanilladata_modules/mojang-entities.json")["data_items"])
    effects = sorted(e["name"] for e in get_json("metadata/vanilladata_modules/mojang-effects.json")["data_items"])
    sounds = sorted(get_json("resource_pack/sounds/sound_definitions.json")["sound_definitions"])

    part_paths = [p for p in paths if p.startswith("resource_pack/particles/") and p.endswith(".json")]

    def particle_id(p):
        try:
            return get_json(p)["particle_effect"]["description"]["identifier"]
        except Exception:
            return None

    with ThreadPoolExecutor(16) as ex:
        particles = sorted(x for x in ex.map(particle_id, part_paths) if x)

    # Varlik gorunum varliklari: morph tablosunun dogrulandigi kaynak.
    ent_paths = [p for p in paths if p.startswith("resource_pack/entity/") and p.endswith(".entity.json")]

    def assets(p):
        try:
            d = get_json(p)["minecraft:client_entity"]["description"]
        except Exception:
            return set(), set(), set()
        return (set((d.get("geometry") or {}).values()),
                set((d.get("textures") or {}).values()),
                set((d.get("materials") or {}).values()))

    geos, texs, mats = set(), set(), set()
    with ThreadPoolExecutor(16) as ex:
        for g, t, m in ex.map(assets, ent_paths):
            geos |= g
            texs |= t
            mats |= m

    data = {
        "//": "Mojang/bedrock-samples main dalindan uretildi; fetch_vanilla_ids.py",
        "blocks": blocks,
        "entities": entities,
        "effects": effects,
        "sounds": sounds,
        "particles": particles,
        "entity_geometry": sorted(geos),
        "entity_textures": sorted(texs),
        "entity_materials": sorted(mats),
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=0, sort_keys=True)
    print(f"{OUT}: blok {len(blocks)}, varlik {len(entities)}, etki {len(effects)}, "
          f"ses {len(sounds)}, parcacik {len(particles)}, "
          f"geometry {len(geos)}, doku {len(texs)}, materyal {len(mats)}")


if __name__ == "__main__":
    sys.exit(main())
