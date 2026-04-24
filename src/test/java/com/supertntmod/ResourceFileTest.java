package com.supertntmod;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ResourceFileTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supertntmod");
    private static final Path DATA   = Path.of("src/main/resources/data/supertntmod");

    @Test
    void langFilesExistAndAreNonEmpty() throws IOException {
        Path en = ASSETS.resolve("lang/en_us.json");
        Path tr = ASSETS.resolve("lang/tr_tr.json");
        assertTrue(Files.exists(en), "en_us.json must exist");
        assertTrue(Files.exists(tr), "tr_tr.json must exist");
        String enContent = Files.readString(en);
        String trContent = Files.readString(tr);
        assertTrue(enContent.startsWith("{"), "en_us.json must be a JSON object");
        assertTrue(trContent.startsWith("{"), "tr_tr.json must be a JSON object");
        assertTrue(enContent.contains("block.supertntmod"), "en_us.json must contain block keys");
        assertTrue(trContent.contains("block.supertntmod"), "tr_tr.json must contain block keys");
    }

    @Test
    void recipesAreValidJsonObjects() throws IOException {
        Path recipes = DATA.resolve("recipe");
        assertTrue(Files.exists(recipes), "recipe directory must exist");

        try (Stream<Path> files = Files.list(recipes)) {
            List<Path> jsonFiles = files
                    .filter(p -> p.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            assertFalse(jsonFiles.isEmpty(), "must have at least one recipe");
            for (Path file : jsonFiles) {
                String content = Files.readString(file).strip();
                assertTrue(content.startsWith("{"),
                        file.getFileName() + " must start with {");
                assertTrue(content.endsWith("}"),
                        file.getFileName() + " must end with }");
                assertTrue(content.contains("\"type\""),
                        file.getFileName() + " must have a type field");
                assertTrue(content.contains("\"result\""),
                        file.getFileName() + " must have a result field");
            }
        }
    }

    @Test
    void everyItemsJsonHasMatchingItemModel() throws IOException {
        Path items  = ASSETS.resolve("items");
        Path models = ASSETS.resolve("models/item");
        if (!Files.exists(items)) return;

        try (Stream<Path> stream = Files.list(items)) {
            List<String> names = stream
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toList());
            for (String name : names) {
                Path model = models.resolve(name + ".json");
                assertTrue(Files.exists(model),
                        "models/item/" + name + ".json missing for items/" + name + ".json");
            }
        }
    }

    @Test
    void blockstateModelReferencesExist() throws IOException {
        Path blockstates = ASSETS.resolve("blockstates");
        Path models      = ASSETS.resolve("models/block");
        if (!Files.exists(blockstates)) return;

        try (Stream<Path> stream = Files.list(blockstates)) {
            List<Path> files = stream.collect(Collectors.toList());
            for (Path file : files) {
                String content = Files.readString(file);
                // Extract all "supertntmod:block/X" model references
                int idx = 0;
                while ((idx = content.indexOf("\"supertntmod:block/", idx)) != -1) {
                    int start = idx + "\"supertntmod:block/".length();
                    int end   = content.indexOf('"', start);
                    if (end == -1) break;
                    String modelName = content.substring(start, end);
                    Path modelFile = models.resolve(modelName + ".json");
                    assertTrue(Files.exists(modelFile),
                            "models/block/" + modelName + ".json missing (referenced in blockstates/"
                                    + file.getFileName() + ")");
                    idx = end;
                }
            }
        }
    }

    @Test
    void advancementFilesAreValidJson() throws IOException {
        Path advancements = DATA.resolve("advancement");
        assertTrue(Files.exists(advancements), "advancement directory must exist");

        try (Stream<Path> files = Files.list(advancements)) {
            List<Path> jsonFiles = files
                    .filter(p -> p.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            assertFalse(jsonFiles.isEmpty(), "must have at least one advancement");
            for (Path file : jsonFiles) {
                String name = file.getFileName().toString();
                String content = Files.readString(file).strip();
                assertTrue(content.startsWith("{"), name + " must be a JSON object");
                if (!name.equals("root.json")) {
                    assertTrue(content.contains("\"parent\""), name + " must have a parent field");
                    assertTrue(content.contains("\"criteria\""), name + " must have a criteria field");
                    assertTrue(content.contains("\"display\""), name + " must have a display field");
                }
            }
        }
    }

    @Test
    void advancementLangKeysExistInBothLangFiles() throws IOException {
        Path advancements = DATA.resolve("advancement");
        Path en = ASSETS.resolve("lang/en_us.json");
        Path tr = ASSETS.resolve("lang/tr_tr.json");
        String enContent = Files.readString(en);
        String trContent = Files.readString(tr);

        try (Stream<Path> files = Files.list(advancements)) {
            List<String> names = files
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .filter(n -> !n.equals("root"))
                    .collect(Collectors.toList());
            for (String name : names) {
                String titleKey = "advancement.supertntmod." + name + ".title";
                String descKey  = "advancement.supertntmod." + name + ".description";
                assertTrue(enContent.contains(titleKey),
                        "en_us.json missing key: " + titleKey);
                assertTrue(trContent.contains(titleKey),
                        "tr_tr.json missing key: " + titleKey);
                assertTrue(enContent.contains(descKey),
                        "en_us.json missing key: " + descKey);
                assertTrue(trContent.contains(descKey),
                        "tr_tr.json missing key: " + descKey);
            }
        }
    }

    @Test
    void modIdFollowsNamingConvention() {
        String modId = "supertntmod";
        assertTrue(modId.matches("[a-z][a-z0-9_]*"),
                "Mod ID must be lowercase alphanumeric + underscores, no spaces");
    }

    @Test
    void everyTntRecipeResultHasCollectAllCriterion() throws IOException {
        Path recipes = DATA.resolve("recipe");
        Path collectAll = DATA.resolve("advancement/collect_all.json");
        assertTrue(Files.exists(collectAll), "collect_all.json must exist");

        String collectContent = Files.readString(collectAll);

        try (Stream<Path> files = Files.list(recipes)) {
            List<String> tntResults = files
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .filter(name -> name.endsWith("_tnt"))
                    .collect(Collectors.toList());

            for (String name : tntResults) {
                String marker = "\"supertntmod:" + name + "\"";
                assertTrue(collectContent.contains(marker),
                        "collect_all.json missing criterion for TNT: " + name);
            }
        }
    }

    @Test
    void everyBlockLangKeyCorrespondsToRecipeOrEntity() throws IOException {
        Path recipes = DATA.resolve("recipe");
        Path en = ASSETS.resolve("lang/en_us.json");
        Path tr = ASSETS.resolve("lang/tr_tr.json");
        String enContent = Files.readString(en);
        String trContent = Files.readString(tr);

        try (Stream<Path> files = Files.list(recipes)) {
            List<String> tntResults = files
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .filter(name -> name.endsWith("_tnt"))
                    .collect(Collectors.toList());

            for (String name : tntResults) {
                String key = "\"block.supertntmod." + name + "\"";
                assertTrue(enContent.contains(key),
                        "en_us.json missing block name for TNT: " + name);
                assertTrue(trContent.contains(key),
                        "tr_tr.json missing block name for TNT: " + name);
                String tooltipKey = "\"block.supertntmod." + name + ".tooltip\"";
                assertTrue(enContent.contains(tooltipKey),
                        "en_us.json missing tooltip for TNT: " + name);
                assertTrue(trContent.contains(tooltipKey),
                        "tr_tr.json missing tooltip for TNT: " + name);
            }
        }
    }

    @Test
    void everyBlockstateHasItemsJsonForCrafting() throws IOException {
        Path blockstates = ASSETS.resolve("blockstates");
        Path items = ASSETS.resolve("items");
        Path recipes = DATA.resolve("recipe");

        try (Stream<Path> recipeStream = Files.list(recipes)) {
            List<String> craftable = recipeStream
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toList());

            try (Stream<Path> bsStream = Files.list(blockstates)) {
                List<String> blockNames = bsStream
                        .map(p -> p.getFileName().toString().replace(".json", ""))
                        .filter(craftable::contains)
                        .collect(Collectors.toList());

                for (String name : blockNames) {
                    Path itemFile = items.resolve(name + ".json");
                    assertTrue(Files.exists(itemFile),
                            "items/" + name + ".json missing (required in MC 1.21.11 for craftable blocks)");
                }
            }
        }
    }
}
