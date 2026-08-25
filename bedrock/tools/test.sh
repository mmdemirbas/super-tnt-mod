#!/bin/bash
# Tablete gondermeden once calistirilacak tek komut.
#
#     bash bedrock/tools/test.sh
#
# Dort katman, ucuzdan pahaliya. Her biri digerinin GORMEDIGI bir seyi gorur:
#   1. build      — paket uretiliyor mu
#   2. check_pack — kimlikler, dil satirlari, ipucu sozlesmesi (statik)
#   3. sim        — betik GERCEKTEN calisiyor mu (Minecraft olmadan)
#   4. mutasyon   — 2 ve 3 gercekten bir sey yakaliyor mu
#
# Uc numara olmadan ilk iki katman "her sey yolunda" der ve paket tablette
# acilir acilmaz olur: olmayan bir cagri, yanlis bir imza ya da modul
# yuklenirken atilan tek bir hata butun betigi sessizce oldurur.
set -u
cd "$(dirname "$0")/../.." || exit 1
fail=0

step() {
  printf '\n\033[1m== %s\033[0m\n' "$1"
  shift
  if "$@"; then
    return 0
  fi
  fail=$((fail + 1))
  printf '\033[31m   GECMEDI\033[0m\n'
}

rm -rf bedrock/__pycache__
step "1/4  paket uretimi" bash -c 'python3 bedrock/build.py | tail -3'
step "2/4  statik denetim" python3 bedrock/tools/check_pack.py
step "3/4  betigi calistir" node bedrock/tools/sim/run.mjs
step "4/4  mutasyonlar" bash -c 'bash bedrock/tools/mutation_test.sh | tail -2; bash bedrock/tools/sim/mutations.sh | tail -2'

printf '\n'
if [ "$fail" -eq 0 ]; then
  printf '\033[32mhepsi gecti — paket: bedrock/out/SuperTNT.mcaddon\033[0m\n'
  python3 - <<'PY'
import json
v = json.load(open("bedrock/super_tnt_BP/manifest.json"))["header"]["version"]
print("surum:", ".".join(map(str, v)))
print("adb push bedrock/out/SuperTNT.mcaddon /sdcard/Download/SuperTNT-%s.mcaddon" % ".".join(map(str, v)))
PY
else
  printf '\033[31m%d adim gecmedi — tablete gonderme\033[0m\n' "$fail"
fi
exit "$fail"
