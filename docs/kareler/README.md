# Oyun içi kareler

Emülatördeki Minecraft'tan `bedrock/tools/shots.sh` ile çekilmiş kareler
(düzen: `docs/emulator.md`). Her sahne için seçilmiş kareler `<sahne>-fNN.jpg`
(2160×1350, üstteki HUD düğmeleri ve kenarlar kırpılmış, yayına hazır) ve
bütün çekimin kontak sayfası `<sahne>.jpg` (30 kare, 6 sütun; ham hal).
Ham PNG'ler `bedrock/out/shots/` altında kalır, depoya girmez.

Web sitesi ve README bu klasörden besleniyor; yeni bir sahne çekince buraya
`shots.sh keep` ile al, aşağıdaki tabloya satırını ekle.

| Sahne | Komut | Seçilen | Not |
|---|---|---|---|
| `lego_high` | `CAM="0 -50 -6 0 -59 8" solo lego_tnt lego_high` | f10, f12 | Site ve README'nin ana görseli (f10) |
| `lego_tnt` | `solo lego_tnt` | f13 | Alçak kamera; tuğlalar ufka kadar |
| `seker_tnt` | yüksek kamera, `solo` | f08, f09 | Şeker kubbesi |
| `rainbow_tnt` | yüksek kamera, `solo` | f07, f08 | 20 blok yarıçapı renkli yün |
| `makarna_tnt` | yüksek kamera, `solo` | f09 | Saman, balkabağı, karpuz |
| `dag_tnt` | yüksek kamera, `solo` | f09, f12 | Taş tepe |
| `gulen_yuz_tnt` | yüksek kamera, `solo` | f08 | Sarı kubbe |
| `elmas_diyari_tnt` | yüksek kamera, `solo` | f08 | Elmas blok tarlası |
| `buz_tnt` | yüksek kamera, `solo` | f14, f15 | Buz yayılırken, yağış |
| `gokkusagi_tnt` | yüksek kamera, `solo` | f08 | Bu uzaklıktan küçük kalıyor; alçak kamera ister |
| `hero` | `stage; row lego_tnt gokkusagi_tnt pasta_tnt zumrut_yagmuru_tnt kalp_tnt dunya_tnt cizgi_tnt; cam 0 -52 -8 0 -58 8; line -6 6 8` | f08, f10, f12 | Yedi TNT birlikte; vanilya hat krater açtı |

"Yüksek kamera" = `CAM="0 -50 -6 0 -59 8"`; `solo`nun varsayılanı alçak
(`0 -56 -1 0 -58 8`). Çekim tarihi: 2026-09-22, paket v1.51.0.
