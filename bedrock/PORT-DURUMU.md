# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.30.1** · Mobil sürüm ana odak; Super TNT tek başına yeterli
olacak şekilde geliştiriliyor (MorphX / mutant paketine bağımlılık yok).

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
iPhone/iCloud; `deploy/wifi-setup.sh` kablosuz adb kurulumu.

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
  **İlk-şahıs kamerası:** Render-ölçek ve collision_box göz yüksekliğini
  DEĞİŞTİRMEZ (Bedrock'ta göz ~1.62 blokta sabit). `minecraft:scale` oyuncuya
  uygulanabilir ama o da model+hitbox'ı ölçekler, gözü değil. POV'u gerçekten
  oynatmak MÜMKÜN ama deneysel **Script Kamera** sistemi gerekir
  (`player.camera.setCamera`/`attachToEntity`, her tick). Çalışan örnek: Coco &
  Vici "True POV Size Changer". Bedeli: dünyada Beta APIs + Experimental
  Creator Cameras açık olmalı ve dönüşüm boyunca oyuncu scripted üçüncü-şahıs/
  orbit kameraya kilitlenir. İstenirse ayrı bir özellik olarak eklenebilir;
  şu an ölçek sadece görsel.
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
Ses Saldırısı, Mega Gübre.

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
Kurulum: `bedrock/install.sh` (MediaStore temizliği dahil).
