#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Uretilen Bedrock paketini denetler. Oyuna girmeden yakalanabilecek her seyi
yakalamak icin; kalan tek dogrulama tablette elle oynamak.

    python3 bedrock/build.py && python3 bedrock/tools/check_pack.py

NE YAKALAR. Bedrock hatalarin cogunu SESSIZCE yutar: olmayan bir doku adi
gorunmez model verir, olmayan bir parcacik/ses hicbir sey yapmaz, olmayan bir
blok id'si setType'ta hata firlatip try icinde kaybolur, eksik bir dil satiri
ekranda ham anahtar gosterir. Hicbiri hata mesaji uretmez, hepsi ancak oyunda
fark edilir. Bu betik onlari build zamaninda yakalar.

Vanilla kimlikleri bedrock/tools/vanilla_ids.json'dan okunur
(fetch_vanilla_ids.py ile yenilenir).
"""
import importlib.util
import json
import glob
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BEDROCK = os.path.normpath(os.path.join(HERE, ".."))
BP = os.path.join(BEDROCK, "super_tnt_BP")
RP = os.path.join(BEDROCK, "super_tnt_RP")

# .pyc YAZMA. build.py'yi disaridan yukluyoruz ve bedrock/__pycache__/ olusuyor;
# onbellek gecerliligi (mtime saniyesi + BOYUT) ile kararlastiriliyor. Mutasyon
# testi build.py'de tek karakter degistirip (boyut ayni) hemen geri aliyor -> ayni
# saniye icinde ayni boyut -> Python ESKI bytecode'u kullaniyor ve denetim artik
# diskteki dosyayi degil MUTASYONU dogruluyor. Sessiz ve yapiskan: bir sonraki
# calistirmada da surer.
sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location("stnt_build", os.path.join(BEDROCK, "build.py"))
build = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(build)          # build() sadece __main__'de calisir

VAN = json.load(open(os.path.join(HERE, "vanilla_ids.json"), encoding="utf-8"))
BLOCKS = set(VAN["blocks"])
ENTITIES = set(VAN["entities"])
# Etki kimlikleri vanilla verisinde "minecraft:speed" olarak duruyor; betikte
# ve addEffect cagrilarinda kisa ad kullaniliyor. Iki bicimi de kabul et.
EFFECTS = {e.split(":", 1)[-1] for e in VAN["effects"]}
SOUNDS = set(VAN["sounds"])
PARTICLES = set(VAN["particles"])
GEOS = set(VAN["entity_geometry"])
TEXS = set(VAN["entity_textures"])
MATS = set(VAN["entity_materials"])
# Blok kiligi morph'lari mob dokusu degil BLOK dokusu kullanir.
BLOCK_TEXS = set(VAN["block_textures"])

# Paketin KENDI varliklari ve modelleri: morph tablosunda bunlar da gecebilir,
# vanilla listelerinde aranmamali.
OWN_ENTITIES = {f"stnt:{m['id']}" for m in build.MONSTERS}
OWN_BLOCKS = {"stnt:" + os.path.basename(p)[:-5]
              for p in glob.glob(os.path.join(BP, "blocks", "*.json"))}
# Ad kalibi degil, DOSYA: RP'de gercekten duran geometry kimlikleri ve doku
# yollari. Eskiden kalip ("geometry.<mob id>") kabul ediliyordu, yani ship
# edilmeyen bir model de denetimden geciyordu.
OWN_GEOS = {g["description"]["identifier"]
            for p in glob.glob(os.path.join(RP, "models", "entity", "*.geo.json"))
            for g in json.load(open(p, encoding="utf-8"))["minecraft:geometry"]}
OWN_TEXS = {os.path.relpath(p, RP)[:-4].replace(os.sep, "/")
            for p in glob.glob(os.path.join(RP, "textures", "entity", "**", "*.png"),
                               recursive=True)}
# Vanilla'da tanimli ama hicbir vanilla varligin kullanmadigi materyaller —
# vanilla entity tanimlarindan toplanan listede gorunmezler.
EXTRA_MATS = {"entity_emissive_alpha"}

# Switch'te case'i olmayan, esya id'sine bakan pasif eylem turleri.
PASSIVE_ACTIONS = {"held_fireproof", "bleed", "heavy", "worn_wool"}
# itemCompleteUse'da (switch disinda) uygulanan yenen/icilen turler.
CONSUMED_ACTIONS = {"eat", "heal_boost"}

FAILS = []
CHECKS = [0]


def check(ok, msg):
    CHECKS[0] += 1
    if not ok:
        FAILS.append(msg)
    return ok


def jload(path):
    return json.load(open(path, encoding="utf-8"))


def lang(code):
    out = {}
    for line in open(os.path.join(RP, "texts", code + ".lang"), encoding="utf-8"):
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


# ---------------------------------------------------------------- 1. JSON / JS
def check_parse():
    bad = []
    for root in (BP, RP):
        for dirpath, _dirs, files in os.walk(root):
            for f in files:
                if f.endswith(".json"):
                    p = os.path.join(dirpath, f)
                    try:
                        jload(p)
                    except Exception as e:
                        bad.append(f"{os.path.relpath(p, BEDROCK)}: {e}")
    check(not bad, "bozuk JSON: " + "; ".join(bad[:5]))

    main_js = os.path.join(BP, "scripts", "main.js")
    check(os.path.exists(main_js), "scripts/main.js yok")
    node = shutil.which("node")
    if node:
        r = subprocess.run([node, "--check", main_js], capture_output=True, text=True)
        check(r.returncode == 0, f"main.js sozdizimi: {r.stderr.strip()[:200]}")
    else:
        print("  not: node bulunamadi, main.js sozdizimi denetlenmedi")


def check_manifest():
    for pack, label in ((BP, "BP"), (RP, "RP")):
        mf = jload(os.path.join(pack, "manifest.json"))
        has_script = any(m.get("type") == "script" for m in mf.get("modules", []))
        for m in mf.get("modules", []):
            if m.get("type") == "script":
                check(bool(m.get("language")),
                      f"{label}: script modulunde 'language' yok -> paket sessizce reddedilir")
        if has_script:
            check("script_eval" in (mf.get("capabilities") or []),
                  f"{label}: script var ama capabilities'te script_eval yok")
        check(mf["header"]["version"] == build.VERSION,
              f"{label}: manifest surumu {mf['header']['version']} != build.VERSION {build.VERSION}")


# ---------------------------------------------------------------- 2. item'lar
def check_items(js):
    tr, en = lang("tr_TR"), lang("en_US")
    itex = jload(os.path.join(RP, "textures", "item_texture.json"))["texture_data"]
    handled = set(re.findall(r'^      case "([a-z_]+)": \{', js, re.M))
    for it in build.ITEMS:
        iid = it["id"]
        p = os.path.join(BP, "items", f"{iid}.json")
        if not check(os.path.exists(p), f"item {iid}: BP/items/{iid}.json yok"):
            continue
        d = jload(p)["minecraft:item"]
        check(d["description"]["identifier"] == f"stnt:{iid}", f"item {iid}: identifier yanlis")
        icon = d["components"].get("minecraft:icon")
        check(icon in itex, f"item {iid}: ikon '{icon}' item_texture.json'da yok")
        if icon in itex:
            rel = itex[icon]["textures"]
            check(os.path.exists(os.path.join(RP, rel + ".png")), f"item {iid}: doku dosyasi yok ({rel}.png)")
        check(f"item.stnt:{iid}.name" in tr, f"item {iid}: tr_TR ad satiri yok")
        check(f"item.stnt:{iid}.name" in en, f"item {iid}: en_US ad satiri yok")
        check(f"stnt.tip.{iid}" in tr, f"item {iid}: tr_TR ipucu yok")
        check(f"stnt.tip.{iid}" in en, f"item {iid}: en_US ipucu yok")
        act = it.get("action")
        if act:
            t = act["type"]
            if t in PASSIVE_ACTIONS:
                # Bu turler switch'te degil, esyanin id'sine bakan dongulerde
                # uygulanir (elde tutunca / giyince surekli etki).
                check(f'"stnt:{iid}"' in js, f"item {iid}: '{t}' hicbir yerde uygulanmiyor")
            else:
                check(t in handled or t in CONSUMED_ACTIONS,
                      f"item {iid}: '{t}' eylemini karsilayan case main.js'te yok")


def check_tnts():
    tr, en = lang("tr_TR"), lang("en_US")
    for t in build.TNTS:
        tid = t["id"]
        check(os.path.exists(os.path.join(BP, "blocks", f"{tid}.json")), f"TNT {tid}: blok json yok")
        check(f"tile.stnt:{tid}.name" in tr, f"TNT {tid}: tr_TR ad satiri yok")
        check(f"tile.stnt:{tid}.name" in en, f"TNT {tid}: en_US ad satiri yok")
        check(f"stnt.tip.{tid}" in tr, f"TNT {tid}: tr_TR ipucu yok")


# ---------------------------------------------------------------- 3. morph
def js_const(js, name):
    for line in js.splitlines():
        if line.startswith(f"const {name} = "):
            body = line[len(f"const {name} = "):]
            body = body.split(";   //")[0].split(";       //")[0].split(";     //")[0]
            return json.loads(body.rstrip().rstrip(";"))
    raise AssertionError(f"main.js'te const {name} yok")


def check_morphs(js):
    morphs = build.MORPHS
    ns = [m["n"] for m in morphs]
    check(len(set(ns)) == len(ns), "morph: n degerleri benzersiz degil")
    check(sorted(ns) == list(range(1, len(morphs) + 1)), "morph: n degerleri 1..N araligi degil")
    check(len({m["key"] for m in morphs}) == len(morphs), "morph: key degerleri benzersiz degil")

    # 3a. mob typeId'leri ve gorsel varliklarin vanilla'da olmasi
    for m in morphs:
        k = m["key"]
        block_morph = "blocks" in m
        if block_morph:
            # Blok kiligi: arkasinda bir mob yok, KIRILAN BLOK var.
            for bid in m["blocks"]:
                check(bid in BLOCKS or bid in OWN_BLOCKS,
                      f"morph {k}: '{bid}' diye bir blok yok")
            check(m["cat"] == "Bloklar", f"morph {k}: blok kiligi 'Bloklar' kategorisinde olmali")
            # Bir kup TEK doku ornekler; ust ve alt yuz ayri gecis ister.
            # Yoksa cim blogunun ustu yan dokusuyla cizilir (v1.40.0 hatasi).
            check([lay.get("geo") for lay in m.get("layers", [])]
                  == [build.BLOCK_GEO_TOP, build.BLOCK_GEO_BOTTOM],
                  f"morph {k}: blok kiliginin ust/alt yuz gecisi yok "
                  "— ustu yan dokusuyla cizilir")
        else:
            tid = f"{m.get('ns', 'minecraft')}:{k}"
            check(tid in ENTITIES or tid in OWN_ENTITIES, f"morph {k}: '{tid}' diye bir varlik yok")
            if tid in OWN_ENTITIES:
                check(os.path.exists(os.path.join(BP, "entities", f"{k}.json")),
                      f"morph {k}: paketin kendi mob'u ama BP/entities/{k}.json yok")
        # Vanilla'da VARSA Minecraft verir; yoksa RP'de ship EDILMIS olmali.
        # Ikisi de degilse model/doku sessizce gorunmez olur.
        check(m["geo"] in GEOS or m["geo"] in OWN_GEOS,
              f"morph {k}: geometry '{m['geo']}' ne vanilla'da var ne RP'de ship ediliyor")
        def tex_var(t):
            """Doku ya vanilla'da olmali ya pakette ship edilmeli. Blok kiliginda
            aranan liste FARKLIDIR: mob dokulari entity_textures'ta, blok
            dokulari block_textures'ta durur."""
            return (t in TEXS or t in OWN_TEXS
                    or (block_morph and (t in BLOCK_TEXS
                                         or os.path.exists(os.path.join(RP, t + ".png")))))
        check(tex_var(m["tex"]),
              f"morph {k}: doku '{m['tex']}' ne vanilla'da var ne RP'de ship ediliyor")
        check(m["mat"] in MATS or m["mat"] in EXTRA_MATS, f"morph {k}: materyal '{m['mat']}' yok")
        for i, lay in enumerate(m.get("layers", [])):
            for t in lay["tex"]:
                check(tex_var(t),
                      f"morph {k} katman {i}: doku '{t}' ne vanilla'da var ne RP'de ship ediliyor")
            check(lay["mat"] in MATS or lay["mat"] in EXTRA_MATS,
                  f"morph {k} katman {i}: materyal '{lay['mat']}' yok")
            if "geo" in lay:
                check(lay["geo"] in GEOS or lay["geo"] in OWN_GEOS,
                      f"morph {k} katman {i}: geometry '{lay['geo']}' yok")
        for _bone, mat in m.get("mat_parts", []):
            check(mat in MATS, f"morph {k}: parca materyali '{mat}' vanilla'da yok")
        check(m["cat"] in build.MORPH_CATS, f"morph {k}: bilinmeyen kategori '{m['cat']}'")

    # 3b. BP player.json olaylari ve ozellik araligi
    bp = jload(os.path.join(BP, "entities", "player.json"))["minecraft:entity"]
    rng = bp["description"]["properties"]["st:morph"]["range"]
    check(rng == [0, len(morphs)], f"player.json st:morph araligi {rng}, beklenen [0, {len(morphs)}]")
    for m in morphs:
        check(f"st:morph_{m['key']}" in bp["events"], f"morph {m['key']}: BP olayi yok")
    check("st:morph_human" in bp["events"], "st:morph_human olayi yok")

    # 3c. RP render controller'lari: her referans cozulmeli, bosta duran olmamali
    rp = jload(os.path.join(RP, "entity", "player.json"))["minecraft:client_entity"]["description"]
    rc = jload(os.path.join(RP, "render_controllers", "morph.render_controllers.json"))["render_controllers"]
    for name, body in rc.items():
        check(body["geometry"].split(".", 1)[1] in rp["geometry"], f"{name}: geometry kaydi yok")
        for t in body["textures"]:
            check(t.split(".", 1)[1] in rp["textures"], f"{name}: doku kaydi yok ({t})")
        for mp in body["materials"]:
            for _bone, mv in mp.items():
                check(mv.split(".", 1)[1] in rp["materials"], f"{name}: materyal kaydi yok ({mv})")
    listed = {n for e in rp["render_controllers"] for n in e}
    for n in listed:
        if n.startswith("controller.render.morph."):
            check(n in rc, f"player.json '{n}' controller'ini istiyor ama tanimli degil")
    for n in rc:
        check(n in listed, f"'{n}' tanimli ama player.json'da kullanilmiyor")
    # her morph icin ilk/ucuncu sahis cifti
    for m in morphs:
        for slot, *_ in build.morph_passes(m):
            for view in ("first_person", "third_person"):
                check(f"controller.render.morph.{m['key']}{slot}.{view}" in rc,
                      f"morph {m['key']}{slot}: {view} controller'i yok")

    # 3d. betikteki tablolar
    mm = js_const(js, "MORPH_MAP")
    mobs = [m for m in morphs if "blocks" not in m]
    blockms = [m for m in morphs if "blocks" in m]
    check(len(mm) == len(mobs), f"MORPH_MAP {len(mm)} girdi, mob morph sayisi {len(mobs)}")

    # Blok kiligi tablosu: her kiligin en az bir blogu, her blogun tek kiligi.
    bmm = js_const(js, "BLOCK_MORPH_MAP")
    check(set(bmm.values()) == {f"st:morph_{m['key']}" for m in blockms},
          "BLOCK_MORPH_MAP ile blok kiliklari ayni kumeyi vermiyor")
    check(len(bmm) == sum(len(m["blocks"]) for m in blockms),
          "BLOCK_MORPH_MAP'te blok sayisi tutmuyor (ayni blok iki kiliga mi bagli?)")
    check(os.path.exists(os.path.join(RP, "models", "entity", "block_morph.geo.json")),
          "blok kiligi modeli RP'de ship edilmiyor")
    forms = js_const(js, "MORPH_FORMS")
    menu = [i["ev"] for g in forms for i in g["items"]]
    check(len(menu) == len(morphs), f"menude {len(menu)} mob var, {len(morphs)} olmali")
    check(len(set(menu)) == len(menu), "menude ayni mob birden fazla")
    check(set(menu) == {f"st:morph_{m['key']}" for m in morphs}, "menu ile MORPHS ayni kumeyi vermiyor")
    check([g["cat"] for g in forms] == build.MORPH_CATS, "menu kategori sirasi MORPH_CATS ile ayni degil")

    abil = js_const(js, "MORPH_ABIL")
    acts = set(re.findall(r'if \(act === "([a-z_]+)"\)', js))
    for n, ab in abil.items():
        check(int(n) in ns, f"MORPH_ABIL {n}: boyle bir morph yok")
        if "pas" in ab:
            check(ab["pas"][0] in EFFECTS, f"MORPH_ABIL {n}: '{ab['pas'][0]}' diye bir etki yok")
        if "act" in ab:
            check(ab["act"] in acts, f"MORPH_ABIL {n}: '{ab['act']}' yetenegini karsilayan dal morphAct'te yok")
        check("pow" not in ab or "act" in ab, f"MORPH_ABIL {n}: 'pow' var ama 'act' yok — guc bir yere baglanmiyor")

    # Donusum ipucu: cocuk hangi gucu aldigini eylem cubugunda gorur.
    hint = js_const(js, "MORPH_HINT")
    for m in morphs:
        ev = f"st:morph_{m['key']}"
        check(ev in hint, f"morph {m['key']}: MORPH_HINT girdisi yok")
        t = hint.get(ev, "")
        check(m["tr"] in t, f"morph {m['key']}: ipucunda adi gecmiyor ({t!r})")
        if "act" in m:
            check(build.ACT_TIP[m["act"]] in t, f"morph {m['key']}: ipucu '{m['act']}' yetenegini yazmiyor")
        if "pas" in m:
            check(build.PAS_TIP[m["pas"][0]] in t, f"morph {m['key']}: ipucu pasif etkiyi yazmiyor")


# ---------------------------------------------------------------- 4. kimlikler
def check_ids(js):
    def used(pattern):
        return set(re.findall(pattern, js))

    # spawnParticle'a dogrudan verilenler VE spray() yardimcisina gecirilenler.
    # spray'e gecirilenler bir sure denetlenmiyordu: minecraft:portal_particle ve
    # minecraft:end_rod diye parcacik yok, dokuz cagri sessizce bos donuyordu.
    for pid in used(r'spawnParticle\(\s*"([^"]+)"') | used(r'spray\([^;]*?"(minecraft:[a-z_0-9]+)"'):
        check(pid in PARTICLES, f"main.js: '{pid}' diye bir parcacik yok")
    for sid in used(r'playSound\(\s*"([^"]+)"'):
        check(sid in SOUNDS, f"main.js: '{sid}' diye bir ses yok")
    for eid in used(r'addEffect\(\s*"([a-z_]+)"'):
        check(eid in EFFECTS, f"main.js: '{eid}' diye bir etki yok")
    for bid in used(r'setType\(\s*"(minecraft:[a-z_0-9]+)"'):
        check(bid in BLOCKS, f"main.js: '{bid}' diye bir blok yok")
    for eid in used(r'spawnEntity\(\s*"(minecraft:[a-z_0-9]+)"'):
        check(eid in ENTITIES, f"main.js: '{eid}' diye bir varlik yok")

    # Veri tablolarindaki kimlikler. main.js'e duz metin olarak degil, JSON
    # tablosu icinde gomulu gidiyorlar; cagri desenine bakan denetim gormez.
    for row in build.TNTS + build.ITEMS:
        for val in row.values():
            if not isinstance(val, dict):
                continue
            if isinstance(val.get("particle"), str):
                check(val["particle"] in PARTICLES,
                      f"{row.get('id')}: '{val['particle']}' diye bir parcacik yok")
            if isinstance(val.get("sound"), str):
                check(val["sound"] in SOUNDS, f"{row.get('id')}: '{val['sound']}' diye bir ses yok")

    # kaynak tablolar
    for logid, leafid in build.TREE_WOOD.values():
        check(logid in BLOCKS, f"TREE_WOOD: '{logid}' diye bir blok yok")
        check(leafid in BLOCKS, f"TREE_WOOD: '{leafid}' diye bir blok yok")
    for sap in build.TREE_WOOD:
        check(sap in BLOCKS, f"TREE_WOOD: '{sap}' diye bir fidan blogu yok")
    # Paletlerde paketin KENDI bloklari da gecebilir (lego_tnt gibi) — onlari
    # uretilen BP/blocks klasorunden dogrula.
    own = {f"stnt:{f[:-5]}" for f in os.listdir(os.path.join(BP, "blocks")) if f.endswith(".json")}
    for t in build.TNTS:
        eff = t["effect"]
        for b in ([eff["block"]] if eff.get("block") else []) + list(eff.get("palette") or []):
            check(b in BLOCKS or b in own, f"TNT {t['id']}: '{b}' diye bir blok yok")
    for it in build.ITEMS:
        a = it.get("action") or {}
        if a.get("effect"):
            check(a["effect"] in EFFECTS, f"item {it['id']}: '{a['effect']}' diye bir etki yok")


# --------------------------------------------------- 4c. RP client entity'leri
def check_client_entities():
    """RP/entity/*.json'daki geometry / doku / animasyon adlari cozuluyor mu?

    Paketin kendi mob'lari icin kimlikler stnt_ onekiyle yeniden adlandiriliyor
    (ozgun paket de kuruluysa ad cakismasin diye). Tek bir yerde degistirmeyi
    unutmak modeli ya da animasyonu sessizce yok eder.
    """
    shipped_anims = set()
    for p in glob.glob(os.path.join(RP, "animations", "*.json")):
        shipped_anims |= set(jload(p).get("animations", {}))
    for p in sorted(glob.glob(os.path.join(RP, "entity", "*.json"))):
        name = os.path.basename(p)
        if name == "player.json":
            continue                      # morph tablosu 3d'de ayrica denetleniyor
        d = jload(p)["minecraft:client_entity"]["description"]
        for slot, gid in d.get("geometry", {}).items():
            check(gid in GEOS or gid in OWN_GEOS,
                  f"{name} geometry.{slot}: '{gid}' ne vanilla'da var ne RP'de ship ediliyor")
        for slot, tex in d.get("textures", {}).items():
            check(tex in TEXS or tex in OWN_TEXS,
                  f"{name} textures.{slot}: '{tex}' ne vanilla'da var ne RP'de ship ediliyor")
        for slot, mat in d.get("materials", {}).items():
            check(mat in MATS or mat in EXTRA_MATS, f"{name} materials.{slot}: '{mat}' yok")
        for slot, aid in d.get("animations", {}).items():
            # Vanilla animasyon adlarinin listesi elimizde yok; kendi
            # onekimizle baslayanlar ship EDILMIS olmali.
            if aid.startswith("animation.stnt_"):
                check(aid in shipped_anims,
                      f"{name} animations.{slot}: '{aid}' RP'de ship edilmiyor")
        for slot, sid in d.get("sound_effects", {}).items():
            check(sid in SOUNDS, f"{name} sound_effects.{slot}: '{sid}' diye bir ses yok")
        for slot, pid in d.get("particle_effects", {}).items():
            check(pid in PARTICLES, f"{name} particle_effects.{slot}: '{pid}' diye bir parcacik yok")
        # scripts.animate icindeki her ad animations tablosunda tanimli mi?
        for step in d.get("scripts", {}).get("animate", []):
            key = list(step)[0] if isinstance(step, dict) else step
            check(key in d.get("animations", {}),
                  f"{name} scripts.animate: '{key}' animations tablosunda yok")


# ------------------------------------------------ 4d. paket butunlugu
# Ayni gorunmesi BILEREK istenen dokular. Kilik degistirme bir ozellik:
# Gizli TNT'nin ipucu "Cam kiliginda" diyor, Yanlis Altin Plaka'nin ipucu
# "Altin plaka gibi gorunur" diyor. Listede olmayan bir tekrar hatadir.
DISGUISES = [
    {"stnt_gizli_tnt", "stnt_cam_tnt"},
    {"stnt_right_golden_plate", "stnt_wrong_golden_plate"},
]


def check_pack_integrity():
    import hashlib

    # (a) Envanter ikonlari benzersiz olmali. Ayni ikon iki esyada = cocuk
    # hangisinin ne oldugunu ayirt edemez (bir kez yasandi: on pastel kare).
    seen = {}
    for p in sorted(glob.glob(os.path.join(RP, "textures", "items", "*.png"))):
        h = hashlib.md5(open(p, "rb").read()).hexdigest()
        name = os.path.basename(p)[:-4]
        check(h not in seen, f"ikon tekrari: {name} ile {seen.get(h)} ayni goruntu")
        seen.setdefault(h, name)

    # (b) Blok dokulari: ayni goruntu yalniz AYNI blogun yuzleri arasinda ya da
    # bilerek kilik degistirenlerde olabilir.
    faces = {}
    for p in sorted(glob.glob(os.path.join(RP, "textures", "blocks", "*.png"))):
        h = hashlib.md5(open(p, "rb").read()).hexdigest()
        faces.setdefault(h, []).append(os.path.basename(p)[:-4])
    for group in faces.values():
        if len(group) < 2:
            continue
        base = {re.sub(r"_(top|bottom|side)$", "", g) for g in group}
        if len(base) < 2:
            continue                      # ayni blogun iki yuzu — normal
        allowed = any(base <= d for d in DISGUISES)
        # Yalnizca YAN yuz cakismasi ciddidir: envanterde ve dunyada gorulen o.
        sides = {g for g in group if g.endswith("_side")}
        check(allowed or len(sides) < 2,
              f"iki blogun yan yuzu ayni: {sorted(sides)} — envanterde ayirt edilemez")

    # (c) Butun PNG'ler gecerli mi
    for p in glob.glob(os.path.join(RP, "textures", "**", "*.png"), recursive=True):
        d = open(p, "rb").read()
        check(d[:8] == b"\x89PNG\r\n\x1a\x0a"[:8] and d[12:16] == b"IHDR",
              f"bozuk PNG: {os.path.relpath(p, RP)}")

    # (d) Dil dosyalari: tekrar eden anahtar sessizce sonuncuyu kazandirir;
    # iki dil arasindaki fark oyunda ham anahtar gosterir.
    langs = {}
    for lang in ("tr_TR", "en_US"):
        ks = []
        for line in open(os.path.join(RP, "texts", f"{lang}.lang"), encoding="utf-8"):
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                ks.append(line.split("=", 1)[0])
        for k in {k for k in ks if ks.count(k) > 1}:
            check(False, f"{lang}.lang: '{k}' anahtari birden fazla")
        langs[lang] = set(ks)
    for k in sorted(langs["tr_TR"] - langs["en_US"]):
        check(False, f"en_US.lang'da eksik anahtar: {k}")
    for k in sorted(langs["en_US"] - langs["tr_TR"]):
        check(False, f"tr_TR.lang'da eksik anahtar: {k}")

    # (e) Ayni gorunen ad: envanterde iki farkli sey ayni yaziyi tasimamali.
    for lang in ("tr_TR", "en_US"):
        names = {}
        for line in open(os.path.join(RP, "texts", f"{lang}.lang"), encoding="utf-8"):
            if line.startswith(("item.", "tile.")) and ".name=" in line:
                k, v = line.strip().split("=", 1)
                check(v not in names, f"{lang}: '{v}' adi hem {names.get(v)} hem {k}")
                names.setdefault(v, k)

    # (f) Manifest capraz referanslari. BP'nin bagimlilik uuid'i RP'nin header
    # uuid'i degilse Minecraft kaynak paketini HIC yuklemez ve her sey mor-siyah
    # kare olur; surumler ayrisirsa "bagimlilik bulunamadi" der.
    bpm = jload(os.path.join(BP, "manifest.json"))
    rpm = jload(os.path.join(RP, "manifest.json"))
    deps = [d for d in bpm.get("dependencies", []) if "uuid" in d]
    check(any(d["uuid"] == rpm["header"]["uuid"] for d in deps),
          "BP bagimliligi RP header uuid'ini gostermiyor")
    for d in deps:
        if d["uuid"] == rpm["header"]["uuid"]:
            check(d["version"] == rpm["header"]["version"],
                  f"BP bagimlilik surumu {d['version']} != RP header {rpm['header']['version']}")
    uuids = [bpm["header"]["uuid"], rpm["header"]["uuid"]] + \
            [m["uuid"] for m in bpm["modules"] + rpm["modules"]]
    check(len(uuids) == len(set(uuids)), "manifest uuid'lerinden ikisi ayni")

    # (g) Yaratici menu grubu. Grubu olmayan bir esya menude HICBIR YERDE
    # gorunmez — cocuk yeni esyayi bulamaz ve "eklenmemis" sanir. Grup adinin
    # dil karsiligi da olmali, yoksa sekme ham anahtar yazar.
    groups = set()
    for p in glob.glob(os.path.join(BP, "items", "*.json")) + glob.glob(os.path.join(BP, "blocks", "*.json")):
        kind = "minecraft:item" if os.sep + "items" + os.sep in p else "minecraft:block"
        desc = jload(p)[kind]["description"]
        mc = desc.get("menu_category")
        check(bool(mc and mc.get("group")),
              f"{os.path.basename(p)}: yaratici menu grubu yok, menude gorunmez")
        if mc and mc.get("group"):
            groups.add(mc["group"])
    for lang in ("tr_TR", "en_US"):
        text = open(os.path.join(RP, "texts", f"{lang}.lang"), encoding="utf-8").read()
        for g in sorted(groups):
            check(f"{g}=" in text, f"{lang}.lang: '{g}' grup adinin karsiligi yok")

    # (h) Tarif kimlikleri gercek mi
    own = {"stnt:" + os.path.basename(p)[:-5]
           for p in glob.glob(os.path.join(BP, "items", "*.json")) +
                    glob.glob(os.path.join(BP, "blocks", "*.json"))}
    for p in sorted(glob.glob(os.path.join(BP, "recipes", "*.json"))):
        d = jload(p)
        r = d.get("minecraft:recipe_shaped") or d.get("minecraft:recipe_shapeless") or {}
        ids = []
        res = r.get("result")
        if isinstance(res, dict):
            ids.append(res.get("id") or res.get("item"))
        for v in (r.get("key") or {}).values():
            ids.append(v.get("item") if isinstance(v, dict) else v)
        for i in ids:
            if not i:
                continue
            check(i in own or i in BLOCKS or i.startswith("minecraft:"),
                  f"{os.path.basename(p)}: '{i}' diye bir sey yok")


# ------------------------------------------------------- 4b. ipucu sozlesmesi
def check_tooltip_contract(js):
    """CLAUDE.md: ipucu bir sozlesmedir. Yeni esyalarin ipucundaki sayilar
    davranistan turetilmis olmali, elle yazilmis olmamali."""
    # "N blok yaricap" yazan her ipucu gercek yaricapi yazmali. Dort TNT'de
    # metin kodun ilerisindeydi (30 yaziyordu, kod 14/18/20 yapiyordu) — cocuk
    # ipucuna gore konumlaniyor, bu yuzden sayi sozlesmenin parcasi.
    rad = re.compile(r"(\d+)\s*blok\s*yarıçap", re.I)
    for row in build.TNTS + build.ITEMS:
        spec = row.get("effect") or row.get("action") or {}
        for m in rad.finditer(row.get("trtip", "")):
            check(int(m.group(1)) == spec.get("radius"),
                  f"{row['id']}: ipucu {m.group(1)} blok yaricap diyor, kod {spec.get('radius')}")

    # --- 2026-08-26 denetimi: 70 TNT'nin ipucu ile kodu karsilastirildi, dokuz
    #     yerde ayrildilar. Her bulgu tek TNT'ye degil, GENEL bir kurala
    #     cevrildi ki ayni tuzaga yeni bir TNT dusmesin.
    BLOK_YAZAN = {"transform", "place", "freeze", "break", "tunnel"}
    for t in build.TNTS:
        e = t.get("effect") or {}
        tr, en, tid = t["trtip"], t["entip"], t["id"]

        # (1) "Söndürür" diyen TNT ates blogunu GERCEKTEN degistirmeli.
        #     onlyAir tek basina ates blogunu atliyordu: soz verilen tek is
        #     yapilmiyordu, cocuk atesin uzerine atip hicbir sey olmuyordu.
        if "söndür" in tr:
            check(e.get("kind") == "place" and (e.get("douses") or not e.get("onlyAir")),
                  f"{tid}: ipucu atesi sondurdugunu soyluyor, kod ates blogunu atliyor")

        # (2) Hava olayi TUM DUNYAYI etkiler — haritanin obur ucundeki kardes
        #     de yagmurda kalir. Sure kisa olmali ve ipucunda yazmali.
        if e.get("weather"):
            check(e.get("weatherTicks", 24000) <= 2400,
                  f"{tid}: {e.get('weatherTicks')} tick hava cok uzun, tum dunyayi etkiliyor")
            check("dakika" in tr and "minute" in en,
                  f"{tid}: dunya havasini degistiriyor ama ipucu suresini yazmiyor")

        # (3) Yildirim yakar ve oldurur. Ipucu bunu yazmadan cagiramaz.
        if e.get("entity") == "minecraft:lightning_bolt" or e.get("weather") == "Thunder":
            check("yakar" in tr and "öldür" in tr,
                  f"{tid}: yildirim cagiriyor ama ipucu yakma/olum riskini yazmiyor")
            check("burns" in en and "kill" in en,
                  f"{tid}: ingilizce ipucu yildirim riskini yazmiyor")

        # (4) End kristali vurulunca buyuk patlar — ipucu uyarmali.
        if e.get("entity") == "minecraft:ender_crystal":
            check("vurursan" in tr, f"{tid}: kristalin patladigi ipucunda yazmiyor")
            check("hitting" in en, f"{tid}: ingilizce ipucu kristal patlamasini yazmiyor")

        # (5) Aninda olduren TNT'de "patlatan haric" iddiasi kodla ayni olmali.
        #     Kodda varsayilan patlatani AYIRIR (exceptIgniter !== false).
        if e.get("kind") == "instakill":
            ayirir = e.get("exceptIgniter") is not False
            check(ayirir == ("hariç" in tr),
                  f"{tid}: kod patlatani {'ayiriyor' if ayirir else 'ayirmiyor'}, ipucu tersini soyluyor")
            check(ayirir == ("except" in en),
                  f"{tid}: ingilizce ipucu patlatan istisnasini yanlis yaziyor")

        # (6) "Çevirir / boyar / kaplar" blok yazan bir is vaat eder. Cizgi TNT
        #     "her yeri deftere cevirir" diyordu ama yalnizca esya sacyordu.
        if any(w in tr for w in ("çevirir", "boyar", "kaplar")):
            check(e.get("kind") in BLOK_YAZAN,
                  f"{tid}: ipucu blok degistirmeyi vaat ediyor ama kind={e.get('kind')} blok yazmiyor")

        # (7) Ipucu ayri bir SES vaat ediyorsa o ses calinmali. Paket her TNT'de
        #     ayni patlama sesini calar; "cingirak sesiyle" bosa cikiyordu.
        if "sesiyle" in tr:
            check(e.get("sound"), f"{tid}: ipucu ayri bir ses vaat ediyor ama spec'te sound yok")

        # (8) Bedrock'ta "Snow" diye bir hava tipi YOK; kar yalnizca soguk
        #     biyomda yagisin gorunusudur. "Kar yağar" diyen ipucu yalan olur.
        check("kar yağar" not in tr, f"{tid}: Bedrock'ta Snow hava tipi yok, kar sozu verilemez")

        # (9) Iki dilin kapsami ayni olmali: TR "herkese" derken EN "players"
        #     diyorsa biri yaniltiyor demektir.
        if e.get("kind") == "heal":
            check("herkese" not in tr,
                  f"{tid}: kod yalnizca oyunculara veriyor, TR ipucu 'herkese' diyor")

    for it in build.ITEMS:
        a = it.get("action") or {}
        tip = it["trtip"]
        if a.get("type") == "heal_boost":
            check(str(a["hp"]) in tip, f"item {it['id']}: ipucu {a['hp']} canini yazmiyor")
            if not a.get("forever"):
                check(str(a["seconds"] // 60) in tip, f"item {it['id']}: ipucu sureyi yazmiyor")
        if a.get("type") == "heal_boost":
            # Bedrock'ta efekt amplifier tavani 255. Ustune cikan bir can
            # hedefi sessizce hic uygulanmaz — esya bosa tiklanir.
            check(a["amp"] <= 255,
                  f"item {it['id']}: amplifier {a['amp']} > 255, efekt uygulanmaz")
            check(build.POTION_BASE_HP + 4 * (a["amp"] + 1) == a["hp"],
                  f"item {it['id']}: amp {a['amp']} -> "
                  f"{build.POTION_BASE_HP + 4 * (a['amp'] + 1)} can, ipucu {a['hp']} diyor")
            if a.get("forever"):
                # "Suresiz" bir SOZ: efektin kendisi sonsuz olamaz, tazeleyen
                # bir dongu olmali. Yoksa ipucu yalan soyler.
                check(a["seconds"] == 0, f"item {it['id']}: forever ama seconds {a['seconds']}")
                check("Süresi yok" in tip or "hiç bitmez" in tip,
                      f"item {it['id']}: ipucu suresiz oldugunu yazmiyor")
                check("stnt:hpforever" in js and "FOREVER_PROP" in js,
                      f"item {it['id']}: suresiz isareti betikte yok")
                check(js.count("getDynamicProperty(FOREVER_PROP)") >= 1,
                      f"item {it['id']}: suresiz can tazeleme dongusu yok")
                # Geri alma yolu ipucunda yazmali ve kodda GERCEKTEN olmali.
                check("Temizleyici" in tip, f"item {it['id']}: ipucu geri alma yolunu yazmiyor")
                check(js.count("setDynamicProperty(FOREVER_PROP, undefined)") >= 1,
                      f"item {it['id']}: isareti silen hicbir yol yok — geri alinamaz")
        if it.get("unbreakable"):
            comps = jload(os.path.join(BP, "items", f"{it['id']}.json"))["minecraft:item"]["components"]
            check("minecraft:durability" not in comps,
                  f"item {it['id']}: kirilmaz denmis ama dayaniklilik bileseni duruyor")
            check("kırılmaz" in tip or "eskimez" in tip,
                  f"item {it['id']}: ipucu kirilmaz oldugunu yazmiyor")
        if a.get("type") == "rename":
            check(str(a["maxLen"]) in tip, f"item {it['id']}: ipucu {a['maxLen']} harf sinirini yazmiyor")
        if a.get("type") == "sonic":
            check(str(a["range"]) in tip, f"item {it['id']}: ipucu {a['range']} blok menzili yazmiyor")
        if a.get("type") == "mega_tree":
            check(str(a["height"]) in tip, f"item {it['id']}: ipucu govde boyunu yazmiyor")
        if a.get("type") == "dragon_breath":
            # Bedrock'ta 1 kalp = 2 hasar. "bir bir gitsin" -> hasar CIFT olmali,
            # yoksa yarim kalpler gider ve ipucu yalan soyler.
            check(a["damage"] % 2 == 0,
                  f"item {it['id']}: hasar {a['damage']} tek sayi -> yarim kalp goturur")
            check(f"{a['damage'] // 2} kalb" in tip,
                  f"item {it['id']}: ipucu {a['damage'] // 2} kalbi yazmiyor")
            check(str(a["radius"]) in tip, f"item {it['id']}: ipucu {a['radius']} blok yaricapi yazmiyor")
            check(str(a["seconds"]) in tip, f"item {it['id']}: ipucu {a['seconds']} saniyeyi yazmiyor")
            check(a["pulse"] % 20 == 0, f"item {it['id']}: pulse {a['pulse']} tam saniye degil")


# ---------------------------------------------------------------- 5. mega agac
def check_tree(js):
    """Ipucu metnindeki olculer ile betigin gercekten kurdugu agac ayni mi."""
    cy1_js = re.search(r"const cy1 = H \+ Math\.round\(CR \* ([0-9.]+)\);", js)
    check(bool(cy1_js), "megaTree: cy1 formulu bulunamadi (ipucu ile karsilastirilamadi)")
    if cy1_js:
        expect = build.TREE_H + round(build.TREE_CROWN * float(cy1_js.group(1)))
        check(expect == build.TREE_TOP,
              f"mega agac: ipucu {build.TREE_TOP} blok diyor, betik {expect} blok kuruyor")
    it = [i for i in build.ITEMS if i["id"] == "mega_gubre"][0]
    check(str(build.TREE_TOP) in it["trtip"] and str(2 * build.TREE_CROWN + 1) in it["trtip"],
          "mega agac: ipucu yukseklik/genislik sayilarini yazmiyor")
    check(it["action"]["perTick"] <= 800,
          f"mega agac: tick basina {it['action']['perTick']} blok — tablette takilir")


# ------------------------------------------------------------------ 6. duruslar
# Kolun nereye dustugu hicbir kimlik/dosya denetiminde gorunmez; ancak modeli
# GORUNCE anlasilir. mutant_warden'in ozgun bind pozunda iki on kol govdenin
# orta cizgisinde, bel altinda birlesiyordu — cocuklarin ekraninda edepsiz
# duruyordu. Omuzlara 15 derece disa aci verildi. Bu denetim onu kilitler:
# kutu koordinatlari yeniden hesaplanip "edepsiz kutu"ya giren var mi bakilir.
POSE_MODELS = {
    # model dosyasi: (kemikler, edepsiz kutu x0,x1,y0,y1,z0,z1)
    "mutant_warden.geo.json": (("right_arm", "left_arm"), (-6, 6, -4, 20, -16, 6)),
}


def _cube_box(bone, cube, pose):
    piv = cube.get("pivot", bone.get("pivot", [0, 0, 0]))
    m = pose.rot_matrix(*cube["rotation"]) if cube.get("rotation") else None
    faces = pose.cube_faces(cube["origin"], cube["size"], piv, m)
    if any(bone.get("rotation") or [0, 0, 0]):
        bm = pose.rot_matrix(*bone["rotation"])
        bp = bone.get("pivot", [0, 0, 0])
        faces = [[pose.rot_about(p, bp, bm) for p in f] for f in faces]
    pts = [p for f in faces for p in f]
    return tuple(fn(p[i] for p in pts) for i in range(3) for fn in (min, max))


def check_poses():
    spec = importlib.util.spec_from_file_location(
        "pose_render", os.path.join(HERE, "pose_render.py"))
    pose = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(pose)
    for fname, (bone_names, rude) in POSE_MODELS.items():
        path = os.path.join(BEDROCK, "custom", fname)
        if not check(os.path.exists(path), f"poz: {fname} yok"):
            continue
        geo = json.load(open(path, encoding="utf-8"))["minecraft:geometry"][0]
        bones = {b["name"]: b for b in geo["bones"]}
        for bname in bone_names:
            if not check(bname in bones, f"poz: {fname} icinde {bname} kemigi yok"):
                continue
            bone = bones[bname]
            for i, cube in enumerate(bone.get("cubes", [])):
                bx = _cube_box(bone, cube, pose)
                # Kutuya DEGMEK degil, ICINE GIRMEK aranir. Omuz kutusu koseyi
                # yarim birim siyiriyor; bu bir durus hatasi degil. Esik her
                # eksende 2 birim (1/8 blok) — gercek hata 6 x 22 x 20 birim
                # girmisti, iki tarafa da genis pay var.
                pen = [min(bx[2 * k + 1], rude[2 * k + 1]) - max(bx[2 * k], rude[2 * k])
                       for k in range(3)]
                hit = all(p >= 2.0 for p in pen)
                check(not hit,
                      f"poz: {fname} {bname}#{i} bacak arasi kutusuna giriyor "
                      f"— x[{bx[0]:.1f}..{bx[1]:.1f}] y[{bx[2]:.1f}..{bx[3]:.1f}] "
                      f"z[{bx[4]:.1f}..{bx[5]:.1f}]")


# ------------------------------------------------------ 7. oyuncunun kontrolu
def check_player_control(js):
    """Oyuncunun elinden HAREKET KONTROLUNU alan hicbir sey kalmamali.

    Kucultme/Buyutme POV kamerasi 'minecraft:free' kamerayi her tick surer.
    Bedrock'ta ilk-sahis goz yuksekligini oynatmanin baska yolu yok. Ama yanlis
    surulurse oyun oynanmaz hale gelir ve Bedrock bunun icin HATA VERMEZ; ancak
    tablette fark edilir. v1.40.0'da uc sey birden yanlisti, ucu de burada
    kilitli:
      - konum p.location + sabit 1.62 idi -> comelince goz inmiyordu,
      - easing yoktu -> kamera saniyede 20 kez zipliyordu,
      - one oteleme yoktu -> kamera oyuncunun kendi kafasinin icinde, ekran
        siyah.
    Ayrica hareketi gercekten kilitleyen sey /inputpermission'dir; serbest
    kamera tek basina kilitlemez. O yuzden inputpermission YASAK."""
    # YORUMLAR ATILIR. Bu denetimlerin hepsi bir kez yorum satirina takildi:
    # bloktaki aciklama "getHeadLocation() ile" yaziyor, kod onu kullanmasa da
    # kelime dosyada duruyor ve denetim bos geciyordu. Denetim KODA bakmali.
    # (Blokta // iceren metin sabiti yok; olsaydi bu kesme yanlis olurdu.)
    def kodu(metin):
        return "\n".join(satir.split("//")[0] for satir in metin.splitlines())

    kod = kodu(js)
    cam = re.search(r"POV kamerasi[\s\S]*?\}, 1\);", js)
    check(bool(cam), "POV kamerasi blogu main.js'te bulunamadi")
    blok = kodu(cam.group(0)) if cam else ""
    check("p.getHeadLocation()" in blok,
          "POV kamerasi goz yuksekligini getHeadLocation()'dan almiyor "
          "— comelince goz inmez")
    check("easeOptions" in blok,
          "POV kamerasi easing'siz suruluyor — kamera saniyede 20 kez zipliyor")
    check("SIZE_CAM_FWD[sz]" in blok,
          "POV kamerasi one otelenmiyor — kamera oyuncunun kafasinin icinde, "
          "ekran siyah olur")
    # Oteleme iki sinirin ARASINDA olmali: kafanin on yuzunun ilerisinde
    # (yoksa kendi modelinin icinde kalir) ve carpisma kutusunun gerisinde
    # (yoksa duvara dayaninca blogun icinde kalir). Ikisi de ekrani siyah yapar.
    for i, (olcek, cw, _ch) in build.SIZE_TABLE.items():
        fwd = build.SIZE_CAM_FWD[i]
        check(0.25 * olcek < fwd < cw / 2,
              f"kademe {i}: kamera otelemesi {fwd} — kafa on yuzu "
              f"{0.25 * olcek:.3f} ile carpisma kutusu {cw / 2:.3f} arasinda degil")
    check(not re.search(r"inputpermission|inputPermissions", kod, re.I),
          "main.js inputpermission kullaniyor — oyuncunun hareketini kilitler")
    check("camera.clear" in kod,
          "kamerayi birakan camera.clear() cagrisi yok — normal boyuta donen "
          "oyuncu scripted kamerada kalir")
    # Kucultmenin GORUNUR karsiligi tek blokluk bosluga girebilmektir. Bunun
    # icin carpisma yuksekligi 1.0'in ALTINDA olmali; tam 1.00 sinirda kalip
    # calismiyordu.
    check(build.SIZE_TABLE[1][2] < 1.0,
          f"kademe 1 carpisma yuksekligi {build.SIZE_TABLE[1][2]} — tek blokluk "
          "bosluga girilemez")
    # Blok kirmayan bir patlama cocugun ekraninda SESTEN ibarettir: patlama
    # oyuncunun ayagi dibinde olusur, hasar geri konur, ortada iz kalmaz.
    for m in build.MORPHS:
        if m.get("act") == "explode":
            check(m.get("pow", {}).get("breaks"),
                  f"morph {m['key']}: patlamasi blok kirmiyor — cocuk yalnizca "
                  "patlama sesi duyar, hicbir sey olmaz")
    # Slowness amp >= 6 hareket hizini SIFIRLAR. Iki yerde bilerek kullanilir
    # (Buz TNT, Dondurucu) ve ikisi de ipucunda "dondurur" der. Ucuncusu
    # cikarsa bilerek eklenmis olmali.
    donma = re.findall(r'addEffect\("slowness",[^)]*amplifier:\s*(\d+)', js)
    check(sum(1 for a in donma if int(a) >= 6) <= 2,
          f"slowness amplifier>=6 (tam felc) {sum(1 for a in donma if int(a) >= 6)} "
          "yerde — ipucu 'dondurur' demeyen bir yere eklenmis olabilir")
    # Buz TNT'nin dondurmasi YARICAPLA sinirli olmali; sinirsiz hali dunyanin
    # obur ucundaki kardesi sebepsiz 30 saniye kilitliyordu.
    freeze = re.search(r'case "freeze": \{[\s\S]*?getPlayers\(([^)]*)\)', js)
    check(bool(freeze) and "maxDistance" in freeze.group(1),
          "Buz TNT dondurmasi yaricapla sinirli degil (dunyadaki herkesi donduruyor)")


# ------------------------------------------------------------------ 8. ikonlar
# Bir ikonun arkaplaninin boyali kalmasi ya da hic cizilmemis olmasi baska
# hicbir denetimde gorunmez — ancak envanterde bakinca anlasilir.
# Gozle bakmak icin: python3 bedrock/tools/icon_sheet.py
def check_icons():
    spec = importlib.util.spec_from_file_location(
        "icon_sheet", os.path.join(HERE, "icon_sheet.py"))
    ico = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ico)
    for it in build.ITEMS:
        p = os.path.join(RP, "textures", "items", f"stnt_{it['id']}.png")
        if not check(os.path.exists(p), f"ikon: {it['id']} dosyasi yok"):
            continue
        w, h, px = ico.read_png(p)
        cizili = sum(1 for row in px for c in row if c[3] > 0)
        # Bos ikon = SYMBOL_IDS'e id eklenip ciziminin unutulmasi. En seyrek
        # gercek ikon 27 piksel; 10 esigi ikisini de rahatca ayirir.
        check(cizili >= 10, f"ikon: {it['id']} neredeyse bos ({cizili} piksel cizili)")
        if it['id'] in build.SYMBOL_BG:
            continue                      # arkaplani BILEREK boyali iki ikon
        kenar = ([px[0][x] for x in range(w)] + [px[h - 1][x] for x in range(w)]
                 + [px[y][0] for y in range(h)] + [px[y][w - 1] for y in range(h)])
        opak = sum(1 for c in kenar if c[3] > 0)
        # Boyali arkaplan kenarin %100'unu doldurur; gercek ikonlarin en
        # yuksegi %25 (cizmenin tabani). %50 ikisinin ortasinda.
        check(opak * 2 <= len(kenar),
              f"ikon: {it['id']} arkaplani boyali "
              f"(kenar pikselinin {opak * 100 // len(kenar)}%'i opak)")


# --------------------------------------------------- 8b. morph durus pozu
def check_morph_pose():
    """Bind pozu kotu olan vanilla model icin poz animasyonu.

    Vanilla geometry.warden'in kollarinda hicbir rotation yok ve kollar uzun
    (y 6..34); bacaklarin (y 0..13) x araligina TAM DEGIYOR — olculdu: sag kol
    x -17..-9, sag bacak x -9..-3, ortusme 0. Oyunda mob'un kollari surekli
    salindigi icin bu goze carpmaz; oyuncu morph'u ise statik bind pozunda
    durur ve kollar bacaklara yapisik kalir. Cocuk "hala bacaklarimizi
    tutuyoruz" diye bildirdi.
    """
    posed = [m for m in build.MORPHS if m.get("pose")]
    check(any(m["key"] == "warden" for m in posed),
          "vanilla warden morph'unun durus pozu yok — kollar bacaklara yapisir")
    if not posed:
        return
    anim_yol = os.path.join(RP, "animations", "morph_pose.animation.json")
    check(os.path.exists(anim_yol), "poz animasyon dosyasi uretilmemis")
    if not os.path.exists(anim_yol):
        return
    anims = jload(anim_yol)["animations"]
    pl = jload(os.path.join(RP, "entity", "player.json"))["minecraft:client_entity"]["description"]
    animate = pl["scripts"]["animate"]
    for m in posed:
        aid = f"animation.stnt.pose.{m['key']}"
        check(aid in anims, f"{m['key']}: poz animasyonu {aid} dosyada yok")
        check(m["pose"], f"{m['key']}: pose alani bos — hicbir kemik donmez")
        if aid in anims:
            kemikler = anims[aid].get("bones", {})
            check(set(kemikler) == set(m["pose"]),
                  f"{m['key']}: poz kemikleri spec ile ayni degil")
        # Oyuncuya BAGLANMIS olmali: kayit + st:morph ile kapili animate girdisi.
        # Kayit olmadan animasyon dosyada durur ve hicbir zaman kosmaz.
        check(pl["animations"].get(f"pose_{m['key']}") == aid,
              f"{m['key']}: poz animasyonu player.json'a kaydedilmemis")
        gecerli = [e for e in animate if isinstance(e, dict) and f"pose_{m['key']}" in e]
        check(len(gecerli) == 1, f"{m['key']}: poz animate listesine eklenmemis")
        if gecerli:
            kosul = gecerli[0][f"pose_{m['key']}"]
            check(f"== {m['n']}" in kosul,
                  f"{m['key']}: poz kosulu yanlis morph sayisini kullaniyor: {kosul}")
        # Poz KENDI geometrimizde degil, VANILLA modelde duzeltme yapar.
        # Kendi modelimizde dogru yer geometrinin rotation alani (bkz. v1.40).
        check(not m.get("ns"),
              f"{m['key']}: kendi modelimiz — poz animasyonu degil, geometriye yazilmali")


# ---------------------------------------------------------------- calistir
def main():
    js = open(os.path.join(BP, "scripts", "main.js"), encoding="utf-8").read()
    check_parse()
    check_manifest()
    check_items(js)
    check_tnts()
    check_morphs(js)
    check_ids(js)
    check_client_entities()
    check_pack_integrity()
    check_tooltip_contract(js)
    check_tree(js)
    check_poses()
    check_morph_pose()
    check_player_control(js)
    check_icons()
    if FAILS:
        print(f"HATA — {CHECKS[0]} denetimden {len(FAILS)} tanesi gecmedi:")
        for f in FAILS:
            print("  -", f)
        return 1
    print(f"gecti — {CHECKS[0]} denetim")
    return 0


if __name__ == "__main__":
    sys.exit(main())
