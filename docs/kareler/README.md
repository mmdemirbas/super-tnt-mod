# Oyun içi kareler

Emülatördeki Minecraft'tan `bedrock/tools/shots.sh` ile çekilmiş kareler
(düzen: `docs/emulator.md`). Her sahne için seçilmiş kareler `<sahne>-fNN.jpg`
(2160×1350, üstteki HUD düğmeleri ve kenarlar kırpılmış, yayına hazır) ve
bütün çekimin kontak sayfası `<sahne>.jpg` (30 kare, 6 sütun; ham hal).
`web/` altında aynı karelerin 1440 px kopyası durur — site onları kullanır
(tam boy ~700 KB, web ~270 KB). Ham PNG'ler `bedrock/out/shots/` altında
kalır, depoya girmez.

Web sitesi (`site/index.html` ve `site/kareler.html`) ile README bu klasörden
besleniyor; yeni bir sahne çekince buraya `shots.sh keep` ile al, aşağıdaki
tabloya satırını ekle, sonra açılış sayfasını yeniden üret
(`atolye/bin/web/landing/build.py specs/super-tnt-mod.json .`).

Aynı kareler kişisel sitede de kullanılır: `~/dev/mmdemirbas/mdemirbas-com`,
`src/content/projects.yaml` içindeki `super-tnt-mod` kartı
(`static/media/projects/super-tnt-lego.jpg`, 16:10 kırpılmış). Daha iyi bir
kare çıkınca oraya **yeni bir dosya adıyla** konur — eski ad bir hafta
önbellekte kalır.

Yeni bir eşya ya da TNT eklenince akış: `test.sh` → `./ctl deploy android` →
oyunda gör → iyi bir kare çek, buraya al → tablete gönder.

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
| `gokkusagi_tnt` | yüksek kamera, `solo` | f08 | Bu uzaklıktan küçük kalıyor; alçak kamera ister — sitede kullanılmadı |
| `patlayici_kum` | `stage`; `/setblock` ile x=-9..6 arası 3 aralıkla 6 `stnt:patlayici_kum` + x=9'a `stnt:diamond_tnt`; `cam 0 -50 -8 0 -59 8`; `fire -11 8`; `burst patlayici_kum 36 0.1` | f04, f05, f06, f07 | Zincir: f05 dalga sağdan sola, f06 Elmas TNT'nin fitili yanıyor — sitede f05, f06. 2026-09-23 |
| `hero` | `stage; row lego_tnt gokkusagi_tnt pasta_tnt zumrut_yagmuru_tnt kalp_tnt dunya_tnt cizgi_tnt; cam 0 -52 -8 0 -58 8; line -6 6 8` | f08, f10, f12 | Yedi TNT birlikte; vanilya hat krater açtı — sitede yalnız f12 |

"Yüksek kamera" = `CAM="0 -50 -6 0 -59 8"`; `solo`nun varsayılanı alçak
(`0 -56 -1 0 -58 8`). Çekim tarihi: 2026-09-22, paket v1.51.0.
