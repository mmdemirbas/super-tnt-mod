# Family Link Cihazda USB Hata Ayıklama Kurulumu

Samsung Galaxy Tab S10 FE (One UI 7 / Android 15) cihazlarda Google Family Link gözetimi altındaki çocuk hesabıyla USB hata ayıklamayı (ADB) etkinleştirmek için izlenen adımlar. Süreç birden fazla katmanı atlatmayı gerektirir; sıralama önemlidir.

## Kapsam

- **Cihaz:** Samsung Galaxy Tab S10 FE (model `SM-X520`, `gts10fewifi`)
- **OS:** One UI 7 / Android 15
- **Gözetim:** Family Link gözetimli çocuk Google hesabı (cihaz tek kullanıcılı)
- **Ebeveyn cihazı:** iPhone'da Family Link uygulaması
- **Bilgisayar:** macOS, `adb` (`brew install --cask android-platform-tools`)

Diğer ebeveyn cihazlarında (Android Family Link, web `families.google.com`) menü adları değişebilir; mantık aynıdır.

## Engel katmanları

İzin yığını (üstten alta doğru her biri ayrı kapı):

1. **Family Link** — gözetim altında "Geliştirici seçenekleri" menüsü varsayılan olarak gizli.
2. **Android Geliştirici Seçenekleri** — her cihazda build number 7-tap ile manuel açılır.
3. **Samsung Auto Blocker (Otomatik Engelleyici)** — One UI 6+ güvenlik katmanı, USB üzerinden gelen ADB komutlarını ayrıca blokluyor; Family Link'ten bağımsız.
4. **USB hata ayıklama toggle'ı** — Geliştirici Seçenekleri içinde, üst katmanlar açıkken etkinleşir.
5. **Per-host trust** — her bilgisayar için tabletten tek seferlik onay.

Aşağıdaki adımlar bu sırayı takip eder.

## Adımlar

### 1. Family Link'ten ön izin (ebeveyn cihazından)

iPhone'da Family Link uygulaması:

1. Çocuğun profilini aç
2. **Kontroller** sekmesi
3. Aşağı kaydır → **Cihazlar** bölümüne gel
4. İlgili Galaxy Tab S10 FE'ye dokun
5. Cihaz ayarları listesinde **"Geliştirici seçenekleri"** (veya yerelleştirmeye göre "Geliştirici seçeneklerine izin ver" / "Allow developer options") toggle'ını **AÇ**

Bu toggle'ı görmek için Family Link uygulamasının güncel olması lazım. Toggle hâlâ yoksa: ebeveyn cihazında uygulamayı App Store'dan güncelle, veya `families.google.com` üzerinden web arayüzünü dene.

### 2. Tablette Geliştirici Seçenekleri'ni etkinleştir

Tablette:

1. **Ayarlar → Tablet hakkında → Yazılım bilgileri**
2. **Build numarası**'na 7 kez ardı ardına dokun
3. "Artık geliştirici oldunuz" mesajı çıkar
4. Geri → Ayarlar listesinde alta inince **Geliştirici Seçenekleri** görünür

Family Link adım 1'i atladıysa, build number 7-tap "Ebeveyn izni gerekiyor" mesajıyla durur.

### 3. Auto Blocker (Otomatik Engelleyici) — KRİTİK

Bu adım yapılmadan USB hata ayıklama toggle'ı, Geliştirici Seçenekleri'nde **"Otomatik engelleyici tarafından engellendi"** notuyla gri kalır ve açılamaz.

Tablette:

1. **Ayarlar → Güvenlik ve gizlilik → Otomatik Engelleyici (Auto Blocker)**
2. İki seçenekten biri:
   - **A (en kolay)**: Üstteki ana toggle'ı KAPAT
   - **B (daha güvenli)**: Auto Blocker açık kalsın, içinden **"USB kablosu komutlarını engelle"** alt toggle'ını kapat. Diğer korumalar (uygulama yan yükleme engeli, mesaj filtresi vs.) aktif kalır.

USB hata ayıklama gerek olmadığında Auto Blocker'ı tekrar açabilirsin. USB debug toggle'ı açık kalır; Auto Blocker yalnızca yeni ADB komutu yetkilendirmesini engeller, mevcut yetkili host bağlantıları çalışmaya devam eder.

### 4. USB hata ayıklamayı aç

Tablette:

1. **Ayarlar → Geliştirici Seçenekleri**
2. Liste içinde **USB hata ayıklama**'yı bul → **AÇ**
3. "Otomatik engelleyici tarafından engellendi" notu kaybolmuş olmalı

Aynı menüde işe yarayan diğer ayarlar:
- **Varsayılan USB yapılandırması → "Dosya transferi"** — kabloyu her takmada MTP modunu seçmek zorunda kalmazsın
- **USB hata ayıklama yetkilerini iptal et** — herhangi bir host trust'ını sıfırlamak için

### 5. İlk bağlantı + per-host trust

1. Tabletin ekranı açık ve kilidi açık halde USB-C **data kablosuyla** Mac'e bağla (charge-only kablolar Mac tarafından görünmez)
2. Mac'te:
   ```bash
   adb devices -l
   ```
3. Tablette diyalog: **"USB hata ayıklamasına izin ver?"**
   - **"Bu bilgisayardan her zaman izin ver"** kutusunu işaretle
   - **İzin Ver**'e dokun
4. Aynı komut artık tableti `device` (sadece seri numara değil) olarak listelemeli:
   ```
   R5GYC4BG...    device usb:0-1 product:gts10fewifitur model:SM_X520 ...
   ```

`unauthorized` durumda kalırsa:
- Tablette diyalog çıkmamış olabilir → kabloyu çıkar tak, ekran kilidi açık olsun
- Diyalog yine çıkmıyorsa: Geliştirici Seçenekleri → **USB hata ayıklama yetkilerini iptal et** → tekrar `adb devices` → diyalog yeniden çıkar

## Kullanım sonrası

Tüm ayarlar kalıcı. Sonraki bağlantılarda hiçbir şey yapmana gerek yok — kabloyu tak, çalışır.

Yalnız şu durumlarda tekrar müdahale gerekir:
- **Yeni bir bilgisayar** → adım 5'i o bilgisayar için tekrarla
- **Tablet fabrika ayarlarına dönerse** → tüm adımları baştan
- **OS güncellemesi Auto Blocker'ı tekrar açtıysa** → adım 3'ü kontrol et
- **Family Link'ten Geliştirici Seçenekleri toggle'ı kapatıldıysa** → adım 1'i tekrar yap

## Sorun giderme

### `adb devices` cihazı hiç listelemiyor

| Olası sebep | Test / çözüm |
|---|---|
| Charge-only kablo | Bilinen iyi USB-C data kablosuyla dene (örn. tablet kutusundan çıkan veya Apple USB-C) |
| Tablet kilitli takıldı | Kabloyu çıkar, ekran açık ve kilit açık halde tak |
| MTP yerine "şarj" modunda | Üst bildirim panelini aç → "USB" / "Android System" bildirimine dokun → "Dosya transferi" seç. Veya Geliştirici Seçenekleri → Varsayılan USB yapılandırması → Dosya transferi |
| Mac'te `adb` yok | `brew install --cask android-platform-tools` |
| USB hub | Doğrudan Mac portuna tak |

### `adb devices` `unauthorized` gösteriyor

Tablette güvenilir cihaz diyalogunu kabul etmedin veya per-host trust eskidi. Çözüm:

- Tablette **Geliştirici Seçenekleri → USB hata ayıklama yetkilerini iptal et**
- Kabloyu çıkar tak veya `adb kill-server && adb devices` çalıştır
- Diyalog tekrar çıkar → "Her zaman izin ver" işaretle → İzin Ver

### USB hata ayıklama toggle'ı gri / "Otomatik engelleyici tarafından engellendi"

Adım 3'ü atlamışsın. Auto Blocker'ı kapat veya "USB kablosu komutlarını engelle" alt toggle'ını kapat.

### Family Link'te "Geliştirici seçenekleri" toggle'ı yok

- Family Link uygulamasını App Store'dan güncelle
- iPhone Family Link app yerine `families.google.com` web arayüzünden bak (bazı toggle'lar oraya daha hızlı geliyor)
- Toggle bazı yerelleştirmelerde "Add or remove users" / "Hesap ekleme/kaldırma" altında gizli; "More" / "Other" sekmelerine bak

### Tablette build number 7-tap "Ebeveyn izni gerekiyor" diyor

Adım 1 yapılmamış veya senkron olmamış. Family Link'ten toggle'ı aç, sonra tableti yeniden başlat (politika senkronu hızlanır).

### `adb devices` her şey doğru ama tek tablet listede

Birden fazla tablet bağladıysan ve biri görünmüyorsa: o tablette adım 5'i ayrı yap (her tablet için per-host trust ayrıdır). `adb devices -l` ile seri numarasından hangisinin eksik olduğunu görebilirsin.

## İlgili komutlar

```bash
# Bağlı cihazlar (ayrıntılı)
adb devices -l

# Belirli cihaza komut çalıştır (birden fazla bağlıysa)
adb -s <SERIAL> shell ls /sdcard/

# Tek tableti yeniden algıla
adb kill-server && adb start-server && adb devices

# USB yetkilerini sıfırla (tabletten yapılır, bu sadece referans)
# Ayarlar → Geliştirici Seçenekleri → USB hata ayıklama yetkilerini iptal et
```
