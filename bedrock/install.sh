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
# KABLOSUZ
# Kablo gerekmez. Tablet ayni wifi'da ve kablosuz hata ayiklama aciksa bu
# script kendisi baglanir (bkz. tablet-wifi.sh). Yeni bir tablette bir
# kereligine kablo gerekir: "bedrock/tablet-wifi.sh kur".
#
# AYNI TABLETE IKI KEZ GONDERMEME
# Kablo TAKILIYKEN kablosuz da bagliysa ayni fiziksel tablet "adb devices"
# icinde IKI transport olarak gorunur (R5GY... ve Android.local:46341).
# Naif bir dongu ayni tablete iki kez push eder. Bu yuzden transport'lar
# ro.serialno ile grupleniyor ve her tablet icin TEK transport seciliyor —
# varsa kablosuz olan, cunku bu akisin asil yolu o.
#
# KULLANIM
#   bedrock/install.sh out/SuperTNT.mcaddon            # tum tabletler
#   bedrock/install.sh out/SuperTNT.mcaddon R5GYC4BGJJZ  # tek cihaz

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"

FILE="${1:?kullanim: install.sh <dosya.mcaddon> [cihaz-seri]}"
[ -f "$FILE" ] || { echo "dosya yok: $FILE" >&2; exit 1; }
NAME="$(basename "$FILE")"
DEST="/sdcard/Download/$NAME"

# "adb shell" stdin'i yutar; cagiran dongunun geri kalanini yememesi icin
# her cagriya </dev/null verilir.
transport_seri() { adb -s "$1" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '\r'; }

if [ $# -ge 2 ]; then
  DEVICES="$2"
else
  # USB mu kablosuz mu — transport ADINA BAKARAK anlasilmaz. Kablosuz
  # transport "Android.local:46341" (iki nokta VAR) ya da mDNS adiyla
  # "adb-R5GY...-ffM6cn._adb-tls-connect._tcp" (iki nokta YOK) gelebilir.
  # Ada bakan filtre ikincisini USB sanir. "adb devices -l" USB transport'lara
  # " usb:<yol>" alani koyuyor; tek guvenilir isaret bu.
  KABLOSUZ="$(adb devices -l | awk 'NR>1 && $2=="device" && $0 !~ / usb:/ && $1 !~ /^emulator/ {print $1}')"
  USB="$(adb devices -l | awk 'NR>1 && $2=="device" && $0 ~ / usb:/ {print $1}')"
  # Hic kablosuz yoksa once baglanmayi dene (kablosuz asil yol).
  if [ -z "${KABLOSUZ// /}" ]; then
    "$HERE/tablet-wifi.sh" bagla || echo "(kablosuz kurulamadi, USB ile devam)" >&2
    KABLOSUZ="$(adb devices -l | awk 'NR>1 && $2=="device" && $0 !~ / usb:/ && $1 !~ /^emulator/ {print $1}')"
  fi
  # Kablosuzlar once listelenir ki ayni seride o kazansin.
  DEVICES=""; GORULEN=""
  for T in $KABLOSUZ $USB; do
    S="$(transport_seri "$T")"
    [ -n "$S" ] || continue
    case " $GORULEN " in *" $S "*) continue ;; esac
    GORULEN="$GORULEN $S"
    DEVICES="$DEVICES $T"
  done
fi
[ -n "${DEVICES// /}" ] || { echo "bagli tablet yok" >&2; exit 1; }

for D in $DEVICES; do
  WHO="$(adb -s "$D" shell "pm list users" </dev/null 2>/dev/null \
         | grep -oE '\{0:[^:]*' | cut -d: -f2 | tr -d '\r' || echo "$D")"
  if adb devices -l | awk -v t="$D" 'NR>1 && $1==t && $0 ~ / usb:/ {b=1} END{exit !b}'; then
    VIA="USB"; else VIA="kablosuz"; fi
  echo "=== $WHO ($D, $VIA)"

  # gonder + butunluk dogrula
  adb -s "$D" push "$FILE" "$DEST" >/dev/null
  LOCAL="$(wc -c <"$FILE" | tr -d ' ')"
  REMOTE="$(adb -s "$D" shell "stat -c %s '$DEST'" | tr -d '\r ')"
  if [ "$LOCAL" != "$REMOTE" ]; then
    echo "  HATA: boyut uyusmuyor (mac=$LOCAL tablet=$REMOTE)"; continue
  fi
  echo "  gonderildi ve dogrulandi: $LOCAL bayt"

  # MediaStore temizligi — KRITIK.
  # adb push edilen dosya bazen MediaStore'a "application/octet-stream"
  # (bilinmeyen tip) olarak giriyor. Samsung "Dosyalarim" boyle kayitli
  # .mcaddon'lari Minecraft'a degil Google Play'e yonlendiriyor. MediaStore'da
  # HIC kaydi olmayan .mcaddon'lari ise uzantidan tanip Minecraft'a veriyor.
  # Bu yuzden push sonrasi kaydi siliyoruz; Samsung dosyayi uzantidan bulur.
  adb -s "$D" shell "content delete --uri content://media/external/downloads \
      --where \"_display_name='$NAME'\"" >/dev/null 2>&1 || true

  cat <<EOF
  Simdi tablette:
    Dosyalarim > Indirilenler > $NAME  (uzun bas > "Sununla ac")
    > Minecraft (duz cim-blogu ikonu, "Education" degil) > Yalnizca bir defa
  Minecraft acilir ve "Iceri aktariliyor" der. Sonra Dunya Ayarlari'nda
  Davranis + Kaynak paketlerinden IKISINI de etkinlestir.
EOF
done
