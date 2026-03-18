# 🧨 Super TNT Mod

Fabric 1.21.1 | 22 Özel Blok + 1 Item

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
| 🎂 Pasta TNT | Pasta | Lezzetli bir pasta... mı acaba? |

## Item'ler

| Item | Özellik |
|------|---------|
| 🥏 TNT Frizbi | Atılabilir TNT - + şeklinde yok eder, size geri döner |

## Komut Bloğu TNT Kullanımı

1. Komut TNT'yi yerleştir
2. **Elinde bir blok tut + sağ tıkla** → hedef blok ayarlanır
3. **Eğil (Shift) + boş elle sağ tıkla** → yarıçap değiştirir (10 → 20 → 30 → 50)
4. **Boş elle sağ tıkla** → mevcut ayarları gösterir
5. **Çakmakla sağ tıkla** → ateşler

## Crafting Tarifi

Tüm TNT'ler aynı kalıbı kullanır:

```
M M M
M T M   → 1 Özel TNT
M M M
```
`M` = İlgili malzeme, `T` = Normal TNT

## Ateşleme

- Çakmak Taşı / Ateş Topu ile sağ tık
- Redstone sinyali
- Patlama zinciri (başka TNT patlarsa)

## Başlatma

```bash
./gradlew runClient   # Test ortamı (geliştirme)
./gradlew build       # build/libs/*.jar → .minecraft/mods/
```
