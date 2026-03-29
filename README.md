# 🧨 Super TNT Mod

Fabric 1.21.1 | 25 Blok + 5 Item

## TNT Türleri

| TNT | Malzeme | Özellik |
|-----|---------|---------|
| 💎 Elmas TNT | Elmas | 2.5x büyük patlama |
| 🥇 Altın TNT | Altın Külçe | 5 küçük patlama dalgası |
| 🪨 Bedrock TNT | Obsidian | Bedrock dahil her şeyi kırar (25 blok yarıçap) |
| 💚 Zümrüt TNT | Zümrüt | Patlama + emerald, elmas, altın, lapis yağmuru |
| ⚡ Yıldırım TNT | Şimşek Çubuğu | 20 yıldırım fırtınası |
| ☢ Nükleer TNT | Nether Yıldızı | Dev patlama + radyasyon zehri |
| ❄ Dondurucu TNT | Buzlu Buz | Suları dondurur, kar yağdırır |
| 🌲 Odun TNT | Odun | Ağaçları yok eder, 1 dakika sonra geri gelir |
| 🎮 Komut Bloğu TNT | Komut Bloğu | Seçilen blok türünü ayarlanan yarıçapta yok eder |
| 🚪 TNT Kapı | Demir Kapı | İlk kullanan sahip olur, başkaları patlar |
| 🔒 Şifreli TNT Sandık | Sandık | Şifreli sandık, başkaları 15 kalp hasar alır |
| 🚶 Yürüyen TNT | TNT | Göz teması kuranları takip eder, temas edince patlar |
| 🧊 Canavar Dondurucu TNT | Kar Topu | Tüm düşman canavarları 10 dakika dondurur |
| 💧 Su TNT | Su Kovası | Ateşleri söndürür, su dalgasıyla iter |
| 🌈 Gökkuşağı TNT | Yün | Blokları renkli yüne dönüştürür (30 blok yarıçap) |
| 🧱 Lego TNT | Tuğla | Çıkıntılı lego tuğlalarından yapılar inşa eder |
| 🍝 Makarna TNT | Buğday | Blokları yenilebilen bloklara dönüştürür |
| 🍬 Şeker TNT | Şeker Kamışı | Çikolata ve şeker dünyası yaratır + hız güçlendirmesi |
| 🔽 Küçülten TNT | Tavşan Derisi | Yakındaki canlıları minik boyuta küçültür |
| 🔼 Büyüten TNT | Kemik Bloğu | Yakındaki canlıları dev boyuta büyütür |
| ✨ Temizleyici TNT | Süt Kovası | Tüm efektleri ve boyut değişikliklerini temizler |
| 🎂 Pasta TNT | Pasta | Lezzetli bir pasta... mı acaba? (Sağ tık veya kırınca patlar!) |

## Item'ler

| Item | Özellik |
|------|---------|
| 🥏 TNT Frizbi | Atılabilir TNT - + şeklinde yok eder, size geri döner |
| 🔫 Portal Silahı | Portal atar - önce pembe, sonra yeşil. İçinden geçerek ışınlan! |
| ⛏ Tünel Kazma Aleti | 1/12 ölçeğe küçülünce otomatik verilir, bloklara sağ tıklayarak minik delikler açar |
| 🟩 Yeşil Lego Tuğla | Portal Silahı crafting malzemesi |
| 🟪 Pembe Lego Tuğla | Portal Silahı crafting malzemesi |

## Dekoratif Bloklar

| Blok | Özellik |
|------|---------|
| 🧱 Lego Tuğla | Lego TNT tarafından oluşturulan dekoratif blok (16 renk) |
| 🌀 Portal | Portal Silahı tarafından oluşturulan ışınlanma portalı |
| 🕳 Tünel Blok | Tünel Kazma Aleti ile oluşturulan kısmen kazılmış blok |

## Portal Silahı Kullanımı

1. **Sağ tıkla** → pembe portal ateşlenir
2. **Tekrar sağ tıkla** → yeşil portal ateşlenir
3. Portalların birine gir → diğerine ışınlan!

Crafting: 8x Yeşil Lego Tuğla + 1x Pembe Lego Tuğla (ortada)

## Tünel Kazma Aleti Kullanımı

1. Küçülten TNT ile 1/12 ölçeğe küçül
2. Alet otomatik olarak envantere verilir
3. **Bloklara sağ tıkla** → 4×4×4 sub-voxel grid üzerinde minik delikler aç
4. Bir bloğun tüm sub-voxel'leri kaldırılırsa blok tamamen yok olur

## Komut Bloğu TNT Kullanımı

1. Komut TNT'yi yerleştir
2. **Elinde bir blok tut + sağ tıkla** → hedef blok ayarlanır
3. **Eğil (Shift) + boş elle sağ tıkla** → yarıçap değiştirir (10 → 20 → 30 → 50)
4. **Boş elle sağ tıkla** → mevcut ayarları gösterir
5. **Çakmakla sağ tıkla** → ateşler

## Crafting Tarifi

Çoğu TNT aynı kalıbı kullanır:

```
M M M
M T M   → 1 Özel TNT
M M M
```
`M` = İlgili malzeme, `T` = Normal TNT

İstisnalar: TNT Frizbi, TNT Kapı, Şifreli TNT Sandık ve Portal Silahı kendine özgü tariflere sahiptir.

## Ateşleme

- Çakmak Taşı / Ateş Topu ile sağ tık
- Redstone sinyali
- Patlama zinciri (başka TNT patlarsa)

## Başlatma

```bash
./gradlew runClient   # Test ortamı (geliştirme)
./gradlew build       # build/libs/*.jar → .minecraft/mods/
```
