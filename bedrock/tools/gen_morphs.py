#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""build.py'deki MORPHS tablosunu Mojang/bedrock-samples deposundan uretir.

    python3 bedrock/tools/gen_morphs.py            # tabloyu stdout'a yazar
    python3 bedrock/tools/gen_morphs.py --write    # build.py icindeki blogu degistirir

NEDEN. Morph, oyuncuyu vanilla mob'un geometry/texture/material ADLARINA
baglar; dosyalar pakete konmaz, Minecraft kendi kaynagindan verir. Ad bir harf
yanlissa model GORUNMEZ olur ve hata da vermez. Bu yuzden adlar elle yazilmaz:
tek dogru kaynak Mojang'in yayinladigi resource_pack/entity/*.entity.json
dosyalaridir, betik onlari okur.

Yeni mob eklemek: asagidaki R listesine bir satir ekle, betigi --write ile
calistir, build.py'yi kur. Secilen texture/material/geometry anahtari o mob'un
tanimda yoksa betik durur ve mevcut anahtarlari yazar.
"""
import json
import os
import re
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

REPO = "https://api.github.com/repos/Mojang/bedrock-samples/git/trees/main?recursive=1"
RAW = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/"
HERE = os.path.dirname(os.path.abspath(__file__))
BUILD_PY = os.path.join(HERE, "..", "build.py")


def fetch_vanilla_entities():
    """RP dosya adi -> {geometry, textures, materials}. Vanilla json'lari // ile
    yorum icerir (kati JSON degil) — okumadan once temizlenir."""
    tree = json.load(urllib.request.urlopen(REPO, timeout=60))["tree"]
    paths = [t["path"] for t in tree
             if t["path"].startswith("resource_pack/entity/") and t["path"].endswith(".entity.json")]

    def get(path):
        text = urllib.request.urlopen(RAW + path, timeout=60).read().decode("utf-8", "replace")
        desc = json.loads(re.sub(r"^\s*//.*$", "", text, flags=re.M))["minecraft:client_entity"]["description"]
        return path.split("/")[-1].replace(".entity.json", ""), {
            "geometry": desc.get("geometry") or {},
            "textures": desc.get("textures") or {},
            "materials": desc.get("materials") or {}}

    with ThreadPoolExecutor(16) as ex:
        return dict(ex.map(get, paths))


RP = fetch_vanilla_entities()

A = "Hayvanlar"; S = "Su"; C = "Canavarlar"; B = "Devler"
R = [
    # --- hayvanlar / dostlar
    ("allay", "allay", "Allay", A, dict(pas=("slow_falling", 0), act="float")),
    ("armadillo", "armadillo", "Armadillo", A, dict(pas=("resistance", 0))),
    ("bat", "bat", "Yarasa", A, dict(pas=("slow_falling", 0), act="float")),
    ("bee", "bee", "Arı", A, dict(pas=("slow_falling", 0), act="float")),
    ("camel", "camel", "Deve", A, dict(pas=("speed", 1))),
    ("cat", "cat", "Kedi", A, dict(tex="red", pas=("speed", 1))),
    ("chicken", "chicken", "Tavuk", A, dict(pas=("slow_falling", 0))),
    ("copper_golem", "copper_golem", "Bakır Golem", A,
     dict(pas=("resistance", 0), layers=[("eyes_default", "eyes")])),
    ("cow", "cow", "İnek", A, {}),
    ("donkey_v3", "donkey", "Eşek", A, dict(tex="donkey", pas=("speed", 1))),
    ("fox", "fox", "Tilki", A, dict(tex="red", pas=("speed", 1))),
    ("frog", "frog", "Kurbağa", A, dict(tex="temperate", pas=("jump_boost", 2))),
    ("goat", "goat", "Keçi", A, dict(pas=("jump_boost", 1))),
    ("horse_v3", "horse", "At", A, dict(tex="base_brown", pas=("speed", 2))),
    ("iron_golem", "iron_golem", "Demir Golem", A, dict(pas=("resistance", 1))),
    ("llama", "llama", "Lama", A, dict(tex="creamy")),
    ("mooshroom", "mooshroom", "Mantar İnek", A, {}),
    ("mule_v3", "mule", "Katır", A, dict(tex="mule", pas=("speed", 1))),
    ("ocelot", "ocelot", "Ocelot", A, dict(tex="wild", pas=("speed", 1))),
    ("panda", "panda", "Panda", A, {}),
    ("parrot", "parrot", "Papağan", A, dict(tex="red_blue", pas=("slow_falling", 0), act="float")),
    ("pig", "pig", "Domuz", A, {}),
    ("polar_bear", "polar_bear", "Kutup Ayısı", A, dict(pas=("resistance", 0))),
    ("rabbit", "rabbit", "Tavşan", A, dict(tex="brown", pas=("jump_boost", 2))),
    ("sheep", "sheep", "Koyun", A, {}),
    ("sniffer", "sniffer", "Koklayıcı", A, {}),
    ("snow_golem", "snow_golem", "Kardan Adam", A,
     dict(mat_parts=[("head*", "head")])),
    ("strider", "strider", "Adımlayıcı", A, dict(pas=("fire_resistance", 0))),
    ("trader_llama", "trader_llama", "Tüccar Laması", A, dict(tex="creamy")),
    ("villager_v2", "villager_v2", "Köylü", A,
     dict(tex="base", layers=[(["plains", "farmer"], "masked")])),
    ("wandering_trader", "wandering_trader", "Gezgin Tüccar", A, {}),
    ("wolf", "wolf", "Kurt", A, dict(pas=("speed", 1))),
    # --- su
    ("axolotl", "axolotl", "Axolotl", S, dict(tex="wild", pas=("water_breathing", 0))),
    ("cod", "cod", "Morina Balığı", S, dict(pas=("water_breathing", 0))),
    ("dolphin", "dolphin", "Yunus", S, dict(pas=("water_breathing", 0))),
    ("glow_squid", "glow_squid", "Işıklı Mürekkep Balığı", S, dict(pas=("water_breathing", 0))),
    ("pufferfish", "pufferfish", "Balon Balığı", S, dict(geo="large", pas=("water_breathing", 0))),
    ("salmon", "salmon", "Somon", S, dict(pas=("water_breathing", 0))),
    ("squid", "squid", "Mürekkep Balığı", S, dict(pas=("water_breathing", 0))),
    ("tadpole", "tadpole", "İribaş", S, dict(pas=("water_breathing", 0))),
    ("tropicalfish", "tropicalfish", "Tropikal Balık", S,
     dict(geo="typeA", tex="typeA", pas=("water_breathing", 0))),
    ("turtle", "turtle", "Kaplumbağa", S, dict(pas=("water_breathing", 0))),
    ("guardian", "guardian", "Muhafız", S, dict(tex="default", pas=("water_breathing", 0))),
    ("elder_guardian", "elder_guardian", "Yaşlı Muhafız", S,
     dict(tex="elder", pas=("water_breathing", 0))),
    # --- canavarlar
    ("blaze", "blaze", "Blaze", C,
     dict(mat="body", mat_parts=[("head", "head")], pas=("fire_resistance", 0), act="fireball")),
    ("bogged", "bogged", "Bataklık İskeleti", C,
     dict(layers=[("overlay", "overlay", "overlay")])),
    ("breeze", "breeze", "Esinti", C, dict(pas=("slow_falling", 0), act="float")),
    ("cave_spider", "cave_spider", "Mağara Örümceği", C, dict(pas=("jump_boost", 2))),
    ("creaking", "creaking", "Gıcırdak", C, dict(layers=[("eyes", "eyes")])),
    ("creeper", "creeper", "Creeper", C, dict(act="explode")),
    ("drowned", "drowned", "Boğulmuş", C, dict(pas=("water_breathing", 0))),
    ("enderman", "enderman", "Enderman", C, dict(act="teleport")),
    ("endermite", "endermite", "Endermit", C, {}),
    ("evocation_illager", "evocation_illager", "Evoker", C, {}),
    ("ghast", "ghast", "Ghast", C, dict(pas=("slow_falling", 0), act="fireball")),
    ("hoglin", "hoglin", "Hoglin", C, {}),
    ("husk", "husk", "Çöl Zombisi", C, {}),
    ("magma_cube", "magma_cube", "Magma Küpü", C,
     dict(pas=("fire_resistance", 0), act=None)),
    ("phantom", "phantom", "Hayalet Kuş", C, dict(pas=("slow_falling", 0), act="float")),
    ("piglin", "piglin", "Piglin", C, {}),
    ("piglin_brute", "piglin_brute", "Piglin Kabadayı", C, dict(pas=("resistance", 0))),
    ("pillager", "pillager", "Yağmacı", C, {}),
    ("ravager", "ravager", "Ravager", C, dict(pas=("resistance", 0))),
    ("shulker", "shulker", "Shulker", C, dict(tex="undyed")),
    ("silverfish", "silverfish", "Gümüş Balığı", C, {}),
    ("skeleton", "skeleton", "İskelet", C, {}),
    ("skeleton_horse_v3", "skeleton_horse", "İskelet At", C, dict(tex="skeleton", pas=("speed", 2))),
    ("slime", "slime", "Slime", C, dict(pas=("jump_boost", 2))),
    ("spider", "spider", "Örümcek", C, dict(pas=("jump_boost", 2))),
    ("stray", "stray", "Buz İskeleti", C, dict(layers=[("overlay", "overlay", "overlay")])),
    ("vex", "vex", "Vex", C, dict(pas=("slow_falling", 0), act="float")),
    ("vindicator", "vindicator", "Vindicator", C, {}),
    ("witch", "witch", "Cadı", C, {}),
    ("wither_skeleton", "wither_skeleton", "Wither İskeleti", C, dict(pas=("fire_resistance", 0))),
    ("zoglin", "zoglin", "Zoglin", C, {}),
    ("zombie", "zombie", "Zombi", C, {}),
    ("zombie_horse_v3", "zombie_horse", "Zombi At", C, dict(tex="zombie", pas=("speed", 1))),
    ("zombie_pigman", "zombie_pigman", "Zombi Piglin", C, dict(pas=("fire_resistance", 0))),
    ("zombie_villager_v2", "zombie_villager_v2", "Zombi Köylü", C, {}),
    # --- devler / patronlar
    ("warden", "warden", "Warden", B, dict(pas=("resistance", 1), act="sonic")),
    ("wither", "wither", "Wither", B,
     dict(scale=0.8, pas=("fire_resistance", 0), act="float")),
    ("ender_dragon", "ender_dragon", "Ender Ejderha", B,
     dict(scale=0.4, pas=("fire_resistance", 0), act="float")),
    ("happy_ghast", "happy_ghast", "Mutlu Ghast", B,
     dict(tex="happy_ghast", scale=0.5, pas=("slow_falling", 0), act="float")),
]

# Onceki 15 morph'un n degerleri KORUNUR: kayitli st:morph degeri olan oyuncu
# guncellemeden sonra baska bir moba donusmesin.
LEGACY = ["creeper", "zombie", "skeleton", "enderman", "iron_golem", "wolf", "pig",
          "cow", "chicken", "spider", "piglin", "allay", "wither_skeleton", "ghast", "slime"]

def pick(d, name, key, what):
    if key is None:
        if "default" in d:
            return d["default"]
        sys.exit(f"{name}: {what} icin 'default' yok, secenek: {list(d)}")
    if key not in d:
        sys.exit(f"{name}: {what} anahtari '{key}' yok, secenek: {list(d)}")
    return d[key]

rows = {}
for rpfile, key, tr, cat, o in R:
    v = RP.get(rpfile)
    if not v:
        sys.exit(f"{rpfile}: bedrock-samples'ta yok")
    geo = pick(v["geometry"], key, o.get("geo"), "geometry")
    tex = pick(v["textures"], key, o.get("tex"), "texture")
    mat = pick(v["materials"], key, o.get("mat"), "material")
    row = dict(key=key, tr=tr, cat=cat, geo=geo, tex=tex, mat=mat)
    if o.get("scale", 1.0) != 1.0:
        row["scale"] = o["scale"]
    if o.get("mat_parts"):
        row["mat_parts"] = [[b, pick(v["materials"], key, mk, "material")]
                            for b, mk in o["mat_parts"]]
    if o.get("layers"):
        ls = []
        for lay in o["layers"]:
            tk, mk = lay[0], lay[1]
            gk = lay[2] if len(lay) > 2 else None
            lt = [pick(v["textures"], key, k, "texture") for k in (tk if isinstance(tk, list) else [tk])]
            l = dict(tex=lt, mat=pick(v["materials"], key, mk, "material"))
            if gk:
                l["geo"] = pick(v["geometry"], key, gk, "geometry")
            ls.append(l)
        row["layers"] = ls
    if o.get("pas"):
        row["pas"] = list(o["pas"])
    if o.get("act"):
        row["act"] = o["act"]
    rows[key] = row

# n atamasi: once eski 15, sonra kalanlar kategori + Turkce ada gore
order = [rows[k] for k in LEGACY]
rest = [r for k, r in rows.items() if k not in LEGACY]
catorder = {A: 0, S: 1, C: 2, B: 3}
rest.sort(key=lambda r: (catorder[r["cat"]], r["tr"]))
order += rest
for i, r in enumerate(order, 1):
    r["n"] = i

def lit(r):
    p = [f'key="{r["key"]}"', f'n={r["n"]}', f'tr="{r["tr"]}"', f'cat="{r["cat"]}"',
         f'geo="{r["geo"]}"', f'tex="{r["tex"]}"', f'mat="{r["mat"]}"']
    for extra in ("scale", "mat_parts", "layers", "pas", "act"):
        if extra in r:
            p.append(f'{extra}={json.dumps(r[extra], ensure_ascii=False)}')
    body = ", ".join(p)
    out, line = [], "    dict("
    for tok in body.split(", "):
        if len(line) + len(tok) + 2 > 96:
            out.append(line.rstrip())
            line = "         "
        line += tok + ", "
    out.append(line.rstrip().rstrip(",") + "),")
    return "\n".join(out)

block = "MORPHS = [\n" + "\n".join(lit(r) for r in order) + "\n]\n"

if "--write" in sys.argv:
    src = open(BUILD_PY, encoding="utf-8").read()
    start = src.index("MORPHS = [\n")
    end = src.index("\n]\n", start) + len("\n]\n")
    open(BUILD_PY, "w", encoding="utf-8").write(src[:start] + block + src[end:])
    print(f"build.py guncellendi: {len(order)} morph", file=sys.stderr)
else:
    sys.stdout.write(block)
    print(f"# {len(order)} morph", file=sys.stderr)
