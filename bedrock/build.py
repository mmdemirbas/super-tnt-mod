#!/usr/bin/env python3
"""
Super TNT Mod -> Minecraft Bedrock Add-On uretici.

Java modundaki (Fabric) TNT'lerin bir alt kumesini Bedrock Add-On'una
cevirir ve tek .mcaddon olarak paketler.

TASARIM KARARLARI
-----------------
K1. Ateslenen blok fiziksel bir varliga donusur (firlar, duser, yanip
    soner) - vanilla TNT gibi. Her TNT'nin `stnt:<ad>_primed` varligi var.

K2. Atesleme uc yoldan: cakmak, redstone (yerlestirilen bloklar dunya
    dinamik ozelliginde tutulup yoklanir) ve zincirleme patlama.

K3. Dokular TNT SABLONU yeniden renklendirilerek uretilir, duz renk kare
    degil. Java'da aile TNT'leri `minecraft:block/*_concrete` kullaniyor
    ama Bedrock'ta vanilla doku yollari farkli adlandirilmis
    (light_gray -> concrete_silver gibi) ve kirik doku riski var.
    Bunun yerine gercek TNT dokusunun parlakligi korunup rengi
    degistiriliyor -> "TNT" yazisi ve fitil duruyor.
    NOT: duz renk kare uretmek ilk denemede envanterde birbirinden
    ayirt edilemeyen 10 pastel kareye yol acmisti.

K4. Aile TNT'lerinin tonlari ayristirildi. Java'da abi/anne/baba/bebek
    dordu de ayni pembe; envanterde ayirt edilemiyor. Ayni sicak paletten
    farkli tonlar verildi. Bilincli sapma.

K5. Davranislar ve METINLER Java kaynagindan BIREBIR alindi. Sayilar
    ilgili *TntEntity.java'dan, ad/aciklama lang/tr_tr.json'dan.
    Turkce karakterler korunur - ilk denemede ASCII'ye kirpilmisti.

K6. Java'da tooltip her TNT'de var, Bedrock'ta blok tooltip'i yok.
    Karsilik: TNT elde secilince aciklama eylem cubugunda gosterilir,
    ceviri anahtariyla (oyuncunun dilinde).

K7. Kalp TNT buff'i YALNIZCA oyunculara. Java'da 88b9de1 ayni hatayi
    Harf TNT'de duzeltti (tum LivingEntity'ye verilince zombi/iskelet
    guclenip cocuk icin tehlikeli oluyordu); KalpTntEntity.java'da gozden
    kacmisti, her iki tarafta da duzeltildi.

K8. Zincirleme taramasi tick'lere yayilir. Tek tick'te (2R+1)^3 blok
    sorgusu tablette gorunur donmaya yol aciyordu.
"""
import json, os, re, shutil, struct, zlib, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA_TEX = os.path.join(ROOT, "src/main/resources/assets/supertntmod/textures/block")
BP = os.path.join(HERE, "super_tnt_BP")
RP = os.path.join(HERE, "super_tnt_RP")
OUT = os.path.join(HERE, "out")

BP_UUID = "3f8a1c62-7d54-4b90-9e21-8a4f6c0d1e73"
BP_MOD_UUID = "4a9b2d73-8e65-4ca1-af32-9b5a7d1e2f84"
BP_SCRIPT_UUID = "5bac3e84-9f76-4db2-b043-ac6b8e2f3a95"
RP_UUID = "6cbd4f95-a087-4ec3-b154-bd7c9f3a4b06"
RP_MOD_UUID = "7dce50a6-b198-4fd4-c265-ce8da04b5c17"
# Surum: ayni UUID + ayni surum tekrar import edilirse Minecraft bunu
# "ayni paket" sayar ve listede ikinci bir kopya gosterebilir. Surum
# YUKSELTILIRSE guncelleme olarak alir ve o paketi kullanan dunyalar yeni
# surume gecer. Bu yuzden her yeni .mcaddon'da burayi artir.
VERSION = [1, 3, 0]
MIN_ENGINE = [1, 21, 0]

# ---------------------------------------------------------------- TNT tanimlari
# renk: (top, side, bottom) RGB. tex: Java projesinden kopyalanacak taban ad.
# tr/trtip metinleri Java projesinin lang/tr_tr.json dosyasindan BIREBIR alindi.
# Turkce karakterler korunur (.lang dosyalari UTF-8).
TNTS = [
    dict(id="diamond_tnt", tr="Elmas TNT", en="Diamond TNT",
         trtip="2.5x patlama gücü - dev kraterler açar",
         entip="2.5x explosion power - opens huge craters",
         tex="diamond_tnt", mat="minecraft:diamond",
         effect=dict(kind="explode", power=10)),
    dict(id="bounce_tnt", tr="Zıplatan TNT", en="Bounce TNT",
         trtip="Yakındaki HER ŞEYİ gökyüzüne fırlatır! Blok hasarı yoktur.",
         entip="Launches EVERYTHING nearby into the sky! No block damage.",
         tex="bounce_tnt", mat="minecraft:slime_ball",
         effect=dict(kind="launch", radius=12, force=1.5)),
    dict(id="zeynep_tnt", tr="Zeynep TNT", en="Zeynep TNT",
         trtip="Lacivert TNT — patlayınca etrafa boş kağıtlar saçar!",
         entip="Navy blue TNT - scatters blank paper everywhere!",
         color=((44, 62, 158), (44, 62, 158), (44, 62, 158)), mat="minecraft:paper",
         effect=dict(kind="items", item="minecraft:paper", count=50, spread=8.0)),
    dict(id="coklu_zeynep_tnt", tr="Çoklu Zeynep TNT", en="Multi Zeynep TNT",
         trtip="Patladığında etrafa 'Zeynep' isimli köylüler saçar.",
         entip="Scatters villagers named 'Zeynep' when it explodes.",
         color=((160, 40, 150), (214, 96, 168), (160, 40, 150)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Zeynep", count=15, spread=7.0)),
    dict(id="abi_tnt", tr="Abi TNT", en="Abi TNT",
         trtip="Patladığında etrafa 'Abi' isimli köylüler saçar.",
         entip="Scatters villagers named 'Abi' when it explodes.",
         color=((196, 76, 132), (196, 76, 132), (196, 76, 132)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Abi", count=12, spread=6.0)),
    dict(id="anne_tnt", tr="Anne TNT", en="Anne TNT",
         trtip="Patladığında etrafa 'Anne' isimli köylüler saçar.",
         entip="Scatters villagers named 'Anne' when it explodes.",
         color=((224, 122, 168), (224, 122, 168), (224, 122, 168)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Anne", count=12, spread=6.0)),
    dict(id="baba_tnt", tr="Baba TNT", en="Baba TNT",
         trtip="Patladığında etrafa 'Baba' isimli köylüler saçar.",
         entip="Scatters villagers named 'Baba' when it explodes.",
         color=((150, 60, 110), (150, 60, 110), (150, 60, 110)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Baba", count=12, spread=6.0)),
    dict(id="bebek_tnt", tr="Bebek TNT", en="Bebek TNT",
         trtip="Patladığında etrafa 'Bebek' isimli yavru köylüler saçar.",
         entip="Scatters baby villagers named 'Bebek' when it explodes.",
         color=((245, 178, 208), (245, 178, 208), (245, 178, 208)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Bebek", count=15, spread=6.0, baby=True)),
    dict(id="bulut_tnt", tr="Bulut TNT", en="Cloud TNT",
         trtip="Yağmur başlatır ve gökyüzünü bulutlandırır.",
         entip="Starts rain and clouds over the sky.",
         color=((236, 240, 244), (150, 200, 232), (236, 240, 244)), mat="minecraft:white_wool",
         effect=dict(kind="weather", weather="Rain")),
    dict(id="ay_tnt", tr="Ay TNT", en="Moon TNT",
         trtip="Gündüzse güneşi aya dönüştürür — gece olur.",
         entip="Turns the sun into the moon - night falls.",
         color=((186, 190, 194), (186, 190, 194), (186, 190, 194)), mat="minecraft:glowstone_dust",
         effect=dict(kind="time", time=18000)),
    # DIKKAT: buff yalnizca OYUNCULARA. Java tarafinda 88b9de1 "Harf TNT'sinin
    # can/kalkan buff'u sadece oyunculara verilsin" ayni hatayi duzeltti:
    # tum LivingEntity'ye verilince zombi/iskelet guclenip cocuk icin
    # tehlikeli hale geliyordu. KalpTntEntity.java'da gozden kacmisti;
    # denetimde bulundu ve her iki tarafta da duzeltildi.
    dict(id="kalp_tnt", tr="Kalp TNT", en="Heart TNT",
         trtip="Yakındaki herkese can yenileme + kalkan verir, efektleri ve boyutu sıfırlar.",
         entip="Heals and shields nearby players, clears effects and size changes.",
         color=((140, 40, 46), (196, 48, 54), (92, 48, 52)), mat="minecraft:gold_ingot",
         effect=dict(kind="heal", radius=20)),
    dict(id="buz_tnt", tr="Buz TNT", en="Ice TNT",
         trtip="14 blok yarıçapı buzla kaplar. Patlatan hariç herkesi 30 sn dondurur. 30 sn kar yağar.",
         entip="Covers a 14 block radius with ice. Freezes everyone except the igniter for 30s. Snows for 30s.",
         color=((150, 200, 232), (176, 216, 240), (150, 200, 232)), mat="minecraft:packed_ice",
         effect=dict(kind="freeze", radius=14, freeze_seconds=30)),
]

FUSE_TICKS = 80  # Java tarafinda setFuse(80)


# ---------------------------------------------------------------- PNG uretici
def png(path, rows):
    """rows: 16 satir, her biri 16 adet (r,g,b) uclusu."""
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in rows)
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    hdr = struct.pack('>IIBBBBB', 16, 16, 8, 2, 0, 0, 0)
    blob = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', hdr)
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(blob)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


# Yeniden renklendirme sablonu: gercek TNT dokusu. Duz renk kare uretmek
# yerine bunun parlakligini koruyup rengini degistiriyoruz; boylece "TNT"
# yazisi, fitil ve bant duruyor, sadece govde rengi degisiyor.
TEMPLATE = "diamond_tnt"


def tnt_face(path, base, band=True, face="side"):
    """TNT sablonunu hedef renge boyar. PIL yoksa duz renge duser."""
    src = os.path.join(JAVA_TEX, f"{TEMPLATE}_{face}.png")
    try:
        from PIL import Image
        import colorsys
        if not os.path.exists(src):
            raise FileNotFoundError(src)
        im = Image.open(src).convert("RGB")
        th, ts, _ = colorsys.rgb_to_hsv(*[c / 255 for c in base])
        out = Image.new("RGB", im.size)
        px_in, px_out = im.load(), out.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                r, g, b = px_in[x, y]
                _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                # ton hedeften, doygunluk hedefe olceklenir, parlaklik korunur
                # -> yazi/fitil/golge yapisi aynen kalir
                ns = min(1.0, s * (0.35 + ts))
                nr, ng, nb = colorsys.hsv_to_rgb(th, ns, v)
                px_out[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        out.save(path)
        return
    except Exception:
        pass
    # yedek: duz renk
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            c = shade(base, 1.0 + ((x * 7 + y * 13) % 5 - 2) * 0.02)
            if band and 6 <= y <= 9:
                c = shade(base, 1.30)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.78)
            row.append(c)
        rows.append(row)
    png(path, rows)


def compose_entity_texture(t, dst):
    """Blok yuzlerinden varlik dokusu (64x32) uretir.

    MC kutu-UV duzeni, 16x16x16 kup, uv [0,0]:
      top (16,0) | bottom (32,0) | dogu (0,16) | kuzey (16,16)
      bati (32,16) | guney (48,16)
    PIL varsa gercek yuz PNG'leri birlestirilir; yoksa duz renge duser.
    """
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    faces = {}
    for face in ("top", "side", "bottom"):
        p = os.path.join(RP, f"textures/blocks/stnt_{t['id']}_{face}.png")
        faces[face] = p if os.path.exists(p) else None
    try:
        from PIL import Image
        atlas = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        def put(face, xy):
            if faces[face]:
                atlas.paste(Image.open(faces[face]).convert("RGBA").resize((16, 16)), xy)
        put("top", (16, 0)); put("bottom", (32, 0))
        for xy in ((0, 16), (16, 16), (32, 16), (48, 16)):
            put("side", xy)
        atlas.save(dst)
        return "pil"
    except ImportError:
        # yedek: renk tanimindan duz doku (PIL yoksa)
        base = t.get('color', ((200, 60, 60),) * 3)
        rows = [[base[0] if y < 16 else base[1] for _ in range(64)] for y in range(32)]
        png_rgb(dst, rows, 64, 32)
        return "fallback"


def png_rgb(path, rows, wd, ht):
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in rows)
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    hdr = struct.pack('>IIBBBBB', wd, ht, 8, 2, 0, 0, 0)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', hdr)
                          + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# ---------------------------------------------------------------- yapi
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)


def build():
    for d in (BP, RP, OUT):
        if os.path.exists(d):
            shutil.rmtree(d)

    # ---------- manifestler
    w(os.path.join(BP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": "Super TNT Mod [BP]",
                   "description": "Super TNT Mod - Bedrock surumu",
                   "uuid": BP_UUID, "version": VERSION,
                   "min_engine_version": MIN_ENGINE},
        "modules": [
            {"type": "data", "uuid": BP_MOD_UUID, "version": VERSION},
            {"type": "script", "uuid": BP_SCRIPT_UUID, "version": VERSION,
             "entry": "scripts/main.js"}],
        "dependencies": [
            {"uuid": RP_UUID, "version": VERSION},
            # 1.14.0: MorphX bu surumu kullaniyor ve ayni cihazda yuklendigi
            # dogrulandi. Tahmin yerine bilinen-calisan surume yaslaniyoruz.
            {"module_name": "@minecraft/server", "version": "1.14.0"}],
    })
    w(os.path.join(RP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": "Super TNT Mod [RP]",
                   "description": "Super TNT Mod - dokular",
                   "uuid": RP_UUID, "version": VERSION,
                   "min_engine_version": MIN_ENGINE},
        "modules": [{"type": "resources", "uuid": RP_MOD_UUID, "version": VERSION}],
    })

    # ---------- dokular
    terrain = {}
    copied = generated = 0
    for t in TNTS:
        for face in ("top", "side", "bottom"):
            key = f"stnt_{t['id']}_{face}"
            rel = f"textures/blocks/{key}"
            dst = os.path.join(RP, rel + ".png")
            src = os.path.join(JAVA_TEX, f"{t.get('tex','')}_{face}.png")
            if t.get('tex') and os.path.exists(src):
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copy(src, dst)
                copied += 1
            else:
                idx = {"top": 0, "side": 1, "bottom": 2}[face]
                tnt_face(dst, t['color'][idx], band=(face == "side"), face=face)
                generated += 1
            terrain[key] = {"textures": rel}
    w(os.path.join(RP, "textures/terrain_texture.json"),
      {"resource_pack_name": "super_tnt", "texture_name": "atlas.terrain",
       "padding": 8, "num_mip_levels": 4, "texture_data": terrain})

    # ---------- bloklar
    for t in TNTS:
        w(os.path.join(BP, f"blocks/{t['id']}.json"), {
            "format_version": "1.21.0",
            "minecraft:block": {
                # menu_category'de "group" verilmiyor: gecerliligi dogrulanmamis
                # bir grup adi blogun yaratici menude hic gorunmemesine yol acar.
                "description": {"identifier": f"stnt:{t['id']}",
                                "menu_category": {"category": "construction"}},
                "components": {
                    "minecraft:material_instances": {
                        "up": {"texture": f"stnt_{t['id']}_top", "render_method": "opaque"},
                        "down": {"texture": f"stnt_{t['id']}_bottom", "render_method": "opaque"},
                        "*": {"texture": f"stnt_{t['id']}_side", "render_method": "opaque"}},
                    "minecraft:destructible_by_mining": {"seconds_to_destroy": 0.0},
                    "minecraft:destructible_by_explosion": {"explosion_resistance": 0},
                    "minecraft:light_dampening": 15,
                    "minecraft:geometry": "minecraft:geometry.full_block",
                },
            },
        })

    # ---------- ates alan TNT varligi (F1)
    # 16x16x16 tek kup. MC kutu-UV duzeni: 64x32 doku.
    w(os.path.join(RP, "models/entity/stnt_tnt.geo.json"), {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.stnt_tnt",
                            "texture_width": 64, "texture_height": 32,
                            "visible_bounds_width": 2, "visible_bounds_height": 2,
                            "visible_bounds_offset": [0, 1, 0]},
            "bones": [{"name": "body", "pivot": [0, 0, 0],
                       "cubes": [{"origin": [-8, 0, -8], "size": [16, 16, 16], "uv": [0, 0]}]}],
        }],
    })
    # yanip sonen beyaz kaplama - vanilla TNT hissi
    w(os.path.join(RP, "render_controllers/stnt_tnt.render_controllers.json"), {
        "format_version": "1.10.0",
        "render_controllers": {
            "controller.render.stnt_tnt": {
                "geometry": "Geometry.default",
                "materials": [{"*": "Material.default"}],
                "textures": ["Texture.default"],
                "overlay_color": {
                    "r": 1.0, "g": 1.0, "b": 1.0,
                    "a": "math.mod(math.floor(query.life_time * 10), 2) * 0.55",
                },
            }
        },
    })

    for t in TNTS:
        ent = f"stnt:{t['id']}_primed"
        w(os.path.join(BP, f"entities/{t['id']}_primed.json"), {
            "format_version": "1.21.0",
            "minecraft:entity": {
                "description": {"identifier": ent, "is_spawnable": False,
                                "is_summonable": True, "is_experimental": False},
                "components": {
                    "minecraft:collision_box": {"width": 0.98, "height": 0.98},
                    "minecraft:physics": {},
                    "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": True},
                    "minecraft:fire_immune": True,
                    "minecraft:damage_sensor": {"triggers": [{"deals_damage": False}]},
                    "minecraft:conditional_bandwidth_optimization": {},
                },
            },
        })
        w(os.path.join(RP, f"entity/{t['id']}_primed.json"), {
            "format_version": "1.10.0",
            "minecraft:client_entity": {
                "description": {
                    "identifier": ent,
                    "materials": {"default": "entity_alphatest"},
                    "textures": {"default": f"textures/entity/stnt/{t['id']}"},
                    "geometry": {"default": "geometry.stnt_tnt"},
                    "render_controllers": ["controller.render.stnt_tnt"],
                },
            },
        })
        compose_entity_texture(t, os.path.join(RP, f"textures/entity/stnt/{t['id']}.png"))

    # ---------- tarifler (8 malzeme + ortada TNT)
    for t in TNTS:
        w(os.path.join(BP, f"recipes/{t['id']}.json"), {
            "format_version": "1.20.10",
            "minecraft:recipe_shaped": {
                "description": {"identifier": f"stnt:{t['id']}_recipe"},
                "tags": ["crafting_table"],
                "pattern": ["MMM", "MTM", "MMM"],
                "key": {"M": {"item": t['mat']}, "T": {"item": "minecraft:tnt"}},
                "result": {"item": f"stnt:{t['id']}", "count": 1},
            },
        })

    # ---------- dil
    for pack in (BP, RP):
        os.makedirs(os.path.join(pack, "texts"), exist_ok=True)
        w(os.path.join(pack, "texts/languages.json"), ["en_US", "tr_TR"])
        for lang, nk, tk in (("en_US", "en", "entip"), ("tr_TR", "tr", "trtip")):
            lines = [f"pack.name=Super TNT Mod",
                     f"pack.description=Super TNT Mod"]
            for t in TNTS:
                lines.append(f"tile.stnt:{t['id']}.name={t[nk]}")
                lines.append(f"stnt.tip.{t['id']}={t[tk]}")
            open(os.path.join(pack, f"texts/{lang}.lang"), 'w',
                 encoding='utf-8').write("\n".join(lines) + "\n")

    # ---------- script
    spec = {t['id']: t['effect'] for t in TNTS}
    tips = {t['id']: t['tr'] for t in TNTS}
    tiptext = {t['id']: t['trtip'] for t in TNTS}
    script = SCRIPT_TEMPLATE.replace("__SPEC__", json.dumps(spec, indent=2)) \
                            .replace("__NAMES__", json.dumps(tips, ensure_ascii=False)) \
                            .replace("__TIPS__", json.dumps(tiptext, ensure_ascii=False)) \
                            .replace("__FUSE__", str(FUSE_TICKS))
    os.makedirs(os.path.join(BP, "scripts"), exist_ok=True)
    open(os.path.join(BP, "scripts/main.js"), 'w', encoding='utf-8').write(script)

    # ---------- paketle
    os.makedirs(OUT, exist_ok=True)
    target = os.path.join(OUT, "SuperTNT.mcaddon")
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as z:
        for p in (BP, RP):
            top = os.path.basename(p)
            for dp, _, fs in os.walk(p):
                for f in fs:
                    full = os.path.join(dp, f)
                    z.write(full, os.path.join(top, os.path.relpath(full, p)))

    print(f"TNT sayisi        : {len(TNTS)}")
    print(f"doku              : {copied} kopyalandi, {generated} uretildi")
    print(f"cikti             : {target}")
    print(f"boyut             : {os.path.getsize(target)/1024:.0f} KB")
    return target


SCRIPT_TEMPLATE = r'''// Super TNT Mod - Bedrock
// Uretilmis dosya. Kaynak: bedrock/build.py  (elle duzenleme, yeniden uretilir)
import { world, system, ItemStack } from "@minecraft/server";

const SPEC = __SPEC__;
const NAMES = __NAMES__;
const TIPS = __TIPS__;
const FUSE = __FUSE__;

// ---------------------------------------------------------------- tooltip
// Bedrock'ta Java'daki gibi blok tooltip'i yok. Java surumunde her TNT'nin
// ne yaptigini anlatan bir aciklama var ve cocuklarin hangi TNT'nin ne
// yaptigini bilmesi buna bagli. Yerine: TNT elde secilince aciklama
// eylem cubugunda (action bar) gosteriliyor.
const lastSel = new Map();   // playerId -> gosterilen id
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    let held = null;
    try {
      const inv = p.getComponent("minecraft:inventory");
      const it = inv?.container?.getItem(p.selectedSlotIndex);
      held = it ? it.typeId : null;
    } catch (e) { continue; }
    if (lastSel.get(p.id) === held) continue;
    lastSel.set(p.id, held);
    if (held && held.startsWith("stnt:")) {
      const s = held.slice(5);
      if (!TIPS[s]) continue;
      // Ham metin yerine ceviri anahtari: oyuncunun dilinde gorunur ve
      // texts/*.lang'daki anahtarlar olu kalmaz.
      try {
        p.onScreenDisplay.setActionBar({
          rawtext: [
            { text: "§e" }, { translate: `tile.stnt:${s}.name` },
            { text: "§r §7- " }, { translate: `stnt.tip.${s}` },
          ],
        });
      } catch (e) {
        try { p.onScreenDisplay.setActionBar(`§e${NAMES[s]}§r §7- ${TIPS[s]}`); } catch (e2) {}
      }
    }
  }
}, 5);

// ---------------------------------------------------------------- yerlestirilen
// bloklarin kaydi. Redstone yoklamasi sadece bu listedeki bloklara bakar,
// boylece maliyet oyuncunun koydugu TNT sayisiyla sinirli kalir.
// Dunya dinamik ozelliginde saklanir -> dunya yeniden acilinca kaybolmaz.
const TRACK_PROP = "stnt:placed";
const TRACK_MAX = 400;
let tracked = new Set();

function loadTracked() {
  try {
    const raw = world.getDynamicProperty(TRACK_PROP);
    if (typeof raw === "string") tracked = new Set(JSON.parse(raw));
  } catch (e) { tracked = new Set(); }
}
function saveTracked() {
  try {
    world.setDynamicProperty(TRACK_PROP, JSON.stringify([...tracked].slice(-TRACK_MAX)));
  } catch (e) {}
}
function track(dimId, loc) {
  tracked.add(`${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`);
  if (tracked.size > TRACK_MAX) tracked = new Set([...tracked].slice(-TRACK_MAX));
  saveTracked();
}
function untrack(dimId, loc) {
  tracked.delete(`${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`);
  saveTracked();
}

world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (b && b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
    track(b.dimension.id, b.location);
  }
});

// ---------------------------------------------------------------- atesleme
world.afterEvents.playerInteractWithBlock.subscribe((ev) => {
  const { block, itemStack, player } = ev;
  if (!block || !itemStack) return;
  if (itemStack.typeId !== "minecraft:flint_and_steel") return;
  const id = block.typeId;
  if (!id.startsWith("stnt:")) return;
  const short = id.slice(5);
  if (!SPEC[short]) return;
  ignite(block.dimension, block.location, short, player.id);
  try { player.sendMessage(`§e${NAMES[short] ?? short} §7ateslendi!`); } catch (e) {}
});

// F2 - redstone. Kayitli bloklari 10 tick'te bir yoklar.
system.runInterval(() => {
  if (tracked.size === 0) return;
  for (const k of [...tracked]) {
    try {
      const [dimId, xyz] = k.split("|");
      const [x, y, z] = xyz.split(",").map(Number);
      const dim = world.getDimension(dimId);
      const b = dim.getBlock({ x, y, z });
      if (!b) continue;                       // yuklu degil, sonraki tura birak
      if (!b.typeId.startsWith("stnt:")) { tracked.delete(k); saveTracked(); continue; }
      if ((b.getRedstonePower() ?? 0) > 0) ignite(dim, b.location, b.typeId.slice(5));
    } catch (e) {}
  }
}, 10);

// F3 - baska bir patlama bizim blogu vuruyorsa yok etme, atesle.
try {
  world.beforeEvents.explosion.subscribe((ev) => {
    const dim = ev.dimension;
    const keep = [];
    let lit = 0;
    for (const b of ev.getImpactedBlocks()) {
      try {
        if (b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
          const loc = { x: b.location.x, y: b.location.y, z: b.location.z };
          const short = b.typeId.slice(5);
          system.run(() => ignite(dim, loc, short));
          lit++;
          continue;                            // patlamanin yok etmesini engelle
        }
      } catch (e) {}
      keep.push(b);
    }
    if (lit > 0) ev.setImpactedBlocks(keep);
  });
} catch (e) {
  console.warn("[SuperTNT] explosion olayi yok, zincirleme sadece kendi TNT'lerimizde");
}

// ---------------------------------------------------------------- fitil
// F1 - blok kaldirilir, yerine fiziksel bir varlik dogar. Firlatilabilir,
// duser, ziplar - vanilla TNT gibi.
const primed = new Map();  // entityId -> { short, left, dim }

function ignite(dim, loc, short, igniterId) {
  const k = key(dim.id, loc);
  try {
    const b = dim.getBlock(loc);
    if (!b || b.typeId !== `stnt:${short}`) return;   // zaten ateslenmis
    b.setType("minecraft:air");
  } catch (e) { return; }
  untrack(dim.id, loc);

  const c = { x: Math.floor(loc.x) + 0.5, y: Math.floor(loc.y) + 0.5, z: Math.floor(loc.z) + 0.5 };
  try { dim.playSound("random.fuse", c, { volume: 1.0 }); } catch (e) {}
  try {
    const e = dim.spawnEntity(`stnt:${short}_primed`, c);
    try { e.applyImpulse({ x: rnd(0.04), y: 0.22, z: rnd(0.04) }); } catch (err) {}
    primed.set(e.id, { short, left: FUSE, e, ig: igniterId });
  } catch (err) {
    // varlik dogmadiysa yerinde patlat - islev kaybolmasin
    system.runTimeout(() => detonate(dim, c, short, igniterId), FUSE);
  }
}

// fitil sayaci
system.runInterval(() => {
  for (const [id, p] of [...primed]) {
    p.left -= 2;
    let loc = null, dim = null;
    try { loc = p.e.location; dim = p.e.dimension; } catch (e) { primed.delete(id); continue; }
    try {
      dim.spawnParticle("minecraft:basic_smoke_particle", { x: loc.x, y: loc.y + 0.7, z: loc.z });
    } catch (e) {}
    if (p.left <= 0) {
      primed.delete(id);
      try { p.e.remove(); } catch (e) {}
      detonate(dim, loc, p.short, p.ig);
    }
  }
}, 2);

// F3 - patlama aninda cevredeki TNT'leri kademeli atesle.
//
// PERFORMANS: kup taramasi tek tick'te yapilirsa (2R+1)^3 blok sorgusu eder.
// R=6'da 2197; 10'lu bir zincirde 22 bin sorgu ve tablette gorunur donma.
// Bu yuzden yaricap 5'e cekildi ve tarama dx dilimlerine bolunup tick'lere
// yayildi: tick basina 2 dilim = ~242 sorgu.
function chain(dim, c) {
  const R = 5;
  const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
  let dx = -R, found = 0;
  const job = system.runInterval(() => {
    for (let slice = 0; slice < 2 && dx <= R; slice++, dx++) {
      for (let dy = -R; dy <= R; dy++) {
        for (let dz = -R; dz <= R; dz++) {
          if (found >= 30) { system.clearRun(job); return; }
          try {
            const p = { x: cx + dx, y: cy + dy, z: cz + dz };
            const b = dim.getBlock(p);
            if (!b) continue;
            const t = b.typeId;
            if (!t.startsWith("stnt:")) continue;
            const s = t.slice(5);
            if (!SPEC[s]) continue;
            found++;
            system.runTimeout(() => { try { ignite(dim, p, s); } catch (e) {} },
                              2 + Math.floor(Math.random() * 8));
          } catch (e) {}
        }
      }
    }
    if (dx > R) system.clearRun(job);
  }, 1);
}

function rnd(n) { return (Math.random() - 0.5) * n; }

function detonate(dim, c, short, igniterId) {
  const s = SPEC[short];
  if (!s) return;
  chain(dim, c);   // F3 - zinciri kur (patlamadan once, bloklar hala duruyor)
  try {
    switch (s.kind) {
      case "explode":
        dim.createExplosion(c, s.power, { breaksBlocks: true, causesFire: false });
        break;

      case "launch": {
        dim.createExplosion(c, 0.1, { breaksBlocks: false, causesFire: false });
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            if (e.typeId === "minecraft:player") {
              e.applyKnockback({ x: rnd(0.6), z: rnd(0.6) }, s.force * 1.2);
            } else {
              e.applyImpulse({ x: rnd(0.4), y: s.force, z: rnd(0.4) });
            }
          } catch (err) {}
        }
        spray(dim, c, "minecraft:totem_particle", 40, 3);
        break;
      }

      case "items": {
        for (let i = 0; i < s.count; i++) {
          const p = { x: c.x + rnd(s.spread), y: c.y + 1 + Math.random() * 2, z: c.z + rnd(s.spread) };
          try { dim.spawnItem(new ItemStack(s.item, 1), p); } catch (e) {}
        }
        spray(dim, c, "minecraft:crop_growth_emitter", 30, 3);
        break;
      }

      case "villagers": {
        for (let i = 0; i < s.count; i++) {
          const p = { x: c.x + rnd(s.spread), y: c.y + 1, z: c.z + rnd(s.spread) };
          try {
            const v = dim.spawnEntity("minecraft:villager_v2", p);
            v.nameTag = s.name;
            if (s.baby) { try { v.triggerEvent("minecraft:entity_born"); } catch (e) {} }
          } catch (e) {}
        }
        spray(dim, c, "minecraft:heart_particle", 30, 3);
        break;
      }

      case "weather":
        try { dim.setWeather(s.weather, 24000); } catch (e) {}
        spray(dim, c, "minecraft:water_evaporation_actor_emitter", 40, 4);
        break;

      case "time":
        // AyTntEntity.java: yalnizca GUNDUZSE geceye cevirir (tod < 12300).
        // Kosulsuz set etmek geceyi de basa sariyordu.
        try {
          const tod = world.getTimeOfDay();
          if (tod < 12300) world.setTimeOfDay(s.time);
        } catch (e) {
          try { world.setTimeOfDay(s.time); } catch (e2) {}
        }
        spray(dim, c, "minecraft:end_rod", 60, 4);
        break;

      case "heal": {
        // SADECE OYUNCULAR. Butun varliklara verilirse zombi/iskelet de
        // guclenir - Java tarafinda 88b9de1 ile duzeltilen hata buydu.
        for (const p of dim.getPlayers({ location: c, maxDistance: s.radius })) {
          try {
            // Java KalpTntEntity once tum efektleri temizliyor, sonra veriyor
            for (const eff of p.getEffects()) {
              try { p.removeEffect(eff.typeId); } catch (err) {}
            }
            // Java ayrica olcek degisikligini sifirliyor; Bedrock'ta oyuncu
            // olcegi script'ten hic degistirilemedigi icin bozulmasi da
            // mumkun degil - o adimin Bedrock'ta karsiligi yok.
            p.addEffect("regeneration", 600, { amplifier: 2, showParticles: true });
            p.addEffect("absorption", 600, { amplifier: 4, showParticles: true });
          } catch (err) {}
        }
        spray(dim, c, "minecraft:heart_particle", 80, 5);
        break;
      }

      case "freeze": {
        // BuzTntEntity.java ile birebir:
        //  - setWeather(0, WEATHER_TICKS, true, false) -> YAGMUR (thunder yok).
        //    Bedrock'ta "Snow" diye bir hava tipi YOK; kar, soguk biyomda
        //    yagmurun gorunumudur. "Snow" gecmek sessizce basarisiz oluyordu.
        //  - Tum OYUNCULAR (yaricap sinirli degil), patlatan haric
        //  - SLOWNESS amp 6 + MINING_FATIGUE amp 4, 600 tick
        //  - Gorsel patlama: guc 1.0, blok hasari yok
        // Bedrock'ta setFrozenTicks karsiligi yok - gercek donma gorseli eksik.
        const ticks = (s.freeze_seconds ?? 30) * 20;
        for (const p of dim.getPlayers()) {
          try {
            if (igniterId && p.id === igniterId) continue;   // patlatan haric
            p.addEffect("slowness", ticks, { amplifier: 6, showParticles: true });
            p.addEffect("mining_fatigue", ticks, { amplifier: 4, showParticles: false });
          } catch (err) {}
        }
        try { dim.setWeather("Rain", ticks); } catch (err) {}
        try { dim.createExplosion(c, 1.0, { breaksBlocks: false, causesFire: false }); } catch (err) {}
        const r = s.radius;
        // agir is: tek tick'te degil, dilim dilim
        let dx = -r;
        const job = system.runInterval(() => {
          for (let n = 0; n < 1 && dx <= r; n++, dx++) {
            for (let dz = -r; dz <= r; dz++) {
              for (let dy = -4; dy <= 4; dy++) {
                if (dx * dx + dz * dz > r * r) continue;
                const p = { x: Math.floor(c.x) + dx, y: Math.floor(c.y) + dy, z: Math.floor(c.z) + dz };
                try {
                  const b = dim.getBlock(p);
                  if (!b) continue;
                  const t = b.typeId;
                  if (t === "minecraft:water" || t === "minecraft:flowing_water") b.setType("minecraft:packed_ice");
                  else if (t === "minecraft:lava" || t === "minecraft:flowing_lava") b.setType("minecraft:stone");
                  else if (t !== "minecraft:air") {
                    const up = dim.getBlock({ x: p.x, y: p.y + 1, z: p.z });
                    if (up && up.typeId === "minecraft:air") up.setType("minecraft:snow_layer");
                  }
                } catch (e) {}
              }
            }
          }
          if (dx > r) system.clearRun(job);
        }, 1);
        spray(dim, c, "minecraft:snowflake_particle", 60, 5);
        break;
      }
    }
  } catch (e) {
    console.warn(`[SuperTNT] ${short} patlamasi basarisiz: ${e}`);
  }
}

function spray(dim, c, particle, n, spread) {
  for (let i = 0; i < n; i++) {
    try {
      dim.spawnParticle(particle, {
        x: c.x + rnd(spread), y: c.y + Math.random() * spread, z: c.z + rnd(spread),
      });
    } catch (e) {}
  }
}

// worldLoad her API surumunde yok; olmazsa ilk tick'te yuklenir.
try {
  world.afterEvents.worldLoad.subscribe(() => loadTracked());
} catch (e) {}
system.run(() => {
  loadTracked();
  console.warn(`[SuperTNT] yuklendi - ${Object.keys(SPEC).length} TNT, ${tracked.size} kayitli blok`);
});
'''

if __name__ == "__main__":
    build()
