# PLAN.md — Super TNT Mod

## Kılavuz İlkeler

Bu bölüm, tüm geliştirme çalışmalarında uyulması gereken kalite standartlarını tanımlar.

### Plan Odaklı Geliştirme
- Her görev bu dosyada planlanır ve önceliklendirilir. Plansız iş yapılmaz.
- Çalışma sırasında keşfedilen hatalar, fikirler veya iyileştirmeler bu dosyaya eklenir — anında düzeltilmez.
- Bir göreve başlamadan önce analizi ve kabul kriterleri net olmalıdır.

### Anlamsal Commit'ler
- Değişiklikler anlamsal gruplara ayrılarak commit edilir. Bir commit tek bir mantıksal değişikliği temsil eder.
- Her commit kendi başına derlenebilir ve tutarlı olmalıdır.
- Commit mesajı değişikliğin *ne*'sini değil *neden*'ini açıklar.

### Belge Güncelliği
- Kod değişiklikleriyle birlikte ilgili belgeler (README.md, PLAN.md, lang dosyaları) aynı commit'te güncellenir.
- Belgelerin eskimesine izin verilmez. Bir özellik eklendiyse README.md'de yer almalıdır.

### Commit Öncesi İnceleme Kontrol Listesi
Her commit öncesi şu perspektiflerden gözden geçir:

| # | Perspektif | Kontrol |
|---|-----------|---------|
| 1 | Tamlık | Tüm gerekli dosyalar (blok, entity, model, texture, recipe, items JSON, lang, advancement) ekli mi? |
| 2 | Doğrulama | `./gradlew build` başarılı mı? Oyun içi test yapıldı mı? |
| 3 | Performans | N+1 döngü, sınırsız koleksiyon, lag spike riski var mı? Multi-tick işleme gerekli mi? |
| 4 | Kullanıcı Deneyimi | Tooltip'ler açıklayıcı mı? Etkileşim sezgisel mi? Geri bildirim yeterli mi? |
| 5 | Eğlence Faktörü | Efektler, görsel geri bildirim, sürpriz unsurları yeterli mi? |
| 6 | Türkçe Çeviriler | `tr_tr.json` ve `en_us.json` güncel ve eksiksiz mi? Doğru Türkçe karakterler (çöşığü) kullanılmış mı? |
| 7 | Semboller / İkonlar | Özel texture'lar, model dosyaları, items JSON dosyaları eksiksiz mi? |

### Yeni TNT / Eşya Ekleme Kontrol Listesi
1. Block sınıfı (`block/`)
2. Entity sınıfı (`entity/`)
3. `ModBlocks.java` kaydı
4. `ModEntities.java` kaydı
5. `SuperTntModClient.java` renderer kaydı
6. Blockstate JSON
7. Block model JSON
8. Item model JSON + `items/*.json`
9. Texture dosyaları
10. Recipe JSON
11. `en_us.json` — isim + tooltip
12. `tr_tr.json` — isim + tooltip
13. README.md güncellemesi
14. Advancement (opsiyonel)

---

## Tamamlanan Görevler

| ID | Kategori | Özet |
|----|----------|------|
| D-01 | TNT | Elmas TNT — 2.5x patlama gücü |
| D-02 | TNT | Altın TNT — 5 küçük patlama dalgası |
| D-03 | TNT | Kaya Katmanı TNT — bedrock dahil her şeyi kırar (25 blok yarıçap) |
| D-04 | TNT | Zümrüt TNT — değerli maden yağmuru |
| D-05 | TNT | Yıldırım TNT — 20 yıldırım fırtınası |
| D-06 | TNT | Nükleer TNT — dev patlama + radyasyon zehri |
| D-07 | TNT | Dondurucu TNT — su dondurma, kar yağdırma |
| D-08 | TNT | Odun TNT — ağaç yok etme, 1 dk sonra geri gelme |
| D-09 | TNT | Komut Bloğu TNT — yapılandırılabilir hedef blok ve yarıçap |
| D-10 | TNT | Yürüyen TNT — göz teması takibi, AI pathfinding |
| D-11 | TNT | Canavar Dondurucu TNT — düşman mob'ları 10 dk dondurma |
| D-12 | TNT | Su TNT — ateş söndürme, su dalgası |
| D-13 | TNT | Gökkuşağı TNT — blokları renkli yüne dönüştürme (30 blok yarıçap) |
| D-14 | TNT | Lego TNT — lego tuğla yapıları inşa etme |
| D-15 | TNT | Makarna TNT — blokları yenilebilir bloklara dönüştürme |
| D-16 | TNT | Şeker TNT — çikolata/şeker dünyası + hız efekti |
| D-17 | TNT | Küçülten TNT — canlıları küçültme |
| D-18 | TNT | Büyüten TNT — canlıları büyütme |
| D-19 | TNT | Temizleyici TNT — efekt ve boyut temizleme |
| D-20 | TNT | Pasta TNT (Fake TNT) — pasta görünümü, etkileşimde patlama |
| D-21 | TNT | Zıplatan TNT — canlıları gökyüzüne fırlatma |
| D-22 | TNT | Mıknatıs TNT — 3 sn çekme + patlama |
| D-23 | TNT | Yerçekimi TNT — 10 sn ters yerçekimi |
| D-24 | TNT | Görünmez TNT — taş kılığı |
| D-25 | TNT | Takas TNT — konum karıştırma |
| D-26 | Blok | TNT Kapı — sahip tabanlı kapı, izinsiz erişimde patlama |
| D-27 | Blok | Şifreli TNT Sandık — chat tabanlı şifre sistemi |
| D-28 | Blok | Yakınlık Mayını — 2 blok yakınında patlama |
| D-29 | Blok | Ayna — Enderman'ları kendilerine saldırtma |
| D-30 | Blok | Işık Bloğu — 100 blok yarıçapında düşman yok etme |
| D-31 | Blok | Lego Tuğla — 16 renk dekoratif blok |
| D-32 | Blok | Portal — ışınlanma portalı (Portal Silahı ile oluşturulur) |
| D-33 | Blok | Tünel Blok — kısmen kazılmış blok (Tünel Kazma Aleti ile) |
| D-34 | Eşya | TNT Frizbi — atılabilir, + şeklinde yıkım, geri dönüş |
| D-35 | Eşya | Portal Silahı — pembe/yeşil portal ışınlanma |
| D-36 | Eşya | Tünel Kazma Aleti — 1/12 ölçekte sub-voxel kazma |
| D-37 | Eşya | Kanca — bloklara saplanma ve çekme |
| D-38 | Eşya | Among Us Rapor — anında öldürme |
| D-39 | Eşya | Küçültme Topu — kendini küçültme |
| D-40 | Eşya | Büyütme Topu — kendini büyütme |
| D-41 | Eşya | Küçültme İksiri — hedefi küçültme |
| D-42 | Eşya | Büyütme İksiri — hedefi büyütme |
| D-43 | Eşya | Ölçek Kilidi — boyut değişikliğini engelleme |
| D-44 | Eşya | Çizim Eşyası — 16 katman, 16 renk, lego blok inşası |
| D-45 | Eşya | Craft Baltası — iki nokta arası duvar inşası |
| D-46 | Eşya | Acılı Cips — Hız III efekti (20 sn) |
| D-47 | Eşya | Enerji Kristali — bedrock kırma |
| D-48 | Eşya | Pembe / Yeşil Lego Tuğla — crafting malzemeleri |
| D-49 | Zırh | TNT Zırh Seti — 4 parça, saldırana patlama |
| D-50 | Sistem | Mixin: TNT zırh hasar tepkisi |
| D-51 | Sistem | Mixin: Ölçek sınırı (ClampedEntityAttribute) |
| D-52 | Sistem | Ağ: DrawingC2SPayload (çizim istemci-sunucu iletişimi) |
| D-53 | Başarım | 7 advancement: root, collect_all, first_craft, nuclear_power, frisbee_master, walking_danger, ice_age |
| D-54 | Kaynak | 48 crafting tarifi |
| D-55 | Kaynak | 156 çeviri girişi (en_us + tr_tr) |

---

## Planlanan Görevler

| ID | Durum | Boyut | Kategori | Özet | Detay |
|----|-------|-------|----------|------|-------|
| P-01 | Tamamlandı | L | Zırh | Ametist Zırh | [Detay](#p-01-ametist-zırh) |
| P-02 | Tamamlandı | XL | Eşya | Kara Delik | [Detay](#p-02-kara-delik) |
| P-03 | Tamamlandı | M | Eşya | Yıldırım Büyüsü | [Detay](#p-03-yıldırım-büyüsü) |
| P-04 | Tamamlandı | XL | Eşya | Kontrol Kumandası | [Detay](#p-04-kontrol-kumandası) |
| P-05 | Tamamlandı | L | Blok | Blocker Sandık | [Detay](#p-05-blocker-sandık) |
| P-06 | Tamamlandı | XL | Mob | Ender Send | [Detay](#p-06-ender-send) |
| P-07 | Tamamlandı | S | Bakım | fabric.mod.json açıklaması güncelle | Açıklama 25 TNT + 8 blok + 20+ eşya olarak güncellendi. |
| P-08 | Tamamlandı | S | Bakım | Advancement'lar güncelle | 25 TNT türü için collect_all ve first_craft güncellendi. |
| P-09 | Tamamlandı | M | Bakım | Crafting tarifleri denetimi | Pembe/Yeşil Lego Tuğla tarifleri eklendi. |

---

## Detaylı Görev Açıklamaları

### P-01: Ametist Zırh

**Kategori:** Zırh  
**Boyut:** L  
**Durum:** Bekliyor

**Açıklama:**
Giyildikten sonra çıkarılamayan özel bir zırh seti. Normal yollarla çıkarılamaz; ancak Enerji Kristali kullanarak yukarı ok tuşuna basılırsa zırh "gevşer" ve başka zırhlarla değiştirilebilir hale gelir.

**Analiz ve Açık Sorular:**
- **Çıkarılamama mekanizması:** Envanter etkileşimini engellemek için mixin veya event gerekebilir. Vanilya Minecraft'ta zırh slot'ları serbest değiştirilebilir — bunu engellemek `ScreenHandler` veya `InventoryChangedEvent` seviyesinde müdahale gerektirir.
- **Gevşetme mekanizması:** Enerji Kristali zaten mevcut bir eşya. Yukarı ok tuşu (`W` veya `Up Arrow`?) keybind olarak dinlenmeli. Client-side tuş dinleme + server-side doğrulama gerekir (yeni bir `C2SPayload`).
- **Zırh istatistikleri:** Elmas zırh seviyesinde mi? Özel dayanıklılık? TNT Zırh gibi özel bir efekti olacak mı (örneğin ametist parçacık efekti)?
- **Crafting tarifi:** Ametist parçası/bloğu + ne? Nether Yıldızı gibi nadir bir malzeme ile mi?
- **Texture/Model:** 4 parça (kask, göğüslük, pantolon, bot) için ayrı texture gerekir. Ametist temalı mor/parlak tasarım.
- **Gevşetme sonrası:** Zırh tamamen çıkarılabilir mi yoksa sadece değiştirilebilir mi? Gevşeme kalıcı mı yoksa tekrar kilitlenir mi?

### P-02: Kara Delik

**Kategori:** Eşya  
**Boyut:** XL  
**Durum:** Bekliyor

**Açıklama:**
Kullanıldığında karşı tarafı (düşmanı) kör eder ve kendine doğru çekerek öldüren bir eşya.

**Analiz ve Açık Sorular:**
- **Hedefleme:** Bakılan hedefe mi yoksa belirli bir yarıçapa mı etki eder? Tek hedef mi çoklu mu?
- **Körlük efekti:** Vanilya `StatusEffects.BLINDNESS` kullanılabilir. Süre ne kadar?
- **Çekme mekanizması:** Mıknatıs TNT'deki (`MagnetTntEntity`) velocity manipülasyonu referans alınabilir. Sürekli çekme + hasar mı? Anlık öldürme mi?
- **Görsel efekt:** Kara delik partikül efekti (siyah/mor girdap). Client-side partikül rendering gerekir.
- **Ses efekti:** Özel ses dosyası mı yoksa mevcut Minecraft sesleri mi?
- **Denge:** Çok güçlü olabilir. Cooldown süresi? Nadir crafting malzemesi? Tek kullanımlık mı?
- **Oyuncu etkileşimi:** PvP'de oyunculara da çalışır mı? Ölçek Kilidi veya başka bir eşya koruma sağlar mı?

### P-03: Yıldırım Büyüsü

**Kategori:** Eşya  
**Boyut:** M  
**Durum:** Bekliyor

**Açıklama:**
Kullanıldığında dokunulan yere şimşek çaktıran bir eşya.

**Analiz ve Açık Sorular:**
- **Kullanım şekli:** Sağ tıklama ile bloğa mı yoksa mob'a mı hedeflenir? Yıldırım TNT'den farkı: tek seferlik, kontrollü yıldırım.
- **Uygulama:** `world.spawnEntity(EntityType.LIGHTNING_BOLT)` — Yıldırım TNT'deki (`LightningTntEntity`) kod referans alınabilir.
- **Menzil:** Dokunma mesafesi (5 blok) mı yoksa bakılan noktaya mı (raycast)?
- **Dayanıklılık:** Sınırsız kullanım mı? Durability bar mı? Tek kullanımlık mı?
- **Crafting:** Şimşek Çubuğu + TNT + başka malzeme?

### P-04: Kontrol Kumandası

**Kategori:** Eşya  
**Boyut:** XL  
**Durum:** Bekliyor

**Açıklama:**
Kullanıldığında T tuşuna basılırsa 25 blok yakınındaki tüm mob'ları (oyuncular dahil) kontrolden çıkarır: 1.5 dakika boyunca hepsi kör olur ve donar.

**Analiz ve Açık Sorular:**
- **Aktivasyon:** T tuşu özel keybind mi yoksa sağ tık + T kombinasyonu mu? Eşya elde tutulurken T'ye basılması mantıklı. Client-side keybind + C2S payload gerekir.
- **"Kontrolden çıkarma":** Mob AI devre dışı bırakma — `NoAI` NBT etiketi veya AI goal temizleme. Vanilya `mob.setAi(false)` yeterli olabilir.
- **Körlük + Dondurma:** `BLINDNESS` + `SLOWNESS 255` veya `setFrozenTicks()`. Canavar Dondurucu TNT'deki (`MobFreezeTntEntity`) dondurma mekanizması referans alınabilir.
- **Süre:** 1.5 dakika = 1800 tick.
- **Oyuncular dahil:** Oyunculara `BLINDNESS` + `SLOWNESS` uygulanabilir ama `setAi(false)` çalışmaz. Hareket kısıtlama için farklı bir yaklaşım gerekir (çok yüksek slowness?).
- **Denge:** 25 blok yarıçap + oyuncu dahil + 1.5 dk çok güçlü. Cooldown? Nadir malzeme? Tek kullanımlık?

### P-05: Blocker Sandık

**Kategori:** Blok  
**Boyut:** L  
**Durum:** Bekliyor

**Açıklama:**
Sadece koyan kişi açabilir. Başkası açarsa anında ölür ve 1 dakika boyunca hiçbir sandık açamaz.

**Analiz ve Açık Sorular:**
- **Farkı Şifreli TNT Sandık'tan:** Şifreli Sandık şifre tabanlı, Blocker Sandık tamamen UUID tabanlı. Şifre yok, sadece sahiplik.
- **Ölüm mekanizması:** `player.kill()` mı yoksa yüksek hasar mı? Zırh koruma sağlar mı?
- **Sandık yasağı (1 dk):** Bu efekt nasıl uygulanır? Özel bir `StatusEffect` mi? `PersistentState` ile UUID+timestamp takibi mi? Sandık açma event'i dinlenip engellenecek — mixin veya `UseBlockCallback` ile.
- **Uygulama:** `EncryptedTntChestBlock` ve `ChestPersistentState` referans alınabilir; şifre mantığı çıkarılıp saf UUID kontrolü yapılır.
- **Görsel:** Özel texture. Şifreli sandıktan ayırt edilebilir olmalı (kırmızı kilit simgesi?).

### P-06: Ender Send

**Kategori:** Mob (Yeni Entity)  
**Boyut:** XL  
**Durum:** Bekliyor

**Açıklama:**
20 blok boyunda, 8 bacaklı dev bir mob. Kafası 5 blok boyunda ve kafa görünümü Enderman ile aynı.

**Analiz ve Açık Sorular:**
- **Entity türü:** Bu mod'daki ilk gerçek mob (Walking TNT hariç). `PathAwareEntity` veya `HostileEntity` extend edilecek. Walking TNT'nin AI sistemi referans alınabilir.
- **Boyut:** 20 blok boy çok büyük. Minecraft entity rendering ve hitbox sistemi bu boyutu destekliyor mu? Warden 3.5 blok, Ender Dragon ~8 blok. 20 blok boyut sınırları test edilmeli.
- **8 bacak:** Özel model gerektirir. Vanilya entity model sistemi (Blockbench ile .json model) veya özel `EntityModel` Java sınıfı. Bacak animasyonları karmaşık olabilir.
- **Kafa:** 5 blok boyunda Enderman kafası. Enderman'ın `EnderManEntityModel` kafa texture'ı referans alınabilir ama 5 kat büyütülecek.
- **Davranış:** Tanımlanmamış. Agresif mi? Nötr mü? Enderman gibi göz temasıyla mı tetikleniyor? Özel saldırı mekaniği?
- **Spawn koşulları:** Doğal spawn mı? Yumurta ile mi? Özel bir ritüel ile mi?
- **Sağlık ve hasar:** 20 blok boyutta çok dayanıklı olmalı. Enderman'ın 40 HP'si referans → belki 200-500 HP?
- **Loot tablosu:** Öldürülünce ne düşürür?
- **Bu görev çok büyük.** Model, animasyon, AI, spawn, loot gibi alt görevlere bölünmeli.

---

## Açık Sorular ve Tartışma

### Genel Denge Soruları
- Yeni eşyaların çoğu çok güçlü. Genel bir denge stratejisi belirlenmeli mi? (Cooldown, dayanıklılık, nadir malzeme, tek kullanımlık gibi)
- PvP dengesi özellikle Kara Delik, Kontrol Kumandası ve Among Us Rapor için düşünülmeli.

### fabric.mod.json Versiyonu
- Mod hâlâ `1.0.0` versiyonunda. Semantik versiyonlama başlatılmalı mı? (1.0.0 → 1.1.0 → ...)

### Advancement Genişletmesi
- Mevcut 7 başarım yeni içerikle orantısız. Yeni eşya/bloklar için ek başarımlar planlanmalı mı?

### Test Stratejisi
- Şu an sadece manuel test var. Basit bir JUnit test altyapısı kurulmalı mı? (Entity oluşturma, registry doğrulama gibi)
