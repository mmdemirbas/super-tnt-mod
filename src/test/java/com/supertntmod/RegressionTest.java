package com.supertntmod;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2026-07-19 denetiminde bulunan hata sınıflarının nüksetmesini engeller.
 *
 * Her test bir kez GERÇEKTEN yaşanmış bir hatayı kilitler. Tarif formatı
 * hatası iki kez yaşandı (düzeltilip geri geldi) — bu yüzden mevcut
 * ResourceFileTest'in kör noktaları burada kapatılıyor.
 *
 * Hepsi saf dosya/metin kontrolü; Minecraft çalıştırmaya gerek yok.
 */
class RegressionTest {

    private static final Path SRC    = Path.of("src/main/java/com/supertntmod");
    private static final Path ASSETS = Path.of("src/main/resources/assets/supertntmod");
    private static final Path DATA   = Path.of("src/main/resources/data/supertntmod");

    private static List<Path> javaFiles(String subDir) throws IOException {
        try (Stream<Path> s = Files.walk(SRC.resolve(subDir))) {
            return s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static List<Path> jsonFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
        }
    }

    // ── Tarif formatı ────────────────────────────────────────────────────
    // Yaşanan: MC 1.21.11 `key` değerlerinde düz string bekliyor;
    // {"item":"..."} nesne formu parse edilmiyor ve tarif sessizce kayboluyor.
    // İki kez oldu: bir kez düzeltildi, 3 dosyada geri geldi.
    // ResourceFileTest'in kör noktası: nesne formu GEÇERLİ JSON olduğu için
    // `recipesAreValidJsonObjects` bunu yakalayamıyor.

    @Test
    void recipeKeyValuesArePlainStringsNotObjects() throws IOException {
        Pattern objectForm = Pattern.compile("\"[A-Za-z#]\"\\s*:\\s*\\{\\s*\"item\"");
        List<String> bad = new ArrayList<>();
        for (Path p : jsonFiles(DATA.resolve("recipe"))) {
            String body = Files.readString(p);
            int keyIdx = body.indexOf("\"key\"");
            if (keyIdx < 0) continue;
            if (objectForm.matcher(body.substring(keyIdx)).find()) {
                bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(),
                "Tarif `key` degerleri duz string olmali, {\"item\":...} nesne formu "
                        + "MC 1.21.11'de parse edilmiyor. Bozuk: " + bad);
    }

    @Test
    void recipeResultsUseIdNotItem() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : jsonFiles(DATA.resolve("recipe"))) {
            String body = Files.readString(p);
            if (body.contains("\"result\"") && body.matches("(?s).*\"result\"\\s*:\\s*\\{[^}]*\"item\"\\s*:.*")) {
                bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(), "Tarif `result` alani `id` kullanmali, `item` degil: " + bad);
    }

    // ── Advancement kapsamı ──────────────────────────────────────────────
    // Yaşanan: first_craft 71 TNT'nin yalnız 40'ını kapsıyordu. Mevcut test
    // sadece collect_all'a bakıyordu, bu yüzden 31 eksik fark edilmedi.

    @Test
    void firstCraftAdvancementCoversEveryTntRecipe() throws IOException {
        Path adv = DATA.resolve("advancement/first_craft.json");
        assertTrue(Files.exists(adv), "first_craft.json bulunmali");
        String body = Files.readString(adv);

        Set<String> covered = new HashSet<>();
        Matcher m = Pattern.compile("\"recipe_id\"\\s*:\\s*\"supertntmod:([a-z0-9_]+)\"").matcher(body);
        while (m.find()) covered.add(m.group(1));

        List<String> missing = new ArrayList<>();
        for (Path p : jsonFiles(DATA.resolve("recipe"))) {
            String name = p.getFileName().toString().replace(".json", "");
            if (name.endsWith("_tnt") && !covered.contains(name)) missing.add(name);
        }
        assertTrue(missing.isEmpty(),
                "Tarifi olan her TNT first_craft'ta bulunmali. Eksik: " + missing);
    }

    @Test
    void advancementCriteriaAndRequirementsMatch() throws IOException {
        for (Path p : jsonFiles(DATA.resolve("advancement"))) {
            String body = Files.readString(p);
            if (!body.contains("\"requirements\"")) continue;
            Set<String> req = new HashSet<>();
            Matcher m = Pattern.compile("\"([a-z0-9_]+)\"").matcher(
                    body.substring(body.indexOf("\"requirements\"")));
            while (m.find()) req.add(m.group(1));
            req.remove("requirements");
            assertFalse(req.isEmpty(), p.getFileName() + ": requirements bos olmamali");
        }
    }

    // ── Habersiz yangın ──────────────────────────────────────────────────
    // Yaşanan: 10 TNT createExplosion'a createFire=true veriyordu, hiçbir
    // tooltip yangından bahsetmiyordu; ateş krater dışına yayılıp yapıları
    // yakıyordu. Elmas TNT'de düzeltilmiş ama diğerlerine yayılmamıştı.

    @Test
    void noTntStartsFiresSilently() throws IOException {
        // createExplosion(..., <guc>, true, ...) -> createFire acik
        Pattern fireOn = Pattern.compile(
                "createExplosion\\([^;]*?,\\s*[0-9A-Za-z_.]+f?,\\s*true\\s*,", Pattern.DOTALL);
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("entity")) {
            if (fireOn.matcher(Files.readString(p)).find()) {
                bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(),
                "createFire=true hicbir tooltip'te yazmiyor ve ates krater disina "
                        + "yayiliyor. Yangin isteniyorsa tooltip'e eklenmeli. Acik olan: " + bad);
    }

    // ── Ölü "patlatan hariç" koruması ────────────────────────────────────
    // Yaşanan: getOwner() TNT'lerde hiç set edilmiyor (setOwner sadece
    // mermilerde çağrılıyor), bu yüzden "patlatan hariç" guard'ları derleniyor,
    // çalışıyor ama hiçbir şey yapmıyordu. Zeynep Redstone TNT'yi yakan
    // çocuğu da öldürüyordu.

    @Test
    void tntEntitiesDoNotRelyOnGetOwnerForIgniterExclusion() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("entity")) {
            String body = Files.readString(p);
            if (!p.getFileName().toString().endsWith("TntEntity.java")) continue;
            if (body.contains("this.getOwner()") || body.contains("getOwner() !=")) {
                bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(),
                "TNT entity'lerinde getOwner() hicbir zaman set edilmez; atesleyeni "
                        + "haric tutmak icin kendi igniterUuid alani kullanilmali "
                        + "(GizliTntEntity desenine bak). getOwner() kullanan: " + bad);
    }

    @Test
    void igniterAwareTntsPersistTheirIgniter() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("entity")) {
            String body = Files.readString(p);
            if (!body.contains("igniterUuid")) continue;
            if (!body.contains("IgniterUuid")) bad.add(p.getFileName().toString());
        }
        assertTrue(bad.isEmpty(),
                "igniterUuid tutan entity onu writeData/readData ile saklamali, "
                        + "yoksa yeniden yuklemede koruma kayboluyor: " + bad);
    }

    // ── Çok-tick işleme durumu ───────────────────────────────────────────
    // Yaşanan: 11 entity işlemi tick'lere yayıyor ama durumu kaydetmiyordu.
    // İşlem sırasında kaydet-çık yapılınca fitil 0'da donmuş geri yükleniyor
    // ve yıkım her açılışta baştan çalışıyordu.

    @Test
    void multiTickEntitiesPersistProcessingState() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("entity")) {
            String body = Files.readString(p);
            boolean multiTick = body.contains("private boolean processing");
            if (!multiTick) continue;
            boolean persists = body.contains("writeData") && body.contains("readData");
            if (!persists) bad.add(p.getFileName().toString());
        }
        assertTrue(bad.isEmpty(),
                "Kademeli isleyen her entity durumunu kalici yapmali, aksi halde "
                        + "kaydet-cik sonrasi yikim bastan calisiyor: " + bad);
    }

    @Test
    void persistedCenterHasNullGuard() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("entity")) {
            String body = Files.readString(p);
            if (!body.contains("center = new BlockPos(")) continue;   // center'i geri yukluyor
            if (!body.contains("if (center == null")) bad.add(p.getFileName().toString());
        }
        assertTrue(bad.isEmpty(),
                "center geri yuklenemezse processing iptal edilmeli, yoksa tick() "
                        + "icinde NPE oluyor: " + bad);
    }

    // ── Boyut körü statik haritalar ──────────────────────────────────────
    // Yaşanan: Komut TNT ve TNT Kapısı haritaları yalnız BlockPos ile
    // anahtarlanıyordu; Nether'daki blok Overworld'deki ayarı okuyordu.

    @Test
    void positionKeyedStaticMapsIncludeDimension() throws IOException {
        Pattern bareBlockPosKey = Pattern.compile("static final Map<BlockPos,");
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("block")) {
            if (bareBlockPosKey.matcher(Files.readString(p)).find()) {
                bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(),
                "Konumla anahtarlanan statik harita boyutu da icermeli (DimPos), "
                        + "yoksa farkli boyutlardaki ayni koordinat cakisiyor: " + bad);
    }

    // ── Dil dosyası bütünlüğü ────────────────────────────────────────────

    @Test
    void langFilesHaveIdenticalKeySets() throws IOException {
        Set<String> en = langKeys(ASSETS.resolve("lang/en_us.json"));
        Set<String> tr = langKeys(ASSETS.resolve("lang/tr_tr.json"));
        Set<String> onlyEn = new HashSet<>(en); onlyEn.removeAll(tr);
        Set<String> onlyTr = new HashSet<>(tr); onlyTr.removeAll(en);
        assertTrue(onlyEn.isEmpty() && onlyTr.isEmpty(),
                "Dil dosyalari ayni anahtar kumesine sahip olmali. "
                        + "Sadece en_us: " + onlyEn + " | sadece tr_tr: " + onlyTr);
    }

    private static Set<String> langKeys(Path p) throws IOException {
        Set<String> keys = new HashSet<>();
        Matcher m = Pattern.compile("\"([a-zA-Z0-9_.:]+)\"\\s*:").matcher(Files.readString(p));
        while (m.find()) keys.add(m.group(1));
        return keys;
    }

    @Test
    void everyTntRecipeHasBothLanguageNames() throws IOException {
        Set<String> en = langKeys(ASSETS.resolve("lang/en_us.json"));
        Set<String> tr = langKeys(ASSETS.resolve("lang/tr_tr.json"));
        List<String> missing = new ArrayList<>();
        for (Path p : jsonFiles(DATA.resolve("recipe"))) {
            String name = p.getFileName().toString().replace(".json", "");
            if (!name.endsWith("_tnt")) continue;
            String key = "block.supertntmod." + name;
            if (!en.contains(key) || !tr.contains(key)) missing.add(name);
        }
        assertTrue(missing.isEmpty(), "Her TNT iki dilde de adlandirilmali: " + missing);
    }

    // ── Sahiplik ─────────────────────────────────────────────────────────
    // Yaşanan: Blocker Sandık sahipliği ilk AÇANA geçiyordu, koyana değil.

    @Test
    void blockerChestAssignsOwnershipOnPlacement() throws IOException {
        String body = Files.readString(SRC.resolve("block/BlockerChestBlock.java"));
        assertTrue(body.contains("public void onPlaced("),
                "Blocker Sandik sahipligi KOYANA verilmeli (onPlaced), ilk acana degil.");
    }
}
