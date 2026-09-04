# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.48.0** · Mobil sürüm ana odak; Super TNT tek başına yeterli
olacak şekilde geliştiriliyor (MorphX / mutant paketine bağımlılık yok).

## v1.48.0 — Sahte Elmas Aletler (Bedrock'a özel, Java'da yok)

Beş alet: **Sahte Elmas Kılıç, Kazma, Balta, Kürek, Çapa**. Envanterde gerçek
elmas takımı gibi görünür, elmas kadar vurur ve elmas hızında kazar — ta ki
ilk kullanıma kadar. Bir canlıya (mob ya da oyuncu) vurunca **ya da** bir blok
kırınca elindeki alet aynı türden **tahta** alete dönüşür.

**Kılık gerçekten kılık olmalı.** Üç yerde tam elmas taklidi yapılıyor, çünkü
üçünden biri açık verirse tuzak hiç kurulmuyor:

| Ne | Neden |
|---|---|
| `minecraft:digger`, hız 8 | Elmas kademesi. Yavaş kazan bir "elmas" kazmayı çocuk ilk blokta anlar. |
| `max_durability` 1561 | Elmasın dayanıklılığı; eşyaya bakan dolu bir çubuk görür. |
| Elle çizilen saydam ikon | Elmas mavisi + ahşap sap, vanilla aletlerin sol-alttan sağ-üste duruşu. Görselde hiçbir ipucu yok. |

**Kazma hızı iki etiket kümesinden geliyor.** Her alet önce kendi
`minecraft:is_<tür>_item_destructible` etiketini alıyor — bu, o alet türünün
kırması *gereken* tam blok kümesi — sonra üstüne malzeme etiketleri (`stone`,
`wood`, `sand`...) biniyor. İkinci küme tek başına bırakılsaydı, adı yanlış
yazılmış bir etiket sessizce "hız yok" demek olurdu: Molang `query.any_tag`
tanımadığı etikete hata vermez, `false` döner. İki küme birbirini yedekliyor.
Kılıç da dahil beş aletin hepsinde `digger` var; vanilla kılıç da ağ ve bambu
kırar.

**Değiştirme bir tick ertelenir.** `entityHitEntity` ve `playerBreakBlock`
after-event; oyun vuruş/kırma sonrası eldeki eşyaya dayanıklılık hasarını
yazıyor, aynı tick'te slotu değiştirirsek o yazma bizim koyduğumuz tahta aleti
ezebiliyor. Ertelemenin bedeli oyuncunun arada slot değiştirmiş olabilmesi —
o yüzden ikinci tick'te slot yeniden okunur, hâlâ sahte bir alet değilse
dokunulmaz.

**Tooltip'ler tek şablondan üretiliyor.** Beş aletin de tetikleyicisi aynı;
elle yazılsalar biri "ilk blokta", öteki "ilk vuruşta" der ve tooltip koda
yalan söylemeye başlardı.

## v1.47.0 — Warden kılığında kollar hâlâ bacaklara yapışıyordu

v1.40.0'da düzeltilen **Mutant Warden**'dı: kendi modelimiz, düzeltme
geometrideki `rotation` alanına yazıldı. Menüde bir de **Warden** var ve o
vanilla `geometry.warden`'ı kullanıyor — oraya hiç dokunulmamıştı.

Ölçüldü: vanilla warden'ın kollarında hiçbir `rotation` yok, kollar uzun
(y 6..34) ve bacakların (y 0..13) x aralığına **tam değiyor** — sağ kol
x −17..−9, sağ bacak x −9..−3, örtüşme 0. Oyunda mob'un kolları sürekli
salındığı için bu göze çarpmıyor; oyuncu kılığı ise statik bind pozunda durur
(`animation_speed` hareketsizken 0) ve kollar bacaklara yapışık kalır.

Vanilla modeli değiştiremeyiz. Bunun yerine morph'lara **`pose`** alanı
eklendi: oyuncuya `st:morph` ile kapılı, tek kareli bir animasyon bağlanıyor.
Bedrock animasyon dönüşleri bind pozunun **üstüne eklendiği** için bu, vanilla
modele dokunmadan duruşu düzeltir. Açı, Mutant Warden'daki ile aynı 15°.

### Bütün vanilla kılıklar tarandı

Aynı hata başka nerede var diye 80 vanilla modelin geometrisi indirilip
ölçüldü. Kolu bacağa değen ya da binen altı model çıktı: frog, enderman,
warden, pillager, piglin, evoker. Bunlardan yalnızca warden düzeltildi —
diğerlerinde kol kısa ve gövdeye yapışık durmak vanilla görünümün kendisi
(Steve'in kolları da öyle). Warden'ı ayıran şey kolların **yere kadar uzun**
olması.

7174 statik denetim, 38 statik mutasyon.

## v1.46.0 — Geçici su ve buz, oyundan çıkınca da temizleniyor

Su TNT'nin bıraktığı su 10 saniye, Buz TNT'ninki 30 saniye sonra geri
alınıyordu. Geri alma bir `system.runTimeout` idi — yani **yalnızca
bellekte**. Çocuk o pencere içinde oyundan çıkarsa su sonsuza kadar
kalıyordu. Tablette oyundan çıkmak sıradan bir şey, o yüzden bu sık
yaşanacak bir durum.

Artık her yerleştirme için küçük bir kayıt dünyaya yazılıyor: merkez,
yarıçap, blok türü ve bitiş anı. Aynı seansta `runTimeout` işi vaktinde
yapıyor; dünya yeniden yüklenmişse süresi dolmuş kayıtları 5 saniyede bir
çalışan bir süpürge topluyor. Çevre yüklü değilse kayıt bırakılıyor ve çocuk
oraya döndüğünde temizleniyor.

Süre **dünya saatiyle** tutuluyor (`world.getAbsoluteTime`, mayınlardaki ile
aynı gerekçe): `system.currentTick` yeniden yüklemede sıfırlanır, dünya saati
sıfırlanmaz.

Sahte dünyaya `sertReload` eklendi — gerçek bir yeniden yükleme bekleyen
`runTimeout`'ları kaybeder, eski `reload()` bunu taklit etmiyordu ve bu soru
hiç sınanamıyordu. 129 çalışan test, 36 statik ve 35 sim mutasyonu.

## v1.45.0 — Sessizce ölen döngüler ve bedavaya geçen testler

İki tarama: biri kodun kendisine, biri **test takımının kendisine**.

### Test takımı 24 TNT'yi bedavaya geçiriyormuş

Karşıt denetim, per-TNT davranış testinin üç ayrı sızıntısı olduğunu buldu ve
beş mutasyonla kanıtladı: **24 TNT tamamen içi boş bırakıldı, bütün katmanlar
yeşil kaldı.**

| Sızıntı | Kimi kaçırıyordu |
|---|---|
| "Blok değişti" ölçümü TNT'nin kendi bloğunu sayıyordu — ölçüm blok konmadan alındığı için fark hep 1 çıkıyordu | 7 transform + 6 place TNT'si tek blok bile değiştirmeden geçiyordu |
| Bütün TNT'ler aynı merkezde patlıyordu; `reset()` çalışan işleri iptal etmiyor, önceki TNT'nin işi sonrakinin sayacını dolduruyordu | Bedrock, Cam, Kıyamet, Odun, Komut TNT hiçbir blok kırmadan geçiyordu |
| Sahte `createExplosion` her patlamada hasar veriyor; "etki" ve "hasar" bu hasarla doluyordu | Nükleer, Redstone, Pırt, Uyku TNT hiç efekt uygulamadan; Zeynep Redstone ve Elmas Zırh TNT kimseyi öldürmeden geçiyordu |

Artık: TNT'nin kendi konumu dışındaki gerçek değişiklik, kendi menzili içinde
sayılıyor; her TNT kendi merkezinde patlıyor; sahte `applyDamage` vuruşun
sebebini kaydediyor ve ölçüm patlama dışı vuruş arıyor.

### Sessizce ölen döngüler

Bedrock'ta `runInterval` geri çağırmasından kaçan bir istisna o döngüyü
durdurabiliyor — çocuk ne hata görür ne bir ipucu.

- **Mega Ağaç kalıcı kilitleniyordu.** Ağaç büyürken çıkan çocuğun kimliği
  `treeBusy`'de kalıyordu. Bedrock kimliği relog'da aynı kaldığı için eşya bir
  daha çalışmıyordu. Kimlik artık önceden yakalanıyor.
- **Craft Axe düz seçimde tableti takıyordu.** Bütçe yalnızca `while`
  başlığında bakılıyordu; 1×100×200 bir duvarda tek x-dilimi bütün seçim
  demek, yani tek tick'te 20 000 `getBlock`. Mevcut test 20×20×20 bir küp
  kullandığı için (bir dilim 400 konum, bütçe 700) bunu hiç görmüyordu.
- **Renkli portallar** oyuncu başına gövdeyi korumasız çalıştırıyordu; kardeş
  dönguler (tuzak blokları, boyut kamerası) hepsi koruyor.
- **Bütün sihirli eşyaların hatası tek bir boş `catch`'te yutuluyordu** —
  "çocuk eşyayı yanlış tutuyor" ile "eşya bozuk" ayırt edilemiyordu. Artık
  `console.warn` ve ekranda bir satır var.
- **`mines` ve `owners` hiçbir tavana bağlı değildi** ve kayıt yazımı
  başarısız olursa sessizce yutuluyordu: dünya yeniden yüklenince bütün
  sahiplik ve mayınlar izsiz kaybolabilirdi.
- Eşya ve blok kırma dinleyicileri artık boş olaya karşı korumalı.

### Doğru çıkmayan bir bulgu

Tarama, Ejderha Nefesi'nin sahibi oyundan çıkınca **altı kullanımdan sonra
kalıcı kilitlendiğini** bildirdi. Ölçüldü: doğru değil. Bitiş koşulu
`t >= total`, eşitlik değil — atışın olduğu tick'te gövde yarıda kesilse bile
bir sonraki atış-dışı tick bitişi çalıştırıyor ve sayaç düzeliyor. Yakalanan
oyuncu kimliği yine de düzeltildi (bulut atış başına 12 istisna atıyordu) ama
test, ölçülen şeyi sınıyor: döngü gövdesi istisna atmamalı.

Toplam: **127 çalışan test**, 36 statik ve **33 sim mutasyonu**.

## v1.44.0 — Yetmiş TNT'nin ipucu ile kodu tek tek karşılaştırıldı

Son tarama bütün TNT'lerin ipucunu kodunun yaptığı işle yan yana koydu ve
dokuz yerde ayrıldıklarını buldu. Hepsi aynı sınıftan: **ipucu bir şey söz
veriyor, kod başka bir şey yapıyor.** İpucu bu paketin sözleşmesi — çocuk ne
olacağını oradan okuyor.

### Söz verilip yapılmayanlar

| TNT | İpucu ne diyordu | Kod ne yapıyordu | Ne yapıldı |
|---|---|---|---|
| Su TNT | "Ateşleri söndürür" | `onlyAir` yüzünden ateş bloğuna hiç dokunmuyordu | `douses` bayrağı: ateş bloğu da suya çevriliyor |
| Gülen Yüz TNT | "Çıngırak sesiyle" | hiçbir ses çalmıyordu | `note.bell` çalınıyor |
| Çizgi TNT | "Her yeri el yazısı defterine çevirir" | tek bir blok bile değişmiyordu | ipucu yaptığı işi anlatıyor: kağıt/mürekkep/tüy saçar |
| Gizli TNT | "tüm canlıları öldürür" | patlatanı ayırıyordu | "patlatan hariç" yazıldı |
| Kalp TNT | TR "herkese", EN "players" | yalnızca oyunculara | TR de "oyunculara" |
| Buz TNT | "kar yağar" | Bedrock'ta Snow hava tipi yok, yağmur veriyordu | "yağış başlar (soğuk biyomda kar)" |

### Söylenmeyen zararlar

| TNT | Ne oluyordu | Ne yapıldı |
|---|---|---|
| Şimşek Yağmuru TNT | **20 dakika** gerçek fırtına — haritanın öbür ucundaki kardeşin de başına yıldırım iniyordu | süre 1 dakika, ipucunda yazıyor |
| Yağmur / Bulut TNT | aynı şekilde 20 dakika yağmur, süresi hiç yazmıyordu | 1 dakika, ipucunda yazıyor |
| Şimşek TNT | 20 yıldırım; yıldırımın yaktığı ipucunda yoktu | "Yıldırım yakar ve öldürebilir" |
| Güneş Kristal TNT | 15 End kristali; kristale vurulunca büyük patlar, uyarı yoktu | "DİKKAT: kristale vurursan büyük patlar!" |

### Bulgular tek TNT'ye değil, kurala çevrildi

Dokuz düzeltmenin dokuzu da tek satırlık bir yamayla kapatılabilirdi. Bunun
yerine her biri `check_pack.py` içinde **bütün TNT'lere bakan bir kural** oldu,
çünkü asıl mesele bu TNT'ler değil, aynı tuzağa düşecek bir sonraki TNT:

- "Söndürür" diyen her TNT ateş bloğunu gerçekten değiştirmeli.
- Dünya havasını değiştiren her TNT 2 dakikayı aşamaz ve süresini ipucunda
  yazmalı — hava olayı haritanın tamamını etkiliyor.
- Yıldırım çağıran her TNT yakma ve ölüm riskini iki dilde de yazmalı.
- End kristali yaratan her TNT kristalin patladığını yazmalı.
- Anında öldüren TNT'de "patlatan hariç" iddiası `exceptIgniter` ile aynı olmalı.
- "Çevirir / boyar / kaplar" diyen ipucu blok yazan bir etkiye bağlı olmalı.
- Ayrı bir ses vaat eden ipucun `sound` alanı olmalı.
- Hiçbir ipucu "kar yağar" diyemez — Bedrock'ta öyle bir hava tipi yok.
- Yalnızca oyunculara etki eden TNT "herkese" diyemez.

Toplam: 7069 → **7165 statik denetim**, 120 → **124 çalışan test**,
33 → **36 statik mutasyon**, 22 → **25 sim mutasyonu**.

Ayrıca `mutation_test.sh` yarıda kesilirse artık `build.py`'yi mutasyonlu
bırakmıyor — çıkışta kaynağı geri alıp paketi yeniden üretiyor.

## v1.43.0 — Hata taraması: ipucu sözleşmesi, sıkışma, tablet yükü

Dört ayrı taramanın bulguları. Hepsi kod okunarak doğrulandı, hiçbiri tablette
görülmedi — çoğunun ortak yanı şu: **kod çalışıyor, hata vermiyor, ama söz
verilen şey olmuyor.**

### İpucu ne diyorsa kod onu yapsın

| Ne | Neydi | Ne oldu |
|---|---|---|
| **Güneş TNT** | Zaman değiştirme koşulu Ay TNT'den kopyalanmıştı: `tod < 12300` yani **zaten gündüzse**. Güneş TNT'yi gece patlatınca hiçbir şey olmuyordu — tam da kullanılacağı anda. | Koşul yön değiştiriyorsa çalışır: Ay gündüzü geceye, Güneş geceyi gündüze. |
| **Mağara TNT** | "Mağara oyar" diyordu; patlaması `breaksBlocks: false` idi, tek bir blok bile kırılmıyordu. | Patlaması artık kırıyor (`digs`). Diğer `spawn` TNT'leri kırmıyor — çocuğun evini yıkmasın. |
| **Altın TNT** | "Merkez + 5 küçük dalga" diyordu; tek patlama vardı. | Merkezin çevresine gecikmeli beş küçük patlama. |
| **Şeker TNT** | "+ hız ve zıplama" diyordu; hiç efekt vermiyordu. | Yarıçaptaki oyunculara 30 sn hız + zıplama. |
| **Küp TNT** | Adı Küp, kazısı küreydi (`bx²+by²+bz²>r²`). | Gerçekten küp (`cube`). |
| **Uyku TNT** | İpucu dört efekt sayıyordu, kod beş uyguluyordu — kazma yorgunluğu yazılmamıştı. | İpucuda yazıyor. |
| **Lazer Kılıcı** | İpucu yalnızca "blok keser" diyordu; ışının içindeki canlıya 30 hasar (15 kalp) veriyordu. | İpucuda yazıyor. |
| **Enerji Kristali** | "Baktığın bedrock'u kırar" diyordu; baktığın **her** bloğu siliyordu. `setType` normal kırma yolunu atladığı için sandığa tıklayınca içindekiler de yok oluyordu. | Yalnızca bedrock; başka bloğa bakınca söylüyor. |
| **Kalp Baltası** | "Mob'u tek vuruşta öldürür, **oyunculara ağır hasar**" diyordu; oyuncuya taban 20 + betikten 25 = 45 hasar biniyordu, yani 20 canlı kardeş de tek vuruşta ölüyordu. | Taban 7, betikten ek hasar yok. Mob hâlâ tek vuruşta ölüyor. |
| **Ender Send Yumurtası** | "Işınlanır" diyordu; çağrılan boss'ta ışınlanma davranışı hiç yok. | İpucudan çıkarıldı. |
| **Yıldırım Büyüsü** | Türkçe ipucu "yakar ve öldürür" diyor, İngilizce demiyordu. | İkisi de diyor. |

### Sıkışma ve kaybolan durum

- **Blok kılığı dünya yeniden yüklenince ölüyordu.** Hangi kılıkta olduğun
  `st:morph`'ta kalıcı ama ayrıca bellekte bir `Map` tutuluyordu. Dünya kapanıp
  açılınca o Map boşalıyor, çocuk blok görünüyor ama ne yerleşebiliyor ne
  gökyüzüne bakıp insana dönebiliyordu — iki çıkış yolunun ikisi de sessizce
  hiçbir şey yapmıyordu. Map kaldırıldı; durum artık `st:morph`'tan
  **türetiliyor**. Tek doğru kaynak kuralı: aynı bilgi iki yerde durmasın.
- **Alçak tavanın altında büyümek reddediliyor.** Küçülüp bir bloklık boşluğa
  giren çocuk orada büyüyünce blokların içinde kalıyordu. Küçülmede kontrol yok
  — küçük kutu her zaman sığar.

### Tablet yükü

- **Craft Axe doldurması tick'e bölündü.** Paketteki tek bölünmemiş toplu blok
  işiydi; tavan yalnızca **konan** bloğu sayıyordu, **bakılan** konumu değil, o
  yüzden masif taşın içini doldurmaya çalışan çocuk hiçbir blok koymadan 20 000
  konum tarıyordu.
- **Kamera ötelemesi kademe başına tabloya çevrildi.** Yazdığım formül
  (`0.25 × ölçek + 0.10`) dört kademenin üçünde çarpışma kutusunun dışına
  taşıyordu, yani düzelttiğimi sandığım siyahlık üç kademede duruyordu. Hesapla
  görüldü, tabletle değil.

### Bakılıp temiz çıkanlar

İki kardeşin birbirine karışması (bütün bekleme süreleri oyuncu kimliğine
bağlı), boyut anahtarlı kayıtlarda boyut kimliği, TNT fitilinin yarış durumu,
`getEntities` çağrılarının yarıçapı, kalıcı listelerin (portal, izlenen TNT)
üst sınırı — hepsi doğrulandı, sorun yok. Sahip/mayın kayıtlarının üst sınırı
yok; iki oyunculu bir dünyada aylarca sorun çıkarmaz, açık bırakıldı.

## v1.42.0 — POV kamerası geri geldi, bu sefer doğru sürülüyor

**v1.41.0'daki teşhis yanlıştı.** "Küçülünce ilerleyemiyorum, geri gidemiyorum,
eğilemiyorum" şikâyetini `minecraft:free` kameranın oyuncuyu kilitlemesine
bağlamıştım ve kamerayı tamamen kaldırmıştım. Belge bunun tersini söylüyor:
hareketi kilitleyen şey `/inputpermission`, serbest kamera değil ([Free Camera
Preset Tutorial](https://learn.microsoft.com/en-us/minecraft/creator/documents/camerasystem/camerapresetfree)).
Yani oyuncu hareket edebiliyordu; sorun kameranın **yanlış sürülmesiydi** ve
kamerayı kaldırınca çocukların sevdiği "küçükken dünya kocaman" etkisi de gitti.

Kamera geri geldi. v1.40.0'daki hali üç şeyi birden yanlış yapıyordu:

| Belirti | Sebep | Düzeltme |
|---|---|---|
| Eğilince görüntü inmiyor | Konum `p.location` + **sabit** 1.62 × ölçek idi | `getHeadLocation()` — gerçek göz konumu, çömelme dahil |
| Kamera arkadan gelip yetişiyor | Her tick tek bir konuma **atlıyordu**; oyun 60 kare çizerken kamera 20 kez zıplıyor | Her set bir tick boyunca lineer `easeOptions` ile sürülüyor |
| Görüntüde siyahlık | Kamera oyuncunun **kendi kafasının içinde**; serbest kamerada kendi modelin de çizilir | Bakışın yatay bileşeninde kafanın dışına ötelendi (`camForward`) |

**Kalan sınır:** betik saniyede 20 kez çalışır, kamera en iyi ihtimalle bir tick
geride kalır. Easing bunu *akıcı* yapar, sıfırlamaz. Gerçekten oyuncuya bağlı
bir kamera için deneysel "Creator Cameras" üçüncü-şahıs ön-ayarları gerekir,
onlar da dünyada bayrak açmayı zorunlu kılar.

**Neden başka yolu yok.** Bedrock'ta ilk-şahıs göz yüksekliği sabittir;
`minecraft:scale` belgede "visual size multiplier" olarak tanımlı ve
[Geyser #5554](https://github.com/GeyserMC/Geyser/issues/5554) ölçeklenen
oyuncunun ilk-şahıs bakışının değişmediğini bildiriyor. Özel kamera ön-ayarları
da işe yaramıyor: *"A custom Camera Preset can inherit from other custom Camera
Presets, or from the `minecraft:free` preset. For now, the other built-in camera
perspectives can't be specified here."*

### Tek bloklık boşluğa girme

Kademe 1'in çarpışma yüksekliği tam **1.00** idi — bir bloklık boşluk için
sınırda kalıyor ve çocuk "küçüldüm ama giremiyorum" diyordu. **0.95** oldu, pay
bırakıyor. Görsel ölçek de hizalandı (0.95/1.8 → 0.53).

### Normal creeper'ın patlaması

Dev Creeper çömelince blok kırıyor, normal creeper ise yalnızca ses çıkarıyordu:
patlama oyuncunun ayağı dibinde oluşuyor, canı geri konuyor, `breaksBlocks`
false olduğu için ortada **hiçbir iz kalmıyordu**. Gerçek creeper da blok kırar;
artık bu da kırıyor (yarıçap 3, Dev Creeper 6 ile daha sert kalıyor). İpucu
kendiliğinden "(blok kırar!)" yazıyor.

### Bu sınıfı bir daha kaçırmamak için

En sinsi hata türü buydu: **kod çalışır, hata vermez, çocuk hiçbir şey görmez.**
Hiçbir kimlik/dosya denetimi bunu yakalayamaz. İki yeni tarama eklendi:

- `check_player_control()` — POV kamerası `getHeadLocation()` kullanmak,
  `easeOptions` ile sürülmek ve öne ötelenmek **zorunda**; `inputpermission`
  yasak; kademe 1 yüksekliği 1.0'ın altında olmak zorunda; `explode` yeteneği
  olan her dönüşüm blok kırmak zorunda.
- Sim'de **her eşya ve her dönüşüm yeteneği** için "görünür bir şey yaptı mı"
  taraması: parçacık, ses, patlama, düşen eşya, komut, eylem çubuğu yazısı,
  hasar, savurma, ışınlanma ya da blok değişikliğinden en az biri olmalı.
  Sağ tıkla değil başka yolla iş gören eşyalar (`bleed`, `heavy`, `worn_wool`,
  `held_fireproof`) açıkça listelenmiş durumda — sessizce muaf olan yok.

Bir denetim bir kez kendi **yorum satırına** takıldı: blokta "getHeadLocation()
ile" yazıyor, kod onu kullanmasa da kelime dosyada duruyordu ve denetim boş
geçiyordu. Denetimler artık koda bakmadan önce `//` yorumlarını atıyor.

## v1.41.0 — Oyunu kilitleyen hatalar, blok kılığının üst yüzü, ikonlar

Üç ayrı iş. Hepsi tablette görülen bir şikâyetten çıktı.

### 1. Küçültme/Büyütme oyuncuyu kilitliyordu

Boyut normalden farklıyken script her tick `player.camera.setCamera(
"minecraft:free", …)` sürüyordu ve çocuk "ilerleyemiyorum, geri gidemiyorum,
eğilemiyorum" diyordu. O sürümde kamerayı tamamen kaldırdım.

> **Bu teşhis yanlıştı — v1.42.0'da düzeltildi.** Serbest kamera hareketi
> kilitlemez; kilitleyen `/inputpermission`. Oyuncu hareket edebiliyordu,
> kamera yanlış sürüldüğü için görüntü takip etmiyordu. Kamerayı kaldırmak
> belirtiyi geçirdi ama çocukların sevdiği "küçükken dünya kocaman" etkisini
> de götürdü. Ayrıntı v1.42.0 bölümünde.

Aynı aileden iki hata daha bulundu ve düzeltildi:

| Ne | Neydi | Ne oldu |
|---|---|---|
| Buz TNT | Dünyadaki **her** oyuncuyu 30 sn slowness amp 6 ile felç ediyordu (amp ≥ 6 = hız sıfır). 300 blok ötedeki kardeş sebepsiz kilitleniyordu. | Yarıçapla (14 blok) sınırlandı; ipucu da öyle diyor. Java sürümünden bilerek ayrılan tek yer. |
| Ölüm | `st:size` / `st:morph` oyuncunun üzerinde kalıcı; minicik ya da blok kılığında ölen çocuk aynı halde uyanıyordu ve geri dönüş yolunu (Kalp TNT / asa) her zaman bulamıyor. | Yeniden doğuşta boyut ve kılık normale döner. Dünyaya ilk girişte dokunulmaz. |
| Among Us Rapor | İpucu "Tek kullanımlık" diyor ama eşya harcanmıyordu; 3 sn'de bir sınırsız öldürme. | Kullanınca envanterden düşüyor. |

`check_pack.py` → `check_player_control()` bunları kilitliyor: `setCamera`
yasak, slowness amp ≥ 6 en fazla iki yerde, Buz TNT'nin yarıçapı zorunlu.

### 2. Blok kılığının üst yüzü yan yüzün aynısıydı

Kılık tek bir 16×16×16 küptü ve bir çizim geçişi **tek** doku örnekler; o
yüzden çim bloğunun üstü de yandan görünüşüyle çiziliyordu. Üst ve alt yüz
artık ayrı geçişte, kübün 0,1 birim dışına konan düz levha olarak çiziliyor
(z-fighting olmasın diye). Ayrı dokusu olan 16 blok `BLOCK_FACES` tablosunda:
çim üstü `grass_carried`, altı `dirt`; kütük `log_oak_top`; TNT `tnt_top` /
`tnt_bottom`; tezgâh `crafting_table_top` / `planks_oak` … Bal bloğunun yan
dokusu da yanlıştı (`honey_top` yazıyordu), `honey_side` oldu.

### 3. İkonlar

`bedrock/tools/icon_sheet.py` eklendi: bütün ikonları tek bir PNG'de dama
tahtası zemin üzerinde gösterir, böylece "arkaplanı boyalı mı" bir bakışta
görünür. Bakarak bulunanlar:

- **Arkaplanlar kaldırıldı.** Elle çizilen ikonlar opak RGB yazılıyordu;
  envanterde eşyanın arkasında renkli bir kare duruyordu. Artık RGBA ve
  arkaplan saydam. **Ses Saldırısı ve Ejderha Nefesi hariç** — ikisi de bir
  dalga/ışık etkisi çizer ve saydam zeminde havada asılı duruyordu.
- **Dönüşüm Asası'nın ikonu yoktu** (düz mor kare), **TNT Frizbi** TNT
  bloğunun yan yüzünü gösteriyordu, **Among Us Rapor** ve **Şimşek Büyüsü**
  boyalı arkaplanla geliyordu — dördü de yeniden çizildi.
- **Kalp Baltası** lolipop gibi okunuyordu (gümüş ağız eklendi), **Gökkuşağı
  Çizme** üst üste renkli çubuklardı (çizme silueti oldu), **Takım Asası**
  tek piksel kalınlığındaydı, **Dondurucu**'nun beyaz kar tanesi açık renkli
  envanter kutusunda kayboluyordu (buz mavisi oldu).
- İki ikonda gövdenin dışında kalan yüzen piksel vardı (sağlık iksirinin
  parlaması, warden yumurtasının beneği) — içeri alındı.

`check_icons()` ikonun boş olmadığını ve kenar piksellerinin saydam olduğunu
denetliyor (boyalı arkaplan kenarın %100'ünü doldurur; gerçek ikonların en
yükseği %25).

## v1.40.0 — Mutant Warden'ın duruşu düzeltildi

Üçüncü taraf modelin özgün bind pozunda iki ön kol gövdenin orta çizgisinde,
bel altında birleşiyordu. Ekranda edepsiz duruyordu. Omuzlara **15° dışa** açı
verildi; ön kollar artık bacakların dışında duruyor.

Modelin bir animasyonu yok denecek kadar az iş yapıyor: duruyorken kol
salınımı `variable.animation_speed` ile çarpılıyor ve o değer hareketsizken 0,
üstelik 11 animasyonun hepsi kolları `[0,0,0]`'dan başlatıyor. Yani **duruş
pozu = bind pozu**; düzeltmenin doğru yeri geometrideki `right_arm` /
`left_arm` kemiklerinin `rotation` alanı. Bedrock animasyonları bind pozuna
**eklendiği** için animasyonlar bozulmuyor.

### Bunu görebilmek için: `bedrock/tools/pose_render.py`

Bir modelin "nasıl durduğu" hiçbir kimlik/dosya denetiminde görünmez; ancak
görülerek anlaşılır ve tablete gidip gelmek pahalı. Yeni betik bir
`.geo.json`'ı ön / yan / üst görünüm olarak PNG'ye çiziyor, her kemiği ayrı
renkte. `--rot right_arm=0,0,15` ile deneme açısı verilip sonuç anında
görülebiliyor.

**Dönüş konvansiyonu tahmin değil, ölçüldü.** İlk yazdığım matris yanlıştı ve
render sessizce yanlış bir şey çiziyordu — uzuvlar gövdeden kopuk havada
duruyordu, dolayısıyla ondan çıkardığım poz ölçümleri de geçersizdi. Doğrusu
şöyle bulundu: bir modelde her kutu ya kendi kemiğindeki başka bir kutuya ya
da gövdeye **değmek** zorundadır. 6 sıra × 8 işaret = 48 kombinasyon denendi;
yalnızca X ve Z'si ters çevrilmiş olanlar "toplam kopukluk 0.0" veriyor, düz
`Rz·Ry·Rx` ise 34.8 birim kopukluk. `pose_render.py` bu ölçümü kendi
docstring'inde taşıyor.

### Denetim

`check_pack.py` → `check_poses()`: kol kemiklerinin kutuları yeniden
hesaplanıp "bacak arası kutusu"na (x −6..6, y −4..20, z −16..6) giren var mı
diye bakılıyor. Değmek değil, **girmek** aranıyor — eşik her eksende 2 birim,
çünkü omuz kutusu köşeyi yarım birim sıyırıyor ve bu bir duruş hatası değil.
22. mutasyon açıyı geri alıp denetimin gerçekten yakaladığını doğruluyor.

## v1.39.0 — Sınırsız can, kırılmaz Kalp Baltası

**Can Artırıcı artık süresiz.** Eskiden 1000 can veriyor ve 30 dakika sonra
bitiyordu; çocuk oyunun ortasında normale dönüyordu. Şimdi:

- Can **1044** oluyor. Bu bir tercih değil, Bedrock'un tavanı: `health_boost`
  efektinin `amplifier` alanı en fazla 255, her kademe 4 can ekliyor, taban can
  20 → `20 + 4 × 256 = 1044`. 255'in üstüne çıkan bir değer verildiğinde efekt
  **hiç** uygulanmaz, yani "daha büyük sayı yaz" sessiz bir hataya dönerdi.
  `check_pack.py` bu tavanı denetliyor.
- **Süre yok.** Bedrock'ta sonsuz süreli efekt yoktur, en fazla uzun süreli
  vardır. Onun yerine oyuncuya kalıcı bir işaret (`stnt:hpforever` dinamik
  özelliği) konuyor; 2 saniyede bir çalışan bir döngü işareti görüp efekti 60
  saniyelik olarak tazeliyor ve canı tepeye çekiyor. Tazeleme aralığının
  süreden çok kısa olması bilerek: çocuk tam tazeleme anında oyundan çıkıp
  girse bile efekt üstünde kalıyor.
- İşaret kalıcı olduğu için **çıkıp girince de duruyor**.
- Vazgeçmek için **Temizleyici TNT** (ve Kalp TNT) işareti de siliyor. Silmese
  döngü bir sonraki turda efekti geri koyardı ve "tüm efektleri temizler" sözü
  yalan olurdu.

**Kalp Baltası artık eskimiyor** — hayatta kalma modunda bile. Bedrock'ta
"sonsuz dayanıklılık" diye bir değer yok; doğru yol `minecraft:durability`
bileşenini hiç koymamak. Bileşeni olmayan eşya hasar almaz.

## v1.38.0 — Blok Kılığı (saklambaç)

Elinde **Blok Kılığı** varken bir blok kır — o bloğa dönüşürsün. Üç hareket,
üçü de birbirinden ayrı:

| Hareket | Ne olur |
|---|---|
| Blok **kır** (eşya elde) | O bloğun kılığına girersin |
| Blokken bir yere **basılı tut** | Oraya blok gibi yerleşirsin (ızgaraya hizalı) |
| **Gökyüzüne** bakıp basılı tut | İnsana dönersin |

**48 blok kılığı**, 74 blok kimliği: taş, toprak, çim, kum, çakıl, tahta,
kütük, yaprak, cam, tuğla, obsidyen, altı maden bloğu, netherrack, buz, kar,
balkabağı, kabak feneri, karpuz, kitaplık, tezgah, fırın, TNT, saman, sünger,
kil, kuvars, sakız, bal, mercan, sculk, kaya katmanı, beş yün rengi — artı
paketin kendi dört TNT'si. Aynı kılığa birden fazla blok bağlı: meşe kütüğü de
koyu meşe kütüğü de aynı kılığa sokar.

**Neden mob dönüşümüyle aynı makine.** Kılık, `st:morph` özelliğine bağlı bir
render controller — mob morph'larıyla birebir aynı yol. Tek farkı geometry
(16×16×16 küp, her yüzü aynı 16×16 dokuyu örnekler) ve dokunun bir mob değil
**blok** dokusu olması. Vanilla blok dokuları da tıpkı mob dokuları gibi
**adıyla** referans alınır, pakete konmaz. Bunun getirisi: dönüşüm menüsüne
"Bloklar" sekmesi kendiliğinden geldi, isim gizleme ve ölçek zinciri de
kendiliğinden çalışıyor.

**Çıkış yolu bilerek "boşluğa bak".** Çömelme seçilmedi: çömelme morph
yeteneklerini tetikliyor. Kapalı bir odada bile tavana bakıp çıkılabilsin diye
"bakılan blok yok" koşulu seçildi. Mutasyon testi bu çıkış yolunun kapanmasını
ayrıca yakalıyor — çocuğun blok kılığında mahsur kalması, oyunu en çok bozacak
şey olurdu.

`vanilla_ids.json` artık **1227 blok dokusunu** da taşıyor; denetim her kılığın
dokusunu ve her blok kimliğini doğruluyor (5410 kontrol). Sim tarafında sekiz
yeni senaryo: eşya elde değilken kırmak dönüştürmemeli, farklı blok farklı
kılığa sokmalı, kılığı olmayan blok söylenmeli, yerleşme ızgaraya hizalanmalı,
gökyüzü çıkışı çalışmalı, mob kılığındayken yerleşme çalışmamalı.

## v1.37.0 — git-gel önleme turu

Tablete gidip gelmeyi gerektirecek her şeyi yerelde aramaya çalıştım. İki
gerçek hata, bir isim karışıklığı ve dokuz yeni denetim çıktı.

**Temizleyici TNT ipucunun ikinci yarısını yapmıyordu.** İpucu "Tüm efektleri
**ve boyut değişikliklerini** temizler" diyor; kod yalnızca efektleri
siliyordu. Küçültülmüş çocuk temizleyiciyi patlatıp küçük kalıyor ve eşyayı
bozuk sanıyordu. Kalp TNT bunu zaten yapıyordu — aynı satır eklendi.

**Envanterde iki "Pembe Lego Tuğla" vardı** (aynısı yeşilde de). Biri
konulabilen blok, diğeri hiçbir şey yapmayan ganimet. Çocuk ganimeti koymaya
çalışıp eşyayı bozuk sanıyor. Ganimet tarafı "Lego Parçası" oldu ve ipucu
farkı yazıyor.

**Bilerek aynı görünenler doğrulandı, değiştirilmedi.** Gizli TNT ile Cam TNT
her yüzden aynı — ipucu "Cam kılığında" diyor, kılık değiştirme bir özellik.
Doğru/Yanlış Altın Plaka aynı sebeple aynı. İkisi de denetimde adı geçen bir
istisna listesinde; listede olmayan bir tekrar artık hata.

**Yetmiş TNT'nin hepsi çalıştırıldı** — daha önce dördü denenmişti. Her TNT
türüne göre beklenen işi yapıyor mu diye bakılıyor: blok işleyen blok
değiştirmeli, saçan eşya bırakmalı, etki veren yakındaki cana etki işlemeli.
"Patladı ama hiçbir şey olmadı" sınıfı artık yakalanıyor. (Virüs TNT bilerek
hiçbir şey yapmaz; ipucu da öyle diyor, istisnası yazılı.)

**Yeni denetimler** (4359 kontrol, 20 + 7 mutasyon): envanter ikonlarının
benzersizliği, blok yan yüzlerinin çakışmaması, bütün PNG'lerin geçerliliği,
dil dosyalarında tekrar eden anahtar ve iki dil paritesi, aynı görünen ad,
manifest çapraz referansları (BP bağımlılığı RP'nin uuid'i mi, sürümler aynı
mı, uuid'ler benzersiz mi), tarif kimlikleri ve **yaratıcı menü grubu** —
grubu olmayan bir eşya menüde hiçbir yerde görünmez, çocuk "eklenmemiş" sanır.

## v1.36.0 — İsim Değiştirme, Can Artırıcı 1000

**Can Artırıcı artık 1000 can veriyor** (300'dü). Efekt amplifier'ı 244 —
Bedrock'un 255 tavanının altında. Denetime kural eklendi: tavanı aşan bir can
hedefi sessizce hiç uygulanmaz, yani eşya boşa tıklanırdı.

*Not: 1000 can 500 kalp demek. Bedrock'un can çubuğunun bu kadar kalbi nasıl
çizdiği tablette görülmeli — ekranı kaplarsa sayıyı düşürmek gerekebilir.*

**İsim Değiştirme.** Basılı tut, çıkan kutuya yeni ismini yaz, sekiz renkten
birini seç. Diğer oyuncular tepende o ismi görür. Kutuyu boş bırakıp
onaylarsan gerçek ismine dönersin. En fazla 20 harf; satır sonu ve fazla
boşluk temizleniyor.

Takma ad oyuncunun kendi kalıcı özelliğinde tutuluyor ve dönüşüm döngüsü her
turda oradan okuyor. Aksi hâlde döngü beş tick sonra gerçek ismi geri yazar ve
takma ad bir anda kaybolurdu — aynı döngü dönüşünce ismi gizleyen döngü.
Dönüşmüşken isim zaten gizli; insana dönünce yeni isim görünür.

*Sınır: `nameTag` yalnızca kafanın üstündeki yazıdır. Sohbetteki ad ve oyuncu
listesi değişmez — onları betik değiştiremez.*

Denetim 3223 kontrol, 15 mutasyon.

## v1.35.0 — tablet öncesi denetim: on bir hata

Bağımsız bir inceleme paketin tamamını taradı. Bulunanların hiçbiri yeni
eklemelerden değil; bir kısmı sürümlerdir duruyordu. Üç gruba ayrıldı.

### Oyun süresinden yiyenler

**Tick bütçesi yapılan işi değil sonucu sayıyordu.** Blok işleyen döngülerde
sayaç yalnızca `setType`'ın yanındaydı, yani *değiştirilen* bloğu sayıyordu.
Filtreye uyan blok yoksa sayaç sıfırda kalıyor, `while` koşulu hiçbir zaman
yanlışlanmıyor ve tüm hacim **tek tick'te** taranıyordu. Cam TNT (r=30) camsız
arazide 226 981 pozisyon (~113 000 `getBlock`), Kıyamet TNT (r=35) havada
patlarsa ~180 000. Bu takılma değil, saniyelerce donma. Mega ağaçta da aynısı:
yamaçta büyüyen ağacın tepe katmanları `onlyAir`'e takılıp geri dönüyor, bütçe
sıfırda kalıyor ve kalan ~58 katman tek tick'te iniyordu.

**Craft Axe'ın hacim tavanı yoktu** — köşe koyup 500 blok yürüyünce 25 milyon
pozisyon tek tick'te taranıyordu. Artık 20 000 (yaklaşık 27×27×27) üstü hiç
başlamıyor.

**Zincir işlerine tavan (8).** 30 TNT'lik bir yığın — çocukların normal
kullanımı — ~30 eş zamanlı iş, tick başına ~7000 `getBlock` demekti.

### Öldüren / tuzağa düşürenler

**"Patlatanı esirger" kuralı çoğu yolda uygulanmıyordu.** `igniterId` yalnızca
çakmak ve kumanda yollarından geliyordu; redstone, zincir ve başka bir
patlamanın tetiklemesi onu boş bırakıyor ve kural sessizce düşüyordu. Zeynep
Redstone TNT anlık ölüm / 30 blok ve ipucu "patlatan hariç" diyor — kola basan
çocuk her seferinde ölüyordu. Artık bloğu kimin koyduğu kayıttan bulunuyor
(kayıt zaten sahibi tutuyordu, sadece okunmuyordu).

**Dağ TNT oyuncuyu diri diri gömüyordu** — tek kalıcı `onlyAir` doldurma o.
Artık oyuncunun durduğu iki blok atlanıyor.

**End Gate sabit koordinata, zemin kontrolü olmadan ışınlıyordu.** (100, 70, 0)
serbest düşüş: platform y≈49'da, yani en iyi ihtimalle 9 kalp; (100, 0) ana
adanın kenarı olduğundan ıskalarsa boşluk ve tüm envanter. Artık zemin
aranıyor, yoksa obsidyen platform kuruluyor.

**Portal ping-pong.** İniş karesinin kendisi portal sayıldığı için oyuncu iki
saniyede bir geri çekiliyordu — yerinden kımıldamadıkça sonsuza kadar.

### Sessizce hiçbir şey yapmayanlar

**Zıplatan TNT oyuncuyu hiç fırlatmıyor, Mıknatıs TNT hiçbir şeyi
çekmiyordu.** `applyKnockback`'in nesne biçimi `@minecraft/server` 2.x'te
geldi; manifest 1.14.0'a bağlı, orada imza dört sayı. İki çağrı yalnızca nesne
biçimini kullanıyor, hata fırlatıp çevredeki `catch`'te kayboluyordu.

**Kara Delik diğer oyunculara hiçbir şey yapmıyordu.** `applyImpulse` oyuncuda
hata veriyor ve `try`'ın ilk satırıydı; körlük ve hasar hiç çalışmıyordu.

**Mayınlar dünya yeniden yüklenince kalıcı olarak etkisiz kalıyordu.**
`system.currentTick` diske yazılmıştı; sayaç her yüklemede sıfırlanıyor, kayıtlı
değer büyük kalıyor. Among Us raporu da aynı sebeple "hazırlanıyor" deyip
duruyordu. İkisi de kalıcı dünya saatine geçti.

**Dört TNT'nin ipucu koddan büyük yazıyordu** (30/50 yazan, 14/18/20/40 yapan).
Çocuk ipucuna göre konumlandığı için bu bir sayı hatası değil güven hatası.
Denetime kural eklendi: "N blok yarıçap" yazan her ipucu gerçek yarıçapı
yazmalı — 3211 kontrol, 13 mutasyon.

## v1.34.0 — tablete gitmeden önceki düzeltmeler

Üçü de "yeteneği kullananı cezalandıran" sınıfından; hiçbiri tablette
denenmeden bulundu.

**Creeper dönüşümü kendi patlamasında öldürüyordu.** Patlama oyuncunun ayağının
dibinde oluşuyor. Vanilla TNT gücü 4 ve sıfır mesafede zırhsız oyuncuyu
öldürür; buradaki güç creeper'da 3, Dev Creeper'da 6. Yani yeteneği her
kullanan ölüyordu — çocuk bir daha kullanmaz. İki katman kondu: patlamadan önce
Direnç V (sürümler arasında %80 mi %100 mü kestiğine güvenmiyoruz) ve iki tick
sonra patlama öncesi canın geri konması. Tek başına can geri koyma yetmez;
oyuncu o tick'te ölürse geri koyacak can kalmaz.

**Ender Send'in ışınlanması gökyüzüne bakınca sessizce hiçbir şey
yapmıyordu** — üstelik beklemeyi de yakıyordu, yani çocuk yeteneğin bozuk
olduğunu sanıyordu. Artık "Işınlanmak için bir yere bak" yazıyor ve bekleme
yanmıyor. Havaya ışınlamak çözüm değil: düşme hasarı verir.

**Ejderha Nefesi'ne eş zamanlı bulut tavanı (6).** Kişi başı 3 saniye bekleme
vardı ama bulut 12 saniye yaşıyor: tek çocuk 4, dört çocuk 16 bulut açabiliyordu.
Her bulut 5 tick'te 24 parçacık demek — 16 bulut tick başına ~77 parçacık,
tablette takılma. Tavan dolunca "Çok fazla bulut var, biraz bekle" yazıyor.

Denetim de genişledi: RP client entity'lerindeki ses ve parçacık kimlikleri de
kontrol ediliyor (3196 kontrol). Dışarıdan alınan Mutant Warden beş warden sesi
ve bir parçacık referansı taşıyor; biri yanlış yazılsa sessizce hiçbir şey
olmazdı.

## v1.33.0 — Mutant Warden'ın kendi modeli

Mutant Warden vanilla Warden'ın 2 kat büyütülmüşüydü: dev ama tanıdık. Artık
kendi modeli var — 11 kemik, 20 kutu, 11 animasyon (yürüme, saldırı, kükreme,
ses patlaması, toprağa gömülme, ürperme…) ve altı katmanlı doku: gövde,
biyolüminesan katman, iki leke katmanı, tendriller, kalp.

Model üçüncü tarafa ait (`ibu_craft:mutant_warden`); kaynağı ve yeniden dağıtım
koşulu `bedrock/custom/KAYNAKLAR.md` içinde yazılı.

**Kimlikler `stnt_` önekiyle yeniden adlandırıldı.** Özgün paket de aynı
tablette kuruluysa `geometry.mutant_warden` ve `animation.mutant_warden.*` iki
kez tanımlanır ve hangisinin kazandığı belirsiz olur. Vanilla adları
(`controller.render.warden*`, `controller.animation.warden.*`, materyaller)
değiştirilmedi — onları Minecraft kendi kaynağından verir.

**Ölçekler yeniden hesaplandı.** Yeni model 3,69 blok, vanilla Warden 2,9 —
yani 1,27 kat uzun. Boss ölçeği 2,0'dan **1,6**'ya indi (aynı görünen boy,
~5,9 blok; 2,0 kalsaydı 7,4 bloka çıkıp kapalı alanda tavana girerdi). Morph
ölçeği 1,5'ten **1,15**'e indi: 1,5'te oyuncu 5,2 blok oluyor ve üçüncü şahıs
kamerası modelin içinde kalıyordu. Dönüşünce artık boss ile aynı model
görünüyor, biyolüminesan katman da dahil.

**Denetim iki yerde sıkılaştı.** (1) `OWN_GEOS`/`OWN_TEXS` artık ad kalıbından
değil, RP'de gerçekten duran dosyalardan türetiliyor — eskiden
`geometry.<mob id>` kalıbına uyan her ad, dosya olmasa bile kabul ediliyordu.
(2) Yeni `check_client_entities()`: `RP/entity/*.json` içindeki her geometry /
doku / materyal / animasyon adının çözülmesi ve `scripts.animate` adımlarının
`animations` tablosunda tanımlı olması kontrol ediliyor. Dışarıdan varlık
alırken tek bir yerde yeniden adlandırmayı unutmak modeli sessizce görünmez
yapar; bu denetim tam onu yakalar. Toplam 3189 kontrol.

## v1.32.0 — Ejderha Nefesi, dönüşünce isim gizleme

**Ejderha Nefesi.** Sağ tıkla — baktığın yere ejderhanın mor nefesi saçılır.
5 blok yarıçapında bir bulut, 12 saniye kalıcı, içindeki her canlının saniyede
**1 tam kalbini** götürür. Bedrock'ta `applyDamage(1)` yarım kalp götürür, o
yüzden hasar 2 ve vuruş aralığı tam bir saniye; denetim hasarın çift sayı
olmasını ve ipucundaki kalp sayısıyla uyuşmasını kontrol ediyor. Bulut sahibini
de yakar — vanilla ejderha nefesi de öyle — ve ipucu bunu yazıyor. Eşya ve
tecrübe küreleri etkilenmez. Oyuncu başına 3 saniye bekleme var, yoksa çocuk
tabletin üzerine yirmi bulut yığar.

**Dönüşünce isim etiketi gizleniyor.** Creeper gibi görünüp tepende adın
yazarsa dönüşümün anlamı kalmıyordu. Dönüşünce etiket boşaltılıyor, insana
dönünce geri konuyor. Karşılaştırmalı yazılıyor (her turda `nameTag !== hedef`
ise yazılır), yani oyuncu dönüşmüşken çıkıp girse de kendini toparlıyor.

**Diğer oyuncuların dönüşümü görmesi zaten çalışıyor.** `st:morph` özelliği
`client_sync: true` ile tanımlı, yani değeri tüm istemcilere gidiyor; dönüşüm
render controller'ları da `!variable.is_first_person` koşuluyla, yani başka
birinin ekranında senin bedenini çizen dal. Kod tarafında eksik yok. *Bu
doğrudan gözlemle değil paket tanımından okundu; iki tablette karşılıklı
denenmedi.*

## v1.31.0 — paketin kendi boss'larına dönüşüm

Dönüşüm listesi 83 vanilla mob'u kapsıyordu ama paketin **kendi** mob'larını
kapsamıyordu: çocuk Dev Creeper'ı çağırıp asayı ona dokundurduğunda "Bu
yaratığa dönüşülemiyor" yazıyordu. Kendi çağırdığı boss'a dönüşememek, listenin
en çok denenecek dört girdisinin eksik olması demekti. Menüye beşinci bir sekme
geldi — **Süper TNT**: Dev Creeper, Dev Zombi, Mutant Warden, Ender Send.

**Boyutlar bilerek küçültüldü.** Çağrılan boss'ların kendi ölçekleri 2,0-3,2;
morph'ta 1,4-2,0 kullanıldı. Üçüncü şahıs kamerası oyuncuya sabit uzaklıkta
durur, ölçek büyüdükçe kamera modelin *içinde* kalır ve çocuk kendi karnına
bakar. Dev Creeper morph'u yine de vanilla creeper morph'unun iki katı.

**Güçler vanilla eşinden sert.** Yetenek tablosu artık mob başına parametre
taşıyor (`pow`); vanilla morph'lar bu alanı boş geçtiği için eski dengeleri
aynen korunuyor.

| Morph | Çömelince | Pasif |
|---|---|---|
| Dev Creeper | 6 yarıçapında patlama, **blok kırar** (vanilla creeper: 3, kırmaz) | — |
| Dev Zombi | yer sarsıntısı: 7 blokta 10 hasar + savurma | dayanıklılık II |
| Mutant Warden | ses saldırısı 34 blok / 22 hasar (vanilla warden: 24 / 14) | dayanıklılık II |
| Ender Send | 96 bloka ışınlanma (vanilla enderman: 48) | dayanıklılık II |

**Dönüşünce yetenek eylem çubuğunda yazıyor.** Önceden yalnız "Dönüştün!"
yazıyordu; 87 mob'un hangisinin ne yaptığını çocuğun denemeyle bulması
gerekiyordu. Artık "Mutant Warden oldun! çömel → ses saldırısı" gibi. Metin
yetenek tablosundan **üretiliyor**, elle yazılmıyor — yeni morph eklendiğinde
ipucu kendiliğinden doğru olur. Blok kıran güçler bunu ayrıca söylüyor
("blok kırar!"), çünkü çocuk kendi evini havaya uçurmadan önce bilmeli.

**Yan bulgu — on görsel efekt hiç görünmüyormuş.** Ayrı commit'te düzeltildi:
`minecraft:portal_particle` ve `minecraft:end_rod` diye parçacık yok (doğruları
`mob_portal` ve `endrod`), dolayısıyla Yerçekimi/Zaman TNT, Portal Tabancası,
Kara Delik, Yer Değiştirme, küçültme/büyütme ve dönüşüm efektleri sessizce boş
dönüyordu. Denetim bunları göremiyordu çünkü kimlikler `spray()` yardımcısına
parametre ve TNT tablosuna `particle=` alanı olarak gidiyor; ikisi de düz
`spawnParticle("...")` desenine uymuyor.

Denetim bu sürümle 2908 kontrole, mutasyon testi 11 mutasyona çıktı.

## v1.30.1 — gözden geçirme düzeltmeleri

Son üç sürümün (83 morph, Can Artırıcı/Ses Saldırısı, Mega Gübre) gözden
geçirilmesinde çıkan altı sorun. Hiçbiri tablette denenmeden bulundu; ikisi
oyunu doğrudan bozuyordu.

**Dev ağaç oyuncuyu diri diri gömüyordu.** Gövde taban yarıçapı 5, çocuk fidana
1-2 blok mesafeden tıklıyor — yani neredeyse her seferinde gövdenin içinde
kalıyordu ve ilk tick'te logların arasında boğuluyordu. Artık büyüme başlamadan
kenara çekiliyor (gövde + 3 blok, ayağının altında zemin olan en yakın
yükseklik); güvenli yer bulunamazsa ağaç hiç büyümüyor ve kaç blok geri
çekilmesi gerektiği yazılıyor. Gövdede delik bırakma seçeneği denenmedi: o da
oyuncuyu 1×1'lik bir cukura hapsediyordu. Yukarıda uçan oyuncu yerinden
edilmiyor — yaprak boğmaz, tehlike yalnız gövde yüksekliğinde.

**Fidan türü hep yok sayılıyordu.** `getBlockFromViewDirection` geçilebilir
blokları atlar; ışın fidanın içinden geçip altındaki toprağa çarpıyordu, bu
yüzden `TREE_WOOD` araması hiçbir zaman tutmuyor ve her ağaç meşe oluyordu.
Artık çarpılan blok zemin kabul edilip fidan bir üst katmanda, üstelik 3×3
içinde aranıyor — çocuk minik fidana tam nişan alamayabilir.

**Dönüşüm Asası boşa tıklıyordu.** Bir mob'a bakarken boşluğa sağ tık hiçbir şey
yapmıyordu; "dokunma olayı devralır" varsayılmıştı ama düşman mob'larda interact
olayı hiç tetiklenmez ve sağ tık bir vuruş da değildir. 15 mob'la nadir olan bu
durum 83 mob'la neredeyse her yerde oluyordu. Artık bakılan mob'a doğrudan
dönüşülüyor.

**Ses Saldırısı yerdeki eşyayı ve tecrübeyi savuruyordu** — çocuk kendi
ganimetini 20 blok öteye uçuruyordu. Artık atlanıyor.

**Ses Saldırısı'nda iki ses üst üste biniyordu.** `mob.warden.sonic_charge`
~1,4 saniyelik bir yükseliş; boom ile aynı tick'te çalınca patlamadan *sonra*
vınlamaya devam ediyordu. Yalnız boom kaldı. Ayrıca 10 tick'lik bekleme
eklendi: her atış 24 adım + 24 varlık taraması demek, hızlı tıklama tableti
yorardı.

**Ağaç blok çeşitleri tek `try` içindeydi.** Yaprak çözümlemesi hata verirse
gövde de düz `setType`'a düşüyordu — ve yaprak için düz `setType` demek
`persistent_bit` olmaması, yani tepenin çürümesi demek. Her biri ayrı `try`.

Ayrıca `bedrock/tools/check_pack.py` eklendi (~2500 denetim) ve
`mutation_test.sh` ile denetimin gerçekten yakaladığı doğrulandı — ayrıntı
`bedrock/README.md` → "Göndermeden önce".

## v1.30.0 — Mega Gübre (100 blokluk dev ağaç)

Bir fidana sağ tıkla: gövdesi **100 blok**, yapraklarıyla **113 blok**
yüksekliğinde, tepesi **45 blok geniş** bir ağaç büyüyor. Fidanın türü ağacın
türünü belirliyor (meşe, huş, ladin, jungle, akasya, kara meşe, kiraz, soluk
meşe, kavak, mangrov); düz toprağa tıklanırsa meşe.

**Neden blok blok kuruluyor.** Vanilla ağaç üretimi bu ölçeğe çıkmaz, o yüzden
ağaç betikten örülüyor: gövde konikleşen bir silindir, dört katta 4-5 dal,
tepesi sinüs profilli bir kabuk. Ölçülen toplam **20 732 blok** (gövde 3 924,
dal 373, yaprak 16 435) — sayılar `bedrock/tools/tree_check.py` çıktısından;
o betik aynı geometriyi kurup blok sayısını, en yoğun katmanı ve süreyi yazar.

**Tek tick'te konmuyor.** 20 bin blok tek karede konsa tablet donardı; iş
aşağıdan yukarı, katman katman, tick başına 400 blok bütçesiyle ilerliyor
(TNT'lerin `transform`/`place` işleriyle aynı desen) — yaklaşık **52 tick,
2,6 saniye**. Yan fayda: çocuk ağacın büyüdüğünü görüyor. Aynı oyuncu ikinci
kez tıklarsa yeni iş başlamıyor.

**Yapraklar kalıcı.** Betikten konan yaprak varsayılan olarak `persistent_bit`
yanlış gelir ve gövdeye 4 bloktan uzaksa **çürür**. Tepe yarıçapı 22 olduğu
için tepenin neredeyse tamamı kaybolurdu; bu yüzden yaprak `setType` ile değil,
`persistent_bit=true` permutation'ı ile konuyor. Gövde blokları da `pillar_axis`
ile yönlendiriliyor — dallar yatay duruyor.

**Tepe havayı eziyor, gövde araziyi.** Yaprak yalnız havanın (ve yaprağın)
yerine konuyor, yani ağacın tepesi araziyi yemiyor; gövde ve dallar ise kaya
dışında her şeyi eziyor, yoksa yamaçta büyüyen ağaç delik deşik kalırdı.

**Tavan kontrolü.** 113 blok boş yer yoksa ağaç **büyümüyor** ve kaç blok
aşağıda denemesi gerektiği yazılıyor. Budayıp yarım ağaç dikmek tooltip'teki
sözü tutmazdı; Nether'de (tavan 127) çoğu yerde zaten sığmaz.

Blok id'leri (fidan → gövde/yaprak) Mojang/bedrock-samples'ın
`metadata/vanilladata_modules/mojang-blocks.json` dosyasından doğrulandı.
Kavağın düz `poplar_leaves`'i yok, `yellow_poplar_leaves` kullanılıyor.

## v1.29.0 — 83 mob'a dönüşüm, Can Artırıcı, Ses Saldırısı

### Her mob'a dönüşülebiliyor (15 → 83)

Dönüşüm Asası artık vanilla Bedrock'un **83 mob'unun** hepsini kapsıyor.
Önceki 15'lik liste elle yazılmıştı ve sadece "tek katmanda temiz render eden"
moblar alınmıştı; warden, koyun, köylü, axolotl gibi çok dokulu olanlar bilerek
dışarıda bırakılmıştı. Bu, oyunda "bu yaratığa dönüşülemiyor" olarak görünüyordu.

**Tablo artık elle yazılmıyor.** `bedrock/tools/gen_morphs.py`,
Mojang/bedrock-samples deposundaki `resource_pack/entity/*.entity.json`
dosyalarını okuyup `build.py`'deki `MORPHS` bloğunu üretiyor. Morph, oyuncuyu
vanilla `geometry`/`texture`/`material` **adlarına** bağlar (dosyalar pakete
konmaz, Minecraft kendi kaynağından verir); tek harf yanlışsa model görünmez
olur ve hata da vermez. Betik seçilen anahtar mob'un tanımında yoksa durur.

Aynı taramada iki eski hata çıktı: **ghast** dokusu `textures/entity/ghast`
yazılmıştı (doğrusu `textures/entity/ghast/ghast`), **domuz** materyali `pig`
yazılmıştı (doğrusu `pig_v3`).

**Çok katmanlı moblar.** Bir morph artık ek `layers` tanımlayabiliyor: her
katman ayrı bir render controller. Bataklık/Buz iskeletinin giysisi, Gıcırdak'ın
gözleri, Bakır Golem'in gözleri, köylünün meslek dokusu böyle geliyor. Blaze'in
kafası ve Kardan Adam'ın bal kabağı için de kemik başına materyal (`mat_parts`)
var. Vanilla animasyon değişkeni isteyen katmanlar (warden'ın nabız gibi yanıp
sönen lekeleri, bakır golemin çiçeği) **alınmadı** — o Molang değişkenlerini
oyuncu varlığı hesaplamaz, katman sabit takılırdı.

**Dönüşüm menüsü.** 83 mob'un hepsine dokunarak ulaşmak imkânsız (ender ejderha,
wither, deniz mobları). Asayı boşluğa sağ tıklayınca kategorili liste açılıyor:
Hayvanlar (32) · Su (12) · Canavarlar (35) · Devler (4). "İnsana dön" listenin
ilk maddesi. Form bazen "UserBusy" ile geri döner (parmak hâlâ basılı) —
o durumda kısa aralıklarla tekrar deneniyor.

**Yetenekler artık sayıya değil mob'a bağlı.** Eskiden `m === 15` gibi sabit
`st:morph` sayıları JS'e elle yazılmıştı; liste büyüyünce o sayılar kayar ve
yanlış mob yanlış gücü alırdı. Yetenek tablosu artık `MORPHS`'tan üretiliyor.
Su mobları su altında nefes alıyor, ateş mobları yanmıyor, uçanlar süzülüyor;
çömelince creeper patlıyor, enderman ışınlanıyor, ghast/blaze ateş topu atıyor,
warden **ses saldırısı** yapıyor.

Eski 15 mob'un `st:morph` sayıları **korundu** — güncellemeden sonra kayıtlı
oyuncu başka bir yaratığa dönüşmesin diye.

### Can Artırıcı — 300 can, 30 dakika

Sağlık İksiri'nin (200 can, 10 dk) büyüğü. Aynı `health_boost` mekaniği,
`heal_amp()` ile türetilen amplifier: (300 - 20) / 4 - 1 = 69.

### Ses Saldırısı — Warden'ın sonic boom'u

Sağ tıkla bakılan yöne ses dalgası: 24 blok, **bloklardan geçer** (vanilla
Warden'da da öyle), önüne çıkan her varlığa 14 hasar verir ve savurur. Her
varlık yalnız bir kez vurulur; yoksa dalganın 24 adımının her birinde tekrar
hasar alırdı. Parçacık `minecraft:sonic_explosion`, sesler
`mob.warden.sonic_charge` + `mob.warden.sonic_boom`. Hasar nedeni `sonicBoom`
bazı sürümlerde bulunmayabilir — o durumda düz hasara düşüyor.

Aynı saldırı, Warden'a dönüşünce çömelme ile de kullanılabiliyor.

## v1.28.0 — Sağlık İksiri (Bedrock'a özel, Java'da yok)

İçince canı 200'e çıkaran iksir. Dev boss'lar (Mutant Warden 500 can, Ender
Send 300 can) karşısında çocuğun elinde bir koz olsun diye eklendi.

**Nasıl çalışıyor.** Bedrock'ta oyuncunun taban azami canı 20'dir ve betikten
doğrudan değiştirilemez; tek yol `health_boost` etkisi. Bu etki seviye başına
+4 can ekler (seviye = amplifier + 1), yani 200 can için amplifier 44. Etki
yalnız azami canı büyütür, mevcut canı doldurmaz — ayrıca yeni azami değer
aynı tick'te okunamıyor. Bu yüzden betik etkiyi verip **bir tick sonra**
`resetToMaxValue()` ile canı tepeye çekiyor.

**Süre 10 dakika, sonra normale döner.** Kalıcı değil: süre bitince azami can
20'ye iner ve mevcut can oraya kırpılır (ölüm olmaz). Tooltip bunu iki dilde
de açıkça yazıyor.

Yeni `kind="drink"` item türü: yeme yerine **içme animasyonu**, doyum 0
(iksir yiyecek değil), yığın 16. Etki sağ tıkta değil `itemCompleteUse` ile
uygulanıyor — yoksa çocuk tıklayıp bırakıyor ve eşya harcanmadan can alıyor.

Simge elle çiziliyor (`symbol_texture`): mantar tıpalı şişe, kırmızı sıvı,
ortada beyaz kalp.

## v1.17 – v1.19.1 — mobil odak, kalite ve içerik

**Morph modülü (kendi, v1.19.0-1.19.1).** Dönüşüm Asası: bir mob'a dokun → o
mob olursun. O sürümde 15 mob vardı ve liste elle yazılmıştı; v1.29.0'da 83'e
çıkarıldı ve üretilir hâle geldi (yukarı bak). Küçülme (st:size) korundu.

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
iPhone/iCloud; `./ctl wifi kur` kablosuz adb kurulumu.

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
  **İlk-şahıs kamerası:** Bedrock'ta göz yüksekliği sabittir; render-ölçek de,
  collision_box da, `minecraft:scale` de onu değiştirmez. POV'u oynatmanın tek
  yolu betikle sürülen `minecraft:free` kamera. v1.40.0'da eklendi, v1.41.0'da
  yanlış teşhisle kaldırıldı, **v1.42.0'da doğru sürülerek geri geldi**:
  `getHeadLocation()` (çömelme), bir ticklik lineer easing (zıplamasın), öne
  öteleme (kendi kafanın içinde kalıp siyah görmeyesin). `check_pack.py` →
  `check_player_control()` üçünü de kilitliyor ve `inputpermission`'ı
  yasaklıyor. Paket hiçbir deneysel bayrak (Beta APIs / Creator Cameras)
  istemez.
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

**Bedrock'a özel (Java'da karşılığı yok):** Sağlık İksiri, Can Artırıcı,
Ses Saldırısı, Mega Gübre, Sahte Elmas Aletler (kılıç/kazma/balta/kürek/çapa).

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
Kurulum: `./ctl deploy tablet` (MediaStore temizliği dahil).
