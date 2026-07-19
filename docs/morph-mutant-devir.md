# MorphX + Mutant Creatures — Birleştirme ve Kurulum Devri

Tarih: 2026-07-19 · Cihaz: SM-X520 (`R5GYC4BGJJZ`) · Minecraft Bedrock 1.26.33.1

---

## 1. Ne yapıldı

İki ücretsiz Add-On indirildi, mutant mobların tamamı MorphX'in dönüşüm
listesine bağlandı, dördü tek `.mcaddon` içinde paketlenip tablete atıldı.

| | Paket | Sürüm | min_engine | Kaynak |
|---|---|---|---|---|
| Morph | MorphX | 1.1.1 (10 Haz 2026) | **1.26.0** | MCPEDL → `edge.mcpedl.com` doğrudan CDN |
| Mutant | Mutant Creatures Bedrock | 4.25 (11 Oca 2026) | 1.19.50 | MCPEDL → `edge.mcpedl.com` doğrudan CDN |

Reklam geçidi, Linkvertise veya CAPTCHA ile karşılaşılmadı — `__NUXT__` sayfa
state'inden doğrudan CDN URL'si çıkarıldı. İndirilen iki dosya da temiz zip;
`.exe/.apk/.msi/.sh/.dll` içermiyor.

**Cihazdaki dosya:** `/sdcard/Download/MorphX-Mutant-Merged.mcaddon`
(16 328 470 bayt, Mac'teki dosyayla bit bit aynı doğrulandı)

---

## 2. Senin tek dokunuşun

1. Tablette **Dosyalar** (My Files) → **Download** klasörünü aç.
2. `MorphX-Mutant-Merged.mcaddon` dosyasına **bir kez dokun**.
3. Açılan uygulama seçicide **Minecraft**'ı seç.
4. Minecraft açılır ve "İçe aktarıldı / Import başarılı" mesajı verir.

`adb` ile import tetiklenmeye çalışıldı (`am start` VIEW intent, hem genel hem
doğrudan MC bileşenine). Minecraft ön plana geldi ama dosya işlenmedi —
Android'in `file://` URI kısıtı. Zorlanmadı.

Tek dosyada **4 paket** var; tek dokunuşta dördü birden içe aktarılır.

---

## 3. Dünyada ne aktive edilecek

Dünya Ayarları → **Behavior Packs** ve **Resource Packs** altında dördünü de aç:

| Tür | Görünen ad |
|---|---|
| Behavior | `Morph X + Mutant [BP]` |
| Behavior | `Mutant Creatures Bedrock v4.25` |
| Resource | `Morph X + Mutant [RP]` |
| Resource | `Mutant Creatures Bedrock v4.25` |

Manifest bağımlılığı eklendiği için MorphX paketleri mutant paketleri olmadan
aktive edilemez — eksik bırakırsan Minecraft uyarır. Bu bilerek yapıldı: MorphX
artık mutant modellerine referans veriyor, mutant paketi kapalıyken morph
görünmez olurdu.

**Deney anahtarları (Experiments):** manifestlere göre **zorunlu değil**.
Script API sürümleri stable (`@minecraft/server` 1.14.0 ve 1.10.0, hiçbirinde
`-beta` yok). MorphX `capabilities: ["script_eval"]` istiyor; bu normalde
ek anahtar gerektirmez. Yine de menü hiç açılmazsa Bölüm 5'teki S3'e bak.

Cihazda kayıtlı dünya yok — yeni bir dünya kurman gerekecek.

---

## 4. Oyun içi kullanım (bunu bilmeden test edilemez)

MorphX'te morph **satın alınmaz, avlanır**:

1. Envanterinde **Soul Stone** (`pa:soul stone`) bulunmalı.
2. Bir mutant mob'u **öldür**. Öldürdüğün anda o mob'un morph'u kilidi açılır
   **ve anında ona dönüşürsün**.
3. Soul Stone'a sağ tık / uzun bas → morph menüsü. **Yeşil** yazan isimler
   açılmış, **kırmızı** yazanlar henüz avlanmamış. Kırmızıya basarsan
   *"Please collect a mob first to morph"* der.
4. İnsan haline dönmek için menüden `player` seç.

Kaynak: `interactMobs.js` (`mode_activate = 1` → tetikleyici `entityDie`).

---

## 5. Doğrulama checklist — her madde için "çalışmazsa sebebi"

Tek bir semptom bildirmen yeterli; eşlemeler tek turda düzeltmek için.

| # | Kontrol | Beklenen | Çalışmazsa muhtemel sebep |
|---|---|---|---|
| **S1** | Import sonrası paket listesi | 4 paketin dördü de görünür | Sadece bazıları geldiyse çok-paketli `.mcaddon` import'u kısmi kalmıştır → paketleri ayrı ayrı kurarım (bende ayrık hâlleri hazır) |
| **S2** | Dünya ayarlarında 4 paket aktive edilebiliyor | Hepsi açılır | "Bağımlılık eksik" uyarısı → mutant paketini önce aktive et; sırayı ben yanlış kurmuşsam manifest deps'i düzeltirim |
| **S3** | Dünyaya girince Soul Stone tarifi/eşyası var | Envanterde/craft'ta bulunur | Menü hiç açılmıyor + hiçbir script çalışmıyorsa → Script API sürüm uyuşmazlığı. En olası suçlu **mutant paketinin `@minecraft/server 1.10.0`** talebi (eski stable, 1.26'da kaldırılmış olabilir). Bunu bildir; mutant manifest'inde sürümü yükseltirim |
| **S4** | Morph menüsünde `mutant_*` isimleri görünüyor | 28 yeni satır, kırmızı | Vanilla 30 morph var ama mutantlar yok → `morph_item.js` dizim yüklenmemiş; BP paketi eski sürüm kalmış olabilir |
| **S5** | Menüdeki mutant satırlarının ikonu | **Boş/eksik ikon normal** | İkon üretilmedi (bkz. Bölüm 6). İsim okunuyorsa sorun yok |
| **S6** | Bir mutant zombie öldür | Kilidi açılır, anında dönüşürsün | Dönüşüm olmuyorsa → `pa:entity` property aralığı yazılamıyor; tip numarası 200'ü aşmış olabilir (aşmıyor, 121–148) veya event adı tutmuyor |
| **S7** | Üçüncü şahısta görünüm | Mutant modeli, doğru doku | **Görünmez** → mutant Resource Pack aktif değil ya da sıralamada MorphX RP'nin altında kalmış. **Mor/siyah damalı** → doku yolu çözülmedi (bende 0 çözülemeyen referans çıkmıştı, bildir) |
| **S8** | Hareket ederken animasyon | Model **sabit/donuk olabilir** | Bilinen boşluk, bkz. Bölüm 6 |
| **S9** | Birinci şahıs görünüm | Varsayılan oyuncu kolları | Bilinen boşluk, bkz. Bölüm 6 |
| **S10** | Menüden `player` seçip geri dön | İnsan hâline döner | Dönmüyorsa `pa:reset` bloğu eksik — bende 59 girdi var, bildir |

---

## 6. Bilinen boşluklar (dürüst liste)

**B1 — Animasyon yok (S8).** MorphX bağlı 30 vanilla morph'unun her biri için
~16 el yazımı animasyon içeriyor. Mutantlar için bunları üretmedim; mutant
paketinin kendi animasyonları var ama oyuncu iskeletine bağlanmaları ayrı bir
yazarlık işi. Sonuç: mutant morph'u doğru model ve dokuyla **görünür**, ama
yürürken uzuvları oynamayabilir. Bu, çalışmama değil eksik cila.

**B2 — Birinci şahıs (S9).** MorphX her morph'a ayrı birinci-şahıs geometrisi
yazıyor. Üretmedim; sadece üçüncü şahıs render controller'ı ekledim. Birinci
şahısta varsayılan oyuncu kolların görünür.

**B3 — Menü ikonları (S5).** Menü ikonu `textures/icons/morph_menu/<ad>/<ad>`
yolundan okunuyor. Mutantlar için ikon üretmedim (görsel varlık üretmek
uydurma olurdu). Buton çalışır, ikonu boş görünür.

**B4 — Varyant sabitlendi.** 5 mutantın `default` doku anahtarı yoktu; sabit bir
varyant seçildi: wolf→`pale_default`, axolotl→`marigold`, endolotl→`vanilla`,
villager ve zombie_villager→`base`. Morph tek görünümde kalır, varyant değişmez.

**B5 — MorphX'in kendi bozuk dosyaları.** Orijinal pakette 4 geçersiz JSON var
(`werewolf.json`, `goblin2.json`, `crocodile.json`,
`quadruped.morph.animation.json`). İndirilen dosyayla bit bit aynı — benim
değişikliğimden değil. Dokunmadım. Bunlar zaten paketin bağlanmamış
premium varlıkları.

**B6 — MorphX'in 31 morph'u zaten çalışmıyordu.** Ücretsiz sürüm menüde 61
morph gösteriyor ama sadece 30'u (hepsi vanilla) tam bağlı. werewolf, robot,
dragon, pikachu gibi 31 girdinin event'i, geometry'si, dokusu ve render
controller'ı pakette yok. Bu benim eklediklerimi etkilemez, ama menüde
tıklayınca hiçbir şey olmayan satırlar göreceksin — onlar bunlar.

---

## 7. Değişiklik kaydı

Değiştirilen 3 dosya + 1 yeni dosya. Mutant paketine **hiç dokunulmadı**.

| Dosya | Değişiklik |
|---|---|
| `MorphX[BP]/entities/player.json` | `pa:entity` range `[-1,120]` → `[-1,200]`; 28 yeni event (`pa:mutant_*`); `pa:transform` sequence 31→59; `pa:reset` sequence 31→59; 28 yeni component_group |
| `MorphX[RP]/entity/player.json` | 28 geometry + 28 texture + 28 material girdisi; 28 yeni render controller referansı (194 toplam) |
| `MorphX[BP]/scripts/morph_item.js` | `morphs` dizisi 61 → 89 girdi |
| `MorphX[RP]/render_controllers/mutant.morph.render_controllers.json` | **yeni** — 28 üçüncü-şahıs controller tanımı |
| `MorphX[BP]/manifest.json` | ad → "Morph X + Mutant [BP]"; mutant BP bağımlılığı eklendi |
| `MorphX[RP]/manifest.json` | ad → "Morph X + Mutant [RP]"; mutant RP bağımlılığı eklendi |

**UUID çakışması yoktu** — 8 UUID'nin tamamı benzersiz, hiçbiri değiştirilmedi.

**Vanilla override çakışması yok** — `player.json`'ı yalnızca MorphX override
ediyor; mutant paketi etmiyor. En riskli senaryo mevcut değil.

### Eklenen 28 mutant (tip numarasıyla)

```
121 mutant_zombie          131 mutant_zombie_pigman     141 mutant_skeleton_wolf
122 mutant_creeper         132 mutant_zombified_piglin  142 mutant_ocelot
123 mutant_enderman        133 mutant_piglin            143 mutant_axolotl
124 mutant_skeleton        134 mutant_piglin_brute      144 mutant_endolotl
125 mutant_wither_skeleton 135 mutant_villager          145 mutant_snow_golem
126 mutant_stray           136 mutant_evoker            146 mutant_spider_pig
127 mutant_bogged          137 mutant_vindicator        147 mutant_bouldering_zombie
128 mutant_husk            138 mutant_pillager          148 mutant_lobber_zombie
129 mutant_drowned         139 mutant_vex
130 mutant_zombie_villager 140 mutant_wolf
```

Bilerek dışarıda bırakılanlar (mob değil, yardımcı entity):
`mcreeper_explosion`, `mutant_creeper_death`, `mutant_enderman_explosion`,
`mutant_enderman_loot`, `mutated_evoker_fangs`, `mutated_iron_spikes`,
`target_thrower`, `*_bones` (4 adet), minion'lar.

### Doğrulama sonuçları

- `node --check morph_item.js` → geçti; 89 girdi, tekrar eden id/type yok
- 541 JSON parse → 4 hatalı, dördü de orijinalden gelen (B5)
- **Çözülemeyen geometry referansı: 0 / 28**
- **Çözülemeyen texture referansı: 0 / 28**
  (mutant RP'nin 175 geometry tanımı ve 298 doku dosyasına karşı denetlendi)

---

## 8. Yeniden üretim

```
.mcwork/merge.py     # birleştirme, karar defteri dosya başında
.mcwork/package.py   # 4 paketi tek .mcaddon yapar
.mcwork/out/MorphX-Mutant-Merged.mcaddon
```

`.mcwork/` gitignore'a eklendi.
