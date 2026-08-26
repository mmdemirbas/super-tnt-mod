#!/bin/bash
# check_pack.py gercekten bir sey yakaliyor mu?
#
#     bash bedrock/tools/mutation_test.sh
#
# Hep gecen bir denetim, gecmesi anlamsiz bir denetimdir. Bu betik paketi ve
# build.py'yi kasten bozup check_pack.py'nin her birini yakaladigini dogrular,
# sonra her seyi geri alir. Yeni bir denetim eklerken buraya da onu tetikleyen
# bir mutasyon ekle; yoksa denetimin ise yarayip yaramadigini kimse bilmez.
#
# Mutasyon 5, gercekten yasanmis bir hatanin ta kendisi: ghast morph'unun
# dokusu "textures/entity/ghast" yazilmisti, dogrusu ".../ghast/ghast".
set -u
cd "$(dirname "$0")/../.." || exit 1
RP=bedrock/super_tnt_RP
BP=bedrock/super_tnt_BP
BAK=$(mktemp -d)
trap 'rm -rf "$BAK"' EXIT
pass=0
fail=0

run() {   # run <ad> <beklenen metin parcasi>
  rm -rf bedrock/__pycache__          # bkz. check_pack.py: bayat .pyc tuzagi
  out=$(python3 bedrock/tools/check_pack.py 2>&1)
  if echo "$out" | grep -q "$2"; then
    echo "  YAKALADI  $1"
    pass=$((pass + 1))
  else
    echo "  KACIRDI   $1  (beklenen: $2)"
    echo "$out" | head -3 | sed 's/^/            /'
    fail=$((fail + 1))
  fi
}

echo "mutasyon testleri:"

# 1) dil satiri silinirse -> oyunda ham anahtar gorunur
cp "$RP/texts/tr_TR.lang" "$BAK/lang.bak"
grep -v "^stnt.tip.mega_gubre=" "$BAK/lang.bak" > "$RP/texts/tr_TR.lang"
run "eksik tr ipucu satiri" "tr_TR ipucu yok"
cp "$BAK/lang.bak" "$RP/texts/tr_TR.lang"

# 2) render controller olmayan bir doku kaydina bakarsa -> gorunmez model
cp "$RP/render_controllers/morph.render_controllers.json" "$BAK/rc.bak"
python3 - <<'PY'
import json
p = "bedrock/super_tnt_RP/render_controllers/morph.render_controllers.json"
d = json.load(open(p, encoding="utf-8"))
d["render_controllers"]["controller.render.morph.creeper.third_person"]["textures"] = ["Texture.yok"]
json.dump(d, open(p, "w", encoding="utf-8"))
PY
run "cozulmeyen doku kaydi" "doku kaydi yok"
cp "$BAK/rc.bak" "$RP/render_controllers/morph.render_controllers.json"

# 3) item ikonu doku atlasindan silinirse -> mor-siyah kare
cp "$RP/textures/item_texture.json" "$BAK/itex.bak"
python3 - <<'PY'
import json
p = "bedrock/super_tnt_RP/textures/item_texture.json"
d = json.load(open(p, encoding="utf-8"))
del d["texture_data"]["stnt_ses_saldirisi"]
json.dump(d, open(p, "w", encoding="utf-8"))
PY
run "ikon atlasta yok" "item_texture.json'da yok"
cp "$BAK/itex.bak" "$RP/textures/item_texture.json"

# 4) betikte olmayan bir parcacik -> sessizce hicbir sey olmaz
cp "$BP/scripts/main.js" "$BAK/main.bak"
sed 's/minecraft:sonic_explosion/minecraft:olmayan_parcacik/' "$BAK/main.bak" > "$BP/scripts/main.js"
run "olmayan parcacik" "diye bir parcacik yok"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 5) morph tablosunda yanlis doku adi -> gorunmez oyuncu (yasanmis ghast hatasi)
cp bedrock/build.py "$BAK/build.bak"
sed 's|tex="textures/entity/ghast/ghast"|tex="textures/entity/ghast"|' "$BAK/build.bak" > bedrock/build.py
run "morph dokusu vanilla'da yok (eski ghast hatasi)" "doku 'textures/entity/ghast' ne vanilla"
cp "$BAK/build.bak" bedrock/build.py

# 6) ipucu ile davranis ayrisirsa -> tooltip sozlesmesi bozulur
sed 's/^BOOST_HP = POTION_BASE_HP + 4 \* (BOOST_AMP + 1).*$/BOOST_HP = POTION_BASE_HP + 8 * (BOOST_AMP + 1)/' "$BAK/build.bak" > bedrock/build.py
run "can matematigi ipucu ile uyusmuyor" "can, ipucu"   # BOOST_HP degisse de tutar
cp "$BAK/build.bak" bedrock/build.py

# 7) menuden bir kategori duserse -> o mob'lara ulasilamaz
sed 's/for c in MORPH_CATS\]/for c in MORPH_CATS][:3]/' "$BAK/build.bak" > bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1
run "menude eksik kategori" "menude"
cp "$BAK/build.bak" bedrock/build.py

# 8) spray() yardimcisina olmayan parcacik -> hicbir gorsel efekt yok
cp "$BP/scripts/main.js" "$BAK/main2.bak"
sed 's/minecraft:mob_portal/minecraft:mob_portal_yok/' "$BAK/main2.bak" > "$BP/scripts/main.js"
run "spray() parcacigi yok (yasanmis portal_particle hatasi)" "diye bir parcacik yok"
cp "$BAK/main2.bak" "$BP/scripts/main.js"

# 9) veri tablosundaki parcacik kimligi bozulursa -> TNT efektsiz patlar
sed 's/particle="minecraft:mob_portal"/particle="minecraft:yok_boyle"/' "$BAK/build.bak" > bedrock/build.py
run "TNT tablosunda olmayan parcacik" "diye bir parcacik yok"
cp "$BAK/build.bak" bedrock/build.py

# 10) paketin kendi morph modeli RP'ye konmazsa -> gorunmez oyuncu
mv "$RP/models/entity/ender_send.geo.json" "$BAK/geo.bak"
run "kendi modelimiz ship edilmiyor" "ne RP.de ship ediliyor"
mv "$BAK/geo.bak" "$RP/models/entity/ender_send.geo.json"

# 11) donusum ipucu yetenegi yazmazsa -> cocuk gucunu ogrenemez
sed 's/    return " · ".join(parts)/    return ""/' "$BAK/build.bak" > bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1
run "ipucu yetenegi yazmiyor" "ipucu"
cp "$BAK/build.bak" bedrock/build.py

# 12) tek sayili hasar -> yarim kalp gider, ipucu "1 kalp" diyor: sozlesme kirilir
sed 's/type="dragon_breath", radius=5, damage=2/type="dragon_breath", radius=5, damage=3/' "$BAK/build.bak" > bedrock/build.py
run "tek sayili hasar (yarim kalp)" "yarim kalp goturur"
cp "$BAK/build.bak" bedrock/build.py

# 13) ipucundaki yaricap koddan ayrilirsa -> cocuk yanlis mesafeye gore kacar
sed 's/radius=14, onlyAir=False, tempSeconds=30/radius=9, onlyAir=False, tempSeconds=30/' "$BAK/build.bak" > bedrock/build.py
run "ipucu yaricapi koddan farkli" "blok yaricap diyor"
cp "$BAK/build.bak" bedrock/build.py

# 14) can hedefi Bedrock'un amplifier tavanini asarsa -> esya sessizce bos tiklar
sed 's/^BOOST_AMP = 255 .*$/BOOST_AMP = 400/' "$BAK/build.bak" > bedrock/build.py
run "amplifier 255 tavanini asiyor" "efekt uygulanmaz"
cp "$BAK/build.bak" bedrock/build.py

# 15) isim sinirini ipucu yazmazsa -> cocuk kesilen ismi hata sanir
sed 's/type="rename", maxLen=20/type="rename", maxLen=32/' "$BAK/build.bak" > bedrock/build.py
run "isim siniri ipucunda yok" "harf sinirini yazmiyor"
cp "$BAK/build.bak" bedrock/build.py

# 16) iki esya ayni ikonu tasirsa -> envanterde ayirt edilemez
cp "$RP/textures/items/stnt_ejderha_nefesi.png" "$BAK/icon.bak"
cp "$RP/textures/items/stnt_mega_gubre.png" "$RP/textures/items/stnt_ejderha_nefesi.png"
run "iki esyanin ikonu ayni" "ikon tekrari"
cp "$BAK/icon.bak" "$RP/textures/items/stnt_ejderha_nefesi.png"

# 17) BP bagimliligi RP'yi gostermezse -> kaynak paketi HIC yuklenmez
cp "$BP/manifest.json" "$BAK/bpman.bak"
python3 - <<'PYX'
import json
p = "bedrock/super_tnt_BP/manifest.json"
d = json.load(open(p))
for dep in d["dependencies"]:
    if "uuid" in dep:
        dep["uuid"] = "00000000-0000-4000-8000-000000000000"
json.dump(d, open(p, "w"))
PYX
run "BP bagimliligi RP'yi gostermiyor" "RP header uuid'ini gostermiyor"
cp "$BAK/bpman.bak" "$BP/manifest.json"

# 18) iki sey ayni adi tasirsa -> envanterde hangisi oldugu belli olmaz
sed 's/tr="Pembe Lego Parçası"/tr="Pembe Lego Tuğla"/' "$BAK/build.bak" > bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1
run "iki seyin adi ayni" "adi hem"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1      # paketi geri getir, sonraki mutasyon temiz bassin

# 19) kilik degistirme listesi bosalirsa -> Gizli TNT yakalanmali (liste
#     gercekten is goruyor mu, yoksa denetim bos mu geciyor?)
cp bedrock/tools/check_pack.py "$BAK/chk.bak"
sed 's/^DISGUISES = \[$/DISGUISES = [] and [/' "$BAK/chk.bak" > bedrock/tools/check_pack.py
run "kilik listesi bos (denetim bos gecmiyor mu)" "yan yuzu ayni"
cp "$BAK/chk.bak" bedrock/tools/check_pack.py

# 20) esya yaratici menu grubunu kaybederse -> menude HIC gorunmez
cp "$BP/items/ejderha_nefesi.json" "$BAK/it.bak"
python3 - <<'PYX'
import json
p = "bedrock/super_tnt_BP/items/ejderha_nefesi.json"
d = json.load(open(p))
del d["minecraft:item"]["description"]["menu_category"]
json.dump(d, open(p, "w"))
PYX
run "esya yaratici menude yok" "menude gorunmez"
cp "$BAK/it.bak" "$BP/items/ejderha_nefesi.json"

# 22) Mutant Warden'in omuz acisi geri alinsin -> on kollar bel altinda,
#     govde orta cizgisinde birlesir; cocuklarin ekraninda edepsiz durur.
cp bedrock/custom/mutant_warden.geo.json "$BAK/geo.bak"
python3 - <<'PYX'
import io
p = "bedrock/custom/mutant_warden.geo.json"
s = io.open(p, encoding="utf-8").read()
for v in ("15", "-15"):
    s = s.replace('"rotation": [\n            0,\n            0,\n            %s\n          ],' % v,
                  '"rotation": [\n            0,\n            0,\n            0\n          ],', 1)
io.open(p, "w", encoding="utf-8").write(s)
PYX
run "Warden kollari bacak arasinda" "bacak arasi kutusuna giriyor"
cp "$BAK/geo.bak" bedrock/custom/mutant_warden.geo.json

# 21) Kalp Baltasi'na dayaniklilik geri konsun -> hayatta kalmada kirilir
sed 's/^            if not it.get("unbreakable"):$/            if True:/' "$BAK/build.bak" > bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1
run "Kalp Baltasi yine kiriliyor" "dayaniklilik bileseni duruyor"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1

# 23) POV kamerasi yine ayak konumundan surulsun -> comelince goz inmez.
#     Gercekten yasanmis hata: v1.40.0'da cocuk "egilemiyorum" diyordu.
cp "$BP/scripts/main.js" "$BAK/main.bak"
mutate_js() {   # mutate_js <eski> <yeni>   (main.js icinde tek yer degistirir)
  python3 - "$1" "$2" <<'PYX'
import io, sys
p = "bedrock/super_tnt_BP/scripts/main.js"
s = io.open(p, encoding="utf-8").read()
assert s.count(sys.argv[1]) == 1, f"beklenen tek eslesme: {sys.argv[1]!r}"
io.open(p, "w", encoding="utf-8").write(s.replace(sys.argv[1], sys.argv[2], 1))
PYX
}
mutate_js "head = p.getHeadLocation();" "head = { y: loc.y + 1.62 };"
run "POV kamerasi comelmeyi izlemiyor" "comelince goz inmez"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 28) easing kaldirilsin -> kamera saniyede 20 kez ziplayarak arkadan yetisir
mutate_js "easeOptions: { easeTime: 0.05" "noEase: { easeTime: 0.05"
run "POV kamerasi easing'siz" "kamera saniyede 20 kez zipliyor"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 29) one oteleme kaldirilsin -> kamera oyuncunun kafasinin icinde, ekran siyah
mutate_js "const fwd = camForward(scale);" "const fwd = 0;"
run "POV kamerasi kafanin icinde" "ekran siyah olur"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 30) hareketi kilitleyen /inputpermission eklensin
mutate_js 'const eye = loc.y' 'p.runCommand("inputpermission set @s movement disabled");\n    const eye = loc.y'
run "oyuncunun hareketi kilitleniyor" "hareketini kilitler"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 31) kademe 1 carpisma yuksekligi tam 1.00'e donsun -> tek blokluk bosluga
#     girilemez (cocuk "kuculdum ama giremiyorum" diyordu)
sed 's/^    1: (0.53, 0.50, 0.95).*$/    1: (0.53, 0.50, 1.00),/' "$BAK/build.bak" > bedrock/build.py
run "tek blokluk bosluga girilemiyor" "tek blokluk"
cp "$BAK/build.bak" bedrock/build.py

# 32) creeper patlamasi yine blok kirmasin -> cocuk yalnizca ses duyar
sed 's/^         pow=dict(r=3, breaks=True, cd=100)),$/         pow=dict(r=3, breaks=False, cd=100)),/' "$BAK/build.bak" > bedrock/build.py
run "creeper patlamasi iz birakmiyor" "yalnizca"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1

# 24) Buz TNT'nin yaricapi kaldirilsin -> dunyanin obur ucundaki kardes de
#     30 saniye yerinden kimildayamaz (slowness amp 6 = tam felc).
python3 - <<'PYX'
import io
# Ayni cagri metni dosyada bes yerde geciyor; korlemesine ilk esleseni
# degistirmek baska bir TNT'yi bozar ve mutasyon hedefini isabet ETTIRMEZ.
# Bu yuzden once "case \"freeze\"" bulunur, sonra ONDAN SONRAKI cagri.
p = "bedrock/super_tnt_BP/scripts/main.js"
s = io.open(p, encoding="utf-8").read()
i = s.index('case "freeze": {')
j = s.index("dim.getPlayers(", i)
k = s.index(")", j) + 1
io.open(p, "w", encoding="utf-8").write(s[:j] + "dim.getPlayers()" + s[k:])
PYX
run "Buz TNT butun dunyayi donduruyor" "yaricapla sinirli degil"
cp "$BAK/main.bak" "$BP/scripts/main.js"

# 25) Ikon arkaplani yine boyansin -> envanterde esya degil renkli kare gorunur
cp "$BAK/build.bak" bedrock/build.py
python3 - <<'PYX'
import io
p = "bedrock/build.py"
s = io.open(p, encoding="utf-8").read()
s = s.replace('    bg = SYMBOL_BG.get(item_id)',
              '    bg = SYMBOL_BG.get(item_id) or (60, 30, 12)', 1)
io.open(p, "w", encoding="utf-8").write(s)
PYX
python3 bedrock/build.py > /dev/null 2>&1
run "ikon arkaplani yine boyali" "arkaplani boyali"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1

# 27) Blok kiliginin ust/alt yuz gecisi kaldirilsin -> cim blogunun ustu de
#     yan dokusuyla cizilir (kutu her yuzde ayni dokuyu ornekler)
python3 - <<'PYX'
import io
p = "bedrock/build.py"
s = io.open(p, encoding="utf-8").read()
old = "        layers=[dict(geo=BLOCK_GEO_TOP"
i = s.index(old)
j = s.index("\n", s.index("mat=BLOCK_MAT)])", i))
io.open(p, "w", encoding="utf-8").write(s[:i] + "        )" + s[j:])
PYX
python3 bedrock/build.py > /dev/null 2>&1
run "blok kiliginin ustu yan dokusu" "ust/alt yuz gecisi yok"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1

# 26) Bir ikonun cizimi silinsin -> SYMBOL_IDS'te var ama bos kare uretiliyor
python3 - <<'PYX'
import io
p = "bedrock/build.py"
s = io.open(p, encoding="utf-8").read()
s = s.replace('    elif iid == "lightning_spell":', '    elif iid == "__yok__":', 1)
io.open(p, "w", encoding="utf-8").write(s)
PYX
python3 bedrock/build.py > /dev/null 2>&1
run "ikonun cizimi unutulmus" "neredeyse bos"
cp "$BAK/build.bak" bedrock/build.py
python3 bedrock/build.py > /dev/null 2>&1

echo
python3 bedrock/build.py > /dev/null && echo "paket yeniden uretildi"
echo "yakalanan $pass / kacirilan $fail"
[ "$fail" -eq 0 ]
