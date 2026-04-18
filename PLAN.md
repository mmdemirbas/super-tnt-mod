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
- **Başka bir göreve geçmeden önce mevcut değişiklikler commit edilir.** Yarım kalan iş commit'siz bırakılmaz.

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

## Planlanan Görevler

| ID | Durum | Boyut | Kategori | Özet | Detay |
|----|-------|-------|----------|------|-------|
| P-10 | Bekliyor | M | Eşya | Lazer Kılıcı | [Detay](#p-10-lazer-kılıcı) |
| P-11 | Bekliyor | L | Blok | Herobrine Çağırıcı | [Detay](#p-11-herobrine-çağırıcı) |
| P-12 | Bekliyor | S | Eşya | Lav Kristali | [Detay](#p-12-lav-kristali) |

---

## Detaylı Görev Açıklamaları

### P-10: Lazer Kılıcı

**Kategori:** Eşya
**Boyut:** M
**Durum:** Bekliyor

**Açıklama:**
Sol tıkla kullanılan bir kılıç. Dokunulan bloğu anında yok eder. Bir canlıya vurulursa tek seferinde 15 kalp (30 HP) hasar verir — diğer oyuncular dahil.

**Analiz ve Açık Sorular:**
- **Blok yıkma:** Sol tıkla blok kırmayı özel bir eşyada override etmek için `PlayerBlockBreakEvents` veya `Item.useOnBlock()` kullanılabilir. Yavaş kırma animasyonu atlanarak anında kırma için `world.removeBlock()` çağrılabilir.
- **Canlıya hasar:** `Item.useOnEntity()` ile `entity.damage(source, 30f)` — 30 HP = 15 kalp.
- **Raycast / menzil:** Varsayılan saldırı menzili mi (3 blok)? Yoksa daha uzun bir lazer mı? Görsel efekt (partikül çizgisi) gerekir mi?
- **Ses efekti:** Lazer sesi — özel ses dosyası mı yoksa mevcut bir ses mi?
- **Denge:** Çok güçlü. Dayanıklılık (kaç kullanım)? Cooldown? Crafting malzemesi pahalı olmalı.
- **PvP:** Oyunculara 15 kalp tek vuruş çok güçlü. Tasarım gereği mi?

### P-11: Herobrine Çağırıcı

**Kategori:** Blok
**Boyut:** L
**Durum:** Bekliyor

**Açıklama:**
Yere koyulduğunda yanında anında bir Herobrine spawns olur.

**Analiz ve Açık Sorular:**
- **Herobrine entity:** Vanilla'da Herobrine yoktur. Özel bir mob olarak implemente edilmeli. Görünüm: Steve skin + beyaz gözler. Özel `EntityModel` + texture gerekir.
- **Davranış:** Agresif mi? Koyduğun kişiye mi saldırır, herkese mi? Herobrine'e özgü bir davranış (ışınlanma, çevre bozma, meşale söndürme)?
- **Spawn tetikleyicisi:** `Block.onPlaced()` veya `BlockEntity.onPlaced()` ile entity spawn edilebilir.
- **Blok koyulunca kaybolur mu:** Sadece tetikleyici mi, yoksa kalıcı bir blok mu? Blok koyulunca kaybolursa `world.removeBlock()` + `world.spawnEntity()`.
- **Sağlık ve hasar:** Herobrine için ne kadar HP ve saldırı gücü?
- **Loot:** Öldürülünce ne düşürür?
- **Crafting:** Ne ile yapılır? Nether Yıldızı + özel malzeme mantıklı olabilir.
- **Teknik not:** Entity model ve texture işi Walking TNT'den daha karmaşık. Blockbench ile .json model önerilir.

### P-12: Lav Kristali

**Kategori:** Eşya
**Boyut:** S
**Durum:** Bekliyor

**Açıklama:**
Elde tutulduğunda (hotbar veya envanter) oyuncuyu ateş ve lav hasarından tamamen korur.

**Analiz ve Açık Sorular:**
- **Koruma mekanizması:** `LivingEntityMixin` ile `fireImmune()` override edilebilir; ya da `EntityDamageEvent` / `LivingEntity.isFireImmune()` hook. Enerji Kristali'nin `items` etkinleştirme mantığı referans alınabilir.
- **Aktif koşul:** Sadece elde tutulunca mı (main/off hand) yoksa envanterde herhangi bir yerde mi? Ölçek Kilidi'nin `isInInventory()` kontrolü referans alınabilir.
- **Görsel:** Lav kristali texture — kırmızı/turuncu, kristal şekli.
- **Crafting:** Magma Bloğu / Ateş Topu + Elmas / Nether Yıldızı kombinasyonu mantıklı.
- **Tooltip:** "Elinde tut — ateş ve lavdan zarar görmezsin."

---

## Açık Sorular ve Tartışma

### Genel Denge Soruları
- Yeni eşyaların çoğu çok güçlü. Genel bir denge stratejisi belirlenmeli mi? (Cooldown, dayanıklılık, nadir malzeme, tek kullanımlık gibi)
- PvP dengesi özellikle Kara Delik, Kontrol Kumandası ve Lazer Kılıcı için düşünülmeli.

### fabric.mod.json Versiyonu
- Mod hâlâ `1.0.0` versiyonunda. Semantik versiyonlama başlatılmalı mı? (1.0.0 → 1.1.0 → ...)

### Advancement Genişletmesi
- Mevcut 7 başarım yeni içerikle orantısız. Yeni eşya/bloklar için ek başarımlar planlanmalı mı?

### Test Stratejisi
- Şu an sadece manuel test var. Basit bir JUnit test altyapısı kurulmalı mı? (Entity oluşturma, registry doğrulama gibi)
