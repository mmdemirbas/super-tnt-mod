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

echo
echo "yakalanan $pass / kacirilan $fail"
[ "$fail" -eq 0 ]
