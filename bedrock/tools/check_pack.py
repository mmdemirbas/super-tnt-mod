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

# Paketin KENDI varliklari ve modelleri: morph tablosunda bunlar da gecebilir,
# vanilla listelerinde aranmamali.
OWN_ENTITIES = {f"stnt:{m['id']}" for m in build.MONSTERS}
OWN_GEOS = {f"geometry.{m['id']}" for m in build.MONSTERS}
OWN_TEXS = {f"textures/entity/{m['id']}" for m in build.MONSTERS}
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
        tid = f"{m.get('ns', 'minecraft')}:{k}"
        check(tid in ENTITIES or tid in OWN_ENTITIES, f"morph {k}: '{tid}' diye bir varlik yok")
        if tid in OWN_ENTITIES:
            check(os.path.exists(os.path.join(BP, "entities", f"{k}.json")),
                  f"morph {k}: paketin kendi mob'u ama BP/entities/{k}.json yok")
        check(m["geo"] in GEOS or m["geo"] in OWN_GEOS, f"morph {k}: geometry '{m['geo']}' yok")
        if m["geo"] in OWN_GEOS:
            # Kendi modelimizse RP'de GERCEKTEN ship edilmis olmali; edilmezse
            # oyuncu gorunmez olur ve hicbir hata cikmaz.
            gid = m["geo"].split(".", 1)[1]
            check(os.path.exists(os.path.join(RP, "models", "entity", f"{gid}.geo.json")),
                  f"morph {k}: '{m['geo']}' modeli RP'de ship edilmiyor")
        check(m["tex"] in TEXS or m["tex"] in OWN_TEXS, f"morph {k}: doku '{m['tex']}' yok")
        if m["tex"] in OWN_TEXS:
            check(os.path.exists(os.path.join(RP, m["tex"] + ".png")),
                  f"morph {k}: '{m['tex']}.png' RP'de yok")
        check(m["mat"] in MATS or m["mat"] in EXTRA_MATS, f"morph {k}: materyal '{m['mat']}' yok")
        for i, lay in enumerate(m.get("layers", [])):
            for t in lay["tex"]:
                check(t in TEXS, f"morph {k} katman {i}: doku '{t}' vanilla'da yok")
            check(lay["mat"] in MATS, f"morph {k} katman {i}: materyal '{lay['mat']}' vanilla'da yok")
            if "geo" in lay:
                check(lay["geo"] in GEOS, f"morph {k} katman {i}: geometry '{lay['geo']}' vanilla'da yok")
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
    check(len(mm) == len(morphs), f"MORPH_MAP {len(mm)} girdi, morph sayisi {len(morphs)}")
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


# ------------------------------------------------------- 4b. ipucu sozlesmesi
def check_tooltip_contract():
    """CLAUDE.md: ipucu bir sozlesmedir. Yeni esyalarin ipucundaki sayilar
    davranistan turetilmis olmali, elle yazilmis olmamali."""
    for it in build.ITEMS:
        a = it.get("action") or {}
        tip = it["trtip"]
        if a.get("type") == "heal_boost":
            # amplifier -> can matematigi: seviye basina +4, seviye = amp + 1
            check(build.POTION_BASE_HP + 4 * (a["amp"] + 1) == a["hp"],
                  f"item {it['id']}: amp {a['amp']} -> "
                  f"{build.POTION_BASE_HP + 4 * (a['amp'] + 1)} can, ipucu {a['hp']} diyor")
            check(str(a["hp"]) in tip, f"item {it['id']}: ipucu {a['hp']} canini yazmiyor")
            check(str(a["seconds"] // 60) in tip, f"item {it['id']}: ipucu sureyi yazmiyor")
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


# ---------------------------------------------------------------- calistir
def main():
    js = open(os.path.join(BP, "scripts", "main.js"), encoding="utf-8").read()
    check_parse()
    check_manifest()
    check_items(js)
    check_tnts()
    check_morphs(js)
    check_ids(js)
    check_tooltip_contract()
    check_tree(js)
    if FAILS:
        print(f"HATA — {CHECKS[0]} denetimden {len(FAILS)} tanesi gecmedi:")
        for f in FAILS:
            print("  -", f)
        return 1
    print(f"gecti — {CHECKS[0]} denetim")
    return 0


if __name__ == "__main__":
    sys.exit(main())
