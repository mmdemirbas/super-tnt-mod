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
hold()   { printf '%s⏸%s %s\n'  "$C_WARN" "$C_0" "$*" >&2; exit 75; }
need()   { command -v "$1" >/dev/null 2>&1 || { err "$1 bulunamadi"; exit 1; }; }

VERBS="build	Sadece SuperTNT.mcaddon derle
deploy	Derle ve bir cihaza/hedefe gonder
wifi	Tabletleri kablosuza al / kablosuz baglan
status	Bagli cihazlar, AVD'ler ve simulatorler"

qualifiers() {
  case "$1" in
    deploy) printf '%s\n' \
      "tablet	Tabletlere kablosuz gonder — varsayilan; [zeynep|omer|all]" \
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
  ./ctl deploy tablet                 # iki tablete birden, kablosuz
  ./ctl deploy tablet zeynep          # yalniz Zeynep'in tableti
  ./ctl deploy tablet omer            # yalniz Omer'in tableti
  ./ctl wifi kur                      # ILK kurulum (bir kereligine kablo)
  ./ctl deploy android
  AVD=bb_tablet ./ctl deploy android
  IOS_SIM='iPhone 17 Pro' ./ctl deploy sim
  ./ctl status                        # hangi AVD ve simulator adlari var

NOTLAR
  * tablet: kablo GEREKMEZ. Ayni wifi yeterli; baglantiyi ./ctl kendi kurar.
    Yeni bir tablette bir kereligine kablo gerekir: ./ctl wifi kur
  * Tablet yeniden baslarsa adb, cihaz BIR KEZ ACILANA (unlock) kadar
    kapali kalir — Android guvenlik davranisi, ayarla kapatilamaz. Cocuk
    tableti acinca kablosuz kendiliginden geri gelir.
  * Kod adlari: $HOME/.config/tablet-adlari  (zeynep, omer)
  * tablet / android: Minecraft'in o cihazda KURULU olmasi gerekir.
  * iphone: App Store Minecraft'ina USB'den dogrudan dosya yazilamaz (Apple
    sinirlamasi). iCloud Drive senkronu tek guvenilir kanaldir.
  * sim: iOS Simulator App Store uygulamalarini (Minecraft dahil) CALISTIRAMAZ;
    komut yalnizca simulatoru acar.
EOF
}

# ---- tabletler: kod adlari + kablosuz --------------------------------------
# Tabletlere kablo takmadan gonderebilmek icin Android 11+ "kablosuz hata
# ayiklama"si kullanilir. Eski "adb tcpip 5555" yolu SECILMEDI: o bir sistem
# ozelligi, cihaz yeniden baslayinca kayboluyor ve her seferinde kablo
# isterdi.
#
# Olculen uc tuzak (2026-09-04, SM-X520 / Android 16), ucunun de karsiligi
# asagida:
#   1) "adb mdns services" YANLIS IP veriyor — iki tableti de ayni adresle
#      listeliyordu; o adrese baglanmak "Connection refused". Kesif bu yuzden
#      macOS'un kendi Bonjour aracina (dns-sd) yaptiriliyor.
#   2) PORT her acilista degisiyor. Hicbir yerde saklanmiyor, her calistirmada
#      yeniden kesfediliyor.
#   3) IP de DHCP ile degisebilir. Tabletler kendi ".local" adlarini yayinliyor
#      ve o ad sabit; baglanti IP ile degil o adla kuruluyor.
MDNS_SERVICE="_adb-tls-connect._tcp"
TARAMA_SN="${TARAMA_SN:-4}"

# Cocuklarin kod adlari. Ad DONANIM seri numarasina (ro.serialno) bagli: o
# numara kabloda da kablosuzda da ayni ve oturumlar arasi degismez, yani
# "zeynep" hep ayni fiziksel tablet — IP ya da port degisse de bozulmaz.
# Dosya REPO DISINDA: seri numarasi kisisel donanim bilgisi. Ayni dosyayi
# bilgebaykus projesi de okur, boylece adlar iki projede ayrisamaz.
# Bicim:  <kod-adi> <seri-no> <model>
ADLAR="${TABLET_ADLARI:-$HOME/.config/tablet-adlari}"

ad_to_seri() {   # eslesme yoksa girdiyi AYNEN dondurur (seri no da yazilabilsin)
  if [ -f "$ADLAR" ]; then
    awk -v k="$1" '$1==k {print $2; f=1; exit} END{if (!f) print k}' "$ADLAR"
  else printf '%s\n' "$1"; fi
}
seri_to_ad() { [ -f "$ADLAR" ] || return 0; awk -v s="$1" '$2==s {print $1; exit}' "$ADLAR"; }

# dns-sd akis halinde calisir, kendi kendine bitmez: arka planda baslat, sure
# dolunca kes.
dns_sd_calistir() {
  local sure=$1 out; shift
  out="$(mktemp "${TMPDIR:-/tmp}/ctl-mdns.XXXXXX")"
  "$@" >"$out" 2>&1 & local pid=$!
  sleep "$sure"; kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null
  cat "$out"; rm -f "$out"
}
# Yayindaki kayit adlari: adb-<SERI>-xxxx
mdns_kayitlar() {
  dns_sd_calistir "$TARAMA_SN" dns-sd -B "$MDNS_SERVICE" | awk '$2=="Add" {print $NF}' | sort -u
}
# Kayit adi -> "host:port". dns-sd satiri:
#   <ad>._adb-tls-connect._tcp.local. can be reached at Android-2.local.:46341 (interface 14)
mdns_adres() {
  dns_sd_calistir 3 dns-sd -L "$1" "$MDNS_SERVICE" \
    | sed -n 's/.*can be reached at \([^ ]*\) .*/\1/p' | sed 's/\.:/:/' | head -1
}

# Bir transport'un ARKASINDAKI fiziksel tablet. "</dev/null" SART: "adb shell"
# stdin'i okur ve cagiran dongunun beslemesini yer (cihazlar "kirpilir").
transport_seri() { "$ADB" -s "$1" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '\r'; }

# USB mu kablosuz mu — transport ADINA BAKARAK anlasilmaz: kablosuz transport
# "Android.local:46341" (iki nokta VAR) ya da mDNS adiyla
# "adb-R5GY...._adb-tls-connect._tcp" (iki nokta YOK) olabilir. Ada bakan
# filtre ikincisini USB sanar ve USB'ye ozel bir komut kablosuz cihaza
# cekilirse adbd yeniden baslar, baglanti duser. "adb devices -l" USB
# transport'lara " usb:<yol>" alani koyuyor; tek guvenilir isaret bu.
usb_transportlar()      { "$ADB" devices -l | awk 'NR>1 && $2=="device" && $0 ~ / usb:/ {print $1}'; }
kablosuz_transportlar() { "$ADB" devices -l | awk 'NR>1 && $2=="device" && $0 !~ / usb:/ && $1 !~ /^emulator/ {print $1}'; }
transport_usb_mu()      { "$ADB" devices -l | awk -v t="$1" 'NR>1 && $1==t && $0 ~ / usb:/ {b=1} END{exit !b}'; }

# Yayindaki tabletlere baglan. Sessiz mod: yalnizca deploy oncesi denemede.
wifi_bagla() {
  local sessiz="${1:-}" kayit seri ad adres n=0
  for kayit in $(mdns_kayitlar); do
    seri="$(printf '%s\n' "$kayit" | sed -E 's/^adb-(.+)-[^-]+$/\1/')"
    ad="$(seri_to_ad "$seri")"
    adres="$(mdns_adres "$kayit")"
    [ -n "$adres" ] || continue
    if "$ADB" connect "$adres" 2>&1 | grep -qE '^(connected|already connected)'; then
      [ -n "$sessiz" ] || ok "${ad:-$seri} kablosuz: $adres"
      n=$((n + 1))
    fi
  done
  [ "$n" -gt 0 ]
}

# Hedefe uyan transport'lari sec. Ayni tablet hem kabloyla hem kablosuz
# bagliysa "adb devices" onu IKI kez listeler; seri numarasina gore teke
# indiriyoruz, kablosuz olan tercih ediliyor (bu akisin asil yolu o).
# $1 bos ya da "all" -> hepsi; degilse kod adi veya seri no.
tablet_sec() {
  local istenen="${1:-}" t seri gorulen=" " secilen=""
  [ -n "$istenen" ] && [ "$istenen" != "all" ] && istenen="$(ad_to_seri "$istenen")" || istenen=""
  for t in $(kablosuz_transportlar) $(usb_transportlar); do
    seri="$(transport_seri "$t")"
    [ -n "$seri" ] || continue
    [ -n "$istenen" ] && [ "$seri" != "$istenen" ] && continue
    case "$gorulen" in *" $seri "*) continue ;; esac
    gorulen="$gorulen$seri "
    secilen="$secilen $t"
  done
  printf '%s\n' "${secilen# }"
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
# ./ctl deploy tablet            -> bagli tum tabletler
# ./ctl deploy tablet zeynep     -> yalniz o cocugun tableti
cmd_tablet() {
  local hedef="${1:-all}" devs
  need "$ADB"
  build
  # Kablosuz baglantiyi KENDI kurar; ayri bir komut calistirmak gerekmez.
  # Zaten bagliysa "already connected" doner, zararsiz.
  devs="$(tablet_sec "$hedef")"
  if [ -z "$devs" ]; then
    header "kablosuz tabletler araniyor"
    wifi_bagla || true
    devs="$(tablet_sec "$hedef")"
  fi
  if [ -z "$devs" ]; then
    if [ "$hedef" != "all" ]; then
      hold "'$hedef' bagli degil. Bagli olanlar icin: ./ctl status"
    fi
    hold "tablet yok. Tablet acik ve ayni wifi'da mi? Ilk kurulum: ./ctl wifi kur"
  fi
  local d ad
  for d in $devs; do
    ad="$(seri_to_ad "$(transport_seri "$d")")"
    if transport_usb_mu "$d"; then
      header "gonder: ${ad:-$d} (USB)"
    else
      header "gonder: ${ad:-$d} (kablosuz)"
    fi
    "$DEPLOY" --no-build "$d"
  done
}

# ./ctl wifi        -> yayindaki tabletlere baglan
# ./ctl wifi kur    -> ILK kurulum; bir kereligine KABLO ister
cmd_wifi() {
  need "$ADB"; need dns-sd
  case "${1:-bagla}" in
    kur|setup)
      local usb; usb="$(usb_transportlar)"
      [ -n "$usb" ] || hold "kabloyla bagli tablet yok. 'kur' bir kereligine KABLO ister: tableti USB-C data kablosuyla tak, ekrani ACIK olsun."
      local d
      for d in $usb; do
        header "kablosuz hata ayiklama aciliyor: $(seri_to_ad "$(transport_seri "$d")" || true) ($d)"
        "$ADB" -s "$d" shell settings put global adb_wifi_enabled 1 </dev/null
      done
      header "servisin yayina cikmasi bekleniyor"
      sleep 4
      wifi_bagla || hold "yayin gorunmedi. Tablette Ayarlar > Gelistirici Secenekleri > Kablosuz hata ayiklama ACIK mi?"
      ok "kablo cikarilabilir. Bundan sonra: ./ctl deploy tablet"
      ;;
    bagla|connect)
      header "Bonjour ile taraniyor (${TARAMA_SN} sn)"
      wifi_bagla || hold "yayinda tablet yok. Tablet acik ve ayni wifi'da mi? Misafir agi cihazlari yalitir. Ilk kurulum: ./ctl wifi kur"
      ;;
    *) err "bilinmeyen: ./ctl wifi $1  (bagla|kur)"; exit 2 ;;
  esac
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
  header "bagli Android cihazlar (kod adi — transport — baglanti)"
  local t seri ad var=0
  for t in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    case "$t" in
      emulator-*) printf '  %-10s %-24s %s\n' "—" "$t" "emulator" ;;
      *) seri="$(transport_seri "$t")"; ad="$(seri_to_ad "$seri")"
         if transport_usb_mu "$t"; then
           printf '  %-10s %-24s %s\n' "${ad:-—}" "$t" "USB"
         else
           printf '  %-10s %-24s %s\n' "${ad:-—}" "$t" "kablosuz"
         fi ;;
    esac
    var=1
  done
  [ "$var" = 1 ] || warn "cihaz yok. Kablosuz icin: ./ctl wifi"
  [ -f "$ADLAR" ] || warn "kod adi dosyasi yok: $ADLAR"
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
    wifi)   cmd_wifi "$@" ;;
    deploy)
      case "${1:-tablet}" in
        tablet)  shift 2>/dev/null || true; cmd_tablet  "${1:-all}" ;;
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
