#!/bin/bash
# run.mjs gercekten bir sey yakaliyor mu?
#
#     bash bedrock/tools/sim/mutations.sh
#
# Hep gecen bir test, gecmesi anlamsiz bir testtir. Bu betik build.py'yi kasten
# bozup run.mjs'in dustugunu dogrular, sonra geri alir. Her mutasyon GERCEKTEN
# yasanmis bir hatanin ta kendisi; yanlarindaki sayilar mutasyonlu kosudan
# olculmustur, tahmin degildir.
set -u
cd "$(dirname "$0")/../../.." || exit 1
BAK=$(mktemp -d)
trap 'cp "$BAK/build.bak" bedrock/build.py; rm -rf bedrock/__pycache__; python3 bedrock/build.py > /dev/null; rm -rf "$BAK"' EXIT
cp bedrock/build.py "$BAK/build.bak"
pass=0
fail=0

run() {   # run <ad> <beklenen metin parcasi>
  rm -rf bedrock/__pycache__
  python3 bedrock/build.py > /dev/null 2>&1
  out=$(node bedrock/tools/sim/run.mjs 2>&1)
  if echo "$out" | grep -q "$2"; then
    echo "  YAKALADI  $1"
    pass=$((pass + 1))
  else
    echo "  KACIRDI   $1  (beklenen: $2)"
    echo "$out" | tail -3 | sed 's/^/            /'
    fail=$((fail + 1))
  fi
}

echo "sim mutasyonlari:"

# 1) tick butcesi yeniden DEGISTIRILEN blogu saysin (olculdu: 113 323 getBlock/tick)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace('''              d++;
              try {
                const b = dim.getBlock({ x: cx + bx, y: cy + by, z: cz + bz }); if (!b) continue;''',
              '''              try {
                const b = dim.getBlock({ x: cx + bx, y: cy + by, z: cz + bz }); if (!b) continue;''', 1)
s = s.replace('''                b.setType("minecraft:air");
              } catch (e) {}
            }
            bx++;''', '''                b.setType("minecraft:air"); d++;
              } catch (e) {}
            }
            bx++;''', 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "tick butcesi sonucu sayiyor (donma)" "tick basina is sinirli"
cp "$BAK/build.bak" bedrock/build.py

# 2) applyKnockback'in 2.x nesne bicimi (Ziplatan TNT hic firlatmiyordu)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace('''              try { e.applyKnockback(rnd(0.6), rnd(0.6), 0.9, s.force * 1.2); }
              catch (e2) { e.applyKnockback({ x: rnd(0.6), z: rnd(0.6) }, s.force * 1.2); }''',
              '''              e.applyKnockback({ x: rnd(0.6), z: rnd(0.6) }, s.force * 1.2);''', 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "yanlis applyKnockback imzasi" "Ziplatan TNT oyuncuyu firlatiyor"
cp "$BAK/build.bak" bedrock/build.py

# 3) creeper morph'u kendi patlamasinda olsun (olculdu: can 0.0)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace('    try { p.addEffect("resistance", 60, { amplifier: 4, showParticles: false }); } catch (e) {}\n', '', 1)
s = s.replace('''    if (hp !== null) {
      system.runTimeout(() => {''', '''    if (false) {
      system.runTimeout(() => {''', 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "creeper morph'u kendini olduruyor" "kendi patlamasinda olmuyor"
cp "$BAK/build.bak" bedrock/build.py

# 4) Kara Delik'te cekme yeniden try'in ilk satiri olsun (korluk+hasar dusuyordu)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace('''            try {
              if (e.typeId === "minecraft:player") e.applyKnockback(dx / len, dz / len, 0.5, 0.1);
              else e.applyImpulse({ x: dx / len * 0.5, y: 0.1, z: dz / len * 0.5 });
            } catch (e2) {}''',
              '''            e.applyImpulse({ x: dx / len * 0.5, y: 0.1, z: dz / len * 0.5 });''', 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "Kara Delik oyuncuda sessizce dusuyor" "Kara Delik oyuncuyu kor ediyor"
cp "$BAK/build.bak" bedrock/build.py

# 5) takma ad donusum dongusunde okunmasin (isim bes tick sonra kaybolurdu)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace('''(nickOf(p) || p.name);''', '''p.name;''', 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "takma adi dongu siliyor" "donusum dongusu takma adi SILMIYOR"
cp "$BAK/build.bak" bedrock/build.py

# 6) Craft Axe hacim tavani kalksin (25 milyon pozisyon tek tick'te)
python3 - "$BAK" <<'PY'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
s = s.replace("const CRAFT_AXE_MAX = 20000;", "const CRAFT_AXE_MAX = 99999999;", 1)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s)
PY
run "Craft Axe hacim tavani yok" "cok buyuk alan reddediliyor"
cp "$BAK/build.bak" bedrock/build.py

# 7) Temizleyici TNT boyutu sifirlamasin (ipucunun ikinci yarisi:
#    "efektleri VE BOYUT degisikliklerini temizler")
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
line = '              if (e.typeId === "minecraft:player") { try { e.triggerEvent("st:size___SIZE_DEFAULT__"); } catch (x) {} }\n'
assert s.count(line) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(line, "", 1))
PYX
run "Temizleyici TNT boyutu birakiyor" "boyutu normale donduruyor"
cp "$BAK/build.bak" bedrock/build.py

# 8) blok kirinca kiliga girme kancasi elde esya aramasin -> her kirmada donusur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '    if (held !== "stnt:blok_kiligi") return;'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "", 1))
PYX
run "blok kiligi elde olmadan da donusturuyor" "esya elde degilken blok kirmak donusturmuyor"
cp "$BAK/build.bak" bedrock/build.py

# 9) yerlesme izgaraya hizalanmasin -> blok gibi durmaz, yarim karede kalir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '    pl.teleport({ x: Math.floor(b.x) + 0.5, y: Math.floor(b.y) + 1, z: Math.floor(b.z) + 0.5 });'
new = '    pl.teleport({ x: b.x, y: b.y + 1, z: b.z });'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, new, 1))
PYX
run "yerlesme kare ortasina hizalamiyor" "kare ortasina hizalaniyor"
cp "$BAK/build.bak" bedrock/build.py

# 10) cikis yolu kapansin -> cocuk blok kiliginda MAHSUR kalir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '''    morphTo(pl, "st:morph_human", "İnsana geri döndün");\n    return;'''
assert s.count(old) == 1, s.count(old)
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, '    return;', 1))
PYX
run "blok kiligindan cikis yolu yok" "insana donuluyor"
cp "$BAK/build.bak" bedrock/build.py

# 11) sinirsiz can tazeleme dongusu kalksin -> "hic bitmez" sozu tutulmaz
python3 - "$BAK" <<'PYX'
import io, sys, re
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = 'p.addEffect("health_boost", __FOREVER_TICKS__, { amplifier: __BOOST_AMP__, showParticles: false });'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, 'void 0;', 1))
PYX
run "sinirsiz can tazelenmiyor" "dongu efekti tazeliyor"
cp "$BAK/build.bak" bedrock/build.py

# 12) geri alma yolu kapansin -> cocuk sinirsiz candan bir daha cikamaz
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '              try { e.setDynamicProperty(FOREVER_PROP, undefined); } catch (x) {}\n'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, '', 1))
PYX
run "Temizleyici sinirsiz cani geri alamiyor" "suresiz cani geri aliyor"
cp "$BAK/build.bak" bedrock/build.py

# 13) Among Us Rapor yine harcanmasin -> "Tek kullanimlik" diyen ipucu yalan
#     olur; cocuk kardesini 3 saniyede bir sinirsiz oldurur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '          consumeHeld(player, "stnt:among_us_report");\n'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, '', 1))
PYX
run "rapor harcanmiyor" "envanterden dusuyor"
cp "$BAK/build.bak" bedrock/build.py

# 14) olum boyutu/kiligi sifirlamasin -> minicik olen cocuk minicik uyanir ve
#     geri donmenin yolunu bulamayabilir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '      if (p.getProperty("st:size") !== __SIZE_DEFAULT__) p.triggerEvent("st:size___SIZE_DEFAULT__");'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, '      void 0;', 1))
PYX
run "olunce boyut sifirlanmiyor" "olunce boyut normale dondu"
cp "$BAK/build.bak" bedrock/build.py

# 15) dunyaya girerken kilik SIFIRLANSIN -> her acilista cocugun kiligi bozulur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '    if (ev.initialSpawn) return;\n'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, '', 1))
PYX
run "dunyaya girerken kilik bozuluyor" "dunyaya girerken kilik korunuyor"
cp "$BAK/build.bak" bedrock/build.py

# 16) POV kamerasinin goz yuksekligi olceklenmesin -> kucukken dunya buyuk
#     gorunmez, kamera sadece normal goz hizasinda durur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "const eye = loc.y + (head.y - loc.y) * scale;"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "const eye = head.y;", 1))
PYX
run "POV kamerasi olceklenmiyor" "kucukken goz ALCALIYOR"
cp "$BAK/build.bak" bedrock/build.py

# 17) normale donunce kamera birakilmasin -> cocuk scripted kamerada mahsur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "      if (camState.has(p.id)) releaseCamera(p);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "      void 0;", 1))
PYX
run "normale donunce kamera birakilmiyor" "normal boyutta kamera birakiliyor"
cp "$BAK/build.bak" bedrock/build.py

# 18) bir esyanin eylemi sessizce hicbir sey yapmasin -> "gorunur bir sey
#     yapiyor" taramasi bunu yakalamali (kod calisir, cocuk hicbir sey gormez)
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
# Ayni satir dosyada uc yerde geciyor; hedef 'case "tunnel"' blogundaki.
i = s.index('case "tunnel": {')
j = s.index("\n", i) + 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s[:j] + "        break;\n" + s[j:])
PYX
run "Delici sessizce hicbir sey yapmiyor" "sessiz kalan"
cp "$BAK/build.bak" bedrock/build.py

# 19) Craft Axe'in tick butcesi kalksin -> kabul edilen alanin tamami TEK
#     tick'te taranir; masif tasin icini doldurmaya calisan cocukta 8000
#     getBlock tek callback'te ve tablet takilir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "const CRAFT_AXE_PER_TICK = 700;"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "const CRAFT_AXE_PER_TICK = 999999;", 1))
PYX
run "Craft Axe tick butcesi yok" "tick'e bolunerek taraniyor"
cp "$BAK/build.bak" bedrock/build.py

# 20) buyume tavan kontrolu kalksin -> cocuk bir bloklik boslukta buyuyup
#     bloklarin icinde sikisir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "          if (SIZE_H[hedef] > SIZE_H[sz] && !tavanVarMi(player, SIZE_H[hedef])) {"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "          if (false) {", 1))
PYX
run "tavan kontrolu yok" "tavan altinda buyume reddediliyor"
cp "$BAK/build.bak" bedrock/build.py

# 21) blok kiligi yine bellekteki tabloya bagli olsun -> dunya yeniden
#     yuklenince cocuk blok gorunur ama ne yerlesebilir ne insana donebilir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "  return BLOCK_MORPH_BY_N[m] || BLOCK_MORPH_BY_N[String(m)] || null;"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "  return (blockNickTmp && blockNickTmp.get(pl.id)) || null;", 1))
PYX
run "blok kiligi yeniden yuklemede kayboluyor" "insana donulebiliyor"
cp "$BAK/build.bak" bedrock/build.py

# 22) Kalp Baltasi oyuncuya yine ek hasar versin -> tek vurusta oldurur,
#     oysa ipucu "agir hasar" diyor
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '        if (ev.hurtEntity.typeId !== "minecraft:player") ev.hurtEntity.applyDamage(1000);'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "        ev.hurtEntity.applyDamage(1000);", 1))
PYX
run "Kalp Baltasi oyuncuyu tek vurusta olduruyor" "oyuncuyu tek vurusta oldurmuyor"
cp "$BAK/build.bak" bedrock/build.py

# 23) Su TNT yine ates blogunu atlasin -> ipucu "atesleri sondurur" diyor ama
#     cocuk atesin ustune atinca hicbir sey olmuyor
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = ('                if (s.onlyAir && t !== "minecraft:air"\n'
       '                    && !(s.douses && t === "minecraft:fire")) continue;')
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, '                if (s.onlyAir && t !== "minecraft:air") continue;', 1))
PYX
run "Su TNT atesi sondurmuyor" "ates blogunu gercekten sonduruyor"
cp "$BAK/build.bak" bedrock/build.py

# 24) ipucunun vaat ettigi ses calinmasin -> "cingirak sesiyle" bos bir soz olur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '        if (s.sound) { try { dim.playSound(s.sound, c, { volume: 1.2 }); } catch (e) {} }'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "", 1))
PYX
run "vaat edilen ses calinmiyor" "cingirak sesini caliyor"
cp "$BAK/build.bak" bedrock/build.py

# 25) firtina yine 20 dakika sursun -> haritanin obur ucundeki kardesin de
#     basina yildirim iner
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "if (s.weather) { try { dim.setWeather(s.weather, s.weatherTicks || 1200); } catch (e) {} }"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, 'if (s.weather) { try { dim.setWeather(s.weather, 24000); } catch (e) {} }', 1))
PYX
run "firtina 20 dakika suruyor" "2 dakikadan uzun hava birakmiyor"
cp "$BAK/build.bak" bedrock/build.py

# 26) transform TNT'leri hicbir blok yazmasin -> Gokkusagi, Makarna, Seker,
#     Gulen Yuz, Karisik Kurusuk, Elmas Diyari ve Lego TNT ici bos kalir.
#     Bu yedisi ESKIDEN testi geciyordu: "blok degisti" olcumu TNT'nin KENDI
#     blogunu sayiyordu, o yuzden her zaman dogruydu.
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "                b.setType(pal[Math.abs(tx * 7 + ty * 13 + tz * 17) % pal.length]);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "                ;", 1))
PYX
run "transform TNT'leri hicbir blok degistirmiyor" "rainbow_tnt (transform: blok yok)"
cp "$BAK/build.bak" bedrock/build.py

# 27) place TNT'leri hicbir blok koymasin -> Buz, Olumcul Su, Mob Dondurucu,
#     Nether, End ve Dag TNT bos patlar
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "                b.setType(s.block);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "                ;", 1))
PYX
run "place TNT'leri hicbir blok koymuyor" "freeze_tnt (place: blok yok)"
cp "$BAK/build.bak" bedrock/build.py

# 28) status TNT'leri hicbir efekt uygulamasin -> Nukleer, Redstone, Pirt ve
#     Uyku TNT yalnizca patlar. Sahte createExplosion'un hasari bunu
#     ortuyordu; artik PATLAMA DISI vurus araniyor.
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "              e.addEffect(ef.id, ef.seconds * 20, { amplifier: ef.amp || 0, showParticles: true });"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "              ;", 1))
PYX
run "status TNT'leri hicbir efekt uygulamiyor" "nuclear_tnt (status: etki yok)"
cp "$BAK/build.bak" bedrock/build.py

# 29) anlik olduren TNT kimseyi oldurmesin -> Zeynep Redstone (r=30) ve Elmas
#     Zirh (r=40) TNT kimseyi olduremeden testi geciyordu
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "            e.applyDamage(1000);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "            ;", 1))
PYX
run "anlik olduren TNT kimseyi oldurmuyor" "elmas_zirh_tnt (instakill: hasar yok)"
cp "$BAK/build.bak" bedrock/build.py

# 30) blok kiran TNT hicbir blok kirmasin -> Bedrock, Cam, Kiyamet, Odun ve
#     Komut TNT. Eskiden ONCEKI TNT'nin hala calisan isi bunlarin blok
#     sayisini degistiriyor ve bes TNT birden bedavaya geciyordu.
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = '                b.setType("minecraft:air");'
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "                ;", 1))
PYX
run "blok kiran TNT hicbir blok kirmiyor" "bedrock_tnt (break: blok yok)"
cp "$BAK/build.bak" bedrock/build.py

# 31) Craft Axe butcesi yine yalnizca while basliginda bakilsin -> duz bir
#     duvar secildiginde tek tick'te 10 000 getBlock, tablet takilir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "              for (let y = by0; y <= by1 && placed < 4096 && d < CRAFT_AXE_PER_TICK; y++)\n                for (let z = bz0; z <= bz1 && placed < 4096 && d < CRAFT_AXE_PER_TICK; z++) {"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(
    old, "              for (let y = by0; y <= by1 && placed < 4096; y++)\n                for (let z = bz0; z <= bz1 && placed < 4096; z++) {", 1))
PYX
run "Craft Axe duz secimde tek tick'te kosuyor" "DUZ secimde de tick'e bolunuyor"
cp "$BAK/build.bak" bedrock/build.py

# 32) Ejderha Nefesi yine yakalanan oyuncudan kimlik okusun -> cocuk cikinca
#     bulut her atisinda istisna atar. (Dongunun KALICI oldugu iddiasi
#     olculdu ve dogru cikmadi: bitis kosulu `t >= total` oldugu icin bir
#     sonraki atis-disi tick bitisi calistiriyor.)
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "        if (e.id === pid) { try { e.applyDamage(a.damage); } catch (err) {} continue; }"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(
    old, "        if (e.id === player.id) { try { e.applyDamage(a.damage); } catch (err) {} continue; }", 1))
PYX
run "Ejderha Nefesi bulutu cikan sahibinde istisna atiyor" "sahibi oyundan cikinca istisna atmiyor"
cp "$BAK/build.bak" bedrock/build.py

# 33) Mega Agac yine yakalanan oyuncudan kimlik okusun -> cocuk cikinca
#     treeBusy'de kimlik kalir, ayni kimlikle geri girse de esya calismaz
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "      treeBusy.delete(treePid);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "      treeBusy.delete(player.id);", 1))
PYX
run "Mega Agac cikan oyuncudan sonra kilitleniyor" "cikan oyuncudan sonra kilitlenmiyor"
cp "$BAK/build.bak" bedrock/build.py

# 34) gecici blok kaydi dunyaya yazilmasin -> geri alma yine yalnizca
#     bellekte kalir: cocuk 10 saniye dolmadan oyundan cikinca su sonsuza
#     kadar durur (buz TNT'de pencere 30 saniye)
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "          const tid = tempEkle(dim.id, cx, cy, cz, r, s.block, s.tempSeconds);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, '          const tid = "kayitsiz";', 1))
PYX
run "gecici blok kaydi dunyaya yazilmiyor" "yeniden yuklemeden sonra su geri aliniyor"
cp "$BAK/build.bak" bedrock/build.py

# 35) sure dunya saati yerine tick sayacina baglansin -> iki saat farkli
#     taban kullaniyor: supurge kaydi ANINDA suresi dolmus sayar ve suyu
#     konar konmaz siler. (Gercekte de ayni sinif: tick sayaci yeniden
#     yuklemede sifirlanir, dunya saati sifirlanmaz.)
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "  a.push({ i: id, d: dimId, x, y, z, r, b: blok, t: wclock() + saniye * 20 });"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "  a.push({ i: id, d: dimId, x, y, z, r, b: blok, t: system.currentTick + saniye * 20 });", 1))
PYX
run "gecici blok suresi tick sayacina bagli" "Su TNT once suyu koyuyor"
cp "$BAK/build.bak" bedrock/build.py

# 36) varis kaydi olmasin -> bekleme suresi dolunca iki ates arasinda ping-pong
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "      if (arr && arr.d === p.dimension.id && arr.x === at.x && arr.y === at.y && arr.z === at.z) continue;"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "", 1))
PYX
run "ender atesi varista geri isinliyor (ping-pong)" "varista geri sicramiyor"
cp "$BAK/build.bak" bedrock/build.py

# 37) tasinan totem baska yuvada mi diye bakilmasin -> her tasimada KOPYA
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "          if (gone && !anySlotHas(con, gone)) { try { con.setItem(i, restoreUnending(gone)); next[i] = gone; } catch (e) {} }"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "          if (gone) { try { con.setItem(i, restoreUnending(gone)); next[i] = gone; } catch (e) {} }", 1))
PYX
run "tasinan tukenmez totem kopyalaniyor" "KOPYALANMIYOR"
cp "$BAK/build.bak" bedrock/build.py

# 38) Nether taramasi butcesiz olsun -> tek tick'te 17 bin getBlock, tablet donar
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "    if (++n >= EF.perTick) return false;"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "", 1))
PYX
run "Nether taramasi tek tick'te bitiyor (donma)" "tick basina butceli"
cp "$BAK/build.bak" bedrock/build.py

# 39) Patlayici Kum'un patlamasi blok kirmasin -> ipucu "Blokları yıkar" diyor,
#     kazan cocuk sadece ses duyar, kum yerinde kalmis gibi olur
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "  try { dim.createExplosion(c, KUM_POWER, { breaksBlocks: true, causesFire: false }); } catch (e) {}"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(
    s.replace(old, "  try { dim.createExplosion(c, KUM_POWER, { breaksBlocks: false, causesFire: false }); } catch (e) {}", 1))
PYX
run "Patlayici Kum blok kirmiyor" "Patlayici Kum blok yikiyor"
cp "$BAK/build.bak" bedrock/build.py

# 40) patlama olayi kumu zincire almasin -> yandaki kum sessizce silinir,
#     "zincirleme patlatir" sozu bos kalir
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = """        if (b.typeId === "stnt:patlayici_kum") {  // zincir: kum da patlar (bkz. kumChain)
          kumChain(dim, b.location);
          lit++;
          continue;
        }
"""
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "", 1))
PYX
run "patlama kumu zincire almiyor" "bir sonraki turda patliyor"
cp "$BAK/build.bak" bedrock/build.py

# 41) zincir tavani kalksin -> kum tarlasi tek turda patlar, tablet donar
python3 - "$BAK" <<'PYX'
import io, sys
s = io.open(sys.argv[1] + "/build.bak", encoding="utf-8").read()
old = "  const n0 = Math.min(KUM_PER_TICK, kumQueue.length);"
assert s.count(old) == 1
io.open("bedrock/build.py", "w", encoding="utf-8").write(s.replace(old, "  const n0 = kumQueue.length;", 1))
PYX
run "kum zinciri tek turda patliyor (donma)" "tur basina"
cp "$BAK/build.bak" bedrock/build.py

echo
echo "yakalanan $pass / kacirilan $fail"
[ "$fail" -eq 0 ]
