#!/bin/bash
# Emulatordeki Minecraft'ta sahne kur, TNT'yi atesle, kare kare cek.
#
#     bash bedrock/tools/shots.sh stage                # duz zemin, hava acik, kimse yok
#     bash bedrock/tools/shots.sh row  kiyamet_tnt gokkusagi_tnt lego_tnt ...   # z=8 hattina dizer
#     bash bedrock/tools/shots.sh cam  0 -57 -1  0 -60 8                          # kamera: konum, bakis
#     bash bedrock/tools/shots.sh fire 0 10             # oradaki vanilya TNT hattaki bloklari ateslar
#     bash bedrock/tools/shots.sh line -7 7 8           # sıranın ustune vanilya TNT hatti; hepsi ~ayni anda patlar
#     bash bedrock/tools/shots.sh burst lego 28 0.3     # bedrock/out/shots/lego/f01..f28.png
#     bash bedrock/tools/shots.sh solo lego_tnt          # temizle, tek TNT, yakin kamera, atesle, cek
#     bash bedrock/tools/shots.sh keep lego_tnt 12 14 20  # docs/kareler/lego_tnt-f12.jpg... + kontak sayfasi
#     bash bedrock/tools/shots.sh cmd "/say merhaba"    # tek komut
#
# ON KOSULLAR: `./ctl deploy android` ile paket emulatore gitmis, Minecraft'ta
# hileleri acik, yaratici, DUZ bir dunya ACIK ve oyuncu dunyada olmali. Dunya
# kurulumu bir kez elle yapilir (Play > Create New World > Creative, Cheats,
# Flat, Behavior packs > Super TNT). Sonrasi buradan surulur.
#
# NEDEN VANILYA TNT: paketin TNT'si yalniz betigin ignite() yolundan patlar —
# cakmak, redstone (yalniz betigin kaydettigi bloklar) ya da baska bir patlamanin
# vurmasi. /summon ile cagrilan *_primed varligi betige kayitli olmadigi icin
# sonsuza dek yanip soner. /summon minecraft:tnt 4 saniye sonra patlar ve hattaki
# paket bloklarini betik uzerinden ateslar; bloklar patlamayla YOK OLMAZ (F3).
#
# EMULATOR: mc_tablet AVD'si (Play Store'lu Android 15 tablet imaji), GPU modu
# swiftshader_indirect — ana makine GPU koprusu paketin sikistirilmis dokularini
# (ETC2/ASTC) reddedip siyah kare veriyor. 2560x1600'de ~1 kare/sn cekilir.
set -u
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
# Hedef: mc_tablet AVD'si. Baska bir proje ayni anda kendi emulatorunu
# acmis olabilir; o zaman ciplak adb "more than one device" der ya da yanlis
# cihaza dokunur. adb ANDROID_SERIAL'i kendiliginden kullanir.
if [ -z "${ANDROID_SERIAL:-}" ]; then
  for s in $("$ADB" devices | awk '$1 ~ /^emulator-/ && $2=="device"{print $1}'); do
    [ "$("$ADB" -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r')" = "${AVD:-mc_tablet}" ] && { export ANDROID_SERIAL="$s"; break; }
  done
  [ -n "${ANDROID_SERIAL:-}" ] || { echo "${AVD:-mc_tablet} emulatoru calismiyor: ./ctl deploy android" >&2; exit 1; }
fi
# Klavye acikken Gboard input text'i yeniden siralar (kelimeler karisir, "/"
# kayar). Emulator yeniden acilinca geri gelebiliyor; her calismada kapat.
# </dev/null: adb shell dongunun stdin'ini yiyip ikinci klavyeyi atlatiyordu.
for _ime in $("$ADB" shell ime list -s </dev/null | tr -d '\r'); do
  "$ADB" shell ime disable "$_ime" </dev/null >/dev/null
done
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/bedrock/out/shots"      # ham kareler (gitignore)
KEEP="$ROOT/docs/kareler"          # depoya alinan kareler
W=2560; H=1600   # ekran; dokunma koordinatlari buna gore

tap() { "$ADB" shell input swipe "$1" "$2" "$1" "$2" 220; sleep "${3:-1}"; }

# Sohbeti kapat, HUD'daki sohbet dugmesinden ac, alana dokun, kelime kelime yaz, gonder.
# Tek seferde yazmak karakter dusuruyor; %s bosluk.
cmd() {
  "$ADB" shell input keyevent 111; sleep 0.8
  tap 1280 38 2.5
  tap 1400 1530 0.8
  local first=1 w
  for w in $1; do
    [ $first = 1 ] || "$ADB" shell input text "%s"
    "$ADB" shell input text "$w"; first=0; sleep 0.3
  done
  sleep 0.4; "$ADB" shell input keyevent 66; sleep "${2:-1.5}"
}

stage() {
  cmd "/gamerule sendcommandfeedback false"
  cmd "/gamerule domobspawning false"
  cmd "/gamerule dodaylightcycle false"
  cmd "/time set 1000"
  cmd "/tp @s 0 -60 2"
  cmd "/tickingarea add -30 -64 -20 30 -40 40 stage"
  cmd "/weather clear"
  cmd "/fill -30 -64 -20 30 -64 40 bedrock"
  cmd "/fill -30 -63 -20 30 -62 40 dirt"
  cmd "/fill -30 -61 -20 30 -61 40 grass_block"
  cmd "/fill -30 -60 -20 30 -55 40 air"    # /fill en fazla 32768 blok; 61x61x6 = 22326
  cmd "/fill -30 -54 -20 30 -49 40 air"
  cmd "/fill -30 -48 -20 30 -43 40 air"
  cmd "/fill -30 -42 -20 30 -37 40 air"
  cmd "/fill -30 -36 -20 30 -31 40 air"
  cmd "/kill @e[type=!player]"
  cmd "/tp @s 25 -60 2"          # oyuncu kadraj disina
  cmd "/hud @s hide all" 3
}

row() {  # z=8 hattina, x=-(n-1)..(n-1) adimla 2
  local n=$# x=$(( -($# - 1) )) id
  for id in "$@"; do cmd "/setblock $x -60 8 stnt:$id" 0.8; x=$((x + 2)); done
}

cam() { cmd "/camera @s set minecraft:free pos $1 $2 $3 facing $4 $5 $6" 2; cmd "/hud @s hide all" 2; }

# Sohbet Enter'dan sonra yavas kapaniyor; hemen ardindan gelen tiklama yaziyi
# bozuyor (kelimeler dusuyor, "/" sona kayiyor). Bir saniye bekle, sonra ESC.
fire() { cmd "/summon minecraft:tnt $1 -59 $2" 1; "$ADB" shell input keyevent 111; sleep 0.5; }

# Vanilya TNT hatti, siranin 3 blok ustunde: bir ucu tutusunca zincir 1 sn icinde
# hattin sonuna varir ve altindaki paket bloklari hep birlikte ateslenir (tek
# TNT'den zincir hop basina FUSE ~4 sn surer, ilk efekt sonrakileri orter).
# Havadaki patlama da zemini oyar; krater kacinilmaz, kadraj onu saklar.
line() {  # line <x1> <x2> <z>
  cmd "/fill $1 -57 $3 $2 -57 $3 tnt" 0.8
  cmd "/summon minecraft:tnt $(( ($1 + $2) / 2 )) -56 $3" 0.1; "$ADB" shell input keyevent 111
}

# Tek TNT, yakin ve alcak kamera: 0,-60,8'e koyar, 20 blok yaricapi temizler,
# ustune vanilya TNT dusurur, cekere. Dunyayi donusturen TNT'ler icin (lego,
# rainbow, seker...) en guclu kare buradan cikar.
solo() {  # solo <id> [ad=id] [kare=30] [aralik=0.2]   CAM="x y z fx fy fz" ile kamera
  cmd "/kill @e[type=!player]"
  cmd "/weather clear"
  cmd "/time set 1000"
  cmd "/fill -20 -60 -12 20 -42 28 air"     # 41x41x19 = 31939 < 32768
  cmd "/fill -20 -41 -12 20 -31 28 air"
  cmd "/fill -20 -63 -12 20 -62 28 dirt"
  cmd "/fill -20 -61 -12 20 -61 28 grass_block"
  cmd "/setblock 0 -60 8 stnt:$1" 0.8
  cam ${CAM:-0 -56 -1 0 -58 8}
  fire 0 8
  burst "${2:-$1}" "${3:-30}" "${4:-0.2}"
}

burst() {  # burst <ad> [kare=28] [aralik=0.3]
  local dir="$OUT/$1" n="${2:-28}" gap="${3:-0.3}" i
  rm -rf "$dir"; mkdir -p "$dir"
  for i in $(seq -w 1 "$n"); do "$ADB" exec-out screencap -p > "$dir/f$i.png"; sleep "$gap"; done
  echo "$dir: $n kare"
}

# Secilen kareleri depoya al: docs/kareler/<ad>-fNN.jpg — ustteki HUD dugmeleri ve
# kenarlar kirpilmis 2160x1350 (16:10), yayina hazir; docs/kareler/<ad>.jpg butun
# cekimin kontak sayfasi (6 sutun, kirpilmamis). Ham PNG'ler bedrock/out'ta kalir.
# web/ altinda 1440 px kopyasi durur; site onu kullanir (tam boy ~700 KB, web ~270 KB).
# Duz klasor, essiz adlar: acilis sayfasi ureteci varliklari dosya adiyla kopyaliyor.
# ImageMagick 7 (`brew install imagemagick`); montage yerine append, cunku montage
# yazi tipi bulamayinca hata koduyla cikiyor.
keep() {  # keep <ad> <NN>...
  local src="$OUT/$1" tmp i row=0
  [ -d "$src" ] || { echo "cekim yok: $src" >&2; return 1; }
  mkdir -p "$KEEP"; tmp="$(mktemp -d)"
  mkdir -p "$KEEP/web"
  for i in "${@:2}"; do
    magick "$src/f$i.png" -crop 2160x1350+260+250 +repage -quality 84 "$KEEP/$1-f$i.jpg"
    magick "$KEEP/$1-f$i.jpg" -resize 1440x -quality 78 "$KEEP/web/$1-f$i.jpg"   # siteye giden boy
  done
  ls "$src"/f*.png | xargs -n 6 | while read -r files; do
    row=$((row + 1)); magick $files -resize 300x -background '#222' +append "$tmp/r$(printf %02d $row).png"
  done
  magick "$tmp"/r*.png -append -quality 75 "$KEEP/$1.jpg"; rm -rf "$tmp"
  echo "$KEEP/$1-f*.jpg: $(( $# - 1 )) kare + $KEEP/$1.jpg"
}

case "${1:-}" in
  stage) stage ;;
  row)   shift; row "$@" ;;
  cam)   shift; cam "$@" ;;
  fire)  shift; fire "$@" ;;
  line)  shift; line "$@" ;;
  solo)  shift; solo "$@" ;;
  keep)  shift; keep "$@" ;;
  burst) shift; burst "$@" ;;
  cmd)   shift; cmd "$@" ;;
  *) sed -n '2,15p' "$0"; exit 2 ;;
esac
