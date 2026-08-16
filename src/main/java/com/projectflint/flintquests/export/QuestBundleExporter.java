package com.projectflint.flintquests.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectflint.flintquests.FlintQuests;
import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/** Builds a self-contained, player-facing Flint Quests jar containing the author's current quest data. */
public final class QuestBundleExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUNDLED_ROOT = "assets/flintquests/bundled/";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private QuestBundleExporter() {
    }

    public static Path buildNestableJar() throws IOException {
        if (!ConfigManager.devEnvironmentAvailable()) {
            throw new IllegalStateException("Developer Environment must be enabled to export a quest-pack jar.");
        }

        ModContainer container = FabricLoader.getInstance().getModContainer(FlintQuests.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Flint Quests mod container is unavailable."));

        Path exportDirectory = exportDirectory();
        Files.createDirectories(exportDirectory);
        String version = container.getMetadata().getVersion().getFriendlyString();
        String safeVersion = version.replaceAll("[^A-Za-z0-9._+-]", "_");
        String filename = "FlintQuests-QuestPack-" + safeVersion + "-" + FILE_TIME.format(LocalDateTime.now()) + ".jar";
        Path destination = exportDirectory.resolve(filename);
        Path temporary = exportDirectory.resolve(filename + ".tmp");

        List<Path> questFiles = jsonFiles(ConfigManager.questsDirectory());
        List<Path> categoryFiles = jsonFiles(ConfigManager.categoriesDirectory());
        Set<String> written = new HashSet<>();
        List<String> bundledQuestResources = new ArrayList<>();
        List<String> bundledCategoryResources = new ArrayList<>();

        try (OutputStream fileOut = Files.newOutputStream(temporary);
             JarOutputStream jar = new JarOutputStream(fileOut)) {
            for (Path root : container.getRootPaths()) {
                copyModRoot(root, jar, written, version);
            }

            for (Path quest : questFiles) {
                String resource = BUNDLED_ROOT + "quests/" + quest.getFileName();
                writeFile(jar, written, resource, quest);
                bundledQuestResources.add(resource);
            }
            for (Path category : categoryFiles) {
                String resource = BUNDLED_ROOT + "categories/" + category.getFileName();
                writeFile(jar, written, resource, category);
                bundledCategoryResources.add(resource);
            }

            String migrationResource = "";
            Path migrations = ConfigManager.root().resolve("quest_id_migrations.json");
            if (Files.isRegularFile(migrations)) {
                migrationResource = BUNDLED_ROOT + "quest_id_migrations.json";
                writeFile(jar, written, migrationResource, migrations);
            }

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("format", 1);
            manifest.put("playerOnly", true);
            manifest.put("sourceModVersion", version);
            manifest.put("quests", bundledQuestResources);
            manifest.put("categories", bundledCategoryResources);
            manifest.put("migrations", migrationResource);

            String themeResource = "";
            Path activeTheme = QuestThemeManager.activeThemeFile();
            if (activeTheme != null && Files.isRegularFile(activeTheme)) {
                themeResource = BUNDLED_ROOT + "theme.json";
                writeFile(jar, written, themeResource, activeTheme);
            }
            manifest.put("theme", themeResource);
            writeBytes(jar, written, BUNDLED_ROOT + "player-bundle.json",
                    GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable throwable) {
            Files.deleteIfExists(temporary);
            if (throwable instanceof IOException ioException) throw ioException;
            throw new IOException("Failed to build Flint Quests player bundle", throwable);
        }

        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    public static Path exportDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("flintquests-exports");
    }

    public static void openExportDirectory() throws IOException {
        Path directory = exportDirectory();
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

    private static List<Path> jsonFiles(Path directory) throws IOException {
        if (Files.notExists(directory)) return List.of();
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static void copyModRoot(Path root, JarOutputStream jar, Set<String> written, String version) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String entryName = root.relativize(path).toString().replace('\\', '/');
                if (entryName.isBlank()) continue;
                if (entryName.startsWith(BUNDLED_ROOT)) continue;
                if (isSignatureFile(entryName)) continue;
                if (!written.add(entryName)) continue;
                JarEntry entry = new JarEntry(entryName);
                entry.setTime(0L);
                jar.putNextEntry(entry);
                if (entryName.equals("fabric.mod.json")) {
                    String metadata = Files.readString(path, StandardCharsets.UTF_8).replace("${version}", version);
                    jar.write(metadata.getBytes(StandardCharsets.UTF_8));
                } else {
                    try (InputStream input = Files.newInputStream(path)) {
                        input.transferTo(jar);
                    }
                }
                jar.closeEntry();
            }
        }
    }

    private static boolean isSignatureFile(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA"));
    }

    private static void writeFile(JarOutputStream jar, Set<String> written, String entryName, Path source) throws IOException {
        if (!written.add(entryName)) throw new IOException("Duplicate export entry " + entryName);
        JarEntry entry = new JarEntry(entryName);
        entry.setTime(0L);
        jar.putNextEntry(entry);
        try (InputStream input = Files.newInputStream(source)) {
            input.transferTo(jar);
        }
        jar.closeEntry();
    }

    private static void writeBytes(JarOutputStream jar, Set<String> written, String entryName, byte[] bytes) throws IOException {
        if (!written.add(entryName)) throw new IOException("Duplicate export entry " + entryName);
        JarEntry entry = new JarEntry(entryName);
        entry.setTime(0L);
        jar.putNextEntry(entry);
        jar.write(bytes);
        jar.closeEntry();
    }
}
