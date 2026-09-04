#!/usr/bin/env bash
# Tabletleri kablosuz ADB uzerinden bulur ve baglar.
#
# NEDEN BOYLE — uc tuzak var, ucu de olculdu (2026-09-04, SM-X520 / Android 16):
#
#   1) "adb mdns services" YANLIS IP veriyor. Iki tablet de ayni adresle
#      listeleniyordu (ikisi de 192.168.68.151). O adrese baglanmak
#      "Connection refused" veriyor. Bu yuzden kesif macOS'un kendi Bonjour
#      aracina (dns-sd) yaptiriliyor; o dogru veriyor.
#
#   2) PORT her acilista degisiyor. Kablosuz hata ayiklama rastgele port
#      seciyor (olculen: 46341 ve 34933; yeniden baslatinca baska olur).
#      Bu yuzden port HICBIR YERDE saklanmiyor, her calistirmada yeniden
#      kesfediliyor.
#
#   3) IP de DHCP ile degisebilir. Tabletler kendi ".local" adlarini
#      yayinliyor (Android.local, Android-2.local) ve bu ad sabit. Bagalanti
#      IP ile degil bu adla kuruluyor.
#
# Sonuc: kablo gerekmez, sabit IP gerekmez, elle port yazmak gerekmez.
#
# KULLANIM
#   bedrock/tablet-wifi.sh            # bul ve bagla (varsayilan)
#   bedrock/tablet-wifi.sh durum      # ne bagli, hangi tablet
#   bedrock/tablet-wifi.sh kur        # YENI tablet: kabloyla bir kez calistir
#   bedrock/tablet-wifi.sh kes        # tum kablosuz baglantilari birak

set -uo pipefail

MDNS_SERVICE="_adb-tls-connect._tcp"
TARAMA_SN="${TARAMA_SN:-4}"     # Bonjour taramasi kac saniye dinlesin
: "${TMPDIR:=/tmp}"

adb_var() { command -v adb >/dev/null 2>&1; }
adb_var || { echo "adb yok: brew install --cask android-platform-tools" >&2; exit 1; }

# dns-sd akis halinde calisir ve kendi kendine bitmez; arka planda baslatip
# sure dolunca kesiyoruz. Ciktisi gecici dosyadan okunur.
dns_sd_calistir() {
  local sure=$1; shift
  local cikti; cikti="$(mktemp "$TMPDIR/tablet-wifi.XXXXXX")"
  "$@" >"$cikti" 2>&1 &
  local pid=$!
  sleep "$sure"
  kill "$pid" 2>/dev/null
  wait "$pid" 2>/dev/null
  cat "$cikti"
  rm -f "$cikti"
}

# Yayindaki tablet kayitlarinin ADLARINI verir (adb-<SERI>-xxxx).
kayitlari_bul() {
  dns_sd_calistir "$TARAMA_SN" dns-sd -B "$MDNS_SERVICE" \
    | awk '$2=="Add" {print $NF}' | sort -u
}

# Kayit adindan "host:port" cozer. dns-sd satiri soyle:
#   <ad>._adb-tls-connect._tcp.local. can be reached at Android-2.local.:46341 (interface 14)
adres_coz() {
  dns_sd_calistir 3 dns-sd -L "$1" "$MDNS_SERVICE" \
    | sed -n 's/.*can be reached at \([^ ]*\) .*/\1/p' | sed 's/\.:/:/' | head -1
}

# Kayit adindaki seri numarasi: adb-R5GYC4BGAQW-ffM6cn -> R5GYC4BGAQW
seri_al() { echo "$1" | sed -E 's/^adb-(.+)-[^-]+$/\1/'; }

# Bir transport'un ARKASINDAKI fiziksel tabletin seri numarasi. "</dev/null"
# sart: "adb shell" stdin'i yutar ve cagiran dongunun geri kalanini yer.
transport_seri() { adb -s "$1" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '\r'; }

# USB mu kablosuz mu — transport ADINA BAKARAK anlasilmaz. Kablosuz transport
# iki bicimde gelebilir: "Android.local:46341" (iki nokta VAR) ve mDNS adiyla
# "adb-R5GY...-ffM6cn._adb-tls-connect._tcp" (iki nokta YOK). Ada bakan bir
# filtre ikincisini USB sanir ve ustune USB'ye ozel komut cekilirse baglanti
# duser. "adb devices -l" ise USB transport'lara " usb:<yol>" alani koyuyor;
# tek guvenilir isaret bu. (emulator'un da usb: alani yok, ayrica eleniyor.)
# --- Cocuklarin kod adlari ------------------------------------------------
# "zeynep" / "omer" hep ayni fiziksel tablet demek: ad, DONANIM seri numarasina
# (ro.serialno) bagli. O numara kabloda da kablosuzda da ayni ve oturumlar
# arasi degismez, yani IP ya da port degisse de ad bozulmaz.
#
# Dosya REPO DISINDA (~/.config/tablet-adlari): seri numarasi kisisel donanim
# bilgisi, git'e girmemeli. Ayni dosyayi bilgebaykus projesi de okuyor —
# adlar tek yerde tanimli olsun, iki projede ayri ayri tutulup birbirinden
# sapmasin diye. Bicim, bilgebaykus'un "devices.local" bicimiyle ayni:
#   <kod-adi> <seri-no> <model>
ADLAR="${TABLET_ADLARI:-}"
if [ -z "$ADLAR" ]; then
  for aday in "$(dirname "${BASH_SOURCE[0]:-$0}")/devices.local" "$HOME/.config/tablet-adlari"; do
    [ -f "$aday" ] && { ADLAR="$aday"; break; }
  done
fi

# kod adi -> seri no. Eslesme yoksa girdiyi AYNEN dondurur, boylece cagiran
# tek yoldan gecer ve seri numarasi da dogrudan yazilabilir.
ad_to_seri() {
  if [ -n "$ADLAR" ] && [ -f "$ADLAR" ]; then
    awk -v k="$1" '$1==k {print $2; f=1; exit} END{if (!f) print k}' "$ADLAR"
  else
    echo "$1"
  fi
}
# seri no -> kod adi (kayitli degilse bos).
seri_to_ad() {
  [ -n "$ADLAR" ] && [ -f "$ADLAR" ] || return 0
  awk -v s="$1" '$2==s {print $1; exit}' "$ADLAR"
}

usb_transportlar() {
  adb devices -l | awk 'NR>1 && $2=="device" && $0 ~ / usb:/ {print $1}'
}
transport_usb_mu() {
  adb devices -l | awk -v t="$1" 'NR>1 && $1==t && $0 ~ / usb:/ {bulundu=1} END{exit !bulundu}'
}

bagla() {
  echo "Bonjour ile taraniyor (${TARAMA_SN} sn)..."
  local kayitlar; kayitlar="$(kayitlari_bul)"
  if [ -z "$kayitlar" ]; then
    cat >&2 <<'EOF'
Yayinda tablet yok. Sirayla bak:
  - Tablet ayni wifi'da mi (ev agi), ve ekrani acik mi?
  - Ayarlar > Gelistirici Secenekleri > Kablosuz hata ayiklama ACIK mi?
    (Kapaliysa: tableti kabloyla tak ve "tablet-wifi.sh kur" calistir.)
  - Mac ve tablet ayni alt agda mi? Misafir wifi'si cihazlari birbirinden
    yalitir, orada calismaz.
EOF
    return 1
  fi
  local ad seri ad_gorunen adres n=0
  for ad in $kayitlar; do
    seri="$(seri_al "$ad")"
    ad_gorunen="$(seri_to_ad "$seri")"
    adres="$(adres_coz "$ad")"
    if [ -z "$adres" ]; then
      echo "  $seri: adres cozulemedi (yayin var, cevap yok)" >&2
      continue
    fi
    # Zaten bagliysa adb "already connected" der; ikisi de basarilidir.
    if adb connect "$adres" 2>&1 | grep -qE "^(connected|already connected)"; then
      echo "  ${ad_gorunen:-$seri}  ->  $adres"
      n=$((n + 1))
    else
      echo "  $seri: $adres baglanamadi" >&2
    fi
  done
  [ "$n" -gt 0 ] || return 1
  echo "$n tablet kablosuz bagli."
}

durum() {
  local t seri ad tur var=0
  printf "%-10s %-24s %-14s %s\n" "KOD ADI" "TRANSPORT" "SERI" "TUR"
  for t in $(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator/ {print $1}'); do
    seri="$(transport_seri "$t")"
    ad="$(seri_to_ad "$seri")"
    if transport_usb_mu "$t"; then tur="USB"; else tur="kablosuz"; fi
    printf "%-10s %-24s %-14s %s\n" "${ad:-—}" "$t" "${seri:-?}" "$tur"
    var=1
  done
  [ "$var" = 1 ] || echo "(bagli cihaz yok)"
  [ -n "$ADLAR" ] || echo "(kod adi dosyasi yok: ~/.config/tablet-adlari)"
}

kur() {
  local usb; usb="$(usb_transportlar)"
  if [ -z "$usb" ]; then
    echo "Kabloyla bagli tablet yok. 'kur' bir kereligine KABLO ister:" >&2
    echo "  tableti USB-C data kablosuyla tak, ekrani acik olsun, tekrar dene." >&2
    return 1
  fi
  local d
  for d in $usb; do
    echo "=== $d"
    adb -s "$d" shell settings put global adb_wifi_enabled 1 </dev/null
    echo -n "  kablosuz hata ayiklama: "
    adb -s "$d" shell settings get global adb_wifi_enabled </dev/null | tr -d '\r'
  done
  echo "Servisin yayina cikmasi bekleniyor..."
  sleep 4
  bagla
}

kes() {
  adb disconnect >/dev/null 2>&1
  echo "Kablosuz baglantilar birakildi (USB varsa duruyor)."
}

# Bu dosya "source" edilirse (install.sh yardimcilari icin) komut
# CALISTIRILMAZ; sadece fonksiyonlar tanimlanmis olur. Iki kabuk iki ayri
# isaret veriyor, ikisi de bakiliyor: bash'te sourcing sirasinda BASH_SOURCE[0]
# ile $0 ayrisir, zsh'de ZSH_EVAL_CONTEXT icinde ":file" gecer. Yalniz bash'e
# bakmak zsh'den source edildiginde sessizce "bagla" calistiriyordu.
case "${ZSH_EVAL_CONTEXT:-}" in *:file*) return 0 ;; esac
[ "${BASH_SOURCE[0]:-$0}" = "$0" ] || return 0

case "${1:-bagla}" in
  bagla|connect) bagla ;;
  durum|status)  durum ;;
  kur|setup)     kur ;;
  kes|disconnect) kes ;;
  *) echo "kullanim: $(basename "$0") [bagla|durum|kur|kes]" >&2; exit 2 ;;
esac
