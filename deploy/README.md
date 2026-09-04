# deploy/

`./ctl` sürücüsünün kullandığı yardımcı. **Normalde buradaki dosyayı doğrudan
çağırmazsın** — tek giriş noktası `./ctl`.

```bash
./ctl deploy tablet            # derle + bağlı tabletlere gönder (kablosuz)
./ctl deploy tablet zeynep     # yalnız bir çocuğun tableti
./ctl deploy iphone            # iCloud Drive kanalıyla
./ctl wifi kur                 # yeni tablet: bir kereliğine kablo
./ctl status                   # ne bağlı
```

## deploy.sh

Paketi cihaza gönderir, boyutunu doğrular ve Samsung MediaStore kaydını
temizler (bu temizlik olmadan Samsung "Dosyalarım" `.mcaddon`'ı Minecraft'a
değil Google Play'e yönlendiriyor).

`./ctl` cihazları seçip her biri için `deploy.sh --no-build <transport>`
çağırıyor; kablosuz keşif, kod adı çözümleme ve aynı tableti iki kez
göndermeme `ctl`'de.

Kablosuz kurulumun tamamı: `docs/tablet-kablosuz-deploy.md`.
