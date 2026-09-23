# Emülatörde Minecraft: doğrulama ve ekran görüntüsü

Add-On'u tablete göndermeden bir Android emülatöründe açıp komutla sahne kurmak,
TNT'yi ateşlemek ve kare kare görüntü almak için kurulan düzen. Kurulum
2026-09-22'de yapıldı ve çalışır halde bırakıldı: `./ctl deploy android` paketi
gönderir, `bedrock/tools/shots.sh` sahneyi sürer.

`test.sh` betiğin çalıştığını ve paketin tutarlı olduğunu kanıtlar; bir
patlamanın *doğru göründüğünü* ancak oyun gösterir. Bu düzen o boşluğu
kapatır — README ve açılış sayfasındaki oyun içi kareler de buradan çıktı.

## AVD: `mc_tablet`

Android Studio > Device Manager ile oluşturuldu; `~/.android/avd/mc_tablet.avd/config.ini`
içinde önemli olanlar:

| Ayar | Değer | Neden |
|---|---|---|
| Sistem imajı | `android-35 / google_apis_playstore_tablet / arm64-v8a` | Minecraft yalnızca Play Store'dan kurulur; Play Store'suz imajda (`bb_tablet`) lisans denetimi geçmez. |
| Cihaz | `pixel_tablet`, 2560×1600 | Çocukların tabletine yakın oran; ekran görüntüleri 16:10 çıkar. |
| `hw.gpu.mode` | `swiftshader_indirect` | Ana makine GPU köprüsü paketin sıkıştırılmış dokularını (`glCompressedTexImage2D unsupported format`) reddedip **siyah kare** veriyor. Yazılım çizimi yavaş (~1 kare/sn ekran yakalama) ama doğru. |
| `hw.ramSize` | 4096 | Minecraft + Play hizmetleri için; daha azı denenmedi. |
| `disk.dataPartition.size` | 8G | Minecraft ~1 GB, Play güncellemeleri üstüne; daha azı denenmedi. |
| `hw.camera.front/back` | `webcam0` | Google hesabı girişi bir adımda selfie istiyor; atlanabiliyor, ama kamera eşliyse o adım da geçilebilir. |

`./ctl deploy android` bu AVD'yi varsayılan alır (`AVD=<ad>` ile değişir),
`-no-boot-anim -gpu swiftshader_indirect` ile başlatır, açılışı ve paylaşılan
depolamanın hazır olmasını bekler.

- **`-gpu` açıkça verilmeli.** `config.ini`'deki `hw.gpu.mode` komut
  satırından başlatmada uygulanmadı (2026-09-23): emülatör Mac'in GPU'suna
  düştü (`dumpsys SurfaceFlinger | grep GLES` → "Apple M1 Max") ve Minecraft
  siyah ekranda kaldı. Doğru olan satırda "SwiftShader" yazar.
- **`boot_completed` yetmez.** Hemen ardından gelen `adb push`
  `secure_mkdirs() failed` ile düştü; `ctl` artık `/sdcard/Download`
  okunabilene kadar bekler.
- **Emülatör paylaşılmaz.** Başka bir proje (bilgebaykus) aynı anda
  `emulator-5554`'e kendi uygulamasını açınca Minecraft arka plana düşüp grafik
  bağlamını kaybetti. Her proje kendi AVD'sini açar (`bb_tablet` →
  `emulator-5556`) ve `adb -s` ile yalnız onu hedefler; `ctl` "çalışan ilk
  emülatörü" alır, o yüzden iki proje aynı anda çalışıyorsa hangi seri
  numarasının kimde olduğuna bakın.

Bir kez elle yapılanlar: Play Store'a Minecraft'ın **satın alınmış olduğu**
Google hesabıyla giriş, Minecraft kurulumu, Microsoft hesabı olmadan "Play"
ile açılış; yaratıcı, hileler açık, **düz** bir dünya (Create New World >
Creative > Cheats > Flat, Behavior packs > Super TNT). Dünya kapatılmaz,
betik onun içinde çalışır.

## Emülatörü elle sürmek

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
~/Library/Android/sdk/emulator/emulator -avd mc_tablet -no-boot-anim &   # aç
$ADB emu kill                                                            # kapat; qemu çıkana kadar bekle
```

Aynı AVD'yi ikinci kez açmak `FATAL: multiple emulators with the same AVD`
verir — `emu kill` sonrası `pgrep -f qemu-system` boşalana kadar beklemek
gerekir.

**Dokunma:** `input tap` 90 ms basıyor ve Minecraft bunu görmüyor;
`input swipe x y x y 220` (aynı noktada 220 ms) her yerde çalışıyor.

**Klavye:** Gboard `input text` ile gelen metni yeniden sıralıyor
(kelimeleri karıştırıyor, `/`'ı taşıyor). Bütün klavyeler kapatıldı:

```bash
$ADB shell ime list -s | while read -r i; do $ADB shell ime disable "$i"; done
```

Sonrasında sohbet alanına odaklanıp kelime kelime `input text` (boşluk `%s`)
ve `keyevent 66` (Enter) ile komut gidiyor; `shots.sh cmd` bunu yapar.

**Paketi elle içe aktarmak** gerekirse: `.mcaddon`'u `Download/`'a `adb push`
edip Files uygulamasında dosyaya dokunmak. `am start` ile `content://`
niyetleri izin hatası veriyor.

## Sahne betiği: `bedrock/tools/shots.sh`

```bash
S="bash bedrock/tools/shots.sh"
$S stage                                        # düz zemin, hava açık, canlı yok, HUD gizli
$S row gokkusagi_tnt lego_tnt buz_tnt           # z=8 hattına 2 aralıkla dizer (stnt: öneki eklenir)
$S cam 0 -52 -8 0 -58 8                         # serbest kamera: konum, bakılan nokta
$S line -6 6 8                                  # sıranın 3 blok üstüne vanilya TNT hattı, ortasından ateşler
$S burst hero 36 0.2                            # bedrock/out/shots/hero/f01..f36.png
$S solo lego_tnt                                # temizle, tek TNT, yakın alçak kamera, ateşle, çek
$S cmd "/say merhaba"                           # tek komut
```

Düz dünyada zemin y=−61 (çim), bloklar y=−60'a konur. Kamera için
`/camera @s set minecraft:free pos … facing …`; `/hud @s hide all` ile HUD
gizlenir ama sağ üstteki üç düğme kalır — kareler kullanılmadan üstten
~80 px kırpılır.

Paketin TNT'si `/summon stnt:*_primed` ile **patlamaz**: yalnızca betiğin
`ignite()` yolu (çakmak, betiğin izlediği redstone, başka bir patlamanın
vurması — F3) tetikler; `/setblock` ile konan blok izlenmediğinden redstone da
işe yaramaz. Bu yüzden ateşleme vanilya TNT ile yapılır. `fire` tek bir vanilya
TNT çağırır; komşular zincirle tutuşur ama her hop bir fitil (FUSE, ~4 sn)
sürer ve ilk efekt sonrakileri örter. `line` sıranın 3 blok üstüne bir hat
vanilya TNT döşer: zincir 1 sn'de hattı bitirir, altındaki paket blokları hep
birlikte ateşlenir. Havadaki patlama da zemini oyar (3,5 blok yükseklikten
denendi, krater açıldı); krater kaçınılmaz, kadraj onu saklar — `solo` bu
yüzden alçak ve yakın kamerayla çeker.

`/fill` yalnızca yüklü parçalarda çalışır; `stage` önce
`/tickingarea add … stage` ile sahneyi yüklü tutar ve 32768 blok sınırı için
katman katman temizler. `kiyamet_tnt` sahnedeki her şeyi taşa çevirip yağmur
başlattığı için grup çekimlerine alınmaz; tek başına çekilir.
