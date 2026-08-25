// main.js'i Minecraft olmadan CALISTIRAN test kosucusu.
//
//     node bedrock/tools/sim/run.mjs
//
// check_pack.py kimlikleri, node --check sozdizimini dogrular; ikisi de kodu
// CALISTIRMAZ. Bu kosucu @minecraft/server taklidiyle betigi yukler, olaylari
// tetikler ve tick'leri ilerletir. Yakaladigi sinif: modul yuklenirken atilan
// hata (butun paketi sessizce oldurur), var olmayan cagri, yanlis imza, hic
// bitmeyen interval, tick basina kontrolsuz is.
//
// Yeni bir esya/yetenek eklerken buraya da bir senaryo ekle. Calismadigi
// gorulmemis kod, calismayan koddur.
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const HERE = dirname(fileURLToPath(import.meta.url));
const BP = join(HERE, "..", "..", "super_tnt_BP");
const SRC = join(BP, "scripts", "main.js");
const COPY = join(HERE, "main.copy.mjs");

const js = readFileSync(SRC, "utf8");
writeFileSync(COPY, js);                       // .mjs -> Node ESM olarak yukler

function constOf(name) {
  const m = js.match(new RegExp(`const ${name} = (\\{[\\s\\S]*?\\}|\\[[\\s\\S]*?\\]);`));
  if (!m) throw new Error(`main.js icinde ${name} bulunamadi`);
  return JSON.parse(m[1]);
}
const ITEM_ACTIONS = constOf("ITEM_ACTIONS");
const MORPH_ABIL = constOf("MORPH_ABIL");
const MORPH_MAP = constOf("MORPH_MAP");
const SPEC = constOf("SPEC");
const playerEvents = JSON.parse(readFileSync(join(BP, "entities", "player.json"), "utf8"))
  ["minecraft:entity"].events;

let pass = 0;
const fails = [];
function ok(cond, name, detail = "") {
  if (cond) { pass++; return true; }
  fails.push(`${name}${detail ? " — " + detail : ""}`);
  return false;
}
function eq(a, b, name) { return ok(a === b, name, `beklenen ${JSON.stringify(b)}, gelen ${JSON.stringify(a)}`); }

// ---------------------------------------------------------------- 1. yukleme
const mc = await import("@minecraft/server");
const ui = await import("@minecraft/server-ui");
const { __sim } = mc;
const { __ui } = ui;

let loadError = null;
try { await import("./main.copy.mjs"); } catch (e) { loadError = e; }
ok(!loadError, "main.js modul olarak yukleniyor", loadError && String(loadError.stack).split("\n")[0]);
if (loadError) { report(); process.exit(1); }

for (const [kind, name] of [["after", "itemUse"], ["after", "itemCompleteUse"],
  ["after", "playerPlaceBlock"], ["after", "playerBreakBlock"], ["after", "entityHurt"],
  ["after", "entityHitEntity"], ["before", "explosion"], ["before", "playerInteractWithEntity"]]) {
  ok(__sim.subCount(kind, name) > 0, `olay kaydi var: ${kind}.${name}`);
}
ok(__sim.jobCount() >= 8, "surekli dongular kuruldu", `${__sim.jobCount()} is`);

const newPlayer = (name, loc) => {
  const p = __sim.addPlayer(name, loc);
  p.__events = playerEvents;
  p.props.set("st:morph", 0);
  p.props.set("st:size", 2);
  return p;
};

function fresh() {
  __sim.reset(); __ui.reset();
  return newPlayer("Zeynep", { x: 0, y: 64, z: 0 });
}
const bars = () => __sim.state.log.actionBars.map((b) => b.msg);
// reset() kalici dongulari SILMEZ (silmemeli — main.js'in kendi dongulerini
// oldururdu). Bir onceki senaryodan kalan gecici isler (bulut, agac, zincir)
// yeni senaryonun olcumune karisir; once onlarin bitmesini bekle.
const settle = () => __sim.tick(400);

// ------------------------------------------------- 2. her esya tiklanabiliyor
{
  const p = fresh();
  __sim.state.viewBlock = { block: { typeId: "minecraft:oak_sapling", location: { x: 0, y: 63, z: 8 }, dimension: p.dimension } };
  const mob = __sim.addMob("minecraft:cow", { x: 0, y: 64, z: 3 });
  __sim.state.viewEntities = [{ entity: mob }];
  let threw = null;
  for (const [id, a] of Object.entries(ITEM_ACTIONS)) {
    __ui.reply({ canceled: true, cancelationReason: "UserClosed" });
    try {
      __sim.fire("after", "itemUse", { itemStack: { typeId: `stnt:${id}` }, source: p });
      __sim.fire("after", "itemCompleteUse", { itemStack: { typeId: `stnt:${id}` }, source: p });
      __sim.tick(3);
    } catch (e) { threw = `${id}: ${e && e.message}`; break; }
  }
  ok(!threw, "her esyanin kullanimi hatasiz calisiyor", threw);
  ok(__sim.errors.length === 0, "esya kullanimlari tick'te hata uretmiyor", __sim.errors[0]);
  ok(Object.keys(ITEM_ACTIONS).length >= 30, "eylemi olan esya sayisi",
     String(Object.keys(ITEM_ACTIONS).length));
}

// -------------------------------------------- 3. creeper donusumu oldurmuyor
for (const [n, label] of [[1, "creeper"], [84, "Dev Creeper"]]) {
  const p = fresh();
  p.props.set("st:morph", n);
  p.isSneaking = true;
  p.health = 20;
  __sim.tick(10);                                   // yetenek dongusu 5 tick'te bir
  const exp = __sim.state.log.explosions;
  ok(exp.length > 0, `${label}: comelince patliyor`);
  ok(!p.dead, `${label}: kendi patlamasinda olmuyor`, `can ${p.health.toFixed(1)}`);
  eq(Math.round(p.health), 20, `${label}: cani geri konuyor`);
  if (n === 84) ok(exp[0].opts.breaksBlocks === true, "Dev Creeper blok kiriyor");
  if (n === 1) ok(exp[0].opts.breaksBlocks === false, "vanilla creeper blok kirmiyor");
}

// ---------------------------------------------------- 4. Ejderha Nefesi bulutu
{
  settle();
  const p = fresh();
  const a = ITEM_ACTIONS["ejderha_nefesi"];
  __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: { x: 0, y: 63, z: 6 }, dimension: p.dimension } };
  const victim = __sim.addMob("minecraft:zombie", { x: 0, y: 64, z: 6 });
  victim.health = 100;
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:ejderha_nefesi" }, source: p });
  __sim.tick(a.pulse);                              // tam bir saniye
  eq(victim.damages.length, 1, "bulut saniyede BIR kez vuruyor");
  eq(victim.damages[0], 2, "vurus TAM bir kalp (2 hasar)");
  ok(__sim.state.log.particles.includes("minecraft:dragon_breath_lingering"), "mor nefes parcacigi cikiyor");
  __sim.tick(a.seconds * 20 + 20);
  eq(victim.damages.length, a.seconds, `bulut ${a.seconds} saniye vuruyor, sonra duruyor`);
  const before = victim.damages.length;
  __sim.tick(200);
  eq(victim.damages.length, before, "suresi bitince interval kendini durduruyor");
}
{
  // Es zamanli bulut tavani. TEK oyuncu tavana ULASAMAZ: bekleme 3 sn, bulut 12
  // sn yasiyor -> en fazla dort bulut. Tavan cok oyunculu durum icin; senaryo da
  // oyle kurulmali, yoksa test hicbir seyi sinamaz.
  settle();
  const p = fresh();
  const others = [newPlayer("Ali", { x: 20, y: 64, z: 0 }), newPlayer("Efe", { x: 40, y: 64, z: 0 })];
  const all = [p, ...others];
  __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: { x: 0, y: 63, z: 6 }, dimension: p.dimension } };
  const cd = ITEM_ACTIONS["ejderha_nefesi"].cd;
  let refused = 0;
  for (let round = 0; round < 4; round++) {
    for (const q of all) {
      __sim.state.log.actionBars.length = 0;
      __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:ejderha_nefesi" }, source: q });
      if (bars().some((m) => m.includes("Çok fazla bulut"))) refused++;
    }
    __sim.tick(cd + 1);
  }
  ok(refused > 0, "uc oyuncu birden bulut acinca tavan devreye giriyor", `reddedilen ${refused}`);
}

// -------------------------------------------------------- 5. Isim Degistirme
{
  const p = fresh();
  __ui.reply({ canceled: false, formValues: ["Ejderha", 3] });      // 3 = Kirmizi
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:isim_degistirici" }, source: p });
  await Promise.resolve(); await Promise.resolve();
  eq(p.nameTag, "§cEjderha", "isim ve renk uygulaniyor");
  __sim.tick(10);
  eq(p.nameTag, "§cEjderha", "donusum dongusu takma adi SILMIYOR");

  p.props.set("st:morph", 5);                                        // donus
  __sim.tick(10);
  eq(p.nameTag, "", "donusmusken isim gizleniyor");
  p.props.set("st:morph", 0);
  __sim.tick(10);
  eq(p.nameTag, "§cEjderha", "insana donunce takma ad geri geliyor");

  __ui.reply({ canceled: false, formValues: ["   ", 0] });           // bos = sifirla
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:isim_degistirici" }, source: p });
  await Promise.resolve(); await Promise.resolve();
  __sim.tick(10);
  eq(p.nameTag, "Zeynep", "bos onay gercek isme donduruyor");

  const max = ITEM_ACTIONS["isim_degistirici"].maxLen;
  __ui.reply({ canceled: false, formValues: ["A".repeat(max + 30) + "\nikinci satir", 0] });
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:isim_degistirici" }, source: p });
  await Promise.resolve(); await Promise.resolve();
  eq(p.nameTag.replace(/^§./, "").length, max, "uzun isim kesiliyor");
  ok(!p.nameTag.includes("\n"), "satir sonu temizleniyor");
}

// ----------------------------------------------- 6. 87 donusumun hepsi calisiyor
{
  const p = fresh();
  let bad = null, abil = 0;
  for (const [typeId, evName] of Object.entries(MORPH_MAP)) {
    try {
      p.triggerEvent(evName);
      const n = p.getProperty("st:morph");
      if (typeof n !== "number" || n === 0) { bad = `${typeId}: st:morph ${n}`; break; }
      if (MORPH_ABIL[String(n)]) {
        abil++;
        p.isSneaking = true;
        __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: { x: 0, y: 63, z: 10 }, dimension: p.dimension } };
        __sim.tick(6);
        p.health = 20;                              // patlayan/vuran morph'lar icin
      }
    } catch (e) { bad = `${typeId}: ${e && e.message}`; break; }
  }
  ok(!bad, "87 donusumun hepsi tetikleniyor ve yetenegi calisiyor", bad);
  ok(abil >= 55, "yetenekli morph sayisi", String(abil));
  ok(__sim.errors.length === 0, "morph yetenekleri tick'te hata uretmiyor", __sim.errors[0]);
}

// ------------------------------------- 7. mayin dunya yeniden yuklenince patlar
{
  const p = fresh();
  const loc = { x: 5, y: 64, z: 5 };
  __sim.setBlock("minecraft:overworld", 5, 64, 5, "stnt:yakinlik_mayini");
  __sim.fire("after", "playerPlaceBlock", {
    block: { typeId: "stnt:yakinlik_mayini", location: loc, dimension: p.dimension }, player: p,
  });
  __sim.tick(60);
  __sim.state.log.explosions.length = 0;
  __sim.reload();                                   // tick sayaci sifirlanir, saat kalir
  await import("./main.copy.mjs");                  // (modul zaten yuklu; dongular yeniden kurulmaz)
  // reload dongulari sildi; mayin taramasini elle canlandirmak yerine kalici
  // saatin kayitli degerin ONUNDE oldugunu dogruluyoruz — kosul buydu.
  const mines = JSON.parse(mc.world.getDynamicProperty("stnt:mines") || "{}");
  const rec = Object.values(mines)[0];
  ok(rec && typeof rec.armed === "number", "mayin diske yazildi");
  ok(rec && rec.armed < mc.world.getAbsoluteTime(), "kurulma zamani KALICI saate gore gecmiste",
    rec ? `armed ${rec.armed}, saat ${mc.world.getAbsoluteTime()}, tick ${mc.system.currentTick}` : "");
  ok(rec && rec.armed > mc.system.currentTick + 1000, "kayitli deger tick sayacinin cok ilerisinde (eski hata bu yuzden olusuyordu)");
}

// ------------------------------- 8. tick butcesi: bos alanda TNT donduruyor mu
// Eski hata: butce DEGISTIRILEN blogu sayiyordu. Cam TNT (r=30) camsiz arazide
// tum kureyi tek tick'te tariyordu (~113 000 getBlock). Simdi INCELENEN sayiliyor.
function detonateTnt(short, p, at) {
  __sim.setBlock(p.dimension.id, at.x, at.y, at.z, `stnt:${short}`);
  __sim.fire("after", "playerPlaceBlock", {
    block: { typeId: `stnt:${short}`, location: at, dimension: p.dimension }, player: p,
  });
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:kontrol_kumandasi" }, source: p });
}
{
  settle();
  const p = fresh();
  __sim.state.ground = -100;                        // her yer hava: hicbir blok eslesmez
  __sim.state.counters.maxGetBlockPerTick = 0;
  detonateTnt("cam_tnt", p, { x: 0, y: 100, z: 0 });
  __sim.tick(400);
  const peak = __sim.state.counters.maxGetBlockPerTick;
  ok(peak > 0, "Cam TNT gercekten calisti", `zirve ${peak}`);
  ok(peak < 12000, "bos alanda tick basina is sinirli kaliyor",
     `zirve ${peak} getBlock/tick (butce bozukken ~113 000)`);
  __sim.state.ground = 63;
}
{
  settle();
  const p = fresh();
  __sim.state.ground = -100;
  __sim.state.counters.maxGetBlockPerTick = 0;
  detonateTnt("kiyamet_tnt", p, { x: 0, y: 150, z: 0 });
  __sim.tick(600);
  ok(__sim.state.counters.maxGetBlockPerTick < 20000, "Kiyamet TNT havada da tick basina sinirli",
     `zirve ${__sim.state.counters.maxGetBlockPerTick} (butce bozukken ~180 000)`);
  __sim.state.ground = 63;
}

// ------------------------- 9. Ziplatan TNT oyuncuyu GERCEKTEN firlatiyor mu
// Taklit, applyKnockback'in NESNE bicimini 1.14'te oldugu gibi reddediyor.
// Yanlis imzayla cagrilirsa savurma hic olmaz ve bu test duser.
{
  settle();
  const p = fresh();
  detonateTnt("bounce_tnt", p, { x: 0, y: 64, z: 2 });
  __sim.tick(200);
  ok(p.knockbacks.length > 0, "Ziplatan TNT oyuncuyu firlatiyor (dort sayili imza)",
     `savurma sayisi ${p.knockbacks.length}`);
}
{
  settle();
  const p = fresh();
  const mob = __sim.addMob("minecraft:cow", { x: 3, y: 64, z: 0 });
  const other = newPlayer("Efe", { x: 2, y: 64, z: 0 });
  detonateTnt("magnet_tnt", p, { x: 0, y: 64, z: 0 });
  __sim.tick(200);
  ok(other.knockbacks.length > 0 || mob.knockbacks.length > 0, "Miknatis TNT gercekten cekiyor");
}

// ------------------------ 10. Kara Delik diger oyuncuya korluk + hasar veriyor
{
  settle();
  const p = fresh();
  const other = newPlayer("Efe", { x: 2, y: 64, z: 0 });
  other.health = 20;
  __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:black_hole" }, source: p });
  __sim.tick(5);
  ok(other.effects.has("blindness"), "Kara Delik oyuncuyu kor ediyor");
  ok(other.damages.length > 0, "Kara Delik oyuncuya hasar veriyor");
  ok(other.knockbacks.length > 0, "Kara Delik oyuncuyu cekiyor (applyImpulse degil knockback)");
}

// ------------------------------------- 11. Craft Axe hacim tavani tutuyor mu
{
  settle();
  const p = fresh();
  const use = (loc) => {
    p.location = { ...loc };
    __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: loc, dimension: p.dimension } };
    __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:craft_axe" }, source: p });
    __sim.tick(2);
  };
  use({ x: 0, y: 64, z: 0 });
  __sim.state.log.actionBars.length = 0;
  __sim.state.counters.maxGetBlockPerTick = 0;
  use({ x: 400, y: 120, z: 400 });                  // kocaman kutu
  ok(bars().some((m) => m.includes("çok büyük")), "cok buyuk alan reddediliyor", bars().join(" | ").slice(0, 120));
  ok(__sim.state.counters.maxGetBlockPerTick < 5000, "reddedilen alan taranmiyor",
     `zirve ${__sim.state.counters.maxGetBlockPerTick}`);
}

// -------------------------------------------------- 12. Blok Kiligi
// Uc hareket birbirine karismamali: KIRMAK kiliga sokar, BASILI TUTMAK
// yerlestirir, GOKYUZUNE bakip basili tutmak insana dondurur.
{
  settle();
  const BLOCK_MORPH_MAP = constOf("BLOCK_MORPH_MAP");
  const p = fresh();
  const breakBlock = (id, holding) => {
    p.mainhand = holding;
    __sim.fire("after", "playerBreakBlock", {
      player: p, dimension: p.dimension,
      block: { typeId: "minecraft:air", location: { x: 0, y: 63, z: 2 }, dimension: p.dimension },
      brokenBlockPermutation: { type: { id } },
      itemStackBeforeBreak: holding ? { typeId: holding } : undefined,
    });
    __sim.tick(2);
  };
  const use = () => {
    __sim.fire("after", "itemUse", { itemStack: { typeId: "stnt:blok_kiligi" }, source: p });
    __sim.tick(2);
  };

  // (a) esya elde DEGILKEN kirmak hicbir sey yapmamali
  breakBlock("minecraft:stone", "minecraft:diamond_pickaxe");
  eq(p.getProperty("st:morph"), 0, "esya elde degilken blok kirmak donusturmuyor");

  // (b) esya eldeyken kirmak o bloga donusturur
  breakBlock("minecraft:stone", "stnt:blok_kiligi");
  ok(p.getProperty("st:morph") > 0, "blok kirinca o bloga donusuluyor",
     `st:morph ${p.getProperty("st:morph")}`);
  const stoneN = p.getProperty("st:morph");

  // (c) baska bir blok baska bir kiliga sokmali
  breakBlock("minecraft:gold_block", "stnt:blok_kiligi");
  ok(p.getProperty("st:morph") !== stoneN, "farkli blok farkli kiliga sokuyor");

  // (d) kiligi olmayan blok: uyari, kilik degismez
  const before = p.getProperty("st:morph");
  __sim.state.log.actionBars.length = 0;
  breakBlock("minecraft:beacon", "stnt:blok_kiligi");
  eq(p.getProperty("st:morph"), before, "kiligi olmayan blok kiligi degistirmiyor");
  ok(bars().some((m) => m.includes("kılığı yok")), "kiligi olmayan blok soyleniyor", bars().join("|"));

  // (e) BLOKKEN bir yere basili tutmak oraya yerlestirir (izgaraya hizali)
  breakBlock("minecraft:stone", "stnt:blok_kiligi");
  __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: { x: 7, y: 63, z: 9 }, dimension: p.dimension } };
  use();
  eq(p.location.x, 7.5, "yerlesince X kare ortasina hizalaniyor");
  eq(p.location.y, 64, "yerlesince blogun USTUNE oturuyor");
  eq(p.location.z, 9.5, "yerlesince Z kare ortasina hizalaniyor");
  ok(p.getProperty("st:morph") > 0, "yerlesmek kiligi bozmuyor");

  // (f) GOKYUZUNE bakip basili tutmak insana donduruyor
  __sim.state.viewBlock = null;
  use();
  eq(p.getProperty("st:morph"), 0, "gokyuzune bakip basili tutunca insana donuluyor");

  // (g) blok kiliginda DEGILKEN kullanmak yol gosteriyor
  __sim.state.log.actionBars.length = 0;
  use();
  ok(bars().some((m) => m.includes("Bir blok kır")), "kilikta degilken ne yapacagi yaziliyor");

  // (h) baska bir morph'a gecince blok kiligi hafizasi temizlenir: mob'ken
  //     "yerlesme" calismamali, yol gosterme yazisi cikmali
  p.triggerEvent("st:morph_creeper");
  __sim.state.viewBlock = { block: { typeId: "minecraft:stone", location: { x: 3, y: 63, z: 3 }, dimension: p.dimension } };
  __sim.state.log.actionBars.length = 0;
  const where = { ...p.location };
  use();
  ok(bars().some((m) => m.includes("Bir blok kır")), "mob kiligindayken yerlesme calismiyor");
  eq(p.location.x, where.x, "mob kiligindayken isinlanma yok");
}

// ------------------------------ 13. YETMIS TNT'nin HEPSI patliyor mu
// Simdiye kadar dort TNT denendi. Bir TNT'nin spec'i bozuksa (olmayan blok,
// eksik alan, yanlis palet) hata try icinde kaybolur ve TNT oyunda "patladi
// ama hicbir sey olmadi" gorunur. Hepsini tek tek atesle.
{
  const baseJobs = (settle(), __sim.jobCount());
  const broken = [];
  const heavy = [];
  const silent = [];
  // "Patladi ama hicbir sey olmadi" hatasini yakalamak icin TURE GORE bekle:
  // blok isleyen TNT blok degistirmeli, sacan TNT esya birakmali, etki veren
  // TNT yakindaki cana etki islemeli. Yoksa test yalnizca "hata firlatmadi"
  // der ki bunu bos bir fonksiyon da saglar.
  const seedFor = (f) => {
    const first = Array.isArray(f) ? f[0] : f;
    if (!first) return "minecraft:stone";
    if (first === "log" || first === "wood") return "minecraft:oak_log";
    if (first === "leaves") return "minecraft:oak_leaves";
    return `minecraft:${first}`;
  };
  const EXPECTS = { place: "blok", transform: "blok", break: "blok", paint: "blok",
                    scatter: "esya", spawn: "varlik", status: "etki", instakill: "hasar" };
  const at = { x: 0, y: 64, z: 40 };
  for (const short of Object.keys(SPEC)) {
    const spec = SPEC[short];
    __sim.reset(); __ui.reset();
    const p = newPlayer("Zeynep", { x: 0, y: 70, z: -60 });   // patlamalarin disinda
    // Hedef canli: status / instakill / spawn olculebilsin.
    const target = __sim.addMob("minecraft:cow", { x: at.x, y: at.y, z: at.z + 1 });
    target.health = 400;
    // Patlamanin MENZILINDE ikinci bir oyuncu: "players" hedefli TNT'ler
    // (Zeynep Komut TNT) ve boyut/temizleme etkileri ancak boyle olculur.
    const near = newPlayer("Efe", { x: at.x + 1, y: at.y, z: at.z });
    near.health = 400;
    target.addEffect("poison", 200, { amplifier: 0 });        // temizleyici icin
    near.addEffect("poison", 200, { amplifier: 0 });
    near.props.set("st:size", 0);                             // kucultulmus oyuncu
    if (spec.kind === "break") {                              // filtreye uyan arazi ser
      const seed = seedFor(spec.filter), R = Math.min(spec.radius || 8, 12);
      for (let x = -R; x <= R; x++) for (let y = -R; y <= R; y++) for (let z = -R; z <= R; z++) {
        __sim.setBlock(p.dimension.id, at.x + x, at.y + y, at.z + z, seed);
      }
    }
    const seededBlocks = __sim.state.blocks.size;
    const seededEnts = __sim.state.entities.length;
    __sim.state.counters.maxGetBlockPerTick = 0;
    const before = __sim.state.log;
    try {
      detonateTnt(short, p, at);
      __sim.tick(700);
    } catch (e) { broken.push(`${short}: ${e && e.message}`); continue; }
    if (__sim.errors.length) { broken.push(`${short}: ${__sim.errors[0].split("\n")[0]}`); __sim.errors.length = 0; continue; }
    const peak = __sim.state.counters.maxGetBlockPerTick;
    if (peak > 20000) heavy.push(`${short} ${peak}`);
    const want = EXPECTS[spec.kind];
    const got = {
      blok: __sim.state.blocks.size !== seededBlocks ||
            [...__sim.state.blocks.values()].some((v) => v === "minecraft:air"),
      esya: before.items.length > 0,
      varlik: __sim.state.entities.length > seededEnts,
      etki: target.effects.size !== 1 || near.effects.size !== 1 ||
            near.getProperty("st:size") !== 0 ||
            target.damages.length > 0 || near.damages.length > 0 ||
            before.commands.length > 0 || before.items.length > 0,
      hasar: target.damages.length > 0 || target.dead,
    };
    // Virus TNT bilerek hicbir sey yapmaz — ipucu da oyle diyor ("Aslinda
    // hicbir sey olmaz"). Sozlesme tutuyor; istisna burada yazili olsun.
    if (short === "virus_tnt") { /* saka TNT'si: hicbir sey yapmamasi DOGRU */ }
    else if (want && !got[want]) silent.push(`${short} (${spec.kind}: ${want} yok)`);
    else if (!want && before.explosions.length + before.particles.length + before.sounds.length === 0) {
      silent.push(`${short} (${spec.kind}: hicbir iz yok)`);
    }
    if (process.env.STNT_DIAG) {
      console.log(`  ${short.padEnd(24)} zirve ${String(peak).padStart(6)}  patlama ${before.explosions.length}` +
        `  parcacik ${before.particles.length}  ses ${before.sounds.length}  blok ${__sim.state.blocks.size}`);
    }
  }
  ok(broken.length === 0, `${Object.keys(SPEC).length} TNT'nin hepsi hatasiz patliyor`, broken.slice(0, 4).join(" | "));
  ok(heavy.length === 0, "hicbir TNT tick basina 20 000 getBlock'u asmiyor", heavy.slice(0, 4).join(" | "));
  ok(silent.length === 0, "her TNT TURUNE GORE beklenen isi yapiyor", silent.join(", "));
  settle();
  ok(__sim.jobCount() <= baseJobs, "70 patlamadan sonra asili is kalmiyor",
     `once ${baseJobs}, sonra ${__sim.jobCount()}`);
}

// ---------------- 15. Temizleyici TNT ipucunun ikinci yarisini da yapiyor mu
// "Tum efektleri VE BOYUT degisikliklerini temizler" — boyut kismi uzun sure
// yapilmiyordu: kucultulmus cocuk temizleyiciyi patlatip kucuk kaliyordu.
{
  settle();
  const p = fresh();
  const near = newPlayer("Efe", { x: 1, y: 64, z: 10 });
  near.addEffect("poison", 200, { amplifier: 0 });
  near.props.set("st:size", 0);                     // kucultulmus
  detonateTnt("cleanse_tnt", p, { x: 0, y: 64, z: 10 });
  __sim.tick(200);
  ok(!near.effects.has("poison"), "Temizleyici TNT efektleri siliyor");
  eq(near.getProperty("st:size"), 2, "Temizleyici TNT boyutu normale donduruyor");
}

// ------------------------------------------ 14. hicbir gecici is asili kalmiyor
{
  const before = __sim.jobCount();
  settle(); settle();
  const after = __sim.jobCount();
  ok(after <= before, "gecici isler bitiyor, kalici dongu sayisi buyumuyor",
     `once ${before}, sonra ${after}`);
  ok(after < 40, "asili kalan is yok", `${after} is`);
}

report();

function report() {
  const total = pass + fails.length;
  if (fails.length) {
    console.log(`HATA — ${total} testten ${fails.length} tanesi gecmedi:`);
    for (const f of fails) console.log("  -", f);
    process.exitCode = 1;
  } else {
    console.log(`gecti — ${total} calisan test`);
  }
}
