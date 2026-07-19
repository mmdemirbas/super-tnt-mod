# Çocuklar için Bedrock Add-On Paketleri

Tarih: 2026-07-19 · Cihaz: SM-X520 · Minecraft Bedrock 1.26.33.1

Tabletteki `/sdcard/Download/` altında üç dosya var. Her biri **bir kez
dokunup** Minecraft'ı seçince içindeki tüm paketleri birden içe aktarır.

| Dosya | İçerik | Boyut |
|---|---|---|
| `MorphX-Mutant-Merged.mcaddon` | Morph + mutant birleştirmesi (4 pack) | 15.6 MB |
| `Oglan-Paketi.mcaddon` | 6 add-on (12 pack) | 47.0 MB |
| `Kiz-Paketi.mcaddon` | 4 add-on (7 pack) | 44.7 MB |

**Önemli:** içe aktarmak paketleri *kurar*, otomatik açmaz. Her dünyada
hangisini istiyorsan onu ayrı ayrı açarsın. Hepsini aynı anda açmak
zorunda değilsin — hatta açma.

---

## Oğlan paketi

Seçim ölçütü: mevcut MorphX kurulumuyla çakışmamak. MorphX `player.json`'ı
override ediyor; aynı dosyayı override eden her add-on onunla kavga eder.
İndirdiğim altı paketin **hiçbiri** `player.json`'a dokunmuyor — bunu
sayfa etiketine bakarak değil, dosyaların içini açıp `minecraft:player`
tanımı arayarak doğruladım.

| Add-On | Ne yapar | min_engine |
|---|---|---|
| **Dinosaurs v1.4** | Evcilleştirilebilir dinozorlar | 1.21.0 |
| **The Last Dragon v1.3** | Binilebilir, evcilleştirilebilir ejderha | 1.21.0 |
| **Fire & Steel: Dragons** | Yumurtadan ejderha yetiştirme döngüsü | 1.20.50 |
| **Nico's Magic Spells v1.3.3** | Büyü asaları (kuvars/ametist/altın/elmas) | 1.26.20 |
| **Mutant Creatures Gods** | Mevcut mutant paketinin tanrı-seviye boss'ları | 1.17.0 |
| **Basic Jetpack 3.0** | Bakış yönüne göre uçuş, başarım dostu | 1.21.80 |

**Öneri:** Mutant Creatures Gods'ı önce dene — zaten kurulu mutant paketinin
doğrudan devamı, tematik olarak en oturaklı olan o. Ejderha paketlerini
(The Last Dragon / Fire & Steel) aynı dünyada birlikte açma; ikisi de ejderha
ekliyor, içerik olarak birbirini gölgeler.

**Elenenler ve sebebi:** "Even More Super Powers" en bariz süper-güç sonucuydu
ama sayfasında açıkça *"This addon modifies player.json"* yazıyor — MorphX ile
doğrudan çakışırdı. Araç/makine kategorisinin tamamı elendi: MCPEDL'de bu alan
2022-2023'te kalmış, 26.x sürümüne uygun temiz bir aday yok.

---

## Kız paketi

Bluey'nin havası hedeflendi: sevimli, sakin, yaratıcı oyun. Dövüş odaklı
olanlar bilerek elendi.

| Add-On | Ne yapar | min_engine |
|---|---|---|
| **Cute Plushies v4.2** | 90 toplanabilir peluş oyuncak, dekorasyon | 1.26.0 |
| **FURNICRAFT v27.1** | 500+ döndürülebilir 3B mobilya (mutfak, banyo, koltuk, TV) | 1.21.0 |
| **Better Cats v4.1.1** | Vanilla kedilere gerçek cins modelleri | 1.26.10 |
| **Cute Mob Models v1.0.21** | Tüm vanilla mobları chibi/sevimli modellere çevirir | 1.20.0 |

**En değerli seçim muhtemelen Cute Mob Models:** creeper, zombi, iskelet dahil
bütün korkutucu mobları sevimli hale getiriyor. Küçük bir çocuk için
Minecraft'ı ürkütücü olmaktan çıkaran tek hamle bu olabilir.

**Bilinen çakışma (engel değil):** Better Cats ve Cute Mob Models'ın ikisi de
`minecraft:cat` ve `minecraft:ocelot` tanımlıyor. Kaynak paket sıralamasında
üstte olan kazanır. Gerçekçi kedi cinsleri isteniyorsa Better Cats üstte
olsun; her şey chibi olsun isteniyorsa altta. Sadece kedi ve ocelot etkilenir,
diğer 88 mob Cute Mob Models'tan gelir.

**Elenen:** *More Cake* listedeydi ama açıp baktığımda `player.json`'ı
override ettiğini gördüm (`More Cake BP/entities/player.json`). Listedeki en
düşük değerli paketti ve tek `player.json` riskini taşıyan oydu; iki çocuk
aynı tableti kullandığı için gizli çakışma bırakmaya değmezdi. Çıkardım.

*Chikawa Pets* görsel olarak Bluey'ye en yakın olandı ama açıklaması tamamen
dövüş odaklı (evcil hayvanlar kılıç/yay kuşanıyor, "fierce fighters"). Sevimli
görünüp otomatik dövüşen evcil hayvanlar sorun değilse tekrar bakılabilir.

**Ücretsizde Bluey yok.** Tek meşru Bluey içeriği resmî Marketplace DLC'si
(Jigarbov/BBC, Şubat 2026) ve ücretli. Planet Minecraft'taki gayriresmî
Bluey add-on'u Ekim 2025'te kalmış, 1.26'dan önce.

---

## Doğrulama (üç dosyanın hepsi için)

Sayfa etiketlerine güvenilmedi; her şey indirilen dosyanın içinden doğrulandı:

- **Çalıştırılabilir içerik:** `.exe/.apk/.msi/.sh/.bat/.dll/.jar` → **0**
- **min_engine_version:** hepsi ≤ 1.26.33 → cihazla uyumlu
- **UUID çakışması:** demet içinde ve kurulu MorphX/Mutant paketleriyle → **yok**
- **player.json override:** yeni iki demette → **yok**
- **Dosya bütünlüğü:** Mac ve tablet bayt sayıları birebir aynı

Bulunup düzeltilen bir kusur: Basic Jetpack'in kaynak paketinde BP'nin header
UUID'si ile RP'nin module UUID'si aynı değerdeydi. Farklı isim alanları olduğu
için muhtemelen zararsızdı, ama belirsizliği bırakmamak için RP module UUID'si
yenilendi.

Fire & Steel Dragons paketinin içinde `node_modules/` var — sadece
`@minecraft/server` TypeScript tip tanımları (506 KB), çalıştırılabilir kod
değil. Geliştirici artığı, güvenlik sorunu değil.

---

## Kurulum sırası önerisi

1. Önce **tek bir add-on** aç, dünyaya gir, çalıştığını gör.
2. Sonra bir tane daha ekle.

Hepsini birden açıp bir sorun çıkarsa hangisinin sebep olduğunu bulmak zor.
Özellikle script kullanan paketlerde (FURNICRAFT, Nico's, Fire & Steel,
Jetpack) Android'de asıl risk çakışma değil **performans** — çok sayıda script
paketi aynı anda açıksa tablet yavaşlayabilir.
