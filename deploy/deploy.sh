#!/usr/bin/env bash
# Super TNT — mobil surumu her yere aktarir.
#
# Tek komut: build eder ve BAGLI olan her yere gonderir:
#   - USB'yle bagli Android tabletler
#   - KABLOSUZ (WiFi) bagli tabletler   (once: ./ctl wifi kur)
#   - Android emulatorleri              (--emu ile)
#   - iPhone / iPad                     (--icloud ile iCloud Drive'a birakir)
#
# KULLANIM
#   deploy/deploy.sh                 # build + tum USB/kablosuz tabletlere
#   deploy/deploy.sh --no-build      # mevcut .mcaddon'i gonder (yeniden build etme)
#   deploy/deploy.sh --emu           # emulatorleri de dahil et
#   deploy/deploy.sh --icloud        # iPhone/iPad icin iCloud Drive'a da kopyala
#   deploy/deploy.sh R5GYC4BGJJZ     # sadece tek cihaz (seri no ya da ip:port)
#
# Normalde bu script dogrudan CAGRILMAZ: ./ctl deploy tablet onu surer.
#
# NEDEN ELLE IMPORT: Minecraft'i adb ile otomatik acmak kirilgan (bkz. asagi).
# Script dosyayi hazirlar; son adimi (Dosyalarim'dan acma) kullanici yapar.

set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
FILE="$ROOT/bedrock/out/SuperTNT.mcaddon"

BUILD=1; EMU=0; ICLOUD=0; ONLY=""
for arg in "$@"; do
  case "$arg" in
    --no-build) BUILD=0 ;;
    --emu) EMU=1 ;;
    --icloud) ICLOUD=1 ;;
    --*) echo "bilinmeyen secenek: $arg" >&2; exit 2 ;;
    *) ONLY="$arg" ;;
  esac
done

# 1) build
if [ "$BUILD" = 1 ]; then
  echo "== build =="
  python3 "$ROOT/bedrock/build.py" | tail -3
fi
[ -f "$FILE" ] || { echo "dosya yok: $FILE (once build)" >&2; exit 1; }
NAME="$(basename "$FILE")"
DEST="/sdcard/Download/$NAME"
LOCAL="$(wc -c <"$FILE" | tr -d ' ')"

# 2) hedef cihazlar. adb devices USB + kablosuz + emulator hepsini listeler.
if [ -n "$ONLY" ]; then
  DEVICES="$ONLY"
else
  DEVICES="$(adb devices | awk 'NR>1 && $2=="device"{print $1}')"
  if [ "$EMU" = 0 ]; then
    DEVICES="$(echo "$DEVICES" | grep -v '^emulator' || true)"
  fi
fi

if [ -z "${DEVICES// }" ]; then
  echo "== bagli Android cihaz yok =="
  echo "   USB tak, ya da kablosuz icin: ./ctl wifi"
else
  for D in $DEVICES; do
    WHO="$(adb -s "$D" shell "pm list users" 2>/dev/null \
           | grep -oE '\{0:[^:]*' | cut -d: -f2 | tr -d '\r' || echo "$D")"
    KIND="USB"; echo "$D" | grep -q ':' && KIND="WiFi"; echo "$D" | grep -q '^emulator' && KIND="Emu"
    echo "== $WHO ($D) [$KIND] =="
    adb -s "$D" push "$FILE" "$DEST" >/dev/null
    REMOTE="$(adb -s "$D" shell "stat -c %s '$DEST'" | tr -d '\r ')"
    if [ "$LOCAL" != "$REMOTE" ]; then
      echo "   HATA: boyut uyusmuyor (mac=$LOCAL cihaz=$REMOTE)"; continue
    fi
    echo "   gonderildi ve dogrulandi: $LOCAL bayt"
    # MediaStore temizligi: Samsung push edilen .mcaddon'i bazen yanlis tiple
    # kaydeder ve Minecraft'a degil Play'e yonlendirir. Kaydi silince uzantidan
    # tanir.
    adb -s "$D" shell "content delete --uri content://media/external/downloads \
        --where \"_display_name='$NAME'\"" >/dev/null 2>&1 || true
    echo "   Tablette: Dosyalarim > Indirilenler > $NAME (uzun bas > Sununla ac > Minecraft)"
  done
fi

# 3) iPhone / iPad — iCloud Drive uzerinden (en guvenilir, AirDrop CLI yok).
if [ "$ICLOUD" = 1 ]; then
  ICLOUD_DIR="$HOME/Library/Mobile Documents/com~apple~CloudDocs/SuperTNT"
  mkdir -p "$ICLOUD_DIR"
  cp "$FILE" "$ICLOUD_DIR/$NAME"
  echo "== iPhone/iPad (iCloud) =="
  echo "   iCloud Drive'a kopyalandi: iCloud Drive > SuperTNT > $NAME"
  echo "   iPhone'da: Dosyalar > iCloud Drive > SuperTNT > $NAME (dokun > Minecraft)"
fi
