# Super TNT Mod → Bedrock Add-On: Port Fizibilitesi

Tarih: 2026-07-19 · Kaynak: bu repo (Fabric, MC Java 1.21.11)
Hedef: Minecraft Bedrock 1.26.x, Android tablet

---

## Kısa cevap

**Kod portu diye bir şey yok — yeniden yazım var.** Java sınıfları Bedrock'a
taşınmaz; Bedrock Add-On'u JSON + JavaScript'tir. Ama *mekaniklerin* büyük
çoğunluğu taşınabilir: mevcut API kullanımının **~%85'inin** birebir Bedrock
Script API karşılığı var.

Taşınamayan kısım küçük ama keskin: oyuncu ölçeklendirme ve blok-başına
kalıcı veri.

---

## Envanter (gerçek tarama)

Repo bugün: **84 entity sınıfı** (8149 satır), **95 blok sınıfı** (4134 satır).
CLAUDE.md'de yazan "22 TNT" güncelliğini yitirmiş.

### API kullanım sıklığı ve Bedrock karşılığı

| Java çağrısı | Adet | Bedrock Script API karşılığı | Durum |
|---|---:|---|---|
| `playSound` | 166 | `dimension.playSound` / `player.playSound` | ✅ birebir |
| `spawnEntity` | 115 | `dimension.spawnEntity` | ✅ birebir |
| `discard` | 86 | `entity.remove()` | ✅ birebir |
| `createExplosion` | 69 | `dimension.createExplosion` | ✅ birebir |
| `getBlockState` | 58 | `dimension.getBlock().permutation` | ✅ birebir |
| `addStatusEffect` | 46 | `entity.addEffect` | ✅ birebir |
| `setBlockState` | 44 | `block.setPermutation` / `setBlockType` | ✅ birebir |
| `setVelocity` / `addVelocity` | 33 | `entity.applyImpulse` / `applyKnockback` | ✅ yakın |
| `damage` | 19 | `entity.applyDamage` | ✅ birebir |
| `getAttributeInstance` | 13 | **yok** (aşağıya bak) | ⛔ engel |
| `breakBlock` | 9 | `setBlockType(air)` + `spawnItem` | ⚠️ ganimet elle |
| `kill` | 8 | `entity.kill()` | ✅ birebir |
| `setWeather` | 5 | `dimension.setWeather` | ✅ birebir |
| `teleport` | 4 | `entity.teleport` | ✅ birebir |
| `dropStack` | 2 | `dimension.spawnItem` | ✅ birebir |

### Yapısal desenler

| Desen | Dosya | Bedrock'ta | Durum |
|---|---:|---|---|
| `ScreenHandler` (özel GUI) | 6 | `@minecraft/server-ui` ActionForm/ModalForm | ✅ **daha kolay** |
| `ServerTickEvents` | 3 | `system.runInterval` | ✅ birebir |
| `ALLOW_CHAT_MESSAGE` | 1 | `world.beforeEvents.chatSend` | ✅ birebir |
| `PathAwareEntity` + `Goal` | 5 | JSON `minecraft:behavior.*` bileşenleri | ✅ yeniden yazım |
| `DataTracker` | 11 | entity dynamic properties / entity properties | ✅ yakın |
| `BlockEntity` + `NbtCompound` | 12 | **blok başına keyfi veri yok** | ⛔ engel |
| `EntityAttributeModifier` | 6 | oyuncuda ölçek değiştirilemez | ⛔ engel |

---

## İki gerçek engel

### E1 — Shrink / Growth TNT (oyuncu ölçeği)

Java'da `SCALE` attribute'una modifier takıyoruz. Bedrock'ta oyuncunun
ölçeği script'ten değiştirilemez. `minecraft:scale` bir bileşen ve yalnızca
entity tanımında, component_group üzerinden değişir — oyuncu entity'sinde
işe yaramaz.

**Kısmi çözüm:** moblar üzerinde çalışır (component_group + event, tıpkı
MorphX'in morph mekaniği gibi). Yani "etraftaki mobları küçült" portlanabilir,
"oyuncuyu küçült" portlanamaz.

### E2 — Blok başına kalıcı veri

TNT Door ve Encrypted TNT Chest `ownerUuid` ve şifreyi blok NBT'sinde
tutuyor. Bedrock'ta bloklarda keyfi NBT yok.

**Çözüm var:** veriyi `world.setDynamicProperty` altında koordinat anahtarlı
bir JSON haritasında tut (`"door:12,64,-30" -> {owner, pw}`). Çalışır ama
elle temizlik gerektirir — blok kırılınca kaydı silmezsen sızıntı olur.
Bunu tasarımda baştan çözmek lazım, sonradan yamamak değil.

---

## Portlanabilirlik dökümü

| Kategori | Örnekler | Değerlendirme |
|---|---|---|
| **Doğrudan portlanır** | Diamond, Gold, Emerald, Bedrock, Nuclear, Lightning, Freeze, Water, Rainbow, Lego, Makarna, Şeker, Cleanse, Nether, End, Gravity | Hepsi blok değiştirme + patlama + parçacık + ses. En kalabalık grup. |
| **Yeniden yazımla portlanır** | Walking TNT (AI → JSON behavior), Encrypted Chest (GUI → ModalForm, chat → chatSend), Command TNT, Fake TNT | Mekanik korunur, uygulama tamamen değişir. Bedrock GUI'si Java'dan **kolay**. |
| **Kısmen portlanır** | Shrink, Growth | Mob üzerinde evet, oyuncu üzerinde hayır (E1). |
| **Tasarım değişikliği gerekir** | TNT Door, Encrypted Chest'in sahiplik kaydı | E2'deki dynamic property şeması ile. |
| **Bedava gelir** | Çoklu tick işleme (Bedrock, Java'dan iyi: `system.runJob` var) | Lag yayma zaten dilde. |

---

## Gerçek maliyet: ateşleme mekaniği

Görünmeyen ama en can sıkıcı kısım: Bedrock'ta "vanilla TNT gibi ateşlenen
özel blok" hazır gelmiyor. Java'da `CustomTntBlock` bunu miras alıyor.
Bedrock'ta her özel TNT için elle kurman gerekenler:

1. `minecraft:block` JSON tanımı
2. Çakmakla etkileşim → `minecraft:on_interact` + item kontrolü
3. Redstone ile ateşleme → script tarafında redstone olayı dinleme
4. Ateş/patlama zinciri ile ateşleme → komşuluk taraması
5. Yanan TNT entity'si (fuse animasyonu, sıçrama fiziği) → ayrı entity + geometry
6. Blok kırılınca item düşürme, craft tarifi, dil dosyası

Yani **her TNT ~6 dosya**. 22 TNT için ~130 dosya. Bu yüzden hepsini birden
portlamak yanlış hamle.

---

## Önerilen yol: 6 TNT'lik dikey dilim

Hepsini portlamak yerine **tek bir TNT'yi uçtan uca** çalıştır, sonra
şablonu çoğalt. Sıralama:

1. **Diamond TNT** — en basit: büyük patlama + elmas blok saçılımı.
   Tüm iskeleti (blok + entity + ateşleme + tarif + dil) burada kur.
2. **Freeze TNT** — blok değiştirme deseni (taş→buz).
3. **Lightning TNT** — entity spawn + hava durumu.
4. **Rainbow TNT** — çok-tick işleme, `system.runJob`.
5. **Lego TNT** — 16 renk varyantı, blok permutation'ı.
6. **Walking TNT** — AI, JSON behavior bileşenleriyle. En eğlenceli, en öğretici.

Bu altısı iskeletin tamamını kanıtlar. Kalan 16'sı 1-2 saatlik kopyala-uyarla
işine döner.

---

## Geliştirme döngüsü kısıtı (ölçüldü)

| Yol | Yazılabilir? |
|---|---|
| `/sdcard/Download/` | ✅ |
| `/sdcard/Android/data/` (üst seviye) | ✅ |
| `/sdcard/Android/data/com.mojang.minecraftpe/**` | ⛔ **salt okunur** |

Sonuç: **paketi doğrudan oyunun klasörüne yazamayız.** Her deneme turu
`.mcaddon` → `adb push` → **kullanıcı bir kez dokunur** → import.

Bunu hafifletmenin yolları:
- **Tur başına çok değişiklik topla.** Tek özellik için tur harcama.
- **Offline doğrulamayı sertleştir.** MorphX birleştirmesinde yaptığımız gibi:
  JSON parse, JS syntax, geometry/texture referans çözümleme. Bu üçü oyunu
  hiç açmadan hataların çoğunu yakaladı (0/28 çözülemeyen referans).
- **Oyun içi hata günlüğü.** Ayarlar → Creator → "Content Log File" açılırsa
  script hataları dosyaya yazılır. Ama o dosya da dahili depolamada; ekran
  görüntüsüyle okumak gerekir.
- Bir dahaki sefere: tablet yerine **PC Bedrock** (Windows) geliştirme için
  çok daha rahat — `development_behavior_packs` klasörü doğrudan yazılabilir
  ve oyun yeniden başlatmadan paketi tazeler. Elde Windows makine varsa
  gerçek geliştirme orada, tablete sadece bitmiş sürüm gider.

---

## Özet karar

- Portlanabilirlik: mekaniklerin **~%85'i**, kodun **%0'ı**.
- Engeller: oyuncu ölçeği (çözümsüz), blok-başına veri (çözülebilir, tasarım işi).
- Doğru başlangıç: 6 TNT'lik dikey dilim, Diamond TNT ile.
- En büyük sürtünme kodda değil, **geliştirme döngüsünde** (tur başına bir dokunuş).
