---
title: Kurulum
order: 10
summary: Paketi derleme, tablete kablosuz gönderme, elle .mcaddon kurulumu ve dünyada paketi etkinleştirme.
---

> [!TLDR]
> `python3 bedrock/build.py` paketi üretir; `./ctl deploy tablet` bağlı tabletlere gönderir. Elle kurulumda `.mcaddon` dosyasını Minecraft ile açıp dünyanın ayarlarında iki paketi de etkinleştirin.

## Geliştirici bilgisayarından tablete {#tablet}

```oku-step-flow
{"steps":[{"t":"Paketi üret","b":"`python3 bedrock/build.py` → `SuperTNT.mcaddon` (bedrock/out/ altında). `super_tnt_BP/` ve `super_tnt_RP/` klasörleri bu adımda sıfırdan yazılır; elle düzenlenmez."},{"t":"Tableti bir kez kabloyla tanıt","b":"`./ctl wifi kur` — yeni bir tablet için bir kereliğine USB; sonrası kablosuz."},{"t":"Gönder","b":"`./ctl deploy tablet` bağlı her tablete, `./ctl deploy tablet zeynep` yalnız birine gönderir. Paket boyutu doğrulanır ve Samsung'un dosya kaydı temizlenir; yoksa Dosyalarım `.mcaddon`'ı Minecraft'a değil Play'e yönlendirir."},{"t":"Ne bağlı, bak","b":"`./ctl status`"}]}
```

## Elle kurulum {#elle}

```oku-step-flow
{"steps":[{"t":"Dosyayı cihaza kopyala","b":"`SuperTNT.mcaddon` dosyasını tabletin İndirilenler klasörüne koyun (kablo, iCloud Drive, bir sohbet uygulaması — hepsi olur)."},{"t":"Minecraft ile aç","b":"Dosyaya dokunun; Minecraft açılır ve paketi içe aktarır. Dosya Play Store'a gidiyorsa \"Birlikte aç\" menüsünden Minecraft'ı seçin."},{"t":"Dünyada etkinleştir","b":"Dünyanın ayarlarında Davranış Paketleri ve Kaynak Paketleri altında Super TNT'yi etkinleştirin; iki paket de gerekir."}]}
```

## Sürüm {#surum}

Sürüm numarasının tek kaynağı `build.py` (bedrock/ altında) içindeki `VERSION`; paket manifestosu oradan üretilir. Ne değiştiği `PORT-DURUMU.md` (bedrock/ altında)'de, en yenisi üstte.
