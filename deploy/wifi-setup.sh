#!/usr/bin/env bash
# Android tableti KABLOSUZ adb'ye baglar — bir kez kurunca kablo gerekmez.
# Ayni WiFi/LAN'da olman yeterli. Android 11+ (Samsung tabletler dahil).
#
# KULLANIM
#   deploy/wifi-setup.sh          # eslestirme sihirbazi
#   deploy/wifi-setup.sh --list   # kayitli kablosuz cihazlari goster/yeniden bagla
#
# Kurulumdan sonra: deploy/deploy.sh otomatik olarak kablosuz cihaza da gonderir.

set -euo pipefail
STORE="$HOME/.super_tnt_wifi_devices"

if [ "${1:-}" = "--list" ]; then
  [ -f "$STORE" ] || { echo "kayitli kablosuz cihaz yok. once: deploy/wifi-setup.sh"; exit 0; }
  echo "== kayitli kablosuz cihazlar (yeniden baglaniyor) =="
  while read -r ADDR; do
    [ -z "$ADDR" ] && continue
    printf "  %s ... " "$ADDR"
    adb connect "$ADDR" >/dev/null 2>&1 && echo "bagli" || echo "ulasilamadi (tablet acik+ayni WiFi mi?)"
  done < "$STORE"
  adb devices
  exit 0
fi

cat <<'EOF'
== Kablosuz adb kurulumu ==

Tablette (bir kez):
  1. Ayarlar > Telefon hakkinda > Yazilim bilgisi > "Yapi numarasi"na 7 kez
     dokun (Gelistirici secenekleri acilir) — daha once yaptiysan atla.
  2. Ayarlar > Gelistirici secenekleri > "Kablosuz hata ayiklama" > AC.
  3. "Kablosuz hata ayiklama" ekranini AC (icine gir).
  4. "Eslestirme koduyla cihazi eslestir"e dokun.
     -> Ekranda bir "IP adresi ve baglanti noktasi" (orn 192.168.1.42:37518)
        ve 6 haneli bir "WiFi eslestirme kodu" cikar.

Asagiya once ESLESTIRME ip:port'unu ve kodu, sonra kablosuz hata ayiklama
ANA ekranindaki ip:port'u gir (bu ikisi FARKLI portlardir).
EOF

read -r -p "Eslestirme ip:port (pair): " PAIRADDR
read -r -p "6 haneli eslestirme kodu:   " CODE
echo "== eslestiriliyor =="
adb pair "$PAIRADDR" "$CODE"

read -r -p "Kablosuz hata ayiklama ANA ekranindaki ip:port (connect): " CONNADDR
echo "== baglaniyor =="
adb connect "$CONNADDR"

# kaydet (tekrarsiz)
touch "$STORE"
grep -qxF "$CONNADDR" "$STORE" || echo "$CONNADDR" >> "$STORE"

echo
echo "== tamam =="
adb devices
echo "Artik kabloyu cikarabilirsin. Gondermek icin: deploy/deploy.sh"
echo "Tablet yeniden baslarsa: deploy/wifi-setup.sh --list ile tekrar bagla."
