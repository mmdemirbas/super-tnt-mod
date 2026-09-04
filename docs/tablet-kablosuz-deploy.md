# Tabletlere Kablosuz Deploy

Add-On'u tabletlere kablo takmadan, aynı wifi üzerinden göndermek için kurulan
düzen. Kurulum 2026-09-04'te iki Galaxy Tab S10 FE (`SM-X520`, Android 16 /
API 36) üzerinde yapıldı ve çalışır halde bırakıldı.

Kablolu ilk kurulum (Family Link, Auto Blocker, USB hata ayıklama) ayrı bir
belgede: `family-link-usb-debug-setup.md`. Bu belge onun **üstüne** gelir —
kablosuz çalışmanın ön koşulu, tabletin bir kez kabloyla yetkilendirilmiş
olmasıdır.

## Günlük kullanım

```bash
python3 bedrock/build.py                       # paketi üret (VERSION'ı artır!)
bedrock/install.sh bedrock/out/SuperTNT.mcaddon   # iki tablete birden gönder
```

`install.sh` kablosuz bağlantıyı kendisi kuruyor; ayrı bir komut çalıştırmana
gerek yok. Kablo takılı olsun ya da olmasın aynı komut çalışır.

Gönderdikten sonrası elle: tablette **Dosyalarım > İndirilenler >
SuperTNT.mcaddon**'a uzun bas, "Sununla aç" > **Minecraft**. Sonra Dünya
Ayarları'nda Davranış + Kaynak paketlerinin ikisini de etkinleştir.

Yardımcı komutlar:

```bash
bedrock/tablet-wifi.sh durum    # ne bağlı, hangi tablet, kablosuz mu USB mi
bedrock/tablet-wifi.sh bagla    # sadece bağlan, deploy etme
bedrock/tablet-wifi.sh kes      # kablosuz bağlantıları bırak
bedrock/tablet-wifi.sh kur      # YENİ tablet — bir kereliğine KABLO ister
```

## Nasıl çalışıyor

Android 11+ "Kablosuz hata ayıklama" (wireless debugging) kullanılıyor:
TLS'li, cihaz başına yetkilendirilmiş, ve ayarı yeniden başlatmaya dayanacak
şekilde `settings` veritabanında saklı.

```
settings put global adb_wifi_enabled 1     # tablette, bir kereliğine
```

Tablet bunu açınca ağda bir Bonjour (mDNS) kaydı yayınlar:

```
adb-R5GYC4BGAQW-ffM6cn._adb-tls-connect._tcp   ->  Android-2.local:46341
adb-R5GYC4BGJJZ-LhvfAg._adb-tls-connect._tcp   ->  Android.local:34933
```

`tablet-wifi.sh` her çalıştırmada bu kaydı yeniden keşfedip `adb connect`
yapıyor. Hiçbir adres dosyada saklanmıyor.

## Ölçülen dört tuzak

Bunlar kurulum sırasında karşılaşıldı; çözümleri script'in içinde.

### 1. `adb mdns services` yanlış IP veriyor

adb'nin kendi mDNS istemcisi iki tableti de **aynı** adresle listeliyordu:

```
adb-R5GYC4BGJJZ-LhvfAg  _adb-tls-connect._tcp  192.168.68.151:34933   <- yanlış
adb-R5GYC4BGAQW-ffM6cn  _adb-tls-connect._tcp  192.168.68.151:46341
```

`.151` aslında AQW'nin adresi; JJZ `.107`'de. O adrese bağlanmak
`Connection refused` veriyor.

**Çözüm:** keşif macOS'un kendi Bonjour aracına (`dns-sd`) yaptırılıyor. O
doğru veriyor ve üstelik IP yerine `.local` adını döndürüyor.

### 2. Port her açılışta değişiyor

Kablosuz hata ayıklama rastgele port seçiyor. Ölçülen: 44853 → 46341 (aynı
oturumda servis oturunca), sonra sabit kaldı. Yeniden başlatınca başka olur.

**Çözüm:** port hiçbir yerde saklanmıyor, her çalıştırmada mDNS'ten yeniden
okunuyor.

### 3. Transport'un USB mi kablosuz mu olduğu ADINDAN anlaşılmaz

Kablosuz transport **iki** biçimde gelebilir:

```
Android-2.local:46341                            <- iki nokta VAR
adb-R5GYC4BGAQW-ffM6cn._adb-tls-connect._tcp     <- iki nokta YOK
```

"İçinde `:PORT` varsa kablosuzdur" kuralı ikincisini **USB sanır**. Zararı
teorik değil: USB'ye özel bir komut (`adb tcpip` gibi) kablosuz bir cihaza
çekilirse `adbd` yeniden başlar ve bağlantı düşer. Aynı hata Bilgebaykuş
projesindeki `ctl` sürücüsünde de çıktı ve orada da düzeltildi.

**Çözüm:** ada bakma. `adb devices -l` USB transport'lara ` usb:<yol>` alanı
koyuyor; tek güvenilir işaret bu.

```bash
adb devices -l | awk 'NR>1 && $2=="device" && $0 ~ / usb:/ {print $1}'   # USB
```

(`emulator-*` girdilerinin de `usb:` alanı yoktur, ayrıca elenir.)

### 4. `adb shell` stdin'i yutuyor

```bash
for T in $(adb devices ...); do
  adb -s "$T" shell getprop ro.serialno     # <- döngünün geri kalanını yer
done
```

Bu döngü ilk cihazdan sonra sessizce duruyor — hata yok, sadece eksik çıktı.
Kurulum sırasında "bağlantılar kopuyor" sanıldı; kopmuyordu, döngü okunacak
satırları `adb shell`'e kaptırmıştı.

**Çözüm:** her `adb shell` çağrısına `</dev/null`. Hem `tablet-wifi.sh` hem
`install.sh` böyle.

## Aynı tablete iki kez gönderme sorunu

Kablo takılıyken kablosuz da bağlıysa aynı fiziksel tablet `adb devices`
içinde **iki transport** olarak görünür:

```
R5GYC4BGAQW              device    <- USB
Android-2.local:46341    device    <- kablosuz, AYNI tablet
```

`install.sh` artık transport'ları `ro.serialno` ile gruplayıp her tablet için
tek transport seçiyor — varsa kablosuz olanı, çünkü asıl yol o.

## Bakım

| Durum | Ne yapmalı |
|---|---|
| Yeni tablet eklendi | Bir kereliğine kabloyla: `tablet-wifi.sh kur` |
| Tablet bulunamıyor | Ekranı aç, aynı wifi'da mı bak. Misafir ağı çalışmaz (cihaz yalıtımı) |
| Fabrika ayarı / OS güncellemesi | `family-link-usb-debug-setup.md` adımlarını gözden geçir, sonra `kur` |
| Router değişti / IP değişti | Bir şey yapma. Bağlantı IP ile değil `.local` adıyla kuruluyor |

## Açık kalan iki nokta

**Yeniden başlatma sınanmadı.** `adb_wifi_enabled` kalıcı bir ayar, yani
tabletler yeniden başlayınca kablosuz hata ayıklamanın açık kalması bekleniyor
— ama bu **denenmedi**. Tabletler bir kapanıp açıldığında `tablet-wifi.sh
durum` ile doğrula. Kablosuz kayıt yayında değilse ayar kapanmış demektir; o
zaman bir kereliğine kabloyla `tablet-wifi.sh kur`.

**Port 5555 bilerek açık, dokunma.** İki tablette de
`service.adb.tcp.port=5555` ayarlı. Bu bir kaza değil: tabletleri kablosuza
ilk açan **Bilgebaykuş** projesindeki `ctl` sürücüsü iki katmanı birden
kurdu — `adb tcpip 5555` (hemen çalışır, yeniden başlatmada gider) ve
`adb_wifi_enabled` (kalıcı olması beklenen asıl yol). 5555 kasıtlı bir yedek
katman; bu repodaki düzen onu kullanmıyor ama kapatılması da istenmedi.

## Aynı tabletleri iki repo yönetiyor

| Repo | Komut | Ne gönderir |
|---|---|---|
| Bilgebaykuş | `./ctl deploy tablet all` | kendi içeriği |
| Bu repo | `bedrock/install.sh` | `SuperTNT.mcaddon` |

İkisi de aynı iki tablete, aynı kablosuz altyapı üzerinden bağlanıyor. Tablet
kod adları Bilgebaykuş tarafında: `zeynep` = `R5GYC4BGAQW`, `omer` =
`R5GYC4BGJJZ`. Kablosuz kurulum ortak; biri kurunca öteki de kullanır.
