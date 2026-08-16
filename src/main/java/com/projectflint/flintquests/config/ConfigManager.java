package com.projectflint.flintquests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("flintquests");
    private static final Path FILE = ROOT.resolve("flintquests.json");
    private static final String PLAYER_BUNDLE_MANIFEST = "assets/flintquests/bundled/player-bundle.json";
    private static FlintQuestConfig config = new FlintQuestConfig();
    private static BundleManifest bundleManifest;
    private static boolean bundleManifestChecked;

    private ConfigManager() {
    }

    public static void load() {
        try {
            Files.createDirectories(ROOT.resolve("quests"));
            Files.createDirectories(ROOT.resolve("categories"));
            Files.createDirectories(ROOT.resolve("themes"));
            if (Files.notExists(FILE)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(FILE)) {
                FlintQuestConfig loaded = GSON.fromJson(reader, FlintQuestConfig.class);
                if (loaded != null) config = loaded;
                config.normalize();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Flint Quests config", exception);
        }
    }

    public static void save() {
        config.normalize();
        try {
            Files.createDirectories(ROOT);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save Flint Quests config", exception);
        }
    }

    public static FlintQuestConfig get() {
        return config;
    }

    public static boolean devToolsEnabled() {
        return devEnvironmentAvailable() && config.questEditing;
    }

    public static boolean devEnvironmentAvailable() {
        return !isPlayerBundle() && config.devEnvironment;
    }

    /**
     * True for exported player-facing quest-pack jars. These distributions keep the
     * quest engine/API but hard-disable all in-game authoring tools regardless of a
     * local config left over from a development install.
     */
    public static boolean isPlayerBundle() {
        return bundleManifest() != null;
    }

    public static List<String> bundledQuestResources() {
        BundleManifest manifest = bundleManifest();
        return manifest == null || manifest.quests == null ? List.of() : List.copyOf(manifest.quests);
    }

    public static List<String> bundledCategoryResources() {
        BundleManifest manifest = bundleManifest();
        return manifest == null || manifest.categories == null ? List.of() : List.copyOf(manifest.categories);
    }

    public static String bundledMigrationResource() {
        BundleManifest manifest = bundleManifest();
        return manifest == null || manifest.migrations == null ? "" : manifest.migrations;
    }

    public static String bundledThemeResource() {
        BundleManifest manifest = bundleManifest();
        return manifest == null || manifest.theme == null ? "" : manifest.theme;
    }

    public static InputStream openBundledResource(String path) {
        if (path == null || path.isBlank()) return null;
        return ConfigManager.class.getClassLoader().getResourceAsStream(path);
    }

    private static synchronized BundleManifest bundleManifest() {
        if (bundleManifestChecked) return bundleManifest;
        bundleManifestChecked = true;
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(PLAYER_BUNDLE_MANIFEST)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                BundleManifest loaded = GSON.fromJson(reader, BundleManifest.class);
                if (loaded != null && loaded.playerOnly) {
                    if (loaded.quests == null) loaded.quests = new ArrayList<>();
                    if (loaded.categories == null) loaded.categories = new ArrayList<>();
                    bundleManifest = loaded;
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read Flint Quests bundled player manifest", exception);
        }
        return bundleManifest;
    }

    private static final class BundleManifest {
        boolean playerOnly;
        List<String> quests = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        String migrations = "";
        String theme = "";
    }


    public static Path root() {
        return ROOT;
    }

    public static Path questsDirectory() {
        return ROOT.resolve("quests");
    }

    public static Path categoriesDirectory() {
        return ROOT.resolve("categories");
    }

    public static Path themesDirectory() {
        return ROOT.resolve("themes");
    }
}
