- Küçülüp de elimizdeki deliciyle blokların ortasını deldiğimizde blok kırmızıya dönüşüyor. Orjinal
  haliyle kalsın, kırmızıya dönüşmesin.
- Bloğun deldiğimiz kısmına girebilelim. Şu an boyumuzun sığacağı kadar büyük bir delik açsak bile
  içine girmemizi engelliyor. Bu şekilde delerek blokların içinde tünel açıp ilerleyebilmeliyiz.
- Bir seferde deldiğimiz kısım karakterin o anki boyunun yarısı kadar olacak. Yani iki kez delme
  işlemi yaptığında kendi sığacağı büyüklükte bir delik açmış olacak. Yani delme boyutu, karakterin
  boyutuna göre 1/2 oranında değişecek. Büyükken büyük delik, küçükken küçük delik açacağız.
- Among Us Report eşyası: Eline bu eşyayı alıp bir mob veya oyuncuya tıkladığında o mob veya oyuncu
  anında ölür.
- Şu ana kadar oluşturduğumuz blok türlerinin vs. simgeleri default kalmış. E ile envantere girip de
  arattığımızda simgelerine bakarak ayırt etmemiz mümkün olmuyor. Çünkü hepsi aynı simgeyi
  kullanıyor. Bu simgeleri ayrıştırmamız gerekiyor. Her birine kendisine uygun bir simge tasarla ve
  kullanılmasını sağla. Eğer hali hazırda bunu yapmışsan başka bir sorun var demektir. Çünkü oyunu
  gradle runClient komutuyla çalıştırdığımızda simgeler hala aynı görünüyor. Bunu düzeltmek için ne
  yapılması gerekiyorsa araştır, yap ve doğrula.
- Büyütme bloğunun üzerinde + işareti zaten var. Bu doğru. Küçültme bloğunun üzerinde de - işareti
  olmalı.
- Şu an sanırım en fazla 2 veya 3 defa küçülebiliyoruz. Daha fazla küçülmeyi desteklemeliyiz.
- Karakterimizin koyduğu blokların boyutları da kendi boyutuyla orantılı olmalı. Bu sayede normal
  bir blok içinde delik açtıktan sonra kendisine orada küçük bloklarla bir ev yapabilir.

- İki yeni nesne tanımla. Birisi kırmızı, diğer yeşil top olsun. Kırmızı top küçültme, yeşil top
  büyütme topu olacak. Tıpkı küçültme ve büyütme TNT'si gibi bunlar da bu nesneleri eline alıp da
  atan kişiyi büyütecek veya küçültecek. TNT'lerden farklı olarak etraftaki diğer canlılara etki
  etmeyecekler. Simgelerini de buna uygun şekilde tasarla.
- Küçültme TNT'si ile küçüldüğümüzde artık bazı nesneleri koyamaz hale geliyoruz. Örneğin büyütme
  TNT'sini koymaya çalıştığımızda anlık olarak koyuyor gibi görünüyor, ancak hemen görünmez bir blok
  oluyor. Kırınca da taşa dönüşerek kırılıyor gibi bir his veriyor. Yani küçükken tekrar büyümek
  için büyütme TNT'si kullanamıyoruz. Diğer özel nesnelerimizde de aynı sorun var. Bunu düzelt.
- Özel nesnelerimizin hepsinin isimlerinin ve çevirilerinin eksiksiz ve doğru olduğundan emin ol.
  Bazıları eksik gibi gördüm.
