---
title: Katalog
order: 20
summary: Paketteki her TNT, blok, eşya, canavar ve kılık — bedrock/build.py listelerinden üretilir.
---

> [!TLDR]
> Sürüm 1.51.0: 70 TNT, 51 blok, 53 eşya, 4 canavar, 135 kılık. Tarifte `M` malzeme, ortadaki `T` normal TNT: 8 malzeme + 1 TNT = 1 özel TNT.

## TNT'ler {#tnt}

| TNT | Ne yapar | Tarif malzemesi |
|---|---|---|
| Elmas TNT | 2.5x patlama gücü - dev kraterler açar | diamond |
| Zıplatan TNT | Yakındaki HER ŞEYİ gökyüzüne fırlatır! Blok hasarı yoktur. | slime ball |
| Zeynep TNT | Lacivert TNT — patlayınca etrafa boş kağıtlar saçar! | paper |
| Çoklu Zeynep TNT | Patladığında etrafa 'Zeynep' isimli köylüler saçar. | emerald |
| Abi TNT | Patladığında etrafa 'Abi' isimli köylüler saçar. | emerald |
| Anne TNT | Patladığında etrafa 'Anne' isimli köylüler saçar. | emerald |
| Baba TNT | Patladığında etrafa 'Baba' isimli köylüler saçar. | emerald |
| Bebek TNT | Patladığında etrafa 'Bebek' isimli yavru köylüler saçar. | emerald |
| Bulut TNT | 1 dakika yağmur başlatır ve gökyüzünü bulutlandırır. | white wool |
| Ay TNT | Gündüzse güneşi aya dönüştürür — gece olur. | glowstone dust |
| Kalp TNT | Yakındaki oyunculara can yenileme + kalkan verir, efektleri ve boyutu sıfırlar. | gold ingot |
| Buz TNT | 14 blok yarıçapı buzla kaplar. O alandaki herkesi (patlatan hariç) 30 sn dondurur. 30 sn yağış başlar (soğuk biyomda kar). | packed ice |
| Küçültme TNT | Yakındaki oyuncuları minicik yapar! Kalp TNT ile eski boyutuna dönersin. | amethyst shard |
| Büyütme TNT | Yakındaki oyuncuları dev yapar! Kalp TNT ile eski boyutuna dönersin. | pumpkin |
| Çizgi TNT | Etrafa kağıt, mürekkep ve tüy saçar — kendi defterini topla! | paper |
| Zeynep Redstone TNT | Dev patlama — patlatan hariç yakındaki herkesi yener! Dikkatli kullan. | redstone block |
| Altın TNT | Merkez patlama + etrafa 5 küçük patlama dalgası saçar. | gold ingot |
| Zümrüt TNT | Zümrüt, elmas, altın ve lapis yağdırır. | emerald |
| Şimşek TNT | 20 yıldırım çağırır. Yıldırım yakar ve öldürebilir — patlattıktan sonra uzaklaş! | lightning rod |
| Nükleer TNT | Dev patlama + radyasyon (wither) hasarı — öldürebilir! | emerald |
| Görünmez TNT | Taş kılığında — kimse fark etmez! | stone |
| Redstone TNT | Çok güçlü patlama; 20 blok yarıçapındaki oyunculara hız ve güç verir. | redstone block |
| Maden TNT | Patladığında her işlenmiş madenden 10 tane saçar. | iron ingot |
| Çalışma Tezgahı TNT | Etrafa altın kasklar, netherite kılıçlar, demir baltalar ve bir netherite külçesi saçar. | crafting table |
| Dünya TNT | Etrafa bir sürü 'dünya' (ender pearl, toprak, çim, su) saçar. | grass block |
| Gökkuşağı TNT | Gerçeğinden bile güzel renkli yünler saçar! | white wool |
| Zümrüt Yağmuru TNT | Patladığında etrafa zümrüt blokları saçar. | emerald block |
| 200 TL TNT | Patladığında etrafa bir sürü kağıt para saçar. | paper |
| Kuruş TNT | Patladığında etrafa bir sürü madeni para saçar. | gold nugget |
| Pasta TNT | Patladığında etrafa pastalar saçar. | cake |
| Zebra TNT | Siyah-beyaz çizgili — kağıt saçar! | paper |
| Harf TNT | Etrafa kağıt saçar. | paper |
| Zehir TNT | Yakındaki herkese yavaşlatma ve zehir verir. | spider eye |
| Zeynep Komut TNT | Yakındaki oyunculara tüm güçlü efektleri ve 10 Nether Yıldızı verir. | nether star |
| Pırt TNT | Etrafa pırt kokusu (yeşil bulut) saçar — ufak bulantı verir. | brown mushroom |
| Temizleyici TNT | Tüm efektleri ve boyut değişikliklerini temizler. | milk bucket |
| Yağmur TNT | 1 dakika yağmur başlatır. | water bucket |
| Şimşek Yağmuru TNT | Yakına 30 yıldırım yağdırır ve 1 dakika fırtına başlatır. Yıldırım yakar ve canlıyı öldürebilir — uzakta dur! | lightning rod |
| Güneş TNT | Geceyse ayı güneşe dönüştürür — gündüz olur. | glowstone |
| Virüs TNT | Bir sürü virüs saçar... ama hepsi yok olur. Aslında hiçbir şey olmaz. | slime ball |
| Kaya Katmanı TNT | 25 blok yarıçapında HER ŞEYİ yok eder - bedrock dahil! | bedrock |
| Cam TNT | Patlayınca 30 blok yarıçapındaki tüm cam bloklarını kırar! | glass |
| Kıyamet TNT | Her şeyi yok eder ve tüm oyuncuları öldürür — sen de öldün. | wither skeleton skull |
| Dondurucu TNT | 9 blok yarıçapını buzla kaplar. | ice |
| Su TNT | Ateşleri söndürür ve geçici su birikintileri bırakır (10 sn). | water bucket |
| Ölümcül Su TNT | 15 blok yarıçapını suyla doldurur (30 sn sonra kurur). | water bucket |
| Mob Dondurucu TNT | 14 blok yarıçapını geçici buza çevirir. | blue ice |
| Nether TNT | Patlayınca büyük netherrack adası yaratır! | netherrack |
| End TNT | Patlayınca end stone adası yaratır! | end stone |
| Gökkuşağı Yün TNT | Blokları renkli yüne dönüştürür (20 blok yarıçap). | white wool |
| Makarna TNT | Blokları yiyecek görünümlü bloklara dönüştürür - afiyet olsun! | wheat |
| Şeker TNT | Çikolata ve şeker dünyası yaratır + hız ve zıplama! (İnerken dikkat.) | sugar |
| Gülen Yüz TNT | Çıngırak sesiyle dünyayı sarıya boyar! 12 blok yarıçap. | yellow dye |
| Karışık Kuruşuk TNT | Dünyayı ezik büzük yapar — siyah/kahverengi/gri beton. | gravel |
| Elmas Diyarı TNT | 18 blok yarıçapındaki dünyayı elmas blokuna çevirir. | diamond block |
| Mıknatıs TNT | 3 saniye boyunca yakındaki her şeyi çeker, sonra BOOM! | iron ingot |
| Takas TNT | Yakındaki tüm canlıların konumlarını rastgele karıştırır! | ender pearl |
| Güneş Kristal TNT | 15 End kristali yaratır. DİKKAT: kristale vurursan büyük patlar! | end crystal |
| Uyku TNT | Patlatan dışındaki herkesi 30 sn 'uyutur': yavaşlama, körlük, bulantı, güçsüzlük, kazma yorgunluğu. | pink wool |
| Gizli TNT | Cam kılığında — 25 blok yarıçapında patlatan hariç her canlıyı anında öldürür! | glass |
| Mağara TNT | Yer altında mağara oyar ve korkunç yaratıklar doğurur! | stone |
| Küp TNT | Yerin içine doğru büyük bir küp şeklinde kazı yapar! | stone |
| Lego TNT | Blokları renkli Lego tuğlalarına dönüştürür! | brick |
| Odun TNT | Yakındaki ağaçları yok eder (gövde ve yapraklar). | oak log |
| Yerçekimi TNT | Yakındaki canlıları 10 sn havaya kaldırır — inerken dikkat! | feather |
| Komut TNT | 20 blok yarıçapındaki her şeyi yok eder. | command block |
| Dağ TNT | Etrafa taştan bir tepe yükseltir! | stone |
| Elmas Zırh TNT | 40 blok yarıçapındaki TÜM canlıları yok eder — dev patlama! | diamond block |
| Üreyen TNT | Güçlü bir patlamayla dünyayı sarsar! | tnt |
| Yürüyen TNT | Büyük bir patlama yapar! | tnt |

## Bloklar {#bloklar}

| Blok | Ne yapar |
|---|---|
| Hayalet Blok | Çim gibi görünür ama içinden geçilir — tuzak! |
| Tahta Hayalet Blok | Tahta gibi görünür ama içinden geçilir — tuzak! |
| Ayna | Karanlıkta parlayan dekoratif ayna. |
| Doğru Altın Plaka | Altın plaka gibi görünür — ama tamamen zararsız. |
| Yanlış Altın Plaka | Altın plaka gibi görünür — üstüne basan anında ölür! |
| Işık Bombası | Çok parlak ışık saçan dekoratif blok. |
| Zehir Toprağı | Zehirli toprak — üstüne basan zehirlenir ve zarar görür! |
| ? Bloğu | Kır — içinden rastgele bir şey çıkar! |
| Herobrine Çağırıcı | Yerleştir — korkunç yaratıklar çağırır! |
| End Kapısı | Üstüne çık — End boyutuna ışınlanırsın! |
| Sahip Kapısı | Sadece koyan kişi açabilir — başkası geçemez! |
| Kilitli Sandık | Sadece koyan kişi açabilir — başkası açamaz! |
| Şifreli Sandık | Şifre koy — doğru şifreyi giren açar! (Şifre ekranda görünür.) |
| Sahte TNT | TROL: pasta gibi görünür — kırınca ya da 'yiyince' sadece SENİ patlatır! Blok hasarı yok. |
| Yakınlık Mayını | Yaklaşan olursa patlar! Kurulunca 2 saniye içinde kaç. Blokları da yıkar. |
| Patlayıcı Kum | TUZAK: normal kum gibi görünür — kazan TNT gibi patlar! Blokları yıkar; yakındaki TNT'leri ve patlayıcı kumları da zincirleme patlatır. Ortaya barut, kenarlara kum ile yapılır. |
| Tükenmezlik Örsü | Elinde bir eşyayla dokun — o eşya TÜKENMEZ olur: yığın hep dolu kalır, alet ve zırh yıpranmaz, totem bitmez. Ok, blok, yiyecek... her eşyada çalışır. 8 elmas + örs ile yapılır. |

Lego tuğlaları 16 renkte gelir (Beyaz, Turuncu, Eflatun, Açık Mavi, Sarı, Fıstık Yeşili, Pembe, Gri, Açık Gri, Camgöbeği, Mor, Mavi, Kahverengi, Yeşil, Kırmızı, Siyah); Lego TNT ve Çizim Eşyası bunlardan yapı kurar.

## Eşyalar {#esyalar}

| Eşya | Ne yapar |
|---|---|
| Acılı Cips | Ye ve 20 sn Hız III kazan! |
| Sağlık İksiri | İç — canın 200 olur ve tamamen dolar! 10 dakika sürer, sonra normale döner. |
| Can Artırıcı | İç — canın 1044 olur ve SÜREKLİ dolu kalır. Süresi yok, hiç bitmez; çıkıp girsen de durur. Vazgeçersen Temizleyici TNT ile normale dönersin. |
| Blok Kılığı | Bu elindeyken bir blok KIR — o bloğa dönüşürsün. Blokken bir yere basılı tut, oraya blok gibi yerleşirsin. Gökyüzüne bakıp basılı tut, insana dönersin. Saklambaç için birebir! |
| İsim Değiştirme | Basılı tut — çıkan kutuya yeni ismini yaz, rengini seç. Diğer oyuncular tepende o ismi görür. En fazla 20 harf. Kutuyu boş bırakıp onaylarsan gerçek ismine dönersin. Dönüşmüşken isim zaten gizlidir; insana dönünce yeni ismin görünür. |
| Mega Gübre | Bir fidana sağ tıkla — gövdesi 100 blok, yapraklarıyla 113 blok yüksekliğinde DEV bir ağaç büyür! Tepesi 45 blok geniş. Gözünün önünde, aşağıdan yukarı büyür. Toprağa tıklarsan meşe olur; üstünde 113 blok boş yer yoksa büyümez. |
| Ses Saldırısı | Sağ tıkla — Warden gibi ses dalgası fırlatır! 24 blok gider, duvarlardan geçer, önüne çıkan herkesi vurup savurur. |
| Ejderha Nefesi | Sağ tıkla — baktığın yere ejderhanın mor nefesini saçar! 5 blok yarıçapında bir bulut olur, 12 saniye kalır ve içindeki herkesin saniyede 1 kalbini götürür. Kendine de zarar verir — bulutun içine girme! |
| Yıldırım Büyüsü | Sağ tıkla — baktığın yere GERÇEK yıldırım çakar! Yakar ve öldürür. |
| Kara Delik | Sağ tıkla — yakındaki her şeyi çeker, kör eder ve hasar verir. |
| Enerji Kristali | Sağ tıkla — baktığın bedrock'u (kaya katmanını) kırar! |
| Lazer Kılıcı | Sağ tıkla — önündeki bloklarda 9 bloklık lazer açar! Lazerin içinde kalan canlıyı da yakar (15 kalp). |
| Kanca | Sağ tıkla — baktığın yere doğru fırlarsın! |
| Dondurucu | Sağ tıkla — baktığın canlıyı dondurur! |
| Koku Bombası | Sağ tıkla — etrafa zehirli koku saçar! |
| Among Us Rapor | Sağ tıkla — baktığın canlıyı anında öldürür! Tek kullanımlık. |
| Hız Eşyası | Kullan ve 60 sn süper hız kazan! |
| Delici Aleti | TROL: sağ tıkla — önündeki her şeyi deler! Eşya düşmez. |
| Lav Kristali | Elde tutarken ateş ve lav sana zarar vermez. |
| Kanlı Kılıç | Vurduğun canlıdan kırmızı damlalar sıçrar! |
| Kalp Baltası | Tek vuruşta mob öldürür; oyunculara ağır hasar! Hiç eskimez — hayatta kalma modunda bile kırılmaz. |
| Gökkuşağı Botları | Giy — adım attığın yerde renkli yün izi bırakırsın! |
| 1 Kuruş | 1 kuruşluk madeni para. |
| 200 TL | 200 TL'lik banknot. |
| Pembe Lego Parçası | Toplanacak pembe Lego parçası. Konulabilen tuğla ayrı bir bloktur. |
| Yeşil Lego Parçası | Toplanacak yeşil Lego parçası. Konulabilen tuğla ayrı bir bloktur. |
| End İncisi | Sağ tıkla — baktığın yere ışınlanırsın! |
| Nether İncisi | Sağ tıkla — baktığın yere ışınlanır, Ateş Çubuğu alırsın! |
| TNT Frizbi | Sağ tıkla — baktığın yerde artı şeklinde yıkım! |
| Craft Baltası | Sağ tıkla — baktığın yeri işaretle, tekrar tıkla arası dolsun. |
| Takım Asası | Bir mob'a dokun — takımına katılır. Aynı takımdakiler birbirine saldırmaz! |
| Dönüşüm Asası | Bir mob'a dokun — o mob olursun! Boşluğa sağ tık — tüm yaratıkların listesi açılır (insana dönmek de orada). Bazılarının özel gücü var: ÇÖMEL (sneak) dene. |
| Küçültme Topu | Sağ tıkla — bir kademe küçülürsün! En küçükte minicik olursun. |
| Büyütme Topu | Sağ tıkla — bir kademe büyürsün! En büyükte dev olursun. |
| Normal Boyut Topu | Sağ tıkla — normal boyutuna dönersin. |
| Eşya Çalmaca | TROL: sağ tıkla — en yakın oyuncunun bir eşyasını çalar! |
| Kontrol Kumandası | Sağ tıkla — yerleştirdiğin tüm TNT'leri uzaktan ateşler! |
| Portal Silahı | Sağ tıkla — bir portal koy, tekrar tıkla ikincisini koy. Aralarında ışınlan! |
| TNT Kask | Giy — sana vurana TNT patlamasıyla karşılık verir! |
| TNT Göğüslük | Giy — sana vurana TNT patlamasıyla karşılık verir! |
| TNT Pantolon | Giy — sana vurana TNT patlamasıyla karşılık verir! |
| TNT Bot | Giy — sana vurana TNT patlamasıyla karşılık verir! |
| Ender Send Yumurtası | Sağ tıkla — DEV bir Ender Send çağırır! 300 can, çok güçlü. |
| Dev Zombi Yumurtası | Sağ tıkla — DEV bir zombi çağırır! 250 can, ağır yumruk. |
| Dev Creeper Yumurtası | Sağ tıkla — DEV bir creeper çağırır! Yaklaşınca kocaman patlar! |
| Mutant Warden Yumurtası | Sağ tıkla — DEV bir Mutant Warden çağırır! 500 can, çok tehlikeli! |
| Sahte Elmas Kılıç | TROL: Gerçek elmas kılıç gibi görünür ve öyle çalışır — ama bir canlıya vurduğun ya da bir blok kırdığın anda Tahta Kılıç'a dönüşür! |
| Sahte Elmas Kazma | TROL: Gerçek elmas kazma gibi görünür ve öyle çalışır — ama bir canlıya vurduğun ya da bir blok kırdığın anda Tahta Kazma'ya dönüşür! |
| Sahte Elmas Balta | TROL: Gerçek elmas balta gibi görünür ve öyle çalışır — ama bir canlıya vurduğun ya da bir blok kırdığın anda Tahta Balta'ya dönüşür! |
| Sahte Elmas Kürek | TROL: Gerçek elmas kürek gibi görünür ve öyle çalışır — ama bir canlıya vurduğun ya da bir blok kırdığın anda Tahta Kürek'e dönüşür! |
| Sahte Elmas Çapa | TROL: Gerçek elmas çapa gibi görünür ve öyle çalışır — ama bir canlıya vurduğun ya da bir blok kırdığın anda Tahta Çapa'ya dönüşür! |
| Ender Çakmağı | Baktığın bloğun üstüne mavi Ender Ateşi yakar. Ateşe giren oyuncu ışınlanır: çoğunlukla başka bir Ender Ateşi'ne, bazen Nether'daki bir ruh ateşine, çok seyrek Nether'daki turuncu ateşe. Nether ateşleri yakar! Gidecek yer yoksa olduğun yerde kalırsın. 64 kullanım. |
| Altın Flint | Baktığın bloğu altın bloğuna çevirir ve üstüne Altın Ateş yakar. Ateş sönmez, su söndürmez, yayılmaz; içine giren oyuncu yanar. Yumrukla kırılır. Bedrock ve sandık gibi bloklar altına dönmez. 64 kullanım; örste Tamir büyüsü basılabilir. |

## Canavarlar {#canavarlar}

| Canavar | Can | Hasar |
|---|---|---|
| Ender Send | 300 | 15 |
| Dev Zombi | 250 | 12 |
| Dev Creeper | 180 | 8 |
| Mutant Warden | 500 | 22 |

## Kılıklar {#kiliklar}

Dönüşüm Asası ile girilen kılıklar; her biri o canlının hareketini ve gücünü verir.

| Grup | Kılıklar |
|---|---|
| Hayvanlar | Demir Golem, Kurt, Domuz, İnek, Tavuk, Allay, Adımlayıcı, Armadillo, Arı, At, Bakır Golem, Deve, Eşek, Gezgin Tüccar, Kardan Adam, Katır, Kedi, Keçi, Koklayıcı, Koyun, Kurbağa, Kutup Ayısı, Köylü, Lama, Mantar İnek, Ocelot, Panda, Papağan, Tavşan, Tilki, Tüccar Laması, Yarasa |
| Su | Axolotl, Balon Balığı, Işıklı Mürekkep Balığı, Kaplumbağa, Morina Balığı, Muhafız, Mürekkep Balığı, Somon, Tropikal Balık, Yaşlı Muhafız, Yunus, İribaş |
| Canavarlar | Creeper, Zombi, İskelet, Enderman, Örümcek, Piglin, Wither İskeleti, Ghast, Slime, Bataklık İskeleti, Blaze, Boğulmuş, Buz İskeleti, Cadı, Endermit, Esinti, Evoker, Gümüş Balığı, Gıcırdak, Hayalet Kuş, Hoglin, Magma Küpü, Mağara Örümceği, Piglin Kabadayı, Ravager, Shulker, Vex, Vindicator, Yağmacı, Zoglin, Zombi At, Zombi Köylü, Zombi Piglin, Çöl Zombisi, İskelet At |
| Devler | Ender Ejderha, Mutlu Ghast, Warden, Wither |
| Süper TNT | Dev Creeper, Dev Zombi, Mutant Warden, Ender Send |
| Bloklar | Taş, Kaldırım Taşı, Toprak, Çim, Kum, Çakıl, Meşe Tahtası, Meşe Kütüğü, Yaprak, Cam, Tuğla, Obsidyen, Elmas Bloku, Altın Bloku, Demir Bloku, Zümrüt Bloku, Lapis Bloku, Kömür Bloku, Redstone Bloku, Ametist, Netherrack, Buz, Kar, Balkabağı, Kabak Feneri, Karpuz, Kitaplık, Çalışma Tezgahı, Fırın, TNT, Saman Balyası, Sünger, Kil, Kuvars Bloku, Sakız Bloku, Bal Bloku, Mercan, Sculk, Kaya Katmanı, Beyaz Yün, Kırmızı Yün, Mavi Yün, Sarı Yün, Yeşil Yün, Zeynep TNT, Elmas TNT, Kalp TNT, Nükleer TNT |
