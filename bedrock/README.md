# Super TNT Mod — Bedrock Add-On

Java (Fabric) modunun Bedrock sürümü. Tabletlerde çalışır.

```bash
python3 bedrock/build.py     # -> bedrock/out/SuperTNT.mcaddon
```

Üretilen `super_tnt_BP/` ve `super_tnt_RP/` klasörleri **çıktıdır**, elle
düzenlenmez — `build.py` her çalıştığında sıfırdan yazılır. Değişiklik
`build.py` içindeki `TNTS` listesine yapılır.

## İçindeki 12 TNT

| Blok | Ne yapar | Tarif malzemesi |
|---|---|---|
| Elmas TNT | 10 güç patlama | elmas |
| Zıplatan TNT | 12 blok yarıçapındaki her şeyi fırlatır | slime topu |
| Zeynep TNT | 50 boş kağıt saçar | kağıt |
| Çoklu Zeynep TNT | "Zeynep" isimli 15 köylü | zümrüt |
| Abi TNT | "Abi" isimli 12 köylü | zümrüt |
| Anne TNT | "Anne" isimli 12 köylü | zümrüt |
| Baba TNT | "Baba" isimli 12 köylü | zümrüt |
| Bebek TNT | "Bebek" isimli 15 yavru köylü | zümrüt |
| Bulut TNT | Yağmur başlatır | beyaz yün |
| Ay TNT | Gündüzü geceye çevirir | glowstone tozu |
| Kalp TNT | 20 blokta emme + yenilenme | altın külçe |
| Buz TNT | 14 blok yarıçapını buz/karla kaplar | packed ice |

Tarif deseni Java'dakiyle aynı: 8 malzeme + ortada vanilla TNT.

Davranış sayıları (yarıçap, adet, süre) `src/main/java/com/supertntmod/entity/*TntEntity.java`
dosyalarından okundu, uydurulmadı.

## Nasıl ateşlenir

**Çakmakla sağ tık.** Blok yerinde 4 saniye duman çıkarır, sonra patlar.

## Bilinen farklar (Java sürümüne göre)

**F1 — Fitil yerinde yanar.** Java'da TNT ateşlenince fırlayan, zıplayan bir
varlığa dönüşür. Bedrock'ta bunu yapmak için ayrı entity + geometry + render
controller yazmak gerekiyor. Şimdilik blok yerinde durup patlıyor. TNT
fırlatılamaz.

**F2 — Redstone ile ateşleme yok.** Sadece çakmak. Bedrock'ta özel bloğa
redstone dinletmek ayrı bir iş.

**F3 — Zincirleme patlama yok.** Bir TNT diğerini ateşlemiyor.

**F4 — Dokular yeniden üretildi.** Elmas ve Zıplatan TNT kendi PNG'lerini Java
projesinden alıyor. Diğer 10'u Java'da vanilla beton dokusu kullanıyordu;
Bedrock'ta vanilla doku yol adları farklı (`light_gray` → `concrete_silver`
gibi) ve kırık doku riski var, o yüzden 16x16 PNG'ler `build.py` içinde
üretiliyor.

**F5 — Aile TNT'lerinin tonları ayrıştırıldı.** Java'da abi/anne/baba/bebek
dördü de aynı pembe; envanterde ayırt edilemiyor. Aynı sıcak paletten farklı
tonlar verildi. Bilinçli sapma.

**F6 — Buz TNT yaklaşık.** Java sürümü blok listesine göre daha ayrıntılı
dönüşüm yapıyor. Burada: su→packed ice, lav→taş, katı blokların üstüne kar
katmanı.

## Sonraki adımlar

Öncelik sırasıyla:

1. **Fırlayan TNT varlığı (F1).** En görünür eksik. Bir entity + `geometry.tnt`
   benzeri model + fitil animasyonu. Bu çözülünce F3 (zincirleme) de kolaylaşır.
2. **Redstone ateşleme (F2).**
3. **Daha fazla TNT.** Java tarafında 70 sınıf var. Şablon oturduğu için her
   yeni TNT `TNTS` listesine bir kayıt + davranış dalı demek.
4. **Walking TNT.** Java'da AI'lı; Bedrock'ta JSON davranış bileşenleriyle
   yazılır. En eğlencelisi, en öğreticisi.

## Geliştirme döngüsü

Tabletin Minecraft klasörü salt okunur (bkz. `docs/bedrock-port-fizibilite.md`),
yani her deneme:

```bash
python3 bedrock/build.py
adb -s <SERI> push bedrock/out/SuperTNT.mcaddon /sdcard/Download/
# tablette dosyaya bir kez dokun -> Minecraft
```

Tur başına bir dokunuş gerektiği için değişiklikleri topla, tek seferde gönder.
Göndermeden önce `build.py` çıktısındaki doğrulamalara güven: JSON parse, JS
sözdizimi, doku referanslarının çözülmesi.
