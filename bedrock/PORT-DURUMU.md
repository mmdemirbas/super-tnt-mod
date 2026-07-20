# Super TNT — Bedrock Port Durumu

Son sürüm: **v1.13.0** · **128 içerik** portlandı (68 TNT + 34 blok + 26 item).

Java modu 71 TNT + 17 blok + 43 item içeriyor. Aşağıda ne portlandı, ne
portlanamadı — sebepleriyle.

## v1.13.0 — çocukların sevdiği 3 özellik eklendi

- **Küçültme / Büyütme (oyuncu)** — Küçültme TNT + Büyütme TNT + Küçültme
  Topu + Büyütme Topu + Normal Boyut Topu. Oyuncu 5 kademe arasında
  gerçekten küçülüp büyür (görsel + çarpışma kutusu). Kalp TNT boyutu
  normale döndürür. **Yöntem:** `player.json` override — Mojang'ın vanilla
  RP client_entity'sine kademeli render-Molang ölçeği + BP `st:size`
  özelliği + kademe başına `collision_box`. `minecraft:scale` oyuncuda
  çalışmadığı için (script'ten "event does not exist" hatası) MorphX'in
  kanıtlanmış render-Molang yöntemi kullanıldı.
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
| TNT Kapı, Blocker/Şifreli Sandık | Sahip-tabanlı kalıcı container; Bedrock'ta özel container UI yok |
| Portal Silahı + Portal Blok | Eşleşen çift portal + kalıcı durum |
| Tünel *Blok* (kalıcı oyulmuş) | Çarpışma tek AABB — katı duvar + geçilebilir delik imkansız (tünel *item* olarak Delici Aleti'nde var) |

**v1.13.0'da çözülenler (eski "hâlâ portlanmadı" satırları):**
- Küçültme/Büyütme — vanilla `player.json` override + render-Molang ile
  çözüldü (eski not "script'ten ölçek değiştirilemiyor" yanlıştı; oyuncuda
  `minecraft:scale` çalışmıyor ama render-Molang çalışıyor).
- Tünel Kazma — Delici Aleti item'ı ile portlandı (blokları deler).

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
