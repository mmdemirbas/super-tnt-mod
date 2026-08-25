# Super TNT Mod — Bedrock Add-On

Java (Fabric) modunun Bedrock sürümü. Tabletlerde çalışır.

```bash
python3 bedrock/build.py     # -> bedrock/out/SuperTNT.mcaddon
```

Üretilen `super_tnt_BP/` ve `super_tnt_RP/` klasörleri **çıktıdır**, elle
düzenlenmez — `build.py` her çalıştığında sıfırdan yazılır. Değişiklik
`build.py` içindeki `TNTS` listesine yapılır.

## İçindeki 12 TNT

| Blok | Ne yapar | Tarif malzemesi |
|---|---|---|
| Elmas TNT | 10 güç patlama | elmas |
| Zıplatan TNT | 12 blok yarıçapındaki her şeyi fırlatır | slime topu |
| Zeynep TNT | 50 boş kağıt saçar | kağıt |
| Çoklu Zeynep TNT | "Zeynep" isimli 15 köylü | zümrüt |
| Abi TNT | "Abi" isimli 12 köylü | zümrüt |
| Anne TNT | "Anne" isimli 12 köylü | zümrüt |
| Baba TNT | "Baba" isimli 12 köylü | zümrüt |
| Bebek TNT | "Bebek" isimli 15 yavru köylü | zümrüt |
| Bulut TNT | Yağmur başlatır | beyaz yün |
| Ay TNT | Gündüzü geceye çevirir | glowstone tozu |
| Kalp TNT | 20 blokta emme + yenilenme | altın külçe |
| Buz TNT | 14 blok yarıçapını buz/karla kaplar | packed ice |

Tarif deseni Java'dakiyle aynı: 8 malzeme + ortada vanilla TNT.

Davranış sayıları (yarıçap, adet, süre) `src/main/java/com/supertntmod/entity/*TntEntity.java`
dosyalarından okundu, uydurulmadı.

## Nasıl ateşlenir

- **Çakmakla sağ tık**
- **Redstone** — kaldıraç, buton, basınç plakası, redstone tozu
- **Zincirleme** — yakındaki bir patlama onu da ateşler

Ateşlenince blok kaybolur, yerine fiziksel bir TNT varlığı doğar: yukarı
sıçrar, düşer, yuvarlanır, yanıp söner. 4 saniye sonra patlar.

## Nasıl çalışır

**Fırlayan varlık.** Her TNT'nin `stnt:<ad>_primed` adında bir varlığı var.
Ortak `geometry.stnt_tnt` (16³ küp) + TNT'ye özel 64×32 doku. Doku, blok
yüzlerinden Minecraft'ın kutu-UV düzenine göre `build.py` içinde birleştiriliyor
(PIL varsa; yoksa düz renge düşer). Yanıp sönme efekti render controller'da
Molang ile: `math.mod(math.floor(query.life_time * 10), 2)`.

**Redstone.** Bedrock'ta özel bloğa redstone dinletmenin doğrudan yolu yok.
Oyuncunun yerleştirdiği TNT'ler bir listede tutuluyor ve 10 tick'te bir
`block.getRedstonePower()` ile yoklanıyor. Liste dünya dinamik özelliğinde
saklandığı için dünya kapanıp açılınca kaybolmuyor. 400 blokla sınırlı —
maliyeti sabit tutmak için.

*Bilinçli tercih:* özel blok bileşeni (`minecraft:custom_components`) daha
zarif olurdu ama blok JSON'unda bildirilip script tarafında kayıt başarısız
olursa blok tamamen yüklenmez. Test edemediğim bir API için bu riski almadım.

**Zincirleme.** İki yoldan: (a) patlama anında 6 blok yarıçapı taranıp bulunan
TNT'ler 2–10 tick gecikmeyle ateşleniyor (kademeli, hepsi aynı anda değil),
(b) `world.beforeEvents.explosion` ile başka bir patlamanın yok edeceği
TNT'ler listeden çıkarılıp ateşleniyor — yani vanilla TNT veya creeper da
zinciri başlatabiliyor.

**Dönüşüm (morph).** Oyuncu 87 mob'dan birinin — vanilla Bedrock'un 83'ü artı
paketin kendi dört boss'u — **görünümüne** girer: `player.json`'a mob başına `geometry`/`texture`/`material`
kaydı ve `st:morph` özelliğine bağlı render controller eklenir. Vanilla dosyalar
pakete konmaz — Minecraft kendi kaynağından verir, biz yalnız **adlarını**
yazarız. Ad bir harf yanlışsa model sessizce görünmez olur; bu yüzden tablo elle
yazılmıyor:

```bash
python3 bedrock/tools/gen_morphs.py --write   # MORPHS'u bedrock-samples'tan üret
```

Betik Mojang/bedrock-samples deposundaki `resource_pack/entity/*.entity.json`
dosyalarını okur; seçilen anahtar o mob'un tanımında yoksa durur ve mevcut
anahtarları yazar. Çarpışma kutusu morph'tan değil `st:size`'dan gelir, yani
küçülme/büyütme ile çakışmaz.

Üretilen tablo `MORPHS_VANILLA`; paketin kendi mob'ları (Dev Creeper, Dev Zombi,
Mutant Warden, Ender Send) `MORPHS_OWN` içinde **elle** yazılır — üreteç onlara
dokunmaz. Kendi mob'larımızın modeli/dokusu vanilla'dan değil paketten gelir;
nereden geldikleri `bedrock/custom/KAYNAKLAR.md` içinde yazılı. Yetenekler `MORPH_ABIL`'e `st:morph` sayısıyla değil mob tanımından
bağlanır; sayı elle yazılsa liste büyüdükçe kayar ve yanlış moba yanlış güç
bağlanırdı. Dönüşünce eylem çubuğunda yazan ipucu da aynı tablodan üretilir.

## Bilinen farklar (Java sürümüne göre)

**F1, F2, F3 — çözüldü (v1.1.0).** Fırlayan TNT varlığı, redstone ateşleme
ve zincirleme patlama artık var. Ayrıntı aşağıda "Nasıl çalışır".

**F4 — Dokular yeniden renklendirildi.** Elmas ve Zıplatan TNT kendi PNG'lerini
Java projesinden alıyor. Diğer 10'u Java'da vanilla beton dokusu kullanıyordu;
Bedrock'ta vanilla doku yol adları farklı (`light_gray` → `concrete_silver`
gibi) ve kırık doku riski var. Bunun yerine gerçek TNT dokusunun parlaklığı
korunup rengi değiştiriliyor — "TNT" yazısı ve fitil duruyor.

*İlk denemede düz renkli kareler üretilmişti; envanterde 10 pastel kare
birbirinden ayırt edilemiyordu. Görsel denetimde yakalandı.*

**F5 — Aile TNT'lerinin tonları ayrıştırıldı.** Java'da abi/anne/baba/bebek
dördü de aynı pembe; envanterde ayırt edilemiyor. Aynı sıcak paletten farklı
tonlar verildi. Bilinçli sapma.

**F6 — Buz TNT'nin dondurması yaklaşık.** Blok dönüşümü (su→buz, lav→taş,
üste kar katmanı), 30 sn kar yağışı ve "patlatan hariç herkesi dondur"
mekaniği var. Ama Bedrock'ta gerçek donma efektini script'ten vermenin yolu
yok; en yakın karşılık olarak yavaşlık + zayıflık + kazma yorgunluğu
veriliyor.

## Tooltip'ler

Java'da her TNT'nin bir açıklaması var. Bedrock'ta blok tooltip'i yok;
karşılığı olarak TNT eline alındığında açıklama **eylem çubuğunda** görünür.
Metin çeviri anahtarıyla basılır (`stnt.tip.<ad>`), yani oyuncunun dilinde
çıkar — İngilizce oynayan İngilizce görür.

## Denetim notu

v1.2.0 bir gözden geçirmenin sonucu. Java projesinin 57 düzeltme commit'i
tarandı ve aynı hatalara düşülüp düşülmediği kontrol edildi. Bulunanlar:

- **Kalp TNT tüm canlılara buff veriyordu** — zombi ve iskeletleri de
  güçlendiriyordu. Java'da `88b9de1` bu hatayı Harf TNT'de düzeltmiş
  (*"kid için tehlikeli hale getiriyordu"*). Artık yalnızca oyunculara.
  Not: `KalpTntEntity.java` hâlâ `LivingEntity` kullanıyor — Java tarafında
  bu düzeltme yapılmamış görünüyor.
- **Kalp TNT efekt temizlemeyi atlıyordu** — Java sürümü önce tüm efektleri
  siliyor, sonra buff veriyor. Eklendi.
- **Buz TNT dondurma ve kar yağışını hiç yapmıyordu** — tooltip'te yazıyordu
  ama kod yapmıyordu. Java'da `Su TNT` düzeltmesi tam olarak bu sınıftan bir
  hataydı (*"tooltip böyle diyordu ama implementasyon başka şey yapıyordu"*).
- **Türkçe karakterler kırpılmıştı** — "Zıplatan"→"Ziplatan", "güç"→"guc".
- **Tooltip metinleri uydurulmuştu** — Java'daki gerçek metinlerle değiştirildi.
- **Tooltip anahtarları ölüydü** — üretiliyor ama hiçbir yerde kullanılmıyordu.

## Sonraki adımlar

Öncelik sırasıyla:

1. **Daha fazla TNT.** Java tarafında 70 sınıf var. Şablon oturduğu için her
   yeni TNT `TNTS` listesine bir kayıt + davranış dalı demek.
2. **Walking TNT.** Java'da AI'lı; Bedrock'ta JSON davranış bileşenleriyle
   yazılır. En eğlencelisi, en öğreticisi.

## Geliştirme döngüsü

Tabletin Minecraft klasörü salt okunur (bkz. `docs/bedrock-port-fizibilite.md`),
yani her deneme:

```bash
python3 bedrock/build.py
./bedrock/install.sh bedrock/out/SuperTNT.mcaddon
```

`install.sh` dosyayı gönderir, boyutunu doğrular ve import intent'ini
**MIME tipiyle** yollar. MIME olmadan Minecraft eşleşmiyor ve Android
"Play Store'da ara" diyor — ayrıntı `docs/cocuk-paketleri.md`'de.
Tabletin ekranı açık ve kilidi açık olmalı.

Tur başına bir dokunuş gerektiği için değişiklikleri topla, tek seferde gönder.

## Göndermeden önce

```bash
bash bedrock/tools/test.sh     # tek komut: build + denetim + sim + mutasyonlar
```

Dört katman, ucuzdan pahalıya; her biri diğerinin **görmediğini** görür.

| Katman | Ne görür | Ne göremez |
|---|---|---|
| `build.py` | paket üretiliyor mu | çalışıyor mu |
| `check_pack.py` | kimlikler, dil satırları, ipucu sözleşmesi | kod çalışıyor mu |
| `sim/run.mjs` | betik **gerçekten çalışıyor mu** | ekranda nasıl duruyor |
| mutasyonlar | üstteki ikisi gerçekten bir şey yakalıyor mu | — |

### Betiği çalıştıran katman (`sim/`)

`check_pack.py` kimlikleri, `node --check` sözdizimini doğrular; **ikisi de
kodu çalıştırmaz**. Paketin 4000 satırlık betiği uzun süre yalnızca oyunda
koştu — ve modül yüklenirken atılan tek bir `TypeError` bütün paketi sessizce
öldürür. Bir kez yaşandı: `world.beforeEvents.entityHurt` 1.x'te yok, o
satırdan sonrası hiç kaydolmadı.

`bedrock/tools/sim/` sahte bir `@minecraft/server` sağlar ve `main.js`'i Node
altında yükler: olayları tetikler, tick'leri ilerletir, `getBlock` çağrılarını
sayar. Taklit davranışı temsil edecek kadar gerçek — patlama hasar verir,
direnç hasarı azaltır, `applyKnockback`'in nesne biçimi 1.14'te olduğu gibi
**hata fırlatır**. Ölçülen örnek: bütçe bozulduğunda Cam TNT boş alanda tek
tick'te 113 323 `getBlock` yapıyor.

**Yeni bir eşya ya da yetenek eklerken `sim/run.mjs`'e de bir senaryo ekle.**
Çalıştığı görülmemiş kod, çalışmayan koddur.

Tek tek çalıştırmak için:

```bash
python3 bedrock/build.py
python3 bedrock/tools/check_pack.py      # statik denetim
node    bedrock/tools/sim/run.mjs        # betiği çalıştır
bash    bedrock/tools/sim/mutations.sh   # sim gerçekten yakalıyor mu
```

Bedrock hataların çoğunu **sessizce yutar**: olmayan bir doku adı görünmez
model verir, olmayan bir parçacık/ses hiçbir şey yapmaz, olmayan bir blok id'si
`setType`'ta hata fırlatıp `try` içinde kaybolur, eksik bir dil satırı ekranda
ham anahtar gösterir. Hiçbiri hata mesajı üretmez; hepsi ancak oyunda fark
edilir — yani tablete gidip geldikten sonra.

`check_pack.py` bunları build zamanında yakalar: her item'ın ikonu/dokusu/iki
dildeki adı ve ipucu, her eylem türünün karşılığı olan bir kod dalı, her
morph'un vanilla'da gerçekten var olan geometry/texture/material adı, her
render controller referansının çözülmesi, menünün her mob'u tam bir kez
içermesi, betikte geçen her parçacık/ses/etki/blok/varlık kimliği ve ipucundaki
sayıların davranışla aynı olması. Vanilla kimlikleri
`bedrock/tools/vanilla_ids.json`'dan okunur:

```bash
python3 bedrock/tools/fetch_vanilla_ids.py   # yeni MC sürümünde listeyi yenile
bash bedrock/tools/mutation_test.sh          # denetim gerçekten yakalıyor mu
python3 bedrock/tools/tree_check.py          # dev ağacın blok sayısı ve süresi
```

`mutation_test.sh` paketi kasten yedi ayrı şekilde bozup her birinin
yakalandığını doğrular — hep geçen bir denetim, geçmesi anlamsız bir denetimdir.
