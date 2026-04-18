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

### Vizyon: Oyunun Ruhuna Uygun Geliştirme
- Spec'te belirtilmeyen detaylar için Minecraft'ın tasarım felsefesi rehber alınır: eğlence, keşif, sürpriz.
- Efektler görkemli olmalı. Yeni bir eşya veya mob ilk karşılaşmada "vay be" dedirtmeli.
- Tüm resource dosyaları (texture, model, lang, recipe, items JSON) eksiksiz teslim edilir.
- Build başarısız bırakılmaz. Her commit `./gradlew build` geçmeli.

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

Tüm görevler tamamlandı. Yeni görevler için buraya ekle.

---

## Detaylı Görev Açıklamaları

### P-10: Lazer Kılıcı

**Kategori:** Eşya
**Boyut:** M
**Durum:** Bekliyor

**Açıklama:**
Sağ tıkla aktive edilen lazer silahı. Bakılan yönde 9 blok menzilinde lazer atar. Dokunduğu bloğu anında yok eder. Bir canlıya vurursa tek seferinde 15 kalp (30 HP) hasar verir — diğer oyuncular dahil. Dayanıklılık sınırsız. Kullanımdan sonra 20 saniye soğuma süresi.

**Kesinleşmiş Tasarım:**
- **Kullanım:** Sağ tıkla aktive edilir. Raycasting ile 9 blok menzilinde hedef bulunur.
- **Hedef önceliği:** Önce entity kontrolü, sonra blok. İlk entity çarpışması kullanılır.
- **Blok yıkma:** `world.breakBlock()` ile anında yıkım, drop düşer.
- **Entity hasarı:** 30 HP (15 kalp), tüm canlılar dahil oyuncular.
- **Görsel efekt:** `END_ROD` + `ELECTRIC_SPARK` + `REVERSE_PORTAL` partikülleri boyunca lazer çizgisi; çarpma noktasında `FLASH` + patlama.
- **Ses:** `ENTITY_LIGHTNING_BOLT_THUNDER` yüksek pitch (1.8f).
- **Soğuma:** 400 tick = 20 saniye. `ItemCooldownManager.set(stack, 400)`.
- **Dayanıklılık:** Sınırsız (maxDamage yok, `maxCount(1)`).
- **Crafting:** Eye of Ender + Lightning Rod + Blaze Rod (dikey sütun).

### P-11: Herobrine Çağırıcı

**Kategori:** Blok
**Boyut:** L
**Durum:** Bekliyor

**Açıklama:**
Yere koyulduğunda anında bir Herobrine doğar. Blok koyulduktan sonra kendini kaldırır. Herobrine herkese saldırır — koyduğu kişiyi de ayırt etmez.

**Kesinleşmiş Tasarım:**
- **Tetikleyici:** `Block.onPlaced()` → blok kaldırılır, Herobrine entity spawn edilir.
- **Hedefleme:** `ActiveTargetGoal<LivingEntity>` — tüm canlı varlıklar hedef.
- **HP:** 200. **Hasar:** 8. **Hız:** 0.3. **Menzil:** 48 blok.
- **Davranış:** `MeleeAttackGoal` + `WanderAroundFarGoal` + `RevengeGoal`.
- **Renderer:** Block tabanlı (EnderSendEntity gibi) — beyaz beton kafa + gri beton gövde + beyaz renkli cam gözler.
- **Spawn efekti:** Wither spawn sesi + büyük duman/portal partikülleri.
- **Crafting:** 8 Obsidian + 1 Nether Star (merkez).
- **Despawn:** `cannotDespawn()` = true.

### P-12: Lav Kristali

**Kategori:** Eşya
**Boyut:** S
**Durum:** Bekliyor

**Açıklama:**
Elde tutulduğunda (ana el veya yardımcı el) oyuncuyu ateş ve lav hasarından tamamen korur. Aktif olduğunda ateş söndürülür.

**Kesinleşmiş Tasarım:**
- **Aktif koşul:** `player.getMainHandStack()` veya `player.getOffHandStack()` = LavaCrystal.
- **Koruma mekanizması:** `ServerTickEvents.END_SERVER_TICK` → elde tutanlara 40 tick `FIRE_RESISTANCE` uygulanır + ateş söndürülür.
- **Mixin gerektirmez** — tick event yeterli.
- **Crafting:** 4 Blaze Rod + 4 Magma Block + 1 Amethyst Shard (merkez).
- **Tooltip:** "Elinde tut — ateş ve lavdan zarar görmezsin!"

### P-13: Kara Delik 15 Kullanım

**Kategori:** Fix
**Boyut:** S
**Durum:** Bekliyor

**Açıklama:**
Kara Delik mevcut davranışı güncellenir: maxCount(16) yerine maxCount(1) + maxDamage(15). Her kullanımda 1 dayanıklılık düşer, 15 kullanımdan sonra yok olur.

**Değişiklikler:**
- `ModItems.java`: `maxCount(16)` → `maxCount(1).maxDamage(15)`.
- `BlackHoleItem.java`: `stack.decrement(1)` → `stack.damage(1, user, slot)`.

---

## Açık Sorular ve Tartışma

### Denge
- Lazer Kılıcı 20 sn cooldown ile dengeli görünüyor. İlk testlerde gözden geçirilmeli.
- Herobrine Çağırıcı çok pahalı (Nether Star). Bilerek: bu bir "boss çağırma" eşyası.

### Advancement Genişletmesi
Yeni içerikle orantılı olması için planlanan başarımlar (P-16):
- Lazer Kılıcı edin
- Herobrine çağır (ve kurtar — yani öldür)
- Lav Kristali elde et

### Test Stratejisi (P-15)
JUnit 5 altyapısı zaten `build.gradle`'da mevcut. Oluşturulacak testler:
- Resource JSON dosyaları geçerli mi?
- Her `items/*.json` için `models/item/*.json` var mı?
- Lang dosyaları boş değil mi?
