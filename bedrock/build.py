#!/usr/bin/env python3
"""
Super TNT Mod -> Minecraft Bedrock Add-On uretici.

Java modundaki (Fabric) TNT'lerin bir alt kumesini Bedrock Add-On'una
cevirir ve tek .mcaddon olarak paketler.

TASARIM KARARLARI
-----------------
K1. Ateslenen blok fiziksel bir varliga donusur (firlar, duser, yanip
    soner) - vanilla TNT gibi. Her TNT'nin `stnt:<ad>_primed` varligi var.

K2. Atesleme uc yoldan: cakmak, redstone (yerlestirilen bloklar dunya
    dinamik ozelliginde tutulup yoklanir) ve zincirleme patlama.

K3. Dokular TNT SABLONU yeniden renklendirilerek uretilir, duz renk kare
    degil. Java'da aile TNT'leri `minecraft:block/*_concrete` kullaniyor
    ama Bedrock'ta vanilla doku yollari farkli adlandirilmis
    (light_gray -> concrete_silver gibi) ve kirik doku riski var.
    Bunun yerine gercek TNT dokusunun parlakligi korunup rengi
    degistiriliyor -> "TNT" yazisi ve fitil duruyor.
    NOT: duz renk kare uretmek ilk denemede envanterde birbirinden
    ayirt edilemeyen 10 pastel kareye yol acmisti.

K4. Aile TNT'lerinin tonlari ayristirildi. Java'da abi/anne/baba/bebek
    dordu de ayni pembe; envanterde ayirt edilemiyor. Ayni sicak paletten
    farkli tonlar verildi. Bilincli sapma.

K5. Davranislar ve METINLER Java kaynagindan BIREBIR alindi. Sayilar
    ilgili *TntEntity.java'dan, ad/aciklama lang/tr_tr.json'dan.
    Turkce karakterler korunur - ilk denemede ASCII'ye kirpilmisti.

K6. Java'da tooltip her TNT'de var, Bedrock'ta blok tooltip'i yok.
    Karsilik: TNT elde secilince aciklama eylem cubugunda gosterilir,
    ceviri anahtariyla (oyuncunun dilinde).

K7. Kalp TNT buff'i YALNIZCA oyunculara. Java'da 88b9de1 ayni hatayi
    Harf TNT'de duzeltti (tum LivingEntity'ye verilince zombi/iskelet
    guclenip cocuk icin tehlikeli oluyordu); KalpTntEntity.java'da gozden
    kacmisti, her iki tarafta da duzeltildi.

K8. Zincirleme taramasi tick'lere yayilir. Tek tick'te (2R+1)^3 blok
    sorgusu tablette gorunur donmaya yol aciyordu.
"""
import json, os, re, shutil, struct, zlib, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA_TEX = os.path.join(ROOT, "src/main/resources/assets/supertntmod/textures/block")
JAVA_ITEM_TEX = os.path.join(ROOT, "src/main/resources/assets/supertntmod/textures/item")

# Bedrock item id -> Java item texture adi. Turkce id'ler Java'daki Ingilizce
# dosyaya eslenir; listede olmayan (dogrudan eslesen) id'ler kendi adiyla
# aranir. Java'da karsiligi olmayanlar duz-renk decor ikonuna duser.
ITEM_TEX_MAP = {
    "delici": "tunneling_item", "kucultme_topu": "shrink_ball",
    "buyutme_topu": "grow_ball", "normal_boyut_topu": "scale_lock",
    "kontrol_kumandasi": "control_remote", "portal_silahi": "portal_gun",
    "tnt_armor_kask": "tnt_armor_helmet", "tnt_armor_govus": "tnt_armor_chestplate",
    "tnt_armor_pantolon": "tnt_armor_leggings", "tnt_armor_bot": "tnt_armor_boots",
    "ender_send_yumurta": "ender_send_spawn_egg",
    "ender_cakmagi": "ender_flint", "altin_flint": "golden_flint",
}
BP = os.path.join(HERE, "super_tnt_BP")
RP = os.path.join(HERE, "super_tnt_RP")
OUT = os.path.join(HERE, "out")

# UUID'ler ELLE YAZILMAZ. Ilk surumde elle uydurulmuslardi ve RP module
# UUID'sinin varyant nibble'i 'c' cikti (RFC 4122 8/9/a/b bekler).
# Minecraft paketi sessizce reddetti: ne davranis ne kaynak paketi
# listede gorundu, hicbir hata mesaji da yoktu.
# Degistirirken `python3 -c "import uuid;print(uuid.uuid4())"` kullan.
BP_UUID = "3f8a1c62-7d54-4b90-9e21-8a4f6c0d1e73"
BP_MOD_UUID = "4a9b2d73-8e65-4ca1-af32-9b5a7d1e2f84"
BP_SCRIPT_UUID = "5bac3e84-9f76-4db2-b043-ac6b8e2f3a95"
RP_UUID = "6cbd4f95-a087-4ec3-b154-bd7c9f3a4b06"
RP_MOD_UUID = "c409b083-2ea1-4ceb-9aed-0168efe05c98"
# Surum: ayni UUID + ayni surum tekrar import edilirse Minecraft bunu
# "ayni paket" sayar ve listede ikinci bir kopya gosterebilir. Surum
# YUKSELTILIRSE guncelleme olarak alir ve o paketi kullanan dunyalar yeni
# surume gecer. Bu yuzden her yeni .mcaddon'da burayi artir.
VERSION = [1, 50, 0]
MIN_ENGINE = [1, 21, 0]
# Surum etiketi paket ADINA yazilir. UUID + klasor adlari sabit oldugu icin
# Minecraft ayni UUID'li paketi yerinde GUNCELLER; ama cihazda eski surum
# girdisi kalirsa listede iki "Super TNT" gorunur. Adi surumle etiketleyince
# hangisinin yeni oldugu bir bakista belli olur ve eskisi silinebilir.
VER_STR = ".".join(str(n) for n in VERSION)

# ---------------------------------------------------------------- oyuncu boyutu
# Kucultme/Buyutme: Bedrock'ta oyuncuya minecraft:scale UYGULANAMIYOR
# (script'ten cagrilinca "event does not exist on minecraft:player" hatasi
# verir). Calisan yontem MorphX'ten alindi ve dogrulandi: iki bagimsiz katman.
#   1) GORSEL boyut = RP client_entity'de render-scale Molang, bir entity
#      ozelligine (st:size) bagli.
#   2) CARPISMA kutusu = BP component_group icinde minecraft:collision_box,
#      olaylarla degistirilir.
# Ikisini de bir entity int-ozelligi (st:size) yonlendirir; script
# triggerEvent("st:size_N") ile ayarlar. player.json paketler arasi
# BIRLESMEDIGI icin bu ozellik MorphX ile ayni dunyada CAKISIR (ustteki
# paket kazanir); ayri dunyalarda ikisi de calisir. PORT-DURUMU.md'de yazili.
#
# st:size 0..4 -> (gorsel olcek, carpisma genisligi, carpisma yuksekligi).
# 2 = normal (vanilla 0.6 x 1.8). Gorsel olcek carpisma yuksekligi / 1.8
# oranina hizalandi ki model hitbox'in disina tasmasin (denetimde bulunan
# kozmetik uyumsuzluk). MorphX dev boyutu 3.6y kullaniyor; ustunu asmadik.
#
# ILK SAHIS KAMERASI: Bedrock'ta goz yuksekligi SABITTIR; render-scale de,
# collision_box da, minecraft:scale de onu degistirmez. Ozel kamera on-ayarlari
# da yalnizca "minecraft:free"den turetilebiliyor. Yani kucukken dunyayi buyuk
# gormenin tek yolu betikle surulen serbest kamera; nasil surulecegi ve
# v1.40.0'da neyi yanlis yaptigi main.js'teki "POV kamerasi" blogunda yazili.
#
# CARPISMA YUKSEKLIKLERI. Tek blokluk bir bosluga girebilmek icin yukseklik
# 1.0'in ALTINDA olmali; kademe 1 tam 1.00 iken sinirda kaliyordu ve cocuk
# "kuculdum ama giremiyorum" diyordu. 0.95 pay birakiyor. Gorsel olcek
# carpisma yuksekligi / 1.8 oranina hizali: model hitbox'in disina tasmasin.
SIZE_TABLE = {
    0: (0.33, 0.35, 0.60),   # minik   (0.6/1.8)
    1: (0.53, 0.50, 0.95),   # kucuk   (0.95/1.8) — tek blokluk bosluga girer
    2: (1.00, 0.60, 1.80),   # normal  (vanilla)
    3: (1.65, 0.90, 3.00),   # buyuk   (3.0/1.8)
    4: (2.00, 1.20, 3.60),   # dev     (3.6/1.8)
}
SIZE_DEFAULT = 2

# POV kamerasinin BAKIS YONUNDE ne kadar one otelenecegi (blok). Iki sinirin
# ARASINDA kalmak zorunda:
#   - oyuncunun KENDI kafasinin on yuzunden ileride olmali. Serbest kamerada
#     oyuncunun kendi modeli de cizilir; kafanin icinde kalirsa ekran siyah
#     olur. Vanilla oyuncu kafasi 8x8x8 piksel = yarim blok, yani yari
#     derinligi olcek x 0.25.
#   - carpisma kutusunun on yuzunden geride olmali. Disari tasarsa duvara
#     dayanan cocukta kamera blogun ICINDE kalir ve yine siyah olur.
# Ikisinin tam ortasi secildi. Once "0.25 * olcek + sabit pay" formulu
# yazilmisti; hesaplayinca dort kademenin UCUNDE kutunun disina tastigi
# gorulunce tabloya cevrildi. Asagidaki assert bunu her uretimde dogrular.
SIZE_CAM_FWD = {i: round((0.25 * s + cw / 2) / 2, 3)
                for i, (s, cw, _ch) in SIZE_TABLE.items()}
for _i, (_s, _cw, _ch) in SIZE_TABLE.items():
    assert 0.25 * _s < SIZE_CAM_FWD[_i] < _cw / 2, (
        f"kademe {_i}: kamera otelemesi {SIZE_CAM_FWD[_i]} kafa on yuzu "
        f"{0.25 * _s} ile carpisma kutusu {_cw / 2} arasinda degil")

# MorphX'in (calisan referans) oyuncu taban bilesenleri. Bunlar Mojang'in
# vanilla oyuncu bilesen degerleri; hareket/kamera/envanter motor tarafinda
# gomulu oldugu icin player.json'a yazilmaz. Eksik/yanlis deger oyuncuyu
# bozar, o yuzden tahmin degil kanitlanmis degerler kullanildi.
PLAYER_BASE = {
    "minecraft:experience_reward": {"on_death": "Math.Min(query.player_level * 7, 100)"},
    "minecraft:is_hidden_when_invisible": {},
    "minecraft:loot": {"table": "loot_tables/empty.json"},
    "minecraft:can_climb": {},
    "minecraft:exhaustion_values": {
        "heal": 6, "jump": 0.05, "sprint_jump": 0.2, "mine": 0.005, "attack": 0.1,
        "damage": 0.1, "walk": 0.0, "sprint": 0.1, "swim": 0.01},
    "minecraft:player.saturation": {"value": 5, "max": 20},
    "minecraft:player.exhaustion": {"value": 0, "max": 20},
    "minecraft:player.level": {"value": 0, "max": 24791},
    "minecraft:player.experience": {"value": 0, "max": 1},
    "minecraft:nameable": {"always_show": True, "allow_name_tag_renaming": False},
    "minecraft:physics": {"push_towards_closest_space": True},
    "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": True},
    "minecraft:insomnia": {"days_until_insomnia": 3},
    "minecraft:conditional_bandwidth_optimization": {},
    "minecraft:block_climber": {},
}

# ---------------------------------------------------------------- oyuncu morph'u
# Oyuncu GORUNTU olarak vanilla mob'a donusur (render controller + vanilla
# geometry/texture/material). SAF GORSEL: carpisma kutusu st:size'dan gelir,
# morph sadece gorunumu degistirir, boylece st:size ile cakisma olmaz.
# DIKKAT: geometry tek basina animate ETMEZ — morph statik bind-pose'da
# gorunur. Bind pozu kotu olan vanilla model icin 'pose' alani var: oyuncuya
# st:morph ile kapili tek kareli bir animasyon baglanir (Bedrock animasyon
# donusleri bind pozunun USTUNE eklenir), boylece vanilla modeli degistirmeden
# durus duzeltilir.
#
# VANILLA TABLOSU ELLE YAZILMAZ. bedrock/tools/gen_morphs.py, bedrock-samples
# deposundaki resource_pack/entity/*.entity.json dosyalarindan geometry/texture/
# material adlarini okuyup bu blogu uretir; yanlis ad = gorunmez model oldugu
# icin adlarin tek dogru kaynagi odur. Yeni mob eklerken o betige satir ekle
# ve `--write` ile calistir. Paketin kendi mob'lari asagida, MORPHS_OWN'da.
#
# Alanlar:
#   key  mob'un typeId son eki (minecraft:<key>) — MORPH_MAP bununla eslesir
#   n    st:morph tam sayisi. ILK 15 DEGER SABIT: kayitli st:morph'u olan
#        oyuncu guncellemeden sonra baska bir moba donusmesin.
#   cat  donusum menusundeki grup (Hayvanlar / Su / Canavarlar / Devler)
#   mat_parts  [[kemik_deseni, materyal], ...] — tek materyal yetmeyen mob
#              (blaze'in kafasi, kardan adamin bal kabagi)
#   layers     ustune cizilen ek katmanlar (iskelet giysisi, gicirdak gozleri,
#              koylu meslek dokusu). Her katman ayri bir render controller.
#   pas / act  morph yetenegi: pas surekli verilen etki, act comelme ile
#              tetiklenen aktif yetenek (bkz. MORPH_ABIL).
# Vanilla animasyon/degisken gerektiren katmanlar (warden'in nabiz gibi yanip
# sonen lekeleri, bakir golemin cicegi) ALINMADI: o Molang degiskenlerini
# oyuncu varligi hesaplamaz, katman sabit/parlak takilir.
MORPHS_VANILLA = [
    # Creeper'in patlamasi BLOK KIRAR. Eskiden breaks varsayilani false'ti:
    # patlama oluyor, ses cikiyor, ama hicbir sey olmuyordu — cocuk "sadece
    # patlama sesi cikiyor, bozuk" diyordu. Gercek creeper da blok kirar.
    # Yaricap 3 (vanilla creeper gucu), Dev Creeper 6 ile daha sert kaliyor.
    # Ipucu bunu kendiliginden yazar: morph_hint 'breaks' gorunce
    # "(blok kirar!)" ekler.
    dict(key="creeper", n=1, tr="Creeper", cat="Canavarlar", geo="geometry.creeper.v1.8",
         tex="textures/entity/creeper/creeper", mat="creeper", act="explode",
         pow=dict(r=3, breaks=True, cd=100)),
    dict(key="zombie", n=2, tr="Zombi", cat="Canavarlar", geo="geometry.zombie.v1.8",
         tex="textures/entity/zombie/zombie", mat="zombie"),
    dict(key="skeleton", n=3, tr="İskelet", cat="Canavarlar", geo="geometry.skeleton.v1.8",
         tex="textures/entity/skeleton/skeleton", mat="skeleton"),
    dict(key="enderman", n=4, tr="Enderman", cat="Canavarlar", geo="geometry.enderman.v1.8",
         tex="textures/entity/enderman/enderman", mat="enderman", act="teleport"),
    dict(key="iron_golem", n=5, tr="Demir Golem", cat="Hayvanlar", geo="geometry.irongolem",
         tex="textures/entity/iron_golem", mat="iron_golem", pas=["resistance", 1]),
    dict(key="wolf", n=6, tr="Kurt", cat="Hayvanlar", geo="geometry.wolf",
         tex="textures/entity/wolf/wolf", mat="wolf", pas=["speed", 1]),
    dict(key="pig", n=7, tr="Domuz", cat="Hayvanlar", geo="geometry.pig.v3",
         tex="textures/entity/pig/pig_v3", mat="pig_v3"),
    dict(key="cow", n=8, tr="İnek", cat="Hayvanlar", geo="geometry.cow.v2",
         tex="textures/entity/cow/cow_v2", mat="cow"),
    dict(key="chicken", n=9, tr="Tavuk", cat="Hayvanlar", geo="geometry.chicken.v1.12",
         tex="textures/entity/chicken/chicken", mat="chicken", pas=["slow_falling", 0]),
    dict(key="spider", n=10, tr="Örümcek", cat="Canavarlar", geo="geometry.spider.v1.8",
         tex="textures/entity/spider/spider", mat="spider", pas=["jump_boost", 2]),
    dict(key="piglin", n=11, tr="Piglin", cat="Canavarlar", geo="geometry.piglin",
         tex="textures/entity/piglin/piglin", mat="piglin"),
    dict(key="allay", n=12, tr="Allay", cat="Hayvanlar", geo="geometry.allay",
         tex="textures/entity/allay/allay", mat="allay", pas=["slow_falling", 0], act="float"),
    dict(key="wither_skeleton", n=13, tr="Wither İskeleti", cat="Canavarlar",
         geo="geometry.skeleton.wither.v1.8", tex="textures/entity/skeleton/wither_skeleton",
         mat="skeleton", pas=["fire_resistance", 0]),
    dict(key="ghast", n=14, tr="Ghast", cat="Canavarlar", geo="geometry.ghast",
         tex="textures/entity/ghast/ghast", mat="ghast", pas=["slow_falling", 0],
         act="fireball"),
    dict(key="slime", n=15, tr="Slime", cat="Canavarlar", geo="geometry.slime",
         tex="textures/entity/slime/slime", mat="slime", pas=["jump_boost", 2]),
    dict(key="strider", n=16, tr="Adımlayıcı", cat="Hayvanlar", geo="geometry.strider",
         tex="textures/entity/strider/strider", mat="strider", pas=["fire_resistance", 0]),
    dict(key="armadillo", n=17, tr="Armadillo", cat="Hayvanlar", geo="geometry.armadillo",
         tex="textures/entity/armadillo", mat="armadillo", pas=["resistance", 0]),
    dict(key="bee", n=18, tr="Arı", cat="Hayvanlar", geo="geometry.bee",
         tex="textures/entity/bee/bee", mat="bee", pas=["slow_falling", 0], act="float"),
    dict(key="horse", n=19, tr="At", cat="Hayvanlar", geo="geometry.horse.v3",
         tex="textures/entity/horse2/horse_brown", mat="horse_leather_armor", pas=["speed", 2]),
    dict(key="copper_golem", n=20, tr="Bakır Golem", cat="Hayvanlar",
         geo="geometry.copper_golem", tex="textures/entity/copper_golem/copper_golem",
         mat="copper_golem",
         layers=[{"tex": ["textures/entity/copper_golem/copper_golem_eyes"],
         "mat": "copper_golem_eyes"}], pas=["resistance", 0]),
    dict(key="camel", n=21, tr="Deve", cat="Hayvanlar", geo="geometry.camel",
         tex="textures/entity/camel/camel", mat="camel", pas=["speed", 1]),
    dict(key="donkey", n=22, tr="Eşek", cat="Hayvanlar", geo="geometry.horse.v3",
         tex="textures/entity/horse2/donkey", mat="horse", pas=["speed", 1]),
    dict(key="wandering_trader", n=23, tr="Gezgin Tüccar", cat="Hayvanlar",
         geo="geometry.villager_v2", tex="textures/entity/wandering_trader",
         mat="wandering_trader"),
    dict(key="snow_golem", n=24, tr="Kardan Adam", cat="Hayvanlar",
         geo="geometry.snowgolem.v1.8", tex="textures/entity/snow_golem", mat="snow_golem",
         mat_parts=[["head*", "snow_golem_pumpkin"]]),
    dict(key="mule", n=25, tr="Katır", cat="Hayvanlar", geo="geometry.horse.v3",
         tex="textures/entity/horse2/mule", mat="horse", pas=["speed", 1]),
    dict(key="cat", n=26, tr="Kedi", cat="Hayvanlar", geo="geometry.cat",
         tex="textures/entity/cat/redtabby", mat="cat", pas=["speed", 1]),
    dict(key="goat", n=27, tr="Keçi", cat="Hayvanlar", geo="geometry.goat",
         tex="textures/entity/goat/goat", mat="goat", pas=["jump_boost", 1]),
    dict(key="sniffer", n=28, tr="Koklayıcı", cat="Hayvanlar", geo="geometry.sniffer",
         tex="textures/entity/sniffer/sniffer", mat="sniffer"),
    dict(key="sheep", n=29, tr="Koyun", cat="Hayvanlar", geo="geometry.sheep.v1.8",
         tex="textures/entity/sheep/sheep", mat="sheep"),
    dict(key="frog", n=30, tr="Kurbağa", cat="Hayvanlar", geo="geometry.frog",
         tex="textures/entity/frog/temperate_frog", mat="frog", pas=["jump_boost", 2]),
    dict(key="polar_bear", n=31, tr="Kutup Ayısı", cat="Hayvanlar", geo="geometry.polarbear",
         tex="textures/entity/polar_bear", mat="polar_bear", pas=["resistance", 0]),
    dict(key="villager_v2", n=32, tr="Köylü", cat="Hayvanlar", geo="geometry.villager_v2",
         tex="textures/entity/villager2/villager", mat="villager_v2",
         layers=[{"tex": ["textures/entity/villager2/biomes/biome_plains",
         "textures/entity/villager2/professions/farmer"], "mat": "villager_v2_masked"}]),
    dict(key="llama", n=33, tr="Lama", cat="Hayvanlar", geo="geometry.llama.v1.8",
         tex="textures/entity/llama/llama_creamy", mat="llama"),
    dict(key="mooshroom", n=34, tr="Mantar İnek", cat="Hayvanlar", geo="geometry.mooshroom.v2",
         tex="textures/entity/cow/mooshroom_v2", mat="mooshroom"),
    dict(key="ocelot", n=35, tr="Ocelot", cat="Hayvanlar", geo="geometry.ocelot.v1.8",
         tex="textures/entity/cat/ocelot", mat="ocelot", pas=["speed", 1]),
    dict(key="panda", n=36, tr="Panda", cat="Hayvanlar", geo="geometry.panda",
         tex="textures/entity/panda/panda", mat="panda"),
    dict(key="parrot", n=37, tr="Papağan", cat="Hayvanlar", geo="geometry.parrot",
         tex="textures/entity/parrot/parrot_red_blue", mat="parrot", pas=["slow_falling", 0],
         act="float"),
    dict(key="rabbit", n=38, tr="Tavşan", cat="Hayvanlar", geo="geometry.rabbit.v2",
         tex="textures/entity/rabbit/rabbit_brown", mat="rabbit", pas=["jump_boost", 2]),
    dict(key="fox", n=39, tr="Tilki", cat="Hayvanlar", geo="geometry.fox",
         tex="textures/entity/fox/fox", mat="fox", pas=["speed", 1]),
    dict(key="trader_llama", n=40, tr="Tüccar Laması", cat="Hayvanlar",
         geo="geometry.llama.v1.8", tex="textures/entity/llama/llama_creamy", mat="llama"),
    dict(key="bat", n=41, tr="Yarasa", cat="Hayvanlar", geo="geometry.bat_v2",
         tex="textures/entity/bat_v2", mat="bat_v2", pas=["slow_falling", 0], act="float"),
    dict(key="axolotl", n=42, tr="Axolotl", cat="Su", geo="geometry.axolotl",
         tex="textures/entity/axolotl/axolotl_wild", mat="axolotl", pas=["water_breathing", 0]),
    dict(key="pufferfish", n=43, tr="Balon Balığı", cat="Su",
         geo="geometry.pufferfish.large.v1.8", tex="textures/entity/fish/pufferfish",
         mat="pufferfish", pas=["water_breathing", 0]),
    dict(key="glow_squid", n=44, tr="Işıklı Mürekkep Balığı", cat="Su", geo="geometry.squid",
         tex="textures/entity/glow_squid/glow_squid", mat="glow_squid", pas=["water_breathing",
         0]),
    dict(key="turtle", n=45, tr="Kaplumbağa", cat="Su", geo="geometry.turtle",
         tex="textures/entity/sea_turtle", mat="turtle", pas=["water_breathing", 0]),
    dict(key="cod", n=46, tr="Morina Balığı", cat="Su", geo="geometry.cod",
         tex="textures/entity/fish/cod", mat="cod", pas=["water_breathing", 0]),
    dict(key="guardian", n=47, tr="Muhafız", cat="Su", geo="geometry.guardian.v1.8",
         tex="textures/entity/guardian", mat="guardian", pas=["water_breathing", 0]),
    dict(key="squid", n=48, tr="Mürekkep Balığı", cat="Su", geo="geometry.squid",
         tex="textures/entity/squid", mat="squid", pas=["water_breathing", 0]),
    dict(key="salmon", n=49, tr="Somon", cat="Su", geo="geometry.salmon",
         tex="textures/entity/fish/salmon", mat="salmon", pas=["water_breathing", 0]),
    dict(key="tropicalfish", n=50, tr="Tropikal Balık", cat="Su",
         geo="geometry.tropicalfish_a", tex="textures/entity/fish/tropical_a",
         mat="tropicalfish", pas=["water_breathing", 0]),
    dict(key="elder_guardian", n=51, tr="Yaşlı Muhafız", cat="Su",
         geo="geometry.guardian.v1.8", tex="textures/entity/guardian_elder", mat="guardian",
         pas=["water_breathing", 0]),
    dict(key="dolphin", n=52, tr="Yunus", cat="Su", geo="geometry.dolphin",
         tex="textures/entity/dolphin", mat="dolphin", pas=["water_breathing", 0]),
    dict(key="tadpole", n=53, tr="İribaş", cat="Su", geo="geometry.tadpole",
         tex="textures/entity/tadpole/tadpole", mat="tadpole", pas=["water_breathing", 0]),
    dict(key="bogged", n=54, tr="Bataklık İskeleti", cat="Canavarlar",
         geo="geometry.skeleton.bogged", tex="textures/entity/skeleton/bogged", mat="bogged",
         layers=[{"tex": ["textures/entity/skeleton/bogged_clothes"], "mat": "bogged_clothes",
         "geo": "geometry.bogged.armor"}]),
    dict(key="blaze", n=55, tr="Blaze", cat="Canavarlar", geo="geometry.blaze",
         tex="textures/entity/blaze", mat="blaze_body", mat_parts=[["head", "blaze_head"]],
         pas=["fire_resistance", 0], act="fireball"),
    dict(key="drowned", n=56, tr="Boğulmuş", cat="Canavarlar",
         geo="geometry.zombie.drowned.v1.16", tex="textures/entity/zombie/drowned",
         mat="drowned", pas=["water_breathing", 0]),
    dict(key="stray", n=57, tr="Buz İskeleti", cat="Canavarlar",
         geo="geometry.skeleton.stray.v1.8", tex="textures/entity/skeleton/stray", mat="stray",
         layers=[{"tex": ["textures/entity/skeleton/stray_overlay"], "mat": "stray_clothes",
         "geo": "geometry.stray.armor.v1.8"}]),
    dict(key="witch", n=58, tr="Cadı", cat="Canavarlar", geo="geometry.villager.witch.v1.8",
         tex="textures/entity/witch", mat="witch"),
    dict(key="endermite", n=59, tr="Endermit", cat="Canavarlar", geo="geometry.endermite",
         tex="textures/entity/endermite", mat="endermite"),
    dict(key="breeze", n=60, tr="Esinti", cat="Canavarlar", geo="geometry.breeze",
         tex="textures/entity/breeze/breeze", mat="breeze", pas=["slow_falling", 0],
         act="float"),
    dict(key="evocation_illager", n=61, tr="Evoker", cat="Canavarlar",
         geo="geometry.evoker.v1.8", tex="textures/entity/illager/evoker", mat="evoker"),
    dict(key="silverfish", n=62, tr="Gümüş Balığı", cat="Canavarlar",
         geo="geometry.silverfish", tex="textures/entity/silverfish", mat="silverfish"),
    dict(key="creaking", n=63, tr="Gıcırdak", cat="Canavarlar", geo="geometry.creaking",
         tex="textures/entity/creaking/creaking", mat="creaking",
         layers=[{"tex": ["textures/entity/creaking/creaking_eyes"], "mat": "creaking_eyes"}]),
    dict(key="phantom", n=64, tr="Hayalet Kuş", cat="Canavarlar", geo="geometry.phantom",
         tex="textures/entity/phantom", mat="phantom", pas=["slow_falling", 0], act="float"),
    dict(key="hoglin", n=65, tr="Hoglin", cat="Canavarlar", geo="geometry.hoglin",
         tex="textures/entity/hoglin/hoglin", mat="hoglin"),
    dict(key="magma_cube", n=66, tr="Magma Küpü", cat="Canavarlar",
         geo="geometry.magma_cube_v2", tex="textures/entity/slime/magmacube_v2",
         mat="magma_cube", pas=["fire_resistance", 0]),
    dict(key="cave_spider", n=67, tr="Mağara Örümceği", cat="Canavarlar",
         geo="geometry.spider.v1.8", tex="textures/entity/spider/cave_spider", mat="spider",
         pas=["jump_boost", 2]),
    dict(key="piglin_brute", n=68, tr="Piglin Kabadayı", cat="Canavarlar",
         geo="geometry.piglin", tex="textures/entity/piglin/piglin_brute", mat="piglin_brute",
         pas=["resistance", 0]),
    dict(key="ravager", n=69, tr="Ravager", cat="Canavarlar", geo="geometry.ravager",
         tex="textures/entity/illager/ravager", mat="ravager", pas=["resistance", 0]),
    dict(key="shulker", n=70, tr="Shulker", cat="Canavarlar", geo="geometry.shulker.v1.8",
         tex="textures/entity/shulker/shulker_undyed", mat="shulker"),
    dict(key="vex", n=71, tr="Vex", cat="Canavarlar", geo="geometry.vex.v1.8",
         tex="textures/entity/vex/vex", mat="vex", pas=["slow_falling", 0], act="float"),
    dict(key="vindicator", n=72, tr="Vindicator", cat="Canavarlar",
         geo="geometry.vindicator.v1.8", tex="textures/entity/vindicator", mat="vindicator"),
    dict(key="pillager", n=73, tr="Yağmacı", cat="Canavarlar", geo="geometry.pillager",
         tex="textures/entity/pillager", mat="pillager"),
    dict(key="zoglin", n=74, tr="Zoglin", cat="Canavarlar", geo="geometry.hoglin",
         tex="textures/entity/zoglin/zoglin", mat="zoglin"),
    dict(key="zombie_horse", n=75, tr="Zombi At", cat="Canavarlar", geo="geometry.horse.v3",
         tex="textures/entity/horse2/horse_zombie", mat="horse_leather_armor", pas=["speed",
         1]),
    dict(key="zombie_villager_v2", n=76, tr="Zombi Köylü", cat="Canavarlar",
         geo="geometry.zombie.villager_v2",
         tex="textures/entity/zombie_villager2/zombie-villager", mat="zombie_villager_v2"),
    dict(key="zombie_pigman", n=77, tr="Zombi Piglin", cat="Canavarlar", geo="geometry.piglin",
         tex="textures/entity/piglin/zombie_piglin", mat="zombie", pas=["fire_resistance", 0]),
    dict(key="husk", n=78, tr="Çöl Zombisi", cat="Canavarlar", geo="geometry.zombie.husk.v1.8",
         tex="textures/entity/zombie/husk", mat="husk"),
    dict(key="skeleton_horse", n=79, tr="İskelet At", cat="Canavarlar",
         geo="geometry.horse.v3", tex="textures/entity/horse2/horse_skeleton", mat="horse",
         pas=["speed", 2]),
    dict(key="ender_dragon", n=80, tr="Ender Ejderha", cat="Devler", geo="geometry.dragon",
         tex="textures/entity/dragon/dragon", mat="ender_dragon", scale=0.4,
         pas=["fire_resistance", 0], act="float"),
    dict(key="happy_ghast", n=81, tr="Mutlu Ghast", cat="Devler", geo="geometry.happy_ghast",
         tex="textures/entity/happy_ghast/happy_ghast", mat="ghast", scale=0.5,
         pas=["slow_falling", 0], act="float"),
    # POZ: vanilla geometry.warden'in kollarinda hicbir rotation YOK ve kollar
    # uzun (y 6..34), bacaklarin (y 0..13) x araligina TAM DEGIYOR (olculdu:
    # sag kol x -17..-9, sag bacak x -9..-3, ortusme 0). Oyunda warden'in
    # kollari surekli salindigi icin bu goze carpmaz; oyuncu morph'u ise
    # statik bind pozunda durur ve kollar bacaklara YAPISIK kalir.
    # Vanilla modeli duzeltemeyiz, o yuzden poz oyuncu tarafinda animasyonla
    # verilir. Aci, mutant warden'daki ile ayni 15 derece.
    dict(key="warden", n=82, tr="Warden", cat="Devler", geo="geometry.warden",
         tex="textures/entity/warden/warden", mat="warden", pas=["resistance", 1], act="sonic",
         pose={"right_arm": [0, 0, 15], "left_arm": [0, 0, -15]}),
    dict(key="wither", n=83, tr="Wither", cat="Devler", geo="geometry.witherBoss",
         tex="textures/entity/wither_boss/wither", mat="wither_boss", scale=0.8,
         pas=["fire_resistance", 0], act="float"),
]

# Paketin KENDI mob'lari (MONSTERS). Cocuk cagirdigi boss'a da donusebilsin —
# vanilla listesi bunlari kapsamaz. Bunlar bedrock-samples'tan uretilemez,
# elle yazilir; gorsel varliklar ya vanilla'nin (dev creeper = creeper dokusu)
# ya da bu paketin RP'sinde SHIP EDILEN kendi modelimizdir (ender_send).
# ns: typeId onEki. Vanilla morph'larda "minecraft", burada "stnt".
# OLCEK: cagrilan boss'un kendi olceginden (2.0-3.2) KUCUK secildi. Morph saf
# gorsel; ucuncu sahis kamerasi oyuncuya sabit uzaklikta durdugundan 3x model
# kamerayi govdenin ICINDE birakip ekrani kapatiyor. Bu degerlerde cocuk
# belirgin sekilde dev ama etrafini goruyor; daha buyugunu isterse Buyutme
# Topu (st:size) zaten var.
MORPHS_OWN = [
    dict(key="dev_creeper", n=84, tr="Dev Creeper", cat="Süper TNT", ns="stnt",
         geo="geometry.creeper.v1.8", tex="textures/entity/creeper/creeper",
         mat="creeper", scale=2.0, act="explode",
         pow=dict(r=6, breaks=True, cd=140)),
    dict(key="dev_zombi", n=85, tr="Dev Zombi", cat="Süper TNT", ns="stnt",
         geo="geometry.zombie.v1.8", tex="textures/entity/zombie/zombie",
         mat="zombie", scale=2.0, pas=["resistance", 1], act="smash",
         pow=dict(r=7, dmg=10, knock=1.6, cd=100)),
    # Boss ile AYNI model. Olcek 1.5 -> 1.15: yeni model vanilla warden'dan uzun
    # (3,69 blok), 1.5'te oyuncu 5,2 blok olup kamerayi modelin icine aliyordu.
    dict(key="mutant_warden", n=86, tr="Mutant Warden", cat="Süper TNT", ns="stnt",
         geo="geometry.stnt_mutant_warden",
         tex="textures/entity/stnt_mutant_warden/mutant_warden",
         mat="warden", scale=1.15, pas=["resistance", 1], act="sonic",
         layers=[dict(tex=["textures/entity/stnt_mutant_warden/bioluminescent_layer"],
                      mat="warden_bioluminescent_layer")],
         pow=dict(range=34, damage=22, knock=2.6, cd=120)),
    dict(key="ender_send", n=87, tr="Ender Send", cat="Süper TNT", ns="stnt",
         geo="geometry.ender_send", tex="textures/entity/ender_send",
         mat="entity_emissive_alpha", scale=1.4, pas=["resistance", 1], act="teleport",
         pow=dict(range=96, cd=30)),
]
# ------------------------------------------------------------- BLOK KILIGI
# Oyuncuyu bir BLOGA benzetir. Mob morph'lariyla ayni makine: tek fark geometry
# (16x16x16 kup, her yuzu ayni 16x16 dokuyu ornekler) ve dokunun bir mob degil
# BLOK dokusu olmasi. Vanilla blok dokulari da tipki mob dokulari gibi ADIYLA
# referans alinir, pakete konmaz.
#
# blocks: hangi blogu kirinca bu kiliga girilir. Birden fazla olabilir —
# cocuk cimeni de kirsa toprak blogunu da kirsa dogru seye donussun diye
# takma adlar aynı girdiye baglanir.
BLOCK_GEO = "geometry.stnt_block_morph"
BLOCK_GEO_TOP = "geometry.stnt_block_morph_top"
BLOCK_GEO_BOTTOM = "geometry.stnt_block_morph_bottom"
BLOCK_MAT = "entity_alphatest"
# UST ve ALT yuzun dokusu. Cogu blokta uc yuz de aynidir; farkli olanlar
# burada yazili: key -> (ust, alt). Bir cizim gecisi TEK doku kullanabildigi
# icin ust ve alt ayri gecislerde (layers) ayri levha olarak cizilir.
# Ad UYDURULMAZ: check_pack her dokuyu vanilla block_textures listesinde ya da
# pakette ship edilmis olarak bulmak zorunda, yoksa yuz gorunmez olur.
BLOCK_FACES = {
    "cim": ("grass_carried", "dirt"),            # cimin ustu yesil, alti toprak
    "kutuk": ("log_oak_top", "log_oak_top"),     # kutugun ustu yillik halkalari
    "balkabagi": ("pumpkin_top", "pumpkin_top"),
    "fener": ("pumpkin_top", "pumpkin_top"),
    "karpuz": ("melon_top", "melon_top"),
    "kitaplik": ("planks_oak", "planks_oak"),
    "tezgah": ("crafting_table_top", "planks_oak"),
    "firin": ("furnace_top", "furnace_top"),
    "vanilla_tnt": ("tnt_top", "tnt_bottom"),
    "saman": ("hay_block_top", "hay_block_top"),
    "kuvars": ("quartz_block_top", "quartz_block_bottom"),
    "bal": ("honey_top", "honey_bottom"),
    # paketin kendi TNT'leri: ust/alt dokularini zaten kendisi uretiyor
    "zeynep_tnt_blok": ("stnt_zeynep_tnt_top", "stnt_zeynep_tnt_bottom"),
    "elmas_tnt_blok": ("stnt_diamond_tnt_top", "stnt_diamond_tnt_bottom"),
    "kalp_tnt_blok": ("stnt_kalp_tnt_top", "stnt_kalp_tnt_bottom"),
    "nuclear_tnt_blok": ("stnt_nuclear_tnt_top", "stnt_nuclear_tnt_bottom"),
}
BLOCK_MORPHS_RAW = [
    ("tas", "Taş", "stone", ["minecraft:stone", "minecraft:stone_bricks", "minecraft:andesite"]),
    ("kaldirim", "Kaldırım Taşı", "cobblestone", ["minecraft:cobblestone", "minecraft:mossy_cobblestone"]),
    ("toprak", "Toprak", "dirt", ["minecraft:dirt", "minecraft:coarse_dirt", "minecraft:dirt_with_roots"]),
    ("cim", "Çim", "grass_side_carried", ["minecraft:grass_block", "minecraft:grass_path", "minecraft:podzol"]),
    ("kum", "Kum", "sand", ["minecraft:sand", "minecraft:red_sand", "minecraft:sandstone"]),
    ("cakil", "Çakıl", "gravel", ["minecraft:gravel"]),
    ("tahta", "Meşe Tahtası", "planks_oak", ["minecraft:oak_planks", "minecraft:birch_planks", "minecraft:spruce_planks"]),
    ("kutuk", "Meşe Kütüğü", "log_oak", ["minecraft:oak_log", "minecraft:dark_oak_log", "minecraft:stripped_oak_log"]),
    ("yaprak", "Yaprak", "leaves_oak_opaque", ["minecraft:oak_leaves", "minecraft:birch_leaves", "minecraft:dark_oak_leaves"]),
    ("cam", "Cam", "glass", ["minecraft:glass", "minecraft:glass_pane"]),
    ("tugla", "Tuğla", "brick", ["minecraft:brick_block", "minecraft:nether_brick"]),
    ("obsidyen", "Obsidyen", "obsidian", ["minecraft:obsidian", "minecraft:crying_obsidian"]),
    ("elmas", "Elmas Bloku", "diamond_block", ["minecraft:diamond_block"]),
    ("altin", "Altın Bloku", "gold_block", ["minecraft:gold_block"]),
    ("demir", "Demir Bloku", "iron_block", ["minecraft:iron_block"]),
    ("zumrut", "Zümrüt Bloku", "emerald_block", ["minecraft:emerald_block"]),
    ("lapis", "Lapis Bloku", "lapis_block", ["minecraft:lapis_block"]),
    ("komur", "Kömür Bloku", "coal_block", ["minecraft:coal_block"]),
    ("redstone_blok", "Redstone Bloku", "redstone_block", ["minecraft:redstone_block"]),
    ("ametist", "Ametist", "amethyst_block", ["minecraft:amethyst_block"]),
    ("netherrack", "Netherrack", "netherrack", ["minecraft:netherrack"]),
    ("buz_blok", "Buz", "ice", ["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice"]),
    ("kar", "Kar", "snow", ["minecraft:snow", "minecraft:snow_layer"]),
    ("balkabagi", "Balkabağı", "pumpkin_side", ["minecraft:pumpkin", "minecraft:carved_pumpkin"]),
    ("fener", "Kabak Feneri", "pumpkin_face_on", ["minecraft:lit_pumpkin"]),
    ("karpuz", "Karpuz", "melon_side", ["minecraft:melon_block"]),
    ("kitaplik", "Kitaplık", "bookshelf", ["minecraft:bookshelf"]),
    ("tezgah", "Çalışma Tezgahı", "crafting_table_side", ["minecraft:crafting_table"]),
    ("firin", "Fırın", "furnace_side", ["minecraft:furnace", "minecraft:lit_furnace", "minecraft:blast_furnace"]),
    ("vanilla_tnt", "TNT", "tnt_side", ["minecraft:tnt"]),
    ("saman", "Saman Balyası", "hay_block_side", ["minecraft:hay_block"]),
    ("sunger", "Sünger", "sponge", ["minecraft:sponge"]),
    ("kil", "Kil", "clay", ["minecraft:clay"]),
    ("kuvars", "Kuvars Bloku", "quartz_block_side", ["minecraft:quartz_block"]),
    ("sakiz", "Sakız Bloku", "slime", ["minecraft:slime"]),
    ("bal", "Bal Bloku", "honey_side", ["minecraft:honey_block"]),
    ("mercan", "Mercan", "coral_blue", ["minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block"]),
    ("sculk", "Sculk", "sculk", ["minecraft:sculk"]),
    ("kayakat", "Kaya Katmanı", "bedrock", ["minecraft:bedrock"]),
    ("yun_beyaz", "Beyaz Yün", "wool_colored_white", ["minecraft:white_wool"]),
    ("yun_kirmizi", "Kırmızı Yün", "wool_colored_red", ["minecraft:red_wool"]),
    ("yun_mavi", "Mavi Yün", "wool_colored_blue", ["minecraft:blue_wool"]),
    ("yun_sari", "Sarı Yün", "wool_colored_yellow", ["minecraft:yellow_wool"]),
    ("yun_yesil", "Yeşil Yün", "wool_colored_green", ["minecraft:green_wool"]),
]
# Paketin KENDI bloklari: doku RP'de ship ediliyor, adi stnt_ ile basliyor.
BLOCK_MORPHS_OWN = [
    ("zeynep_tnt_blok", "Zeynep TNT", "stnt_zeynep_tnt_side", ["stnt:zeynep_tnt"]),
    ("elmas_tnt_blok", "Elmas TNT", "stnt_diamond_tnt_side", ["stnt:diamond_tnt"]),
    ("kalp_tnt_blok", "Kalp TNT", "stnt_kalp_tnt_side", ["stnt:kalp_tnt"]),
    ("nuclear_tnt_blok", "Nükleer TNT", "stnt_nuclear_tnt_side", ["stnt:nuclear_tnt"]),
]
def _block_morph(k, tr, tex, blocks):
    top, bot = BLOCK_FACES.get(k, (tex, tex))
    return dict(
        key=f"blok_{k}", tr=tr, cat="Bloklar", geo=BLOCK_GEO,
        tex=f"textures/blocks/{tex}", mat=BLOCK_MAT, scale=1.0, blocks=blocks,
        layers=[dict(geo=BLOCK_GEO_TOP, tex=[f"textures/blocks/{top}"], mat=BLOCK_MAT),
                dict(geo=BLOCK_GEO_BOTTOM, tex=[f"textures/blocks/{bot}"], mat=BLOCK_MAT)])


BLOCK_MORPHS = [_block_morph(k, tr, tex, blocks)
                for k, tr, tex, blocks in BLOCK_MORPHS_RAW + BLOCK_MORPHS_OWN]
for _i, _m in enumerate(BLOCK_MORPHS):
    _m["n"] = len(MORPHS_VANILLA) + len(MORPHS_OWN) + 1 + _i

MORPHS = MORPHS_VANILLA + MORPHS_OWN + BLOCK_MORPHS
# Kirilan blok -> kilik olayi. Ayni bloga birden fazla girdi bakmasin.
BLOCK_MORPH_MAP = {}
for _m in BLOCK_MORPHS:
    for _b in _m["blocks"]:
        assert _b not in BLOCK_MORPH_MAP, f"{_b} iki blok kiligina bagli"
        BLOCK_MORPH_MAP[_b] = f"st:morph_{_m['key']}"

# Donusunce eylem cubugunda yazan ipucu. Yetenek tablosundan URETILIR: yeni bir
# morph eklendiginde ipucu kendiliginden dogru olur, elle yazilmaz.
ACT_TIP = {
    "explode": "çömel → patla",
    "teleport": "çömel → baktığın yere ışınlan",
    "fireball": "çömel → ateş topu at",
    "sonic": "çömel → ses saldırısı",
    "smash": "çömel → yer sarsıntısı",
    "float": "çömel → havaya süzül",
}
PAS_TIP = {
    "resistance": "dayanıklılık",
    "speed": "hız",
    "jump_boost": "zıplama",
    "slow_falling": "yavaş düşüş",
    "fire_resistance": "ateşe dayanıklı",
    "water_breathing": "suda nefes",
}


def morph_hint(mo):
    """Tek satirlik yetenek ozeti; yeteneksiz morph icin bos."""
    parts = []
    if "pas" in mo:
        parts.append(PAS_TIP[mo["pas"][0]])
    if "act" in mo:
        act = ACT_TIP[mo["act"]]
        # Ipucu sozlesmesi: blok kiran bir guc bunu SOYLEMEK zorunda, yoksa
        # cocuk kendi evini havaya ucurur.
        if mo.get("pow", {}).get("breaks"):
            act += " (blok kırar!)"
        parts.append(act)
    return " · ".join(parts)


MORPH_HINT = {}
for _mo in MORPHS:
    _h = morph_hint(_mo)
    MORPH_HINT[f"st:morph_{_mo['key']}"] = (
        f"{_mo['tr']} oldun!" + (f" §7{_h}" if _h else " §7yalnız görünüş"))
# mob typeId -> morph olayi (script tiklanan mob'u buradan bulur)
MORPH_MAP = {f"{m.get('ns', 'minecraft')}:{m['key']}": f"st:morph_{m['key']}"
             for m in MORPHS if "blocks" not in m}

# Donusum menusundeki grup sirasi (cocuk once dost yaratiklari gorsun).
MORPH_CATS = ["Hayvanlar", "Su", "Canavarlar", "Devler", "Süper TNT", "Bloklar"]


def morph_passes(mo):
    """Bir morph'un cizim gecisleri. Her gecis = bir render controller cifti
    (ucuncu sahis + ilk sahis). Ilk gecis mob'un govdesi, sonrakiler layers
    girdileri (giysi, goz, meslek dokusu). Dondurdugu demet:
        (slot, geometry, [(doku_kaydi, doku_yolu)], [(kemik, materyal_kaydi, materyal)])
    slot player.json'daki kayit adlarini benzersiz kilar ("" / "__l0" / "__l1").
    """
    k = mo['key']
    mats = [("*", k, mo['mat'])]
    for i, (bone, material) in enumerate(mo.get('mat_parts', [])):
        mats.append((bone, f"{k}__m{i}", material))
    yield "", mo['geo'], [(k, mo['tex'])], mats
    for i, lay in enumerate(mo.get('layers', [])):
        slot = f"__l{i}"
        texs = [(f"{k}{slot}_t{j}", t) for j, t in enumerate(lay['tex'])]
        yield slot, lay.get('geo', mo['geo']), texs, [("*", f"{k}{slot}", lay['mat'])]


# ---------------------------------------------------------------- TNT tanimlari
# renk: (top, side, bottom) RGB. tex: Java projesinden kopyalanacak taban ad.
# tr/trtip metinleri Java projesinin lang/tr_tr.json dosyasindan BIREBIR alindi.
# Turkce karakterler korunur (.lang dosyalari UTF-8).
TNTS = [
    dict(id="diamond_tnt", tr="Elmas TNT", en="Diamond TNT",
         trtip="2.5x patlama gücü - dev kraterler açar",
         entip="2.5x explosion power - opens huge craters",
         tex="diamond_tnt", mat="minecraft:diamond",
         effect=dict(kind="explode", power=10)),
    dict(id="bounce_tnt", tr="Zıplatan TNT", en="Bounce TNT",
         trtip="Yakındaki HER ŞEYİ gökyüzüne fırlatır! Blok hasarı yoktur.",
         entip="Launches EVERYTHING nearby into the sky! No block damage.",
         tex="bounce_tnt", mat="minecraft:slime_ball",
         effect=dict(kind="launch", radius=12, force=1.5)),
    dict(id="zeynep_tnt", tr="Zeynep TNT", en="Zeynep TNT",
         trtip="Lacivert TNT — patlayınca etrafa boş kağıtlar saçar!",
         entip="Navy blue TNT - scatters blank paper everywhere!",
         color=((44, 62, 158), (44, 62, 158), (44, 62, 158)), mat="minecraft:paper",
         effect=dict(kind="items", item="minecraft:paper", count=50, spread=8.0)),
    dict(id="coklu_zeynep_tnt", tr="Çoklu Zeynep TNT", en="Multi Zeynep TNT",
         trtip="Patladığında etrafa 'Zeynep' isimli köylüler saçar.",
         entip="Scatters villagers named 'Zeynep' when it explodes.",
         color=((160, 40, 150), (214, 96, 168), (160, 40, 150)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Zeynep", count=15, spread=7.0)),
    dict(id="abi_tnt", tr="Abi TNT", en="Abi TNT",
         trtip="Patladığında etrafa 'Abi' isimli köylüler saçar.",
         entip="Scatters villagers named 'Abi' when it explodes.",
         color=((196, 76, 132), (196, 76, 132), (196, 76, 132)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Abi", count=12, spread=6.0)),
    dict(id="anne_tnt", tr="Anne TNT", en="Anne TNT",
         trtip="Patladığında etrafa 'Anne' isimli köylüler saçar.",
         entip="Scatters villagers named 'Anne' when it explodes.",
         color=((224, 122, 168), (224, 122, 168), (224, 122, 168)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Anne", count=12, spread=6.0)),
    dict(id="baba_tnt", tr="Baba TNT", en="Baba TNT",
         trtip="Patladığında etrafa 'Baba' isimli köylüler saçar.",
         entip="Scatters villagers named 'Baba' when it explodes.",
         color=((150, 60, 110), (150, 60, 110), (150, 60, 110)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Baba", count=12, spread=6.0)),
    dict(id="bebek_tnt", tr="Bebek TNT", en="Bebek TNT",
         trtip="Patladığında etrafa 'Bebek' isimli yavru köylüler saçar.",
         entip="Scatters baby villagers named 'Bebek' when it explodes.",
         color=((245, 178, 208), (245, 178, 208), (245, 178, 208)), mat="minecraft:emerald",
         effect=dict(kind="villagers", name="Bebek", count=15, spread=6.0, baby=True)),
    dict(id="bulut_tnt", tr="Bulut TNT", en="Cloud TNT",
         trtip="1 dakika yağmur başlatır ve gökyüzünü bulutlandırır.",
         entip="Starts 1 minute of rain and clouds over the sky.",
         color=((236, 240, 244), (150, 200, 232), (236, 240, 244)), mat="minecraft:white_wool",
         effect=dict(kind="weather", weather="Rain", weatherTicks=1200)),
    dict(id="ay_tnt", tr="Ay TNT", en="Moon TNT",
         trtip="Gündüzse güneşi aya dönüştürür — gece olur.",
         entip="Turns the sun into the moon - night falls.",
         color=((186, 190, 194), (186, 190, 194), (186, 190, 194)), mat="minecraft:glowstone_dust",
         effect=dict(kind="time", time=18000)),
    # DIKKAT: buff yalnizca OYUNCULARA. Java tarafinda 88b9de1 "Harf TNT'sinin
    # can/kalkan buff'u sadece oyunculara verilsin" ayni hatayi duzeltti:
    # tum LivingEntity'ye verilince zombi/iskelet guclenip cocuk icin
    # tehlikeli hale geliyordu. KalpTntEntity.java'da gozden kacmisti;
    # denetimde bulundu ve her iki tarafta da duzeltildi.
    dict(id="kalp_tnt", tr="Kalp TNT", en="Heart TNT",
         trtip="Yakındaki oyunculara can yenileme + kalkan verir, efektleri ve boyutu sıfırlar.",
         entip="Heals and shields nearby players, clears effects and size changes.",
         color=((140, 40, 46), (196, 48, 54), (92, 48, 52)), mat="minecraft:gold_ingot",
         effect=dict(kind="heal", radius=20)),
    dict(id="buz_tnt", tr="Buz TNT", en="Ice TNT",
         trtip="14 blok yarıçapı buzla kaplar. O alandaki herkesi (patlatan hariç) 30 sn dondurur. 30 sn yağış başlar (soğuk biyomda kar).",
         entip="Covers a 14 block radius with ice. Freezes everyone in that area (except the igniter) for 30s. Precipitation for 30s (snow in cold biomes).",
         color=((150, 200, 232), (176, 216, 240), (150, 200, 232)), mat="minecraft:packed_ice",
         effect=dict(kind="freeze", radius=14, freeze_seconds=30)),
    dict(id="kucultme_tnt", tr="Küçültme TNT", en="Shrink TNT",
         trtip="Yakındaki oyuncuları minicik yapar! Kalp TNT ile eski boyutuna dönersin.",
         entip="Shrinks nearby players tiny! Heart TNT restores your size.",
         color=((150, 90, 200), (176, 120, 224), (120, 70, 170)), mat="minecraft:amethyst_shard",
         effect=dict(kind="scale", radius=8, delta=-2)),
    dict(id="buyutme_tnt", tr="Büyütme TNT", en="Growth TNT",
         trtip="Yakındaki oyuncuları dev yapar! Kalp TNT ile eski boyutuna dönersin.",
         entip="Grows nearby players giant! Heart TNT restores your size.",
         color=((220, 120, 40), (240, 150, 60), (190, 96, 30)), mat="minecraft:pumpkin",
         effect=dict(kind="scale", radius=8, delta=2)),
    dict(id="cizgi_tnt", tr="Çizgi TNT", en="Line TNT",
         trtip="Etrafa kağıt, mürekkep ve tüy saçar — kendi defterini topla!",
         entip="Scatters paper, ink and feathers around - collect your own notebook!",
         color=((235, 232, 224), (210, 205, 195), (180, 176, 168)), mat="minecraft:paper",
         effect=dict(kind="scatter", spread=6.0, power=2, drops=[
             {"item": "minecraft:paper", "count": 30, "stack": 8},
             {"item": "minecraft:ink_sac", "count": 15, "stack": 4},
             {"item": "minecraft:feather", "count": 15, "stack": 4}])),
    dict(id="zeynep_redstone_tnt", tr="Zeynep Redstone TNT", en="Zeynep Redstone TNT",
         trtip="Dev patlama — patlatan hariç yakındaki herkesi yener! Dikkatli kullan.",
         entip="Huge blast - defeats everyone nearby except the igniter! Use with care.",
         color=((150, 40, 90), (190, 55, 110), (120, 30, 72)), mat="minecraft:redstone_block",
         effect=dict(kind="instakill", radius=30, power=8)),

    # ============ EASY: patlama / saçma / efekt / hava / zaman ============
    dict(id="gold_tnt", tr="Altın TNT", en="Gold TNT",
         trtip="Merkez patlama + etrafa 5 küçük patlama dalgası saçar.",
         entip="A centre blast plus 5 smaller explosion waves.",
         color=((222, 178, 40), (240, 200, 60), (200, 158, 30)), mat="minecraft:gold_ingot",
         effect=dict(kind="explode", power=7, waves=5, waveDist=6, wavePower=3)),
    dict(id="emerald_tnt", tr="Zümrüt TNT", en="Emerald TNT",
         trtip="Zümrüt, elmas, altın ve lapis yağdırır.",
         entip="Rains emeralds, diamonds, gold and lapis.",
         color=((30, 160, 90), (46, 190, 110), (24, 140, 78)), mat="minecraft:emerald",
         effect=dict(kind="scatter", spread=6.0, power=4, drops=[
             {"item": "minecraft:emerald", "count": 24, "stack": 2},
             {"item": "minecraft:gold_ingot", "count": 12, "stack": 2},
             {"item": "minecraft:lapis_lazuli", "count": 12, "stack": 3},
             {"item": "minecraft:diamond", "count": 10}])),
    dict(id="lightning_tnt", tr="Şimşek TNT", en="Lightning TNT",
         trtip="20 yıldırım çağırır. Yıldırım yakar ve öldürebilir — "
               "patlattıktan sonra uzaklaş!",
         entip="Summons 20 lightning bolts. Lightning burns and can kill - "
               "get away after lighting it!",
         color=((70, 90, 150), (110, 140, 210), (60, 76, 130)), mat="minecraft:lightning_rod",
         effect=dict(kind="spawn", entity="minecraft:lightning_bolt", count=20, spread=6.0, power=3)),
    dict(id="nuclear_tnt", tr="Nükleer TNT", en="Nuclear TNT",
         trtip="Dev patlama + radyasyon (wither) hasarı — öldürebilir!",
         entip="Huge explosion + radiation (wither) damage - can kill!",
         color=((60, 180, 60), (90, 220, 70), (44, 140, 44)), mat="minecraft:emerald",
         effect=dict(kind="status", target="all", radius=20, power=15, particle="minecraft:huge_explosion_emitter",
                     effects=[{"id": "poison", "seconds": 30, "amp": 3}, {"id": "slowness", "seconds": 30, "amp": 2},
                              {"id": "weakness", "seconds": 30, "amp": 2}, {"id": "blindness", "seconds": 10, "amp": 0},
                              {"id": "nausea", "seconds": 15, "amp": 0}, {"id": "wither", "seconds": 10, "amp": 1}])),
    dict(id="invisible_tnt", tr="Görünmez TNT", en="Invisible TNT",
         trtip="Taş kılığında — kimse fark etmez!",
         entip="Disguised as stone - nobody notices!",
         color=((124, 124, 124), (136, 136, 136), (112, 112, 112)), mat="minecraft:stone",
         effect=dict(kind="explode", power=4)),
    dict(id="redstone_tnt", tr="Redstone TNT", en="Redstone TNT",
         trtip="Çok güçlü patlama; 20 blok yarıçapındaki oyunculara hız ve güç verir.",
         entip="Very strong blast; gives nearby players speed and strength.",
         color=((190, 40, 40), (220, 55, 55), (150, 30, 30)), mat="minecraft:redstone_block",
         effect=dict(kind="status", target="players", radius=20, power=8, particle="minecraft:redstone_ore_dust_particle",
                     effects=[{"id": "speed", "seconds": 60, "amp": 2}, {"id": "strength", "seconds": 60, "amp": 1}])),
    dict(id="maden_tnt", tr="Maden TNT", en="Mine TNT",
         trtip="Patladığında her işlenmiş madenden 10 tane saçar.",
         entip="Scatters 10 of every processed mineral.",
         color=((110, 110, 118), (140, 140, 150), (92, 92, 100)), mat="minecraft:iron_ingot",
         effect=dict(kind="scatter", spread=6.0, power=3, drops=[
             {"item": m, "count": 10} for m in [
                 "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald",
                 "minecraft:copper_ingot", "minecraft:netherite_scrap", "minecraft:lapis_lazuli",
                 "minecraft:quartz", "minecraft:amethyst_shard", "minecraft:coal", "minecraft:redstone"]])),
    dict(id="crafting_table_tnt", tr="Çalışma Tezgahı TNT", en="Crafting Table TNT",
         trtip="Etrafa altın kasklar, netherite kılıçlar, demir baltalar ve bir netherite külçesi saçar.",
         entip="Scatters golden helmets, netherite swords, iron axes and a netherite ingot.",
         color=((150, 110, 66), (172, 128, 78), (120, 88, 52)), mat="minecraft:crafting_table",
         effect=dict(kind="scatter", spread=6.0, power=3, drops=[
             {"item": "minecraft:golden_helmet", "count": 8}, {"item": "minecraft:netherite_sword", "count": 6},
             {"item": "minecraft:iron_axe", "count": 8}, {"item": "minecraft:netherite_ingot", "count": 1}])),
    dict(id="dunya_tnt", tr="Dünya TNT", en="World TNT",
         trtip="Etrafa bir sürü 'dünya' (ender pearl, toprak, çim, su) saçar.",
         entip="Scatters a lot of 'world' (ender pearls, dirt, grass, water).",
         color=((70, 130, 180), (90, 160, 90), (110, 80, 50)), mat="minecraft:grass_block",
         effect=dict(kind="scatter", spread=6.0, power=2, drops=[
             {"item": "minecraft:ender_pearl", "count": 30}, {"item": "minecraft:dirt", "count": 20, "stack": 8},
             {"item": "minecraft:grass_block", "count": 15, "stack": 4}, {"item": "minecraft:water_bucket", "count": 15}])),
    dict(id="gokkusagi_tnt", tr="Gökkuşağı TNT", en="Rainbow TNT",
         trtip="Gerçeğinden bile güzel renkli yünler saçar!",
         entip="Scatters colorful wool - prettier than the real thing!",
         color=((220, 60, 60), (80, 180, 90), (70, 110, 200)), mat="minecraft:white_wool",
         effect=dict(kind="scatter", spread=7.0, power=1.5, drops=[
             {"item": f"minecraft:{c}_wool", "count": 6, "stack": 4} for c in
             ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple", "pink"]])),
    dict(id="zumrut_yagmuru_tnt", tr="Zümrüt Yağmuru TNT", en="Emerald Rain TNT",
         trtip="Patladığında etrafa zümrüt blokları saçar.",
         entip="Scatters emerald blocks when it explodes.",
         color=((30, 170, 95), (46, 200, 115), (24, 150, 82)), mat="minecraft:emerald_block",
         effect=dict(kind="scatter", spread=8.0, drops=[{"item": "minecraft:emerald_block", "count": 40}])),
    dict(id="iki_yuz_tl_tnt", tr="200 TL TNT", en="200 Lira TNT",
         trtip="Patladığında etrafa bir sürü kağıt para saçar.",
         entip="Scatters a lot of paper money when it explodes.",
         color=((110, 150, 120), (140, 180, 150), (90, 128, 100)), mat="minecraft:paper",
         effect=dict(kind="scatter", spread=8.0, drops=[{"item": "stnt:iki_yuz_tl", "count": 100, "stack": 4}])),
    dict(id="kurus_tnt", tr="Kuruş TNT", en="Coin TNT",
         trtip="Patladığında etrafa bir sürü madeni para saçar.",
         entip="Scatters a lot of coins when it explodes.",
         color=((196, 160, 90), (216, 182, 110), (168, 136, 74)), mat="minecraft:gold_nugget",
         effect=dict(kind="scatter", spread=9.0, drops=[{"item": "stnt:kurus", "count": 200, "stack": 8}])),
    dict(id="pasta_tnt", tr="Pasta TNT", en="Cake TNT",
         trtip="Patladığında etrafa pastalar saçar.",
         entip="Scatters cakes when it explodes.",
         color=((240, 220, 200), (248, 230, 214), (200, 160, 120)), mat="minecraft:cake",
         effect=dict(kind="scatter", spread=7.0, drops=[{"item": "minecraft:cake", "count": 30}])),
    dict(id="zebra_tnt", tr="Zebra TNT", en="Zebra TNT",
         trtip="Siyah-beyaz çizgili — kağıt saçar!",
         entip="Black and white stripes - scatters paper!",
         color=((240, 240, 240), (30, 30, 30), (240, 240, 240)), mat="minecraft:paper",
         effect=dict(kind="scatter", spread=7.0, drops=[{"item": "minecraft:paper", "count": 40, "stack": 4}])),
    dict(id="harf_tnt", tr="Harf TNT", en="Letter TNT",
         trtip="Etrafa kağıt saçar.",
         entip="Scatters paper everywhere.",
         color=((236, 232, 210), (244, 240, 220), (210, 205, 180)), mat="minecraft:paper",
         effect=dict(kind="scatter", spread=6.0, drops=[{"item": "minecraft:paper", "count": 30, "stack": 4}])),
    dict(id="zehir_tnt", tr="Zehir TNT", en="Poison TNT",
         trtip="Yakındaki herkese yavaşlatma ve zehir verir.",
         entip="Gives slowness and poison to everyone nearby.",
         color=((90, 160, 40), (110, 190, 50), (70, 128, 32)), mat="minecraft:spider_eye",
         effect=dict(kind="status", target="all", radius=20, particle="minecraft:mobspell_emitter",
                     effects=[{"id": "slowness", "seconds": 30, "amp": 2}, {"id": "poison", "seconds": 30, "amp": 1}])),
    dict(id="zeynep_komut_tnt", tr="Zeynep Komut TNT", en="Zeynep Command TNT",
         trtip="Yakındaki oyunculara tüm güçlü efektleri ve 10 Nether Yıldızı verir.",
         entip="Gives nearby players every strong effect and 10 Nether Stars.",
         color=((44, 62, 158), (80, 100, 200), (36, 50, 130)), mat="minecraft:nether_star",
         effect=dict(kind="status", target="players", radius=50, give={"item": "minecraft:nether_star", "count": 10},
                     particle="minecraft:totem_particle",
                     effects=[{"id": "speed", "seconds": 300, "amp": 4}, {"id": "strength", "seconds": 300, "amp": 4},
                              {"id": "regeneration", "seconds": 300, "amp": 4}, {"id": "resistance", "seconds": 300, "amp": 4},
                              {"id": "saturation", "seconds": 300, "amp": 4}, {"id": "night_vision", "seconds": 300, "amp": 0},
                              {"id": "fire_resistance", "seconds": 300, "amp": 0}])),
    dict(id="pirt_tnt", tr="Pırt TNT", en="Fart TNT",
         trtip="Etrafa pırt kokusu (yeşil bulut) saçar — ufak bulantı verir.",
         entip="Spreads a fart cloud (green) - mild nausea.",
         color=((150, 170, 60), (176, 196, 76), (124, 140, 48)), mat="minecraft:brown_mushroom",
         effect=dict(kind="status", target="players", radius=10, power=0.5, particle="minecraft:mobspell_emitter",
                     effects=[{"id": "nausea", "seconds": 10, "amp": 0}])),
    dict(id="cleanse_tnt", tr="Temizleyici TNT", en="Cleanse TNT",
         trtip="Tüm efektleri ve boyut değişikliklerini temizler.",
         entip="Clears all effects and size changes.",
         color=((236, 244, 250), (248, 252, 255), (210, 224, 236)), mat="minecraft:milk_bucket",
         effect=dict(kind="status", target="all", radius=20, clear=True, particle="minecraft:snowflake_particle", effects=[])),
    dict(id="yagmur_tnt", tr="Yağmur TNT", en="Rain TNT",
         trtip="1 dakika yağmur başlatır.",
         entip="Starts 1 minute of rain.",
         color=((90, 120, 150), (120, 150, 180), (74, 100, 128)), mat="minecraft:water_bucket",
         effect=dict(kind="weather", weather="Rain", weatherTicks=1200)),
    dict(id="simsek_yagmur_tnt", tr="Şimşek Yağmuru TNT", en="Thunderstorm TNT",
         trtip="Yakına 30 yıldırım yağdırır ve 1 dakika fırtına başlatır. "
               "Yıldırım yakar ve canlıyı öldürebilir — uzakta dur!",
         entip="Rains 30 lightning bolts nearby and starts a 1 minute storm. "
               "Lightning burns and can kill - keep your distance!",
         color=((60, 66, 90), (88, 96, 128), (48, 54, 74)), mat="minecraft:lightning_rod",
         effect=dict(kind="spawn", entity="minecraft:lightning_bolt", count=30, spread=10.0,
                     weather="Thunder", weatherTicks=1200)),
    dict(id="z_gunes_tnt", tr="Güneş TNT", en="Sun TNT",
         trtip="Geceyse ayı güneşe dönüştürür — gündüz olur.",
         entip="Turns the moon into the sun - day breaks.",
         color=((250, 220, 100), (255, 236, 140), (230, 190, 70)), mat="minecraft:glowstone",
         effect=dict(kind="time", time=6000)),
    dict(id="virus_tnt", tr="Virüs TNT", en="Virus TNT",
         trtip="Bir sürü virüs saçar... ama hepsi yok olur. Aslında hiçbir şey olmaz.",
         entip="Spreads a lot of viruses... but they all vanish. Nothing really happens.",
         color=((120, 200, 120), (150, 220, 150), (100, 170, 100)), mat="minecraft:slime_ball",
         effect=dict(kind="status", target="all", radius=1, effects=[], particle="minecraft:mobspell_emitter")),

    # ============ MEDIUM: blok yıkım / koyma / dönüştürme / özel ============
    dict(id="bedrock_tnt", tr="Kaya Katmanı TNT", en="Bedrock TNT",
         trtip="25 blok yarıçapında HER ŞEYİ yok eder - bedrock dahil!",
         entip="Destroys EVERYTHING in a 25 block radius - even bedrock!",
         color=((60, 60, 66), (78, 78, 84), (48, 48, 54)), mat="minecraft:bedrock",
         effect=dict(kind="break", radius=25, perTick=1000, power=4)),
    dict(id="cam_tnt", tr="Cam TNT", en="Glass TNT",
         trtip="Patlayınca 30 blok yarıçapındaki tüm cam bloklarını kırar!",
         entip="Breaks all glass within 30 blocks!",
         color=((190, 220, 230), (210, 236, 244), (170, 200, 212)), mat="minecraft:glass",
         effect=dict(kind="break", radius=30, filter="glass", perTick=400, power=2)),
    dict(id="kiyamet_tnt", tr="Kıyamet TNT", en="Doomsday TNT",
         trtip="Her şeyi yok eder ve tüm oyuncuları öldürür — sen de öldün.",
         entip="Destroys everything and kills all players - you died too.",
         color=((40, 20, 20), (90, 30, 30), (24, 12, 12)), mat="minecraft:wither_skeleton_skull",
         effect=dict(kind="break", radius=35, killPlayers=True, skipBedrock=True, perTick=2000, power=15)),
    dict(id="freeze_tnt", tr="Dondurucu TNT", en="Freeze TNT",
         trtip="9 blok yarıçapını buzla kaplar.",
         entip="Covers a 9 block radius with ice.",
         color=((150, 210, 240), (176, 224, 248), (150, 200, 232)), mat="minecraft:ice",
         effect=dict(kind="place", block="minecraft:packed_ice", radius=9, onlyAir=False,
                     particle="minecraft:snowflake_particle")),
    dict(id="water_tnt", tr="Su TNT", en="Water TNT",
         trtip="Ateşleri söndürür ve geçici su birikintileri bırakır (10 sn).",
         entip="Puts out fires and leaves temporary water (10s).",
         color=((50, 110, 200), (70, 140, 230), (40, 90, 170)), mat="minecraft:water_bucket",
         effect=dict(kind="place", block="minecraft:water", radius=10, onlyAir=True, tempSeconds=10,
                     douses=True, particle="minecraft:water_splash_particle_manual")),
    dict(id="olumcul_su_tnt", tr="Ölümcül Su TNT", en="Deadly Water TNT",
         trtip="15 blok yarıçapını suyla doldurur (30 sn sonra kurur).",
         entip="Fills a 15 block radius with water (dries after 30s).",
         color=((30, 70, 150), (44, 92, 180), (24, 56, 120)), mat="minecraft:water_bucket",
         effect=dict(kind="place", block="minecraft:water", radius=15, onlyAir=True, tempSeconds=30, perTick=700)),
    dict(id="mob_freeze_tnt", tr="Mob Dondurucu TNT", en="Mob Freeze TNT",
         trtip="14 blok yarıçapını geçici buza çevirir.",
         entip="Turns a 14 block radius into temporary ice.",
         color=((160, 210, 235), (186, 226, 246), (150, 200, 232)), mat="minecraft:blue_ice",
         effect=dict(kind="place", block="minecraft:ice", radius=14, onlyAir=False, tempSeconds=30, perTick=700)),
    dict(id="nether_tnt", tr="Nether TNT", en="Nether TNT",
         trtip="Patlayınca büyük netherrack adası yaratır!",
         entip="Creates a big netherrack island!",
         color=((110, 30, 30), (140, 44, 44), (86, 22, 22)), mat="minecraft:netherrack",
         effect=dict(kind="place", block="minecraft:netherrack", radius=12, onlyAir=False, perTick=700,
                     particle="minecraft:basic_flame_particle")),
    dict(id="end_tnt", tr="End TNT", en="End TNT",
         trtip="Patlayınca end stone adası yaratır!",
         entip="Creates an end stone island!",
         color=((220, 224, 170), (236, 240, 190), (200, 204, 150)), mat="minecraft:end_stone",
         effect=dict(kind="place", block="minecraft:end_stone", radius=10, onlyAir=False, perTick=700,
                     particle="minecraft:endrod")),
    dict(id="rainbow_tnt", tr="Gökkuşağı Yün TNT", en="Rainbow Wool TNT",
         trtip="Blokları renkli yüne dönüştürür (20 blok yarıçap).",
         entip="Turns blocks into colored wool (20 block radius).",
         color=((220, 60, 60), (90, 190, 90), (70, 110, 210)), mat="minecraft:white_wool",
         effect=dict(kind="transform", radius=20, perTick=1000, palette=[
             f"minecraft:{c}_wool" for c in ["red", "orange", "yellow", "lime", "green", "cyan",
             "light_blue", "blue", "purple", "magenta", "pink", "white"]])),
    dict(id="makarna_tnt", tr="Makarna TNT", en="Pasta TNT",
         trtip="Blokları yiyecek görünümlü bloklara dönüştürür - afiyet olsun!",
         entip="Turns blocks into food-looking blocks - enjoy!",
         color=((230, 200, 120), (244, 216, 140), (200, 170, 96)), mat="minecraft:wheat",
         effect=dict(kind="transform", radius=14, perTick=1000, palette=[
             "minecraft:cake", "minecraft:melon_block", "minecraft:pumpkin", "minecraft:hay_block",
             "minecraft:brown_mushroom_block", "minecraft:honey_block"])),
    dict(id="seker_tnt", tr="Şeker TNT", en="Candy TNT",
         trtip="Çikolata ve şeker dünyası yaratır + hız ve zıplama! (İnerken dikkat.)",
         entip="Creates a candy world + speed and jump boost! (Mind the landing.)",
         color=((236, 120, 180), (248, 150, 200), (210, 96, 156)), mat="minecraft:sugar",
         effect=dict(kind="transform", radius=14, perTick=1000, palette=[
             f"minecraft:{c}_glazed_terracotta" for c in ["pink", "magenta", "purple", "lime", "yellow", "light_blue"]],
             buff=[{"id": "speed", "seconds": 30, "amp": 1}, {"id": "jump_boost", "seconds": 30, "amp": 2}])),
    dict(id="gulen_yuz_tnt", tr="Gülen Yüz TNT", en="Smiley TNT",
         trtip="Çıngırak sesiyle dünyayı sarıya boyar! 12 blok yarıçap.",
         entip="Paints the world yellow with a giggle! 12 block radius.",
         color=((248, 220, 60), (255, 232, 90), (228, 196, 40)), mat="minecraft:yellow_dye",
         effect=dict(kind="transform", radius=12, perTick=800, sound="note.bell", palette=[
             "minecraft:yellow_wool", "minecraft:yellow_concrete", "minecraft:yellow_terracotta", "minecraft:gold_block"])),
    dict(id="karisik_kurusuk_tnt", tr="Karışık Kuruşuk TNT", en="Crumpled TNT",
         trtip="Dünyayı ezik büzük yapar — siyah/kahverengi/gri beton.",
         entip="Crumples the world - black/brown/gray concrete.",
         color=((70, 60, 56), (96, 82, 74), (54, 46, 42)), mat="minecraft:gravel",
         effect=dict(kind="transform", radius=18, perTick=600, palette=[
             "minecraft:black_concrete", "minecraft:brown_concrete", "minecraft:gray_concrete"])),
    dict(id="elmas_diyari_tnt", tr="Elmas Diyarı TNT", en="Diamond Land TNT",
         trtip="18 blok yarıçapındaki dünyayı elmas blokuna çevirir.",
         entip="Turns an 18 block radius into diamond blocks.",
         color=((100, 220, 220), (130, 236, 236), (80, 190, 190)), mat="minecraft:diamond_block",
         effect=dict(kind="transform", radius=18, perTick=800, palette=["minecraft:diamond_block"])),
    dict(id="magnet_tnt", tr="Mıknatıs TNT", en="Magnet TNT",
         trtip="3 saniye boyunca yakındaki her şeyi çeker, sonra BOOM!",
         entip="Pulls everything nearby for 3 seconds, then BOOM!",
         color=((180, 60, 60), (70, 70, 78), (150, 44, 44)), mat="minecraft:iron_ingot",
         effect=dict(kind="magnet", radius=15, pullTicks=12, power=5)),
    dict(id="swap_tnt", tr="Takas TNT", en="Swap TNT",
         trtip="Yakındaki tüm canlıların konumlarını rastgele karıştırır!",
         entip="Randomly swaps the positions of all nearby creatures!",
         color=((150, 90, 200), (176, 116, 220), (124, 72, 168)), mat="minecraft:ender_pearl",
         effect=dict(kind="swap", radius=15)),
    dict(id="gunes_tnt", tr="Güneş Kristal TNT", en="Sun Crystal TNT",
         trtip="15 End kristali yaratır. DİKKAT: kristale vurursan büyük patlar!",
         entip="Spawns 15 End crystals. CAREFUL: hitting one makes a big blast!",
         color=((250, 210, 90), (255, 226, 120), (230, 186, 66)), mat="minecraft:end_crystal",
         effect=dict(kind="spawn", entity="minecraft:ender_crystal", count=15, spread=8.0)),
    dict(id="uyku_tnt", tr="Uyku TNT", en="Sleep TNT",
         trtip="Patlatan dışındaki herkesi 30 sn 'uyutur': yavaşlama, körlük, bulantı, güçsüzlük, kazma yorgunluğu.",
         entip="Puts everyone but the igniter to 'sleep' for 30s: slowness, blindness, nausea, weakness, mining fatigue.",
         color=((150, 120, 200), (176, 148, 220), (124, 96, 168)), mat="minecraft:pink_wool",
         effect=dict(kind="status", target="all", radius=20, exceptIgniter=True, power=0.5,
                     particle="minecraft:mobspell_emitter",
                     effects=[{"id": "slowness", "seconds": 30, "amp": 4}, {"id": "blindness", "seconds": 30, "amp": 0},
                              {"id": "nausea", "seconds": 30, "amp": 0}, {"id": "mining_fatigue", "seconds": 30, "amp": 4},
                              {"id": "weakness", "seconds": 30, "amp": 4}])),
    dict(id="gizli_tnt", tr="Gizli TNT", en="Hidden TNT",
         trtip="Cam kılığında — 25 blok yarıçapında patlatan hariç her canlıyı anında öldürür!",
         entip="Disguised as glass - instantly kills every creature except the igniter within 25 blocks!",
         color=((190, 220, 230), (210, 236, 244), (170, 200, 212)), mat="minecraft:glass",
         effect=dict(kind="instakill", radius=25)),
    dict(id="magara_tnt", tr="Mağara TNT", en="Cave TNT",
         trtip="Yer altında mağara oyar ve korkunç yaratıklar doğurur!",
         entip="Carves a cave underground and spawns scary creatures!",
         color=((70, 66, 60), (92, 86, 78), (54, 50, 46)), mat="minecraft:stone",
         effect=dict(kind="spawn", entity="minecraft:zombie", count=15, spread=8.0, yoff=-2,
                     power=4, digs=True)),
    dict(id="kup_tnt", tr="Küp TNT", en="Cube TNT",
         trtip="Yerin içine doğru büyük bir küp şeklinde kazı yapar!",
         entip="Digs a big cube down into the ground!",
         color=((90, 90, 96), (112, 112, 118), (72, 72, 78)), mat="minecraft:stone",
         effect=dict(kind="break", radius=12, skipBedrock=True, perTick=1000, power=4, cube=True)),
    dict(id="lego_tnt", tr="Lego TNT", en="Lego TNT",
         trtip="Blokları renkli Lego tuğlalarına dönüştürür!",
         entip="Turns blocks into colored Lego bricks!",
         color=((200, 50, 50), (80, 130, 200), (240, 210, 60)), mat="minecraft:brick",
         effect=dict(kind="transform", radius=12, perTick=800, palette=[
             f"stnt:lego_{c}" for c in ["red", "blue", "yellow", "green", "orange", "purple", "lime", "white"]])),
    dict(id="wood_tnt", tr="Odun TNT", en="Wood TNT",
         trtip="Yakındaki ağaçları yok eder (gövde ve yapraklar).",
         entip="Destroys nearby trees (logs and leaves).",
         color=((110, 82, 48), (134, 100, 60), (90, 66, 40)), mat="minecraft:oak_log",
         effect=dict(kind="break", radius=10, filter=["log", "leaves", "wood"], perTick=500, power=1)),
    dict(id="gravity_tnt", tr="Yerçekimi TNT", en="Gravity TNT",
         trtip="Yakındaki canlıları 10 sn havaya kaldırır — inerken dikkat!",
         entip="Lifts nearby creatures for 10s - mind the landing!",
         color=((120, 90, 200), (146, 116, 220), (96, 72, 168)), mat="minecraft:feather",
         effect=dict(kind="status", target="all", radius=12, particle="minecraft:mob_portal",
                     effects=[{"id": "levitation", "seconds": 10, "amp": 2}])),
    dict(id="command_tnt", tr="Komut TNT", en="Command TNT",
         trtip="20 blok yarıçapındaki her şeyi yok eder.",
         entip="Destroys everything within 20 blocks.",
         color=((90, 70, 130), (116, 92, 164), (72, 56, 104)), mat="minecraft:command_block",
         effect=dict(kind="break", radius=20, skipBedrock=True, perTick=1000, power=3)),
    dict(id="dag_tnt", tr="Dağ TNT", en="Mountain TNT",
         trtip="Etrafa taştan bir tepe yükseltir!",
         entip="Raises a stone hill around it!",
         color=((100, 100, 96), (124, 124, 118), (80, 80, 76)), mat="minecraft:stone",
         effect=dict(kind="place", block="minecraft:stone", radius=8, onlyAir=True, perTick=800,
                     particle="minecraft:basic_smoke_particle")),
    dict(id="elmas_zirh_tnt", tr="Elmas Zırh TNT", en="Diamond Armor TNT",
         trtip="40 blok yarıçapındaki TÜM canlıları yok eder — dev patlama!",
         entip="Destroys ALL creatures within 40 blocks - massive blast!",
         color=((80, 220, 220), (110, 236, 236), (60, 190, 190)), mat="minecraft:diamond_block",
         effect=dict(kind="instakill", radius=40, exceptIgniter=False, power=18)),
    dict(id="ureyen_tnt", tr="Üreyen TNT", en="Breeding TNT",
         trtip="Güçlü bir patlamayla dünyayı sarsar!",
         entip="Shakes the world with a powerful blast!",
         color=((150, 80, 80), (180, 104, 104), (120, 64, 64)), mat="minecraft:tnt",
         effect=dict(kind="explode", power=6)),
    dict(id="walking_tnt", tr="Yürüyen TNT", en="Walking TNT",
         trtip="Büyük bir patlama yapar!",
         entip="Makes a big explosion!",
         color=((60, 40, 30), (90, 60, 44), (48, 32, 24)), mat="minecraft:tnt",
         effect=dict(kind="explode", power=8)),
]

FUSE_TICKS = 80  # Java tarafinda setFuse(80)

# ---------------------------------------------------------------- TNT-olmayan bloklar
# kind: "decor" (sadece dekoratif), "ghost" (carpismasiz, icinden gecilir),
#       "kill" (ustune/dokununca oldurur - script), "glow" (isik sacar)
# 16 renkli Lego + hayalet bloklar + ayna + altin plaka tuzaklari.
_LEGO_COLORS = {
    "white": (236, 236, 236), "orange": (216, 128, 40), "magenta": (190, 74, 190),
    "light_blue": (90, 160, 220), "yellow": (240, 210, 60), "lime": (120, 200, 60),
    "pink": (240, 150, 190), "gray": (80, 80, 86), "light_gray": (150, 150, 156),
    "cyan": (40, 150, 160), "purple": (130, 60, 180), "blue": (50, 70, 190),
    "brown": (120, 80, 50), "green": (80, 130, 50), "red": (200, 50, 50),
    "black": (30, 30, 34),
}
_LEGO_TR = {
    "white": "Beyaz", "orange": "Turuncu", "magenta": "Eflatun", "light_blue": "Açık Mavi",
    "yellow": "Sarı", "lime": "Fıstık Yeşili", "pink": "Pembe", "gray": "Gri",
    "light_gray": "Açık Gri", "cyan": "Camgöbeği", "purple": "Mor", "blue": "Mavi",
    "brown": "Kahverengi", "green": "Yeşil", "red": "Kırmızı", "black": "Siyah",
}

BLOCKS = []
for _cn, _rgb in _LEGO_COLORS.items():
    BLOCKS.append(dict(id=f"lego_{_cn}", tr=f"{_LEGO_TR[_cn]} Lego Tuğla", en=f"{_cn.title()} Lego Brick",
                       trtip="Çıkıntılı Lego tuğlası — inşa et!", entip="Studded Lego brick - build!",
                       kind="lego", color=_rgb, mat="minecraft:brick"))
BLOCKS += [
    dict(id="ghost_block", tr="Hayalet Blok", en="Ghost Block",
         trtip="Çim gibi görünür ama içinden geçilir — tuzak!",
         entip="Looks like grass but you walk through it - a trap!",
         kind="ghost", color=(90, 150, 70), mat="minecraft:grass"),
    dict(id="wooden_ghost_block", tr="Tahta Hayalet Blok", en="Wooden Ghost Block",
         trtip="Tahta gibi görünür ama içinden geçilir — tuzak!",
         entip="Looks like wood but you walk through it - a trap!",
         kind="ghost", color=(150, 110, 66), mat="minecraft:planks"),
    dict(id="mirror", tr="Ayna", en="Mirror",
         trtip="Karanlıkta parlayan dekoratif ayna.",
         entip="A decorative mirror that glows in the dark.",
         kind="glow", color=(210, 230, 240), mat="minecraft:glass"),
    dict(id="right_golden_plate", tr="Doğru Altın Plaka", en="Right Golden Plate",
         trtip="Altın plaka gibi görünür — ama tamamen zararsız.",
         entip="Looks like a gold plate - completely harmless.",
         kind="decor", color=(232, 200, 90), mat="minecraft:gold_ingot"),
    dict(id="wrong_golden_plate", tr="Yanlış Altın Plaka", en="Wrong Golden Plate",
         trtip="Altın plaka gibi görünür — üstüne basan anında ölür!",
         entip="Looks like a gold plate - whoever steps on it dies instantly!",
         kind="kill", color=(232, 200, 90), mat="minecraft:gold_ingot"),
    dict(id="light_bomb", tr="Işık Bombası", en="Light Bomb",
         trtip="Çok parlak ışık saçan dekoratif blok.",
         entip="A decorative block that gives off bright light.",
         kind="glow", color=(250, 246, 190), mat="minecraft:glowstone"),
    dict(id="zehir_toprak", tr="Zehir Toprağı", en="Poison Soil",
         trtip="Zehirli toprak — üstüne basan zehirlenir ve zarar görür!",
         entip="Poison soil - stepping on it poisons and hurts!",
         kind="poison", color=(96, 120, 54), mat="minecraft:dirt"),
    dict(id="soru_blogu", tr="? Bloğu", en="? Block",
         trtip="Kır — içinden rastgele bir şey çıkar!",
         entip="Break it - something random pops out!",
         kind="mystery", color=(232, 192, 64), mat="minecraft:gold_block"),
    dict(id="herobrine_spawner", tr="Herobrine Çağırıcı", en="Herobrine Spawner",
         trtip="Yerleştir — korkunç yaratıklar çağırır!",
         entip="Place it - summons scary creatures!",
         kind="spawner", color=(46, 34, 34), mat="minecraft:soul_sand"),
    dict(id="end_gate", tr="End Kapısı", en="End Gate",
         trtip="Üstüne çık — End boyutuna ışınlanırsın!",
         entip="Step on it - teleport to the End!",
         kind="portal_end", color=(52, 42, 82), mat="minecraft:obsidian"),
    dict(id="sahip_kapi", tr="Sahip Kapısı", en="Owner Door",
         trtip="Sadece koyan kişi açabilir — başkası geçemez!",
         entip="Only the placer can open it - nobody else passes!",
         kind="owner_door", color=(150, 110, 66), mat="minecraft:iron_door"),
    dict(id="blocker_sandik", tr="Kilitli Sandık", en="Blocker Chest",
         trtip="Sadece koyan kişi açabilir — başkası açamaz!",
         entip="Only the placer can open it - nobody else can!",
         kind="owner_chest", color=(140, 100, 60), mat="minecraft:chest"),
    dict(id="sifreli_sandik", tr="Şifreli Sandık", en="Password Chest",
         trtip="Şifre koy — doğru şifreyi giren açar! (Şifre ekranda görünür.)",
         entip="Set a password - whoever types it right opens it!",
         kind="password_chest", color=(210, 175, 80), mat="minecraft:gold_block"),
    dict(id="fake_tnt", tr="Sahte TNT", en="Fake TNT",
         trtip="TROL: pasta gibi görünür — kırınca ya da 'yiyince' sadece SENİ patlatır! Blok hasarı yok.",
         entip="TROLL: looks like cake - break or 'eat' it and only YOU blow up! No block damage.",
         kind="fake_tnt", color=(236, 224, 208), mat="minecraft:cake"),
    dict(id="yakinlik_mayini", tr="Yakınlık Mayını", en="Proximity Mine",
         trtip="Yaklaşan olursa patlar! Kurulunca 2 saniye içinde kaç. Blokları da yıkar.",
         entip="Detonates when something approaches! Run within 2s of arming. Breaks blocks too.",
         kind="proximity", color=(150, 70, 60), mat="minecraft:iron_ingot"),
    # Doku VANILLA'nin kum dosyasi (vtex): kendi PNG'miz yok, atlas girdisi
    # dogrudan vanilla RP yoluna bakar. Blok kiliklari ayni yolu entity
    # dokusu olarak kullaniyor ve tablette dogrulandi. Gucu vanilla TNT ile
    # ayni (KUM_POWER = 4); ipucu "TNT gibi" diyor, check_pack bunu tutuyor.
    dict(id="patlayici_kum", tr="Patlayıcı Kum", en="Explosive Sand",
         trtip="TUZAK: normal kum gibi görünür — kazan TNT gibi patlar! Blokları yıkar. "
               "Ortaya barut, kenarlara kum ile yapılır.",
         entip="TRAP: looks like normal sand - dig it and it explodes like TNT! Breaks blocks. "
               "Crafted with gunpowder in the middle and sand around it.",
         kind="explosive_sand", color=(219, 211, 160), vtex="textures/blocks/sand",
         mat="minecraft:sand",
         recipe=dict(pattern=["SSS", "SGS", "SSS"], key={"S": "minecraft:sand", "G": "minecraft:gunpowder"})),
]

# Mini bloklar: tam bloktan kucuk (hucre ortasinda 8x8x8 kup). Kucultme
# temasiyla uyumlu; cocuklar minik yapilar kurabilir. Her mini kendi
# hucresini kaplar ama KUCUK gorunur (bir hucreye birden fazla mini
# konamaz — Bedrock hucre basina tek blok; bu sinir kabul edildi).
_MINI_COLORS = {
    "red": (210, 60, 60), "orange": (230, 140, 40), "yellow": (240, 210, 60),
    "green": (90, 180, 70), "blue": (60, 110, 210), "purple": (140, 70, 190),
    "pink": (240, 150, 190), "white": (236, 236, 236),
}
_MINI_TR = {
    "red": "Kırmızı", "orange": "Turuncu", "yellow": "Sarı", "green": "Yeşil",
    "blue": "Mavi", "purple": "Mor", "pink": "Pembe", "white": "Beyaz",
}
for _cn, _rgb in _MINI_COLORS.items():
    BLOCKS.append(dict(id=f"mini_{_cn}", tr=f"{_MINI_TR[_cn]} Mini Blok", en=f"{_cn.title()} Mini Block",
                       trtip="Küçük dekoratif blok — minik yapılar kur!",
                       entip="A small decorative block - build tiny things!",
                       kind="mini", color=_rgb, mat="minecraft:clay_ball"))

# Renkli portal ciftleri: Portal Silahi yerlestirir. AYNI renk iki kapi bir cift
# olusturur; farkli ciftler farkli renk (cocuklar karistirmasin). Bir kapi
# kirilinca esi de kirilir (script: playerBreakBlock). Renk sayisi = PORTAL_N.
PORTAL_COLORS = [
    ("mavi", (60, 120, 240)), ("kırmızı", (230, 60, 60)), ("yeşil", (70, 200, 100)),
    ("sarı", (240, 215, 60)), ("mor", (175, 85, 235)), ("turuncu", (245, 140, 45)),
    ("camgöbeği", (60, 210, 210)), ("pembe", (240, 130, 200)),
]
for _i, (_pn, _pc) in enumerate(PORTAL_COLORS):
    BLOCKS.append(dict(id=f"portal_{_i}", tr=f"Portal ({_pn})", en=f"Portal ({_pn})",
                       trtip="Portal Silahı yerleştirir; aynı renk iki kapı birbirine ışınlar. Kırınca eşi de gider.",
                       entip="Placed by the Portal Gun; same-color pair teleports. Break one, its twin goes too.",
                       kind="portal_block", color=_pc, mat="minecraft:ender_pearl"))

# Atesler yaratici menuden GIZLI (hidden): yalniz cakmakla yakilir, Java'daki
# regBlockOnly ile ayni. Dokular Java'nin blok klasorunden (tex).
BLOCKS += [
    dict(id="ender_atesi", tr="Ender Ateşi", en="Ender Fire", kind="fire", hidden=True,
         trtip="Ender Çakmağı ile yakılır. İçine giren oyuncu başka bir ateşe ışınlanır. Sönmez; yumrukla kırılır.",
         entip="Lit by the Ender Flint and Steel. A player who steps in is teleported to another fire. Never goes out; punch to remove.",
         color=(80, 60, 200), tex="ender_fire", mat="minecraft:ender_pearl"),
    dict(id="altin_atesi", tr="Altın Ateş", en="Gold Fire", kind="fire", hidden=True,
         trtip="Altın Flint ile yakılır. Sönmez, yayılmaz; içine giren oyuncu yanar. Yumrukla kırılır.",
         entip="Lit by the Golden Flint. Never goes out, never spreads; a player who steps in burns. Punch to remove.",
         color=(240, 190, 60), tex="gold_fire", mat="minecraft:gold_ingot"),
    dict(id="tukenmezlik_orsu", tr="Tükenmezlik Örsü", en="Unending Anvil", kind="anvil",
         trtip="Elinde bir eşyayla dokun — o eşya TÜKENMEZ olur: yığın hep dolu kalır, alet ve zırh "
               "yıpranmaz, totem bitmez. Ok, blok, yiyecek... her eşyada çalışır. 8 elmas + örs ile yapılır.",
         entip="Tap it while holding an item - that item becomes UNENDING: the stack stays full, tools "
               "and armour never wear out, totems never run out. Works on arrows, blocks, food... any "
               "item. Crafted from 8 diamonds and an anvil.",
         color=(70, 70, 78), mat="minecraft:anvil",
         recipe=dict(pattern=["DDD", "DAD", "DDD"], key={"D": "minecraft:diamond", "A": "minecraft:anvil"})),
]

# ---------------------------------------------------------------- item tanimlari
# kind "food": yenince efekt. "drink": icince efekt (icme animasyonu).
# "raycast": bakilan bloga/varliga etki.
# "self_area": elde kullaninca oyuncunun etrafina etki.

# Saglik Iksiri sayilari. Bedrock'ta oyuncunun taban azami cani 20'dir ve
# betikten dogrudan degistirilemez; tek yol health_boost etkisi. Bu etki
# SEVIYE basina +4 can ekler, seviye = amplifier + 1. 200 can icin +180 can
# gerekir -> 45 seviye -> amplifier 44. Etki sadece azami cani buyutur,
# doldurmaz; betik icildikten sonra cani tepeye cekiyor (bkz. heal_boost).
POTION_BASE_HP = 20          # oyuncunun etkisiz taban azami cani


def heal_amp(hp):
    """Istenen azami can -> health_boost amplifier. Etki SEVIYE basina +4 can
    verir ve seviye = amplifier + 1."""
    assert (hp - POTION_BASE_HP) % 4 == 0, f"{hp} can 4'un kati degil"
    return (hp - POTION_BASE_HP) // 4 - 1


POTION_HP = 200
POTION_SECONDS = 600         # 10 dk; bitince azami can 20'ye doner
POTION_AMP = heal_amp(POTION_HP)
# Can Artirici: iksirin buyugu. Dev boss'lara karsi daha uzun sureli koz.
BOOST_AMP = 255              # Bedrock tavani; ustune cikan efekt HIC uygulanmaz
BOOST_HP = POTION_BASE_HP + 4 * (BOOST_AMP + 1)      # 1044
BOOST_SECONDS = 0            # 0 = sonsuz (dongu tazeler, bkz. FOREVER_TICKS)
# Tazeleme araligi ve efekte verilen sure. Sure araliktan COK daha uzun olmali:
# oyuncu tam tazeleme aninda cikip girse bile efekt ustunde kalsin.
FOREVER_EVERY = 40           # 2 sn'de bir tazele + cani tepeye cek
FOREVER_TICKS = 1200         # efekte verilen sure (60 sn)

# Mega Gubre'nin agac turu tablosu: fidan -> (govde, yaprak). Blok id'leri
# Mojang/bedrock-samples metadata/vanilladata_modules/mojang-blocks.json'dan
# dogrulandi. Kavak (poplar) icin duz "poplar_leaves" YOK, renkli uc cesit var.
# Listede olmayan bir seye tiklanirsa mese kullanilir.
TREE_WOOD = {f"minecraft:{k}_sapling": [f"minecraft:{k}_log", f"minecraft:{k}_leaves"]
             for k in ("oak", "birch", "spruce", "jungle", "acacia", "dark_oak",
                       "cherry", "pale_oak")}
TREE_WOOD["minecraft:poplar_sapling"] = ["minecraft:poplar_log",
                                         "minecraft:yellow_poplar_leaves"]
TREE_WOOD["minecraft:mangrove_propagule"] = ["minecraft:mangrove_log",
                                             "minecraft:mangrove_leaves"]

# Mega Gubre olculeri. Govde TREE_H blok, tepe yarıcapi TREE_CROWN -> tepe
# genisligi 2*TREE_CROWN + 1. Ipucu metni bu sayilardan yaziliyor.
TREE_H = 100
TREE_TRUNK = 5               # taban govde yaricapi (tepeye dogru incelir)
TREE_CROWN = 22              # tepe (yaprak) yaricapi
TREE_TOP = TREE_H + round(TREE_CROWN * 0.6)   # en ust yaprak katmani

# Sahte Elmas Aletlerin kazma hizi. minecraft:digger kademeleri: tahta 2,
# tas 4, demir 6, ELMAS 8, netherite 9. Kilik ancak alet gercekten elmas
# hizinda kazarsa tutuyor — yavas kazan bir "elmas" kazmayi cocuk ilk
# blokta anlar ve tuzak hic kurulmaz.
FAKE_DIG_SPEED = 8

# ---- Ender Atesi: isinlanma hedefleri. Cocuklarin tarifi "%90 / %50 / %1";
# uc sayi yuzde olarak 141 ediyor, bu yuzden AGIRLIK olarak uygulaniyor
# (normalize: ~%64 baska ender atesi, ~%35 Nether ruh atesi, ~%0,7 Nether
# turuncu atesi). Secilen turde yer yoksa siradaki denenir; hicbiri tutmazsa
# oyuncu yerinde kalir ve bunu eylem cubugunda gorur.
#
# Nether taramasi chunk ister; Nether'da oyuncu yoksa hicbir chunk yuklu
# degildir. Betik gecici bir tickingarea acar, chunk gelene kadar bekler,
# tarar, alani kaldirir. Tarama tick basina butceyle ilerler (Craft Baltasi
# deseni): 17x17x61 = 17 637 konum tek tick'te tableti dondururdu.
ENDER_FIRE = dict(
    ender=90, soul=50, fire=1,     # agirliklar: ender atesi / ruh atesi / turuncu ates
    cooldown=40,                   # varista yeniden isinlanmama suresi (tick)
    radius=8, minY=30, maxY=90,    # Nether taramasi: yatay yaricap ve Y araligi
    perTick=1500,                  # tick basina BAKILAN konum
    loadWait=100,                  # tickingarea sonrasi chunk bekleme tavani (tick)
    maxFires=128,                  # kayitli ender atesi tavani (en yeniler kalir)
)
# ---- Altin Ates: icine giren oyuncu yanar. Vanilla atesle ayni tempo:
# 10 tick'te 1 hasar + 8 sn alev.
GOLD_FIRE = dict(burn=8, damage=1, every=10)
# ---- Tukenmezlik: isaret esyanin LORE satiri (gorunur ve kalici). Dongu
# isaretli yigini dolu, aleti yipranmamis tutar.
UNENDING = dict(every=10, mark="§d✦ Tükenmez")

ITEMS = [
    dict(id="spicy_chips", tr="Acılı Cips", en="Spicy Chips", kind="food",
         trtip="Ye ve 20 sn Hız III kazan!", entip="Eat for Speed III for 20s!",
         color=(224, 120, 40), action=dict(type="eat", effect="speed", seconds=20, amp=2)),
    dict(id="saglik_iksiri", tr="Sağlık İksiri", en="Health Potion", kind="drink",
         trtip=f"İç — canın {POTION_HP} olur ve tamamen dolar! "
               f"{POTION_SECONDS // 60} dakika sürer, sonra normale döner.",
         entip=f"Drink - your health becomes {POTION_HP} and refills! "
               f"Lasts {POTION_SECONDS // 60} minutes, then back to normal.",
         color=(215, 40, 65),
         action=dict(type="heal_boost", hp=POTION_HP,
                     seconds=POTION_SECONDS, amp=POTION_AMP)),
    dict(id="can_artirici", tr="Can Artırıcı", en="Health Booster", kind="drink",
         trtip=f"İç — canın {BOOST_HP} olur ve SÜREKLİ dolu kalır. Süresi yok, "
               f"hiç bitmez; çıkıp girsen de durur. Vazgeçersen Temizleyici "
               f"TNT ile normale dönersin.",
         entip=f"Drink - your health becomes {BOOST_HP} and stays full FOREVER. "
               f"No timer, and it survives a relog. Use the Cleanse TNT to go "
               f"back to normal.",
         color=(240, 180, 40),
         action=dict(type="heal_boost", hp=BOOST_HP, forever=True,
                     seconds=BOOST_SECONDS, amp=BOOST_AMP)),
    dict(id="blok_kiligi", tr="Blok Kılığı", en="Block Disguise", kind="raycast",
         trtip="Bu elindeyken bir blok KIR — o bloğa dönüşürsün. Blokken bir "
               "yere basılı tut, oraya blok gibi yerleşirsin. Gökyüzüne bakıp "
               "basılı tut, insana dönersin. Saklambaç için birebir!",
         entip="Break a block while holding this - you become that block. As a "
               "block, hold on a spot to settle there like a placed block. Look "
               "at the sky and hold to turn back into a human. Perfect for "
               "hide-and-seek!",
         color=(120, 120, 130),
         action=dict(type="block_morph")),
    dict(id="isim_degistirici", tr="İsim Değiştirme", en="Name Changer", kind="raycast",
         trtip="Basılı tut — çıkan kutuya yeni ismini yaz, rengini seç. Diğer "
               "oyuncular tepende o ismi görür. En fazla 20 harf. Kutuyu boş "
               "bırakıp onaylarsan gerçek ismine dönersin. Dönüşmüşken isim "
               "zaten gizlidir; insana dönünce yeni ismin görünür.",
         entip="Hold - type your new name in the box and pick a colour. Other "
               "players see that name above your head. Up to 20 letters. Leave "
               "it empty to go back to your real name. While morphed the name "
               "stays hidden; it shows again when you turn back.",
         color=(190, 160, 80),
         action=dict(type="rename", maxLen=20)),
    dict(id="mega_gubre", tr="Mega Gübre", en="Mega Fertilizer", kind="raycast",
         trtip=f"Bir fidana sağ tıkla — gövdesi {TREE_H} blok, yapraklarıyla "
               f"{TREE_TOP} blok yüksekliğinde DEV bir ağaç büyür! Tepesi "
               f"{2 * TREE_CROWN + 1} blok geniş. Gözünün önünde, aşağıdan yukarı "
               f"büyür. Toprağa tıklarsan meşe olur; üstünde {TREE_TOP} blok boş "
               f"yer yoksa büyümez.",
         entip=f"Right-click a sapling - grows a GIANT tree: a {TREE_H}-block trunk, "
               f"{TREE_TOP} blocks tall with its leaves, crown {2 * TREE_CROWN + 1} "
               f"blocks wide. It grows bottom-up before your eyes. Click plain ground "
               f"and you get an oak; it needs {TREE_TOP} blocks of headroom.",
         color=(120, 200, 70),
         action=dict(type="mega_tree", height=TREE_H, trunk=TREE_TRUNK,
                     crown=TREE_CROWN, perTick=400)),
    dict(id="ses_saldirisi", tr="Ses Saldırısı", en="Sonic Attack", kind="raycast",
         trtip="Sağ tıkla — Warden gibi ses dalgası fırlatır! 24 blok gider, "
               "duvarlardan geçer, önüne çıkan herkesi vurup savurur.",
         entip="Right-click - fires a Warden-style sonic boom! Travels 24 blocks, "
               "passes through walls, damages and throws everything in its path.",
         color=(45, 130, 155),
         action=dict(type="sonic", range=24, damage=14, knock=1.8)),
    dict(id="ejderha_nefesi", tr="Ejderha Nefesi", en="Dragon's Breath", kind="raycast",
         trtip="Sağ tıkla — baktığın yere ejderhanın mor nefesini saçar! "
               "5 blok yarıçapında bir bulut olur, 12 saniye kalır ve içindeki "
               "herkesin saniyede 1 kalbini götürür. Kendine de zarar verir — "
               "bulutun içine girme!",
         entip="Right-click - sprays the dragon's purple breath where you look! "
               "A 5-block-radius cloud that lasts 12 seconds and takes 1 full "
               "heart per second from everything inside. It hurts you too - "
               "stay out of the cloud!",
         color=(120, 45, 190),
         action=dict(type="dragon_breath", radius=5, damage=2, seconds=12,
                     pulse=20, reach=40, cd=60)),
    dict(id="lightning_spell", tr="Yıldırım Büyüsü", en="Lightning Spell", kind="raycast",
         trtip="Sağ tıkla — baktığın yere GERÇEK yıldırım çakar! Yakar ve öldürür.",
         entip="Right-click - strikes REAL lightning where you look! It burns and kills.",
         color=(110, 140, 210), action=dict(type="lightning")),
    dict(id="black_hole", tr="Kara Delik", en="Black Hole", kind="self_area",
         trtip="Sağ tıkla — yakındaki her şeyi çeker, kör eder ve hasar verir.",
         entip="Right-click - pulls, blinds and damages nearby entities.",
         color=(40, 30, 60), action=dict(type="blackhole", radius=12)),
    dict(id="energy_crystal", tr="Enerji Kristali", en="Energy Crystal", kind="raycast",
         trtip="Sağ tıkla — baktığın bedrock'u (kaya katmanını) kırar!",
         entip="Right-click - breaks the bedrock you look at!",
         color=(120, 220, 220), action=dict(type="break_bedrock")),
    dict(id="laser_sword", tr="Lazer Kılıcı", en="Laser Sword", kind="raycast",
         trtip="Sağ tıkla — önündeki bloklarda 9 bloklık lazer açar! "
               "Lazerin içinde kalan canlıyı da yakar (15 kalp).",
         entip="Right-click - carves a 9-block laser ahead! It also burns "
               "anything caught in the beam (15 hearts).",
         color=(210, 40, 40), action=dict(type="laser", length=9)),
    dict(id="grappling_hook", tr="Kanca", en="Grappling Hook", kind="raycast",
         trtip="Sağ tıkla — baktığın yere doğru fırlarsın!",
         entip="Right-click - you fling toward where you look!",
         color=(90, 90, 100), action=dict(type="grapple")),
    dict(id="dondurucu", tr="Dondurucu", en="Freezer", kind="raycast",
         trtip="Sağ tıkla — baktığın canlıyı dondurur!",
         entip="Right-click - freezes the creature you look at!",
         color=(150, 210, 240), action=dict(type="freeze_target")),
    dict(id="koku_bombasi", tr="Koku Bombası", en="Stink Bomb", kind="self_area",
         trtip="Sağ tıkla — etrafa zehirli koku saçar!",
         entip="Right-click - spreads a poisonous stink!",
         color=(120, 150, 50), action=dict(type="poison_area", radius=6)),
    dict(id="among_us_report", tr="Among Us Rapor", en="Among Us Report", kind="raycast",
         trtip="Sağ tıkla — baktığın canlıyı anında öldürür! Tek kullanımlık.",
         entip="Right-click - instantly kills what you look at! Single use.",
         color=(200, 60, 60), action=dict(type="kill_target")),
    dict(id="hiz_esyasi", tr="Hız Eşyası", en="Speed Item", kind="food",
         trtip="Kullan ve 60 sn süper hız kazan!", entip="Use for 60s of super speed!",
         color=(90, 200, 230), action=dict(type="eat", effect="speed", seconds=60, amp=3)),
    dict(id="delici", tr="Delici Aleti", en="Driller", kind="raycast",
         trtip="TROL: sağ tıkla — önündeki her şeyi deler! Eşya düşmez.",
         entip="TROLL: right-click - drills everything ahead! No drops.",
         color=(120, 120, 128), action=dict(type="tunnel", length=12, size=1)),
    dict(id="lava_crystal", tr="Lav Kristali", en="Lava Crystal", kind="passive",
         trtip="Elde tutarken ateş ve lav sana zarar vermez.",
         entip="While held, fire and lava can't hurt you.",
         color=(220, 90, 30), action=dict(type="held_fireproof")),
    dict(id="blood_sword", tr="Kanlı Kılıç", en="Blood Sword", kind="weapon",
         trtip="Vurduğun canlıdan kırmızı damlalar sıçrar!",
         entip="Hit creatures to spray red droplets!",
         color=(160, 20, 20), damage=8, action=dict(type="bleed")),
    dict(id="heart_axe", tr="Kalp Baltası", en="Heart Axe", kind="weapon",
         trtip="Tek vuruşta mob öldürür; oyunculara ağır hasar! "
               "Hiç eskimez — hayatta kalma modunda bile kırılmaz.",
         entip="One-shots mobs; heavy damage to players! It never wears out - "
               "unbreakable even in survival.",
         color=(200, 40, 90), damage=7, unbreakable=True,
         action=dict(type="heavy")),
    dict(id="rainbow_boots", tr="Gökkuşağı Botları", en="Rainbow Boots", kind="boots",
         trtip="Giy — adım attığın yerde renkli yün izi bırakırsın!",
         entip="Wear them - leave a colored wool trail where you step!",
         color=(200, 60, 160), action=dict(type="worn_wool")),
    # ganimet item'lari (TNT'ler bunlari sacar; kendi baslarina davranissiz)
    dict(id="kurus", tr="1 Kuruş", en="1 Kurus", kind="loot",
         trtip="1 kuruşluk madeni para.", entip="A 1-kurus coin.", color=(196, 160, 90)),
    dict(id="iki_yuz_tl", tr="200 TL", en="200 Lira", kind="loot",
         trtip="200 TL'lik banknot.", entip="A 200-lira banknote.", color=(110, 150, 120)),
    dict(id="pink_lego_brick", tr="Pembe Lego Parçası", en="Pink Lego Piece", kind="loot",
         trtip="Toplanacak pembe Lego parçası. Konulabilen tuğla ayrı bir bloktur.",
         entip="A collectible pink Lego piece. The placeable brick is a separate block.",
         color=(240, 150, 190)),
    dict(id="green_lego_brick", tr="Yeşil Lego Parçası", en="Green Lego Piece", kind="loot",
         trtip="Toplanacak yeşil Lego parçası. Konulabilen tuğla ayrı bir bloktur.",
         entip="A collectible green Lego piece. The placeable brick is a separate block.",
         color=(80, 130, 50)),
    # atilabilir (mermi varligi yerine: bakilan yone isinla / etki)
    dict(id="end_pearl", tr="End İncisi", en="End Pearl", kind="raycast",
         trtip="Sağ tıkla — baktığın yere ışınlanırsın!",
         entip="Right-click - teleport to where you look!",
         color=(60, 130, 110), action=dict(type="teleport")),
    dict(id="nether_pearl", tr="Nether İncisi", en="Nether Pearl", kind="raycast",
         trtip="Sağ tıkla — baktığın yere ışınlanır, Ateş Çubuğu alırsın!",
         entip="Right-click - teleport and get a Blaze Rod!",
         color=(150, 60, 40), action=dict(type="teleport", give="minecraft:blaze_rod")),
    dict(id="tnt_frisbee", tr="TNT Frizbi", en="TNT Frisbee", kind="raycast",
         trtip="Sağ tıkla — baktığın yerde artı şeklinde yıkım!",
         entip="Right-click - cross-shaped destruction where you look!",
         color=(220, 60, 60), action=dict(type="frisbee")),
    dict(id="craft_axe", tr="Craft Baltası", en="Craft Axe", kind="raycast",
         trtip="Sağ tıkla — baktığın yeri işaretle, tekrar tıkla arası dolsun.",
         entip="Right-click a point, right-click again to fill between.",
         color=(150, 110, 66), action=dict(type="fill_axe")),
    # Takim Asasi: bir mob'a dokun/vur -> senin takimina katilir. Ayni takimdaki
    # mob'lar birbirini olduremez (afterEvents.entityHurt ile hasar geri
    # iyilestirilir). Etiket save/reload'da mob'da kalir. Tablette dusman moba
    # dokunmak saldiri oldugundan hem interact hem entityHitEntity dinlenir.
    dict(id="takim_asasi", tr="Takım Asası", en="Team Wand", kind="raycast",
         trtip="Bir mob'a dokun — takımına katılır. Aynı takımdakiler birbirine saldırmaz!",
         entip="Tap a mob - it joins your team. Same-team mobs won't attack each other!",
         color=(80, 200, 140), action=dict(type="team_wand")),
    # Donusum Asasi: bir mob'a dokun -> o mob'un gorunumune don. Bosluga sag
    # tik -> insana don. 15 mob (bkz MORPHS). Gorunum degisir, carpisma player.
    dict(id="donusum_asasi", tr="Dönüşüm Asası", en="Morph Wand", kind="raycast",
         trtip="Bir mob'a dokun — o mob olursun! Boşluğa sağ tık — tüm yaratıkların listesi açılır (insana dönmek de orada). Bazılarının özel gücü var: ÇÖMEL (sneak) dene.",
         entip="Tap a mob - become it! Right-click air - opens the full creature list (turning back to human is there too). Some have powers: try SNEAK.",
         color=(150, 70, 220), action=dict(type="morph_reset")),
    # boyut toplari: sag tiklayinca KENDINI bir kademe kucultur/buyutur.
    # (Bedrock'ta atilan mermiyle baskasini kucultmek yerine kendine
    #  uygulamak daha guvenilir; player.json st:size ozelligini surer.)
    dict(id="kucultme_topu", tr="Küçültme Topu", en="Shrink Ball", kind="raycast",
         trtip="Sağ tıkla — bir kademe küçülürsün! En küçükte minicik olursun.",
         entip="Right-click - shrink one step! Tiny at the smallest.",
         color=(150, 90, 200), action=dict(type="resize", delta=-1)),
    dict(id="buyutme_topu", tr="Büyütme Topu", en="Growth Ball", kind="raycast",
         trtip="Sağ tıkla — bir kademe büyürsün! En büyükte dev olursun.",
         entip="Right-click - grow one step! Giant at the biggest.",
         color=(220, 120, 40), action=dict(type="resize", delta=1)),
    dict(id="normal_boyut_topu", tr="Normal Boyut Topu", en="Reset Size Ball", kind="raycast",
         trtip="Sağ tıkla — normal boyutuna dönersin.",
         entip="Right-click - return to normal size.",
         color=(80, 180, 100), action=dict(type="resize", reset=True)),
    dict(id="esya_calmaca", tr="Eşya Çalmaca", en="Item Stealer", kind="raycast",
         trtip="TROL: sağ tıkla — en yakın oyuncunun bir eşyasını çalar!",
         entip="TROLL: right-click - steals an item from the nearest player!",
         color=(90, 60, 120), action=dict(type="steal", radius=8)),
    dict(id="kontrol_kumandasi", tr="Kontrol Kumandası", en="Control Remote", kind="raycast",
         trtip="Sağ tıkla — yerleştirdiğin tüm TNT'leri uzaktan ateşler!",
         entip="Right-click - remotely ignites all TNT you placed!",
         color=(40, 44, 52), action=dict(type="remote_detonate")),
    dict(id="portal_silahi", tr="Portal Silahı", en="Portal Gun", kind="raycast",
         trtip="Sağ tıkla — bir portal koy, tekrar tıkla ikincisini koy. Aralarında ışınlan!",
         entip="Right-click a portal, again for the second. Teleport between them!",
         color=(120, 90, 220), action=dict(type="portal_gun", range=48)),
    # TNT Zirhi: giyen hasar alinca saldirgana kucuk patlama misilleme yapar.
    dict(id="tnt_armor_kask", tr="TNT Kask", en="TNT Helmet", kind="tnt_armor",
         slot="slot.armor.head", trtip="Giy — sana vurana TNT patlamasıyla karşılık verir!",
         entip="Wear it - retaliates with a TNT blast against attackers!", color=(196, 48, 54)),
    dict(id="tnt_armor_govus", tr="TNT Göğüslük", en="TNT Chestplate", kind="tnt_armor",
         slot="slot.armor.chest", trtip="Giy — sana vurana TNT patlamasıyla karşılık verir!",
         entip="Wear it - retaliates with a TNT blast against attackers!", color=(196, 48, 54)),
    dict(id="tnt_armor_pantolon", tr="TNT Pantolon", en="TNT Leggings", kind="tnt_armor",
         slot="slot.armor.legs", trtip="Giy — sana vurana TNT patlamasıyla karşılık verir!",
         entip="Wear it - retaliates with a TNT blast against attackers!", color=(196, 48, 54)),
    dict(id="tnt_armor_bot", tr="TNT Bot", en="TNT Boots", kind="tnt_armor",
         slot="slot.armor.feet", trtip="Giy — sana vurana TNT patlamasıyla karşılık verir!",
         entip="Wear it - retaliates with a TNT blast against attackers!", color=(196, 48, 54)),
    # Ender Send: dev bir enderman-benzeri boss cagiran yumurta.
    dict(id="ender_send_yumurta", tr="Ender Send Yumurtası", en="Ender Send Egg", kind="spawn_egg",
         trtip="Sağ tıkla — DEV bir Ender Send çağırır! 300 can, çok güçlü.",
         entip="Right-click - summons a GIANT Ender Send! 300 HP, very strong.",
         color=(40, 30, 60), spawn="stnt:ender_send"),
    dict(id="dev_zombi_yumurta", tr="Dev Zombi Yumurtası", en="Giant Zombie Egg", kind="spawn_egg",
         trtip="Sağ tıkla — DEV bir zombi çağırır! 250 can, ağır yumruk.",
         entip="Right-click - summons a GIANT zombie! 250 HP, heavy punch.",
         color=(60, 100, 50), spawn="stnt:dev_zombi"),
    dict(id="dev_creeper_yumurta", tr="Dev Creeper Yumurtası", en="Giant Creeper Egg", kind="spawn_egg",
         trtip="Sağ tıkla — DEV bir creeper çağırır! Yaklaşınca kocaman patlar!",
         entip="Right-click - summons a GIANT creeper! Blows up huge when close!",
         color=(60, 160, 60), spawn="stnt:dev_creeper"),
    dict(id="mutant_warden_yumurta", tr="Mutant Warden Yumurtası", en="Mutant Warden Egg", kind="spawn_egg",
         trtip="Sağ tıkla — DEV bir Mutant Warden çağırır! 500 can, çok tehlikeli!",
         entip="Right-click - summons a GIANT Mutant Warden! 500 HP, very dangerous!",
         color=(40, 70, 82), spawn="stnt:mutant_warden"),
]

# ---- Sahte Elmas Aletler (TROL). Elmas takimi gibi gorunur, elmas kadar
# vurur ve elmas hizinda kazar — ta ki ILK kullanima kadar: bir canliya
# vurunca ya da bir blok kirinca elindeki alet ayni turden TAHTA alete
# doner (script: revealFake).
#
# Tooltip'ler TEK SABLONDAN uretilir. Bes aletin de tetikleyicisi ayni;
# elle yazilsalardi biri "ilk blokta", oteki "ilk vurusta" der ve tooltip
# koda yalan soylemeye baslardi — tooltip bu projede sozlesmedir.
#
# Alanlar: wood = donusulecek vanilla esya, woodtr = ekrana yazilan ad,
# dat = ayni adin Turkce yonelme hali ("Tahta Kazma'ya"), wooden = Ingilizce
# adi, tur = vanilla alet turu, dig = ek malzeme etiketleri.
#
# KAZMA HIZI IKI ETIKET KUMESINDEN gelir ve sirasi onemli degil, birlesimi
# onemli: her alet once kendi "is_<tur>_item_destructible" etiketini alir —
# bu, o alet turunun kirmasi GEREKEN tam blok kumesidir — sonra ustune
# malzeme etiketleri (stone, wood, sand...) binyor. Ikinci kume tek basina
# birakilsaydi, adini yanlis yazdigim bir etiket sessizce "hiz yok" demek
# olurdu: Molang query.any_tag taninmayan etikete hata vermez, false doner.
# Iki kume birbirini boylece yedekliyor.
# Hasar degerleri Bedrock elmas takimindan: kilic 7, balta 6, kazma 5,
# kurek 5, capa 1.
FAKE_TOOLS = [
    dict(id="sahte_elmas_kilic", tr="Sahte Elmas Kılıç", en="Fake Diamond Sword",
         obj_tr="kılıç", obj_en="sword", wood="minecraft:wooden_sword",
         woodtr="Tahta Kılıç", dat="Tahta Kılıç'a", wooden="Wooden Sword", damage=7,
         tur="sword"),
    dict(id="sahte_elmas_kazma", tr="Sahte Elmas Kazma", en="Fake Diamond Pickaxe",
         obj_tr="kazma", obj_en="pickaxe", wood="minecraft:wooden_pickaxe",
         woodtr="Tahta Kazma", dat="Tahta Kazma'ya", wooden="Wooden Pickaxe", damage=5,
         tur="pickaxe", dig="'stone', 'metal', 'diamond_pick_diggable'"),
    dict(id="sahte_elmas_balta", tr="Sahte Elmas Balta", en="Fake Diamond Axe",
         obj_tr="balta", obj_en="axe", wood="minecraft:wooden_axe",
         woodtr="Tahta Balta", dat="Tahta Balta'ya", wooden="Wooden Axe", damage=6,
         tur="axe", dig="'wood'"),
    dict(id="sahte_elmas_kurek", tr="Sahte Elmas Kürek", en="Fake Diamond Shovel",
         obj_tr="kürek", obj_en="shovel", wood="minecraft:wooden_shovel",
         woodtr="Tahta Kürek", dat="Tahta Kürek'e", wooden="Wooden Shovel", damage=5,
         tur="shovel", dig="'sand', 'dirt', 'gravel'"),
    dict(id="sahte_elmas_capa", tr="Sahte Elmas Çapa", en="Fake Diamond Hoe",
         obj_tr="çapa", obj_en="hoe", wood="minecraft:wooden_hoe",
         woodtr="Tahta Çapa", dat="Tahta Çapa'ya", wooden="Wooden Hoe", damage=1,
         tur="hoe", dig="'leaves'"),
]
ITEMS += [dict(f, kind="fake_tool", color=(93, 222, 212),
               trtip=f"TROL: Gerçek elmas {f['obj_tr']} gibi görünür ve öyle çalışır "
                     f"— ama bir canlıya vurduğun ya da bir blok kırdığın anda "
                     f"{f['dat']} dönüşür!",
               entip=f"TROLL: Looks and works like a real diamond {f['obj_en']} - but "
                     f"the moment you hit something or break a block it turns into a "
                     f"{f['wooden']}!")
          for f in FAKE_TOOLS]

# ---- Cakmaklar. Ikisi de "raycast": bakilan blogun USTUNE ates koyar.
# Dayaniklilik + enchantable(flintsteel): orste Tamir ve Kirilmazlik basilir.
ITEMS += [
    dict(id="ender_cakmagi", tr="Ender Çakmağı", en="Ender Flint and Steel", kind="raycast",
         trtip="Baktığın bloğun üstüne mavi Ender Ateşi yakar. Ateşe giren oyuncu ışınlanır: "
               "çoğunlukla başka bir Ender Ateşi'ne, bazen Nether'daki bir ruh ateşine, çok "
               "seyrek Nether'daki turuncu ateşe. Nether ateşleri yakar! Gidecek yer yoksa "
               "olduğun yerde kalırsın. 64 kullanım.",
         entip="Lights a blue Ender Fire on top of the block you look at. A player who steps in "
               "is teleported: most often to another Ender Fire, sometimes to a soul fire in the "
               "Nether, very rarely to an orange Nether fire. Nether fires burn! If there is "
               "nowhere to go you stay put. 64 uses.",
         color=(120, 70, 200), durability=64, enchant_slot="flintsteel",
         recipe=dict(pattern=["EEE", "EFE", "EEE"],
                     key={"E": "minecraft:ender_pearl", "F": "minecraft:flint_and_steel"}),
         action=dict(type="ender_fire", range=6)),
    dict(id="altin_flint", tr="Altın Flint", en="Golden Flint", kind="raycast",
         trtip="Baktığın bloğu altın bloğuna çevirir ve üstüne Altın Ateş yakar. Ateş sönmez, "
               "su söndürmez, yayılmaz; içine giren oyuncu yanar. Yumrukla kırılır. Bedrock ve "
               "sandık gibi bloklar altına dönmez. 64 kullanım; örste Tamir büyüsü basılabilir.",
         entip="Turns the block you look at into a block of gold and lights a Gold Fire on top. "
               "The fire never goes out, water does not put it out, it never spreads; a player "
               "who steps in burns. Punch it to remove. Bedrock and containers are not turned "
               "to gold. 64 uses; Mending can be applied on an anvil.",
         color=(240, 190, 60), durability=64, enchant_slot="flintsteel",
         # Cocuklarin tarifi: solda altin kulce, ortada demir kulce, sagda ham altin.
         recipe=dict(pattern=["AIH"],
                     key={"A": "minecraft:gold_ingot", "I": "minecraft:iron_ingot",
                          "H": "minecraft:raw_gold"}),
         action=dict(type="gold_fire", range=6)),
]

# ---------------------------------------------------------------- canavarlar
# Ender Send + dev boss'lar. Vanilla mob gorseli olceklenir (materials/texture/
# geometry MC saglar). Adlar MorphX'in calisan dosyasindan dogrulandi.
# spawn egg ITEMS'te (kind=spawn_egg, entity_placer ile cagirir).
MONSTERS = [
    # OZGUN 3 kafali model (custom=True): kafalar tepede. Model zaten uzun
    # (~3.5 blok) modellendi -> scale 2.4 ile ~8 blok dev boss.
    dict(id="ender_send", custom=True, hp=300, scale=2.4, dmg=15, cw=2.0, ch=9.0,
         extra={"minecraft:knockback_resistance": {"value": 0.85}},
         # Vanilla Warden teknigi: bacak/kol donusu modified_distance_moved'dan
         # (harekete senkron). animation.ender_send.move bu degiskenleri baglar.
         pre_anim=[
             "variable.es_speed = Math.min(0.5, 3.0 * query.modified_move_speed);",
             "variable.es_phase = query.modified_distance_moved * 49.388962;",
             "variable.es_leg = 55.0 * math.cos(variable.es_phase) * variable.es_speed;",
             "variable.es_arm_cos = -(38.0 * math.cos(variable.es_phase) * variable.es_speed);",
             "variable.es_arm_sin = -(38.0 * math.sin(variable.es_phase) * variable.es_speed);",
             "variable.es_body = 5.0 * math.cos(variable.es_phase) * variable.es_speed;",
         ]),
    dict(id="dev_zombi", mat="zombie", tex="textures/entity/zombie/zombie",
         geo="geometry.zombie.v1.8", hp=250, scale=3.2, dmg=12, cw=1.5, ch=6.0,
         extra={"minecraft:knockback_resistance": {"value": 0.7},
                "minecraft:burns_in_daylight": {}}),
    dict(id="dev_creeper", mat="creeper", tex="textures/entity/creeper/creeper",
         geo="geometry.creeper.v1.8", hp=180, scale=3.0, dmg=8, cw=1.5, ch=5.5,
         # creeper temasi: yaklasinca sisip DEV patlar
         explode=dict(power=6, fuse=1.5)),
    # OZGUN model: geometry.stnt_mutant_warden (bkz. bedrock/custom/KAYNAKLAR.md)
    # + kendi 11 animasyonu + alti katmanli doku (govde, biyolumine, iki leke
    # katmani, tendril, kalp). Render/animasyon KONTROLCULERI vanilla warden'in.
    # Model zaten iri (3,69 blok) -> scale 1.6 ile ~5,9 blok dev boss; 2.0 olsa
    # 7,4 bloka cikip kapali alanda tavana girerdi.
    dict(id="mutant_warden", mirror="mutant_warden", hp=500, scale=1.6, dmg=22, cw=1.7, ch=8.5,
         extra={"minecraft:knockback_resistance": {"value": 0.9}}),
]


# ---------------------------------------------------------------- PNG uretici
def png(path, rows):
    """rows: 16 satir, her biri 16 adet (r,g,b) uclusu."""
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in rows)
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    hdr = struct.pack('>IIBBBBB', 16, 16, 8, 2, 0, 0, 0)
    blob = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', hdr)
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(blob)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


# Paylasilan yumurta silueti — 4 spawn egg de "yumurta" gibi okunsun (satir -> x araligi)
SPAWN_EGG_MASK = {2: (7, 8), 3: (6, 9), 4: (5, 10), 5: (5, 11), 6: (4, 11), 7: (4, 11),
                  8: (4, 11), 9: (4, 11), 10: (4, 11), 11: (5, 11), 12: (5, 10),
                  13: (6, 9), 14: (7, 8)}


# Ikonu ELLE CIZILEN item'lar. Sira onemsiz; asagidaki symbol_texture bir
# id icin dal bulamazsa saydam bir kare uretir, o yuzden yeni id eklerken
# ciziminin de eklendigi RESIM DENETIMIYLE dogrulanir (check_pack: ikonlar).
SYMBOL_IDS = (
    "dev_zombi_yumurta", "dev_creeper_yumurta", "mutant_warden_yumurta",
    "blood_sword", "heart_axe", "dondurucu", "hiz_esyasi", "koku_bombasi",
    "end_pearl", "nether_pearl", "takim_asasi", "esya_calmaca",
    "rainbow_boots", "kurus", "iki_yuz_tl", "saglik_iksiri", "can_artirici",
    "ses_saldirisi", "mega_gubre", "ejderha_nefesi", "isim_degistirici",
    "blok_kiligi", "donusum_asasi", "tnt_frisbee", "among_us_report",
    "lightning_spell",
    "sahte_elmas_kilic", "sahte_elmas_kazma", "sahte_elmas_balta",
    "sahte_elmas_kurek", "sahte_elmas_capa",
)
# ARKAPLANI BOYALI kalan tek iki ikon. Ikisi de bir ISIK/DALGA etkisi cizer:
# saydam zeminde dalgalar havada asili duruyor gibi duruyordu, koyu zemin
# onlari bir arada tutuyor. Gerisinde arkaplan YOKTUR — envanterde renkli
# kareler degil esyanin kendisi gorunur.
SYMBOL_BG = {
    "ses_saldirisi": (12, 32, 40),
    "ejderha_nefesi": (28, 10, 42),
}


def symbol_texture(path, item_id):
    """Item icin anlamli 16x16 sembol cizer (RGBA, arkaplani saydam).
    Cocuklar item'lari ikonundan taniyabilsin diye; her biri ayirt edilebilir.
    Taninmayan id icin False doner (cagiran Java kopyasina / duz renge duser)."""
    if item_id not in SYMBOL_IDS:
        return False
    bg = SYMBOL_BG.get(item_id)
    g = [[list(bg) if bg else None for _ in range(16)] for _ in range(16)]

    def rect(x0, y0, x1, y1, c):
        for y in range(max(0, y0), min(16, y1 + 1)):
            for x in range(max(0, x0), min(16, x1 + 1)):
                g[y][x] = list(c)

    def hline(y, x0, x1, c): rect(x0, y, x1, y, c)

    def vline(x, y0, y1, c): rect(x, y0, x, y1, c)

    def disc(cx, cy, r, c):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    g[y][x] = list(c)

    def oval(cx, cy, rx, ry, c):
        """Piksel MERKEZINI (x+0.5) baz alan elips. disc() tam sayi merkezli
        oldugu icin 16x16'da simetrik olamiyor ve kenari tirtikli cikiyor;
        16'lik bir ikonda gercek merkez 8.0'dir. rx == ry -> duzgun daire."""
        for y in range(16):
            for x in range(16):
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                    g[y][x] = list(c)

    def px(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            g[y][x] = list(c)

    def egg(body):
        for yy, (x0, x1) in SPAWN_EGG_MASK.items():
            rect(x0, yy, x1, yy, body)

    iid = item_id
    if iid == "dev_zombi_yumurta":
        egg((110, 165, 75))
        for (sx, sy) in [(6, 5), (9, 9), (7, 12), (10, 6)]:
            px(sx, sy, (70, 120, 55))
        rect(5, 7, 6, 8, (20, 20, 20)); rect(9, 7, 10, 8, (20, 20, 20)); hline(11, 6, 9, (20, 20, 20))
    elif iid == "dev_creeper_yumurta":
        egg((95, 195, 95))
        rect(5, 6, 6, 8, (15, 15, 15)); rect(9, 6, 10, 8, (15, 15, 15))
        rect(7, 8, 8, 11, (15, 15, 15)); rect(6, 10, 9, 12, (15, 15, 15))
    elif iid == "mutant_warden_yumurta":
        egg((30, 60, 70))
        rect(5, 6, 6, 8, (130, 255, 240)); rect(9, 6, 10, 8, (130, 255, 240))
        disc(8, 11, 2, (40, 150, 140)); rect(7, 11, 8, 11, (255, 95, 95))
        px(6, 4, (28, 115, 125)); px(9, 13, (28, 115, 125))
    elif iid == "blood_sword":
        rect(7, 2, 8, 10, (205, 210, 220)); vline(7, 2, 10, (235, 240, 250))
        hline(11, 5, 10, (120, 120, 130)); rect(7, 12, 8, 14, (110, 70, 40))
        px(7, 11, (200, 20, 20)); disc(8, 13, 1, (200, 20, 20))
    elif iid == "heart_axe":
        # ahsap sap (sol alttan sag uste) + kalp bicimli agiz
        for (hx, hy) in ((4, 14), (5, 13), (5, 12), (6, 11), (6, 10), (7, 9), (7, 8)):
            px(hx, hy, (104, 68, 38)); px(hx + 1, hy, (140, 96, 56))
        for (hy, x0, x1) in ((3, 8, 9), (3, 12, 13), (4, 7, 14), (5, 7, 14),
                             (6, 7, 14), (7, 8, 13), (8, 9, 12), (9, 10, 11)):
            hline(hy, x0, x1, (226, 40, 72))
        # dis kenardaki gumus kesici agiz olmadan siluet "lolipop" gibi
        # okunuyordu; balta oldugu bundan anlasiliyor.
        for (hy, xx) in ((4, 14), (5, 14), (6, 14), (7, 13), (8, 12), (9, 11)):
            px(xx, hy, (228, 230, 238)); px(xx - 1, hy, (174, 178, 190))
        hline(4, 8, 9, (255, 150, 170))       # agzin parlamasi
    elif iid == "dondurucu":
        # Kar tanesi ARTIK BEYAZ DEGIL: saydam zeminde beyaz, acik renkli
        # envanter kutusunda kayboluyordu. Buz mavisi + beyaz cekirdek.
        ICE, LITE = (104, 186, 236), (232, 248, 255)
        vline(8, 2, 13, ICE); hline(8, 2, 13, ICE)
        for i in range(3, 13):
            px(i, i, ICE); px(i, 16 - i, ICE)
        disc(8, 8, 1, LITE)
        for (sx, sy) in ((8, 2), (8, 13), (2, 8), (13, 8)):
            px(sx, sy, LITE)
    elif iid == "donusum_asasi":
        # donusum asasi: ahsap sap + ucunda mor kristal (takim asasindan
        # ayirt edilsin diye onunki sari yildiz, bu mor kristal)
        for (wx, wy) in ((3, 14), (4, 13), (5, 12), (6, 11), (7, 10), (8, 9)):
            px(wx, wy, (92, 62, 40)); px(wx + 1, wy, (128, 90, 58))
        disc(11, 5, 3, (108, 48, 178)); disc(11, 5, 2, (156, 88, 224))
        disc(11, 5, 1, (214, 168, 255))
        for (sx, sy) in ((6, 2), (14, 2), (14, 9), (7, 7)):
            px(sx, sy, (240, 200, 255))       # pirilti
    elif iid == "tnt_frisbee":
        # Java ikonu TNT BLOGUNUN yan yuzuydu — envanterde frizbi degil blok
        # gorunuyordu. Kirmizi disk + TNT'nin beyaz bandi.
        # Egik bakis -> ELIPS. Daire cizersek top gibi okunuyor.
        oval(8, 9.5, 7.6, 4.4, (150, 26, 26))    # disk kenari
        oval(8, 8.5, 7.6, 4.4, (206, 46, 44))    # ust yuz
        oval(8, 8.5, 6.0, 3.2, (236, 78, 70))
        rect(1, 8, 14, 9, (240, 236, 224))       # TNT'nin beyaz bandi
        hline(8, 5, 10, (70, 58, 52))            # bandin uzerindeki yazi izi
        oval(8, 8.5, 2.2, 1.2, (206, 46, 44))    # ortadaki kabarti
        px(5, 6, (255, 152, 146)); px(6, 6, (255, 152, 146))
    elif iid == "among_us_report":
        # kirmizi rapor rozeti + beyaz unlem (Java ikonu tum kareyi kaplayan
        # duz kirmizi bir arkaplandi)
        oval(8, 8, 7.6, 7.6, (132, 18, 22)); oval(8, 8, 6.4, 6.4, (222, 44, 44))
        oval(8, 6.5, 4.4, 4.4, (238, 78, 78))
        rect(7, 3, 8, 9, (255, 255, 255)); rect(7, 11, 8, 12, (255, 255, 255))
    elif iid == "lightning_spell":
        BOLT = {2: (9, 11), 3: (8, 10), 4: (7, 10), 5: (6, 9), 6: (5, 11),
                7: (6, 10), 8: (7, 9), 9: (6, 8), 10: (5, 8), 11: (5, 7),
                12: (4, 6), 13: (4, 5)}
        for (yy, (x0, x1)) in BOLT.items():   # once koyu kontur
            for dy in (-1, 0, 1):
                rect(x0 - 1, yy + dy, x1 + 1, yy + dy, (138, 90, 12))
        for (yy, (x0, x1)) in BOLT.items():
            hline(yy, x0, x1, (252, 216, 64)); px(x0, yy, (255, 246, 158))
    elif iid == "hiz_esyasi":
        for base_x in (3, 7):
            for i in range(5):
                px(base_x + i, 4 + i, (255, 230, 60)); px(base_x + i, 12 - i, (255, 230, 60))
                px(base_x + i + 1, 4 + i, (255, 230, 60)); px(base_x + i + 1, 12 - i, (255, 230, 60))
    elif iid == "koku_bombasi":
        disc(8, 12, 3, (35, 45, 25)); px(9, 8, (230, 160, 40)); px(10, 7, (230, 160, 40)); px(11, 6, (230, 160, 40))
        disc(5, 6, 2, (175, 215, 95)); disc(9, 5, 2, (175, 215, 95)); disc(7, 3, 1, (175, 215, 95))
    elif iid == "end_pearl":
        disc(8, 8, 5, (30, 150, 140)); disc(8, 8, 2, (15, 60, 60)); px(5, 5, (140, 225, 205)); px(6, 5, (140, 225, 205))
    elif iid == "nether_pearl":
        disc(8, 8, 5, (120, 30, 20)); disc(8, 8, 3, (220, 90, 30)); disc(8, 8, 1, (250, 215, 90))
    elif iid == "takim_asasi":
        # sap CIFT piksel: tek sirali hali saydam zeminde neredeyse gorunmuyordu
        for (wx, wy) in ((3, 14), (4, 13), (5, 12), (6, 11), (7, 10), (8, 9), (9, 8)):
            px(wx, wy, (104, 76, 46)); px(wx + 1, wy, (146, 110, 68))
        for (yy, x0, x1) in ((2, 11, 11), (3, 10, 12), (4, 9, 13), (5, 10, 12), (6, 11, 11)):
            hline(yy, x0, x1, (255, 226, 92))     # sari yildiz
        px(11, 4, (255, 252, 220))
    elif iid == "esya_calmaca":
        vline(5, 5, 9, (220, 185, 145)); vline(8, 4, 9, (220, 185, 145)); vline(11, 5, 9, (220, 185, 145))
        rect(5, 9, 11, 12, (220, 185, 145)); rect(7, 2, 9, 4, (240, 205, 70))
    elif iid == "rainbow_boots":
        # Onceki hali sadece ust uste renkli cubuklardi; saydam zeminde
        # "cizme" oldugu anlasilmiyordu. Simdi konturlu bir cizme silueti:
        # dik konc + one uzayan ayak + koyu taban.
        BOOT = {3: (5, 9), 4: (5, 9), 5: (5, 9), 6: (5, 9), 7: (5, 9),
                8: (5, 9), 9: (5, 10), 10: (5, 11), 11: (5, 13), 12: (4, 14),
                13: (4, 14)}
        BANDS = [(220, 40, 40), (230, 140, 40), (235, 215, 60),
                 (70, 180, 80), (60, 110, 210), (150, 70, 190)]
        for (yy, (x0, x1)) in BOOT.items():
            for dy in (-1, 0, 1):
                rect(x0 - 1, yy + dy, x1 + 1, yy + dy, (48, 40, 56))
        for (yy, (x0, x1)) in BOOT.items():
            hline(yy, x0, x1, BANDS[(yy - 3) // 2 % len(BANDS)])
        rect(3, 14, 14, 15, (48, 40, 56))     # taban
    elif iid == "kurus":
        disc(8, 8, 6, (200, 150, 40)); disc(8, 8, 5, (230, 190, 70)); vline(8, 5, 11, (120, 85, 20)); px(7, 6, (120, 85, 20))
    elif iid == "saglik_iksiri":
        rect(6, 0, 9, 2, (150, 110, 70))            # mantar tipa
        rect(7, 3, 8, 5, (205, 230, 240))           # sise boynu
        disc(8, 10, 5, (205, 230, 240))             # cam govde
        disc(8, 10, 4, (215, 40, 65))               # kirmizi iksir
        hline(7, 6, 10, (250, 140, 160))            # sivinin yuzeyi
        for (hy, hx0, hx1) in ((8, 6, 7), (9, 5, 11), (10, 6, 10), (11, 7, 9), (12, 8, 8)):
            hline(hy, hx0, hx1, (255, 240, 245))    # ortadaki kalp
        hline(8, 9, 10, (255, 240, 245))            # kalbin ikinci tumsegi
        vline(4, 9, 11, (250, 250, 255))            # camin sol parlamasi
    elif iid == "mega_gubre":
        # cuval + tepesinden firlayan yesil filiz -> "gubre" ilk bakista okunur
        vline(8, 0, 4, (60, 150, 55))                 # filizin sapi
        hline(2, 5, 7, (95, 200, 80)); hline(1, 6, 7, (95, 200, 80))    # sol yaprak
        hline(2, 9, 11, (95, 200, 80)); hline(1, 9, 10, (95, 200, 80))  # sag yaprak
        rect(6, 4, 9, 5, (140, 108, 62))              # cuvalin agiz bagi
        rect(4, 6, 11, 6, (206, 190, 150))            # omuz
        rect(3, 7, 12, 14, (206, 190, 150))           # cuval govdesi
        hline(15, 4, 11, (150, 135, 100))             # taban golgesi
        for (yy, x0, x1) in ((9, 6, 9), (10, 5, 10), (11, 6, 9), (12, 7, 8)):
            hline(yy, x0, x1, (70, 165, 60))          # cuvalin uzerindeki yaprak
    elif iid == "can_artirici":
        # buyuk kirmizi kalp + ustunde altin arti: "daha fazla can"
        for (hy, hx0, hx1) in ((4, 3, 5), (4, 10, 12), (5, 2, 13), (6, 2, 13),
                               (7, 2, 13), (8, 3, 12), (9, 4, 11), (10, 5, 10),
                               (11, 6, 9), (12, 7, 8)):
            hline(hy, hx0, hx1, (220, 45, 60))
        hline(5, 4, 5, (255, 150, 165)); hline(6, 3, 4, (255, 150, 165))
        rect(7, 4, 8, 9, (250, 205, 60))     # artinin dikey kolu
        rect(5, 6, 10, 7, (250, 205, 60))    # artinin yatay kolu
    elif iid == "ses_saldirisi":
        # ic ice acilan uc ses dalgasi + kaynaktaki warden mavisi cekirdek
        disc(3, 8, 2, (120, 235, 245))
        for (r, c) in ((5, (60, 190, 210)), (8, (40, 150, 175)), (11, (28, 110, 135))):
            for y in range(16):
                for x in range(16):
                    d2 = (x - 3) ** 2 + (y - 8) ** 2
                    if r * r - 5 <= d2 <= r * r + 5 and x >= 3:
                        px(x, y, c)
    elif iid == "blok_kiligi":
        # yariya kadar bloga donusmus adam: ustu insan, alti tas kup
        rect(6, 2, 9, 5, (226, 190, 150))            # kafa
        px(7, 3, (40, 34, 30)); px(8, 3, (40, 34, 30))
        rect(5, 6, 10, 7, (90, 120, 190))            # omuz/govde
        rect(3, 8, 12, 13, (128, 128, 132))          # tas kup
        for (bx, by) in ((5, 9), (9, 10), (6, 12), (10, 12), (8, 9)):
            px(bx, by, (104, 104, 108))              # tas benegi
        hline(8, 3, 12, (150, 150, 154))             # kup ust kenar isigi
    elif iid == "isim_degistirici":
        # asili duran isim etiketi: ip, delik, kagit, uzerinde iki yazi satiri
        vline(8, 1, 3, (150, 130, 90))               # ip
        rect(3, 4, 13, 12, (226, 208, 160))          # kagit
        rect(3, 4, 13, 4, (176, 158, 116))           # ust kenar golgesi
        px(5, 6, (120, 104, 72)); px(6, 6, (120, 104, 72))   # delik
        hline(8, 5, 11, (96, 80, 52))                # yazi satiri 1
        hline(10, 5, 9, (96, 80, 52))                # yazi satiri 2
    elif iid == "ejderha_nefesi":
        # alttan yukari acilan mor nefes bulutu + yukselen uc duman tutami
        for (cx, cy, r, c) in ((5, 11, 3, (110, 45, 175)), (11, 11, 3, (110, 45, 175)),
                               (8, 10, 4, (140, 62, 210)), (8, 10, 2, (196, 132, 255))):
            disc(cx, cy, r, c)
        for (x, y0) in ((4, 3), (8, 2), (12, 4)):
            vline(x, y0, y0 + 2, (170, 95, 235))
            px(x - 1, y0 + 1, (150, 72, 220)); px(x + 1, y0 + 1, (150, 72, 220))
    elif iid == "iki_yuz_tl":
        rect(2, 5, 13, 11, (210, 225, 200))
        for x in range(2, 14):
            px(x, 5, (90, 160, 110)); px(x, 11, (90, 160, 110))
        vline(2, 5, 11, (90, 160, 110)); vline(13, 5, 11, (90, 160, 110))
        disc(5, 8, 1, (150, 180, 150)); hline(7, 9, 12, (90, 160, 110)); hline(9, 9, 12, (90, 160, 110))
    elif iid.startswith("sahte_elmas_"):
        # TUZAGIN TAMAMI IKONDA. Bu alet ancak envanterde gercek bir elmas
        # aleti gibi okunursa is goruyor: ayni elmas mavisi, ayni ahsap sap,
        # vanilla aletlerin sol-alttan sag-uste uzanan durusu. Ipucu YOK —
        # sahteligi ele veren tek sey adi ve tooltip'i, gorseli degil.
        DL, DM, DD = (150, 248, 240), (93, 222, 212), (52, 166, 162)   # elmas
        WL, WD = (140, 96, 56), (104, 68, 38)                          # ahsap sap

        # Ortak sap: (2,14) -> (10,6), 2 piksel genis capraz cubuk. Baslik her
        # zaman sapin UST UCUNA oturur; arada bosluk kalirsa alet "havada duran
        # iki parca" gibi okunuyor, bu da kiligi bozan ilk sey.
        SAP = ((2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8), (9, 7))
        for (hx, hy) in SAP:
            px(hx, hy, WD); px(hx + 1, hy, WL)

        tool = iid[len("sahte_elmas_"):]
        if tool == "kilic":
            # Kilic tek parca: capraz agiz + ona dik balcak + kisa kabza.
            # Sap yukaridaki dongude cizildi, kilicta ISE YARAMAZ; uzerine
            # kabza ve agiz binecek.
            for i in range(8):                            # 3 piksel kalin agiz
                px(4 + i, 11 - i, DL); px(5 + i, 11 - i, DM); px(6 + i, 11 - i, DM)
            px(12, 3, DL); px(13, 3, DM); px(13, 2, DL)   # sivri uc
            for (gx, gy) in ((2, 9), (3, 10), (4, 11), (5, 12), (6, 13)):
                px(gx, gy, DD); px(gx + 1, gy, DD)        # agza dik balcak
            for (hx, hy) in ((1, 14), (2, 13), (3, 12)):
                px(hx, hy, WD); px(hx + 1, hy, WL)        # kabza
            px(0, 15, DD); px(1, 15, DD)                  # topuz
        elif tool == "kazma":
            # Tepesi ortada, kollari iki yana asagi kivrilan genis yay.
            rect(9, 2, 10, 6, DM)                         # yaydan sapa inen boyun
            hline(1, 7, 12, DL); hline(2, 7, 12, DM)      # yayin tepesi
            for (ax, ay) in ((5, 2), (6, 2), (13, 2), (14, 2)):
                px(ax, ay, DM)
            px(4, 3, DM); px(15, 3, DM)
            px(3, 4, DD); px(15, 4, DD)                   # asagi bakan uclar
        elif tool == "balta":
            # Sapin sag yanina oturan tek yanli kama.
            for (ay, x0, x1) in ((1, 8, 10), (2, 8, 12), (3, 8, 13),
                                 (4, 8, 13), (5, 8, 12), (6, 8, 10)):
                hline(ay, x0, x1, DM)
            for (ex, ey) in ((10, 1), (12, 2), (13, 3), (13, 4), (12, 5), (10, 6)):
                px(ex, ey, DL)                            # disaridaki kesici agiz
            vline(8, 1, 6, DD)                            # sapa bakan kalin sirt
        elif tool == "kurek":
            # Ustu duz, altina dogru daralan kasik.
            hline(1, 7, 12, DL)
            for ay in (2, 3, 4):
                hline(ay, 7, 12, DM)
            hline(5, 8, 11, DM); hline(6, 9, 10, DD)      # boyun
            vline(7, 1, 4, DL)                            # sol kenar parlamasi
        elif tool == "capa":
            # "П": ust kol sola uzanir, iki bacak asagi iner — biri sapa
            # baglanan boyun, oteki toprağı kazan agiz.
            hline(3, 4, 10, DM); hline(4, 4, 10, DD)      # sola uzanan kol
            rect(9, 5, 10, 6, DM)                         # sapa inen boyun
            rect(4, 5, 5, 6, DM); hline(6, 4, 5, DL)      # asagi bakan agiz
            hline(3, 4, 10, DL)                           # kolun ust parlamasi
    if bg:                                  # yalnizca boyali iki ikonda cerceve
        for i in range(16):
            g[0][i] = shade(bg, 0.7); g[15][i] = shade(bg, 0.7)
            g[i][0] = shade(bg, 0.7); g[i][15] = shade(bg, 0.7)
    # Cizilmeyen piksel = tam saydam. Bedrock item ikonu RGBA bekler; opak RGB
    # yazilirsa envanterde esyanin arkasinda renkli bir kare kalir.
    png_rgba(path, [[tuple(c) + (255,) if c else (0, 0, 0, 0) for c in row]
                    for row in g], 16, 16)
    return True


def decor_texture(path, base, kind):
    """TNT-olmayan blok dokusu. lego=cikintili, glow=parlak, plate=cizgili."""
    studs = [(4, 4), (11, 4), (4, 11), (11, 11)]
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            c = shade(base, 1.0 + ((x * 7 + y * 13) % 5 - 2) * 0.02)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.78)
            if kind == "lego":
                for (sx, sy) in studs:
                    if (x - sx) ** 2 + (y - sy) ** 2 <= 3:
                        c = shade(base, 1.35)
            elif kind == "glow":
                if (x + y) % 4 == 0:
                    c = shade(base, 1.30)
            elif kind in ("decor", "kill"):
                if y in (5, 10):
                    c = shade(base, 0.72)
            elif kind == "anvil":
                # ust plaka acik metal, dar bel iki yani karanlik, taban orta ton
                if y <= 3:
                    c = shade(base, 1.45)
                elif 4 <= y <= 9 and (x < 5 or x > 10):
                    c = shade(base, 0.30)
                elif y >= 12:
                    c = shade(base, 1.10)
                if y == 1 and x in (7, 8):
                    c = (222, 120, 255)            # mor parilti: buyu orsu
            row.append(c)
        rows.append(row)
    png(path, rows)


# Yeniden renklendirme sablonu: gercek TNT dokusu. Duz renk kare uretmek
# yerine bunun parlakligini koruyup rengini degistiriyoruz; boylece "TNT"
# yazisi, fitil ve bant duruyor, sadece govde rengi degisiyor.
TEMPLATE = "diamond_tnt"


def tnt_face(path, base, band=True, face="side"):
    """TNT sablonunu hedef renge boyar. PIL yoksa duz renge duser."""
    src = os.path.join(JAVA_TEX, f"{TEMPLATE}_{face}.png")
    try:
        from PIL import Image
        import colorsys
        if not os.path.exists(src):
            raise FileNotFoundError(src)
        im = Image.open(src).convert("RGB")
        th, ts, _ = colorsys.rgb_to_hsv(*[c / 255 for c in base])
        out = Image.new("RGB", im.size)
        px_in, px_out = im.load(), out.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                r, g, b = px_in[x, y]
                _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                # ton hedeften, doygunluk hedefe olceklenir, parlaklik korunur
                # -> yazi/fitil/golge yapisi aynen kalir
                ns = min(1.0, s * (0.35 + ts))
                nr, ng, nb = colorsys.hsv_to_rgb(th, ns, v)
                px_out[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        out.save(path)
        return
    except Exception:
        pass
    # yedek: duz renk
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            c = shade(base, 1.0 + ((x * 7 + y * 13) % 5 - 2) * 0.02)
            if band and 6 <= y <= 9:
                c = shade(base, 1.30)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.78)
            row.append(c)
        rows.append(row)
    png(path, rows)


def compose_entity_texture(t, dst):
    """Blok yuzlerinden varlik dokusu (64x32) uretir.

    MC kutu-UV duzeni, 16x16x16 kup, uv [0,0]:
      top (16,0) | bottom (32,0) | dogu (0,16) | kuzey (16,16)
      bati (32,16) | guney (48,16)
    PIL varsa gercek yuz PNG'leri birlestirilir; yoksa duz renge duser.
    """
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    faces = {}
    for face in ("top", "side", "bottom"):
        p = os.path.join(RP, f"textures/blocks/stnt_{t['id']}_{face}.png")
        faces[face] = p if os.path.exists(p) else None
    try:
        from PIL import Image
        atlas = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        def put(face, xy):
            if faces[face]:
                atlas.paste(Image.open(faces[face]).convert("RGBA").resize((16, 16)), xy)
        put("top", (16, 0)); put("bottom", (32, 0))
        for xy in ((0, 16), (16, 16), (32, 16), (48, 16)):
            put("side", xy)
        atlas.save(dst)
        return "pil"
    except ImportError:
        # yedek: renk tanimindan duz doku (PIL yoksa)
        base = t.get('color', ((200, 60, 60),) * 3)
        rows = [[base[0] if y < 16 else base[1] for _ in range(64)] for y in range(32)]
        png_rgb(dst, rows, 64, 32)
        return "fallback"


def png_rgb(path, rows, wd, ht):
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in rows)
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    hdr = struct.pack('>IIBBBBB', wd, ht, 8, 2, 0, 0, 0)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', hdr)
                          + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def png_rgba(path, rows, wd, ht):
    """RGBA PNG (renk tipi 6). Alpha kanali entity_emissive_alpha materyalinde
    ISIMA seviyesi olur: alpha 255 = isimasiz (normal deri), dusuk alpha =
    parlak isima (karanlikta parlar). rows: her piksel (r,g,b,a)."""
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in rows)
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    hdr = struct.pack('>IIBBBBB', wd, ht, 8, 6, 0, 0, 0)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', hdr)
                          + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def mutant_warden_texture(path):
    """OZGUN mutant warden dokusu (128x128 RGBA). Koyu teal deri + KARANLIKTA
    PARLAYAN (emissive) biyolüminesan damarlar, gozler, agiz, kalp ve tendril
    uclari. Parlaklik ALPHA kanalinda kodlanir (dusuk alpha = guclu isima);
    entity_emissive_alpha materyali bunu isiga cevirir, deri (alpha 255) opak
    kalir. Bolgeler geometry.mutant_warden box-uv koseleriyle hizali: kafa
    uv[40,78] -> on yuz (50,88); kalp uv[0,112]; tendril uv[16/24,46]."""
    W = H = 128
    OPA = 255            # isimasiz (deri)
    G = 0               # en guclu isima (goz/kalp)
    Gv = 90             # yumusak isima (damar)
    Gm = 50             # orta isima (agiz)
    base = (16, 38, 44)
    vein = (44, 160, 168)
    grid = [[[base[0], base[1], base[2], OPA] for _ in range(W)] for _ in range(H)]
    for y in range(H):
        for x in range(W):
            n = ((x * 13 + y * 7) % 17) - 8          # deterministik hafif noise
            ao = -7 if (y % 32) >= 26 else 0         # kup altlarinda hafif golge (derinlik)
            px = [base[0] + n + ao, base[1] + n + ao, base[2] + n + ao, OPA]
            if (x * 7 + y * 3) % 37 < 2:             # ince ISILDAYAN damar cizgileri
                px = [vein[0], vein[1], vein[2], Gv]
            grid[y][x] = [max(0, min(255, v)) for v in px[:3]] + [px[3]]

    def rect(x0, y0, x1, y1, c, a=OPA):
        for yy in range(max(0, y0), min(y1, H)):
            for xx in range(max(0, x0), min(x1, W)):
                grid[yy][xx] = [c[0], c[1], c[2], a]

    rect(50, 88, 64, 101, (8, 20, 24))               # kafa on-yuz koyulasir (deri)
    rect(52, 91, 55, 95, (150, 255, 245), G)         # sol goz (ISILDAR cyan)
    rect(58, 91, 61, 95, (150, 255, 245), G)         # sag goz
    rect(52, 98, 62, 99, (120, 245, 230), Gm)        # agiz cizgisi (isildar)
    rect(0, 112, 16, 122, (140, 28, 36))             # kalp bolgesi deri (koyu kirmizi)
    rect(3, 114, 10, 120, (255, 90, 90), G)          # parlak kalp ortasi (ISILDAR)
    for u in (16, 24):                               # tendril uclari (isildar)
        rect(u, 46, u + 8, 59, (34, 140, 132))
        rect(u + 2, 46, u + 5, 50, (170, 255, 245), G)
    png_rgba(path, grid, W, H)


def ender_send_texture(path):
    """Ender Send dokusu (128x128 RGBA). Koyu siyah-mor enderman derisi + her
    kafanin on-yuzunde KARANLIKTA PARLAYAN mor gozler (isima alpha ile kodlu,
    entity_emissive_alpha). Bolgeler geometry.ender_send box-uv: orta kafa
    uv[40,78] -> on yuz (48,86); sol kafa uv[0,96] -> (6,102); sag (34,102)."""
    W = H = 128
    OPA = 255
    G = 0                                            # gozler en guclu isima
    base = (12, 10, 20)
    glow = (205, 75, 255)
    grid = [[[base[0], base[1], base[2], OPA] for _ in range(W)] for _ in range(H)]
    for y in range(H):
        for x in range(W):
            n = ((x * 11 + y * 5) % 13) - 6
            grid[y][x] = [max(0, min(255, base[0] + n)),
                          max(0, min(255, base[1] + n)),
                          max(0, min(255, base[2] + n + 4)), OPA]

    def rect(x0, y0, x1, y1, c, a=OPA):
        for yy in range(max(0, y0), min(y1, H)):
            for xx in range(max(0, x0), min(x1, W)):
                grid[yy][xx] = [c[0], c[1], c[2], a]

    rect(48, 86, 56, 95, (6, 4, 10))                 # orta kafa yuz (deri)
    rect(49, 89, 51, 91, glow, G)
    rect(53, 89, 55, 91, glow, G)
    rect(6, 102, 12, 109, (6, 4, 10))                # sol kafa yuz
    rect(7, 104, 8, 106, glow, G)
    rect(10, 104, 11, 106, glow, G)
    rect(34, 102, 40, 109, (6, 4, 10))               # sag kafa yuz
    rect(35, 104, 36, 106, glow, G)
    rect(38, 104, 39, 106, glow, G)
    png_rgba(path, grid, W, H)


# ---------------------------------------------------------------- yapi
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)


def check_uuids():
    """Her UUID gecerli RFC 4122 v4 mu ve hepsi benzersiz mi?

    Bir kez elle yazilan UUID'nin varyant nibble'i gecersizdi ve Minecraft
    paketi HICBIR hata mesaji vermeden reddetti - ne davranis ne kaynak
    paketi listede gorundu. Sessiz basarisizlik oldugu icin derlemede
    yakalanmali.
    """
    import uuid as _uuid
    all_ids = {
        "BP header": BP_UUID, "BP data": BP_MOD_UUID, "BP script": BP_SCRIPT_UUID,
        "RP header": RP_UUID, "RP module": RP_MOD_UUID,
    }
    for name, value in all_ids.items():
        u = _uuid.UUID(value)                      # bicim bozuksa burada patlar
        if u.version != 4 or u.variant != _uuid.RFC_4122:
            raise SystemExit(
                f"GECERSIZ UUID [{name}]: {value}\n"
                f"  version={u.version} (4 olmali), variant={u.variant}\n"
                f"  Yenisini uret: python3 -c \"import uuid;print(uuid.uuid4())\"")
    if len(set(all_ids.values())) != len(all_ids):
        raise SystemExit("UUID'ler benzersiz olmali: " + str(all_ids))


def build():
    check_uuids()
    for d in (BP, RP, OUT):
        if os.path.exists(d):
            shutil.rmtree(d)

    # ---------- manifestler
    w(os.path.join(BP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": f"Super TNT Mod v{VER_STR} [BP]",
                   # Hicbir deneysel bayrak GEREKMEZ: v1.41.0'da oyuncuyu
                   # kilitleyen Script Kamera kaldirildi (bkz. SIZE_TABLE ustu).
                   "description": (f"Super TNT Mod v{VER_STR}. "
                                   "Deneysel ayar gerekmez, oldugu gibi calisir."),
                   "uuid": BP_UUID, "version": VERSION,
                   "min_engine_version": MIN_ENGINE},
        # script modulunde "language": "javascript" ZORUNLU. Onsuz Minecraft
        # modulu tanimyor ve TUM davranis paketini sessizce reddediyor — paket
        # listede hic gorunmuyor. MorphX (calisan referans) ile karsilastirinca
        # bulundu.
        "capabilities": ["script_eval"],
        "metadata": {"authors": ["Muhammed"]},
        "modules": [
            {"type": "data", "uuid": BP_MOD_UUID, "version": VERSION},
            {"type": "script", "language": "javascript",
             "uuid": BP_SCRIPT_UUID, "version": VERSION,
             "entry": "scripts/main.js"}],
        "dependencies": [
            {"uuid": RP_UUID, "version": VERSION},
            # 1.14.0: MorphX bu surumu kullaniyor ve ayni cihazda yuklendigi
            # dogrulandi. Tahmin yerine bilinen-calisan surume yaslaniyoruz.
            {"module_name": "@minecraft/server", "version": "1.14.0"},
            # server-ui 1.3.0: @minecraft/server 1.x hattinin es surumu.
            # Sifreli sandigin ModalFormData'si icin. (Sadece o kullanir.)
            {"module_name": "@minecraft/server-ui", "version": "1.3.0"}],
    })
    w(os.path.join(RP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": f"Super TNT Mod v{VER_STR} [RP]",
                   "description": f"Super TNT Mod - dokular (v{VER_STR})",
                   "uuid": RP_UUID, "version": VERSION,
                   "min_engine_version": MIN_ENGINE},
        "modules": [{"type": "resources", "uuid": RP_MOD_UUID, "version": VERSION}],
    })

    # ---------- dokular
    terrain = {}
    copied = generated = 0
    for t in TNTS:
        for face in ("top", "side", "bottom"):
            key = f"stnt_{t['id']}_{face}"
            rel = f"textures/blocks/{key}"
            dst = os.path.join(RP, rel + ".png")
            src = os.path.join(JAVA_TEX, f"{t.get('tex','')}_{face}.png")
            if t.get('tex') and os.path.exists(src):
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copy(src, dst)
                copied += 1
            else:
                idx = {"top": 0, "side": 1, "bottom": 2}[face]
                tnt_face(dst, t['color'][idx], band=(face == "side"), face=face)
                generated += 1
            terrain[key] = {"textures": rel}
    # DECOR blok dokulari (tek yuz, tum yonler ayni). Java'da gercek cizilmis
    # dokusu olan bloklar icin onu kullan (item klasorunde duruyorlar).
    BLOCK_TEX_MAP = {"herobrine_spawner": "herobrine_spawner", "fake_tnt": "fake_tnt",
                     "yakinlik_mayini": "proximity_mine", "sifreli_sandik": "encrypted_tnt_chest",
                     "blocker_sandik": "encrypted_tnt_chest", "sahip_kapi": "tnt_door"}
    for blk in BLOCKS:
        key = f"stnt_{blk['id']}"
        rel = f"textures/blocks/{key}"
        dst = os.path.join(RP, rel + ".png")
        jt = BLOCK_TEX_MAP.get(blk['id'])
        jsrc = os.path.join(JAVA_ITEM_TEX, f"{jt}.png") if jt else None
        if blk.get('tex'):                              # Java'nin blok dokusu
            jsrc = os.path.join(JAVA_TEX, f"{blk['tex']}.png")
        if blk.get('vtex'):                             # vanilla RP'nin dokusu (kilik)
            terrain[key] = {"textures": blk['vtex']}
            continue
        if jsrc and os.path.exists(jsrc):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(jsrc, dst)
        else:
            decor_texture(dst, blk['color'], blk['kind'])
        terrain[key] = {"textures": rel}
        generated += 1
    w(os.path.join(RP, "textures/terrain_texture.json"),
      {"resource_pack_name": "super_tnt", "texture_name": "atlas.terrain",
       "padding": 8, "num_mip_levels": 4, "texture_data": terrain})

    # ---------- item ikonlari
    item_tex = {}
    item_copied = item_gen = 0
    for it in ITEMS:
        key = f"stnt_{it['id']}"
        rel = f"textures/items/{key}"
        dst = os.path.join(RP, rel + ".png")
        # Oncelik: ELLE CIZILEN sembol > Java kopyasi > duz renk.
        # Sembol once gelir cunku Java'dan gelen bazi ikonlar boyali arkaplanla
        # geliyordu (among_us_report duz kirmizi kare, tnt_frisbee TNT blogunun
        # yan yuzu, lightning_spell mor zemin) — bunlarin yerine burada cizilen
        # saydam sembol kullanilir. SYMBOL_IDS disindaki item'lar etkilenmez.
        jtex = ITEM_TEX_MAP.get(it['id'], it['id'])
        src = os.path.join(JAVA_ITEM_TEX, f"{jtex}.png")
        if symbol_texture(dst, it['id']):       # anlamli sembol (spawn egg, silah, vb.)
            item_gen += 1
        elif os.path.exists(src):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(src, dst)
            item_copied += 1
        else:
            decor_texture(dst, it['color'], "plain")
            item_gen += 1
        item_tex[key] = {"textures": rel}
    w(os.path.join(RP, "textures/item_texture.json"),
      {"resource_pack_name": "super_tnt", "texture_data": item_tex})

    # ---------- pack_icon (calisan paketlerin hepsinde var; eksikligi
    #            paketi bozuk gosteriyor)
    for pack, base in ((BP, (196, 48, 54)), (RP, (60, 90, 190))):
        tnt_face(os.path.join(pack, "pack_icon.png"), base, band=True, face="side")

    # ---------- bloklar
    for t in TNTS:
        w(os.path.join(BP, f"blocks/{t['id']}.json"), {
            "format_version": "1.20.20",
            "minecraft:block": {
                # menu_category'de "group" verilmiyor: gecerliligi dogrulanmamis
                # bir grup adi blogun yaratici menude hic gorunmemesine yol acar.
                # Tum TNT'ler yaratici envanterde tek "Super TNT" grubunda
                # toplanir. Craftopia (1.20.20) ile ayni yontem — dogrulandi.
                "description": {"identifier": f"stnt:{t['id']}",
                                "is_experimental": False,
                                "menu_category": {"category": "construction",
                                                  "group": "itemGroup.name.super_tnt"}},
                "components": {
                    "minecraft:material_instances": {
                        "up": {"texture": f"stnt_{t['id']}_top", "render_method": "opaque"},
                        "down": {"texture": f"stnt_{t['id']}_bottom", "render_method": "opaque"},
                        "*": {"texture": f"stnt_{t['id']}_side", "render_method": "opaque"}},
                    "minecraft:destructible_by_mining": {"seconds_to_destroy": 0.0},
                    "minecraft:destructible_by_explosion": {"explosion_resistance": 0},
                    "minecraft:light_dampening": 15,
                    "minecraft:geometry": "minecraft:geometry.full_block",
                },
            },
        })

    # ---------- TNT-olmayan bloklar (lego, hayalet, ayna, plaka)
    for blk in BLOCKS:
        comps = {
            "minecraft:material_instances": {
                "*": {"texture": f"stnt_{blk['id']}", "render_method": "opaque"}},
            "minecraft:destructible_by_mining": {"seconds_to_destroy": 0.4},
            "minecraft:geometry": "minecraft:geometry.full_block",
        }
        if blk['kind'] == "ghost":
            # icinden gecilir: carpisma kutusu yok
            comps["minecraft:collision_box"] = False
        elif blk['kind'] == "glow":
            comps["minecraft:light_emission"] = 15
        elif blk['kind'] == "portal_block":
            # gorunur ama icinden gecilir + parlak (portal hissi)
            comps["minecraft:collision_box"] = False
            comps["minecraft:light_emission"] = 12
        elif blk['kind'] == "fire":
            # Ates: carpismasiz, capraz iki duzlem (kendi geometrimiz; yerlesik
            # "minecraft:geometry.cross" bu surumde dogrulanmadi), isik sacar,
            # isigi kesmez, kirilinca hicbir sey dusurmez. Sonmesi icin bir
            # mekanizma YOK — "sonmez" sozu boyle tutuluyor.
            comps["minecraft:collision_box"] = False
            comps["minecraft:selection_box"] = {"origin": [-8, 0, -8], "size": [16, 6, 16]}
            comps["minecraft:light_emission"] = 15
            comps["minecraft:light_dampening"] = 0
            comps["minecraft:geometry"] = "geometry.stnt_cross"
            comps["minecraft:material_instances"] = {
                "*": {"texture": f"stnt_{blk['id']}", "render_method": "alpha_test",
                      "face_dimming": False, "ambient_occlusion": False}}
            comps["minecraft:destructible_by_mining"] = {"seconds_to_destroy": 0.0}
            comps["minecraft:loot"] = "loot_tables/empty.json"
        elif blk['kind'] == "anvil":
            comps["minecraft:destructible_by_mining"] = {"seconds_to_destroy": 1.2}
            comps["minecraft:destructible_by_explosion"] = {"explosion_resistance": 1200}
        elif blk['kind'] == "explosive_sand":
            # Elle kazma suresi vanilla kumla ayni (0.75 sn). Kirilinca hicbir
            # sey dusurmez: patlayan blok geri kazanilmaz, tuzak kuran onu
            # yeniden yapar. Patlamanin kendisi betikte (playerBreakBlock).
            comps["minecraft:destructible_by_mining"] = {"seconds_to_destroy": 0.75}
            comps["minecraft:loot"] = "loot_tables/empty.json"
        elif blk['kind'] == "mini":
            # tam bloktan kucuk: hucre tabaninda ortali 8x8x8 kup.
            comps["minecraft:geometry"] = "geometry.stnt_mini"
            comps["minecraft:collision_box"] = {"origin": [-4, 0, -4], "size": [8, 8, 8]}
            comps["minecraft:selection_box"] = {"origin": [-4, 0, -4], "size": [8, 8, 8]}
            # kucuk geometry: komsu culling'i kapatmak icin material'i
            # alpha_test yap (opaque kalirsa MC blogu tam-kup sanip komsuyu
            # gorunmez kilabilir).
            comps["minecraft:material_instances"] = {
                "*": {"texture": f"stnt_{blk['id']}", "render_method": "alpha_test"}}
        # hidden: yaratici menude gorunmez (cakmakla yakilan atesler).
        menu = ({"category": "none"} if blk.get('hidden')
                else {"category": "construction", "group": "itemGroup.name.super_tnt"})
        w(os.path.join(BP, f"blocks/{blk['id']}.json"), {
            "format_version": "1.20.20",
            "minecraft:block": {
                "description": {"identifier": f"stnt:{blk['id']}",
                                "is_experimental": False,
                                "menu_category": menu},
                "components": comps,
            },
        })

    # ---------- item'lar (yiyecek, buyu, arac)
    for it in ITEMS:
        icomps = {"minecraft:icon": f"stnt_{it['id']}",
                  "minecraft:max_stack_size": 64 if it['kind'] in ("food", "loot") else 1}
        if it['kind'] in ("food", "drink"):
            # "drink": icme animasyonu + sifir doyum (iksir yiyecek degil).
            # can_always_eat ikisinde de sart: yoksa acligi tok olan oyuncu
            # esyayi hic kullanamaz, itemCompleteUse olayi da hic tetiklenmez.
            drink = it['kind'] == "drink"
            icomps["minecraft:food"] = {"nutrition": 0 if drink else 4,
                                        "can_always_eat": True}
            icomps["minecraft:use_animation"] = "drink" if drink else "eat"
            icomps["minecraft:use_modifiers"] = {"use_duration": 1.4, "movement_modifier": 0.35}
            if drink:
                icomps["minecraft:max_stack_size"] = 16
        elif it['kind'] == "weapon":
            icomps["minecraft:damage"] = it.get('damage', 6)
            # unbreakable: dayaniklilik bileseni HIC konmaz. Bedrock'ta
            # bileseni olmayan esya hasar almaz — "sonsuz dayaniklilik" diye bir
            # deger yok, dogru yol bileseni atlamak.
            if not it.get("unbreakable"):
                icomps["minecraft:durability"] = {"max_durability": 800}
            icomps["minecraft:hand_equipped"] = True
        elif it['kind'] == "fake_tool":
            # Kilik elmas takimi. Dayaniklilik da elmasin (1561): esyayi
            # inceleyen cocuk dolu bir cubuk gorsun. Alet ilk kullanista
            # tahtaya donecegi icin bu deger pratikte hic tukenmez.
            icomps["minecraft:damage"] = it['damage']
            icomps["minecraft:hand_equipped"] = True
            icomps["minecraft:durability"] = {"max_durability": 1561}
            etiket = [f"'minecraft:is_{it['tur']}_item_destructible'"]
            if it.get('dig'):
                etiket.append(it['dig'])
            icomps["minecraft:digger"] = {
                "use_efficiency": True,
                "destroy_speeds": [{"block": {"tags": f"query.any_tag({', '.join(etiket)})"},
                                    "speed": FAKE_DIG_SPEED}],
            }
        elif it['kind'] == "boots":
            icomps["minecraft:wearable"] = {"slot": "slot.armor.feet", "protection": 2}
            icomps["minecraft:durability"] = {"max_durability": 400}
        elif it['kind'] == "tnt_armor":
            icomps["minecraft:wearable"] = {"slot": it['slot'], "protection": 3}
            icomps["minecraft:durability"] = {"max_durability": 500}
        elif it['kind'] == "spawn_egg":
            # sag tikla varligi cagir; Super TNT grubunda gorunur
            icomps["minecraft:entity_placer"] = {"entity": it['spawn']}
            icomps["minecraft:max_stack_size"] = 16
        if it.get('durability'):
            icomps["minecraft:durability"] = {"max_durability": it['durability']}
        if it.get('enchant_slot'):
            # enchantable olmadan ors hicbir buyuyu kabul etmez (Tamir dahil).
            icomps["minecraft:enchantable"] = {"slot": it['enchant_slot'], "value": 10}
        w(os.path.join(BP, f"items/{it['id']}.json"), {
            # 1.20.30: minecraft:wearable en az bu surumu ister (TNT zirhi +
            # gokkusagi botlari). 1.20.20'de wearable sessizce devre disi kalir.
            "format_version": "1.20.30",
            "minecraft:item": {
                "description": {"identifier": f"stnt:{it['id']}",
                                "menu_category": {"category": "equipment",
                                                  "group": "itemGroup.name.super_tnt"}},
                "components": icomps,
            },
        })

    # ---------- ates geometrisi: 45 derece capraz iki duzlem (vanilla ates gibi).
    # Sifir kalinlikta kup = duzlem; alpha_test iki yuzu de cizer.
    if any(b['kind'] == "fire" for b in BLOCKS):
        plane = lambda angle: {
            "origin": [-8, 0, 0], "size": [16, 16, 0], "pivot": [0, 8, 0], "rotation": [0, angle, 0],
            "uv": {"north": {"uv": [0, 0], "uv_size": [16, 16]},
                   "south": {"uv": [16, 0], "uv_size": [-16, 16]}}}
        w(os.path.join(RP, "models/blocks/stnt_cross.geo.json"), {
            "format_version": "1.16.0",
            "minecraft:geometry": [{
                "description": {"identifier": "geometry.stnt_cross",
                                "texture_width": 16, "texture_height": 16,
                                "visible_bounds_width": 1, "visible_bounds_height": 1,
                                "visible_bounds_offset": [0, 0.5, 0]},
                "bones": [{"name": "cross", "pivot": [0, 0, 0],
                           "cubes": [plane(45), plane(-45)]}],
            }],
        })

    # ---------- mini blok geometrisi (hucre tabaninda ortali 8x8x8 kup)
    if any(b['kind'] == "mini" for b in BLOCKS):
        w(os.path.join(RP, "models/blocks/stnt_mini.geo.json"), {
            "format_version": "1.16.0",
            "minecraft:geometry": [{
                "description": {"identifier": "geometry.stnt_mini",
                                "texture_width": 16, "texture_height": 16},
                "bones": [{"name": "mini", "pivot": [0, 0, 0],
                           "cubes": [{"origin": [-4, 0, -4], "size": [8, 8, 8], "uv": [0, 0]}]}],
            }],
        })

    # ---------- ates alan TNT varligi (F1)
    # 16x16x16 tek kup. MC kutu-UV duzeni: 64x32 doku.
    w(os.path.join(RP, "models/entity/stnt_tnt.geo.json"), {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.stnt_tnt",
                            "texture_width": 64, "texture_height": 32,
                            "visible_bounds_width": 2, "visible_bounds_height": 2,
                            "visible_bounds_offset": [0, 1, 0]},
            "bones": [{"name": "body", "pivot": [0, 0, 0],
                       "cubes": [{"origin": [-8, 0, -8], "size": [16, 16, 16], "uv": [0, 0]}]}],
        }],
    })
    # yanip sonen beyaz kaplama - vanilla TNT hissi
    w(os.path.join(RP, "render_controllers/stnt_tnt.render_controllers.json"), {
        "format_version": "1.10.0",
        "render_controllers": {
            "controller.render.stnt_tnt": {
                "geometry": "Geometry.default",
                "materials": [{"*": "Material.default"}],
                "textures": ["Texture.default"],
                "overlay_color": {
                    "r": 1.0, "g": 1.0, "b": 1.0,
                    "a": "math.mod(math.floor(query.life_time * 10), 2) * 0.55",
                },
            }
        },
    })

    for t in TNTS:
        ent = f"stnt:{t['id']}_primed"
        w(os.path.join(BP, f"entities/{t['id']}_primed.json"), {
            "format_version": "1.21.0",
            "minecraft:entity": {
                "description": {"identifier": ent, "is_spawnable": False,
                                "is_summonable": True, "is_experimental": False},
                "components": {
                    "minecraft:collision_box": {"width": 0.98, "height": 0.98},
                    "minecraft:physics": {},
                    "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": True},
                    "minecraft:fire_immune": True,
                    "minecraft:damage_sensor": {"triggers": [{"deals_damage": False}]},
                    "minecraft:conditional_bandwidth_optimization": {},
                },
            },
        })
        w(os.path.join(RP, f"entity/{t['id']}_primed.json"), {
            "format_version": "1.10.0",
            "minecraft:client_entity": {
                "description": {
                    "identifier": ent,
                    "materials": {"default": "entity_alphatest"},
                    "textures": {"default": f"textures/entity/stnt/{t['id']}"},
                    "geometry": {"default": "geometry.stnt_tnt"},
                    "render_controllers": ["controller.render.stnt_tnt"],
                },
            },
        })
        compose_entity_texture(t, os.path.join(RP, f"textures/entity/stnt/{t['id']}.png"))

    # Blok kiligi modeli: 16x16x16 kup, her yuzu ayni 16x16 dokuyu ornekler.
    # Vanilla blok dokulari ADIYLA referans alinir (mob morph'larindaki desen).
    os.makedirs(os.path.join(RP, "models/entity"), exist_ok=True)
    shutil.copy(os.path.join(HERE, "custom/block_morph.geo.json"),
                os.path.join(RP, "models/entity/block_morph.geo.json"))

    # ---------- canavarlar (dev boss'lar, spawn egg ile cagrilir)
    # scale ~3 dev boyut; vanilla mob gorseli olceklenir. 20 blok yapmiyoruz
    # (chunk/tavan sorunu). is_spawnable:false -> dokusuz otomatik egg bastirilir.
    for m in MONSTERS:
        comps = {
            "minecraft:type_family": {"family": ["monster", "mob", m['id']]},
            "minecraft:health": {"value": m['hp'], "max": m['hp']},
            "minecraft:scale": {"value": m['scale']},
            # DIKKAT: minecraft:scale carpisma kutusunu DA carpar (model + hitbox;
            # wiki.bedrock.dev dogrulandi). cw/ch DOGRUDAN yazilirsa efektif hitbox
            # cw*scale x ch*scale olur -> ender 4.8 x 21.6 blok gibi devasa; mob
            # zemine SIKISIR, gecerli yol bulamaz, oylece durur (yurumez). Bu #3
            # "koyunca duruyor" hatasinin sebebi. Efektif hitbox'i YURUYEBILIR bir
            # boyuta (en fazla 2.0 x 4.0 blok) kapiyoruz; scale'e bolunce blok
            # icinde bu deger elde edilir. Model gorsel olarak dev kalir (scale
            # aynen), sadece carpisma kutusu makul -> vanilla yuruyen moblar ~3
            # blok; 4 blok acik alanda rahat yol bulur.
            "minecraft:collision_box": {"width": round(min(m['cw'], 2.0) / m['scale'], 3),
                                        "height": round(min(m['ch'], 4.0) / m['scale'], 3)},
            "minecraft:attack": {"damage": m['dmg']},
            "minecraft:movement": {"value": 0.4},
            "minecraft:navigation.walk": {"can_path_over_water": True, "avoid_water": True},
            "minecraft:movement.basic": {},
            "minecraft:jump.static": {},
            "minecraft:physics": {},
            "minecraft:persistent": {},
            "minecraft:nameable": {},
            "minecraft:behavior.nearest_attackable_target": {"priority": 3, "must_see": True,
                "entity_types": [{"filters": {"test": "is_family", "subject": "other", "value": "player"},
                                  "max_dist": 32}]},
            "minecraft:behavior.random_stroll": {"priority": 6, "speed_multiplier": 1.0},
            "minecraft:behavior.look_at_player": {"priority": 7, "look_distance": 24},
            "minecraft:behavior.random_look_around": {"priority": 8},
        }
        comps.update(m.get('extra', {}))
        # hedefe yuru (chase). Creeper'da bile gerekli: yoksa hedef secer ama
        # yaklasmaz, sensor/swell hic tetiklenmez (denetimde bulundu).
        comps["minecraft:behavior.melee_attack"] = {"priority": 2, "track_target": True}
        groups, events = {}, {}
        if m.get('explode'):
            # vanilla creeper mekanigi: yaklasinca sisip patlar (component_group
            # ile fuse_lit toggle). start/stop olaylari fitili yakar/sondurur.
            ex = m['explode']
            comps["minecraft:explode"] = {"fuse_length": ex['fuse'], "fuse_lit": False,
                                          "power": ex['power'], "causes_fire": False,
                                          "destroy_affected_by_griefing": True}
            comps["minecraft:target_nearby_sensor"] = {"inside_range": 3, "outside_range": 6,
                "must_see": True,
                "on_inside_range": {"event": "stnt:fuse", "target": "self"},
                "on_outside_range": {"event": "stnt:unfuse", "target": "self"}}
            comps["minecraft:behavior.swell"] = {"priority": 2, "start_distance": 3, "stop_distance": 6}
            groups["stnt:fused"] = {"minecraft:explode": {"fuse_length": ex['fuse'], "fuse_lit": True,
                                    "power": ex['power'], "causes_fire": False,
                                    "destroy_affected_by_griefing": True}}
            events = {"stnt:fuse": {"add": {"component_groups": ["stnt:fused"]}},
                      "stnt:unfuse": {"remove": {"component_groups": ["stnt:fused"]}}}
        ent = {"description": {"identifier": f"stnt:{m['id']}", "is_spawnable": False,
                               "is_summonable": True, "is_experimental": False},
               "components": comps}
        if groups: ent["component_groups"] = groups
        if events: ent["events"] = events
        w(os.path.join(BP, f"entities/{m['id']}.json"),
          {"format_version": "1.21.0", "minecraft:entity": ent})
        if m.get('mirror'):
            # GORUNUM custom/{id}.mirror.json'dan gelir. Desen: vanilla adlarini
            # (controller.render.*, controller.animation.*, materyaller) OLDUGU
            # GIBI referans al — Minecraft onlari kendi kaynagindan verir — ve
            # yalniz paketin KENDI parcalarini ship et. Yurumenin sirri
            # pre_animation: bacak/kol donuslerini query.modified_distance_moved'
            # dan hesaplar, move animasyonu kemige baglar (vanilla Warden'in
            # yaptigi). Zamanlayiciyla surulen animasyon "hayalet gibi kayma"
            # veriyordu.
            #
            # Yanindaki {id}.geo.json / {id}.animation.json / {id}_tex/ varsa
            # onlar da kopyalanir. Kopyalanmazsa mirror.json kimlikleri referans
            # eder ama dosyalar pakette olmaz -> model SESSIZCE gorunmez olur.
            os.makedirs(os.path.join(RP, "entity"), exist_ok=True)
            shutil.copy(os.path.join(HERE, f"custom/{m['id']}.mirror.json"),
                        os.path.join(RP, f"entity/{m['id']}.json"))
            geo_src = os.path.join(HERE, f"custom/{m['id']}.geo.json")
            if os.path.exists(geo_src):
                os.makedirs(os.path.join(RP, "models/entity"), exist_ok=True)
                shutil.copy(geo_src, os.path.join(RP, f"models/entity/{m['id']}.geo.json"))
            anim_src = os.path.join(HERE, f"custom/{m['id']}.animation.json")
            if os.path.exists(anim_src):
                os.makedirs(os.path.join(RP, "animations"), exist_ok=True)
                shutil.copy(anim_src, os.path.join(RP, f"animations/{m['id']}.animation.json"))
            tex_src = os.path.join(HERE, f"custom/{m['id']}_tex")
            if os.path.isdir(tex_src):
                tex_dst = os.path.join(RP, f"textures/entity/stnt_{m['id']}")
                os.makedirs(tex_dst, exist_ok=True)
                for tf in sorted(os.listdir(tex_src)):
                    shutil.copy(os.path.join(tex_src, tf), os.path.join(tex_dst, tf))
        elif m.get('custom'):
            # OZGUN model + OZGUN doku, ama animasyon VANILLA MOB TEKNIGIYLE:
            # scripts.pre_animation query.modified_distance_moved'dan bacak/kol
            # degiskenlerini hesaplar, scripts.animate ["move"] animasyonu DOGRUDAN
            # oynatir (animasyon kontrolcusu YOK) ve move animasyonu bu degiskenleri
            # kemige baglar. Vanilla Warden'in aynen yaptigi, KANITLANMIS yontem.
            # (Eski kontrolcu + query.ground_speed yaklasimi "hayalet gibi kayma"
            # veriyordu: bacaklar harekete senkron degildi.) geo + animation.json
            # custom/ altindan kopyalanir, doku {id}_texture() ile uretilir.
            os.makedirs(os.path.join(RP, "models/entity"), exist_ok=True)
            os.makedirs(os.path.join(RP, "animations"), exist_ok=True)
            shutil.copy(os.path.join(HERE, f"custom/{m['id']}.geo.json"),
                        os.path.join(RP, f"models/entity/{m['id']}.geo.json"))
            shutil.copy(os.path.join(HERE, f"custom/{m['id']}.animation.json"),
                        os.path.join(RP, f"animations/{m['id']}.animation.json"))
            globals()[f"{m['id']}_texture"](os.path.join(RP, f"textures/entity/{m['id']}.png"))
            scripts = {"animate": ["move"]}
            if m.get('pre_anim'):
                scripts = {"pre_animation": m['pre_anim'], "animate": ["move"]}
            w(os.path.join(RP, f"entity/{m['id']}.json"), {
                "format_version": "1.10.0",
                "minecraft:client_entity": {
                    "description": {
                        "identifier": f"stnt:{m['id']}",
                        # entity_emissive_alpha: dokunun alpha kanali ISIMA seviyesi
                        # olur (dusuk alpha = parlar). goz/kalp/damar dusuk alpha ->
                        # karanlikta parlar (custom mob gorsel zenginligi).
                        "materials": {"default": "entity_emissive_alpha"},
                        "textures": {"default": f"textures/entity/{m['id']}"},
                        "geometry": {"default": f"geometry.{m['id']}"},
                        "animations": {"move": f"animation.{m['id']}.move"},
                        "scripts": scripts,
                        "render_controllers": ["controller.render.default"],
                    },
                },
            })
        else:
            # RP: vanilla mob gorseli (materials/texture/geometry MC saglar)
            w(os.path.join(RP, f"entity/{m['id']}.json"), {
                "format_version": "1.10.0",
                "minecraft:client_entity": {
                    "description": {
                        "identifier": f"stnt:{m['id']}",
                        "materials": {"default": m['mat']},
                        "textures": {"default": m['tex']},
                        "geometry": {"default": m['geo']},
                        "render_controllers": ["controller.render.default"],
                    },
                },
            })

    # ---------- tarifler (8 malzeme + ortada TNT)
    for t in TNTS:
        w(os.path.join(BP, f"recipes/{t['id']}.json"), {
            "format_version": "1.20.10",
            "minecraft:recipe_shaped": {
                "description": {"identifier": f"stnt:{t['id']}_recipe"},
                "tags": ["crafting_table"],
                "pattern": ["MMM", "MTM", "MMM"],
                "key": {"M": {"item": t['mat']}, "T": {"item": "minecraft:tnt"}},
                "result": {"item": f"stnt:{t['id']}", "count": 1},
            },
        })

    # ---------- esya / blok tarifleri (recipe alani olanlar)
    for row in ITEMS + BLOCKS:
        r = row.get('recipe')
        if not r:
            continue
        w(os.path.join(BP, f"recipes/{row['id']}.json"), {
            "format_version": "1.20.10",
            "minecraft:recipe_shaped": {
                "description": {"identifier": f"stnt:{row['id']}_recipe"},
                "tags": ["crafting_table"],
                "pattern": r['pattern'],
                "key": {k: {"item": v} for k, v in r['key'].items()},
                "result": {"item": f"stnt:{row['id']}", "count": 1},
            },
        })

    # ---------- dil
    for pack in (BP, RP):
        os.makedirs(os.path.join(pack, "texts"), exist_ok=True)
        w(os.path.join(pack, "texts/languages.json"), ["en_US", "tr_TR"])
        for lang, nk, tk in (("en_US", "en", "entip"), ("tr_TR", "tr", "trtip")):
            grp = "§cSuper TNT" if lang == "tr_TR" else "§cSuper TNT"
            lines = [f"pack.name=Super TNT Mod",
                     f"pack.description=Super TNT Mod",
                     f"itemGroup.name.super_tnt={grp}"]
            for t in TNTS:
                lines.append(f"tile.stnt:{t['id']}.name={t[nk]}")
                lines.append(f"stnt.tip.{t['id']}={t[tk]}")
            for blk in BLOCKS:
                lines.append(f"tile.stnt:{blk['id']}.name={blk[nk]}")
                lines.append(f"stnt.tip.{blk['id']}={blk[tk]}")
            for it in ITEMS:
                lines.append(f"item.stnt:{it['id']}.name={it[nk]}")
                lines.append(f"stnt.tip.{it['id']}={it[tk]}")
            open(os.path.join(pack, f"texts/{lang}.lang"), 'w',
                 encoding='utf-8').write("\n".join(lines) + "\n")

    # ---------- oyuncu boyutu (kucultme/buyutme altyapisi)
    # BP: st:size ozelligi + her kademe icin collision_box component_group +
    #     olaylar (triggerEvent ile kademe secilir).
    # RP: VANILLA client_entity (Mojang bedrock-samples) + kademe olcegi.
    #     Vanilla alt-varliklar (humanoid geometry, 72 animasyon, render
    #     controller'lari) MC tarafindan saglanir; sadece client_entity
    #     override edilir, boylece oyuncu bozulmaz — yalniz gorsel olcek eklenir.
    # player.json paketler arasi BIRLESMEZ: bu ozellik MorphX ile ayni dunyada
    #     cakisir (ustteki paket kazanir), ayri dunyalarda ikisi de calisir.
    all_groups = [f"st:size_{i}" for i in SIZE_TABLE]
    size_groups, size_events = {}, {}
    for i, (scale, cw, ch) in SIZE_TABLE.items():
        size_groups[f"st:size_{i}"] = {"minecraft:collision_box": {"width": cw, "height": ch}}
        size_events[f"st:size_{i}"] = {
            "set_property": {"st:size": i},
            "add": {"component_groups": [f"st:size_{i}"]},
            "remove": {"component_groups": [g for g in all_groups if g != f"st:size_{i}"]}}
    # morph olaylari (MORPHS bossa hic uretilmez -> morph tamamen kapali).
    morph_events = {}
    if MORPHS:
        morph_events["st:morph_human"] = {"set_property": {"st:morph": 0}}
        for mo in MORPHS:
            morph_events[f"st:morph_{mo['key']}"] = {"set_property": {"st:morph": mo['n']}}
    _, def_cw, def_ch = SIZE_TABLE[SIZE_DEFAULT]
    w(os.path.join(BP, "entities/player.json"), {
        "format_version": "1.21.0",
        "minecraft:entity": {
            "description": {
                "identifier": "minecraft:player",
                "is_spawnable": False, "is_summonable": False, "is_experimental": False,
                "properties": {
                    "st:size": {"type": "int", "range": [0, 4],
                                "default": SIZE_DEFAULT, "client_sync": True},
                    **({"st:morph": {"type": "int", "range": [0, len(MORPHS)],
                                     "default": 0, "client_sync": True}} if MORPHS else {})}},
            "components": {**PLAYER_BASE,
                           "minecraft:collision_box": {"width": def_cw, "height": def_ch}},
            "component_groups": size_groups,
            "events": {**size_events, **morph_events},
        },
    })
    # MorphX ile ayni bos loot (oyuncu olunce ekstra drop olmasin)
    w(os.path.join(BP, "loot_tables/empty.json"), {"pools": []})
    # RP: vanilla client_entity + kademe olcek carpani (render-Molang).
    # vanilla scale "0.9375" tabani korunur, kademe carpaniyla carpilir.
    rp_player = json.load(open(os.path.join(HERE, "player_rp_base.json"), encoding="utf-8"))
    # Vanilla format 1.26.0 KORUNUR. Onceden 1.10.0'a dusurulmustu (min_engine
    # tutarliligi icin) ama vanilla dosya 1.26 icerikli — 1.10.0 etiketi altinda
    # yeni scripts alanlari (scale dahil) yanlis yorumlanip render olcegini VE
    # morph render controller'larini bozuyordu (kucultunce gorsel degismiyordu).
    # Cihaz 1.26.33 oldugu icin 1.26.0 format sorunsuz calisir.
    rp_desc = rp_player["minecraft:client_entity"]["description"]
    factor = "1.0"
    for i in sorted(SIZE_TABLE):
        if i == SIZE_DEFAULT:
            continue
        factor = f"(query.property('st:size') == {i} ? {SIZE_TABLE[i][0]} : {factor})"
    # morph olcegi: golem gercek boyutuna yakin olsun (creeper zaten ~player boyu)
    mfactor = "1.0"
    for mo in MORPHS:
        if mo.get('scale', 1.0) != 1.0:
            mfactor = f"(query.property('st:morph') == {mo['n']} ? {mo['scale']} : {mfactor})"
    rp_desc["scripts"]["scale"] = f"({rp_desc['scripts']['scale']}) * {factor} * {mfactor}"
    # ---- morph gorunumleri: SADECE MORPHS doluysa (bossa vanilla korunur).
    if MORPHS:
        for mo in MORPHS:
            for slot, geo, texs, mats in morph_passes(mo):
                rp_desc["geometry"][f"{mo['key']}{slot}"] = geo
                for tk, tv in texs:
                    rp_desc["textures"][tk] = tv
                for _bone, mk, mv in mats:
                    rp_desc["materials"][mk] = mv
        # vanilla insan controller'larini st:morph==0 ile guardla + morph ekle.
        guarded = []
        for e in rp_desc["render_controllers"]:
            for name, cond in e.items():
                guarded.append({name: cond} if name.endswith(".map")
                               else {name: f"({cond}) && query.property('st:morph') == 0"})
        for mo in MORPHS:
            for slot, _, _, _ in morph_passes(mo):
                rc = f"controller.render.morph.{mo['key']}{slot}"
                guarded.append({f"{rc}.first_person":
                                f"variable.is_first_person && !query.is_spectator && query.property('st:morph') == {mo['n']}"})
                guarded.append({f"{rc}.third_person":
                                f"!variable.is_first_person && !variable.map_face_icon && !query.is_spectator && query.property('st:morph') == {mo['n']}"})
        rp_desc["render_controllers"] = guarded
        # ---- durus pozu: bind pozu kotu olan vanilla modeller icin.
        # Bedrock animasyon donusleri bind pozunun USTUNE eklenir, o yuzden
        # tek kareli bir animasyon vanilla modeli degistirmeden pozu duzeltir.
        # Kosul st:morph oldugu icin poz yalnizca o kiliktayken uygulanir.
        posed = [mo for mo in MORPHS if mo.get('pose')]
        if posed:
            anims = {}
            for mo in posed:
                anims[f"animation.stnt.pose.{mo['key']}"] = {
                    "loop": True,
                    "bones": {bone: {"rotation": rot} for bone, rot in mo['pose'].items()},
                }
            w(os.path.join(RP, "animations/morph_pose.animation.json"),
              {"format_version": "1.8.0", "animations": anims})
            for mo in posed:
                rp_desc["animations"][f"pose_{mo['key']}"] = f"animation.stnt.pose.{mo['key']}"
                rp_desc["scripts"]["animate"].append(
                    {f"pose_{mo['key']}": f"query.property('st:morph') == {mo['n']}"})
    w(os.path.join(RP, "entity/player.json"), rp_player)

    # ---- morph render controller dosyasi (SADECE MORPHS doluysa)
    if MORPHS:
        morph_rc = {}
        for mo in MORPHS:
            for slot, _, texs, mats in morph_passes(mo):
                name = f"controller.render.morph.{mo['key']}{slot}"
                base = {"geometry": f"Geometry.{mo['key']}{slot}",
                        "materials": [{bone: f"Material.{mk}"} for bone, mk, _ in mats],
                        "textures": [f"Texture.{tk}" for tk, _ in texs]}
                morph_rc[f"{name}.third_person"] = dict(base)
                fp = dict(base)  # ilk sahiste mob'un on uzvu gorunsun ki el bos olmasin
                fp["part_visibility"] = [{"*": False}] + [{b: True} for b in mo.get('fp_bones', [])]
                morph_rc[f"{name}.first_person"] = fp
        w(os.path.join(RP, "render_controllers/morph.render_controllers.json"),
          {"format_version": "1.10.0", "render_controllers": morph_rc})

    # ---------- script
    spec = {t['id']: t['effect'] for t in TNTS}
    tips = {t['id']: t['tr'] for t in TNTS}
    tiptext = {t['id']: t['trtip'] for t in TNTS}
    item_actions = {it['id']: it['action'] for it in ITEMS if it.get('action')}
    fake_tools = {f"stnt:{f['id']}": f['wood'] for f in FAKE_TOOLS}
    fake_names = {f"stnt:{f['id']}": f['woodtr'] for f in FAKE_TOOLS}
    script = SCRIPT_TEMPLATE.replace("__SPEC__", json.dumps(spec, indent=2)) \
                            .replace("__NAMES__", json.dumps(tips, ensure_ascii=False)) \
                            .replace("__TIPS__", json.dumps(tiptext, ensure_ascii=False)) \
                            .replace("__ITEM_ACTIONS__", json.dumps(item_actions)) \
                            .replace("__FAKE_TOOLS__", json.dumps(fake_tools)) \
                            .replace("__FAKE_NAMES__", json.dumps(fake_names, ensure_ascii=False)) \
                            .replace("__MORPH_FORMS__", json.dumps(
                                [{"cat": c, "items": [{"ev": f"st:morph_{mo['key']}", "name": mo['tr']}
                                                      for mo in MORPHS if mo['cat'] == c]}
                                 for c in MORPH_CATS],
                                ensure_ascii=False)) \
                            .replace("__TREE_WOOD__", json.dumps(TREE_WOOD)) \
                            .replace("__SIZE_DEFAULT__", str(SIZE_DEFAULT)) \
                            .replace("__MORPH_HINT__", json.dumps(MORPH_HINT, ensure_ascii=False)) \
                            .replace("__BLOCK_MORPH_MAP__", json.dumps(BLOCK_MORPH_MAP, ensure_ascii=False)) \
                            .replace("__FOREVER_TICKS__", str(FOREVER_TICKS)) \
                            .replace("__FOREVER_EVERY__", str(FOREVER_EVERY)) \
                            .replace("__BOOST_AMP__", str(BOOST_AMP)) \
                            .replace("__BOOST_HP__", str(BOOST_HP)) \
                            .replace("__MORPH_ABIL__", json.dumps(
                                {mo['n']: {k: mo[k] for k in ("pas", "act", "pow") if k in mo}
                                 for mo in MORPHS if 'pas' in mo or 'act' in mo},
                                ensure_ascii=False)) \
                            .replace("__FUSE__", str(FUSE_TICKS)) \
                            .replace("__ENDER_FIRE__", json.dumps(ENDER_FIRE)) \
                            .replace("__GOLD_FIRE__", json.dumps(GOLD_FIRE)) \
                            .replace("__UNENDING__", json.dumps(UNENDING, ensure_ascii=False)) \
                            .replace("__PORTAL_N__", str(len(PORTAL_COLORS))) \
                            .replace("__PORTAL_NAMES__", json.dumps([n for n, _ in PORTAL_COLORS], ensure_ascii=False)) \
                            .replace("__SIZE_SCALES__", json.dumps({i: SIZE_TABLE[i][0] for i in SIZE_TABLE})) \
                            .replace("__SIZE_CAM_FWD__", json.dumps(SIZE_CAM_FWD)) \
                            .replace("__SIZE_H__", json.dumps({i: SIZE_TABLE[i][2] for i in SIZE_TABLE})) \
                            .replace("__BLOCK_MORPH_BY_N__", json.dumps(
                                {m["n"]: f"st:morph_{m['key']}" for m in BLOCK_MORPHS}, ensure_ascii=False)) \
                            .replace("__MORPH_MAP__", json.dumps(MORPH_MAP, ensure_ascii=False))
    os.makedirs(os.path.join(BP, "scripts"), exist_ok=True)
    open(os.path.join(BP, "scripts/main.js"), 'w', encoding='utf-8').write(script)

    # ---------- manifest son kontrol
    # Bir kez script modulunde "language" eksikti ve MC tum davranis paketini
    # sessizce reddetti (listede hic gorunmedi, hata da vermedi). Cihaza
    # atmadan once yakala.
    for pack, label in ((BP, "BP"), (RP, "RP")):
        mf = json.load(open(os.path.join(pack, "manifest.json"), encoding="utf-8"))
        has_script = any(m.get("type") == "script" for m in mf.get("modules", []))
        for m in mf.get("modules", []):
            if m.get("type") == "script" and not m.get("language"):
                raise SystemExit(f"{label}: script modulunde 'language' eksik "
                                 f"-> paket sessizce reddedilir")
        if has_script and "script_eval" not in (mf.get("capabilities") or []):
            raise SystemExit(f"{label}: script var ama capabilities'te 'script_eval' yok")
    print("manifest kontrol: gecti")

    # ---------- paketle
    os.makedirs(OUT, exist_ok=True)
    target = os.path.join(OUT, "SuperTNT.mcaddon")
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as z:
        for p in (BP, RP):
            top = os.path.basename(p)
            for dp, _, fs in os.walk(p):
                for f in fs:
                    full = os.path.join(dp, f)
                    z.write(full, os.path.join(top, os.path.relpath(full, p)))

    print(f"TNT sayisi        : {len(TNTS)}")
    print(f"doku              : {copied} kopyalandi, {generated} uretildi")
    print(f"cikti             : {target}")
    print(f"boyut             : {os.path.getsize(target)/1024:.0f} KB")
    return target


SCRIPT_TEMPLATE = r'''// Super TNT Mod - Bedrock
// Uretilmis dosya. Kaynak: bedrock/build.py  (elle duzenleme, yeniden uretilir)
import { world, system, BlockPermutation, ItemStack, EquipmentSlot } from "@minecraft/server";
import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

const SPEC = __SPEC__;
const NAMES = __NAMES__;
const TIPS = __TIPS__;
const ITEM_ACTIONS = __ITEM_ACTIONS__;
const TREE_WOOD = __TREE_WOOD__;       // fidan -> [govde blogu, yaprak blogu]
const MORPH_FORMS = __MORPH_FORMS__;   // donusum menusu: kategori -> mob listesi
const MORPH_ABIL = __MORPH_ABIL__;     // st:morph -> {pas:[etki,seviye], act:"...", pow:{...}}
const MORPH_HINT = __MORPH_HINT__;     // st:morph_<mob> olayi -> eylem cubugu metni
const BLOCK_MORPH_MAP = __BLOCK_MORPH_MAP__;   // kirilan blok -> blok kiligi olayi
const BLOCK_MORPH_EVENTS = new Set(Object.values(BLOCK_MORPH_MAP));
const MORPH_MAP = __MORPH_MAP__;   // mob typeId -> st:morph_<key> olayi
const SIZE_SCALES = __SIZE_SCALES__;   // st:size kademe -> gorsel olcek
const FUSE = __FUSE__;

// ---------------------------------------------------------------- item'lar
// Elde tutulan esyadan BIR tane eksiltir (yoksa hicbir sey yapmaz).
// "Tek kullanimlik" diyen bir ipucu varsa esya gercekten harcanmalidir; aksi
// halde ipucu yalan soyler ve esya sinirsiz kullanilir. Bedrock'ta ItemStack
// bir KOPYADIR: amount'u degistirip container'a GERI YAZMAK gerekir.
function consumeHeld(pl, typeId) {
  try {
    const inv = pl.getComponent("minecraft:inventory");
    const con = inv && inv.container;
    if (!con) return;
    const slot = pl.selectedSlotIndex;
    const it = con.getItem(slot);
    if (!it || it.typeId !== typeId) return;
    if (it.amount > 1) { it.amount -= 1; con.setItem(slot, it); }
    else con.setItem(slot, undefined);
  } catch (e) {}
}
// Yiyip-icilen esyalar sag tikta DEGIL, animasyon bitince etki etmeli; yoksa
// cocuk tiklayip birakiyor ve esya harcanmadan efekt aliyor.
const CONSUMED = { eat: 1, heal_boost: 1 };
world.afterEvents.itemUse.subscribe((ev) => {
  // Dinleyiciden kacan tek bir istisna BUTUN sihirli esyalari o seans
  // boyunca oldurur; ev.itemStack surume gore bos gelebiliyor.
  if (!ev || !ev.itemStack || !ev.source) return;
  const a = ITEM_ACTIONS[ev.itemStack.typeId.replace("stnt:", "")];
  if (a && !CONSUMED[a.type]) itemAction(ev.source, a);
});
world.afterEvents.itemCompleteUse.subscribe((ev) => {
  if (!ev || !ev.itemStack || !ev.source) return;
  const a = ITEM_ACTIONS[ev.itemStack.typeId.replace("stnt:", "")];
  if (!a) return;
  if (a.type === "eat") {
    try { ev.source.addEffect(a.effect, a.seconds * 20, { amplifier: a.amp, showParticles: true }); } catch (e) {}
  } else if (a.type === "heal_boost") {
    healBoost(ev.source, a);
  }
});

// Saglik Iksiri. health_boost AZAMI cani buyutur ama mevcut cani doldurmaz;
// ayrica yeni azami deger ayni tick'te okunamiyor. Bu yuzden efekti verip bir
// tick sonra cani tepeye cekiyoruz.
// Sinirsiz can isareti oyuncunun kendi dinamik ozelliginde: kalicidir, yani
// cocuk cikip girince de surer. Dongu (asagida) bunu gorup efekti tazeler.
const FOREVER_PROP = "stnt:hpforever";
function healBoost(player, a) {
  try {
    if (a.forever) player.setDynamicProperty(FOREVER_PROP, 1);
    player.addEffect("health_boost", a.forever ? __FOREVER_TICKS__ : a.seconds * 20,
                     { amplifier: a.amp, showParticles: true });
  } catch (e) { return; }
  system.runTimeout(() => {
    try {
      const hp = player.getComponent("minecraft:health");
      if (hp) hp.resetToMaxValue();
      spray(player.dimension, player.location, "minecraft:heart_particle", 12, 1.0);
      player.onScreenDisplay.setActionBar(a.forever
        ? `§aCanın §c${hp ? Math.round(hp.currentValue) : a.hp}§a oldu! §7Süresiz — Temizleyici TNT ile geri alırsın`
        : `§aCanın §c${hp ? Math.round(hp.currentValue) : a.hp}§a oldu! §7${a.seconds / 60} dakika sürer`);
    } catch (e) {}
  }, 1);
}

// Sinirsiz can: isareti duran oyuncuda efekti tazele ve cani tepeye cek.
// Tazeleme olmadan "sinirsiz" sozu tutulmaz — Bedrock'ta efekt suresi sonsuz
// olamaz, en fazla uzun olur. Cani da doldurmak sart: azami can 1044 olsa bile
// dolu olmayan can bir sure sonra biter.
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    try {
      if (!p.getDynamicProperty(FOREVER_PROP)) continue;
      p.addEffect("health_boost", __FOREVER_TICKS__, { amplifier: __BOOST_AMP__, showParticles: false });
      const hp = p.getComponent("minecraft:health");
      if (hp && hp.currentValue < __BOOST_HP__) hp.resetToMaxValue();
    } catch (e) {}
  }
}, __FOREVER_EVERY__);

// ---- Ses Saldirisi (Warden'in sonic boom'u). Bakilan yonde 1 blokluk adimlarla
// ilerleyen bir ses dalgasi. BLOKLARDAN GECER — vanilla Warden'da da oyle; bu
// yuzden hedefi gorup gormedigimize bakmadan yol boyunca varlik tariyoruz.
// Her varlik yalniz BIR KEZ vurulur (hit kumesi), yoksa 24 adimin her birinde
// tekrar hasar alip aninda olurdu.
// Dalganin savurmamasi gerekenler: yerdeki esya ve tecrube kuresi. Yoksa
// cocuk kendi ganimetini 20 blok oteye ucuruyor.
const sonicCd = new Map();
const CRAFT_AXE_MAX = 20000;        // ~27x27x27; kabul edilen en buyuk alan
const CRAFT_AXE_PER_TICK = 700;     // tick basina BAKILAN konum (paketin deseni)
const SONIC_SKIP = { "minecraft:item": 1, "minecraft:xp_orb": 1, "minecraft:xp_bottle": 1 };

function sonicBoom(player, a) {
  const dim = player.dimension, v = player.getViewDirection(), s = player.getHeadLocation();
  const hit = new Set();
  let n = 0;
  for (let i = 1; i <= a.range; i++) {
    const c = { x: s.x + v.x * i, y: s.y + v.y * i, z: s.z + v.z * i };
    try { dim.spawnParticle("minecraft:sonic_explosion", c); } catch (e) {}
    let ents = [];
    try { ents = dim.getEntities({ location: c, maxDistance: 2 }); } catch (e) {}
    for (const e of ents) {
      if (e.id === player.id || hit.has(e.id) || SONIC_SKIP[e.typeId]) continue;
      hit.add(e.id);
      n++;
      // "sonicBoom" hasar nedeni bazi surumlerde yok -> duz hasara dus.
      try { e.applyDamage(a.damage, { cause: "sonicBoom", damagingEntity: player }); }
      catch (err) { try { e.applyDamage(a.damage); } catch (err2) {} }
      // applyKnockback yalniz oyuncu/mob'da var; esya/mermide hata verir.
      try { e.applyKnockback(v.x, v.z, a.knock, 0.45); }
      catch (err) { try { e.applyImpulse({ x: v.x * 0.8, y: 0.35, z: v.z * 0.8 }); } catch (err2) {} }
    }
  }
  // Tek ses. "sonic_charge" ~1,4 sn suren bir yukselis; boom ile ayni tick'te
  // calinca patlamadan SONRA vinlemeye devam ediyordu.
  try { dim.playSound("mob.warden.sonic_boom", s, { volume: 1.4 }); } catch (e) {}
  return n;
}

// ---- Mega Gubre: TREE_H blok boyunda dev agac.
// Vanilla agac uretimi bu olcege cikmaz, agac blok blok kuruluyor: ~20 bin
// blok. Hepsi tek tick'te konursa tablet donar -> asagidan yukari, KATMAN
// KATMAN, tick basina butceyle ilerliyor (TNT'lerin "transform"/"place"
// isleriyle ayni desen). Yan fayda: cocuk agacin buyudugunu goruyor.
// Blok sayisi / sure olcumu: bedrock/tools/tree_check.py — o betik bu
// fonksiyonun aynasidir, buradaki formulleri degistirirsen orayi da degistir.
//
// YAPRAK KALICILIGI. Betikten konan yaprak varsayilan olarak persistent_bit
// yanlis gelir ve govdeye 4 bloktan uzaksa CURUR. Tepe yaricapi 22 oldugundan
// tepenin nerdeyse tamami curuyup yok olurdu; bu yuzden yaprak setType ile
// degil, persistent_bit=true permutation'i ile konuyor.
const treeBusy = new Set();          // ayni oyuncuda ikinci agac baslamasin
// Fidan arama sirasi: once tam ortasi, sonra yanlar, en son koseler.
const TREE_NEAR = [[0, 0], [1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [1, -1], [-1, 1], [-1, -1]];
const DIM_CEIL = { "minecraft:nether": 127, "minecraft:the_end": 255 };

function megaTree(player, a) {
  const dim = player.dimension;
  if (treeBusy.has(player.id)) {
    try { player.onScreenDisplay.setActionBar("§7Ağaç zaten büyüyor…"); } catch (e) {}
    return;
  }
  const hit = player.getBlockFromViewDirection({ maxDistance: 12 });
  if (!hit) {
    try { player.onScreenDisplay.setActionBar("§7Bir fidana ya da toprağa bak"); } catch (e) {}
    return;
  }
  // getBlockFromViewDirection GECILEBILIR bloklari atlar — isin fidanin
  // icinden gecip altindaki topraga carpar. Yani carpilan blok her zaman
  // ZEMIN; agac onun bir ustune dikilir. Fidan da o katmanda aranir, hem de
  // 3x3'te: cocuk kucucuk fidana tam nisan alamayabilir, isin bir blok yandan
  // gecebilir. Bulunamazsa mese.
  const g = hit.block.location;
  let base = { x: g.x, y: g.y + 1, z: g.z };
  let wood = null;
  for (const d of TREE_NEAR) {
    let b = null;
    try { b = dim.getBlock({ x: g.x + d[0], y: g.y + 1, z: g.z + d[1] }); } catch (e) {}
    const w = b && TREE_WOOD[b.typeId];
    if (w) { wood = w; base = { x: g.x + d[0], y: g.y + 1, z: g.z + d[1] }; break; }
  }
  const logId = wood ? wood[0] : "minecraft:oak_log";
  const leafId = wood ? wood[1] : "minecraft:oak_leaves";

  const H = a.height, R0 = a.trunk, CR = a.crown;
  const cy0 = Math.round(H * 0.55);                 // tepenin baslangici
  const cy1 = H + Math.round(CR * 0.6);             // en ust yaprak katmani
  // Tavana sigmiyorsa BUDAMAK yerine reddet: ipucu "100 blok" diyor, yarim
  // agac o sozu tutmaz. Nether'de zaten hic sigmaz, oyuncu bunu bilmeli.
  let maxY = DIM_CEIL[dim.id] !== undefined ? DIM_CEIL[dim.id] : 319;
  try { const hr = dim.heightRange; if (hr && typeof hr.max === "number") maxY = hr.max - 1; } catch (e) {}
  if (base.y + cy1 > maxY) {
    try {
      player.onScreenDisplay.setActionBar(
        `§cBurada yer yok — ağaç ${cy1} blok yüksek. ${base.y + cy1 - maxY} blok aşağıda dene.`);
    } catch (e) {}
    return;
  }

  // Blok cesitleri bir kez cozulur (her blokta resolve etmek pahali).
  // AYRI try'lar: tek blokta cozumleme hata verirse otekiler null kalmasin.
  // (Hepsi tek try'dayken yaprak cozumlemesi patlarsa govde de duz setType'a
  // dusuyordu; yaprak icin duz setType = tepe curur.)
  const resolvePerm = (id, states) => {
    try { return BlockPermutation.resolve(id, states); } catch (e) { return null; }
  };
  const leafPerm = resolvePerm(leafId, { persistent_bit: true, update_bit: false });
  const logY = resolvePerm(logId, { pillar_axis: "y" });
  const logX = resolvePerm(logId, { pillar_axis: "x" });
  const logZ = resolvePerm(logId, { pillar_axis: "z" });

  let total = 0, budget = 0;
  // onlyAir: yaprak yalniz havanin/yapragin yerine konur — agac tepesi araziyi
  // ya da govdeyi yemesin. Govde ve dallar ise kayanin disinda her seyi ezer,
  // yoksa yamacta buyuyen agac delik desik kalir.
  const put = (x, y, z, perm, id, onlyAir) => {
    // budget INCELENEN pozisyonu sayar. Erken return'lerin altinda olsaydi
    // (ve oyleydi) yamacta buyuyen agacta tepe katmanlari onlyAir'e takilip
    // geri doner, budget sifirda kalir ve kalan ~58 katman tek tick'te inerdi.
    budget++;
    try {
      const b = dim.getBlock({ x, y, z });
      if (!b) return;
      const cur = b.typeId;
      if (cur === "minecraft:bedrock") return;
      if (onlyAir && cur !== "minecraft:air" && !cur.endsWith("_leaves")) return;
      if (perm) b.setPermutation(perm); else b.setType(id);
      total++;
    } catch (e) {}
  };

  // Dallar once hesaplanir: y -> [[dx, dz, eksen], ...]. Katman taramasi
  // yukari ciktikca o yukseklige dusen dal parcalarini birlikte koyar.
  const branches = new Map();
  const addBranch = (y, cell) => {
    const l = branches.get(y);
    if (l) l.push(cell); else branches.set(y, [cell]);
  };
  for (let i = 0; i < 4; i++) {
    const y0 = Math.round(H * (0.60 + i * 0.09));
    const n = 4 + (i % 2);                          // katta 4-5 dal
    const len = Math.round(CR * (0.75 - i * 0.12));
    for (let k = 0; k < n; k++) {
      const ang = (k / n) * Math.PI * 2 + i * 0.7;  // her kat kaydirilmis
      const ux = Math.cos(ang), uz = Math.sin(ang);
      const axis = Math.abs(ux) >= Math.abs(uz) ? "x" : "z";
      for (let d = 1; d <= len; d++) {
        const x = Math.round(ux * d), z = Math.round(uz * d);
        const y = y0 + Math.round(d * 0.45);        // dallar yukari dogru
        addBranch(y, [x, z, axis]);
        if (d > 2) addBranch(y - 1, [x, z, axis]);  // iki blok kalinlik
      }
    }
  }

  const trunkR = (y) => Math.max(1, R0 * (1 - 0.72 * Math.min(1, y / H)));
  // Tepe silueti: alttan ve ustten sifir, ortada en genis (sin egrisi).
  const crownR = (y) => {
    if (y < cy0 || y > cy1) return 0;
    return CR * Math.pow(Math.sin(Math.PI * (y - cy0) / (cy1 - cy0)), 0.55);
  };

  // Oyuncu neredeyse her zaman govdenin icinde kalir: taban yaricapi 5, cocuk
  // fidana 1-2 blok mesafeden tikliyor. Ilk tick'te govde onu diri diri
  // gomerdi. Buyume baslamadan kenara cekiyoruz — govdede delik birakmak
  // yerine, cunku delik de oyuncuyu 1x1 bir cukura hapsediyordu.
  // Tehlike YALNIZ govde/dal (log) icin var: yaprak bogmaz, ustelik yaprak
  // sadece havanin yerine konuyor. Bu yuzden yalniz govdenin yukseklik
  // araligindaki oyuncu cekilir — yukarida ucan cocuk yerinden edilmez.
  const safeR = R0 + 3;
  const pl = player.location;
  const yRel = pl.y - base.y;
  let ox = pl.x - (base.x + 0.5), oz = pl.z - (base.z + 0.5);
  let olen = Math.hypot(ox, oz);
  if (olen < 0.001) {                               // tam merkezdeyse arkasina
    const v = player.getViewDirection();
    ox = -v.x; oz = -v.z; olen = Math.hypot(ox, oz) || 1;
  }
  if (olen < safeR && yRel >= -3 && yRel <= H) {
    const tx = Math.floor(base.x + 0.5 + (ox / olen) * safeR);
    const tz = Math.floor(base.z + 0.5 + (oz / olen) * safeR);
    let moved = false;
    // Once ayaginin altinda zemin OLAN bir yer ara (dusme hasari olmasin),
    // asagidan yukari; bulunamazsa vazgec ve oyuncuyu uyar.
    for (let dy = -3; dy <= 6 && !moved; dy++) {
      const ty = Math.floor(pl.y) + dy;
      try {
        const a1 = dim.getBlock({ x: tx, y: ty, z: tz });
        const a2 = dim.getBlock({ x: tx, y: ty + 1, z: tz });
        const gr = dim.getBlock({ x: tx, y: ty - 1, z: tz });
        if (a1 && a2 && gr && a1.typeId === "minecraft:air" && a2.typeId === "minecraft:air"
            && gr.typeId !== "minecraft:air") {
          player.teleport({ x: tx + 0.5, y: ty, z: tz + 0.5 });
          moved = true;
        }
      } catch (e) {}
    }
    if (!moved) {
      try {
        player.onScreenDisplay.setActionBar(
          `§cÇok yakınsın — ağaç seni ezer. ${Math.ceil(safeR)} blok geri çekil.`);
      } catch (e) {}
      return;
    }
    try { player.onScreenDisplay.setActionBar("§eAğaç büyüyor — kenara çekildin!"); } catch (e) {}
  }

  const treePid = player.id;      // yakalanan `player` sonra gecersiz olabilir
  treeBusy.add(treePid);
  let y = -3;                                       // kok: uc katman toprak alti
  const job = system.runInterval(() => {
    budget = 0;
    while (y <= cy1 && budget < a.perTick) {
      if (y <= H) {                                 // govde + kok
        const r = y < 0 ? R0 + 1 : trunkR(y), ri = Math.ceil(r);
        for (let dx = -ri; dx <= ri; dx++) {
          for (let dz = -ri; dz <= ri; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            put(base.x + dx, base.y + y, base.z + dz, logY, logId, false);
          }
        }
      }
      const bs = branches.get(y);                   // dallar
      if (bs) {
        for (const c of bs) {
          put(base.x + c[0], base.y + y, base.z + c[1], c[2] === "x" ? logX : logZ, logId, false);
        }
      }
      const cr = crownR(y);                         // tepe
      if (cr >= 1) {
        const ri = Math.ceil(cr), inner = cr - 2.2;
        for (let dx = -ri; dx <= ri; dx++) {
          for (let dz = -ri; dz <= ri; dz++) {
            const dd = Math.sqrt(dx * dx + dz * dz);
            if (dd > cr) continue;
            // Kabuk dolu, ici seyrek: hem dogal gorunur hem ~3 kat az blok.
            if (dd < inner && Math.random() > 0.1) continue;
            put(base.x + dx, base.y + y, base.z + dz, leafPerm, leafId, true);
          }
        }
      }
      y++;
    }
    if (y > cy1) {
      system.clearRun(job);
      treeBusy.delete(treePid);
      try { player.onScreenDisplay.setActionBar(`§aDev ağaç büyüdü! §f${total}§a blok`); } catch (e) {}
    }
  }, 1);
  spray(dim, base, "minecraft:crop_growth_emitter", 40, 3);
}

function itemAction(player, a) {
  const dim = player.dimension;
  try {
    switch (a.type) {
      case "morph_reset": {
        // Bir moba BAKIYORSAN dogrudan ona donus, yoksa menuyu ac (insana
        // donmek menunun ilk maddesi). Eskiden bu dal moba bakarken hicbir sey
        // yapmiyor, "dokunma olayi devralir" diye birakiliyordu; ama dusman
        // mob'larda interact olayi HIC tetiklenmez ve sag tik bir vurus da
        // degildir -> tik olu kaliyordu. 83 mob'la neredeyse her mob morph
        // edilebilir oldugundan bu her yerde olur hale gelmisti.
        try {
          const hs = player.getEntitiesFromViewDirection({ maxDistance: 4 });
          const tgt = hs.length ? hs[0].entity : null;
          if (tgt && MORPH_MAP[tgt.typeId]) { tryMorph(player, tgt); break; }
          morphMenu(player);
        } catch (e) {}
        break;
      }
      case "mega_tree": {
        megaTree(player, a);
        break;
      }
      case "dragon_breath": {
        dragonBreath(player, a);
        break;
      }
      case "rename": {
        renamePrompt(player, a, 20);
        break;
      }
      case "block_morph": {
        blockMorphUse(player);
        break;
      }
      case "sonic": {
        // Kisa bekleme: her tik 24 adim + 24 varlik taramasi demek, hizli
        // tiklama tableti yorar. 10 tick fark edilmeyecek kadar kisa.
        const now = system.currentTick;
        if ((sonicCd.get(player.id) || 0) > now) {
          try { player.onScreenDisplay.setActionBar("§7Ses dalgası hazırlanıyor…"); } catch (e) {}
          break;
        }
        sonicCd.set(player.id, now + 10);
        const n = sonicBoom(player, a);
        try {
          player.onScreenDisplay.setActionBar(
            n ? `§bSes dalgası §f${n}§b hedefi vurdu!` : "§bSes dalgası gönderildi");
        } catch (e) {}
        break;
      }
      case "team_wand": {
        // bosluga tiklandiginda ipucu; asil isaretleme mob'a dokununca
        // (playerInteractWithEntity handler'i) olur.
        try { player.onScreenDisplay.setActionBar("§eBir mob'a dokun — takımına katsın."); } catch (e) {}
        break;
      }
      case "resize": {
        // KENDINI kademe kucult/buyut ya da normale don (a.reset).
        try {
          let sz = player.getProperty("st:size");
          if (typeof sz !== "number") sz = __SIZE_DEFAULT__;
          const hedef = a.reset ? __SIZE_DEFAULT__ : Math.max(0, Math.min(4, sz + a.delta));
          // BUYURKEN tavan var mi? Kucukken girilen bir bloklik bosluktan
          // sonra buyumek oyuncuyu bloklarin ICINDE birakir; Minecraft ya
          // disari itiyor ya da sikisip kaliyor. Kucultmede kontrol yok:
          // kucuk kutu her zaman sigar.
          if (SIZE_H[hedef] > SIZE_H[sz] && !tavanVarMi(player, SIZE_H[hedef])) {
            try { player.onScreenDisplay.setActionBar("§cBurada büyüyecek yer yok — açık bir yere geç"); } catch (e) {}
            break;
          }
          sz = hedef;
          player.triggerEvent("st:size_" + sz);
          spray(dim, player.location,
                (a.reset || a.delta > 0) ? "minecraft:totem_particle" : "minecraft:mob_portal", 24, 2);
        } catch (e) {}
        break;
      }
      case "steal": {
        // En yakin BASKA oyuncunun ilk dolu slotunu kendine aktar.
        try {
          const mine = player.getComponent("minecraft:inventory")?.container;
          let done = false;
          for (const v of dim.getEntities({ location: player.location, maxDistance: a.radius || 8, type: "minecraft:player" })) {
            if (v.id === player.id) continue;
            const inv = v.getComponent("minecraft:inventory")?.container;
            if (inv && mine) {
              for (let i = 0; i < inv.size; i++) {
                if (inv.getItem(i)) { inv.transferItem(i, mine); done = true; break; }
              }
            }
            if (done) { try { player.onScreenDisplay.setActionBar(`§e${v.name} adlı oyuncunun eşyasını çaldın!`); } catch (e) {} }
            break;   // sadece en yakin oyuncu
          }
          if (!done) { try { player.onScreenDisplay.setActionBar("§7Yakında çalınacak eşya yok"); } catch (e) {} }
        } catch (e) {}
        break;
      }
      case "remote_detonate": {
        // Yerlestirilen tum TNT bloklarini (tracked) uzaktan atesle.
        let n = 0;
        for (const k of [...tracked]) {
          try {
            const [dimId, xyz, owner] = k.split("|");
            if (dimId !== player.dimension.id) continue;
            if (owner && owner !== player.id) continue;   // sadece KENDI koydugun TNT
            const [x, y, z] = xyz.split(",").map(Number);
            const b = dim.getBlock({ x, y, z });
            if (b && b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
              ignite(dim, b.location, b.typeId.slice(5), player.id);
              n++;
            }
          } catch (e) {}
        }
        try { player.onScreenDisplay.setActionBar(`§c${n} TNT uzaktan ateşlendi!`); } catch (e) {}
        break;
      }
      case "portal_gun": {
        // Bakilan yuzeyin ustune RENKLI portal blogu koy + kaydet. Her iki tik
        // ayni rengi kullanir -> bir cift. AYNI RENKTEN iki portal birbirine
        // isinlar (global kayit + en yakin es). Elle envanterden konan portal
        // bloklari da ayni sekilde calisir (playerPlaceBlock handler'i).
        const hit = player.getBlockFromViewDirection({ maxDistance: a.range || 48 });
        if (!hit) { try { player.onScreenDisplay.setActionBar("§7Portal için bir yüzeye bak"); } catch (e) {} break; }
        const above = { x: Math.floor(hit.block.location.x), y: Math.floor(hit.block.location.y) + 1, z: Math.floor(hit.block.location.z) };
        const PORTAL_N = __PORTAL_N__;   // build.py PORTAL_COLORS ile senkron
        const PC = __PORTAL_NAMES__;
        const gc = portalGunCount.get(player.id) || 0;
        portalGunCount.set(player.id, gc + 1);
        const c = Math.floor(gc / 2) % PORTAL_N;   // 1.-2. tik renk0, 3.-4. renk1 ...
        try { dim.getBlock(above)?.setType("stnt:portal_" + c); } catch (e) {}
        addPortal(player.dimension.id, above.x, above.y, above.z, c);
        const total = getPortals().filter((q) => q.c === c).length;
        try { player.onScreenDisplay.setActionBar(total >= 2
          ? `§a${PC[c]} portal bağlandı! Üstüne bas, eşine ışınlan.`
          : `§b${PC[c]} portal kondu — ikinci ${PC[c]} kapı için tekrar tıkla`); } catch (e) {}
        spray(dim, above, "minecraft:mob_portal", 30, 1.5);
        break;
      }
      case "ender_fire": { lightFire(player, dim, a, false); break; }
      case "gold_fire": { lightFire(player, dim, a, true); break; }
      case "lightning": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 64 });
        const p = hit ? hit.block.location : player.location;
        try { dim.spawnEntity("minecraft:lightning_bolt", p); } catch (e) {}
        break;
      }
      case "blackhole": {
        for (const e of dim.getEntities({ location: player.location, maxDistance: a.radius })) {
          if (e.id === player.id) continue;
          try {
            const l = e.location, pl = player.location;
            const dx = pl.x - l.x, dy = pl.y - l.y, dz = pl.z - l.z, len = Math.hypot(dx, dy, dz) || 1;
            // Cekme AYRI try: applyImpulse oyuncuda hata verir ve eskiden bu
            // satir try'in ilkiydi -> korluk ve hasar oyunculara HIC islemiyordu.
            try {
              if (e.typeId === "minecraft:player") e.applyKnockback(dx / len, dz / len, 0.5, 0.1);
              else e.applyImpulse({ x: dx / len * 0.5, y: 0.1, z: dz / len * 0.5 });
            } catch (e2) {}
            e.addEffect("blindness", 100, { amplifier: 0 });
            e.applyDamage(4);
          } catch (err) {}
        }
        spray(dim, player.location, "minecraft:mob_portal", 40, 3);
        break;
      }
      case "break_bedrock": {
        // YALNIZCA bedrock. Eskiden baktigin HER blogu siliyordu; ipucu
        // "bedrock'u kirar" dedigi icin cocuk guvenli saniyor, oysa setType
        // normal kirma yolunu atladigindan sandiga tiklayinca ICINDEKILER de
        // yok oluyordu. Simdi ipucu ne diyorsa o.
        const hit = player.getBlockFromViewDirection({ maxDistance: 8 });
        if (!hit) { try { player.onScreenDisplay.setActionBar("§7Kırmak için bedrock'a bak"); } catch (e) {} break; }
        if (hit.block.typeId !== "minecraft:bedrock") {
          try { player.onScreenDisplay.setActionBar("§7Bu kristal yalnızca §fbedrock§7'u kırar"); } catch (e) {}
          break;
        }
        try { hit.block.setType("minecraft:air"); } catch (e) {}
        break;
      }
      case "laser": {
        const v = player.getViewDirection(), s = player.getHeadLocation();
        for (let i = 1; i <= a.length; i++) {
          const p = { x: s.x + v.x * i, y: s.y + v.y * i, z: s.z + v.z * i };
          try {
            const b = dim.getBlock(p);
            if (b && b.typeId !== "minecraft:air" && b.typeId !== "minecraft:bedrock") b.setType("minecraft:air");
          } catch (e) {}
        }
        for (const h of player.getEntitiesFromViewDirection({ maxDistance: a.length })) {
          try { h.entity.applyDamage(30); } catch (err) {}
        }
        break;
      }
      case "grapple": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 40 });
        if (hit) {
          const l = hit.block.location, pl = player.location;
          const dx = l.x - pl.x, dy = l.y - pl.y, dz = l.z - pl.z, len = Math.hypot(dx, dy, dz) || 1;
          const sp = Math.max(0.9, Math.min(2.8, 0.5 + len * 0.12));
          // player.applyImpulse Bedrock'ta calismaz; player icin applyKnockback (4 sayi)
          try { player.applyKnockback(dx / len, dz / len, sp, Math.max(0.4, dy / len + 0.4)); }
          catch (e) { try { player.applyKnockback({ x: dx / len, z: dz / len }, Math.max(0.4, dy / len + 0.4)); } catch (e2) {} }
        } else { try { player.onScreenDisplay.setActionBar("§7Bir yere/bloğa bak — kancayla oraya çekilirsin"); } catch (e) {} }
        break;
      }
      case "freeze_target": {
        const hs = player.getEntitiesFromViewDirection({ maxDistance: 30 });
        if (hs.length) { try { hs[0].entity.addEffect("slowness", 200, { amplifier: 6 }); hs[0].entity.addEffect("weakness", 200, { amplifier: 2 }); } catch (e) {} }
        else { try { player.onScreenDisplay.setActionBar("§7Bir canlıya bak — onu dondurur"); } catch (e) {} }
        break;
      }
      case "kill_target": {
        // Ipucu "Tek kullanimlik" diyor; o yuzden esya GERCEKTEN harcanir.
        // Onceden yalnizca 3 sn bekleme vardi -> ayni rapor sonsuza kadar
        // kullanilabiliyordu ve bir cocuk kardesini 3 saniyede bir olduruyordu.
        // Bekleme suresi de duruyor (cift tiklamayi yutar).
        const now = wclock();           // dinamik ozellige yazilir -> kalici saat
        const cd = player.getDynamicProperty("stnt:au_cd") || 0;
        if (now < cd) { try { player.onScreenDisplay.setActionBar("§7Rapor hazırlanıyor... birazdan"); } catch (e) {} break; }
        const hs = player.getEntitiesFromViewDirection({ maxDistance: 30 });
        if (hs.length) {
          try { hs[0].entity.applyDamage(1000); } catch (e) {}
          try { player.setDynamicProperty("stnt:au_cd", now + 60); } catch (e) {}
          consumeHeld(player, "stnt:among_us_report");
        } else { try { player.onScreenDisplay.setActionBar("§7Bir canlıya bak — Among Us raporu onu yener"); } catch (e) {} }
        break;
      }
      case "poison_area": {
        for (const e of dim.getEntities({ location: player.location, maxDistance: a.radius })) {
          if (e.id === player.id) continue;
          try { e.addEffect("poison", 200, { amplifier: 1 }); e.addEffect("nausea", 200, { amplifier: 0 }); } catch (err) {}
        }
        spray(dim, player.location, "minecraft:mobspell_emitter", 30, 3);
        break;
      }
      case "tunnel": {
        const v = player.getViewDirection(), s = player.getHeadLocation();
        for (let i = 1; i <= a.length; i++) {
          for (let dx = -a.size; dx <= a.size; dx++) for (let dy = -a.size; dy <= a.size; dy++) {
            const p = { x: Math.floor(s.x + v.x * i + dx), y: Math.floor(s.y + v.y * i + dy), z: Math.floor(s.z + v.z * i) };
            try {
              const b = dim.getBlock(p);
              if (b && b.typeId !== "minecraft:air" && b.typeId !== "minecraft:bedrock") b.setType("minecraft:air");
            } catch (e) {}
          }
        }
        break;
      }
      case "teleport": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 48 });
        if (hit) {
          try { player.teleport({ x: hit.block.location.x + 0.5, y: hit.block.location.y + 1, z: hit.block.location.z + 0.5 }); } catch (e) {}
        }
        if (a.give) { try { player.runCommand(`give @s ${a.give} 1`); } catch (e) {} }
        break;
      }
      case "frisbee": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 40 });
        if (hit) {
          const cc = hit.block.location;
          const offs = [[0,0,0],[1,0,0],[-1,0,0],[0,0,1],[0,0,-1],[2,0,0],[-2,0,0],[0,0,2],[0,0,-2],[0,1,0],[0,-1,0]];
          for (const [dx, dy, dz] of offs) {
            try {
              const b = dim.getBlock({ x: cc.x + dx, y: cc.y + dy, z: cc.z + dz });
              if (b && b.typeId !== "minecraft:air" && b.typeId !== "minecraft:bedrock") b.setType("minecraft:air");
            } catch (e) {}
          }
        }
        break;
      }
      case "fill_axe": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 30 });
        if (!hit) break;
        const l = hit.block.location, key = "stnt:axe_" + player.id;
        const saved = world.getDynamicProperty(key);
        if (typeof saved !== "string") {
          // ILK nokta + DOLDURMA BLOGU = tiklanan blogun tipi. Hangi blokla
          // doldurmak istiyorsan ilk noktayi o bloga tikla (tas/tahta/cam...).
          // Boyut (dim.id) de saklanir ki iki nokta farkli boyutta olamasin.
          world.setDynamicProperty(key,
            `${Math.floor(l.x)},${Math.floor(l.y)},${Math.floor(l.z)},${hit.block.typeId},${dim.id}`);
          const bn = hit.block.typeId.replace("minecraft:", "");
          try { player.onScreenDisplay.setActionBar(`§eİlk nokta — §b${bn}§e ile doldurulacak. İkinci noktaya tıkla`); } catch (e) {}
        } else {
          world.setDynamicProperty(key, undefined);
          const parts = saved.split(",");
          if (parts[4] && parts[4] !== dim.id) {       // iki nokta farkli boyutta -> iptal
            try { player.onScreenDisplay.setActionBar("§cİki nokta farklı boyutta olamaz — baştan başla"); } catch (e) {}
            break;
          }
          const x1 = Number(parts[0]), y1 = Number(parts[1]), z1 = Number(parts[2]);
          const fill = parts[3] || "minecraft:stone";   // ilk noktadaki blok tipi
          const x2 = Math.floor(l.x), y2 = Math.floor(l.y), z2 = Math.floor(l.z);
          const vol = (Math.abs(x1 - x2) + 1) * (Math.abs(y1 - y2) + 1) * (Math.abs(z1 - z2) + 1);
          if (vol > CRAFT_AXE_MAX) {
            try { player.onScreenDisplay.setActionBar(
              `§cAlan çok büyük: §f${vol}§c blok. En fazla §f${CRAFT_AXE_MAX}§c (yaklaşık 27×27×27).`); } catch (e) {}
            break;
          }
          // Is TICK'E BOLUNUR. Paketteki butun toplu blok islerinin deseni bu;
          // Craft Axe tek istisnaydi ve tek callback icinde 20 000 getBlock'a
          // kadar cikabiliyordu. Tavan yalnizca KONAN blogu sayiyordu, taranan
          // konumu degil: masif tasin icini doldurmaya calisan cocuk hicbir
          // blok koymadan 20 000 konum tariyor ve tablet takiliyordu.
          // (Ayni hata daha once mega agacta ve TNT'lerde de yasandi; oradaki
          // yorumlar "konan degil, BAKILAN konumu say" diyor.)
          const bx0 = Math.min(x1, x2), bx1 = Math.max(x1, x2);
          const by0 = Math.min(y1, y2), by1 = Math.max(y1, y2);
          const bz0 = Math.min(z1, z2), bz1 = Math.max(z1, z2);
          let fx = bx0, placed = 0;
          const fjob = system.runInterval(() => {
            let d = 0;
            // Butce her UC seviyede de bakilir. Yalnizca while basliginda
            // bakmak duz secimlerde ise yaramiyordu: 1 genis bir duvarda tek
            // x-dilimi butun secim demek, yani 20 000 getBlock tek tick'te.
            while (fx <= bx1 && d < CRAFT_AXE_PER_TICK && placed < 4096) {
              for (let y = by0; y <= by1 && placed < 4096 && d < CRAFT_AXE_PER_TICK; y++)
                for (let z = bz0; z <= bz1 && placed < 4096 && d < CRAFT_AXE_PER_TICK; z++) {
                  d++;                                  // BAKILAN konum sayilir
                  try {
                    const b = dim.getBlock({ x: fx, y, z });
                    if (b && b.typeId === "minecraft:air") { b.setType(fill); placed++; }
                  } catch (e) {}
                }
              fx++;
            }
            if (fx > bx1 || placed >= 4096) {
              system.clearRun(fjob);
              try { player.onScreenDisplay.setActionBar(`§a${placed} blok dolduruldu (§b${fill.replace("minecraft:", "")}§a)`); } catch (e) {}
            }
          }, 1);
        }
        break;
      }
    }
  } catch (e) {
    // Sessiz yutmak, "cocuk esyayi yanlis tutuyor" ile "esya bozuk"u ayirt
    // edilemez yapiyordu — bu yuzden hicbir zaman bildirilmiyordu.
    console.warn(`[SuperTNT] esya hatasi ${a && a.type}: ${e}`);
    try { player.onScreenDisplay.setActionBar("§cBu eşya şu an çalışmadı"); } catch (err) {}
  }
}

// Pasif itemlar: elde Lav Kristali -> ates korumasi; ayakta Gokkusagi Botu ->
// yun izi. selectedSlotIndex/equippable API'leri surume duyarli, hepsi korumali.
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    try {
      const inv = p.getComponent("minecraft:inventory") && p.getComponent("minecraft:inventory").container;
      let held = null;
      try { held = inv && inv.getItem(p.selectedSlotIndex); } catch (e) {}
      if (held && held.typeId === "stnt:lava_crystal") {
        p.addEffect("fire_resistance", 40, { amplifier: 0, showParticles: false });
      }
      let feet = null;
      try {
        const eq = p.getComponent("minecraft:equippable");
        feet = eq && eq.getEquipment && eq.getEquipment("Feet");
      } catch (e) {}
      if (feet && feet.typeId === "stnt:rainbow_boots") {
        const below = { x: Math.floor(p.location.x), y: Math.floor(p.location.y) - 1, z: Math.floor(p.location.z) };
        const b = p.dimension.getBlock(below);
        if (b && b.typeId === "minecraft:air") {
          const cols = ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple", "pink"];
          b.setType("minecraft:" + cols[Math.floor(Math.random() * cols.length)] + "_wool");
        }
      }
    } catch (e) {}
  }
}, 10);

// ---------------------------------------------------------------- sahte aletler
// Sahte Elmas Aletler: elmas gorunur, elmas gibi kazar — ILK kullanista tahtaya
// doner. Iki tetikleyici var, cunku "kullanmak" iki sey demek: bir canliya
// VURMAK ve bir blok KIRMAK.
const FAKE_TOOLS = __FAKE_TOOLS__;    // "stnt:sahte_elmas_kilic" -> "minecraft:wooden_sword"
const FAKE_NAMES = __FAKE_NAMES__;    // ayni anahtar -> tahta aletin Turkce adi

// Elindeki alet sahteyse tahtaya cevirir.
//
// Degistirme BIR TICK ERTELENIR. Bunlar after-event: oyun vurus/kirma sonrasi
// elindeki esyaya dayaniklilik hasarini YAZIYOR, ve ayni tick'te slotu
// degistirirsek o yazma bizim koydugumuz tahta aleti ezebilir. Ertelemenin
// bedeli, arada oyuncunun slot degistirmis olabilmesi — o yuzden ikinci
// tick'te slot yeniden okunur ve hala sahte bir alet degilse dokunulmaz.
function revealFake(player) {
  if (!player || player.typeId !== "minecraft:player") return;
  let held;
  try {
    const con = player.getComponent("minecraft:inventory")?.container;
    held = con && con.getItem(player.selectedSlotIndex);
  } catch (e) { return; }
  if (!held || !FAKE_TOOLS[held.typeId]) return;
  const wood = FAKE_TOOLS[held.typeId];
  const adi = FAKE_NAMES[held.typeId];
  system.run(() => {
    try {
      const con = player.getComponent("minecraft:inventory")?.container;
      if (!con) return;
      const slot = player.selectedSlotIndex;
      const now = con.getItem(slot);
      if (!now || !FAKE_TOOLS[now.typeId]) return;      // arada el degistiyse birak
      con.setItem(slot, new ItemStack(wood, 1));
      spray(player.dimension, player.location, "minecraft:basic_smoke_particle", 12, 0.7);
      try { player.dimension.playSound("random.break", player.location, { volume: 1.0 }); } catch (e) {}
      player.onScreenDisplay.setActionBar(`§cSahte! §fElmas sandın — §6${adi}§f çıktı.`);
    } catch (e) {}
  });
}
world.afterEvents.entityHitEntity.subscribe((ev) => revealFake(ev.damagingEntity));
world.afterEvents.playerBreakBlock.subscribe((ev) => revealFake(ev.player));

// Silahlar: Kanli Kilic -> kirmizi parcacik; Kalp Baltasi -> agir hasar.
world.afterEvents.entityHurt.subscribe((ev) => {
  try {
    const src = ev.damageSource && ev.damageSource.damagingEntity;
    if (!src || src.typeId !== "minecraft:player") return;
    const inv = src.getComponent("minecraft:inventory") && src.getComponent("minecraft:inventory").container;
    let held = null;
    try { held = inv && inv.getItem(src.selectedSlotIndex); } catch (e) {}
    if (!held) return;
    if (held.typeId === "stnt:blood_sword") {
      spray(ev.hurtEntity.dimension, ev.hurtEntity.location, "minecraft:redstone_ore_dust_particle", 15, 1);
    } else if (held.typeId === "stnt:heart_axe") {
      try {
        // Oyuncuya EK hasar YOK: taban 7 zaten agir. Mob tek vurusta olur.
        if (ev.hurtEntity.typeId !== "minecraft:player") ev.hurtEntity.applyDamage(1000);
      } catch (e) {}
    }
  } catch (e) {}
});

// ---------------------------------------------------------------- mob takimi
// Takim Asasi ile bir mob'a dokun -> mob senin takim etiketini alir. Ayni
// takimdakiler birbirini OLDURMEZ (asagida afterEvents.entityHurt ile hasar
// geri iyilestirilir; script'ten mob AI hedefi degistirilemedigi icin dostlugu
// boyle sagliyoruz). Etiket save/reload'da mob'da kalir.
const TEAM_PREFIX = "stnt_team:";
function teamOf(e) {
  try { return e.getTags().find((t) => t.startsWith(TEAM_PREFIX)); } catch (x) { return undefined; }
}
function recruitToTeam(player, target) {
  if (!target || target.typeId === "minecraft:player") return;
  const tag = TEAM_PREFIX + player.id;
  if (target.hasTag(tag)) return;                   // zaten bu takimda
  for (const t of target.getTags()) if (t.startsWith(TEAM_PREFIX)) target.removeTag(t);
  target.addTag(tag);
  try { player.onScreenDisplay.setActionBar("§aMob takımına katıldı! Aynı takım birbirini öldürmez."); } catch (e) {}
  try { spray(target.dimension, target.location, "minecraft:heart_particle", 10, 1.2); } catch (e) {}
}
// (1) Interact hareketi (PC sag tik, mobilde uysal moba dokunma).
world.beforeEvents.playerInteractWithEntity.subscribe((ev) => {
  try {
    if (!ev.itemStack || ev.itemStack.typeId !== "stnt:takim_asasi") return;
    recruitToTeam(ev.player, ev.target);
  } catch (e) {}
});
// (2) VURMA hareketi. Mobilde dusman moba dokunmak SALDIRIDIR (interact hic
// tetiklenmez) -> Takim Asasi ile vurunca da takima katsin. Donusum Asasi ile
// ayni "mobil odak" cozumu; onsuz tablette hicbir dusman moba takim kurulamaz.
world.afterEvents.entityHitEntity.subscribe((ev) => {
  try {
    const pl = ev.damagingEntity;
    if (!pl || pl.typeId !== "minecraft:player") return;
    const eq = pl.getComponent("equippable");
    const held = eq && eq.getEquipment(EquipmentSlot.Mainhand);
    if (!held || held.typeId !== "stnt:takim_asasi") return;
    recruitToTeam(pl, ev.hitEntity);
  } catch (e) {}
});
// Donusum Asasi: bir moba dokun -> o mob'un gorunumune don. before-event'te
// triggerEvent calismaz (read-only), system.run ile ertele.
// ---- Takma ad. nameTag DIGER oyuncularin tepende gordugu yazidir; sohbetteki
// ad ile oyuncu listesi degismez, onlari betik degistiremez.
// Kayit oyuncunun kendi dinamik ozelliginde: kalicidir ve donusum dongusu her
// turda buradan okur (bkz. asagidaki wantTag) — yoksa dongu bes tick sonra
// gercek ismi geri yazar ve takma ad bir anda kaybolurdu.
const NICK_PROP = "stnt:nick";
const NICK_COLORS = [
  ["Beyaz", "§f"], ["Sarı", "§e"], ["Turuncu", "§6"], ["Kırmızı", "§c"],
  ["Yeşil", "§a"], ["Camgöbeği", "§b"], ["Mor", "§d"], ["Gri", "§7"],
];
function nickOf(pl) {
  try {
    const v = pl.getDynamicProperty(NICK_PROP);
    return typeof v === "string" && v ? v : null;
  } catch (e) { return null; }
}
function renamePrompt(pl, a, tries) {
  const form = new ModalFormData()
    .title("İsim Değiştirme")
    .textField("Yeni ismin (boş bırak = gerçek ismin):", pl.name)
    .dropdown("Renk:", NICK_COLORS.map((c) => c[0]), 0);
  form.show(pl).then((r) => {
    if (r.canceled) {
      // Parmak hala ekrandayken form "UserBusy" doner — kisa araliklarla tekrar
      // dene, yoksa esya ilk dokunusta olu gorunur (menu asasindaki ayni tuzak).
      if (r.cancelationReason === "UserBusy" && tries > 0) {
        system.runTimeout(() => renamePrompt(pl, a, tries - 1), 10);
      }
      return;
    }
    if (!r.formValues) return;
    // Satir sonu ve bosluk temizligi: cok satirli bir ad etiketi bozar.
    let raw = String(r.formValues[0] || "").replace(/[\r\n\t]/g, " ").trim();
    if (raw.length > a.maxLen) raw = raw.slice(0, a.maxLen);
    try {
      if (!raw) {
        pl.setDynamicProperty(NICK_PROP, undefined);
        pl.nameTag = pl.name;
        pl.onScreenDisplay.setActionBar("§7Gerçek ismine döndün: §f" + pl.name);
        return;
      }
      const ci = Number(r.formValues[1]) || 0;
      const col = (NICK_COLORS[ci] || NICK_COLORS[0])[1];
      const nick = col + raw;
      pl.setDynamicProperty(NICK_PROP, nick);
      // Donusmusken etiket gizli kalir; dongu insana donunce yeni adi yazar.
      let m = 0;
      try { m = pl.getProperty("st:morph") || 0; } catch (e) {}
      if (!m) pl.nameTag = nick;
      pl.onScreenDisplay.setActionBar(
        m ? "§aİsmin ayarlandı: " + nick + " §7(dönüşükken gizli)" : "§aArtık ismin: " + nick);
    } catch (e) {}
  }).catch(() => {});
}

// ---- BLOK KILIGI. Blok KIRINCA o bloga donusulur (asagidaki playerBreakBlock
// kancasi); blokken BASILI TUTUNCA bakilan yere "yerlesilir"; GOKYUZUNE bakip
// basili tutunca insana donulur. Uc hareket de ayri, ikisi karismiyor.
// Hangi blok kiligindasin: st:morph OZELLIGINDEN turetilir, ayri bir tabloda
// TUTULMAZ. Once bellekte bir Map vardi ve dunya kapanip acilinca bosaliyordu:
// cocuk hala blok gorunuyordu ama "yerles" ve "gokyuzune bak, insana don"
// hareketlerinin ikisi de sessizce hicbir sey yapmiyordu. st:morph zaten
// kalici; tek dogru kaynak o olsun.
const BLOCK_MORPH_BY_N = __BLOCK_MORPH_BY_N__;   // st:morph sayisi -> olay adi
function isBlockMorph(pl) {
  let m = 0;
  try { m = pl.getProperty("st:morph") || 0; } catch (e) {}
  return BLOCK_MORPH_BY_N[m] || BLOCK_MORPH_BY_N[String(m)] || null;
}
function blockMorphUse(pl) {
  if (!isBlockMorph(pl)) {
    try { pl.onScreenDisplay.setActionBar("§7Bir blok kır — o bloğa dönüşürsün"); } catch (e) {}
    return;
  }
  const hit = pl.getBlockFromViewDirection({ maxDistance: 8 });
  if (!hit) {
    // Gokyuzune / bosluga bakiyor: insana don. Cikis yolunun HER ZAMAN
    // ulasilabilir olmasi sart — kapali bir odada bile tavana bakip cikabilsin
    // diye "blok yok" kosulu secildi, comelme degil (comelme morph yeteneklerini
    // tetikliyor).
    morphTo(pl, "st:morph_human", "İnsana geri döndün");
    return;
  }
  // Bakilan blogun USTUNE, kare ortasina otur: blok izgarasiyla hizalaninca
  // gercekten konmus bir blok gibi gorunur.
  const b = hit.block.location;
  try {
    pl.teleport({ x: Math.floor(b.x) + 0.5, y: Math.floor(b.y) + 1, z: Math.floor(b.z) + 0.5 });
    pl.onScreenDisplay.setActionBar("§aYerleştin! §7Gökyüzüne bakıp basılı tut → insan");
    spray(pl.dimension, pl.location, "minecraft:basic_smoke_particle", 12, 0.4);
  } catch (e) {}
}

// Blok KIRINCA o bloga donus. Elde Blok Kiligi olmali; olay ustundeki esya
// alani surumler arasinda oynadigi icin once o, olmazsa ANA ELDEKI esya.
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try {
    const pl = ev.player;
    if (!pl) return;
    let held = ev.itemStackBeforeBreak && ev.itemStackBeforeBreak.typeId;
    if (!held) {
      const eq = pl.getComponent("minecraft:equippable");
      const it = eq && eq.getEquipment(EquipmentSlot.Mainhand);
      held = it && it.typeId;
    }
    if (held !== "stnt:blok_kiligi") return;
    const bid = (ev.brokenBlockPermutation && ev.brokenBlockPermutation.type &&
                 ev.brokenBlockPermutation.type.id) || null;
    const evName = bid && BLOCK_MORPH_MAP[bid];
    if (!evName) {
      system.run(() => {
        try { pl.onScreenDisplay.setActionBar(
          `§7Bu bloğun kılığı yok: §f${(bid || "?").replace("minecraft:", "")}`); } catch (e) {}
      });
      return;
    }
    // Kirma olayi sirasinda triggerEvent salt-okunur baglamda kalabilir.
    system.run(() => morphTo(pl, evName, "Bloğa dönüştün!"));
  } catch (e) {}
});

function morphTo(pl, evName, msg) {
  try {
    pl.triggerEvent(evName);
    pl.onScreenDisplay.setActionBar("§a" + (MORPH_HINT[evName] || msg));
    spray(pl.dimension, pl.location, "minecraft:mob_portal", 20, 1.5);
  } catch (e) {}
}
function tryMorph(pl, target) {
  const evName = target ? MORPH_MAP[target.typeId] : undefined;
  if (!evName) {
    system.run(() => { try { pl.onScreenDisplay.setActionBar("§7Bu yaratığa dönüşülemiyor — asayı boşluğa tıkla, listeden seç"); } catch (e) {} });
    return;
  }
  system.run(() => morphTo(pl, evName, "Dönüştün! Boşluğa sağ tık → liste."));
}

// Donusum MENUSU. Mob'a dokunmak tek basina yetmiyor: ejderha, wither, deniz
// mob'lari ve dogal ortaminda bulunmasi zor olanlara elle ulasilamiyor. Asayi
// bosluga tiklayinca kategorili liste acilir; her mob buradan secilebilir.
// show() oyuncu tiki hala basiliyken "UserBusy" doner -> kisa araliklarla tekrar.
function showForm(pl, form, onPick, tries) {
  form.show(pl).then((r) => {
    if (r.canceled) {
      if (r.cancelationReason === "UserBusy" && tries > 0) {
        system.runTimeout(() => showForm(pl, form, onPick, tries - 1), 10);
      }
      return;
    }
    onPick(r.selection);
  }).catch(() => {});
}
function morphMenu(pl) {
  const f = new ActionFormData().title("Dönüşüm").body("Ne olmak istersin?");
  f.button("§aİnsana dön");
  for (const g of MORPH_FORMS) f.button(`${g.cat} (${g.items.length})`);
  showForm(pl, f, (i) => {
    if (i === 0) { morphTo(pl, "st:morph_human", "İnsana geri döndün"); return; }
    const g = MORPH_FORMS[i - 1];
    if (!g) return;
    const sub = new ActionFormData().title(g.cat).body("Bir yaratık seç:");
    sub.button("§7◀ Geri");
    for (const m of g.items) sub.button(m.name);
    showForm(pl, sub, (j) => {
      if (j === 0) { morphMenu(pl); return; }
      const m = g.items[j - 1];
      if (m) morphTo(pl, m.ev, m.name + " oldun!");
    }, 20);
  }, 20);
}
// (1) Interact/Use hareketi (PC sag tik, mobilde uysal mob'a uzun dokunma).
world.beforeEvents.playerInteractWithEntity.subscribe((ev) => {
  try {
    if (!ev.itemStack || ev.itemStack.typeId !== "stnt:donusum_asasi") return;
    tryMorph(ev.player, ev.target);
  } catch (e) {}
});
// (2) VURMA hareketi. Mobilde moba dokunmak saldiridir (interact degil) ve
// dusman mob'larda interact hic tetiklenmez. Bu yuzden Donusum Asasi ile
// vurunca da morph olur -> asıl "mobil odak" senaryosu.
world.afterEvents.entityHitEntity.subscribe((ev) => {
  try {
    const pl = ev.damagingEntity;
    if (!pl || pl.typeId !== "minecraft:player") return;
    const eq = pl.getComponent("equippable");
    const held = eq && eq.getEquipment(EquipmentSlot.Mainhand);
    if (!held || held.typeId !== "stnt:donusum_asasi") return;
    tryMorph(pl, ev.hitEntity);
  } catch (e) {}
});
// ---- Morph YETENEKLERI: bir mob'a donunce o mob'un gucunu kazan.
// Pasif olanlar surekli yenilenir; aktif olanlar comelme (sneak) ile tetiklenir.
// Tablo build.py'de MORPHS'tan uretilir; yetenekler st:morph SAYISINA elle
// yazilmaz — mob listesi buyudukce sayilar kayar ve yanlis moba yanlis guc
// baglanirdi.
const morphCd = new Map();
system.runInterval(() => {
  const now = system.currentTick;
  for (const p of world.getPlayers()) {
    let m;
    try { m = p.getProperty("st:morph"); } catch (e) { continue; }
    // Kendini gizlemenin yarisi isim etiketi: mob gibi gorunup tepende adin
    // yazarsa oyun biter. Donunce geri konur; her turda karsilastirilir, yani
    // oyuncu donusmusken cikip girse de kendini toparlar.
    const wantTag = (typeof m === "number" && m !== 0) ? "" : (nickOf(p) || p.name);
    try { if (p.nameTag !== wantTag) p.nameTag = wantTag; } catch (e) {}
    if (typeof m !== "number" || m === 0) continue;
    const ab = MORPH_ABIL[m];
    if (!ab) continue;
    if (ab.pas) {
      try { p.addEffect(ab.pas[0], 20, { amplifier: ab.pas[1], showParticles: false }); } catch (e) {}
    }
    if (ab.act && p.isSneaking && (morphCd.get(p.id) || 0) <= now) {
      try { morphCd.set(p.id, now + morphAct(p, ab.act, ab.pow || {})); } catch (e) {}
    }
  }
}, 5);

// Aktif morph yetenegi. Donen deger = bir sonraki kullanima kadar bekleme (tick).
// pw = MORPH_ABIL[n].pow — paketin KENDI boss'lari vanilla esinden daha sert
// vurur (Dev Creeper blok kirar, Mutant Warden 34 blok tarar). Vanilla morph'lar
// pw'yi bos gecer, asagidaki varsayilanlara duser: eski denge aynen korunur.
function morphAct(p, act, pw) {
  const dim = p.dimension;
  if (act === "explode") {                    // creeper: comel -> patla
    const l = p.location;
    // KENDI patlamandan olme. Patlama ayagin dibinde olusuyor: vanilla TNT gucu
    // 4 ve sifir mesafede zirhsiz oyuncuyu oldururken burada guc 3 ile 6. Iki
    // katman, cunku tek basina hicbiri kesin degil:
    //   1) Direnc V (amplifier 4) hasari buyuk olcude keser — ama surumler
    //      arasinda %80 mi %100 mu kestigine guvenmiyoruz.
    //   2) Patlamadan onceki can iki tick sonra geri konur. Direnc oldurmeyecek
    //      kadar kesmisse bu ikinci katman kalani tamamlar.
    // Tek basina (2) yetmez: oyuncu O TICK'te olurse geri koyacak can kalmaz.
    try { p.addEffect("resistance", 60, { amplifier: 4, showParticles: false }); } catch (e) {}
    let hp = null;
    try { const h = p.getComponent("minecraft:health"); hp = h ? h.currentValue : null; } catch (e) {}
    dim.createExplosion({ x: l.x, y: l.y + 0.5, z: l.z }, pw.r || 3,
                        { breaksBlocks: !!pw.breaks, causesFire: false });
    if (hp !== null) {
      system.runTimeout(() => {
        try {
          const h = p.getComponent("minecraft:health");
          if (h && h.currentValue < hp) h.setCurrentValue(hp);
        } catch (e) {}
      }, 2);
    }
    spray(dim, l, "minecraft:large_explosion", 1, 0);
    return pw.cd || 80;
  }
  if (act === "teleport") {                   // enderman: comel -> baktigin yere isinlan
    const hit = p.getBlockFromViewDirection({ maxDistance: pw.range || 48 });
    if (!hit) {
      // Gokyuzune ya da menzilden uzaga bakinca isin hicbir seye carpmiyor.
      // Eskiden sessizce hicbir sey olmuyor ve bekleme de yaniyordu: cocuk
      // yetenegin bozuk oldugunu saniyordu. Havaya isinlanmak da COZUM DEGIL,
      // dusme hasari verir. Soyle ve beklemeyi yakma.
      try { p.onScreenDisplay.setActionBar("§7Işınlanmak için bir yere bak"); } catch (e) {}
      return 10;
    }
    p.teleport({ x: hit.block.location.x + 0.5, y: hit.block.location.y + 1, z: hit.block.location.z + 0.5 });
    spray(dim, p.location, "minecraft:mob_portal", 20, 1);
    return pw.cd || 30;
  }
  if (act === "fireball") {                   // ghast/blaze: comel -> baktigin yere ates topu
    const hit = p.getBlockFromViewDirection({ maxDistance: 40 });
    const v = p.getViewDirection(), s = p.getHeadLocation();
    const t = hit ? hit.block.location : { x: s.x + v.x * 20, y: s.y + v.y * 20, z: s.z + v.z * 20 };
    dim.createExplosion({ x: t.x + 0.5, y: t.y + 0.5, z: t.z + 0.5 }, 3, { breaksBlocks: false, causesFire: true });
    return pw.cd || 60;
  }
  if (act === "sonic") {                      // warden: comel -> ses saldirisi
    sonicBoom(p, { range: pw.range || 24, damage: pw.damage || 14, knock: pw.knock || 1.8 });
    return pw.cd || 100;
  }
  if (act === "smash") {                      // Dev Zombi: comel -> yer sarsintisi
    const l = p.location, r = pw.r || 6;
    try { dim.playSound("mob.ravager.stun", l, { volume: 1.2 }); } catch (e) {}
    spray(dim, l, "minecraft:smash_ground_particle", 40, r * 0.6);
    let ents = [];
    try { ents = dim.getEntities({ location: l, maxDistance: r }); } catch (e) {}
    for (const e of ents) {
      if (e.id === p.id || SONIC_SKIP[e.typeId]) continue;   // esya/tecrube savrulmasin
      const q = e.location;
      const ax = q.x - l.x, az = q.z - l.z;
      const len = Math.sqrt(ax * ax + az * az) || 1;
      try { e.applyDamage(pw.dmg || 8, { cause: "entityAttack", damagingEntity: p }); }
      catch (err) { try { e.applyDamage(pw.dmg || 8); } catch (err2) {} }
      try { e.applyKnockback(ax / len, az / len, pw.knock || 1.4, 0.7); }
      catch (err) { try { e.applyImpulse({ x: ax / len, y: 0.4, z: az / len }); } catch (err2) {} }
    }
    return pw.cd || 100;
  }
  if (act === "float") {                      // ucan mob'lar: comel -> yukari suzul
    p.applyKnockback(0, 0, 0, 1.0);
    return pw.cd || 20;
  }
  return 20;
}


// KRITIK: world.beforeEvents.entityHurt @minecraft/server 1.x'te YOKTUR (2.x'te
// eklendi). Manifest 1.14.0'a bagli oldugundan eski hali (beforeEvents...cancel)
// modul yuklenirken TypeError atip main.js'i BU SATIRDAN SONRA komple
// durduruyordu -> portal isinlanmasi, cakmakla atesleme, redstone, patlama
// zinciri HIC kaydolmuyordu (portal "sıradan blok" gibiydi). 1.x'te var olan
// afterEvents.entityHurt ile hasari GERI IYILESTIR (iptal edemeyiz). IKI durum:
//   (1) ayni takimdan iki mob birbirini vurursa -> olduremezler.
//   (2) takima aldigin mob SENI (sahibini) vurursa -> "dost" olsun diye hasar
//       geri verilir; mob AI'si degistirilemiyor (1.x) ama sana zarar veremez.
// try ile sarili — asla akisi durdurmaz.
world.afterEvents.entityHurt.subscribe((ev) => {
  try {
    const attacker = ev.damageSource && ev.damageSource.damagingEntity;
    if (!attacker) return;                          // cevre hasari: dokunma
    const at = teamOf(attacker);
    if (!at) return;                                 // saldirgan takimsiz -> normal
    const victim = ev.hurtEntity;
    const ownerId = at.slice(TEAM_PREFIX.length);
    const sameTeam = teamOf(victim) === at;
    const ownerHit = victim.typeId === "minecraft:player" && victim.id === ownerId;
    if (!sameTeam && !ownerHit) return;              // yabanciya normal hasar
    const h = victim.getComponent("minecraft:health");
    if (h && typeof h.setCurrentValue === "function") {
      h.setCurrentValue((h.currentValue || 0) + (ev.damage || 0));
    }
  } catch (e) {}
});

// ---------------------------------------------------------------- tooltip
// Bedrock'ta Java'daki gibi blok tooltip'i yok. Java surumunde her TNT'nin
// ne yaptigini anlatan bir aciklama var ve cocuklarin hangi TNT'nin ne
// yaptigini bilmesi buna bagli. Yerine: TNT elde secilince aciklama
// eylem cubugunda (action bar) gosteriliyor.
const lastSel = new Map();   // playerId -> gosterilen id
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    let held = null;
    try {
      const inv = p.getComponent("minecraft:inventory");
      const it = inv?.container?.getItem(p.selectedSlotIndex);
      held = it ? it.typeId : null;
    } catch (e) { continue; }
    if (lastSel.get(p.id) === held) continue;
    lastSel.set(p.id, held);
    if (held && held.startsWith("stnt:")) {
      const s = held.slice(5);
      if (!TIPS[s]) continue;
      // Ham metin yerine ceviri anahtari: oyuncunun dilinde gorunur ve
      // texts/*.lang'daki anahtarlar olu kalmaz.
      try {
        p.onScreenDisplay.setActionBar({
          rawtext: [
            { text: "§e" }, { translate: `tile.stnt:${s}.name` },
            { text: "§r §7- " }, { translate: `stnt.tip.${s}` },
          ],
        });
      } catch (e) {
        try { p.onScreenDisplay.setActionBar(`§e${NAMES[s]}§r §7- ${TIPS[s]}`); } catch (e2) {}
      }
    }
  }
}, 5);

// ---------------------------------------------------------------- yerlestirilen
// bloklarin kaydi. Redstone yoklamasi sadece bu listedeki bloklara bakar,
// boylece maliyet oyuncunun koydugu TNT sayisiyla sinirli kalir.
// Dunya dinamik ozelliginde saklanir -> dunya yeniden acilinca kaybolmaz.
const TRACK_PROP = "stnt:placed";
const TRACK_MAX = 400;
let tracked = new Set();

function loadTracked() {
  try {
    const raw = world.getDynamicProperty(TRACK_PROP);
    if (typeof raw === "string") tracked = new Set(JSON.parse(raw));
  } catch (e) { tracked = new Set(); }
}
function saveTracked() {
  try {
    world.setDynamicProperty(TRACK_PROP, JSON.stringify([...tracked].slice(-TRACK_MAX)));
  } catch (e) {}
}
function track(dimId, loc, owner) {
  // konum + koyan oyuncu. Kontrol Kumandasi sadece KENDI koydugunu atesler.
  tracked.add(`${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}|${owner || ""}`);
  if (tracked.size > TRACK_MAX) tracked = new Set([...tracked].slice(-TRACK_MAX));
  saveTracked();
}
function ownerOf(dimId, loc) {
  // Blogu KIM koydu? tracked anahtari "dim|x,y,z|owner" bicimindedir. ignite'a
  // igniterId gecmeyen yollar (redstone, zincir, baska patlama) icin tek
  // dogru kaynak burasi. untrack SILMEDEN once cagrilmali.
  const pre = `${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}|`;
  for (const k of tracked) if (k.startsWith(pre)) return k.slice(pre.length) || undefined;
  return undefined;
}
function untrack(dimId, loc) {
  const pre = `${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`;
  for (const k of [...tracked]) if (k === pre || k.startsWith(pre + "|")) tracked.delete(k);
  saveTracked();
}

world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (b && b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
    track(b.dimension.id, b.location, ev.player?.id);
  }
});

// ---------------------------------------------------------------- atesleme
world.afterEvents.playerInteractWithBlock.subscribe((ev) => {
  const { block, itemStack, player } = ev;
  if (!block || !itemStack) return;
  if (itemStack.typeId !== "minecraft:flint_and_steel") return;
  const id = block.typeId;
  if (!id.startsWith("stnt:")) return;
  const short = id.slice(5);
  if (!SPEC[short]) return;
  ignite(block.dimension, block.location, short, player.id);
  try { player.sendMessage(`§e${NAMES[short] ?? short} §7ateslendi!`); } catch (e) {}
});

// F2 - redstone. Kayitli bloklari 10 tick'te bir yoklar.
system.runInterval(() => {
  if (tracked.size === 0) return;
  for (const k of [...tracked]) {
    try {
      const [dimId, xyz] = k.split("|");
      const [x, y, z] = xyz.split(",").map(Number);
      const dim = world.getDimension(dimId);
      const b = dim.getBlock({ x, y, z });
      if (!b) continue;                       // yuklu degil, sonraki tura birak
      if (!b.typeId.startsWith("stnt:")) { tracked.delete(k); saveTracked(); continue; }
      if ((b.getRedstonePower() ?? 0) > 0) ignite(dim, b.location, b.typeId.slice(5));
    } catch (e) {}
  }
}, 10);

// F3 - baska bir patlama bizim blogu vuruyorsa yok etme, atesle.
try {
  world.beforeEvents.explosion.subscribe((ev) => {
    const dim = ev.dimension;
    const keep = [];
    let lit = 0;
    for (const b of ev.getImpactedBlocks()) {
      try {
        if (b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
          const loc = { x: b.location.x, y: b.location.y, z: b.location.z };
          const short = b.typeId.slice(5);
          system.run(() => ignite(dim, loc, short));
          lit++;
          continue;                            // patlamanin yok etmesini engelle
        }
      } catch (e) {}
      keep.push(b);
    }
    if (lit > 0) ev.setImpactedBlocks(keep);
  });
} catch (e) {
  console.warn("[SuperTNT] explosion olayi yok, zincirleme sadece kendi TNT'lerimizde");
}

// ---------------------------------------------------------------- fitil
// F1 - blok kaldirilir, yerine fiziksel bir varlik dogar. Firlatilabilir,
// duser, ziplar - vanilla TNT gibi.
const primed = new Map();  // entityId -> { short, left, e, ig, lastLoc, lastDim }

// Fitili TAM BIR KEZ patlat. Hem sayac (p.left<=0) hem de guvenlik zaman
// asimi bunu cagirir; primed'ten silinmis id ikinci kez patlamaz. Varligin
// konumu okunamiyorsa (chunk bosaldi, itildi, silindi) SON BILINEN konumda
// patlar. "Bazi TNT'ler patlamiyor" hatasinin asil kaynagi buydu: eskiden
// konum okunamayinca sessizce siliniyor, hic patlamiyordu.
function fireOnce(id) {
  const p = primed.get(id);
  if (!p) return;                       // zaten patladi/silindi
  primed.delete(id);
  let loc = p.lastLoc, dim = p.lastDim;
  try { loc = p.e.location; dim = p.e.dimension; } catch (e) {}
  try { p.e.remove(); } catch (e) {}
  if (dim && loc) detonate(dim, loc, p.short, p.ig);
}

function ignite(dim, loc, short, igniterId) {
  // Cagiran bilmiyorsa kaydindan bul (redstone / zincir / baska patlama).
  if (!igniterId) igniterId = ownerOf(dim.id, loc);
  try {
    const b = dim.getBlock(loc);
    if (!b || b.typeId !== `stnt:${short}`) return;   // zaten ateslenmis
    b.setType("minecraft:air");
  } catch (e) { return; }
  untrack(dim.id, loc);

  const c = { x: Math.floor(loc.x) + 0.5, y: Math.floor(loc.y) + 0.5, z: Math.floor(loc.z) + 0.5 };
  try { dim.playSound("random.fuse", c, { volume: 1.0 }); } catch (e) {}
  try {
    const e = dim.spawnEntity(`stnt:${short}_primed`, c);
    try { e.applyImpulse({ x: rnd(0.04), y: 0.22, z: rnd(0.04) }); } catch (err) {}
    const pid = e.id;
    primed.set(pid, { short, left: FUSE, e, ig: igniterId, lastLoc: c, lastDim: dim });
    // Kosulsuz guvenlik zaman asimi: sayac herhangi bir sebeple patlatmazsa
    // yine de patlar. Normalde sayac once patlatir, id silinir, bu no-op olur.
    system.runTimeout(() => fireOnce(pid), FUSE + 6);
  } catch (err) {
    // varlik hic dogmadiysa yerinde patlat - islev kaybolmasin
    system.runTimeout(() => detonate(dim, c, short, igniterId), FUSE);
  }
}

// fitil sayaci
system.runInterval(() => {
  for (const [id, p] of [...primed]) {
    p.left -= 2;
    try {
      const loc = p.e.location;
      p.lastLoc = loc; p.lastDim = p.e.dimension;   // son iyi konumu hatirla
      p.lastDim.spawnParticle("minecraft:basic_smoke_particle", { x: loc.x, y: loc.y + 0.7, z: loc.z });
    } catch (e) {}                                    // konum okunamadi -> son konum kalir, SILME YOK
    if (p.left <= 0) fireOnce(id);
  }
}, 2);

// F3 - patlama aninda cevredeki TNT'leri kademeli atesle.
//
// PERFORMANS: kup taramasi tek tick'te yapilirsa (2R+1)^3 blok sorgusu eder.
// R=6'da 2197; 10'lu bir zincirde 22 bin sorgu ve tablette gorunur donma.
// Bu yuzden yaricap 5'e cekildi ve tarama dx dilimlerine bolunup tick'lere
// yayildi: tick basina 2 dilim = ~242 sorgu.
// Es zamanli zincir isi tavani. Her patlama bir 1-tick interval aciyor ve o
// interval 30 TNT daha atesleyebiliyor, her biri yine chain cagiriyor. 30
// TNT'lik bir yigin — cocuklarin normal kullanimi — ~30 es zamanli is demek,
// tick basina ~7000 getBlock. Tavan dolunca zincir o dalda durur; TNT'ler
// kendi fitilleriyle zaten patlar.
const CHAIN_MAX = 8;
let chainJobs = 0;
function chain(dim, c) {
  if (chainJobs >= CHAIN_MAX) return;
  chainJobs++;
  const R = 5;
  const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
  let dx = -R, found = 0;
  const job = system.runInterval(() => {
    for (let slice = 0; slice < 2 && dx <= R; slice++, dx++) {
      for (let dy = -R; dy <= R; dy++) {
        for (let dz = -R; dz <= R; dz++) {
          if (found >= 30) { system.clearRun(job); chainJobs--; return; }
          try {
            const p = { x: cx + dx, y: cy + dy, z: cz + dz };
            const b = dim.getBlock(p);
            if (!b) continue;
            const t = b.typeId;
            if (!t.startsWith("stnt:")) continue;
            const s = t.slice(5);
            if (!SPEC[s]) continue;
            found++;
            system.runTimeout(() => { try { ignite(dim, p, s); } catch (e) {} },
                              2 + Math.floor(Math.random() * 8));
          } catch (e) {}
        }
      }
    }
    if (dx > R) { system.clearRun(job); chainJobs--; }
  }, 1);
}

// Kalici zaman. system.currentTick her dunya yuklemesinde sifirlanir, bu yuzden
// DISKE yazilan hicbir zaman damgasinda kullanilamaz.
function wclock() {
  try { return world.getAbsoluteTime(); } catch (e) { return system.currentTick; }
}

function rnd(n) { return (Math.random() - 0.5) * n; }

// ---- Gecici bloklar (su, buz) dunyaya KAYDEDILIR.
// Geri alma bir system.runTimeout idi: yalnizca bellekte durur. Cocuk Su
// TNT'yi atip 10 saniye dolmadan oyundan cikinca su SONSUZA KADAR kaliyordu;
// Buz TNT'de pencere 30 saniye. Tablette oyundan cikmak siradan bir sey.
//
// Cozum: her yerlestirme icin kucuk bir kayit (merkez + yaricap + blok turu).
// Bu seansta yaratilan kayitlarla runTimeout zaten ilgileniyor, o yuzden
// bellekteki kume onlari supurgeden ayirir. Onceki seanstan kalan kayitlar
// bellekte olmadigi icin, cevre yuklenir yuklenmez suprulur.
const TEMP_PROP = "stnt:temp";
const TEMP_MAX = 24;                     // bkz. TRACK_MAX: kalici ozelligin bayt butcesi var
const tempSupuruluyor = new Set();       // ayni kaydi iki kez supurmemek icin (yalniz bellek)
function tempOku() {
  try { const r = world.getDynamicProperty(TEMP_PROP); return typeof r === "string" ? JSON.parse(r) : []; }
  catch (e) { return []; }
}
function tempYaz(a) {
  try { world.setDynamicProperty(TEMP_PROP, JSON.stringify(a.slice(-TEMP_MAX))); }
  catch (e) { console.warn(`[SuperTNT] gecici blok kaydi yazilamadi: ${e}`); }
}
// Sure DUNYA SAATIYLE tutulur (bkz. wclock ve mayinlar): system.currentTick
// yeniden yuklemede sifirlanir, dunya saati sifirlanmaz.
function tempEkle(dimId, x, y, z, r, blok, saniye) {
  const id = `${dimId}|${x},${y},${z}|${blok}`;
  const a = tempOku().filter((t) => t.i !== id);
  a.push({ i: id, d: dimId, x, y, z, r, b: blok, t: wclock() + saniye * 20 });
  tempYaz(a);
  return id;
}
function tempBitti(id) {
  tempSupuruluyor.delete(id);
  tempYaz(tempOku().filter((t) => t.i !== id));
}
// Dilim dilim geri alma. Hem runTimeout hem de supurge ayni yolu kullanir;
// is BAKILAN konumu sayar (bkz. transform).
function geciciTemizle(dim, cx, cy, cz, r, blok, per, bitince) {
  let ux = -r;
  const job = system.runInterval(() => {
    let d = 0;
    while (ux <= r && d < per) {
      for (let uy = -r; uy <= r; uy++) for (let uz = -r; uz <= r; uz++) {
        if (ux * ux + uy * uy + uz * uz > r * r) continue;
        d++;
        try {
          const b = dim.getBlock({ x: cx + ux, y: cy + uy, z: cz + uz });
          if (b && b.typeId === blok) b.setType("minecraft:air");
        } catch (e) {}
      }
      ux++;
    }
    if (ux > r) { system.clearRun(job); if (bitince) bitince(); }
  }, 1);
}
// Suresi dolmus kayitlar. Normalde runTimeout onlari zaten temizledi ve
// kaydi sildi; bu supurge yalnizca yeniden yuklemede kalanlar icin. Cevre
// yuklu degilse getBlock bos doner, kayit BIRAKILIR ve cocuk oraya donunce
// supurulur.
system.runInterval(() => {
  const now = wclock();
  for (const t of tempOku()) {
    if (now < t.t || tempSupuruluyor.has(t.i)) continue;
    let dim = null;
    try { dim = world.getDimension(t.d); } catch (e) {}
    if (!dim) { tempBitti(t.i); continue; }
    let yuklu = false;
    try { yuklu = !!dim.getBlock({ x: t.x, y: t.y, z: t.z }); } catch (e) {}
    if (!yuklu) continue;
    tempSupuruluyor.add(t.i);
    geciciTemizle(dim, t.x, t.y, t.z, t.r, t.b, 700, () => tempBitti(t.i));
  }
}, 100);

function detonate(dim, c, short, igniterId) {
  const s = SPEC[short];
  if (!s) return;
  // HER TNT patlama ANI geri bildirimi versin: gorsel patlama + ses. Hasar
  // vermez (createExplosion degil). Efekt-TNT'lerinin cogu blok patlatmaz;
  // onsuz cocuklar "patlamadi" saniyordu. Gercekten patlayanlar zaten
  // createExplosion ile ayrica ses/parcacik uretir, ustune binmesi zararsiz.
  try { dim.spawnParticle("minecraft:huge_explosion_emitter", c); } catch (e) {}
  try { dim.playSound("random.explode", c, { volume: 1.0, pitch: 1.0 }); } catch (e) {}
  chain(dim, c);   // F3 - zinciri kur (patlamadan once, bloklar hala duruyor)
  try {
    switch (s.kind) {
      case "explode":
        dim.createExplosion(c, s.power, { breaksBlocks: true, causesFire: false });
        // s.waves: ipucu "etrafa N kucuk patlama dalgasi" diyen TNT'ler icin
        // merkezin cevresine gecikmeli kucuk patlamalar. Eskiden ipucu bunu
        // soyluyordu ama kod tek patlama yapiyordu.
        for (let w = 0; w < (s.waves || 0); w++) {
          const aci = (w / (s.waves || 1)) * Math.PI * 2, uz = (s.waveDist || 6);
          const wl = { x: c.x + Math.cos(aci) * uz, y: c.y, z: c.z + Math.sin(aci) * uz };
          system.runTimeout(() => {
            try { dim.createExplosion(wl, s.wavePower || 3, { breaksBlocks: true, causesFire: false }); } catch (e) {}
          }, 8 + w * 6);
        }
        break;

      case "scale": {
        // SADECE oyuncular. player.json'daki st:size ozelligi kademe degistirir:
        // gorsel boyut RP render-Molang, hitbox BP collision_box ile eslenir.
        // Kalp TNT (heal) boyutu normale (2) dondurur.
        for (const p of dim.getPlayers({ location: c, maxDistance: s.radius })) {
          try {
            let sz = p.getProperty("st:size");
            if (typeof sz !== "number") sz = 2;
            sz = Math.max(0, Math.min(4, sz + s.delta));
            p.triggerEvent("st:size_" + sz);
          } catch (e) {}
        }
        spray(dim, c, s.delta < 0 ? "minecraft:mob_portal" : "minecraft:totem_particle", 40, 3);
        break;
      }

      case "launch": {
        dim.createExplosion(c, 0.1, { breaksBlocks: false, causesFire: false });
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            if (e.typeId === "minecraft:player") {
              try { e.applyKnockback(rnd(0.6), rnd(0.6), 0.9, s.force * 1.2); }
              catch (e2) { e.applyKnockback({ x: rnd(0.6), z: rnd(0.6) }, s.force * 1.2); }
            } else {
              e.applyImpulse({ x: rnd(0.4), y: s.force, z: rnd(0.4) });
            }
          } catch (err) {}
        }
        spray(dim, c, "minecraft:totem_particle", 40, 3);
        break;
      }

      case "items": {
        for (let i = 0; i < s.count; i++) {
          const p = { x: c.x + rnd(s.spread), y: c.y + 1 + Math.random() * 2, z: c.z + rnd(s.spread) };
          try { dim.spawnItem(new ItemStack(s.item, 1), p); } catch (e) {}
        }
        spray(dim, c, "minecraft:crop_growth_emitter", 30, 3);
        break;
      }

      case "villagers": {
        for (let i = 0; i < s.count; i++) {
          const p = { x: c.x + rnd(s.spread), y: c.y + 1, z: c.z + rnd(s.spread) };
          try {
            const v = dim.spawnEntity("minecraft:villager_v2", p);
            v.nameTag = s.name;
            if (s.baby) { try { v.triggerEvent("minecraft:entity_born"); } catch (e) {} }
          } catch (e) {}
        }
        spray(dim, c, "minecraft:heart_particle", 30, 3);
        break;
      }

      case "weather":
        try { dim.setWeather(s.weather, s.weatherTicks || 1200); } catch (e) {}
        spray(dim, c, "minecraft:water_evaporation_actor_emitter", 40, 4);
        break;

      case "time":
        // Yalnizca YON DEGISTIRIYORSA calisir: Ay TNT gunduzu geceye, Gunes
        // TNT geceyi gunduze cevirir. Eskiden kosul her ikisi icin de
        // "tod < 12300" idi, yani GUNES TNT GECE HICBIR SEY YAPMIYORDU —
        // tam da kullanilacagi anda. (Kosulsuz set etmek de yanlis: gunduz
        // patlatilan Ay TNT gunu basa sariyordu.)
        try {
          const tod = world.getTimeOfDay();
          const gunduz = tod < 12300, hedefGunduz = s.time < 12300;
          if (gunduz !== hedefGunduz) world.setTimeOfDay(s.time);
        } catch (e) {
          try { world.setTimeOfDay(s.time); } catch (e2) {}
        }
        spray(dim, c, "minecraft:endrod", 60, 4);
        break;

      case "heal": {
        // SADECE OYUNCULAR. Butun varliklara verilirse zombi/iskelet de
        // guclenir - Java tarafinda 88b9de1 ile duzeltilen hata buydu.
        for (const p of dim.getPlayers({ location: c, maxDistance: s.radius })) {
          try {
            // Java KalpTntEntity once tum efektleri temizliyor, sonra veriyor
            for (const eff of p.getEffects()) {
              try { p.removeEffect(eff.typeId); } catch (err) {}
            }
            try { p.setDynamicProperty(FOREVER_PROP, undefined); } catch (err) {}
            // Java gibi boyutu da normale (2) dondurur — Kucultme/Buyutme
            // TNT'sinin veya boyut toplarinin etkisini temizler.
            try { p.triggerEvent("st:size_2"); } catch (e) {}
            p.addEffect("regeneration", 600, { amplifier: 2, showParticles: true });
            p.addEffect("absorption", 600, { amplifier: 4, showParticles: true });
          } catch (err) {}
        }
        spray(dim, c, "minecraft:heart_particle", 80, 5);
        break;
      }

      case "freeze": {
        // BuzTntEntity.java ile birebir:
        //  - setWeather(0, WEATHER_TICKS, true, false) -> YAGMUR (thunder yok).
        //    Bedrock'ta "Snow" diye bir hava tipi YOK; kar, soguk biyomda
        //    yagmurun gorunumudur. "Snow" gecmek sessizce basarisiz oluyordu.
        //  - SLOWNESS amp 6 + MINING_FATIGUE amp 4, 600 tick
        //  - Gorsel patlama: guc 1.0, blok hasari yok
        // Bedrock'ta setFrozenTicks karsiligi yok - gercek donma gorseli eksik.
        // JAVA'DAN AYRILAN TEK YER — YARICAP. Java surumu dunyadaki BUTUN
        // oyunculari donduruyor. Slowness amp 6 hareket hizini sifirlar: cocuk
        // 30 saniye boyunca hic yuruyemez. Iki kardes ayni dunyadayken biri
        // evde TNT patlatinca digeri 300 blok oteki madende sebepsiz yere
        // kilitleniyordu — "oyun bozuldu" diye bildirilen sey tam olarak bu.
        // Artik buzun kapladigi yaricapla ayni: donan, olayi GOREN oyuncudur.
        const ticks = (s.freeze_seconds ?? 30) * 20;
        for (const p of dim.getPlayers({ location: c, maxDistance: s.radius })) {
          try {
            if (igniterId && p.id === igniterId) continue;   // patlatan haric
            p.addEffect("slowness", ticks, { amplifier: 6, showParticles: true });
            p.addEffect("mining_fatigue", ticks, { amplifier: 4, showParticles: false });
          } catch (err) {}
        }
        try { dim.setWeather("Rain", ticks); } catch (err) {}
        try { dim.createExplosion(c, 1.0, { breaksBlocks: false, causesFire: false }); } catch (err) {}
        const r = s.radius;
        // agir is: tek tick'te degil, dilim dilim
        let dx = -r;
        const job = system.runInterval(() => {
          for (let n = 0; n < 1 && dx <= r; n++, dx++) {
            for (let dz = -r; dz <= r; dz++) {
              for (let dy = -4; dy <= 4; dy++) {
                if (dx * dx + dz * dz > r * r) continue;
                const p = { x: Math.floor(c.x) + dx, y: Math.floor(c.y) + dy, z: Math.floor(c.z) + dz };
                try {
                  const b = dim.getBlock(p);
                  if (!b) continue;
                  const t = b.typeId;
                  if (t === "minecraft:water" || t === "minecraft:flowing_water") b.setType("minecraft:packed_ice");
                  else if (t === "minecraft:lava" || t === "minecraft:flowing_lava") b.setType("minecraft:stone");
                  else if (t !== "minecraft:air") {
                    const up = dim.getBlock({ x: p.x, y: p.y + 1, z: p.z });
                    if (up && up.typeId === "minecraft:air") up.setType("minecraft:snow_layer");
                  }
                } catch (e) {}
              }
            }
          }
          if (dx > r) system.clearRun(job);
        }, 1);
        spray(dim, c, "minecraft:snowflake_particle", 60, 5);
        break;
      }

      // ---- coklu item sacma (emerald, maden, gokkusagi, dunya, ...)
      case "scatter": {
        for (const d of s.drops) {
          for (let i = 0; i < d.count; i++) {
            const p = { x: c.x + rnd(s.spread), y: c.y + 1 + Math.random() * 2, z: c.z + rnd(s.spread) };
            try { dim.spawnItem(new ItemStack(d.item, d.stack || 1), p); } catch (e) {}
          }
        }
        if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: false, causesFire: false }); } catch (e) {} }
        spray(dim, c, "minecraft:crop_growth_emitter", 40, 4);
        break;
      }

      // ---- durum efekti (zehir, nuclear, buff, cleanse, uyku, ...)
      case "status": {
        const targets = s.target === "all"
          ? dim.getEntities({ location: c, maxDistance: s.radius })
          : dim.getPlayers({ location: c, maxDistance: s.radius });
        for (const e of targets) {
          try {
            if (s.exceptIgniter && igniterId && e.id === igniterId) continue;
            if (s.clear) {
              for (const ef of e.getEffects()) { try { e.removeEffect(ef.typeId); } catch (x) {} }
              // Sinirsiz can isareti de gitmeli: kalmazsa dongu bir sonraki
              // turda efekti geri koyar ve "tum efektleri temizler" yalan olur.
              try { e.setDynamicProperty(FOREVER_PROP, undefined); } catch (x) {}
              // Temizleyici TNT'nin ipucu "efektleri VE BOYUT degisikliklerini
              // temizler" diyordu ama boyutu hic sifirlamiyordu: kucultulmus
              // cocuk temizleyiciyi patlatip kucuk kaliyor ve esyayi bozuk
              // saniyor. Kalp TNT (heal) bunu zaten yapiyor, ayni satir.
              if (e.typeId === "minecraft:player") { try { e.triggerEvent("st:size___SIZE_DEFAULT__"); } catch (x) {} }
            }
            for (const ef of (s.effects || [])) {
              e.addEffect(ef.id, ef.seconds * 20, { amplifier: ef.amp || 0, showParticles: true });
            }
          } catch (err) {}
        }
        if (s.give) {
          for (const p of dim.getPlayers({ location: c, maxDistance: s.radius })) {
            try { p.runCommand(`give @s ${s.give.item} ${s.give.count}`); } catch (e) {}
          }
        }
        if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: false, causesFire: false }); } catch (e) {} }
        spray(dim, c, s.particle || "minecraft:heart_particle", 40, 4);
        break;
      }

      // ---- aninda oldur (gizli: patlatan haric; elmas_zirh: herkes)
      case "instakill": {
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            if (e.typeId === "minecraft:player" && s.exceptIgniter !== false && igniterId && e.id === igniterId) continue;
            e.applyDamage(1000);
          } catch (err) {}
        }
        if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: true, causesFire: false }); } catch (e) {} }
        spray(dim, c, "minecraft:soul_particle", 60, 5);
        break;
      }

      // ---- entity spawn (yildirim, kristal)
      case "spawn": {
        for (let i = 0; i < s.count; i++) {
          const p = { x: c.x + rnd(s.spread || 6), y: c.y + (s.yoff || 0), z: c.z + rnd(s.spread || 6) };
          try { dim.spawnEntity(s.entity, p); } catch (e) {}
        }
        if (s.weather) { try { dim.setWeather(s.weather, s.weatherTicks || 1200); } catch (e) {} }
        // s.digs: ipucu "magara oyar" diyen TNT'ler icin patlama BLOK KIRAR.
        // Digerlerinde kirmaz — cocugun evini yikmasin.
        if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: !!s.digs, causesFire: false }); } catch (e) {} }
        break;
      }

      // ---- blok yikim, dilim dilim (bedrock, cam, kup, kiyamet)
      case "break": {
        const r = s.radius, per = s.perTick || 800;
        const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
        if (s.killPlayers) { for (const p of dim.getPlayers()) { try { p.applyDamage(1000); } catch (e) {} } }
        let bx = -r;
        const job = system.runInterval(() => {
          let d = 0;
          while (bx <= r && d < per) {
            for (let by = -r; by <= r; by++) for (let bz = -r; bz <= r; bz++) {
              // s.cube: adi "Kup TNT" olan icin kure degil KUP. Digerleri kure.
              if (!s.cube && bx * bx + by * by + bz * bz > r * r) continue;
              // d, DEGISTIRILEN degil INCELENEN pozisyonu sayar. Eskiden
              // yalnizca setType'in yanindaydi: filtreye uyan blok yoksa d sifirda
              // kaliyor, while kosulu hic yanlislanmiyor ve TUM kure tek tick'te
              // taraniyordu. Cam TNT (r=30) camsiz arazide 226 981 pozisyon,
              // Kiyamet (r=35) havada patlayinca ~180 000 getBlock -> saniyelerce
              // donma. Isi sayinca en kotu durum bir x-dilimi kadar olur.
              d++;
              try {
                const b = dim.getBlock({ x: cx + bx, y: cy + by, z: cz + bz }); if (!b) continue;
                const t = b.typeId;
                if (t === "minecraft:air") continue;
                if (s.skipBedrock && t === "minecraft:bedrock") continue;
                if (s.filter) {
                  const ok = Array.isArray(s.filter) ? s.filter.some(f => t.includes(f)) : t.includes(s.filter);
                  if (!ok) continue;
                }
                b.setType("minecraft:air");
              } catch (e) {}
            }
            bx++;
          }
          if (bx > r) {
            system.clearRun(job);
            if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: true, causesFire: false }); } catch (e) {} }
          }
        }, 1);
        break;
      }

      // ---- alan doldur, dilim dilim (buz, su, mob_freeze, lego, ...)
      case "place": {
        const r = s.radius, per = s.perTick || 700;
        const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
        let px = -r;
        const job = system.runInterval(() => {
          let d = 0;
          const skip = new Set();
          try {
            for (const pl of dim.getPlayers({ location: c, maxDistance: r + 2 })) {
              const q = pl.location, qx = Math.floor(q.x), qy = Math.floor(q.y), qz = Math.floor(q.z);
              skip.add(`${qx},${qy},${qz}`); skip.add(`${qx},${qy + 1},${qz}`);
            }
          } catch (e) {}
          while (px <= r && d < per) {
            for (let py = -r; py <= r; py++) for (let pz = -r; pz <= r; pz++) {
              if (px * px + py * py + pz * pz > r * r) continue;
              d++;                                  // bkz. transform: is sayilir
              // Oyuncunun durdugu iki blok doldurulmaz — yoksa bogulur.
              if (skip.has(`${cx + px},${cy + py},${cz + pz}`)) continue;
              try {
                const b = dim.getBlock({ x: cx + px, y: cy + py, z: cz + pz }); if (!b) continue;
                const t = b.typeId;
                // s.douses: ipucu "atesleri sondurur" diyen TNT ates blogunu
                // DA suya cevirir. onlyAir tek basina ates blogunu atliyordu,
                // yani soz veren tek is yapilmiyordu.
                if (s.onlyAir && t !== "minecraft:air"
                    && !(s.douses && t === "minecraft:fire")) continue;
                if (!s.onlyAir && t === "minecraft:air") continue;
                if (t === "minecraft:bedrock") continue;
                b.setType(s.block);
              } catch (e) {}
            }
            px++;
          }
          if (px > r) system.clearRun(job);
        }, 1);
        if (s.tempSeconds) {
          // Kayit dunyaya yazilir: cocuk sure dolmadan oyundan cikarsa su/buz
          // sonsuza kadar kalmasin. Bu seansin kaydiyla asagidaki runTimeout
          // ilgilenir, supurge ona dokunmaz.
          const tid = tempEkle(dim.id, cx, cy, cz, r, s.block, s.tempSeconds);
          system.runTimeout(() => {
            geciciTemizle(dim, cx, cy, cz, r, s.block, per, () => tempBitti(tid));
          }, s.tempSeconds * 20);
        }
        spray(dim, c, s.particle || "minecraft:crop_growth_emitter", 40, 4);
        break;
      }

      // ---- blok donusturme, konum-tabanli palet (rainbow, makarna, seker, ...)
      case "transform": {
        const r = s.radius, per = s.perTick || 900, pal = s.palette;
        // s.sound: ipucu ayri bir ses vaat eden TNT'ler icin. Paket normalde
        // her TNT'de ayni "random.explode" sesini calar; ipucu baska bir ses
        // soyluyorsa onu da calmak gerekiyor.
        if (s.sound) { try { dim.playSound(s.sound, c, { volume: 1.2 }); } catch (e) {} }
        // s.buff: ipucu "hiz ve ziplama" diyen TNT'ler icin yakindaki
        // oyunculara efekt. Eskiden ipucu soz veriyor, kod vermiyordu.
        if (s.buff) {
          for (const bp of dim.getPlayers({ location: c, maxDistance: r })) {
            for (const ef of s.buff) {
              try { bp.addEffect(ef.id, ef.seconds * 20, { amplifier: ef.amp || 0, showParticles: true }); } catch (e) {}
            }
          }
        }
        const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
        let tx = -r;
        const job = system.runInterval(() => {
          let d = 0;
          while (tx <= r && d < per) {
            for (let ty = -r; ty <= r; ty++) for (let tz = -r; tz <= r; tz++) {
              if (tx * tx + ty * ty + tz * tz > r * r) continue;
              d++;                                  // bkz. transform: is sayilir
              try {
                const b = dim.getBlock({ x: cx + tx, y: cy + ty, z: cz + tz }); if (!b) continue;
                const t = b.typeId;
                if (t === "minecraft:air" || t === "minecraft:bedrock") continue;
                b.setType(pal[Math.abs(tx * 7 + ty * 13 + tz * 17) % pal.length]);
              } catch (e) {}
            }
            tx++;
          }
          if (tx > r) system.clearRun(job);
        }, 1);
        spray(dim, c, "minecraft:crop_growth_emitter", 40, 4);
        break;
      }

      // ---- yakindaki her seyi merkeze cek, sonra patlat (magnet)
      case "magnet": {
        let pulls = 0;
        const pj = system.runInterval(() => {
          for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
            try {
              const l = e.location;
              const dx = c.x - l.x, dy = c.y - l.y, dz = c.z - l.z;
              const len = Math.hypot(dx, dy, dz) || 1;
              try { e.applyKnockback(dx / len, dz / len, 0.6, 0.5); }
              catch (e2) { e.applyKnockback({ x: dx / len * 0.6, z: dz / len * 0.6 }, 0.5); }
            } catch (err) {}
          }
          if (++pulls >= (s.pullTicks || 60)) {
            system.clearRun(pj);
            try { dim.createExplosion(c, s.power || 5, { breaksBlocks: true, causesFire: false }); } catch (e) {}
          }
        }, 5);
        break;
      }

      // ---- yakindaki canlilarin konumlarini karistir (swap)
      case "swap": {
        const ents = [...dim.getEntities({ location: c, maxDistance: s.radius })];
        const locs = ents.map(e => { try { return e.location; } catch (x) { return null; } });
        for (let i = locs.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          const t = locs[i]; locs[i] = locs[j]; locs[j] = t;
        }
        ents.forEach((e, i) => { try { if (locs[i]) e.teleport(locs[i]); } catch (err) {} });
        spray(dim, c, "minecraft:mob_portal", 50, 5);
        break;
      }
    }
  } catch (e) {
    console.warn(`[SuperTNT] ${short} patlamasi basarisiz: ${e}`);
  }
}

// EJDERHA NEFESI. Baktigin yere kalici mor bulut. Iki sey onemli:
//  - Hasar 2, yani TAM bir kalp. Bedrock'ta applyDamage(1) yarim kalp goturur.
//  - Vurus araligi bir saniye (pulse=20 tick): kalpler tek tek gitsin, tek
//    seferde erimesin. Suresi bitince interval KENDINI durdurur.
// Bulut sahibini de yakar (vanilla ejderha nefesi de oyle); ipucu bunu yaziyor.
const breathCd = new Map();
const BREATH_MAX = 6;                    // es zamanli bulut tavani (bkz. asagi)
let breathLive = 0;
function dragonBreath(player, a) {
  const dim = player.dimension, now = system.currentTick;
  // Kimlik SIMDI okunur. Bulut a.seconds boyunca yasiyor; oyuncu o sirada
  // cikarsa yakalanan `player` uzerindeki her okuma patlar, istisna dongunun
  // disina kacar ve breathLive bir daha azalmaz -> esya kalici kilitlenir.
  const pid = player.id;
  if ((breathCd.get(pid) || 0) > now) return;
  if (breathLive >= BREATH_MAX) {
    try { player.onScreenDisplay.setActionBar("§7Çok fazla bulut var, biraz bekle"); } catch (e) {}
    return;
  }
  breathCd.set(player.id, now + a.cd);
  breathLive++;
  const v = player.getViewDirection(), s = player.getHeadLocation();
  const hit = player.getBlockFromViewDirection({ maxDistance: a.reach });
  const c = hit
    ? { x: hit.block.location.x + 0.5, y: hit.block.location.y + 1.2, z: hit.block.location.z + 0.5 }
    : { x: s.x + v.x * a.reach, y: s.y + v.y * a.reach, z: s.z + v.z * a.reach };
  try { dim.playSound("mob.enderdragon.growl", c, { volume: 1.1 }); } catch (e) {}
  const total = a.seconds * 20;
  let t = 0;
  const job = system.runInterval(() => {
    t += 5;
    // Daireye ESIT dagilim: sqrt olmadan parcaciklar merkeze yigilir.
    for (let i = 0; i < 24; i++) {
      const ang = Math.random() * Math.PI * 2;
      const rr = Math.sqrt(Math.random()) * a.radius;
      try {
        dim.spawnParticle(i % 4 ? "minecraft:dragon_breath_lingering" : "minecraft:dragon_breath_trail",
                          { x: c.x + Math.cos(ang) * rr, y: c.y + Math.random() * 1.8 - 0.9,
                            z: c.z + Math.sin(ang) * rr });
      } catch (e) {}
    }
    if (t % a.pulse === 0) {
      let ents = [];
      try { ents = dim.getEntities({ location: c, maxDistance: a.radius }); } catch (e) {}
      for (const e of ents) {
        if (SONIC_SKIP[e.typeId]) continue;             // esya/tecrube yanmasin
        // Sahibine kendi kimligiyle hasar veremeyiz -> duz hasara dus.
        if (e.id === pid) { try { e.applyDamage(a.damage); } catch (err) {} continue; }
        try { e.applyDamage(a.damage, { cause: "magic", damagingEntity: player }); }
        catch (err) { try { e.applyDamage(a.damage); } catch (err2) {} }
      }
      try { dim.playSound("random.fizz", c, { volume: 0.5 }); } catch (e) {}
    }
    if (t >= total) { system.clearRun(job); breathLive--; }
  }, 5);
}

function spray(dim, c, particle, n, spread) {
  for (let i = 0; i < n; i++) {
    try {
      dim.spawnParticle(particle, {
        x: c.x + rnd(spread), y: c.y + Math.random() * spread, z: c.z + rnd(spread),
      });
    } catch (e) {}
  }
}

// worldLoad her API surumunde yok; olmazsa ilk tick'te yuklenir.
try {
  world.afterEvents.worldLoad.subscribe(() => loadTracked());
} catch (e) {}
system.run(() => {
  loadTracked();
  console.warn(`[SuperTNT] yuklendi - ${Object.keys(SPEC).length} TNT, ${tracked.size} kayitli blok`);
});

// Ustunde durulan tuzak bloklari: yanlis altin plaka oldurur, zehir topragi
// zehirler (Java'daki davranis).
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    try {
      const l = p.location;
      const below = p.dimension.getBlock({ x: Math.floor(l.x), y: Math.floor(l.y) - 1, z: Math.floor(l.z) });
      if (!below) continue;
      if (below.typeId === "stnt:wrong_golden_plate") p.applyDamage(1000);
      else if (below.typeId === "stnt:zehir_toprak") p.addEffect("poison", 100, { amplifier: 2 });
      else if (below.typeId === "stnt:end_gate") {
        // Duz (100, 70, 0) isinlamasi ya 18 hasarlik dusus (obsidyen platform
        // y~49) ya da bosluga dusme demekti: (100, 0) ana adanin kenari.
        // Once oraya git (chunk yuklensin), 10 tick sonra zemini bul ve
        // uzerine kon; zemin yoksa kendi platformumuzu kur.
        try {
          const end = world.getDimension("minecraft:the_end");
          p.teleport({ x: 100.5, y: 70, z: 0.5 }, { dimension: end });
          system.runTimeout(() => {
            try {
              let gy = null;
              for (let y = 70; y >= 30; y--) {
                const b = end.getBlock({ x: 100, y, z: 0 });
                if (b && b.typeId !== "minecraft:air") { gy = y; break; }
              }
              if (gy === null) {
                for (let ax = -2; ax <= 2; ax++) for (let az = -2; az <= 2; az++) {
                  try { end.getBlock({ x: 100 + ax, y: 48, z: az }).setType("minecraft:obsidian"); } catch (e) {}
                }
                gy = 48;
              }
              p.teleport({ x: 100.5, y: gy + 1, z: 0.5 }, { dimension: end });
              spray(end, { x: 100.5, y: gy + 1, z: 0.5 }, "minecraft:mob_portal", 20, 1);
            } catch (e) {}
          }, 10);
        } catch (e) {}
      }
    } catch (e) {}
  }
}, 5);

// Herobrine Cagirici: yerlestirilince yaratik cagirir, sonra kaybolur.
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  try {
    if (ev.block.typeId !== "stnt:herobrine_spawner") return;
    const loc = ev.block.location, dim = ev.dimension;
    ev.block.setType("minecraft:air");
    for (let i = 0; i < 3; i++) {
      try {
        dim.spawnEntity("minecraft:zombie",
          { x: loc.x + 0.5 + (Math.random() - 0.5) * 3, y: loc.y + 1, z: loc.z + 0.5 + (Math.random() - 0.5) * 3 });
      } catch (e) {}
    }
    spray(dim, loc, "minecraft:soul_particle", 40, 3);
  } catch (e) {}
});

// ? Blogu: kirilinca icinden rastgele bir sey cikar.
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try {
    if (ev.brokenBlockPermutation.type.id !== "stnt:soru_blogu") return;
    const loot = ["minecraft:diamond", "minecraft:golden_apple", "minecraft:tnt",
                  "minecraft:iron_ingot", "minecraft:emerald", "minecraft:cooked_beef",
                  "minecraft:gold_ingot", "minecraft:cake"];
    const pick = loot[Math.floor(Math.random() * loot.length)];
    ev.dimension.spawnItem(new ItemStack(pick, 1 + Math.floor(Math.random() * 4)),
      { x: ev.block.location.x + 0.5, y: ev.block.location.y + 0.5, z: ev.block.location.z + 0.5 });
  } catch (e) {}
});

// TANI: oyuncu dunyaya girince chat'e yazar. Bu mesaj gorunuyorsa davranis
// paketi aktif VE script calisiyor demektir. Gorunmuyorsa paket aktif degil.
try {
  world.afterEvents.playerSpawn.subscribe((ev) => {
    if (!ev.initialSpawn) return;
    try {
      ev.player.sendMessage(
        `\u00A7a[Super TNT] Yuklendi! ${Object.keys(SPEC).length} TNT hazir. ` +
        `\u00A7fYaratici envanterde ara ya da: \u00A7e/give @s stnt:zeynep_tnt`);
    } catch (e) {}
  });
} catch (e) {}

// ================= Sahip / Sifre / Portal ozellikleri =================
// Bedrock'ta blogun kendi dinamik ozelligi YOK; sahiplik/sifre bir DUNYA
// dinamik ozelliginde JSON harita olarak tutulur (redstone-TNT listesiyle
// ayni desen). Konum anahtari: "<boyut>:<x>:<y>:<z>".
const OWNER_PROP = "stnt:owners";
let owners = {};
function loadOwners() {
  try { const raw = world.getDynamicProperty(OWNER_PROP); if (typeof raw === "string") owners = JSON.parse(raw); }
  catch (e) { owners = {}; }
}
const OWNER_MAX = 400;          // bkz. TRACK_MAX: kalici ozelligin bayt butcesi var
function saveOwners() {
  const ks = Object.keys(owners);
  // Nesne anahtarlari EKLENME sirasinda gelir, yani en eskiler bastadir.
  if (ks.length > OWNER_MAX) for (const k of ks.slice(0, ks.length - OWNER_MAX)) delete owners[k];
  try { world.setDynamicProperty(OWNER_PROP, JSON.stringify(owners)); }
  catch (e) { console.warn(`[SuperTNT] sahiplik yazilamadi (${ks.length} kayit): ${e}`); }
}
function pkey(dimId, loc) { return `${dimId}:${Math.floor(loc.x)}:${Math.floor(loc.y)}:${Math.floor(loc.z)}`; }
function hashStr(s) { let h = 0; for (let i = 0; i < s.length; i++) { h = (h * 31 + s.charCodeAt(i)) | 0; } return h; }
const OWNER_BLOCKS = new Set(["stnt:sahip_kapi", "stnt:blocker_sandik", "stnt:sifreli_sandik"]);

try { world.afterEvents.worldLoad.subscribe(() => loadOwners()); } catch (e) {}
loadOwners();   // modul degerlendirmesinde bir kez; worldLoad reload icin yedek

world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (!b || !OWNER_BLOCKS.has(b.typeId)) return;
  owners[pkey(b.dimension.id, b.location)] = { owner: ev.player.id, ownerName: ev.player.name };
  saveOwners();
  try { ev.player.onScreenDisplay.setActionBar("\u00A7aBu blok art\u0131k sana ait"); } catch (e) {}
});

world.afterEvents.playerBreakBlock.subscribe((ev) => {
  if (!ev || !ev.block || !ev.dimension) return;
  const k = pkey(ev.dimension.id, ev.block.location);
  if (owners[k]) { delete owners[k]; saveOwners(); }
});

world.beforeEvents.playerInteractWithBlock.subscribe((ev) => {
  const b = ev.block;
  if (!b || !OWNER_BLOCKS.has(b.typeId)) return;
  const k = pkey(b.dimension.id, b.location);
  const rec = owners[k];
  const player = ev.player;
  if (!rec) return;                       // sahipsiz (eski blok) - dokunma
  if (b.typeId === "stnt:sifreli_sandik") {
    ev.cancel = true;                      // vanilla etkilesimi bastir
    // sahip mi? sifre KOYMA sadece sahibe; sifre GIRME herkese acik.
    system.run(() => promptPassword(player, k, rec, player.id === rec.owner));
    return;
  }
  if (player.id !== rec.owner) {           // sahip kapi / kilitli sandik: sadece sahip
    ev.cancel = true;
    try { player.onScreenDisplay.setActionBar(`\u00A7cBu ${rec.ownerName ?? "birinin"} \u2014 a\u00E7amazs\u0131n!`); } catch (e) {}
    return;
  }
  if (b.typeId === "stnt:sahip_kapi") {
    ev.cancel = true;
    const dim = b.dimension, loc = { x: b.location.x, y: b.location.y, z: b.location.z }, type = b.typeId;
    system.run(() => {                     // sahip acinca 4 sn acilir
      try {
        dim.getBlock(loc)?.setType("minecraft:air");
        // 4 sn sonra KOSULSUZ geri koy: onceden "sadece hala air ise" kontrolu
        // vardi; o pencerede bir blok akarsa/konursa kapi kalici kayboluyordu.
        system.runTimeout(() => { try { dim.getBlock(loc)?.setType(type); } catch (e) {} }, 80);
      } catch (e) {}
    });
  } else if (b.typeId === "stnt:blocker_sandik") {
    ev.cancel = true;                      // sahip icin geri bildirim
    system.run(() => { try { player.onScreenDisplay.setActionBar("§aSenin kilitli sandığın — güvende"); } catch (e) {} });
  }
});

function promptPassword(player, k, rec, isOwner) {
  try {
    const first = !rec.hash;
    if (first && !isOwner) {               // sahipsiz sandiga sifreyi baskasi koyamasin
      try { player.onScreenDisplay.setActionBar("§cŞifreyi sadece sandığın sahibi koyabilir"); } catch (e) {}
      return;
    }
    const form = new ModalFormData()
      .title(first ? "\u015Eifre Belirle" : "\u015Eifreli Sand\u0131k")
      .textField(first ? "Bu sand\u0131\u011Fa yeni \u015Fifre koy:" : "\u015Eifreyi gir:", "1234");
    form.show(player).then((r) => {
      if (r.canceled || !r.formValues) return;
      const pw = String(r.formValues[0] || "");
      const h = hashStr(pw);
      if (first) {
        rec.hash = h; owners[k] = rec; saveOwners();
        try { player.onScreenDisplay.setActionBar("\u00A7a\u015Eifre belirlendi! Do\u011Fru \u015Fifreyi giren a\u00E7ar."); } catch (e) {}
      } else if (h === rec.hash) {
        if (!rec.looted) {
          rec.looted = true; owners[k] = rec; saveOwners();
          try { player.runCommand("give @s minecraft:diamond 5"); } catch (e) {}
          try { player.onScreenDisplay.setActionBar("\u00A7aDo\u011Fru \u015Fifre! Sand\u0131ktan 5 elmas ald\u0131n!"); } catch (e) {}
        } else {
          try { player.onScreenDisplay.setActionBar("\u00A7aDo\u011Fru \u015Fifre \u2014 sand\u0131k zaten bo\u015Falt\u0131lm\u0131\u015F"); } catch (e) {}
        }
      } else {
        try { player.onScreenDisplay.setActionBar("\u00A7cYanl\u0131\u015F \u015Fifre!"); } catch (e) {}
      }
    }).catch(() => {});
  } catch (e) {}
}

// ---- Kucultme/Buyutme POV kamerasi
//
// NEDEN BETIKLE SURULEN BIR KAMERA. Bedrock'ta ilk-sahis goz yuksekligi
// SABITTIR; ne render-scale, ne collision_box, ne minecraft:scale onu
// degistirir (belge minecraft:scale'i "visual size multiplier" diye tanimlar).
// Ozel kamera on-ayarlari da ise yaramiyor: "A custom Camera Preset can
// inherit from other custom Camera Presets, or from the 'minecraft:free'
// preset. For now, the other built-in camera perspectives can't be specified
// here." (Camera System Introduction). Yani ilk-sahis kamerayi kaydirmanin
// desteklenen bir yolu YOK. Kucukken dunyayi buyuk gormenin tek yolu
// "minecraft:free" kamerayi betikle surmek.
//
// SERBEST KAMERA HAREKETI KILITLEMEZ. Kilitlemek isteyen ayrica
// /inputpermission cagirmak zorunda (Free Camera Preset Tutorial). Biz
// cagirmiyoruz.
//
// v1.40.0'daki hali uc seyi yanlis yapiyordu; ucu de burada duzeltildi:
//   1) Konum p.location + SABIT 1.62*olcek idi. Comelince goz inmiyordu ve
//      cocuk "egilemiyorum" diyordu. Artik getHeadLocation() ile GERCEK goz
//      konumu okunuyor, yerden yuksekligi olcekleniyor: comelme de olceklenir.
//   2) Her tick tek bir konuma ATLIYORDU. Oyun saniyede 60 kare cizerken
//      kamera 20 kez zipliyor; "kamera arkadan gelip yetisiyor" hissi bu.
//      Artik her set bir tick boyunca LINEER easing ile suruluyor, kamera
//      adim adim degil akici gidiyor.
//   3) Kamera oyuncunun KENDI kafasinin icinde kaliyordu ve ekran SIYAH
//      oluyordu — serbest kamerada oyuncunun kendi modeli de cizilir. Artik
//      kamera, bakis yonunun YATAY bileseninde kafanin disina oteleniyor.
//      Yatay: yukari/asagi bakinca kamera kafanin ustune/altina kacmasin.
//
// KALAN SINIR (duzeltilemez): betik saniyede 20 kez calisir, kamera en iyi
// ihtimalle bir tick geride kalir. Easing bunu AKICI yapar, sifirlamaz.
// Gercekten oyuncuya bagli bir kamera icin deneysel "Creator Cameras" ucuncu-
// sahis on-ayarlari gerekir; onlar da dunyada bayrak acmayi zorunlu kilar.
const camState = new Map();      // playerId -> kamera surulen son boyut
function releaseCamera(p) {      // kamerayi birak: normal ilk-sahis geri gelir
  try { p.camera.clear(); } catch (e) {}
  camState.delete(p.id);
}
// Kameranin one otelenmesi kademe basina TABLODAN gelir; oyuncunun kendi
// kafasinin on yuzu ile carpisma kutusunun on yuzu arasinda durur (ikisinin de
// disi ekrani siyah yapar). Tablonun nasil hesaplandigi build.py SIZE_CAM_FWD.
const SIZE_CAM_FWD = __SIZE_CAM_FWD__;
const SIZE_H = __SIZE_H__;            // st:size -> carpisma yuksekligi (blok)
// Oyuncunun BASININ ustunde hedef yukseklige kadar yer var mi? Yalnizca kendi
// sutununa bakilir: tuzak durumu alcak TAVAN. Yanlara bakmiyoruz, cimen/cicek
// yuzunden bosuna reddetmeyelim; genislik zaten Minecraft tarafindan disari
// iterek cozuluyor, asil sikisma dikeyde oluyor.
const GECIRGEN = new Set(["minecraft:air", "minecraft:water", "minecraft:flowing_water"]);
function tavanVarMi(pl, hedefY) {
  const l = pl.location, dim = pl.dimension;
  const x = Math.floor(l.x), z = Math.floor(l.z), y0 = Math.floor(l.y);
  for (let dy = 2; dy <= Math.ceil(hedefY); dy++) {
    try {
      const b = dim.getBlock({ x, y: y0 + dy, z });
      if (b && !GECIRGEN.has(b.typeId)) return false;
    } catch (e) {}
  }
  return true;
}
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    let sz;
    try { sz = p.getProperty("st:size"); } catch (e) { continue; }
    if (typeof sz !== "number") sz = __SIZE_DEFAULT__;
    if (sz === __SIZE_DEFAULT__) {
      // Hic kamera surmedigimiz oyuncunun varsayilan kamerasina DOKUNMA.
      if (camState.has(p.id)) releaseCamera(p);
      continue;
    }
    if (!p.camera || typeof p.camera.setCamera !== "function") continue;
    const scale = SIZE_SCALES[sz] || SIZE_SCALES[String(sz)] || 1;
    if (!(scale > 0)) continue;
    let loc, head, rot, dir;
    try {
      loc = p.location; head = p.getHeadLocation();
      rot = p.getRotation(); dir = p.getViewDirection();
    } catch (e) { continue; }
    if (!loc || !head || !rot || !dir) continue;
    // Gozun AYAKTAN yuksekligi olceklenir; comelince head.y zaten duser.
    const eye = loc.y + (head.y - loc.y) * scale;
    const fwd = SIZE_CAM_FWD[sz] || SIZE_CAM_FWD[String(sz)] || 0.2;
    const hyp = Math.hypot(dir.x, dir.z) || 1;   // yalnizca yatay bilesen
    try {
      p.camera.setCamera("minecraft:free", {
        location: { x: loc.x + (dir.x / hyp) * fwd, y: eye, z: loc.z + (dir.z / hyp) * fwd },
        rotation: { x: rot.x, y: rot.y },
        easeOptions: { easeTime: 0.05, easeType: "Linear" },   // tam bir tick
      });
      camState.set(p.id, sz);
    } catch (e) {
      // Kamera API'si yoksa sessizce vazgec: gorsel olcek ve carpisma kutusu
      // zaten calisiyor, oyunun geri kalani etkilenmiyor.
    }
  }
}, 1);
try { for (const p of world.getPlayers()) releaseCamera(p); } catch (e) {}
try {
  world.afterEvents.playerSpawn.subscribe((ev) => {
    releaseCamera(ev.player);
    // OLUNCE NORMALE DON. st:size ve st:morph oyuncunun uzerinde KALICIDIR:
    // minicik ya da blok kilikli olen bir cocuk ayni halde uyaniyordu ve geri
    // donmenin yolunu (Kalp TNT / Donusum Asasi) her zaman bulamiyor. Olum
    // artik garantili cikis yolu — potion efektlerinin olumde silinmesiyle
    // ayni beklenti. initialSpawn = dunyaya ILK giris; orada dokunmayiz,
    // yoksa dunya her acilista kilik bozulur.
    if (ev.initialSpawn) return;
    const p = ev.player;
    try {
      if (p.getProperty("st:size") !== __SIZE_DEFAULT__) p.triggerEvent("st:size___SIZE_DEFAULT__");
    } catch (e) {}
    try {
      if (p.getProperty("st:morph") !== 0) morphTo(p, "st:morph_human", "İnsana geri döndün");
    } catch (e) {}
  });
} catch (e) {}

// ---- Portal: AYNI RENK iki blok birbirine isinlar. Kayit global bir dunya
// ozelliginde (herkes icin gecerli); portal blogu KONUNCA eklenir, KIRILINCA
// silinir. Boylece hem Portal Silahi hem elle konan bloklar calisir.
const portalCd = new Map();
const portalArrival = new Map();     // oyuncu -> az once konduğu portal
const portalGunCount = new Map();
function getPortals() {
  try { const r = world.getDynamicProperty("stnt:portals"); return typeof r === "string" ? JSON.parse(r) : []; }
  catch (e) { return []; }
}
function setPortals(a) { try { world.setDynamicProperty("stnt:portals", JSON.stringify(a.slice(-64))); } catch (e) {} }
function addPortal(d, x, y, z, c) {
  const a = getPortals();
  if (!a.some((q) => q.d === d && q.x === x && q.y === y && q.z === z)) { a.push({ c, d, x, y, z }); setPortals(a); }
}
function removePortal(d, x, y, z) {
  setPortals(getPortals().filter((q) => !(q.d === d && q.x === x && q.y === y && q.z === z)));
}
// Portal blogu ELLE konunca da kaydet (Portal Silahi'yla ayni sistem).
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  try {
    const id = ev.block && ev.block.typeId;
    if (!id || !id.startsWith("stnt:portal_")) return;
    const c = parseInt(id.slice("stnt:portal_".length), 10);
    if (Number.isNaN(c)) return;
    const bl = ev.block.location;
    addPortal(ev.block.dimension.id, Math.floor(bl.x), Math.floor(bl.y), Math.floor(bl.z), c);
    const n = getPortals().filter((q) => q.c === c).length;
    try { ev.player.onScreenDisplay.setActionBar(n >= 2
      ? "§aPortal bağlandı! Üstüne bas, eşine ışınlan."
      : "§bPortal kondu — aynı renkten bir tane daha koy."); } catch (e) {}
  } catch (e) {}
});
system.runInterval(() => {
  const now = system.currentTick;
  const all = getPortals();
  if (!all.length) return;
  for (const p of world.getPlayers()) {
    if ((portalCd.get(p.id) || 0) > now) continue;
    try {
    const l = p.location, d = p.dimension.id;
    let on = null;                                   // uzerinde durdugun portal
    for (const P of all) {
      if (P.d === d && Math.abs(l.x - (P.x + 0.5)) < 1 && Math.abs(l.y - P.y) < 1.6 && Math.abs(l.z - (P.z + 0.5)) < 1) { on = P; break; }
    }
    if (!on) { portalArrival.delete(p.id); continue; }
    // Az once BU portala kondu: uzerinden cekilene kadar tekrar isinlama.
    const arr = portalArrival.get(p.id);
    if (arr && arr.d === on.d && arr.x === on.x && arr.y === on.y && arr.z === on.z) continue;
    portalArrival.delete(p.id);
    let best = null, bestDist = Infinity;             // ayni renk EN YAKIN es
    for (const Q of all) {
      if (Q === on || Q.c !== on.c) continue;
      const dx = Q.x - on.x, dy = Q.y - on.y, dz = Q.z - on.z, dist = dx * dx + dy * dy + dz * dz;
      if (dist < bestDist) { bestDist = dist; best = Q; }
    }
    if (!best) continue;
    try {
      p.teleport({ x: best.x + 0.5, y: best.y + 1, z: best.z + 0.5 }, { dimension: world.getDimension(best.d) });
      spray(p.dimension, { x: best.x + 0.5, y: best.y + 1, z: best.z + 0.5 }, "minecraft:mob_portal", 20, 1);
    } catch (e) {}
    portalArrival.set(p.id, best);
    portalCd.set(p.id, now + 40);                     // 2 sn - ileri-geri titremeyi onler
    } catch (e) { continue; }                         // bir oyuncu portallari komple oldurmesin
  }
}, 8);

// ---- Portal blogu kirilinca kayittan cikar
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try {
    if (!ev.brokenBlockPermutation.type.id.startsWith("stnt:portal_")) return;
    const bl = ev.block.location;
    removePortal(ev.dimension.id, Math.floor(bl.x), Math.floor(bl.y), Math.floor(bl.z));
  } catch (e) {}
});

// ---- Sahte TNT: pasta kiligi; kirilinca/etkilesince sadece oyuncuya hasar
function fakeTntBoom(dim, loc, player) {
  const c = { x: loc.x + 0.5, y: loc.y + 0.5, z: loc.z + 0.5 };
  try { dim.createExplosion(c, 2, { breaksBlocks: false, causesFire: false }); } catch (e) {}
  spray(dim, c, "minecraft:large_explosion", 1, 0);
  if (player) {
    try { player.applyDamage(14); } catch (e) {}
    try { player.onScreenDisplay.setActionBar("§cKANDIRILDIN! O bir Sahte TNT'ydi 😄"); } catch (e) {}
  }
}
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try { if (ev.brokenBlockPermutation.type.id === "stnt:fake_tnt") fakeTntBoom(ev.dimension, ev.block.location, ev.player); }
  catch (e) {}
});
world.beforeEvents.playerInteractWithBlock.subscribe((ev) => {
  try {
    if (!ev.block || ev.block.typeId !== "stnt:fake_tnt") return;
    ev.cancel = true;
    const dim = ev.block.dimension, loc = { x: ev.block.location.x, y: ev.block.location.y, z: ev.block.location.z }, player = ev.player;
    system.run(() => { try { dim.getBlock(loc)?.setType("minecraft:air"); } catch (e) {} fakeTntBoom(dim, loc, player); });
  } catch (e) {}
});

// ---- Patlayici Kum: kum kiliginda tuzak. Kazilinca (blok gittikten sonra)
// oldugu yerde vanilla TNT gucunde patlar; blok kirar, ates yakmaz. Kazan
// oyuncu ayrilmaz — tuzagin amaci bu. Vanilla kum (minecraft:sand) etkilenmez.
const KUM_POWER = 4;   // vanilla TNT
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try {
    if (ev.brokenBlockPermutation.type.id !== "stnt:patlayici_kum") return;
    const l = ev.block.location;
    const c = { x: Math.floor(l.x) + 0.5, y: Math.floor(l.y) + 0.5, z: Math.floor(l.z) + 0.5 };
    ev.dimension.createExplosion(c, KUM_POWER, { breaksBlocks: true, causesFire: false });
  } catch (e) {}
});

// ---- Yakinlik Mayini: kurulunca 2 sn (40 tick) arm gecikmesi, sonra 10
// tick'te bir 2.5 blok tarar; canli yaklasirsa patlar (kacis penceresi arm).
const MINE_PROP = "stnt:mines";
let mines = {};
function loadMines() { try { const r = world.getDynamicProperty(MINE_PROP); if (typeof r === "string") mines = JSON.parse(r); } catch (e) { mines = {}; } }
const MINE_MAX = 200;           // ayni gerekce; ustelik her kayit 10 tick'te taraniyor
function saveMines() {
  const ks = Object.keys(mines);
  if (ks.length > MINE_MAX) for (const k of ks.slice(0, ks.length - MINE_MAX)) delete mines[k];
  try { world.setDynamicProperty(MINE_PROP, JSON.stringify(mines)); }
  catch (e) { console.warn(`[SuperTNT] mayinlar yazilamadi (${ks.length} kayit): ${e}`); }
}
try { world.afterEvents.worldLoad.subscribe(() => loadMines()); } catch (e) {}
loadMines();   // modul degerlendirmesinde bir kez; worldLoad reload icin yedek
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (!b || b.typeId !== "stnt:yakinlik_mayini") return;
  mines[pkey(b.dimension.id, b.location)] = { dim: b.dimension.id, x: Math.floor(b.location.x), y: Math.floor(b.location.y), z: Math.floor(b.location.z), armed: wclock() + 40 };
  saveMines();
  try { ev.player.onScreenDisplay.setActionBar("§cMayın kuruluyor — 2 saniye içinde kaç!"); } catch (e) {}
});
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  if (!ev || !ev.block || !ev.dimension) return;
  const k = pkey(ev.dimension.id, ev.block.location);
  if (mines[k]) { delete mines[k]; saveMines(); }
});
system.runInterval(() => {
  const now = wclock();                 // diske yazilan armed ile ayni saat
  let changed = false;
  for (const k of Object.keys(mines)) {
    const m = mines[k];
    if (now < m.armed) continue;
    try {
      const dim = world.getDimension(m.dim);
      const b = dim.getBlock({ x: m.x, y: m.y, z: m.z });
      if (!b) continue;                                  // yuklu degil
      if (b.typeId !== "stnt:yakinlik_mayini") { delete mines[k]; changed = true; continue; }
      const c = { x: m.x + 0.5, y: m.y + 0.5, z: m.z + 0.5 };
      const near = dim.getEntities({ location: c, maxDistance: 2.5 })
        .some((e) => e.typeId === "minecraft:player" || (e.getComponent && e.getComponent("minecraft:health")));
      if (near) {
        b.setType("minecraft:air");
        dim.createExplosion(c, 4, { breaksBlocks: true, causesFire: false });
        delete mines[k]; changed = true;
      }
    } catch (e) {}
  }
  if (changed) saveMines();
}, 10);

// ---- TNT Zirhi: giyen hasar alinca saldirgana kucuk patlama misilleme
world.afterEvents.entityHurt.subscribe((ev) => {
  try {
    const victim = ev.hurtEntity;
    const src = ev.damageSource && ev.damageSource.damagingEntity;
    if (!src || !victim || src.id === victim.id) return;
    const eq = victim.getComponent("minecraft:equippable");
    if (!eq) return;
    let wearing = false;
    for (const sl of [EquipmentSlot.Head, EquipmentSlot.Chest, EquipmentSlot.Legs, EquipmentSlot.Feet]) {
      try { const it = eq.getEquipment(sl); if (it && it.typeId.startsWith("stnt:tnt_armor")) { wearing = true; break; } } catch (e) {}
    }
    if (!wearing) return;
    try { src.dimension.createExplosion(src.location, 2, { breaksBlocks: false, causesFire: false }); } catch (e) {}
    try { src.applyDamage(8); } catch (e) {}
  } catch (e) {}
});

// ---------------------------------------------------------------- atesler
// Ender Atesi (mavi): icine giren oyuncuyu isinlar. Altin Ates (sari): yakar.
// Ikisi de SONMEZ — zamanlanmis sondurme yok, su onlari silmez (blok
// replaceable degil); yalniz yumrukla kirilir.
const EF = __ENDER_FIRE__;
const GF = __GOLD_FIRE__;
const ENDER_FIRE_ID = "stnt:ender_atesi", GOLD_FIRE_ID = "stnt:altin_atesi";
const EF_PROP = "stnt:enderfires";
const EF_AREA = "stnt_ender";        // gecici tickingarea adi (tek seferde bir tarama)

// Altina donusmeyecek bloklar: dunya tabani/bariyer (dunyayi delerdi), sivi,
// kendi atesler ve portallar (kayit tutarliligi), altinin kendisi.
const GOLD_SKIP = new Set(["minecraft:air", "minecraft:bedrock", "minecraft:barrier", "minecraft:command_block",
  "minecraft:end_portal", "minecraft:end_portal_frame", "minecraft:end_gateway", "minecraft:water",
  "minecraft:flowing_water", "minecraft:lava", "minecraft:flowing_lava", "minecraft:gold_block",
  ENDER_FIRE_ID, GOLD_FIRE_ID, "stnt:tukenmezlik_orsu"]);
function goldable(b) {
  if (!b || GOLD_SKIP.has(b.typeId) || b.typeId.startsWith("stnt:portal_")) return false;
  // Sandik gibi icerik tasiyan blok: esyalar sessizce yok olurdu.
  try { if (b.getComponent && b.getComponent("minecraft:inventory")) return false; } catch (e) {}
  return true;
}

// Bakilan blogun USTUNE ates koy. Altin Flint ayrica bakilan blogu altina
// cevirir: "hangi blogun uzerine yakildiysa" bilgisi burada var, ates
// blogunda degil.
function lightFire(player, dim, a, gold) {
  const hit = player.getBlockFromViewDirection({ maxDistance: a.range });
  if (!hit) { try { player.onScreenDisplay.setActionBar("§7Ateş için yakındaki bir bloğa bak"); } catch (e) {} return; }
  const bl = hit.block.location;
  const above = { x: Math.floor(bl.x), y: Math.floor(bl.y) + 1, z: Math.floor(bl.z) };
  const target = dim.getBlock(above);
  if (!target || target.typeId !== "minecraft:air") { try { player.onScreenDisplay.setActionBar("§7Üstü boş bir bloğa bak"); } catch (e) {} return; }
  if (gold && goldable(hit.block)) { try { hit.block.setType("minecraft:gold_block"); } catch (e) {} }
  try { target.setType(gold ? GOLD_FIRE_ID : ENDER_FIRE_ID); } catch (e) {}
  if (!gold) addEnderFire(dim.id, above.x, above.y, above.z);
  spray(dim, { x: above.x + 0.5, y: above.y + 0.2, z: above.z + 0.5 },
        gold ? "minecraft:basic_flame_particle" : "minecraft:blue_flame_particle", 12, 0.6);
  try { dim.playSound("fire.ignite", above, { volume: 1.0, pitch: gold ? 1.3 : 0.7 }); } catch (e) {}
  wearHeld(player, gold ? "stnt:altin_flint" : "stnt:ender_cakmagi");
}

// Elindeki cakmagi 1 yipratir. ItemStack KOPYA: geri yazmak sart. Tukenmez
// isaretli cakmaga dokunulmaz — dongu onu zaten sifirlar, ama son kullanimda
// kirilip yok olmasi dongunun onune gecerdi.
function wearHeld(player, typeId) {
  try {
    const con = player.getComponent("minecraft:inventory")?.container;
    const slot = player.selectedSlotIndex;
    const it = con && con.getItem(slot);
    if (!it || it.typeId !== typeId || isUnending(it)) return;
    const d = it.getComponent("minecraft:durability");
    if (!d) return;
    if (d.damage + 1 >= d.maxDurability) {
      con.setItem(slot, undefined);
      try { player.dimension.playSound("random.break", player.location, { volume: 1.0 }); } catch (e) {}
      return;
    }
    d.damage += 1;
    con.setItem(slot, it);
  } catch (e) {}
}

function getEnderFires() {
  try { const r = world.getDynamicProperty(EF_PROP); return typeof r === "string" ? JSON.parse(r) : []; }
  catch (e) { return []; }
}
function setEnderFires(a) { try { world.setDynamicProperty(EF_PROP, JSON.stringify(a.slice(-EF.maxFires))); } catch (e) {} }
function addEnderFire(d, x, y, z) {
  const a = getEnderFires();
  if (!a.some((q) => q.d === d && q.x === x && q.y === y && q.z === z)) { a.push({ d, x, y, z }); setEnderFires(a); }
}
function removeEnderFire(d, x, y, z) {
  setEnderFires(getEnderFires().filter((q) => !(q.d === d && q.x === x && q.y === y && q.z === z)));
}
// Komutla/elle konan ender atesi de kayda girsin.
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  try {
    const b = ev.block;
    if (!b || b.typeId !== ENDER_FIRE_ID) return;
    addEnderFire(b.dimension.id, Math.floor(b.location.x), Math.floor(b.location.y), Math.floor(b.location.z));
  } catch (e) {}
});
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  try {
    if (ev.brokenBlockPermutation.type.id !== ENDER_FIRE_ID) return;
    const bl = ev.block.location;
    removeEnderFire(ev.dimension.id, Math.floor(bl.x), Math.floor(bl.y), Math.floor(bl.z));
  } catch (e) {}
});

// Agirlikli SIRA: her secenek bir kez cekilir; ilk tutan hedef kullanilir.
function enderOrder() {
  const pool = [["ender", EF.ender], ["soul", EF.soul], ["fire", EF.fire]];
  const order = [];
  while (pool.length) {
    let total = 0; for (const [, w] of pool) total += w;
    let r = Math.random() * total, i = 0;
    for (; i < pool.length - 1; i++) { r -= pool[i][1]; if (r < 0) break; }
    order.push(pool.splice(i, 1)[0][0]);
  }
  return order;
}
// Rastgele sirada en fazla 16 kaydi blokla dogrular; yetim kaydi siler.
// Yuklu olmayan chunk'taki kayda guvenilir (isinlanma chunk'i yukler).
function pickEnderFire(from) {
  const all = getEnderFires().filter((q) => !(q.d === from.d && q.x === from.x && q.y === from.y && q.z === from.z));
  for (let i = all.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [all[i], all[j]] = [all[j], all[i]]; }
  for (const q of all.slice(0, 16)) {
    try {
      const b = world.getDimension(q.d).getBlock({ x: q.x, y: q.y, z: q.z });
      if (!b || b.typeId === ENDER_FIRE_ID) return q;
      removeEnderFire(q.d, q.x, q.y, q.z);
    } catch (e) {}
  }
  return null;
}

const enderPending = new Map();   // oyuncu id -> bekleyen Nether taramasi
const enderCd = new Map();
const enderArrival = new Map();   // oyuncu -> az once vardigi ates (uzerinden cekilene kadar)
let enderBusy = null;             // tek tickingarea: su an tarayan oyuncu

function enderTeleport(p, q) {
  const c = { x: q.x + 0.5, y: q.y + 0.5, z: q.z + 0.5 };
  try { p.teleport({ x: q.x + 0.5, y: q.y, z: q.z + 0.5 }, { dimension: world.getDimension(q.d) }); } catch (e) { return; }
  spray(p.dimension, c, "minecraft:mob_portal", 24, 1);
  try { p.dimension.playSound("mob.endermen.portal", c, { volume: 1.0 }); } catch (e) {}
  enderArrival.set(p.id, q);
  enderCd.set(p.id, system.currentTick + EF.cooldown);
}
// Komutu once oyuncuyla, o gecersizse boyutla calistir (oyuncu cikmis olabilir).
function runCmd(p, cmd) {
  try { if (p) { p.runCommand(cmd); return; } } catch (e) {}
  try { world.getDimension("minecraft:overworld").runCommand(cmd); } catch (e) {}
}
function enderRelease(id, p) {
  enderPending.delete(id);
  if (enderBusy === id) {
    enderBusy = null;
    runCmd(p, `tickingarea remove ${EF_AREA}`);
  }
}
// Siradaki secenegi dener. Ender atesi senkron; Nether secenekleri chunk
// yuklemesi bekledigi icin BEKLEYEN kayda gecer (asagidaki dongu surdurur).
function enderAdvance(st) {
  const p = st.p;
  while (st.i < st.order.length) {
    const k = st.order[st.i++];
    if (k === "ender") {
      const q = pickEnderFire(st.from);
      if (q) { enderRelease(p.id, p); enderTeleport(p, q); return; }
      continue;
    }
    st.fire = k === "soul" ? "minecraft:soul_fire" : "minecraft:fire";
    st.phase = enderBusy && enderBusy !== p.id ? "queue" : "load";
    st.cur = null; st.found = [];
    enderPending.set(p.id, st);
    if (st.phase === "load") enderStartLoad(st);
    return;
  }
  enderRelease(p.id, p);
  try { p.onScreenDisplay.setActionBar("§7Gidecek bir ateş yok"); } catch (e) {}
}
function enderStartLoad(st) {
  enderBusy = st.p.id;
  st.phase = "load";
  st.until = system.currentTick + EF.loadWait;
  try {
    st.p.runCommand(`execute in nether run tickingarea add ${st.cx - EF.radius} ${EF.minY} ${st.cz - EF.radius} `
      + `${st.cx + EF.radius} ${EF.maxY} ${st.cz + EF.radius} ${EF_AREA}`);
  } catch (e) {}
}
// Butceli tarama: null = chunk bekleniyor, false = devam, true = bitti.
function enderScanStep(st) {
  const nether = world.getDimension("minecraft:nether");
  if (!st.cur) {
    let probe; try { probe = nether.getBlock({ x: st.cx, y: 64, z: st.cz }); } catch (e) {}
    if (!probe) return null;
    st.cur = { dx: -EF.radius, dz: -EF.radius, y: EF.minY };
  }
  const c = st.cur;
  let n = 0;
  while (c.dx <= EF.radius) {
    let b; try { b = nether.getBlock({ x: st.cx + c.dx, y: c.y, z: st.cz + c.dz }); } catch (e) {}
    if (b && b.typeId === st.fire) st.found.push({ d: "minecraft:nether", x: st.cx + c.dx, y: c.y, z: st.cz + c.dz });
    if (++c.y > EF.maxY) { c.y = EF.minY; if (++c.dz > EF.radius) { c.dz = -EF.radius; c.dx++; } }
    if (st.found.length >= 32) return true;
    if (++n >= EF.perTick) return false;
  }
  return true;
}
system.runInterval(() => {
  for (const [id, st] of [...enderPending]) {
    try {
      st.p.location;                                   // oyuncu cikti mi? (InvalidEntityError)
    } catch (e) { enderRelease(id, null); continue; }
    try {
      if (st.phase === "queue") { if (!enderBusy) enderStartLoad(st); continue; }
      const r = enderScanStep(st);
      if (r === null) {
        if (system.currentTick <= st.until) continue;   // chunk daha gelmedi
        enderAdvance(st);                                // suresi doldu: siradaki
        continue;
      }
      if (r === false) continue;
      if (st.found.length) {
        const q = st.found[Math.floor(Math.random() * st.found.length)];
        enderRelease(id, st.p);
        enderTeleport(st.p, q);
      } else {
        enderAdvance(st);
      }
    } catch (e) { enderRelease(id, st.p); }
  }
}, 2);
// Modul yuklenirken eski bir taramadan kalan alan olmasin (betik ortada olduyse).
runCmd(null, `tickingarea remove ${EF_AREA}`);

// Altin Ates: yakar. Ates direnci olan yanmaz (vanilla ile ayni).
function burnIn(p) {
  try { if (p.getEffects().some((e) => e.typeId === "fire_resistance")) return; } catch (e) {}
  try { p.setOnFire(GF.burn, true); } catch (e) {}
  try { p.applyDamage(GF.damage, { cause: "fire" }); } catch (e) { try { p.applyDamage(GF.damage); } catch (e2) {} }
}
// Ayak hizasindaki blok: ender atesi -> isinla, altin ates -> yak.
system.runInterval(() => {
  const now = system.currentTick;
  for (const p of world.getPlayers()) {
    try {
      const l = p.location;
      const at = { x: Math.floor(l.x), y: Math.floor(l.y), z: Math.floor(l.z) };
      const b = p.dimension.getBlock(at);
      const id = b ? b.typeId : "";
      if (id === GOLD_FIRE_ID) burnIn(p);
      if (id !== ENDER_FIRE_ID) { enderArrival.delete(p.id); continue; }
      if (enderPending.has(p.id) || (enderCd.get(p.id) || 0) > now) continue;
      const arr = enderArrival.get(p.id);
      if (arr && arr.d === p.dimension.id && arr.x === at.x && arr.y === at.y && arr.z === at.z) continue;
      enderArrival.delete(p.id);
      // Bekleme suresi aramadan ONCE: hedef bulunamasa da yazilir, yoksa
      // atesin icinde duran oyuncu her turda yeni bir Nether taramasi acar.
      enderCd.set(p.id, now + EF.cooldown);
      const scale = p.dimension.id === "minecraft:overworld" ? 0.125 : 1;   // vanilla portal olcegi
      enderAdvance({ p, order: enderOrder(), i: 0, from: { d: p.dimension.id, x: at.x, y: at.y, z: at.z },
                     cx: Math.floor(at.x * scale), cz: Math.floor(at.z * scale) });
    } catch (e) {}
  }
}, GF.every);

// ---------------------------------------------------------------- Tukenmezlik Orsu
// Bedrock'ta ozel buyu yok ve vanilla ors genisletilemez. Isaret bu yuzden
// esyanin LORE satiri: cocuk esyada "Tukenmez" yazisini gorur, isaret esyayla
// birlikte tasinir ve kalicidir. Dongu isaretli yigini dolu, aleti yipranmamis
// tutar; TEK ADETLI isaretli esya (totem gibi) tukenince ayni yuvaya geri
// konur. Yalniz DEGISEN esya geri yazilir — her turda her yuvayi yazmak yay
// germeyi / yemeyi keserdi.
const UN = __UNENDING__;
const ANVIL_ID = "stnt:tukenmezlik_orsu";
function isUnending(it) {
  try { return !!it && it.getLore().includes(UN.mark); } catch (e) { return false; }
}
function markUnending(player) {
  try {
    const con = player.getComponent("minecraft:inventory")?.container;
    const slot = player.selectedSlotIndex;
    const it = con && con.getItem(slot);
    if (!it) { player.onScreenDisplay.setActionBar("§7Elinde bir eşya tut, sonra örse dokun"); return; }
    if (isUnending(it)) { player.onScreenDisplay.setActionBar("§dBu eşya zaten tükenmez"); return; }
    const lore = it.getLore(); lore.push(UN.mark); it.setLore(lore);
    con.setItem(slot, it);
    spray(player.dimension, player.location, "minecraft:enchanting_table_particle", 20, 1.0);
    try { player.dimension.playSound("random.anvil_use", player.location, { volume: 1.0 }); } catch (e) {}
    player.onScreenDisplay.setActionBar("§d✦ Eşyan artık TÜKENMEZ!");
  } catch (e) {}
}
world.beforeEvents.playerInteractWithBlock.subscribe((ev) => {
  try {
    if (!ev.block || ev.block.typeId !== ANVIL_ID) return;
    ev.cancel = true;                                  // elindeki blogu orsun ustune koymasin
    const player = ev.player;
    system.run(() => markUnending(player));
  } catch (e) {}
});
function refreshUnending(it) {                          // true = geri yazilmali
  let changed = false;
  try { if (it.amount < it.maxAmount) { it.amount = it.maxAmount; changed = true; } } catch (e) {}
  try { const d = it.getComponent("minecraft:durability"); if (d && d.damage > 0) { d.damage = 0; changed = true; } } catch (e) {}
  return changed;
}
function anySlotHas(con, typeId) {
  for (let i = 0; i < con.size; i++) {
    try { const it = con.getItem(i); if (it && it.typeId === typeId && isUnending(it)) return true; } catch (e) {}
  }
  return false;
}
function restoreUnending(typeId) {
  const back = new ItemStack(typeId, 1);
  back.setLore([UN.mark]);
  return back;
}
const unendingSeen = new Map();   // oyuncu id -> { yuva: typeId } tek adetli isaretli esyalar
const UN_SLOTS = ["Head", "Chest", "Legs", "Feet", "Offhand"];
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    try {
      const con = p.getComponent("minecraft:inventory")?.container;
      if (!con) continue;
      const seen = unendingSeen.get(p.id) || {};
      const next = {};
      for (let i = 0; i < con.size; i++) {
        const it = con.getItem(i);
        if (!it) {
          // Tek adetli isaretli esya tukendi mi? Baska yuvaya tasindiysa
          // dokunma; envanterde hic yoksa ayni yuvaya geri koy.
          const gone = seen[i];
          if (gone && !anySlotHas(con, gone)) { try { con.setItem(i, restoreUnending(gone)); next[i] = gone; } catch (e) {} }
          continue;
        }
        if (!isUnending(it)) continue;
        if (it.maxAmount === 1) next[i] = it.typeId;
        if (refreshUnending(it)) con.setItem(i, it);
      }
      // Giyilen zirh ve sol el (totem cogunlukla orada).
      const eq = p.getComponent("minecraft:equippable");
      if (eq) for (const name of UN_SLOTS) {
        const sl = EquipmentSlot[name];
        if (!sl) continue;
        try {
          const it = eq.getEquipment(sl);
          if (!it) {
            const gone = seen["eq:" + name];
            if (gone && !anySlotHas(con, gone)) { eq.setEquipment(sl, restoreUnending(gone)); next["eq:" + name] = gone; }
            continue;
          }
          if (!isUnending(it)) continue;
          if (it.maxAmount === 1) next["eq:" + name] = it.typeId;
          if (refreshUnending(it)) eq.setEquipment(sl, it);
        } catch (e) {}
      }
      unendingSeen.set(p.id, next);
    } catch (e) {}
  }
}, UN.every);
'''

if __name__ == "__main__":
    build()
