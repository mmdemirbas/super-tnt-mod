#!/usr/bin/env bash
# Super TNT — tek komut derleme + dagitim.
#
# Fiiller cihaz/hedef secer; derleme (bedrock/build.py) her seferinde bir
# kez calisir.
#
#   ./ctl                 fiil listesi
#   ./ctl --list          aynisi, satir basina bir `ad<TAB>aciklama`
#   ./ctl deploy --list   hedefler
#   ./ctl status          bagli cihazlar, AVD'ler ve simulatorler Android tarafi mevcut deploy/deploy.sh'i (push + dogrulama +
# Samsung MediaStore duzeltmesi) yeniden kullanir.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
DEPLOY="$ROOT/deploy/deploy.sh"
MCADDON="$ROOT/bedrock/out/SuperTNT.mcaddon"
ADB="${ADB:-adb}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
EMU_BIN="$SDK/emulator/emulator"
MC_PKG="com.mojang.minecraftpe"          # Minecraft paket adi (Android)

# ---- cikti yardimcilari (renkler tek yerde) -------------------------------
if [ -t 1 ]; then
  C_H=$'\033[36m'; C_OK=$'\033[32m'; C_WARN=$'\033[33m'; C_ERR=$'\033[31m'; C_0=$'\033[0m'
else
  C_H=''; C_OK=''; C_WARN=''; C_ERR=''; C_0=''
fi
# Her seviye icin tek imge ve tek renk; buradaki butun suruculer ayni besini
# kullanir: ▸ bir adim, ✓ oldu, ! bilinmesi gereken, ⏸ onkosul yok (cikis 75),
# ✗ olmadi (cikis 1). Sozlesme: ajans/docs/reports/driver-scripts-2026-09-02.md.
header() { printf '%s▸%s %s\n' "$C_H" "$C_0" "$*"; }
ok()     { printf '%s✓%s %s\n'   "$C_OK"  "$C_0" "$*"; }
warn()   { printf '%s!%s %s\n'   "$C_WARN" "$C_0" "$*"; }
err()    { printf '%s✗%s %s\n'   "$C_ERR" "$C_0" "$*" >&2; }
need()   { command -v "$1" >/dev/null 2>&1 || { err "$1 bulunamadi"; exit 1; }; }

VERBS="build	Sadece SuperTNT.mcaddon derle
deploy	Derle ve bir cihaza/hedefe gonder
status	Bagli cihazlar, AVD'ler ve simulatorler"

qualifiers() {
  case "$1" in
    deploy) printf '%s\n' \
      "tablet	Bagli Android tabletlere (USB veya WiFi) — varsayilan" \
      "iphone	Bagli iPhone'a, iCloud Drive kanaliyla" \
      "android	Android emulatorunde calistir (yoksa AVD baslatir) — [AVD=<ad>]" \
      "sim	iPhone simulatorunu ac (SINIRLI) — [IOS_SIM=<ad>]" ;;
    *)      return 1 ;;
  esac
}

list_for() {
  if [ -z "$1" ]; then printf '%s\n' "$VERBS"; return 0; fi
  qualifiers "$1" || { err "niteleyicisi yok: $1"; exit 1; }
}

usage() {
  printf 'Super TNT — derle ve dagit\n\nKULLANIM\n  ./ctl <fiil> [hedef]\n\nFIILLER\n'
  printf '%s\n' "$VERBS" | while IFS="$(printf '\t')" read -r name why; do
    printf '  %-8s %s\n' "$name" "$why"
  done
  printf '\nHEDEFLER  (./ctl deploy --list)\n'
  qualifiers deploy | while IFS="$(printf '\t')" read -r name why; do
    printf '  %-8s %s\n' "$name" "$why"
  done
  cat <<'EOF'

ORNEKLER
  ./ctl deploy tablet
  ./ctl deploy android
  AVD=bb_tablet ./ctl deploy android
  IOS_SIM='iPhone 17 Pro' ./ctl deploy sim
  ./ctl status                        # hangi AVD ve simulator adlari var

NOTLAR
  * tablet / android: Minecraft'in o cihazda KURULU olmasi gerekir.
  * iphone: App Store Minecraft'ina USB'den dogrudan dosya yazilamaz (Apple
    sinirlamasi). iCloud Drive senkronu tek guvenilir kanaldir.
  * sim: iOS Simulator App Store uygulamalarini (Minecraft dahil) CALISTIRAMAZ;
    komut yalnizca simulatoru acar.
EOF
}

# ---- ortak adimlar --------------------------------------------------------
build() {
  header "derle"
  python3 "$ROOT/bedrock/build.py" | tail -3
  [ -f "$MCADDON" ] || { err "derleme ciktisi yok: $MCADDON"; exit 1; }
  ok "hazir: $(basename "$MCADDON")"
}

# Calisan bir emulatorun seri numarasini stdout'a yazar; yoksa baslatir.
# Tum ilerleme mesajlari stderr'e gider ki $(ensure_emulator) sadece seriyi alsin.
ensure_emulator() {
  local serial
  serial="$("$ADB" devices | awk '$1 ~ /^emulator-/ && $2=="device"{print $1; exit}')"
  if [ -n "$serial" ]; then
    header "emulator (zaten calisiyor: $serial)" >&2
    printf '%s\n' "$serial"; return 0
  fi
  [ -x "$EMU_BIN" ] || { err "emulator bulunamadi: $EMU_BIN — ANDROID_HOME dogru mu?"; exit 1; }
  local avd="${AVD:-}"
  [ -n "$avd" ] || avd="$("$EMU_BIN" -list-avds 2>/dev/null | head -1)"
  [ -n "$avd" ] || { err "hic AVD yok. Android Studio > Device Manager ile bir tane olustur."; exit 1; }
  header "emulator baslatiliyor: $avd" >&2
  "$EMU_BIN" -avd "$avd" >/dev/null 2>&1 &
  "$ADB" wait-for-device
  local i=0
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    i=$((i + 1)); [ "$i" -gt 180 ] && { err "emulator acilis zaman asimi (180s)"; exit 1; }
    sleep 1
  done
  serial="$("$ADB" devices | awk '$1 ~ /^emulator-/ && $2=="device"{print $1; exit}')"
  ok "emulator hazir: $serial" >&2
  printf '%s\n' "$serial"
}

# Minecraft'i Android cihazda ac (kuruluysa). Kurulu degilse uyar, hata verme.
android_launch_mc() {
  local serial="$1"
  if "$ADB" -s "$serial" shell pm list packages 2>/dev/null | grep -q "$MC_PKG"; then
    "$ADB" -s "$serial" shell monkey -p "$MC_PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    ok "Minecraft acildi — Ayarlar > Global Kaynaklar/Davranislar'da paketi etkinlestir"
  else
    warn "Minecraft bu cihazda kurulu degil. Dosya gonderildi: Indirilenler > $(basename "$MCADDON")"
  fi
}

# ---- alt komutlar ---------------------------------------------------------
cmd_tablet() {
  need "$ADB"
  build
  header "Android tabletlere gonder (USB/WiFi)"
  "$DEPLOY" --no-build            # varsayilan: emulator haric tum Android cihazlar
}

cmd_android() {
  need "$ADB"
  build
  local serial; serial="$(ensure_emulator)"
  header "emulatore gonder ($serial)"
  "$DEPLOY" --no-build "$serial"  # yalnizca bu emulator hedeflenir
  android_launch_mc "$serial"
}

cmd_iphone() {
  need xcrun
  build
  header "iPhone'a gonder (iCloud Drive)"
  if xcrun devicectl list devices 2>/dev/null | grep -qiE 'iphone'; then
    ok "USB'de iPhone algilandi (dosya iCloud ile senkronlanir)"
  else
    warn "USB'de iPhone gorunmuyor — iCloud yine de acik oturuma senkronlar"
  fi
  local name dir
  name="$(basename "$MCADDON")"
  dir="$HOME/Library/Mobile Documents/com~apple~CloudDocs/SuperTNT"
  mkdir -p "$dir"
  cp "$MCADDON" "$dir/$name"
  ok "iCloud Drive'a kopyalandi: iCloud Drive > SuperTNT > $name"
  cat <<EOF
  iPhone'da: Dosyalar (Files) > iCloud Drive > SuperTNT > $name
             dosyaya dokun -> Minecraft ile ac.
  Neden iCloud: App Store Minecraft'ina USB'den dogrudan dosya yazilamaz
  (Apple uygulama kutusu erisimi kapali); iCloud tek guvenilir kanaldir.
EOF
}

cmd_ios() {
  need xcrun
  build
  header "iPhone simulator"
  local dev="${IOS_SIM:-iPhone 17}"
  xcrun simctl boot "$dev" 2>/dev/null || true      # zaten aciksa hata verir -> yut
  open -a Simulator >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$dev" 2>/dev/null || true
  ok "simulator acik: $dev"
  cat <<EOF
  SINIRLAMA: Minecraft Bedrock iOS Simulator'da CALISMAZ — App Store yalnizca
  gercek cihaz (arm64) surumunu dagitir, simulator surumu yoktur. Bu komut
  simulatoru acmaktan oteye gidemez. Gercek iOS testi icin:  ./ctl deploy iphone
  .mcaddon: $MCADDON
EOF
}

cmd_status() {
  header "bagli Android cihazlar"
  "$ADB" devices | sed '1d;/^$/d' || true
  header "AVD'ler  (AVD=<ad> ./ctl deploy android)"
  if [ -x "$EMU_BIN" ]; then "$EMU_BIN" -list-avds 2>/dev/null || true
  else warn "emulator bulunamadi: $EMU_BIN"; fi
  header "iOS simulatorleri  (IOS_SIM=<ad> ./ctl deploy sim)"
  if command -v xcrun >/dev/null 2>&1; then
    xcrun simctl list devices available 2>/dev/null | grep -E '^\s+iPhone|^\s+iPad' | sed 's/ *(.*//' | sed 's/^ */  /' || true
  else warn "xcrun yok"; fi
}

main() {
  local sub="${1:-}"; shift || true

  case "$sub" in
    ""|help|-h|--help) usage; exit 0 ;;
    --list)            list_for ""; exit 0 ;;
  esac

  [ "${1:-}" = "--list" ] && { list_for "$sub"; exit 0; }

  case "$sub" in
    build)  build ;;
    status) cmd_status ;;
    deploy)
      case "${1:-tablet}" in
        tablet)  shift 2>/dev/null || true; cmd_tablet  "$@" ;;
        iphone)  shift; cmd_iphone  "$@" ;;
        android) shift; cmd_android "$@" ;;
        sim)     shift; cmd_ios     "$@" ;;
        *)       err "bilinmeyen hedef: $1  (./ctl deploy --list)"; exit 2 ;;
      esac
      ;;
    *) err "bilinmeyen fiil: $sub"; echo; usage; exit 2 ;;
  esac
}

main "$@"
