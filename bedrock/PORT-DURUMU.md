# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.12.0** · **115 içerik** portlandı (66 TNT + 26 blok + 23 item).

Java modu 71 TNT + 17 blok + 43 item içeriyor. Aşağıda ne portlandı, ne
portlanamadı — sebepleriyle.

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

## Portlanmadı — teknik sebeple

### Bedrock'ta imkansız (Java-only mekanizma)
| İçerik | Neden |
|---|---|
| Çizim Eşyası (canvas) | Bedrock script arayüzü boyama tuvali sunmuyor — sadece buton/kaydırıcı/liste |
| Ametist zırh (çıkarılamaz) | "Çıkaramama" Java mixin'iyle zorlanıyor; Bedrock zırh yuvasını engelleyemez |
| Eşya Çalmaca | Başka oyuncunun envanterini açma API'si yok |
| Kontrol Kumandası | "T tuşu" özel klavye bağlaması; Bedrock'ta özel tuş yok |

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
| Küçültme/Büyütme (TNT + top + iksir + kilit) | Bedrock oyuncu/mob ölçeğini script'ten değiştiremiyor |
| TNT Kapı, Blocker/Şifreli Sandık | Sahip-tabanlı kalıcı container; Bedrock'ta özel container UI yok |
| Portal Silahı + Portal Blok | Eşleşen çift portal + kalıcı durum |
| Tünel Kazma + Tünel Blok | Dinamik çarpışma sınırlı blok varlığı |

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
