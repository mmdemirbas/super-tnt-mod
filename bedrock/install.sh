#!/usr/bin/env bash
# Bir .mcaddon dosyasini tablete gonderir ve Minecraft'ta import'u tetikler.
#
# NEDEN BU SCRIPT VAR
# Dosya yoneticisinden dosyaya dokununca Android artik "Play Store'da ara"
# diyor. Sebep tespit edildi: Minecraft'in intent filtresi MIME tipi
# bekliyor. Olcum (2026-07-19, SM-X520, MC 1.26.33.1):
#
#   MIME'siz file://              -> Docs + Arama  (Minecraft YOK)
#   MIME'li  file://              -> Minecraft VAR
#   content://media/...           -> Mesajlar      (Minecraft YOK)
#
# Samsung "Dosyalarim" MIME'siz ya da content:// gonderdigi icin eslesme
# olmuyor. Bu script dogru intent'i gonderir.
#
# KULLANIM
#   bedrock/install.sh out/SuperTNT.mcaddon            # tum bagli tabletler
#   bedrock/install.sh out/SuperTNT.mcaddon R5GYC4BGJJZ  # tek cihaz
#
# NOT: Tabletin ekrani ACIK ve KILIDI ACIK olmali. Kilitliyken hicbir
# uygulama one gelemez ve import sessizce basarisiz olur.

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

  # 1) gonder + butunluk dogrula
  adb -s "$D" push "$FILE" "$DEST" >/dev/null
  LOCAL="$(wc -c <"$FILE" | tr -d ' ')"
  REMOTE="$(adb -s "$D" shell "stat -c %s '$DEST'" | tr -d '\r ')"
  if [ "$LOCAL" != "$REMOTE" ]; then
    echo "  HATA: boyut uyusmuyor (mac=$LOCAL tablet=$REMOTE)"; continue
  fi
  echo "  gonderildi: $LOCAL bayt"

  # 2) ekran acik ve kilidi acik mi?
  SCREEN="$(adb -s "$D" shell "dumpsys display | grep -m1 mScreenState" | tr -d ' \r')"
  LOCKED="$(adb -s "$D" shell "dumpsys window | grep -oE 'mDreamingLockscreen=[a-z]+'" | head -1 | tr -d '\r')"
  if [ "$SCREEN" != "mScreenState=ON" ] || [ "$LOCKED" = "mDreamingLockscreen=true" ]; then
    echo "  ATLANDI: tabletin ekrani kapali ya da kilitli."
    echo "  Kilidi ac, sonra tekrar calistir. (ekran=$SCREEN $LOCKED)"
    continue
  fi

  # 3) import'u tetikle.
  #    -t  MIME tipi SART: yoksa Minecraft'in filtresi eslesmiyor.
  #    -n  acik bilesen: yoksa uygulama secici cikiyor ve yine dokunmak gerek.
  adb -s "$D" shell "am start -a android.intent.action.VIEW \
      -t application/octet-stream -d 'file://$DEST' \
      -n com.mojang.minecraftpe/com.mojang.minecraftpe.MainActivity" >/dev/null 2>&1
  sleep 8
  FOCUS="$(adb -s "$D" shell "dumpsys window | grep -m1 mCurrentFocus" | tr -d '\r')"
  case "$FOCUS" in
    *minecraftpe*) echo "  Minecraft acildi — import ekranini onayla" ;;
    *)             echo "  Minecraft one gelmedi. Odak: $FOCUS"
                   echo "  Elle: Dosyalarim > Download > $NAME > uzun bas > Sununla ac > Minecraft" ;;
  esac
done
