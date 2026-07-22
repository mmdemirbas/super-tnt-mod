# Aktarım (deploy)

Super TNT mobil sürümünü tabletlere, iPhone'a ve emülatörlere aktarma araçları.

## Hızlı kullanım

```bash
deploy/deploy.sh              # build eder + bağlı tüm tabletlere gönderir
deploy/deploy.sh --no-build   # yeniden build etmeden gönder
deploy/deploy.sh --icloud     # iPhone/iPad için iCloud Drive'a da bırak
deploy/deploy.sh --emu        # Android emülatörlerini de dahil et
deploy/deploy.sh R5GYC4BGJJZ  # sadece tek cihaz (USB seri no ya da ip:port)
```

Gönderdikten sonra tablette son adım elle: **Dosyalarım > İndirilenler >
SuperTNT.mcaddon** (uzun bas > "Sununla aç" > Minecraft). Sonra Dünya
Ayarları'nda hem Davranış hem Kaynak paketini etkinleştir.

## Kablosuz (WiFi) aktarım — kablo takmadan

Aynı WiFi/LAN'da olman yeterli. Bir kez kur:

```bash
deploy/wifi-setup.sh          # eşleştirme sihirbazı (adım adım anlatır)
```

Tablette: Ayarlar > Geliştirici seçenekleri > **Kablosuz hata ayıklama** > Aç >
"Eşleştirme koduyla eşleştir". Script IP+kodu sorar. Kurunca kabloyu çıkar;
`deploy/deploy.sh` artık kablosuz da gönderir.

Tablet yeniden başlarsa yeniden bağla:

```bash
deploy/wifi-setup.sh --list
```

## iPhone / iPad

`--icloud` ile `.mcaddon` iCloud Drive'a kopyalanır. iPhone'da: **Dosyalar >
iCloud Drive > SuperTNT > SuperTNT.mcaddon** (dokun > Minecraft). AirDrop'un
resmî komut satırı olmadığı için iCloud en güvenilir yol.

## Neden otomatik import yok

Minecraft'ı `adb` ile açıp dosyayı otomatik içe aktarmak kırılgan (ikon
sırası/dil/çözünürlük değişince bozulur). Elle import her zaman çalışıyor;
script gönderme + doğrulamayı yapar, son dokunuşu sana bırakır.
