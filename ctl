#!/usr/bin/env bash
# Super TNT — tek komut derleme + dagitim.
#
# Ne yaptigini ./ctl --list soyler; islemler ajans.toml'da tanimli, her biri
# asagida bir op_<fiil>[_<hedef>] fonksiyonu (sozlesme:
# ajans/docs/project-contract.md). Hedefler: ./ctl deploy --list
#
# Derleme (bedrock/build.py) her gonderimde bir kez calisir. Android tarafi
# mevcut deploy/deploy.sh'i (push + dogrulama + Samsung MediaStore duzeltmesi)
# yeniden kullanir.
#
# ORNEKLER
#   ./ctl deploy android                # iki tablete birden, kablosuz
#   ./ctl deploy android zeynep         # yalniz Zeynep'in tableti
#   ./ctl deploy android omer           # yalniz Omer'in tableti
#   ./ctl setup wifi kur                # ILK kurulum (bir kereligine kablo)
#   ./ctl deploy android-emu
#   AVD=bb_tablet ./ctl deploy android-emu
#   IOS_SIM='iPhone 17 Pro' ./ctl deploy ios-sim
#   ./ctl status                        # hangi AVD ve simulator adlari var
#
# NOTLAR
#   * android: kablo GEREKMEZ. Ayni wifi yeterli; baglantiyi ./ctl kendi kurar.
#     Yeni bir tablette bir kereligine kablo gerekir: ./ctl setup wifi kur
#   * Tablet yeniden baslarsa adb, cihaz BIR KEZ ACILANA (unlock) kadar
#     kapali kalir — Android guvenlik davranisi, ayarla kapatilamaz. Cocuk
#     tableti acinca kablosuz kendiliginden geri gelir.
#   * Kod adlari: $HOME/.config/tablet-adlari  (zeynep, omer)
#   * android / android-emu: Minecraft'in o cihazda KURULU olmasi gerekir.
#   * icloud: App Store Minecraft'ina USB'den dogrudan dosya yazilamaz (Apple
#     sinirlamasi). iCloud Drive senkronu tek guvenilir kanaldir; bu bir
#     uygulama kurulumu degil, dosya birakmadir.
#   * ios-sim: iOS Simulator App Store uygulamalarini (Minecraft dahil)
#     CALISTIRAMAZ; komut yalnizca simulatoru acar.

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
# ✗ olmadi (cikis 1). Sozlesme: ajans/docs/project-contract.md.
header() { printf '%s▸%s %s\n' "$C_H" "$C_0" "$*"; }
ok()     { printf '%s✓%s %s\n'   "$C_OK"  "$C_0" "$*"; }
warn()   { printf '%s!%s %s\n'   "$C_WARN" "$C_0" "$*"; }
err()    { printf '%s✗%s %s\n'   "$C_ERR" "$C_0" "$*" >&2; }
hold()   { printf '%s⏸%s %s\n'  "$C_WARN" "$C_0" "$*" >&2; exit 75; }
need()   { command -v "$1" >/dev/null 2>&1 || { err "$1 bulunamadi"; exit 1; }; }

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
derle() {
  header "derle"
  python3 "$ROOT/bedrock/build.py" | tail -3
  [ -f "$MCADDON" ] || { err "derleme ciktisi yok: $MCADDON"; exit 1; }
  ok "hazir: $(basename "$MCADDON")"
}

# Calisan emulatorlerden AVD adi $1 olanin seri numarasi. Baska bir proje
# (bilgebaykus) ayni anda kendi AVD'siyle calisabilir; "ilk emulator" onunki
# olabilir ve paketi yanlis cihaza gonderir (2026-09-23).
emulator_of_avd() {
  local s
  for s in $("$ADB" devices | awk '$1 ~ /^emulator-/ && $2=="device"{print $1}'); do
    [ "$("$ADB" -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r')" = "$1" ] && { printf '%s\n' "$s"; return 0; }
  done
  return 1
}

# Calisan bir emulatorun seri numarasini stdout'a yazar; yoksa baslatir.
# Tum ilerleme mesajlari stderr'e gider ki $(ensure_emulator) sadece seriyi alsin.
ensure_emulator() {
  [ -x "$EMU_BIN" ] || { err "emulator bulunamadi: $EMU_BIN — ANDROID_HOME dogru mu?"; exit 1; }
  # Varsayilan mc_tablet: Play Store'lu imaj, Minecraft ordan kurulu (docs/emulator.md).
  local serial avd="${AVD:-}"
  [ -n "$avd" ] || avd="$("$EMU_BIN" -list-avds 2>/dev/null | awk '/^mc_tablet$/{f=1} END{print f?"mc_tablet":""}')"
  [ -n "$avd" ] || avd="$("$EMU_BIN" -list-avds 2>/dev/null | head -1)"
  [ -n "$avd" ] || { err "hic AVD yok. Android Studio > Device Manager ile bir tane olustur."; exit 1; }
  if serial="$(emulator_of_avd "$avd")"; then
    header "emulator (zaten calisiyor: $avd = $serial)" >&2
    printf '%s\n' "$serial"; return 0
  fi
  header "emulator baslatiliyor: $avd" >&2
  # -gpu ACIKCA verilir: config.ini'deki hw.gpu.mode=swiftshader_indirect
  # komut satirindan baslatmada uygulanmiyor, emulator ana makine GPU'suna
  # duser ve Minecraft paketin sikistirilmis dokularini reddedip SIYAH ekran
  # verir (2026-09-23'te yasandi; docs/emulator.md).
  "$EMU_BIN" -avd "$avd" -no-boot-anim -gpu "${EMU_GPU:-swiftshader_indirect}" >/dev/null 2>&1 &
  local i=0
  until serial="$(emulator_of_avd "$avd")"; do
    i=$((i + 1)); [ "$i" -gt 120 ] && { err "emulator adb'de gorunmedi (120s)"; exit 1; }
    sleep 1
  done
  i=0
  until [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    i=$((i + 1)); [ "$i" -gt 180 ] && { err "emulator acilis zaman asimi (180s)"; exit 1; }
    sleep 1
  done
  # boot_completed paylasilan depolamanin hazir oldugunu soylemez: hemen
  # ardindan gelen push "secure_mkdirs() failed" ile dusuyordu.
  i=0
  until "$ADB" -s "$serial" shell ls /sdcard/Download >/dev/null 2>&1; do
    i=$((i + 1)); [ "$i" -gt 60 ] && { err "/sdcard hazir olmadi (60s)"; exit 1; }
    sleep 1
  done
  serial="$(emulator_of_avd "$avd")"
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

# ==========================================================================
# Islemler — ajans.toml'daki her anahtar icin bir op_ fonksiyonu.
# ==========================================================================

op_build() { derle; }

# Tablete gondermeden onceki tam kapi. Once ajans.toml ile buradaki op_
# fonksiyonlarinin uyusmasi (pano islemleri oradan okuyor), sonra paketin dort
# katmani (bedrock/tools/test.sh; CI de onu kosar).
op_verify() {
  header "ajans.toml ve ctl"
  "$ROOT/ctl" --check || { err "ajans.toml ile ctl uyusmuyor (yukari bak)"; exit 1; }
  bash "$ROOT/bedrock/tools/test.sh"
}

# ./ctl deploy android            -> bagli tum tabletler
# ./ctl deploy android zeynep     -> yalniz o cocugun tableti
op_deploy_android() {
  local hedef="${1:-all}" devs
  need "$ADB"
  derle
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
    hold "tablet yok. Tablet acik ve ayni wifi'da mi? Ilk kurulum: ./ctl setup wifi kur"
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

# iPhone'a uygulama KURMAZ: paketi iCloud Drive'a birakir, iPhone'da Dosyalar
# uygulamasindan Minecraft ile acilir.
op_deploy_icloud() {
  need xcrun
  derle
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

op_deploy_android_emu() {
  need "$ADB"
  derle
  local serial; serial="$(ensure_emulator)"
  header "emulatore gonder ($serial)"
  "$DEPLOY" --no-build "$serial"  # yalnizca bu emulator hedeflenir
  android_launch_mc "$serial"
}

op_deploy_ios_sim() {
  need xcrun
  derle
  header "iPhone simulator"
  local dev="${IOS_SIM:-iPhone 17}"
  xcrun simctl boot "$dev" 2>/dev/null || true      # zaten aciksa hata verir -> yut
  open -a Simulator >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$dev" 2>/dev/null || true
  ok "simulator acik: $dev"
  cat <<EOF
  SINIRLAMA: Minecraft Bedrock iOS Simulator'da CALISMAZ — App Store yalnizca
  gercek cihaz (arm64) surumunu dagitir, simulator surumu yoktur. Bu komut
  simulatoru acmaktan oteye gidemez. Gercek iOS denemesi icin: ./ctl deploy icloud
  .mcaddon: $MCADDON
EOF
}

# ./ctl setup wifi        -> yayindaki tabletlere baglan
# ./ctl setup wifi kur    -> ILK kurulum; bir kereligine KABLO ister
op_setup_wifi() {
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
      ok "kablo cikarilabilir. Bundan sonra: ./ctl deploy android"
      ;;
    bagla|connect)
      header "Bonjour ile taraniyor (${TARAMA_SN} sn)"
      wifi_bagla || hold "yayinda tablet yok. Tablet acik ve ayni wifi'da mi? Misafir agi cihazlari yalitir. Ilk kurulum: ./ctl setup wifi kur"
      ;;
    *) err "bilinmeyen: ./ctl setup wifi $1  (bagla|kur)"; exit 64 ;;
  esac
}

op_status() {
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
  [ "$var" = 1 ] || warn "cihaz yok. Kablosuz icin: ./ctl setup wifi"
  [ -f "$ADLAR" ] || warn "kod adi dosyasi yok: $ADLAR"
  header "AVD'ler  (AVD=<ad> ./ctl deploy android-emu)"
  if [ -x "$EMU_BIN" ]; then "$EMU_BIN" -list-avds 2>/dev/null || true
  else warn "emulator bulunamadi: $EMU_BIN"; fi
  header "iOS simulatorleri  (IOS_SIM=<ad> ./ctl deploy ios-sim)"
  if command -v xcrun >/dev/null 2>&1; then
    xcrun simctl list devices available 2>/dev/null | grep -E '^\s+iPhone|^\s+iPad' | sed 's/ *(.*//' | sed 's/^ */  /' || true
  else warn "xcrun yok"; fi
}

# >>> ajans runtime 1 >>>
# Written by `ajans contract sync`; edits here are overwritten. The operations
# are declared in ajans.toml and implemented above as op_<verb>[_<target>].

_ajans_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

_ajans_fn() {
	local name="op_$*"
	name="${name// /_}"
	name="${name//-/_}"
	printf '%s' "${name//./_}"
}

# Reads ajans.toml with the first python3 that has tomllib. Modes: list [verb], keys.
_ajans_toml() {
	local py
	if [[ ! -f "$_ajans_root/ajans.toml" ]]; then
		printf '✗ no ajans.toml beside %s\n' "$_ajans_root/ctl" >&2
		return 66
	fi
	for py in python3 /opt/homebrew/bin/python3 /usr/local/bin/python3; do
		if ! command -v "$py" >/dev/null 2>&1 || ! "$py" -c 'import tomllib' 2>/dev/null; then
			continue
		fi
		"$py" - "$_ajans_root/ajans.toml" "$@" <<'PY'
import re, sys, tomllib
try:
    with open(sys.argv[1], 'rb') as f:
        manifest = tomllib.load(f)
except tomllib.TOMLDecodeError as e:
    sys.exit(f'✗ ajans.toml: {e}')
ops, verbs = manifest.get('operations', {}), manifest.get('verbs', {})
mode, rest = sys.argv[2], sys.argv[3:]
if mode == 'keys':
    for key in ops:
        print('op_' + re.sub(r'[ .-]', '_', key))
elif rest:
    for key, op in ops.items():
        verb, _, target = key.partition(' ')
        if verb == rest[0] and target:
            print(f"{target}\t{op.get('summary', '')}")
else:
    seen = []
    for key in ops:
        verb = key.partition(' ')[0]
        if verb not in seen:
            seen.append(verb)
            print(f"{verb}\t{ops[verb]['summary'] if verb in ops else verbs.get(verb, '')}")
PY
		return
	done
	printf '✗ reading ajans.toml needs python3 3.11 or later (tomllib)\n' >&2
	return 69
}

_ajans_check() {
	local declared defined missing extra fn count noun
	declared="$(_ajans_toml keys)" || return
	declared="$(printf '%s\n' "$declared" | sort)"
	defined="$(declare -F | awk '$3 ~ /^op_/ { print $3 }' | sort)"
	missing="$(comm -23 <(printf '%s\n' "$declared") <(printf '%s\n' "$defined") | sed '/^$/d')"
	extra="$(comm -13 <(printf '%s\n' "$declared") <(printf '%s\n' "$defined") | sed '/^$/d')"
	if [[ -z "$missing" && -z "$extra" ]]; then
		count="$(printf '%s\n' "$declared" | grep -c .)"
		if [[ "$count" == 1 ]]; then noun=operation; else noun=operations; fi
		printf '✓ ajans.toml and ctl agree: %s %s\n' "$count" "$noun"
		return 0
	fi
	if [[ -n "$missing" ]]; then
		while IFS= read -r fn; do printf '✗ declared, no function: %s\n' "$fn" >&2; done <<<"$missing"
	fi
	if [[ -n "$extra" ]]; then
		while IFS= read -r fn; do printf '✗ function, not declared: %s\n' "$fn" >&2; done <<<"$extra"
	fi
	return 1
}

_ajans_main() {
	case "${1:-}" in
	'' | --list | -h | --help)
		_ajans_toml list
		return
		;;
	--check)
		_ajans_check
		return
		;;
	esac
	if [[ "${2:-}" == --list ]]; then
		_ajans_toml list "$1"
		return
	fi
	local fn
	if [[ $# -ge 2 ]]; then
		fn="$(_ajans_fn "$1 $2")"
		if declare -F "$fn" >/dev/null; then
			shift 2
			"$fn" "$@"
			return
		fi
	fi
	fn="$(_ajans_fn "$1")"
	if declare -F "$fn" >/dev/null; then
		shift
		"$fn" "$@"
		return
	fi
	if declare -F | awk -v p="$fn"_ '$3 ~ "^" p { found = 1 } END { exit !found }'; then
		printf '✗ %s needs a target: ./ctl %s --list\n' "$1" "$1" >&2
	else
		printf '✗ %s: not an operation of this project (./ctl --list)\n' "$*" >&2
	fi
	return 64
}

_ajans_main "$@"
# <<< ajans runtime <<<
