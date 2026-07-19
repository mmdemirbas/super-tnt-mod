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
VERSION = [1, 8, 0]
MIN_ENGINE = [1, 21, 0]

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
         effect=dict(kind="scatter", spread=8.0, drops=[{"item": "minecraft:paper", "count": 100, "stack": 4}])),
    dict(id="kurus_tnt", tr="Kuruş TNT", en="Coin TNT",
         trtip="Patladığında etrafa bir sürü madeni para saçar.",
         entip="Scatters a lot of coins when it explodes.",
         color=((196, 160, 90), (216, 182, 110), (168, 136, 74)), mat="minecraft:gold_nugget",
         effect=dict(kind="scatter", spread=9.0, drops=[{"item": "minecraft:gold_nugget", "count": 200, "stack": 8}])),
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
        "header": {"name": "Super TNT Mod [BP]",
                   "description": "Super TNT Mod - Bedrock surumu",
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
            {"module_name": "@minecraft/server", "version": "1.14.0"}],
    })
    w(os.path.join(RP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": "Super TNT Mod [RP]",
                   "description": "Super TNT Mod - dokular",
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
    # DECOR blok dokulari (tek yuz, tum yonler ayni)
    for blk in BLOCKS:
        key = f"stnt_{blk['id']}"
        rel = f"textures/blocks/{key}"
        decor_texture(os.path.join(RP, rel + ".png"), blk['color'], blk['kind'])
        terrain[key] = {"textures": rel}
        generated += 1
    w(os.path.join(RP, "textures/terrain_texture.json"),
      {"resource_pack_name": "super_tnt", "texture_name": "atlas.terrain",
       "padding": 8, "num_mip_levels": 4, "texture_data": terrain})

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
            open(os.path.join(pack, f"texts/{lang}.lang"), 'w',
                 encoding='utf-8').write("\n".join(lines) + "\n")

    # ---------- script
    spec = {t['id']: t['effect'] for t in TNTS}
    tips = {t['id']: t['tr'] for t in TNTS}
    tiptext = {t['id']: t['trtip'] for t in TNTS}
    script = SCRIPT_TEMPLATE.replace("__SPEC__", json.dumps(spec, indent=2)) \
                            .replace("__NAMES__", json.dumps(tips, ensure_ascii=False)) \
                            .replace("__TIPS__", json.dumps(tiptext, ensure_ascii=False)) \
                            .replace("__FUSE__", str(FUSE_TICKS))
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
import { world, system, ItemStack } from "@minecraft/server";

const SPEC = __SPEC__;
const NAMES = __NAMES__;
const TIPS = __TIPS__;
const FUSE = __FUSE__;

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
function track(dimId, loc) {
  tracked.add(`${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`);
  if (tracked.size > TRACK_MAX) tracked = new Set([...tracked].slice(-TRACK_MAX));
  saveTracked();
}
function untrack(dimId, loc) {
  tracked.delete(`${dimId}|${Math.floor(loc.x)},${Math.floor(loc.y)},${Math.floor(loc.z)}`);
  saveTracked();
}

world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  const b = ev.block;
  if (b && b.typeId.startsWith("stnt:") && SPEC[b.typeId.slice(5)]) {
    track(b.dimension.id, b.location);
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
const primed = new Map();  // entityId -> { short, left, dim }

function ignite(dim, loc, short, igniterId) {
  const k = key(dim.id, loc);
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
    primed.set(e.id, { short, left: FUSE, e, ig: igniterId });
  } catch (err) {
    // varlik dogmadiysa yerinde patlat - islev kaybolmasin
    system.runTimeout(() => detonate(dim, c, short, igniterId), FUSE);
  }
}

// fitil sayaci
system.runInterval(() => {
  for (const [id, p] of [...primed]) {
    p.left -= 2;
    let loc = null, dim = null;
    try { loc = p.e.location; dim = p.e.dimension; } catch (e) { primed.delete(id); continue; }
    try {
      dim.spawnParticle("minecraft:basic_smoke_particle", { x: loc.x, y: loc.y + 0.7, z: loc.z });
    } catch (e) {}
    if (p.left <= 0) {
      primed.delete(id);
      try { p.e.remove(); } catch (e) {}
      detonate(dim, loc, p.short, p.ig);
    }
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
  chain(dim, c);   // F3 - zinciri kur (patlamadan once, bloklar hala duruyor)
  try {
    switch (s.kind) {
      case "explode":
        dim.createExplosion(c, s.power, { breaksBlocks: true, causesFire: false });
        break;

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
            // Java ayrica olcek degisikligini sifirliyor; Bedrock'ta oyuncu
            // olcegi script'ten hic degistirilemedigi icin bozulmasi da
            // mumkun degil - o adimin Bedrock'ta karsiligi yok.
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

      // ---- aninda oldur, patlatan haric (gizli)
      case "instakill": {
        for (const e of dim.getEntities({ location: c, maxDistance: s.radius })) {
          try {
            if (e.typeId === "minecraft:player" && igniterId && e.id === igniterId) continue;
            e.applyDamage(1000);
          } catch (err) {}
        }
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
                if (s.filter === "glass" && !t.includes("glass")) continue;
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

// Yanlis Altin Plaka: ustunde duran oyuncuyu oldurur (Java'daki tuzak).
system.runInterval(() => {
  for (const p of world.getPlayers()) {
    try {
      const l = p.location;
      const below = p.dimension.getBlock({ x: Math.floor(l.x), y: Math.floor(l.y) - 1, z: Math.floor(l.z) });
      if (below && below.typeId === "stnt:wrong_golden_plate") {
        p.applyDamage(1000);
      }
    } catch (e) {}
  }
}, 5);

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
'''

if __name__ == "__main__":
    build()
