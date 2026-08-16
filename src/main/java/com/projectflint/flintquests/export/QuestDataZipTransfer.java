package com.projectflint.flintquests.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.theme.QuestThemeManager;

import java.awt.Desktop;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/** Author collaboration import/export for editable Flint Quests data. */
public final class QuestDataZipTransfer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String MANIFEST = "flintquests-data.json";
    private static final int CURRENT_FORMAT = 2;
    private static final String CURRENT_SCHEMA = "flintquests.quest-data.v2";
    private static final String DATA_TYPE = "flintquests-editable-data";

    private QuestDataZipTransfer() {
    }

    public static Path exportDataZip() throws IOException {
        return exportDataZip("FlintQuests-QuestData-");
    }

    private static Path exportDataZip(String prefix) throws IOException {
        requireDev();
        Path directory = transferDirectory();
        Files.createDirectories(directory);
        Path destination = directory.resolve(prefix + FILE_TIME.format(LocalDateTime.now()) + ".zip");
        try (OutputStream output = Files.newOutputStream(destination);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("format", CURRENT_FORMAT);
            manifest.put("schema", CURRENT_SCHEMA);
            manifest.put("type", DATA_TYPE);
            manifest.put("created", LocalDateTime.now().toString());
            manifest.put("activeTheme", QuestThemeManager.activeId());
            writeBytes(zip, MANIFEST, GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
            // These container entries are part of the v2 import contract. They let Flint Quests
            // distinguish a deliberately empty data set from an unrelated ZIP.
            writeDirectory(zip, "quests/");
            writeDirectory(zip, "categories/");
            addJsonDirectory(zip, ConfigManager.questsDirectory(), "quests/");
            addJsonDirectory(zip, ConfigManager.categoriesDirectory(), "categories/");
            Path migrations = ConfigManager.root().resolve("quest_id_migrations.json");
            if (Files.isRegularFile(migrations)) writeFile(zip, "quest_id_migrations.json", migrations);
            Path activeTheme = QuestThemeManager.activeThemeFile();
            if (activeTheme != null && Files.isRegularFile(activeTheme)) {
                writeFile(zip, "theme/" + activeTheme.getFileName(), activeTheme);
            }
        }
        return destination;
    }

    public static Path chooseAndImportDataZip() throws IOException {
        requireDev();
        Files.createDirectories(transferDirectory());
        Path startDirectory = downloadsDirectory();
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pattern = stack.UTF8("*.zip");
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(0, MemoryUtil.memAddress(pattern));
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Import Flint Quests Data ZIP",
                    startDirectory.toAbsolutePath().toString(),
                    filters,
                    "Flint Quests ZIP (*.zip)",
                    false);
        } catch (Throwable throwable) {
            throw new IOException("Could not open the native ZIP picker: " + throwable.getClass().getSimpleName(), throwable);
        }
        if (selected == null || selected.isBlank()) return null;
        return importDataZip(Path.of(selected));
    }

    public static Path importDataZip(Path source) throws IOException {
        requireDev();
        if (source == null || !Files.isRegularFile(source)) throw new IOException("Selected ZIP does not exist.");

        Path temp = Files.createTempDirectory("flintquests-import-");
        try {
            boolean manifestFound = false;
            boolean questsContainerFound = false;
            boolean categoriesContainerFound = false;
            int recognizedPayloadFiles = 0;
            try (InputStream input = Files.newInputStream(source);
                 ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = normalizeEntry(entry.getName());
                    if (entry.isDirectory()) {
                        if (name.equals("quests/")) questsContainerFound = true;
                        if (name.equals("categories/")) categoriesContainerFound = true;
                        continue;
                    }
                    if (name.equals(MANIFEST)) {
                        manifestFound = true;
                        Files.copy(zip, safeTarget(temp, MANIFEST), StandardCopyOption.REPLACE_EXISTING);
                    } else if (name.startsWith("quests/") && name.endsWith(".json")) {
                        questsContainerFound = true;
                        recognizedPayloadFiles++;
                        copyZipEntry(zip, safeTarget(temp, name));
                    } else if (name.startsWith("categories/") && name.endsWith(".json")) {
                        categoriesContainerFound = true;
                        recognizedPayloadFiles++;
                        copyZipEntry(zip, safeTarget(temp, name));
                    } else if (name.equals("quest_id_migrations.json")) {
                        recognizedPayloadFiles++;
                        copyZipEntry(zip, safeTarget(temp, name));
                    } else if (name.startsWith("theme/") && name.endsWith(".json")) {
                        recognizedPayloadFiles++;
                        copyZipEntry(zip, safeTarget(temp, name));
                    }
                }
            }
            if (!manifestFound) {
                throw new IOException("Not a Flint Quests data ZIP: required marker " + MANIFEST + " is missing.");
            }
            Map<?, ?> manifest = validateManifest(temp.resolve(MANIFEST));
            validatePayloadContract(manifest, questsContainerFound, categoriesContainerFound, recognizedPayloadFiles);

            Path backup = exportDataZip("FlintQuests-PreImportBackup-");
            replaceJsonDirectory(temp.resolve("quests"), ConfigManager.questsDirectory());
            replaceJsonDirectory(temp.resolve("categories"), ConfigManager.categoriesDirectory());
            Path importedMigration = temp.resolve("quest_id_migrations.json");
            Path targetMigration = ConfigManager.root().resolve("quest_id_migrations.json");
            if (Files.isRegularFile(importedMigration)) {
                Files.copy(importedMigration, targetMigration, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(targetMigration);
            }

            Path importedThemes = temp.resolve("theme");
            if (Files.isDirectory(importedThemes)) {
                Files.createDirectories(ConfigManager.themesDirectory());
                try (Stream<Path> stream = Files.list(importedThemes)) {
                    for (Path themeFile : stream.filter(Files::isRegularFile)
                            .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                            .toList()) {
                        Files.copy(themeFile, ConfigManager.themesDirectory().resolve(themeFile.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                Object activeTheme = manifest.get("activeTheme");
                if (activeTheme instanceof String themeId && !themeId.isBlank()) {
                    Path candidate = ConfigManager.themesDirectory().resolve(themeId + ".json");
                    if (Files.isRegularFile(candidate)) {
                        ConfigManager.get().activeTheme = themeId;
                        ConfigManager.save();
                    }
                }
            }
            return backup;
        } finally {
            deleteTree(temp);
        }
    }

    private static Path downloadsDirectory() {
        String home = System.getProperty("user.home", "").trim();
        if (!home.isBlank()) {
            Path downloads = Path.of(home).resolve("Downloads");
            if (Files.isDirectory(downloads)) return downloads;
        }
        Path fallback = transferDirectory();
        try {
            Files.createDirectories(fallback);
        } catch (IOException ignored) {
        }
        return fallback;
    }

    public static Path transferDirectory() {
        return QuestBundleExporter.exportDirectory().resolve("quest-data");
    }

    public static void openTransferDirectory() throws IOException {
        Path directory = transferDirectory();
        Files.createDirectories(directory);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(directory.toFile());
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder process;
        if (os.contains("win")) process = new ProcessBuilder("explorer.exe", directory.toAbsolutePath().toString());
        else if (os.contains("mac")) process = new ProcessBuilder("open", directory.toAbsolutePath().toString());
        else process = new ProcessBuilder("xdg-open", directory.toAbsolutePath().toString());
        process.start();
    }

    private static void requireDev() {
        if (!ConfigManager.devEnvironmentAvailable()) {
            throw new IllegalStateException("Developer Environment must be enabled for quest-data import/export.");
        }
    }

    private static Map<?, ?> validateManifest(Path manifest) throws IOException {
        Map<?, ?> data;
        try (var reader = Files.newBufferedReader(manifest)) {
            data = GSON.fromJson(reader, Map.class);
        } catch (Exception exception) {
            throw new IOException("Invalid " + MANIFEST + ": the marker file is not valid JSON.", exception);
        }
        if (data == null || !DATA_TYPE.equals(data.get("type"))) {
            throw new IOException("Not a Flint Quests editable-data ZIP: invalid type in " + MANIFEST + ".");
        }
        Object rawFormat = data.get("format");
        if (!(rawFormat instanceof Number number)) {
            throw new IOException("Invalid Flint Quests data ZIP: missing numeric format version.");
        }
        int format = number.intValue();
        if (format < 1 || format > CURRENT_FORMAT) {
            throw new IOException("Unsupported Flint Quests data ZIP format " + format + ". Supported formats: 1-" + CURRENT_FORMAT + ".");
        }
        if (format >= 2 && !CURRENT_SCHEMA.equals(data.get("schema"))) {
            throw new IOException("Invalid Flint Quests v2 data ZIP: schema marker must be " + CURRENT_SCHEMA + ".");
        }
        return data;
    }

    private static void validatePayloadContract(Map<?, ?> manifest, boolean questsContainerFound,
                                                boolean categoriesContainerFound, int recognizedPayloadFiles) throws IOException {
        int format = ((Number) manifest.get("format")).intValue();
        if (format >= 2) {
            if (!questsContainerFound) {
                throw new IOException("Invalid Flint Quests data ZIP: required quests/ container is missing.");
            }
            if (!categoriesContainerFound) {
                throw new IOException("Invalid Flint Quests data ZIP: required categories/ container is missing.");
            }
            return;
        }
        // Legacy v1 exports did not write explicit directory entries, so accept them only when
        // they contain at least one recognized Flint Quests payload file.
        if (recognizedPayloadFiles <= 0) {
            throw new IOException("Invalid legacy Flint Quests data ZIP: no quest/category/theme data was found.");
        }
    }

    private static void addJsonDirectory(ZipOutputStream zip, Path directory, String prefix) throws IOException {
        if (Files.notExists(directory)) return;
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList()) {
                writeFile(zip, prefix + file.getFileName(), file);
            }
        }
    }

    private static void writeDirectory(ZipOutputStream zip, String name) throws IOException {
        String normalized = name.endsWith("/") ? name : name + "/";
        zip.putNextEntry(new ZipEntry(normalized));
        zip.closeEntry();
    }

    private static void writeFile(ZipOutputStream zip, String name, Path source) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = Files.newInputStream(source)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }

    private static void writeBytes(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static String normalizeEntry(String raw) throws IOException {
        String value = raw.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        if (value.contains("../") || value.equals("..")) throw new IOException("Unsafe ZIP entry: " + raw);
        return value;
    }

    private static Path safeTarget(Path root, String entryName) throws IOException {
        Path target = root.resolve(entryName).normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException("Unsafe ZIP entry: " + entryName);
        }
        return target;
    }

    private static void copyZipEntry(ZipInputStream zip, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void replaceJsonDirectory(Path imported, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.list(target)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
        if (Files.notExists(imported)) return;
        try (Stream<Path> stream = Files.list(imported)) {
            for (Path source : stream.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList()) {
                Files.copy(source, target.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || Files.notExists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
