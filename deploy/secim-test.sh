#!/usr/bin/env bash
# ./ctl'nin CIHAZ SECME mantigini gercek tablet olmadan sinar.
#
# NEDEN VAR. Secim mantiginin en kritik durumu, elde tablet varken bile zor
# uretilir: ayni tabletin hem kabloyla hem KABLOSUZ bagli olmasi, ve kablosuz
# transport'un mDNS-ad biciminde (iki nokta YOK) gelmesi. O bicim adb'nin
# onbellegine bagli, istenince olusmuyor. Sahte bir "adb" ile o tablo bir
# saniyede kuruluyor.
#
# Sinanan davranislar:
#   - ayni tablet iki transport'ta gorunuyorsa TEK kez secilir (yoksa paket
#     ayni tablete iki kez gider)
#   - kablosuz olan tercih edilir (bu akisin asil yolu o)
#   - kod adi ("zeynep") ve ham seri no ayni sekilde calisir
#   - emulator tablet SAYILMAZ
#   - kablosuz yoksa USB'ye duser
#
# KULLANIM
#   deploy/secim-test.sh          # cikis 0 = hepsi gecti

set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
CTL="$ROOT/ctl"
[ -f "$CTL" ] || { echo "ctl bulunamadi: $CTL" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# --- sahte adb -------------------------------------------------------------
# Yalniz ctl'nin kullandigi iki cagriyi taklit eder:
#   adb devices -l                    -> cihaz tablosu (SAHNE ile secilir)
#   adb -s <transport> shell getprop  -> o transport'un ARKASINDAKI seri no
cat > "$TMP/adb" <<'STUB'
#!/usr/bin/env bash
if [ "${1:-} ${2:-}" = "devices -l" ]; then
  case "${SAHNE:-ikisi}" in
    ikisi) cat <<'E'
List of devices attached
R5GYC4BGAQW            device usb:1048576X product:gts10fewifitur model:SM_X520
R5GYC4BGJJZ            device usb:17825792X product:gts10fewifitur model:SM_X520
Android-2.local:46341  device product:gts10fewifitur model:SM_X520
adb-R5GYC4BGJJZ-LhvfAg._adb-tls-connect._tcp device product:gts10fewifitur model:SM_X520
emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64
E
    ;;
    sadece_usb) cat <<'E'
List of devices attached
R5GYC4BGAQW            device usb:1048576X product:gts10fewifitur model:SM_X520
emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64
E
    ;;
    bos) echo "List of devices attached" ;;
  esac
  exit 0
fi
if [ "${1:-}" = "-s" ] && [ "${3:-}" = "shell" ]; then
  case "$2" in
    R5GYC4BGAQW|Android-2.local:46341)   echo "R5GYC4BGAQW" ;;
    R5GYC4BGJJZ|*_adb-tls-connect._tcp)  echo "R5GYC4BGJJZ" ;;
    emulator-*)                          echo "emulator-5554" ;;
  esac
fi
STUB
chmod +x "$TMP/adb"

# --- sabit kod ad tablosu (makinedekine bagimli olmamak icin) --------------
cat > "$TMP/adlar" <<'EOF'
omer R5GYC4BGJJZ SM-X520
zeynep R5GYC4BGAQW SM-X520
EOF

# --- ctl'nin tablet bolumunu ayikla ---------------------------------------
# ctl sonunda main'i cagirdigi icin dogrudan source EDILEMEZ; yalniz cihaz
# secme bolumu aliniyor. Sinir imleri ctl'de degisirse burasi bos kalir ve
# test asagida bunu yakalar.
sed -n '/^# ---- tabletler: kod adlari/,/^# ---- ortak adimlar/p' "$CTL" > "$TMP/bolum.sh"
grep -q 'tablet_sec()' "$TMP/bolum.sh" || {
  echo "ctl'den tablet bolumu ayiklanamadi — bolum basligi degismis olabilir" >&2; exit 1; }

ADB="$TMP/adb"; export ADB
TABLET_ADLARI="$TMP/adlar"; export TABLET_ADLARI
# shellcheck source=/dev/null
. "$TMP/bolum.sh"

gecti=0; kaldi=0
bekle() {
  if [ "$2" = "$3" ]; then gecti=$((gecti + 1)); printf '  ok    %s\n' "$1"
  else kaldi=$((kaldi + 1)); printf '  KALDI %s\n        beklenen: [%s]\n        alinan  : [%s]\n' "$1" "$2" "$3"; fi
}

IKI="Android-2.local:46341 adb-R5GYC4BGJJZ-LhvfAg._adb-tls-connect._tcp"

SAHNE=ikisi; export SAHNE
bekle "all: tablet basina TEK transport, kablosuz tercihli" "$IKI" "$(tablet_sec all)"
bekle "hedefsiz cagri all ile ayni"                         "$IKI" "$(tablet_sec)"
bekle "kod adi zeynep"                    "Android-2.local:46341" "$(tablet_sec zeynep)"
bekle "kod adi omer (mDNS-ad bicimi, iki nokta YOK)" \
      "adb-R5GYC4BGJJZ-LhvfAg._adb-tls-connect._tcp"            "$(tablet_sec omer)"
bekle "ham seri no da calisir"            "Android-2.local:46341" "$(tablet_sec R5GYC4BGAQW)"
bekle "bilinmeyen ad -> bos"                                   "" "$(tablet_sec yokboyle)"
bekle "emulator tablet sayilmaz"                               "" "$(tablet_sec emulator-5554)"

SAHNE=sadece_usb
bekle "kablosuz yokken USB'ye duser"                "R5GYC4BGAQW" "$(tablet_sec all)"
bekle "kablosuz yokken kod adi da calisir"          "R5GYC4BGAQW" "$(tablet_sec zeynep)"
bekle "bagli olmayan cocuk -> bos"                             "" "$(tablet_sec omer)"

SAHNE=bos
bekle "hic cihaz yok -> bos"                                   "" "$(tablet_sec all)"

printf '\n%d gecti, %d kaldi\n' "$gecti" "$kaldi"
[ "$kaldi" -eq 0 ]
