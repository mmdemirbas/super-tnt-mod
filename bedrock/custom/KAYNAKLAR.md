# Bu klasördeki varlıkların kaynağı

`bedrock/custom/` altındaki dosyalar `build.py` tarafından üretilmez, elle
konur ve olduğu gibi pakete kopyalanır. Nereden geldikleri burada yazılı olsun:
bir varlığın kaynağını bilmeden ne güncellenebilir ne de paylaşılabilir.

| Dosya | Kaynak |
|---|---|
| `ender_send.geo.json`, `ender_send.animation.json` | Bu proje için elle yazıldı (üç kafalı özgün model). |
| `mutant_warden.geo.json`, `mutant_warden.animation.json`, `mutant_warden.mirror.json`, `mutant_warden_tex/` | **Üçüncü taraf.** `ibu_craft:mutant_warden` — indirilen paketten (`.mcwork/dl/luzaluza_mutant_warden_INSPECT`) alındı, kimlikleri `stnt_` önekiyle yeniden adlandırıldı. |

## Mutant Warden hakkında

Model, 11 animasyon ve altı katmanlı doku üçüncü tarafa ait; bu paket ticari
değil, ev içi kullanım için. Yeniden dağıtılacak olursa **önce yapımcısının
izni alınmalı** ve adı burada anılmalı.

Kimlikler neden yeniden adlandırıldı: özgün paket de aynı tablette kuruluysa
`geometry.mutant_warden` ve `animation.mutant_warden.*` iki kez tanımlanır,
hangisinin kazandığı belirsizdir. `stnt_` öneki bu çakışmayı ortadan kaldırır.
Vanilla adları (`controller.render.warden*`, `controller.animation.warden.*`,
`warden` / `warden_bioluminescent_layer` materyalleri) **değiştirilmedi** —
onları Minecraft kendi kaynağından verir, pakete konmazlar.

Yeniden üretmek için: `python3 tmp/port_warden.py` benzeri bir dönüştürme,
kaynak klasör + `stnt_` yeniden adlandırma. Dönüştürmenin doğruluğunu
`check_pack.py` → `check_client_entities()` denetler: `RP/entity/*.json`
içindeki her geometry / doku / animasyon adının ya vanilla'da bulunması ya da
pakette ship edilmesi gerekir. Tek bir yerde yeniden adlandırmayı unutmak
modeli sessizce görünmez yapar — Bedrock bu durumda hata vermez.
