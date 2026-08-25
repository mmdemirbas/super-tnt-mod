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

// ------------------------------------------ 12. hicbir gecici is asili kalmiyor
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
