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
    void modIdFollowsNamingConvention() {
        String modId = "supertntmod";
        assertTrue(modId.matches("[a-z][a-z0-9_]*"),
                "Mod ID must be lowercase alphanumeric + underscores, no spaces");
    }
}
