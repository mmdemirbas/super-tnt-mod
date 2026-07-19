#!/usr/bin/env bash
# Bir .mcaddon dosyasini tablete gonderir, butunlugunu dogrular ve
# import icin net tek adim birakir.
#
# NEDEN "am start" ILE OTOMATIK IMPORT YOK
# Denendi, calismiyor:
#   -n MainActivity (explicit)  -> Minecraft normal aciliyor, dosyayi
#                                  ISLEMIYOR. Ana menuye dusuyor.
#   MIME'li implicit intent      -> "Sununla ac" secicisi cikiyor.
#                                  Seciciyi adb ile gecmek kirilgan:
#                                  ikon sirasi, dil ve cozunurluk degisir.
# Elle import ise her zaman calisiyor (MorphX bu tabletlere boyle yuklendi).
# Bu yuzden script hazirligi yapar, import'u kullaniciya birakir.
#
# KULLANIM
#   bedrock/install.sh out/SuperTNT.mcaddon            # tum bagli tabletler
#   bedrock/install.sh out/SuperTNT.mcaddon R5GYC4BGJJZ  # tek cihaz

set -euo pipefail

FILE="${1:?kullanim: install.sh <dosya.mcaddon> [cihaz-seri]}"
[ -f "$FILE" ] || { echo "dosya yok: $FILE" >&2; exit 1; }
NAME="$(basename "$FILE")"
DEST="/sdcard/Download/$NAME"

if [ $# -ge 2 ]; then
  DEVICES="$2"
else
  DEVICES="$(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator/ {print $1}')"
fi
[ -n "$DEVICES" ] || { echo "bagli tablet yok" >&2; exit 1; }

for D in $DEVICES; do
  WHO="$(adb -s "$D" shell "pm list users" 2>/dev/null \
         | grep -oE '\{0:[^:]*' | cut -d: -f2 | tr -d '\r' || echo "$D")"
  echo "=== $WHO ($D)"

  # gonder + butunluk dogrula
  adb -s "$D" push "$FILE" "$DEST" >/dev/null
  LOCAL="$(wc -c <"$FILE" | tr -d ' ')"
  REMOTE="$(adb -s "$D" shell "stat -c %s '$DEST'" | tr -d '\r ')"
  if [ "$LOCAL" != "$REMOTE" ]; then
    echo "  HATA: boyut uyusmuyor (mac=$LOCAL tablet=$REMOTE)"; continue
  fi
  echo "  gonderildi ve dogrulandi: $LOCAL bayt"
  cat <<EOF
  Simdi tablette:
    Dosyalarim > Download > $NAME
    uzun bas > "Sununla ac" > Minecraft (duz cim-blogu ikonu) > Yalnizca bir defa
  Minecraft acilir ve "Iceri aktariliyor" der. Sonra Dunya Ayarlari'nda
  Davranis + Kaynak paketlerinden etkinlestir.
EOF
done
