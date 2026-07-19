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
| **Mutant Creatures - Güçlü Bosslar** | Mevcut mutant paketinin daha güçlü boss sürümleri | 1.17.0 |
| **Basic Jetpack 3.0** | Bakış yönüne göre uçuş, başarım dostu | 1.21.80 |

**Paket adı değiştirildi.** Bu paketin özgün adı "Mutant Creatures **Gods**"
idi ve oyun içi paket listesinde öyle görünüyordu. İçeriği açıp inceledim:
23 entity var, hepsi güçlendirilmiş mutant mob ve patlama/lazer/diken gibi
efekt varlıkları. Tapınma, sunak, put, ilah figürü **yok**; tek "god"
eşleşmesi geliştiricinin unuttuğu bir şaka satırı (`ohmygod` = "We're up for
round 2, Matt!"). Kelime oyun jargonunda "boss seviyesi" anlamında
kullanılmış. Yine de görünen ad "Mutant Creatures - Güçlü Bosslar" olarak
değiştirildi; içerik aynı, sadece etiket. Paketi tamamen çıkarmak istersen
tek satırlık iş.

**Öneri:** Güçlü Bosslar'ı önce dene — zaten kurulu mutant paketinin
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
| **Craftopia Furniture WE 2.2** | 2000+ dekorasyon ve mobilya bloğu | 1.20.30 |
| **Better Cats v4.1.1** | Vanilla kedilere gerçek cins modelleri | 1.26.10 |
| **Cute Mob Models v1.0.21** | Tüm vanilla mobları chibi/sevimli modellere çevirir | 1.20.0 |

### FURNICRAFT neden çıkarıldı

İlk seçimde FURNICRAFT vardı ("500+ mobilya"). Dosyayı açıp içine bakınca
mobilyanın yanında **51 kostüm** olduğu görüldü: FNAF karakterleri (Freddy,
Bonnie, Chica, Foxy, Fredbear), Herobrine, `demon_wings` (şeytan kanatları),
Hatsune Miku, Megumin, Chainsaw Man. Korku ve anime içeriği, "sevimli mobilya"
beklentisiyle uyuşmuyor.

Yerine Craftopia Furniture kondu: aynı işi daha geniş yapıyor (2000+ blok),
26.30 etiketli ve tarandığında kostüm/korku/anime içeriği çıkmadı. (Taramada
25 "şüpheli" eşleşme çıktı ama hepsi yanlış alarmdı — "s**witch**" kelimesi
Nintendo Switch mobilyasından geliyordu.)

Ders: paket açıklaması ve kategori adı içeriği anlatmıyor. Dosyanın içine
bakmak gerekiyor.

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


---

## Dosyaya dokununca "Play Store'da ara" çıkıyorsa

**Sebep bulundu ve ölçüldü** (2026-07-19, SM-X520, MC 1.26.33.1).

Minecraft'ın `.mcaddon` intent filtresi **MIME tipi bekliyor**. Aynı dosya için
üç deneme:

| Gönderilen intent | Minecraft çıkıyor mu? |
|---|---|
| MIME'siz `file://` | ❌ Docs + Arama |
| **MIME'li `file://`** (`application/octet-stream`) | ✅ **Minecraft** |
| `content://media/...` | ❌ Mesajlar |

Samsung "Dosyalarım" MIME'siz ya da `content://` gönderdiği için eşleşme
olmuyor; Android da "bu dosyayı açacak uygulama yok" deyip Play Store'a
yönlendiriyor. Minecraft'ta bir sorun yok — dosyalar MediaStore'da kayıtlı ve
filtre yerinde duruyor, sadece iki taraf anlaşamıyor.

### Çözüm

```bash
./bedrock/install.sh bedrock/out/SuperTNT.mcaddon
```

Script dosyayı gönderir ve boyutunu doğrular. **Import'u sen yaparsın** —
tam otomatik import denendi ama güvenilir değil (açık bileşenle Minecraft
dosyayı işlemiyor, implicit intent'te uygulama seçici çıkıyor ve onu adb ile
geçmek kırılgan). Elle import her zaman çalışıyor:

Dosyalarım → Download → dosyaya **uzun bas** → **Şununla aç** → **Minecraft**
(düz çim-bloğu ikonu, "Education" olan değil) → **Yalnızca bir defa**.

### Elle yol (script olmadan)

Dosyalarım → Download → dosyaya **uzun bas** → **Şununla aç** → Minecraft.
"Şununla aç" listesi MIME'den bağımsız çalıştığı için Minecraft görünür.
