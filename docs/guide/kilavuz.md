---
title: Kılavuz
order: 30
summary: Tarif kalıbı, ateşleme yolları, komutlar ve eşyaların nasıl kullanıldığı.
---

> [!TLDR]
> Çoğu TNT aynı tarifle yapılır: 8 malzeme çevrede, ortada normal TNT. Çakmak, redstone ya da yandaki patlama ateşler; 4 saniye sonra patlar. Hangi malzeme, hangi TNT: [katalogda](katalog.html).

## Tarif {#tarif}

```text
M M M
M T M   →  1 özel TNT
M M M
```

`M` katalogdaki tarif malzemesi, `T` normal TNT. TNT Frizbi, TNT Kapı, Şifreli TNT Sandık ve Portal Silahı kendi tariflerine sahiptir.

## Ateşleme {#atesleme}

```oku-table
{"headers":["Yol","Nasıl"],"rows":[["Çakmak","TNT'ye çakmakla sağ tık"],["Redstone","Kaldıraç, buton, basınç plakası, redstone tozu"],["Zincirleme","Yakındaki bir patlama onu da ateşler; Patlayıcı Kum da zincire girer"]]}
```

Ateşlenince blok kaybolur, yerine fiziksel bir TNT varlığı doğar: yukarı sıçrar, düşer, yuvarlanır, yanıp söner. 4 saniye sonra patlar.

## Komutlar {#komutlar}

Yaratıcı modda ya da hile açıkken:

```text
/give @s stnt:diamond_tnt
/give @s stnt:zeynep_tnt
/give @s stnt:portal_silahi
/give @s minecraft:flint_and_steel
```

Kimlikler katalogdaki isimlerin İngilizce/Türkçe karşılığıdır; tam liste `build.py` (bedrock/ altında) içindeki listelerde.

## Eşyalar {#esyalar}

```oku-table
{"headers":["Eşya","Nasıl kullanılır"],"rows":[["Portal Silahı","Sağ tık: pembe portal. Tekrar sağ tık: yeşil portal. Birine gir, diğerinden çık."],["Dönüşüm Asası","Elinde tut, bir kılık seç: hayvan, canavar, dev ya da blok. Hareketi ve gücü o canlınındır."],["Küçültme / Büyütme Topu","Ayağının dibine at: küçülür ya da büyürsün. Normal Boyut Topu geri alır."],["Kontrol Kumandası","Elinde tut, 25 blok yarıçapındaki canlılar bir buçuk dakika donar."],["Craft Baltası","İlk kırdığın blokta konum kaydedilir, ikinci kırdığında arasına duvar örülür."],["Kanca","Fırlat; bloğa saplanır ve seni oraya çeker."],["Lazer Kılıcı","Sağ tık: 9 blok lazer, büyük hasar, sonra soğuma."],["Blok Kılığı","Elindeyken bir blok kır: o bloğa dönüşürsün."],["Among Us Rapor","Bir mob ya da oyuncuya sağ tık: anında bitirir."]]}
```

Her eşyanın ve TNT'nin oyun içi ipucu, üzerinde yazar; [katalog](katalog.html) aynı metinleri toplu verir.

## Tablet kısayolları {#kisayollar}

```oku-table
{"headers":["Dokunuş","İşlev"],"rows":[["Sol sanal çubuk","Yürü"],["Ekrana dokun","Blok yerleştir / eşya kullan"],["Basılı tut","Blok kır / saldır"],["Zıpla tuşuna çift dokun","Yaratıcı modda uç"],["Sohbet simgesi","Komut yaz (`/` ile başlar)"]]}
```
