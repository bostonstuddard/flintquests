package com.projectflint.flintquests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("flintquests");
    private static final Path FILE = ROOT.resolve("flintquests.json");
    private static FlintQuestConfig config = new FlintQuestConfig();

    private ConfigManager() {
    }

    public static void load() {
        try {
            Files.createDirectories(ROOT.resolve("quests"));
            Files.createDirectories(ROOT.resolve("categories"));
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

    public static Path root() {
        return ROOT;
    }

    public static Path questsDirectory() {
        return ROOT.resolve("quests");
    }

    public static Path categoriesDirectory() {
        return ROOT.resolve("categories");
    }
}
