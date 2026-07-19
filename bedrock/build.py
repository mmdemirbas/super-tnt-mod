#!/usr/bin/env python3
"""
Super TNT Mod -> Minecraft Bedrock Add-On uretici.

Java modundaki (Fabric) TNT'lerin bir alt kumesini Bedrock Add-On'una
cevirir ve tek .mcaddon olarak paketler.

TASARIM KARARLARI
-----------------
K1. Fitil yerinde yanar. Bedrock'ta "vanilla TNT gibi ateslenip ziplayan
    ozel blok" hazir gelmiyor; ates alan blogun yerine ucan bir varlik
    koymak ayri geometri + render controller yazarligi demek. Bunun yerine
    blok yerinde durur, 4 saniye duman + fitil sesi cikarir, sonra etki
    calisir. Islevsel fark: TNT firlatilamaz/ziplamaz.

K2. Ateslendirme cakmakla. Script `playerInteractWithBlock` olayini dinler,
    elde cakmak varsa fitili baslatir. Redstone ile atesleme YOK (Bedrock'ta
    ozel bloga redstone dinletmek ayri is).

K3. Dokular uretiliyor, vanilla'ya referans verilmiyor. Java tarafinda aile
    TNT'leri `minecraft:block/*_concrete` kullaniyor ama Bedrock'ta vanilla
    doku yollari farkli adlandirilmis (light_gray -> concrete_silver gibi).
    Kirik doku riskini almamak icin 16x16 PNG'ler burada uretiliyor.
    Kendi dokusu olan TNT'ler (diamond, bounce) Java projesinden kopyalanir.

K4. Aile TNT'lerinin tonlari ayristirildi. Java'da abi/anne/baba/bebek
    dordu de ayni pembe; envanterde ayirt edilemiyor. Ayni sicak paletten
    farkli tonlar verildi. Bilincli sapma.

K5. Davranislar Java kaynagindan okundu, uydurulmadi. Sayilar (yaricap,
    adet, sure) ilgili *TntEntity.java dosyalarindan alindi.
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
VERSION = [1, 0, 0]
MIN_ENGINE = [1, 21, 0]

# ---------------------------------------------------------------- TNT tanimlari
# renk: (top, side, bottom) RGB. tex: Java projesinden kopyalanacak taban ad.
TNTS = [
    dict(id="diamond_tnt", tr="Elmas TNT", en="Diamond TNT",
         trtip="Devasa patlama - 10 guc.", entip="Huge explosion - power 10.",
         tex="diamond_tnt", mat="minecraft:diamond",
         effect=dict(kind="explode", power=10)),
    dict(id="bounce_tnt", tr="Ziplatan TNT", en="Bounce TNT",
         trtip="Yakindaki her seyi gokyuzune firlatir. Blok hasari yok.",
         entip="Launches everything nearby into the sky. No block damage.",
         tex="bounce_tnt", mat="minecraft:slime_ball",
         effect=dict(kind="launch", radius=12, force=1.5)),
    dict(id="zeynep_tnt", tr="Zeynep TNT", en="Zeynep TNT",
         trtip="Etrafa 50 bos kagit sacar.", entip="Scatters 50 sheets of paper.",
         color=((44, 62, 158), (44, 62, 158), (44, 62, 158)), mat="minecraft:paper",
         effect=dict(kind="items", item="minecraft:paper", count=50, spread=8.0)),
    dict(id="coklu_zeynep_tnt", tr="Coklu Zeynep TNT", en="Multi Zeynep TNT",
         trtip="Etrafa 'Zeynep' isimli 15 koylu sacar.",
         entip="Scatters 15 villagers named 'Zeynep'.",
         color=((160, 40, 150), (214, 96, 168), (160, 40, 150)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Zeynep", count=15, spread=7.0)),
    dict(id="abi_tnt", tr="Abi TNT", en="Abi TNT",
         trtip="Etrafa 'Abi' isimli 12 koylu sacar.",
         entip="Scatters 12 villagers named 'Abi'.",
         color=((196, 76, 132), (196, 76, 132), (196, 76, 132)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Abi", count=12, spread=6.0)),
    dict(id="anne_tnt", tr="Anne TNT", en="Anne TNT",
         trtip="Etrafa 'Anne' isimli 12 koylu sacar.",
         entip="Scatters 12 villagers named 'Anne'.",
         color=((224, 122, 168), (224, 122, 168), (224, 122, 168)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Anne", count=12, spread=6.0)),
    dict(id="baba_tnt", tr="Baba TNT", en="Baba TNT",
         trtip="Etrafa 'Baba' isimli 12 koylu sacar.",
         entip="Scatters 12 villagers named 'Baba'.",
         color=((150, 60, 110), (150, 60, 110), (150, 60, 110)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Baba", count=12, spread=6.0)),
    dict(id="bebek_tnt", tr="Bebek TNT", en="Bebek TNT",
         trtip="Etrafa 'Bebek' isimli 15 yavru koylu sacar.",
         entip="Scatters 15 baby villagers named 'Bebek'.",
         color=((245, 178, 208), (245, 178, 208), (245, 178, 208)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Bebek", count=15, spread=6.0, baby=True)),
    dict(id="bulut_tnt", tr="Bulut TNT", en="Cloud TNT",
         trtip="Yagmur baslatir ve gokyuzunu bulutlandirir.",
         entip="Starts rain and clouds over the sky.",
         color=((236, 240, 244), (150, 200, 232), (236, 240, 244)), mat="minecraft:white_wool",
         effect=dict(kind="weather", weather="Rain")),
    dict(id="ay_tnt", tr="Ay TNT", en="Moon TNT",
         trtip="Gunduzse gece yapar.", entip="Turns day into night.",
         color=((186, 190, 194), (186, 190, 194), (186, 190, 194)), mat="minecraft:glowstone_dust",
         effect=dict(kind="time", time=18000)),
    dict(id="kalp_tnt", tr="Kalp TNT", en="Heart TNT",
         trtip="20 blok yaricapindaki herkese emme ve yenilenme verir.",
         entip="Gives absorption and regeneration to everyone within 20 blocks.",
         color=((140, 40, 46), (196, 48, 54), (92, 48, 52)), mat="minecraft:gold_ingot",
         effect=dict(kind="heal", radius=20)),
    dict(id="buz_tnt", tr="Buz TNT", en="Ice TNT",
         trtip="14 blok yaricapini buz ve karla kaplar.",
         entip="Covers a 14 block radius with ice and snow.",
         color=((150, 200, 232), (176, 216, 240), (150, 200, 232)), mat="minecraft:packed_ice",
         effect=dict(kind="freeze", radius=14)),
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


def tnt_face(path, base, band=True):
    """TNT'ye benzeyen 16x16: govde rengi + ortada acik bant (yandan)."""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            c = base
            # hafif doku gurultusu (deterministik)
            n = ((x * 7 + y * 13) % 5 - 2) * 0.02
            c = shade(c, 1.0 + n)
            if band and 6 <= y <= 9:
                c = shade(base, 1.35) if (x + y) % 2 == 0 else shade(base, 1.25)
            if x == 0 or y == 0 or x == 15 or y == 15:
                c = shade(c, 0.78)
            row.append(c)
        rows.append(row)
    png(path, rows)


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
                tnt_face(dst, t['color'][idx], band=(face == "side"))
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
    script = SCRIPT_TEMPLATE.replace("__SPEC__", json.dumps(spec, indent=2)) \
                            .replace("__NAMES__", json.dumps(tips, ensure_ascii=False)) \
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
const FUSE = __FUSE__;

const active = new Set();          // "x,y,z" - ayni blok iki kez ateslenmesin
const key = (l) => `${l.x},${l.y},${l.z}`;

world.afterEvents.playerInteractWithBlock.subscribe((ev) => {
  const { block, itemStack, player } = ev;
  if (!block || !itemStack) return;
  if (itemStack.typeId !== "minecraft:flint_and_steel") return;
  const id = block.typeId;
  if (!id.startsWith("stnt:")) return;
  const short = id.slice(5);
  if (!SPEC[short]) return;
  ignite(block.dimension, block.location, short, player);
});

function ignite(dim, loc, short, source) {
  const k = key(loc);
  if (active.has(k)) return;
  active.add(k);
  try {
    dim.playSound("random.fuse", loc, { volume: 1.0 });
    player_msg(source, `§e${NAMES[short] ?? short} §7ateslendi!`);
  } catch (e) {}

  // fitil: yerinde yanar (bkz. build.py K1)
  let left = FUSE;
  const tick = system.runInterval(() => {
    left -= 5;
    try {
      dim.spawnParticle("minecraft:basic_smoke_particle",
        { x: loc.x + 0.5, y: loc.y + 1.1, z: loc.z + 0.5 });
    } catch (e) {}
    if (left <= 0) {
      system.clearRun(tick);
      active.delete(k);
      try {
        const b = dim.getBlock(loc);
        if (b && b.typeId === `stnt:${short}`) b.setType("minecraft:air");
      } catch (e) {}
      detonate(dim, { x: loc.x + 0.5, y: loc.y + 0.5, z: loc.z + 0.5 }, short);
    }
  }, 5);
}

function player_msg(p, msg) { try { p.sendMessage(msg); } catch (e) {} }

function rnd(n) { return (Math.random() - 0.5) * n; }

function detonate(dim, c, short) {
  const s = SPEC[short];
  if (!s) return;
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
        try { world.setTimeOfDay(s.time); } catch (e) {}
        spray(dim, c, "minecraft:end_rod", 60, 4);
        break;

      case "heal": {
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            e.addEffect("absorption", 600, { amplifier: 4, showParticles: true });
            e.addEffect("regeneration", 600, { amplifier: 2, showParticles: true });
          } catch (err) {}
        }
        spray(dim, c, "minecraft:heart_particle", 80, 5);
        break;
      }

      case "freeze": {
        const r = s.radius;
        // agir is: tek tick'te degil, dilim dilim
        let dx = -r;
        const job = system.runInterval(() => {
          for (let n = 0; n < 3 && dx <= r; n++, dx++) {
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

console.warn("[SuperTNT] yuklendi - " + Object.keys(SPEC).length + " TNT");
'''

if __name__ == "__main__":
    build()
