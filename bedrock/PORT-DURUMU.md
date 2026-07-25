# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.19.1** · Mobil sürüm ana odak; Super TNT tek başına yeterli
olacak şekilde geliştiriliyor (MorphX / mutant paketine bağımlılık yok).

## v1.17 – v1.19.1 — mobil odak, kalite ve içerik

**Morph modülü (kendi, v1.19.0-1.19.1).** Dönüşüm Asası: bir mob'a dokun → o
mob olursun (15 mob: creeper, zombi, iskelet, enderman, demir golem, kurt,
domuz, inek, tavuk, örümcek, piglin, allay, wither iskeleti, ghast, slime).
Boşluğa sağ tık → insana dön. Asset id'leri bedrock-samples'tan doğrulandı;
sadece temiz render eden moblar (warden/sheep/villager/axolotl bilinçli dışlandı).
Küçülme (st:size) korundu. **Morph yetenekleri:** çömelme ile creeper patlar,
enderman ışınlanır, ghast ateş topu atar, allay süzülür; slime/örümcek zıplar,
tavuk/allay yavaş düşer, golem dayanıklı, kurt hızlı.

**Canavar modelleri (v1.17-1.18).** Mutant Warden özgün custom model + kendi
animasyonu (artık hareket ediyor, donuk değil). Ender Send özgün **3 kafalı**
model. İkisi de kendi geo + doku + walk/idle animasyonu.

**Simgeler (v1.18).** 15 düz-renk item'a anlamlı sembol (spawn egg'ler yumurta
silueti + tür yüzü — Mutant Warden yumurtası artık bulunuyor), 6 bloğa gerçek
Java dokusu.

**Portal (v1.18.1).** 8 renkli çift; her çift ayrı renk (karışmaz); bir kapıyı
kırınca eşi de gider.

**UX düzeltmeleri (v1.18.2-1.19).** Kontrol Kumandası sadece kendi TNT'ni
patlatır; Among Us bekleme süresi; hedef-gerektiren item'lara boş-tık ipucu;
Sahip Kapısı kalıcı-kayıp fix; Şifreli Sandık şifresini sadece sahip koyar;
Craft Baltası boyut güvenliği; Gökkuşağı tooltip dürüstlüğü.

**Aktarım (deploy/).** `deploy/deploy.sh` USB + kablosuz WiFi + emülatör +
iPhone/iCloud; `deploy/wifi-setup.sh` kablosuz adb kurulumu.

### Bilinen sınırlar / sıradaki
- Kilitli/Şifreli Sandık'ta gerçek eşya envanteri yok (Bedrock script-API'de
  özel sandık UI'si — ChestFormData — yok; sahiplik/şifre mantığı çalışıyor).
- ? Blok / Sahte TNT / Herobrine survival'da kendini düşürür (creative'de sorun
  değil; empty loot table eklenebilir).

---

## v1.16.0 — kendi morph + canavar modülü (eski, artık genişletildi)

## v1.16.0 — kendi (3. partiden bağımsız) morph + canavar modülü

Çocukların ücretli/3. parti morph paketleri tableti dondurdu; onun yerine
Super TNT'ye kendi hafif modülümüz eklendi (tam kontrol, ücretsiz, çakışmasız).

- **Dönüşüm Asası** — sağ tıkla İnsan → Creeper → Demir Golem → İnsan dönüş.
  Oyuncu görüntü olarak vanilla mob'a dönüşür (MorphX'in kanıtlanmış render-
  controller + `st:morph` property pattern'i; vanilla geometry/texture MC'den).
  **Saf görsel:** çarpışma kutusu küçülme sisteminden gelir, çakışma yok.
  **Bilinçli sınır:** Ender Dragon eklenmedi — geometry tek başına animate
  etmediği için (kendi animasyonu kopyalanmadan) donmuş görünürdü; creeper/golem
  statik bile net okunuyor. Dragon ileride kendi animasyonuyla eklenebilir.
- **Canavarlar (dev boss):** Ender Send + **Dev Zombi** + **Dev Creeper**
  (yaklaşınca şişip patlar). Her biri spawn yumurtasıyla çağrılır. Custom
  entity + ölçekli vanilla görsel — Ender Send'de kanıtlanmış desen.
- **Item ikonları:** 23 item artık Java'nın gerçek çizilmiş dokusunu kullanıyor
  (düz-renk yerine); 4 TNT zırh parçası + spawn yumurtaları ayırt edilebilir.

Java modu 71 TNT + 17 blok + 43 item içeriyor. Aşağıda ne portlandı, ne
portlanamadı — sebepleriyle.

Java modu 71 TNT + 17 blok + 43 item içeriyor. Aşağıda ne portlandı, ne
portlanamadı — sebepleriyle.

## v1.15.0 — içerik denetimi sonrası kalan 6 özellik

Java ↔ Bedrock tam içerik diff'i yapıldı: imkansız/atlanan dışında gerçekten
eksik olan her şey portlandı.

- **Ender Send** — dev enderman-benzeri boss (300 can, 15 hasar, ışınlanır).
  Spawn yumurtasıyla çağrılır. Java 20 blok; Bedrock'ta ~9 blok (scale 3.5) —
  20 blok chunk/tavan sorunları çıkarır. Vanilla enderman görseli ölçeklenir.
- **TNT Zırhı** (kask/göğüslük/pantolon/bot) — giyen hasar alınca saldırgana
  TNT patlamasıyla karşılık verir (`entityHurt` → `createExplosion`).
- **Yakınlık Mayını** — kurulunca 2 sn arm gecikmesi, sonra yaklaşanı patlatır.
- **Sahte TNT** — pasta kılığı; kır/etkileş → sadece oyuncuya 7 kalp, blok
  hasarı yok (troll).
- **Zeynep Redstone TNT** — dev patlama, patlatan hariç yakındaki herkesi yener.
- **Çizgi TNT** — kağıt + mürekkep + tüy saçar (el yazısı teması).

`shrink/grow iksiri` eklenmedi: boyut topları (`kucultme_topu`/`buyutme_topu`)
zaten aynı işi yapıyor — gereksiz tekrar olurdu.

## v1.14.0 — `@minecraft/server-ui` + dünya durumu ile 6 özellik daha

Önceki "container/UI yok" değerlendirmesi yanlıştı: `@minecraft/server-ui`
(form/şifre girişi) ve dünya dinamik özelliği (konum-anahtarlı JSON harita)
birçok özelliği açtı. server-ui 1.3.0'ın cihazda çalıştığı doğrulandı
(MorphX aynı sürümü kullanıyor).

- **Portal Silahı + Portal Bloğu** — sağ tıkla iki portal koy, aralarında
  ışınlan. Konum çifti oyuncu başına dünya özelliğinde saklanır (yeniden
  yüklemede kalır). 2 sn ışınlama beklemesi ile ileri-geri titreme önlenir.
- **Eşya Çalmaca** — sağ tıkla, en yakın oyuncunun bir eşyasını çal
  (`Container.transferItem`).
- **Sahip Kapısı** — sadece koyan kişi açar (4 sn açılır); başkası geçemez.
- **Kilitli Sandık** — sadece koyan kişi "açar"; başkası engellenir.
- **Şifreli Sandık** — `ModalFormData` ile şifre. Sahip şifre belirler,
  doğru gireni ödüllendirir (tek sefer). *Şifre ekranda görünür — Bedrock'ta
  maskeli alan yok.*
- **Kontrol Kumandası** — sağ tıkla, yerleştirdiğin tüm TNT'leri uzaktan
  ateşle (Java'daki T-tuşu yerine sağ tık jesti).

Sahiplik/şifre bir dünya JSON haritasında (`stnt:owners`) tutulur; Bedrock'ta
blokların kendi dinamik özelliği yoktur.

## v1.13.0 — çocukların sevdiği 3 özellik eklendi

- **Küçültme / Büyütme (oyuncu)** — Küçültme TNT + Büyütme TNT + Küçültme
  Topu + Büyütme Topu + Normal Boyut Topu. Oyuncu 5 kademe arasında
  gerçekten küçülüp büyür (görsel + çarpışma kutusu). Kalp TNT boyutu
  normale döndürür. **Yöntem:** `player.json` override — Mojang'ın vanilla
  RP client_entity'sine kademeli render-Molang ölçeği + BP `st:size`
  özelliği + kademe başına `collision_box`. `minecraft:scale` oyuncuda
  çalışmadığı için (script'ten "event does not exist" hatası) MorphX'in
  kanıtlanmış render-Molang yöntemi kullanıldı.
  **Motor sınırı:** Bu ölçek yalnız GÖRSELDİR — üçüncü şahıs modeli ve
  başkalarının gördüğü boyut değişir, ama oyuncunun kendi ILK-ŞAHIS göz
  yüksekliği motor tarafından sabittir (Bedrock'ta göz yüksekliği ne
  render-ölçekten ne collision_box'tan etkilenir). "Küçülünce dünya büyük
  görünsün" add-on ile yapılamaz; kamerayı tümüyle devralmak (`/camera` /
  Script Camera) normal oyunu bozar, çocuklar için uygun değil. Özellik
  bilinçli olarak sadece kozmetik.
  **Bilinen kısıt:** `player.json` Bedrock'ta paketler arası birleşmez —
  bu özellik MorphX ile **aynı dünyada** kullanılamaz (üstteki paket
  kazanır). Ayrı dünyalarda ikisi de çalışır.
- **Mini blok (küçük blok koyma)** — 8 renkli mini blok (hücre tabanında
  8×8×8 küp). Çocuklar minik yapılar kurar. Bir hücreye tek mini konur
  (Bedrock hücre başına tek blok).
- **Tünel açma** — `Delici Aleti` item'ı zaten var: sağ tıkla, önündeki 12
  bloğu deler ve içinden geçilebilir tünel açar. Java'daki tünel
  mekaniğinin karşılığı. (Kalıcı *oyulmuş blok* — katı duvar + geçilebilir
  delik — Bedrock'ta imkansız: çarpışma tek kutu, VoxelShape yok.)

## Portlandı ✅

### TNT (66 / 71)
Patlama, saçma, efekt, hava, zaman, blok yıkma/koyma/dönüştürme, mıknatıs,
takas, anında öldürme, yerçekimi (levitation), lego dönüştürme, ağaç yıkma.
Aile TNT'leri, Komut, Dağ, Elmas Zırh, Üreyen, Yürüyen dahil.

### Blok (26)
16 renk Lego, hayalet bloklar (içinden geçilir), ayna, ışık bombası,
doğru/yanlış altın plaka, zehir toprağı, ? bloğu, Herobrine Çağırıcı, End Kapısı,
+ TNT'lerin fırlayan varlıkları.

### Item (23)
Acılı Cips, Hız Eşyası, Yıldırım Büyüsü, Kara Delik, Enerji Kristali,
Lazer Kılıcı, Kanca, Dondurucu, Koku Bombası, Among Us Rapor, Delici,
Lav Kristali, Kanlı Kılıç, Kalp Baltası, Gökkuşağı Botları, End/Nether İncisi, TNT Frizbi, Craft Baltası, + ganimet item'ları (Kuruş, 200 TL, Lego tuğlaları).

## Portlanmadı — teknik sebeple

### Bedrock'ta imkansız (Java-only mekanizma)
| İçerik | Neden |
|---|---|
| Çizim Eşyası (canvas) | Bedrock script arayüzü boyama tuvali sunmuyor — sadece buton/kaydırıcı/liste. `server-ui` buton ızgarası (tıkla-boya mozaik) mümkün ama küçük çocuk için hantal; Java-only bırakıldı |
| Günlük (Diary) | Eşya-başına kalıcı veri (NBT) API'si yok — sahip-kilidi + saklanan metin eşyada tutulamaz. Oyuncu-özelliği + sohbet yakalama ile taklit edilebilir ama "kişisel kitap" hissi kaybolur |

### Yaklaşık portlanabilir ama bilinçli eklenmedi
| İçerik | Durum |
|---|---|
| Ametist zırh (çıkarılamaz) | Yapılabilir (`setEquipment` ile her tick geri giydir) ama 1-tick kaçış penceresi var ve çocuk için sinir bozucu — istenirse eklenir |

### Yaklaşık portlandı (Java'daki tam mekanik yerine benzeri)
- **Komut TNT** — ayar arayüzü yerine sabit 20-blok yıkım.
- **Elmas Zırh TNT** — sunucu kapatma yerine 40-blok anında öldürme + dev patlama.
- **Üreyen / Yürüyen TNT** — özyineli çoğalma / AI yerine güçlü patlama.
- **Dağ TNT** — arazi algoritması yerine taş tepe.
- **End/Nether İncisi** — mermi yerine bakılan yere ışınlama.
- **TNT Frizbi** — dönen disk yerine bakılan yerde artı-yıkım.
- **End Kapısı** — üstüne basınca End'e ışınlar.

### Hâlâ portlanmadı
| İçerik | Neden |
|---|---|
| Tünel *Blok* (kalıcı oyulmuş) | Çarpışma tek AABB — katı duvar + geçilebilir delik imkansız (tünel *item* olarak Delici Aleti'nde var) |

**Çözülenler (eski "hâlâ portlanmadı" satırları):**
- Küçültme/Büyütme (v1.13.0) — vanilla `player.json` override + render-Molang
  (eski not "script'ten ölçek değiştirilemiyor" yanlıştı; oyuncuda
  `minecraft:scale` çalışmıyor ama render-Molang çalışıyor).
- Tünel Kazma (v1.13.0) — Delici Aleti item'ı ile (blokları deler).
- TNT Kapı, Kilitli/Şifreli Sandık (v1.14.0) — dünya JSON haritası +
  `ModalFormData` (eski not "container UI yok" yanlıştı; `server-ui` var).
- Portal Silahı + Portal Blok (v1.14.0) — dünya dinamik özelliğinde konum
  çifti + ışınlama.

## Bilinen sınırlar (portlanan içerikte)

- **Fitil davranışı** vanilla TNT gibi (fırlar, düşer, yanıp söner), tam sadık.
- **Item mekanikleri** sürüme duyarlı API'ler (raycast, knockback, equippable)
  — çevrimdışı doğrulandı ama oyunda ilk kez çalışacak, sürprizler olabilir.
- **Dokular** üretilmiş: TNT'ler gerçek TNT şablonundan renklendirildi,
  bloklar/item'lar renk-tabanlı. Elmas ve Zıplatan TNT kendi Java PNG'lerini
  kullanıyor.

## Yapı

`bedrock/build.py` tek kaynak. TNTS / BLOCKS / ITEMS listeleri + effect
türleri. `super_tnt_BP/` ve `super_tnt_RP/` üretilen çıktı (gitignore).
Kurulum: `bedrock/install.sh` (MediaStore temizliği dahil).
