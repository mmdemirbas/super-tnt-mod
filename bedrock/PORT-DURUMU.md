# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.11.0** · **100 içerik** portlandı (61 TNT + 24 blok + 15 item).

Java modu 71 TNT + 17 blok + 43 item içeriyor. Aşağıda ne portlandı, ne
portlanamadı — sebepleriyle.

## Portlandı ✅

### TNT (61 / 71)
Patlama, saçma, efekt, hava, zaman, blok yıkma/koyma/dönüştürme, mıknatıs,
takas, anında öldürme, yerçekimi (levitation), lego dönüştürme, ağaç yıkma.
Aile TNT'leri dahil (Zeynep, Abi, Anne, Baba, Bebek).

### Blok (24)
16 renk Lego, hayalet bloklar (içinden geçilir), ayna, ışık bombası,
doğru/yanlış altın plaka (tuzak), zehir toprağı, ? bloğu (rastgele ganimet),
+ TNT'lerin fırlayan varlıkları.

### Item (15)
Acılı Cips, Hız Eşyası, Yıldırım Büyüsü, Kara Delik, Enerji Kristali,
Lazer Kılıcı, Kanca, Dondurucu, Koku Bombası, Among Us Rapor, Delici,
Lav Kristali, Kanlı Kılıç, Kalp Baltası, Gökkuşağı Botları.

## Portlanmadı — teknik sebeple

### Bedrock'ta imkansız (Java-only mekanizma)
| İçerik | Neden |
|---|---|
| Çizim Eşyası (canvas) | Bedrock script arayüzü boyama tuvali sunmuyor — sadece buton/kaydırıcı/liste |
| Ametist zırh (çıkarılamaz) | "Çıkaramama" Java mixin'iyle zorlanıyor; Bedrock zırh yuvasını engelleyemez |
| Eşya Çalmaca | Başka oyuncunun envanterini açma API'si yok |
| Kontrol Kumandası | "T tuşu" özel klavye bağlaması; Bedrock'ta özel tuş yok |

### Zor / düşük öncelik (yapılabilir ama karmaşık)
| İçerik | Durum |
|---|---|
| Küçültme/Büyütme (TNT + top + iksir) | Bedrock oyuncu ölçeğini script'ten değiştiremiyor |
| Komut TNT | Ayar arayüzü gerekiyor (sabit hedefle basitleştirilebilir) |
| Elmas Zırh TNT | Sunucu kapatma — Bedrock'ta karşılığı yok |
| Yürüyen TNT | Özel AI davranışı (JSON behavior ile yeniden yazılabilir) |
| Üreyen / Dağ TNT | Özyineli çoğalma / arazi şekillendirme |
| End/Nether İncisi | Boyutlar arası ışınlama script'ten zor |
| TNT Frizbi, throwable'lar | Fırlatılan mermi varlığı gerekiyor |
| TNT Kapı, Blocker/Şifreli Sandık | Sahip-tabanlı kalıcı durum (yapılabilir, ayrı iş) |
| Portal Silahı + Portal Blok | Eşleşen portal + kalıcı durum |
| Kuruş / 200 TL / Lego item'ları | TNT'ler şimdilik vanilla eşdeğer (kağıt/nugget) saçıyor |

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
