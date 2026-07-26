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
VERSION = [1, 27, 0]
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
# ILK SAHIS KAMERASI: render-scale ve collision_box goz yuksekligini
# DEGISTIRMEZ (Java'nin aksine Bedrock'ta goz ~1.62 blokta sabit). minecraft:
# scale oyuncuya UYGULANABILIR (MorphX kaniti: component_group+event ile) ama
# o da model+hitbox'i olcekler, goz yuksekligini DEGIL. POV'u gercekten
# oynatmanin TEK yolu deneysel Script Kamera sistemi: player.camera.setCamera
# ("minecraft:free" / attachToEntity) her tick surulur. Bunu yapan calisan
# ornek: Coco & Vici "True POV Size Changer". BEDELI: dunyada "Beta APIs" +
# "Experimental Creator Cameras" acik olmali, ve donusum boyunca oyuncu
# scripted ucuncu-sahis/orbit kameraya KILITLENIR (gercek ilk-sahise gecince
# yukseklik sifirlanir). POV EKLENDI: script'te camState runInterval'i boyut
# normalden farkliysa p.camera.setCamera("minecraft:free")'i olcekli goz
# yuksekligine surer; bayraklar kapaliysa try ile yutulur (mod bozulmaz).
# Kaynak: learn.microsoft.com Camera Script API; curseforge Coco & Vici
# True POV Size Changer.
SIZE_TABLE = {
    0: (0.33, 0.35, 0.60),   # minik   (0.6/1.8)
    1: (0.55, 0.50, 1.00),   # kucuk   (1.0/1.8)
    2: (1.00, 0.60, 1.80),   # normal  (vanilla)
    3: (1.65, 0.90, 3.00),   # buyuk   (3.0/1.8)
    4: (2.00, 1.20, 3.60),   # dev     (3.6/1.8)
}
SIZE_DEFAULT = 2

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
# geometry/texture/material — MorphX'in kanitlanmis patterni). SAF GORSEL:
# carpisma kutusu st:size'dan gelir, morph sadece gorunumu degistirir, boylece
# st:size ile carpisma-kutusu cakismasi olmaz.
# DIKKAT: geometry tek basina animate ETMEZ — mob statik bind-pose'da gorunur.
# Creeper statik bile net okunur; golem iki-bacakli, player animasyonu tolere
# eder. Ender dragon animasyon olmadan donmus/kotu goruneceginden EKLENMEDI.
# Asset adlari (geometry.creeper.v1.8 vb.) MorphX'in RP player.json'undan
# dogrulandi; dosya SHIP EDILMEZ, MC vanilla'dan saglar.
# Super TNT'nin kendi morph'u KALDIRILDI: morph paketi (merge/MorphX) zaten
# 90+ mob veriyor (warden dahil, calisiyor) ve player.json'i override eden iki
# sistem ayni dunyada CAKISIR. Super TNT morph'u ayrica statik (animasyonsuz)
# oldugundan dusuk kaliteliydi. MORPHS bos -> morph kodu uretilmez, kucultme
# (st:size) aynen korunur. Ileride istenirse buraya mob eklenir.
# Mob morph'lari: Donusum Asasi ile bir mob'a dokununca o mob'un GORUNUMUNE
# gecersin (carpisma kutusu st:size'dan gelir; morph sadece gorunum). Asset
# adlari (geometry/texture/material) bedrock-samples'tan bire bir dogrulandi —
# yanlisi = gorunmez model. SADECE tek-katman temiz render eden moblar alindi;
# warden (kara blob, glow katmanlari), sheep (kel pembe), villager/axolotl
# (default texture yok) bilincli DISLANDI. fp_bones=[] -> ilk sahiste el bos
# (cocuk ucuncu sahis oynar; onemsiz). n = st:morph tam sayisi.
MORPHS = [
    dict(key="creeper", n=1, tr="Creeper", geo="geometry.creeper.v1.8",
         tex="textures/entity/creeper/creeper", mat="creeper", scale=1.0, fp_bones=[]),
    dict(key="zombie", n=2, tr="Zombi", geo="geometry.zombie.v1.8",
         tex="textures/entity/zombie/zombie", mat="zombie", scale=1.0, fp_bones=[]),
    dict(key="skeleton", n=3, tr="İskelet", geo="geometry.skeleton.v1.8",
         tex="textures/entity/skeleton/skeleton", mat="skeleton", scale=1.0, fp_bones=[]),
    dict(key="enderman", n=4, tr="Enderman", geo="geometry.enderman.v1.8",
         tex="textures/entity/enderman/enderman", mat="enderman", scale=1.0, fp_bones=[]),
    dict(key="iron_golem", n=5, tr="Demir Golem", geo="geometry.irongolem",
         tex="textures/entity/iron_golem", mat="iron_golem", scale=1.0, fp_bones=[]),
    dict(key="wolf", n=6, tr="Kurt", geo="geometry.wolf",
         tex="textures/entity/wolf/wolf", mat="wolf", scale=1.0, fp_bones=[]),
    dict(key="pig", n=7, tr="Domuz", geo="geometry.pig.v3",
         tex="textures/entity/pig/pig_v3", mat="pig", scale=1.0, fp_bones=[]),
    dict(key="cow", n=8, tr="İnek", geo="geometry.cow.v2",
         tex="textures/entity/cow/cow_v2", mat="cow", scale=1.0, fp_bones=[]),
    dict(key="chicken", n=9, tr="Tavuk", geo="geometry.chicken.v1.12",
         tex="textures/entity/chicken/chicken", mat="chicken", scale=1.0, fp_bones=[]),
    dict(key="spider", n=10, tr="Örümcek", geo="geometry.spider.v1.8",
         tex="textures/entity/spider/spider", mat="spider", scale=1.0, fp_bones=[]),
    dict(key="piglin", n=11, tr="Piglin", geo="geometry.piglin",
         tex="textures/entity/piglin/piglin", mat="piglin", scale=1.0, fp_bones=[]),
    dict(key="allay", n=12, tr="Allay", geo="geometry.allay",
         tex="textures/entity/allay/allay", mat="allay", scale=1.0, fp_bones=[]),
    dict(key="wither_skeleton", n=13, tr="Wither İskeleti", geo="geometry.skeleton.wither.v1.8",
         tex="textures/entity/skeleton/wither_skeleton", mat="skeleton", scale=1.0, fp_bones=[]),
    dict(key="ghast", n=14, tr="Ghast", geo="geometry.ghast",
         tex="textures/entity/ghast/ghast", mat="ghast", scale=1.0, fp_bones=[]),
    dict(key="slime", n=15, tr="Slime", geo="geometry.slime",
         tex="textures/entity/slime/slime", mat="slime", scale=1.0, fp_bones=[]),
]
# mob typeId -> morph olayi (script tiklanan mob'u buradan bulur)
MORPH_MAP = {f"minecraft:{m['key']}": f"st:morph_{m['key']}" for m in MORPHS}

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
         trtip="Yağmur başlatır ve gökyüzünü bulutlandırır.",
         entip="Starts rain and clouds over the sky.",
         color=((236, 240, 244), (150, 200, 232), (236, 240, 244)), mat="minecraft:white_wool",
         effect=dict(kind="weather", weather="Rain")),
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
         trtip="Yakındaki herkese can yenileme + kalkan verir, efektleri ve boyutu sıfırlar.",
         entip="Heals and shields nearby players, clears effects and size changes.",
         color=((140, 40, 46), (196, 48, 54), (92, 48, 52)), mat="minecraft:gold_ingot",
         effect=dict(kind="heal", radius=20)),
    dict(id="buz_tnt", tr="Buz TNT", en="Ice TNT",
         trtip="14 blok yarıçapı buzla kaplar. Patlatan hariç herkesi 30 sn dondurur. 30 sn kar yağar.",
         entip="Covers a 14 block radius with ice. Freezes everyone except the igniter for 30s. Snows for 30s.",
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
         trtip="Her yeri el yazısı defterine çevirir — kağıt, mürekkep ve tüy saçar!",
         entip="Turns everywhere into a handwriting notebook - scatters paper, ink and feathers!",
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
         effect=dict(kind="explode", power=7)),
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
         trtip="20 yıldırım fırtınası çağırır.",
         entip="Summons 20 lightning bolts.",
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
         trtip="Yağmur başlatır.",
         entip="Starts rain.",
         color=((90, 120, 150), (120, 150, 180), (74, 100, 128)), mat="minecraft:water_bucket",
         effect=dict(kind="weather", weather="Rain")),
    dict(id="simsek_yagmur_tnt", tr="Şimşek Yağmuru TNT", en="Thunderstorm TNT",
         trtip="Fırtına başlatır ve 30 yıldırım yağdırır!",
         entip="Starts a storm and rains 30 lightning bolts!",
         color=((60, 66, 90), (88, 96, 128), (48, 54, 74)), mat="minecraft:lightning_rod",
         effect=dict(kind="spawn", entity="minecraft:lightning_bolt", count=30, spread=10.0,
                     weather="Thunder", weatherTicks=24000)),
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
                     particle="minecraft:water_splash_particle_manual")),
    dict(id="olumcul_su_tnt", tr="Ölümcül Su TNT", en="Deadly Water TNT",
         trtip="15 blok yarıçapını suyla doldurur (30 sn sonra kurur).",
         entip="Fills a 15 block radius with water (dries after 30s).",
         color=((30, 70, 150), (44, 92, 180), (24, 56, 120)), mat="minecraft:water_bucket",
         effect=dict(kind="place", block="minecraft:water", radius=15, onlyAir=True, tempSeconds=30, perTick=700)),
    dict(id="mob_freeze_tnt", tr="Mob Dondurucu TNT", en="Mob Freeze TNT",
         trtip="30 blok yarıçapını geçici buza çevirir.",
         entip="Turns a 30 block radius into temporary ice.",
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
         trtip="Blokları renkli yüne dönüştürür (30 blok yarıçap).",
         entip="Turns blocks into colored wool (30 block radius).",
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
             f"minecraft:{c}_glazed_terracotta" for c in ["pink", "magenta", "purple", "lime", "yellow", "light_blue"]])),
    dict(id="gulen_yuz_tnt", tr="Gülen Yüz TNT", en="Smiley TNT",
         trtip="Çıngırak sesiyle dünyayı sarıya boyar! 12 blok yarıçap.",
         entip="Paints the world yellow with a giggle! 12 block radius.",
         color=((248, 220, 60), (255, 232, 90), (228, 196, 40)), mat="minecraft:yellow_dye",
         effect=dict(kind="transform", radius=12, perTick=800, palette=[
             "minecraft:yellow_wool", "minecraft:yellow_concrete", "minecraft:yellow_terracotta", "minecraft:gold_block"])),
    dict(id="karisik_kurusuk_tnt", tr="Karışık Kuruşuk TNT", en="Crumpled TNT",
         trtip="Dünyayı ezik büzük yapar — siyah/kahverengi/gri beton.",
         entip="Crumples the world - black/brown/gray concrete.",
         color=((70, 60, 56), (96, 82, 74), (54, 46, 42)), mat="minecraft:gravel",
         effect=dict(kind="transform", radius=18, perTick=600, palette=[
             "minecraft:black_concrete", "minecraft:brown_concrete", "minecraft:gray_concrete"])),
    dict(id="elmas_diyari_tnt", tr="Elmas Diyarı TNT", en="Diamond Land TNT",
         trtip="30 blok yarıçapındaki dünyayı elmas blokuna çevirir.",
         entip="Turns a 30 block radius into diamond blocks.",
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
         trtip="15 End kristali yaratır.",
         entip="Spawns 15 End crystals.",
         color=((250, 210, 90), (255, 226, 120), (230, 186, 66)), mat="minecraft:end_crystal",
         effect=dict(kind="spawn", entity="minecraft:ender_crystal", count=15, spread=8.0)),
    dict(id="uyku_tnt", tr="Uyku TNT", en="Sleep TNT",
         trtip="Patlatan dışındaki herkesi 30 sn 'uyutur': yavaşlama, körlük, bulantı, güçsüzlük.",
         entip="Puts everyone but the igniter to 'sleep' for 30s.",
         color=((150, 120, 200), (176, 148, 220), (124, 96, 168)), mat="minecraft:pink_wool",
         effect=dict(kind="status", target="all", radius=20, exceptIgniter=True, power=0.5,
                     particle="minecraft:mobspell_emitter",
                     effects=[{"id": "slowness", "seconds": 30, "amp": 4}, {"id": "blindness", "seconds": 30, "amp": 0},
                              {"id": "nausea", "seconds": 30, "amp": 0}, {"id": "mining_fatigue", "seconds": 30, "amp": 4},
                              {"id": "weakness", "seconds": 30, "amp": 4}])),
    dict(id="gizli_tnt", tr="Gizli TNT", en="Hidden TNT",
         trtip="Cam kılığında — 25 blok yarıçapındaki tüm canlıları anında öldürür!",
         entip="Disguised as glass - instantly kills every creature within 25 blocks!",
         color=((190, 220, 230), (210, 236, 244), (170, 200, 212)), mat="minecraft:glass",
         effect=dict(kind="instakill", radius=25)),
    dict(id="magara_tnt", tr="Mağara TNT", en="Cave TNT",
         trtip="Yer altında mağara oyar ve korkunç yaratıklar doğurur!",
         entip="Carves a cave underground and spawns scary creatures!",
         color=((70, 66, 60), (92, 86, 78), (54, 50, 46)), mat="minecraft:stone",
         effect=dict(kind="spawn", entity="minecraft:zombie", count=15, spread=8.0, yoff=-2, power=4)),
    dict(id="kup_tnt", tr="Küp TNT", en="Cube TNT",
         trtip="Yerin içine doğru büyük bir küp şeklinde kazı yapar!",
         entip="Digs a big cube down into the ground!",
         color=((90, 90, 96), (112, 112, 118), (72, 72, 78)), mat="minecraft:stone",
         effect=dict(kind="break", radius=12, skipBedrock=True, perTick=1000, power=4)),
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
         effect=dict(kind="status", target="all", radius=12, particle="minecraft:portal_particle",
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
         trtip="50 blok yarıçapındaki TÜM canlıları yok eder — dev patlama!",
         entip="Destroys ALL creatures within 50 blocks - massive blast!",
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

# ---------------------------------------------------------------- item tanimlari
# kind "food": yenince efekt. "raycast": bakilan bloga/varliga etki.
# "self_area": elde kullaninca oyuncunun etrafina etki.
ITEMS = [
    dict(id="spicy_chips", tr="Acılı Cips", en="Spicy Chips", kind="food",
         trtip="Ye ve 20 sn Hız III kazan!", entip="Eat for Speed III for 20s!",
         color=(224, 120, 40), action=dict(type="eat", effect="speed", seconds=20, amp=2)),
    dict(id="lightning_spell", tr="Yıldırım Büyüsü", en="Lightning Spell", kind="raycast",
         trtip="Sağ tıkla — baktığın yere GERÇEK yıldırım çakar! Yakar ve öldürür.",
         entip="Right-click - strikes REAL lightning where you look!",
         color=(110, 140, 210), action=dict(type="lightning")),
    dict(id="black_hole", tr="Kara Delik", en="Black Hole", kind="self_area",
         trtip="Sağ tıkla — yakındaki her şeyi çeker, kör eder ve hasar verir.",
         entip="Right-click - pulls, blinds and damages nearby entities.",
         color=(40, 30, 60), action=dict(type="blackhole", radius=12)),
    dict(id="energy_crystal", tr="Enerji Kristali", en="Energy Crystal", kind="raycast",
         trtip="Sağ tıkla — baktığın bedrock'u kırar!",
         entip="Right-click - breaks the bedrock you look at!",
         color=(120, 220, 220), action=dict(type="break_any")),
    dict(id="laser_sword", tr="Lazer Kılıcı", en="Laser Sword", kind="raycast",
         trtip="Sağ tıkla — önündeki bloklarda 9 bloklık lazer açar!",
         entip="Right-click - carves a 9-block laser ahead!",
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
         trtip="Tek vuruşta mob öldürür; oyunculara ağır hasar!",
         entip="One-shots mobs; heavy damage to players!",
         color=(200, 40, 90), damage=20, action=dict(type="heavy")),
    dict(id="rainbow_boots", tr="Gökkuşağı Botları", en="Rainbow Boots", kind="boots",
         trtip="Giy — adım attığın yerde renkli yün izi bırakırsın!",
         entip="Wear them - leave a colored wool trail where you step!",
         color=(200, 60, 160), action=dict(type="worn_wool")),
    # ganimet item'lari (TNT'ler bunlari sacar; kendi baslarina davranissiz)
    dict(id="kurus", tr="1 Kuruş", en="1 Kurus", kind="loot",
         trtip="1 kuruşluk madeni para.", entip="A 1-kurus coin.", color=(196, 160, 90)),
    dict(id="iki_yuz_tl", tr="200 TL", en="200 Lira", kind="loot",
         trtip="200 TL'lik banknot.", entip="A 200-lira banknote.", color=(110, 150, 120)),
    dict(id="pink_lego_brick", tr="Pembe Lego Tuğla", en="Pink Lego Brick", kind="loot",
         trtip="Pembe Lego tuğlası.", entip="A pink Lego brick.", color=(240, 150, 190)),
    dict(id="green_lego_brick", tr="Yeşil Lego Tuğla", en="Green Lego Brick", kind="loot",
         trtip="Yeşil Lego tuğlası.", entip="A green Lego brick.", color=(80, 130, 50)),
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
         trtip="Bir mob'a dokun — o mob olursun! Bazılarının özel gücü var: ÇÖMEL (sneak) dene. Boşluğa sağ tık — insana dön.",
         entip="Tap a mob - become it! Some have powers: try SNEAK. Right-click air - turn back.",
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
         trtip="Sağ tıkla — DEV bir Ender Send çağırır! 300 can, ışınlanır, çok güçlü.",
         entip="Right-click - summons a GIANT Ender Send! 300 HP, teleports, very strong.",
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
    # OZGUN model (custom=True): kendi geometry.mutant_warden + dokumuz + kendi
    # animation.mutant_warden.move'umuz (query.ground_speed ile olcekli uzuv).
    # Doku emissive: goz/kalp/damar karanlikta parlar. Model zaten iri
    # modellendi -> scale dusuk (1.6) yeter; ~5 blok dev boss.
    dict(id="mutant_warden", mirror="warden", hp=500, scale=2.0, dmg=22, cw=1.7, ch=8.5,
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


def symbol_texture(path, item_id):
    """Duz-renk kalan item'lar icin anlamli 16x16 sembol cizer (opak RGB).
    Cocuklar item'lari ikonundan taniyabilsin diye; her biri ayirt edilebilir.
    Taninmayan id icin False doner (cagiran duz-renge duser)."""
    SPECS = {
        # id -> (arka plan). Cizim asagida id'ye gore.
        "dev_zombi_yumurta": (45, 80, 40), "dev_creeper_yumurta": (40, 110, 45),
        "mutant_warden_yumurta": (18, 40, 46), "blood_sword": (70, 14, 18),
        "heart_axe": (120, 40, 70), "dondurucu": (35, 85, 160),
        "hiz_esyasi": (55, 165, 210), "koku_bombasi": (95, 120, 45),
        "end_pearl": (24, 20, 40), "nether_pearl": (48, 14, 14),
        "takim_asasi": (65, 38, 105), "esya_calmaca": (55, 42, 80),
        "rainbow_boots": (120, 180, 220), "kurus": (60, 52, 38),
        "iki_yuz_tl": (65, 120, 85),
    }
    if item_id not in SPECS:
        return False
    bg = SPECS[item_id]
    g = [[list(bg) for _ in range(16)] for _ in range(16)]

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
        px(6, 4, (28, 115, 125)); px(10, 13, (28, 115, 125))
    elif iid == "blood_sword":
        rect(7, 2, 8, 10, (205, 210, 220)); vline(7, 2, 10, (235, 240, 250))
        hline(11, 5, 10, (120, 120, 130)); rect(7, 12, 8, 14, (110, 70, 40))
        px(7, 11, (200, 20, 20)); disc(8, 13, 1, (200, 20, 20))
    elif iid == "heart_axe":
        for (hx, hy) in [(5, 14), (6, 13), (7, 11), (8, 9), (9, 7)]:
            px(hx, hy, (110, 70, 40))
        disc(9, 5, 2, (235, 45, 80)); disc(12, 5, 2, (235, 45, 80)); rect(10, 7, 11, 9, (235, 45, 80))
    elif iid == "dondurucu":
        vline(8, 2, 13, (235, 245, 255)); hline(8, 2, 13, (235, 245, 255))
        for i in range(3, 13):
            px(i, i, (235, 245, 255)); px(i, 16 - i, (235, 245, 255))
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
        for (wx, wy) in [(4, 13), (5, 12), (6, 11), (7, 9), (8, 8), (9, 7), (10, 6), (11, 5)]:
            px(wx, wy, (110, 80, 50))
        px(12, 4, (255, 235, 90)); px(12, 2, (255, 235, 90)); px(12, 6, (255, 235, 90)); px(10, 4, (255, 235, 90)); px(14, 4, (255, 235, 90))
    elif iid == "esya_calmaca":
        vline(5, 5, 9, (220, 185, 145)); vline(8, 4, 9, (220, 185, 145)); vline(11, 5, 9, (220, 185, 145))
        rect(5, 9, 11, 12, (220, 185, 145)); rect(7, 2, 9, 4, (240, 205, 70))
    elif iid == "rainbow_boots":
        bands = [(3, 4, (220, 40, 40)), (5, 6, (230, 140, 40)), (7, 8, (235, 215, 60)),
                 (9, 10, (70, 180, 80)), (11, 12, (60, 110, 210)), (13, 14, (150, 70, 190))]
        for (y0, y1, c) in bands:
            for y in range(y0, y1 + 1):
                x0, x1 = (5, 8) if y <= 10 else (5, 12)
                rect(x0, y, x1, y, c)
    elif iid == "kurus":
        disc(8, 8, 6, (200, 150, 40)); disc(8, 8, 5, (230, 190, 70)); vline(8, 5, 11, (120, 85, 20)); px(7, 6, (120, 85, 20))
    elif iid == "iki_yuz_tl":
        rect(2, 5, 13, 11, (210, 225, 200))
        for x in range(2, 14):
            px(x, 5, (90, 160, 110)); px(x, 11, (90, 160, 110))
        vline(2, 5, 11, (90, 160, 110)); vline(13, 5, 11, (90, 160, 110))
        disc(5, 8, 1, (150, 180, 150)); hline(7, 9, 12, (90, 160, 110)); hline(9, 9, 12, (90, 160, 110))
    for i in range(16):                     # 1px cerceve (tum ikonlarla tutarli)
        g[0][i] = shade(bg, 0.7); g[15][i] = shade(bg, 0.7)
        g[i][0] = shade(bg, 0.7); g[i][15] = shade(bg, 0.7)
    png(path, g)
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
                   # Aciklama = "surum notu": Kucultme/Buyutme kamerasi icin
                   # gereken deneysel ayarlar burada da yazili (paket listesinde
                   # gorunur), boylece hangi bayragin acilacagi unutulmaz.
                   "description": (f"Super TNT Mod v{VER_STR}. "
                                   "Kucultme/Buyutme KAMERASI icin dunya ayarlarinda "
                                   "Deneyler > 'Beta APIs' + 'Creator Cameras' acin "
                                   "(kapaliyken gerisi normal calisir)."),
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
        # Java'da gercek (sembollu) ikon varsa onu kullan; yoksa duz-renk uret.
        jtex = ITEM_TEX_MAP.get(it['id'], it['id'])
        src = os.path.join(JAVA_ITEM_TEX, f"{jtex}.png")
        if os.path.exists(src):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(src, dst)
            item_copied += 1
        elif symbol_texture(dst, it['id']):     # anlamli sembol (spawn egg, silah, vb.)
            item_gen += 1
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
        w(os.path.join(BP, f"blocks/{blk['id']}.json"), {
            "format_version": "1.20.20",
            "minecraft:block": {
                "description": {"identifier": f"stnt:{blk['id']}",
                                "is_experimental": False,
                                "menu_category": {"category": "construction",
                                                  "group": "itemGroup.name.super_tnt"}},
                "components": comps,
            },
        })

    # ---------- item'lar (yiyecek, buyu, arac)
    for it in ITEMS:
        icomps = {"minecraft:icon": f"stnt_{it['id']}",
                  "minecraft:max_stack_size": 64 if it['kind'] in ("food", "loot") else 1}
        if it['kind'] == "food":
            icomps["minecraft:food"] = {"nutrition": 4, "can_always_eat": True}
            icomps["minecraft:use_animation"] = "eat"
            icomps["minecraft:use_modifiers"] = {"use_duration": 1.4, "movement_modifier": 0.35}
        elif it['kind'] == "weapon":
            icomps["minecraft:damage"] = it.get('damage', 6)
            icomps["minecraft:durability"] = {"max_durability": 800}
            icomps["minecraft:hand_equipped"] = True
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
            # GORUNUM = BIREBIR vanilla mob (custom/{id}.mirror.json). Vanilla
            # geometry/doku/materyal/render-controller/animation ADIYLA referans
            # alinir (morph deseni — paketleme yok, kopya-id cakismasi olmaz).
            # Warden icin: pre_animation query.modified_distance_moved ile bacak/
            # kol degiskenlerini hesaplar, animation.warden.move kemige baglar ->
            # GERCEK Warden gorunumu + yuruyusu. BP tarafi kendi dusman AI+scale.
            os.makedirs(os.path.join(RP, "entity"), exist_ok=True)
            shutil.copy(os.path.join(HERE, f"custom/{m['id']}.mirror.json"),
                        os.path.join(RP, f"entity/{m['id']}.json"))
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
            rp_desc["materials"][mo['key']] = mo['mat']
            rp_desc["textures"][mo['key']] = mo['tex']
            rp_desc["geometry"][mo['key']] = mo['geo']
        # vanilla insan controller'larini st:morph==0 ile guardla + morph ekle.
        guarded = []
        for e in rp_desc["render_controllers"]:
            for name, cond in e.items():
                guarded.append({name: cond} if name.endswith(".map")
                               else {name: f"({cond}) && query.property('st:morph') == 0"})
        for mo in MORPHS:
            guarded.append({f"controller.render.morph.{mo['key']}.first_person":
                            f"variable.is_first_person && !query.is_spectator && query.property('st:morph') == {mo['n']}"})
            guarded.append({f"controller.render.morph.{mo['key']}.third_person":
                            f"!variable.is_first_person && !variable.map_face_icon && !query.is_spectator && query.property('st:morph') == {mo['n']}"})
        rp_desc["render_controllers"] = guarded
    w(os.path.join(RP, "entity/player.json"), rp_player)

    # ---- morph render controller dosyasi (SADECE MORPHS doluysa)
    if MORPHS:
        morph_rc = {}
        for mo in MORPHS:
            base = {"geometry": f"Geometry.{mo['key']}",
                    "materials": [{"*": f"Material.{mo['key']}"}],
                    "textures": [f"Texture.{mo['key']}"]}
            morph_rc[f"controller.render.morph.{mo['key']}.third_person"] = dict(base)
            fp = dict(base)  # ilk sahiste mob'un on uzvu gorunsun ki el bos olmasin
            fp["part_visibility"] = [{"*": False}] + [{b: True} for b in mo['fp_bones']]
            morph_rc[f"controller.render.morph.{mo['key']}.first_person"] = fp
        w(os.path.join(RP, "render_controllers/morph.render_controllers.json"),
          {"format_version": "1.10.0", "render_controllers": morph_rc})

    # ---------- script
    spec = {t['id']: t['effect'] for t in TNTS}
    tips = {t['id']: t['tr'] for t in TNTS}
    tiptext = {t['id']: t['trtip'] for t in TNTS}
    item_actions = {it['id']: it['action'] for it in ITEMS if it.get('action')}
    script = SCRIPT_TEMPLATE.replace("__SPEC__", json.dumps(spec, indent=2)) \
                            .replace("__NAMES__", json.dumps(tips, ensure_ascii=False)) \
                            .replace("__TIPS__", json.dumps(tiptext, ensure_ascii=False)) \
                            .replace("__ITEM_ACTIONS__", json.dumps(item_actions)) \
                            .replace("__MORPH_FORMS__", json.dumps(
                                [{"ev": "st:morph_human", "name": "İnsan"}]
                                + [{"ev": f"st:morph_{mo['key']}", "name": mo['tr']} for mo in MORPHS],
                                ensure_ascii=False)) \
                            .replace("__FUSE__", str(FUSE_TICKS)) \
                            .replace("__PORTAL_N__", str(len(PORTAL_COLORS))) \
                            .replace("__PORTAL_NAMES__", json.dumps([n for n, _ in PORTAL_COLORS], ensure_ascii=False)) \
                            .replace("__SIZE_SCALES__", json.dumps({i: SIZE_TABLE[i][0] for i in SIZE_TABLE})) \
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
import { world, system, ItemStack, EquipmentSlot } from "@minecraft/server";
import { ModalFormData } from "@minecraft/server-ui";

const SPEC = __SPEC__;
const NAMES = __NAMES__;
const TIPS = __TIPS__;
const ITEM_ACTIONS = __ITEM_ACTIONS__;
const MORPH_FORMS = __MORPH_FORMS__;
const MORPH_MAP = __MORPH_MAP__;   // mob typeId -> st:morph_<key> olayi
const SIZE_SCALES = __SIZE_SCALES__;   // st:size kademe -> gorsel olcek
const FUSE = __FUSE__;

// ---------------------------------------------------------------- item'lar
world.afterEvents.itemUse.subscribe((ev) => {
  const a = ITEM_ACTIONS[ev.itemStack.typeId.replace("stnt:", "")];
  if (a && a.type !== "eat") itemAction(ev.source, a);
});
world.afterEvents.itemCompleteUse.subscribe((ev) => {
  const a = ITEM_ACTIONS[ev.itemStack.typeId.replace("stnt:", "")];
  if (a && a.type === "eat") {
    try { ev.source.addEffect(a.effect, a.seconds * 20, { amplifier: a.amp, showParticles: true }); } catch (e) {}
  }
});

function itemAction(player, a) {
  const dim = player.dimension;
  try {
    switch (a.type) {
      case "morph_reset": {
        // Donusum Asasi bosluga sag tik -> insana geri don. Mob'a dokunma
        // (morph) playerInteractWithEntity handler'inda; bir moba BAKIYORSAN
        // reset'i atla ki dokunma morph'u devralsin.
        try {
          const hs = player.getEntitiesFromViewDirection({ maxDistance: 4 });
          if (hs.length && MORPH_MAP[hs[0].entity && hs[0].entity.typeId]) break;
          player.triggerEvent("st:morph_human");
          player.onScreenDisplay.setActionBar("§aİnsana geri döndün");
          spray(dim, player.location, "minecraft:portal_particle", 20, 1.5);
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
          if (typeof sz !== "number") sz = 2;
          sz = a.reset ? 2 : Math.max(0, Math.min(4, sz + a.delta));
          player.triggerEvent("st:size_" + sz);
          spray(dim, player.location,
                (a.reset || a.delta > 0) ? "minecraft:totem_particle" : "minecraft:portal_particle", 24, 2);
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
        spray(dim, above, "minecraft:portal_particle", 30, 1.5);
        break;
      }
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
            e.applyImpulse({ x: dx / len * 0.5, y: 0.1, z: dz / len * 0.5 });
            e.addEffect("blindness", 100, { amplifier: 0 });
            e.applyDamage(4);
          } catch (err) {}
        }
        spray(dim, player.location, "minecraft:portal_particle", 40, 3);
        break;
      }
      case "break_any": {
        const hit = player.getBlockFromViewDirection({ maxDistance: 8 });
        if (hit) { try { hit.block.setType("minecraft:air"); } catch (e) {} }
        else { try { player.onScreenDisplay.setActionBar("§7Kırmak için bir bloğa bak"); } catch (e) {} }
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
        // "tek kullanim" hissi + kardes spam'ini onlemek icin 3 sn bekleme.
        const now = system.currentTick;
        const cd = player.getDynamicProperty("stnt:au_cd") || 0;
        if (now < cd) { try { player.onScreenDisplay.setActionBar("§7Rapor hazırlanıyor... birazdan"); } catch (e) {} break; }
        const hs = player.getEntitiesFromViewDirection({ maxDistance: 30 });
        if (hs.length) {
          try { hs[0].entity.applyDamage(1000); } catch (e) {}
          try { player.setDynamicProperty("stnt:au_cd", now + 60); } catch (e) {}
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
          let placed = 0;
          // iki nokta arasi kutu doldurulur; noktalar farkli yukseklikteyse
          // dikey de dolar -> otomatik DUVAR (aralarindaki yuksekligi orer).
          for (let x = Math.min(x1, x2); x <= Math.max(x1, x2) && placed < 4096; x++)
            for (let y = Math.min(y1, y2); y <= Math.max(y1, y2) && placed < 4096; y++)
              for (let z = Math.min(z1, z2); z <= Math.max(z1, z2) && placed < 4096; z++) {
                try { const b = dim.getBlock({ x, y, z }); if (b && b.typeId === "minecraft:air") { b.setType(fill); placed++; } } catch (e) {}
              }
          try { player.onScreenDisplay.setActionBar(`§a${placed} blok dolduruldu (§b${fill.replace("minecraft:", "")}§a)`); } catch (e) {}
        }
        break;
      }
    }
  } catch (e) {}
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
        ev.hurtEntity.applyDamage(ev.hurtEntity.typeId === "minecraft:player" ? 25 : 1000);
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
function tryMorph(pl, target) {
  const evName = target ? MORPH_MAP[target.typeId] : undefined;
  if (!evName) {
    system.run(() => { try { pl.onScreenDisplay.setActionBar("§7Bu yaratığa dönüşülemiyor"); } catch (e) {} });
    return;
  }
  system.run(() => {
    try {
      pl.triggerEvent(evName);
      pl.onScreenDisplay.setActionBar("§aDönüştün! Boşluğa sağ tık → insana dön.");
      spray(pl.dimension, pl.location, "minecraft:portal_particle", 20, 1.5);
    } catch (e) {}
  });
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
// Pasif olanlar surekli yenilenir; aktif olanlar comelme (sneak) ile tetiklenir
// (3 sn bekleme). Sadece gorunum degil -> morph anlamli ve eglenceli.
const morphCd = new Map();
system.runInterval(() => {
  const now = system.currentTick;
  for (const p of world.getPlayers()) {
    let m;
    try { m = p.getProperty("st:morph"); } catch (e) { continue; }
    if (typeof m !== "number" || m === 0) continue;
    const dim = p.dimension;
    try {                                           // pasif yetenekler
      if (m === 15 || m === 10) p.addEffect("jump_boost", 20, { amplifier: 2, showParticles: false });   // slime/orumcek ziplar
      else if (m === 9 || m === 12) p.addEffect("slow_falling", 20, { amplifier: 0, showParticles: false }); // tavuk/allay yavas duser
      else if (m === 5) p.addEffect("resistance", 20, { amplifier: 1, showParticles: false });            // demir golem dayanikli
      else if (m === 6) p.addEffect("speed", 20, { amplifier: 1, showParticles: false });                 // kurt hizli
    } catch (e) {}
    if (p.isSneaking && (morphCd.get(p.id) || 0) <= now) {   // aktif yetenek (comel)
      try {
        if (m === 1) {                              // creeper: comel -> patla (blok kirmaz)
          const l = p.location;
          dim.createExplosion({ x: l.x, y: l.y + 0.5, z: l.z }, 3, { breaksBlocks: false, causesFire: false });
          spray(dim, l, "minecraft:large_explosion", 1, 0);
          morphCd.set(p.id, now + 80);
        } else if (m === 4) {                       // enderman: comel -> baktigin yere isinlan
          const hit = p.getBlockFromViewDirection({ maxDistance: 48 });
          if (hit) {
            p.teleport({ x: hit.block.location.x + 0.5, y: hit.block.location.y + 1, z: hit.block.location.z + 0.5 });
            spray(dim, p.location, "minecraft:portal_particle", 20, 1);
          }
          morphCd.set(p.id, now + 30);
        } else if (m === 14) {                      // ghast: comel -> baktigin yere ates topu
          const hit = p.getBlockFromViewDirection({ maxDistance: 40 });
          const v = p.getViewDirection(), s = p.getHeadLocation();
          const t = hit ? hit.block.location : { x: s.x + v.x * 20, y: s.y + v.y * 20, z: s.z + v.z * 20 };
          dim.createExplosion({ x: t.x + 0.5, y: t.y + 0.5, z: t.z + 0.5 }, 3, { breaksBlocks: false, causesFire: true });
          morphCd.set(p.id, now + 60);
        } else if (m === 12) {                      // allay: comel -> yukari suzul
          p.applyKnockback(0, 0, 0, 1.0);
          morphCd.set(p.id, now + 20);
        }
      } catch (e) {}
    }
  }
}, 5);

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
function chain(dim, c) {
  const R = 5;
  const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
  let dx = -R, found = 0;
  const job = system.runInterval(() => {
    for (let slice = 0; slice < 2 && dx <= R; slice++, dx++) {
      for (let dy = -R; dy <= R; dy++) {
        for (let dz = -R; dz <= R; dz++) {
          if (found >= 30) { system.clearRun(job); return; }
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
    if (dx > R) system.clearRun(job);
  }, 1);
}

function rnd(n) { return (Math.random() - 0.5) * n; }

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
        spray(dim, c, s.delta < 0 ? "minecraft:portal_particle" : "minecraft:totem_particle", 40, 3);
        break;
      }

      case "launch": {
        dim.createExplosion(c, 0.1, { breaksBlocks: false, causesFire: false });
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            if (e.typeId === "minecraft:player") {
              e.applyKnockback({ x: rnd(0.6), z: rnd(0.6) }, s.force * 1.2);
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
        try { dim.setWeather(s.weather, 24000); } catch (e) {}
        spray(dim, c, "minecraft:water_evaporation_actor_emitter", 40, 4);
        break;

      case "time":
        // AyTntEntity.java: yalnizca GUNDUZSE geceye cevirir (tod < 12300).
        // Kosulsuz set etmek geceyi de basa sariyordu.
        try {
          const tod = world.getTimeOfDay();
          if (tod < 12300) world.setTimeOfDay(s.time);
        } catch (e) {
          try { world.setTimeOfDay(s.time); } catch (e2) {}
        }
        spray(dim, c, "minecraft:end_rod", 60, 4);
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
        //  - Tum OYUNCULAR (yaricap sinirli degil), patlatan haric
        //  - SLOWNESS amp 6 + MINING_FATIGUE amp 4, 600 tick
        //  - Gorsel patlama: guc 1.0, blok hasari yok
        // Bedrock'ta setFrozenTicks karsiligi yok - gercek donma gorseli eksik.
        const ticks = (s.freeze_seconds ?? 30) * 20;
        for (const p of dim.getPlayers()) {
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
            if (s.clear) { for (const ef of e.getEffects()) { try { e.removeEffect(ef.typeId); } catch (x) {} } }
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
        if (s.weather) { try { dim.setWeather(s.weather, s.weatherTicks || 12000); } catch (e) {} }
        if (s.power) { try { dim.createExplosion(c, s.power, { breaksBlocks: false, causesFire: false }); } catch (e) {} }
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
              if (bx * bx + by * by + bz * bz > r * r) continue;
              try {
                const b = dim.getBlock({ x: cx + bx, y: cy + by, z: cz + bz }); if (!b) continue;
                const t = b.typeId;
                if (t === "minecraft:air") continue;
                if (s.skipBedrock && t === "minecraft:bedrock") continue;
                if (s.filter) {
                  const ok = Array.isArray(s.filter) ? s.filter.some(f => t.includes(f)) : t.includes(s.filter);
                  if (!ok) continue;
                }
                b.setType("minecraft:air"); d++;
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
          while (px <= r && d < per) {
            for (let py = -r; py <= r; py++) for (let pz = -r; pz <= r; pz++) {
              if (px * px + py * py + pz * pz > r * r) continue;
              try {
                const b = dim.getBlock({ x: cx + px, y: cy + py, z: cz + pz }); if (!b) continue;
                const t = b.typeId;
                if (s.onlyAir && t !== "minecraft:air") continue;
                if (!s.onlyAir && t === "minecraft:air") continue;
                if (t === "minecraft:bedrock") continue;
                b.setType(s.block); d++;
              } catch (e) {}
            }
            px++;
          }
          if (px > r) system.clearRun(job);
        }, 1);
        if (s.tempSeconds) {
          system.runTimeout(() => {
            let ux = -r;
            const ujob = system.runInterval(() => {
              let d = 0;
              while (ux <= r && d < per) {
                for (let uy = -r; uy <= r; uy++) for (let uz = -r; uz <= r; uz++) {
                  if (ux * ux + uy * uy + uz * uz > r * r) continue;
                  try {
                    const b = dim.getBlock({ x: cx + ux, y: cy + uy, z: cz + uz });
                    if (b && b.typeId === s.block) { b.setType("minecraft:air"); d++; }
                  } catch (e) {}
                }
                ux++;
              }
              if (ux > r) system.clearRun(ujob);
            }, 1);
          }, s.tempSeconds * 20);
        }
        spray(dim, c, s.particle || "minecraft:crop_growth_emitter", 40, 4);
        break;
      }

      // ---- blok donusturme, konum-tabanli palet (rainbow, makarna, seker, ...)
      case "transform": {
        const r = s.radius, per = s.perTick || 900, pal = s.palette;
        const cx = Math.floor(c.x), cy = Math.floor(c.y), cz = Math.floor(c.z);
        let tx = -r;
        const job = system.runInterval(() => {
          let d = 0;
          while (tx <= r && d < per) {
            for (let ty = -r; ty <= r; ty++) for (let tz = -r; tz <= r; tz++) {
              if (tx * tx + ty * ty + tz * tz > r * r) continue;
              try {
                const b = dim.getBlock({ x: cx + tx, y: cy + ty, z: cz + tz }); if (!b) continue;
                const t = b.typeId;
                if (t === "minecraft:air" || t === "minecraft:bedrock") continue;
                b.setType(pal[Math.abs(tx * 7 + ty * 13 + tz * 17) % pal.length]); d++;
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
              e.applyKnockback({ x: dx / len * 0.6, z: dz / len * 0.6 }, 0.5);
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
        spray(dim, c, "minecraft:portal_particle", 50, 5);
        break;
      }
    }
  } catch (e) {
    console.warn(`[SuperTNT] ${short} patlamasi basarisiz: ${e}`);
  }
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
        try { p.teleport({ x: 100, y: 70, z: 0 }, { dimension: world.getDimension("minecraft:the_end") }); } catch (e) {}
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
    // Girises hatirlatmasi: Kucultme/Buyutme KAMERASI icin deneysel ayarlar.
    // Bu bilgi ucuncu kez burada gorunur (paket aciklamasi + kucululunce anlik
    // uyari da var) \u2014 kimse hangi bayragi acacagini unutmasin.
    try {
      ev.player.sendMessage(
        "\u00A77Not: \u00A7fKucultme/Buyutme kamerasi icin dunya ayarlarinda " +
        "\u00A7aBeta APIs\u00A7f + \u00A7aCreator Cameras\u00A7f (Deneyler) acik olmali. " +
        "\u00A77Kapaliyken gerisi normal calisir.");
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
function saveOwners() { try { world.setDynamicProperty(OWNER_PROP, JSON.stringify(owners)); } catch (e) {} }
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

// ---- Kucultme/Buyutme POV kamerasi (DENEYSEL — dunyada "Beta APIs" +
// "Experimental Creator Cameras" acik olmalidir). Boyut normalden (2) farkliysa
// kamerayi OLCEKLI goz yuksekligine surer: kucukken goz yere yakin -> dunya DEV
// gorunur; buyukken goz yukarida -> dunya kucuk gorunur. Her tick oyuncunun
// konumu + bakis acisiyla guncellenir (minecraft:free serbest kamera).
// GRACEFUL DEGRADATION: bayraklar KAPALIYSA/Kamera API yoksa setCamera hata
// atar -> try ile YUTULUR; kamera oldugu gibi kalir, mod'un geri kalani ve
// gorsel olcek (render-scale) calismaya DEVAM eder. Normale (2) donunce kamera
// bir kez temizlenir -> normal ilk-sahis geri gelir.
const camState = new Map();   // playerId -> kamera surulen son size (yoksa hic surmedik)
const camWarned = new Set();  // bayrak uyarisi oyuncu basina bir kez
// Kucultme/Buyutme kamerasi calismasi icin GEREKEN deneysel ayarlar. Bunu 3
// yerde hatirlatiriz (bkz. giris mesaji + paket aciklamasi): en onemlisi
// asagida — oyuncu KUCULUP kamera calismayinca tam o an gosterilir (en isabetli).
const CAM_FLAGS_HINT =
  "§e§l[Süper TNT] Küçülme/Büyüme kamerası kapalı!§r\n" +
  "§fAçmak için: §bDünyayı Düzenle → Ayarlar → Deneyler (Experiments)§f bölümünde\n" +
  "§a‘Beta APIs’§f ve §a‘Creator Cameras / Yaratıcı Kameralar’§f seçeneklerini aç,\n" +
  "§7sonra dünyayı yeniden yükle. (Kapalıyken oyunun geri kalanı normal çalışır.)";
function warnCamFlags(p) {
  if (camWarned.has(p.id)) return;                   // spam yok — oyuncu basina bir kez
  camWarned.add(p.id);
  try { p.sendMessage(CAM_FLAGS_HINT); } catch (e) {}
}
function clearCam(p) {         // kamerayi guvenle birak (normal ilk-sahis geri gelir)
  try { p.camera.clear(); } catch (e) {}
  camState.set(p.id, 2);
}
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    let sz;
    try { sz = p.getProperty("st:size"); } catch (e) { continue; }  // ozellik yoksa dokunma
    if (typeof sz !== "number") sz = 2;
    const prev = camState.get(p.id);
    if (sz === 2) {                                 // normal boyut
      // SADECE daha once kamera surdukse temizle -> hic kuculmemis oyuncunun
      // varsayilan kamerasina asla dokunma (gereksiz clear/titreme olmaz).
      if (prev !== undefined && prev !== 2) clearCam(p);
      continue;
    }
    // Boyut normalden farkli -> kamerayi surmeye CALIS.
    // (1) Kamera API'si hic yoksa (Beta APIs kapali) -> ROTASYONA gerek yok,
    //     hemen hatirlat ve gec. Boylece "oyuncu kuculdu ama kamera degismedi"
    //     durumu her zaman aciklanir.
    if (!p.camera || typeof p.camera.setCamera !== "function") { warnCamFlags(p); continue; }
    const scale = SIZE_SCALES[sz] || SIZE_SCALES[String(sz)] || 1;
    if (typeof scale !== "number" || scale <= 0) continue;
    const loc = p.location;
    let rot;
    try { rot = p.getRotation(); } catch (e) { rot = null; }
    if (!loc || !rot || typeof rot.x !== "number" || typeof rot.y !== "number") continue;  // gecici
    try {
      p.camera.setCamera("minecraft:free", {
        location: { x: loc.x, y: loc.y + 1.62 * scale, z: loc.z },  // goz ~1.62 blok
        rotation: { x: rot.x, y: rot.y },
      });
      camState.set(p.id, sz);
    } catch (e) {
      // (2) API var ama deneysel "Creator Cameras" kapali -> setCamera hata verir
      warnCamFlags(p);
    }
  }
}, 1);

// ---- Portal: AYNI RENK iki blok birbirine isinlar. Kayit global bir dunya
// ozelliginde (herkes icin gecerli); portal blogu KONUNCA eklenir, KIRILINCA
// silinir. Boylece hem Portal Silahi hem elle konan bloklar calisir.
const portalCd = new Map();
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
    const l = p.location, d = p.dimension.id;
    let on = null;                                   // uzerinde durdugun portal
    for (const P of all) {
      if (P.d === d && Math.abs(l.x - (P.x + 0.5)) < 1 && Math.abs(l.y - P.y) < 1.6 && Math.abs(l.z - (P.z + 0.5)) < 1) { on = P; break; }
    }
    if (!on) continue;
    let best = null, bestDist = Infinity;             // ayni renk EN YAKIN es
    for (const Q of all) {
      if (Q === on || Q.c !== on.c) continue;
      const dx = Q.x - on.x, dy = Q.y - on.y, dz = Q.z - on.z, dist = dx * dx + dy * dy + dz * dz;
      if (dist < bestDist) { bestDist = dist; best = Q; }
    }
    if (!best) continue;
    try {
      p.teleport({ x: best.x + 0.5, y: best.y + 1, z: best.z + 0.5 }, { dimension: world.getDimension(best.d) });
      spray(p.dimension, { x: best.x + 0.5, y: best.y + 1, z: best.z + 0.5 }, "minecraft:portal_particle", 20, 1);
    } catch (e) {}
    portalCd.set(p.id, now + 40);                     // 2 sn - ileri-geri titremeyi onler
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

// ---- Yakinlik Mayini: kurulunca 2 sn (40 tick) arm gecikmesi, sonra 10
// tick'te bir 2.5 blok tarar; canli yaklasirsa patlar (kacis penceresi arm).
const MINE_PROP = "stnt:mines";
let mines = {};
function loadMines() { try { const r = world.getDynamicProperty(MINE_PROP); if (typeof r === "string") mines = JSON.parse(r); } catch (e) { mines = {}; } }
function saveMines() { try { world.setDynamicProperty(MINE_PROP, JSON.stringify(mines)); } catch (e) {} }
try { world.afterEvents.worldLoad.subscribe(() => loadMines()); } catch (e) {}
loadMines();   // modul degerlendirmesinde bir kez; worldLoad reload icin yedek
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (!b || b.typeId !== "stnt:yakinlik_mayini") return;
  mines[pkey(b.dimension.id, b.location)] = { dim: b.dimension.id, x: Math.floor(b.location.x), y: Math.floor(b.location.y), z: Math.floor(b.location.z), armed: system.currentTick + 40 };
  saveMines();
  try { ev.player.onScreenDisplay.setActionBar("§cMayın kuruluyor — 2 saniye içinde kaç!"); } catch (e) {}
});
world.afterEvents.playerBreakBlock.subscribe((ev) => {
  const k = pkey(ev.dimension.id, ev.block.location);
  if (mines[k]) { delete mines[k]; saveMines(); }
});
system.runInterval(() => {
  const now = system.currentTick;
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
'''

if __name__ == "__main__":
    build()
