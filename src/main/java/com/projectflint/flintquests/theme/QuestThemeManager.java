package com.projectflint.flintquests.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectflint.flintquests.config.ConfigManager;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class QuestThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, QuestTheme> THEMES = new LinkedHashMap<>();
    private static QuestTheme current = new QuestTheme();

    private QuestThemeManager() {
    }

    public static synchronized void load() {
        THEMES.clear();
        if (ConfigManager.isPlayerBundle()) {
            String resource = ConfigManager.bundledThemeResource();
            if (!resource.isBlank()) {
                try (InputStream stream = ConfigManager.openBundledResource(resource)) {
                    if (stream != null) {
                        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                            QuestTheme theme = GSON.fromJson(reader, QuestTheme.class);
                            if (theme != null) {
                                theme.normalize();
                                THEMES.put("bundled", theme);
                                current = theme;
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            current = new QuestTheme();
            return;
        }

        Path directory = ConfigManager.themesDirectory();
        try {
            Files.createDirectories(directory);
            ensureBuiltInThemes(directory);
            try (Stream<Path> stream = Files.list(directory)) {
                for (Path path : stream.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .sorted()
                        .toList()) {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        QuestTheme theme = GSON.fromJson(reader, QuestTheme.class);
                        if (theme == null) continue;
                        theme.normalize();
                        THEMES.put(idFor(path), theme);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Flint Quests themes", exception);
        }

        if (THEMES.isEmpty()) THEMES.put("default", new QuestTheme());
        String requested = ConfigManager.get().activeTheme == null ? "default" : ConfigManager.get().activeTheme;
        current = THEMES.getOrDefault(requested, THEMES.values().iterator().next());
        if (!THEMES.containsKey(requested)) {
            ConfigManager.get().activeTheme = activeId();
            ConfigManager.save();
        }
    }

    public static synchronized QuestTheme current() {
        if (THEMES.isEmpty()) load();
        return current;
    }

    public static synchronized String activeId() {
        if (THEMES.isEmpty()) load();
        for (Map.Entry<String, QuestTheme> entry : THEMES.entrySet()) {
            if (entry.getValue() == current) return entry.getKey();
        }
        return "default";
    }

    public static synchronized String activeName() {
        return current().name;
    }

    public static synchronized List<String> ids() {
        if (THEMES.isEmpty()) load();
        return new ArrayList<>(THEMES.keySet()).stream().sorted(Comparator.naturalOrder()).toList();
    }

    public static synchronized void cycle() {
        List<String> ids = ids();
        if (ids.isEmpty()) return;
        int index = ids.indexOf(activeId());
        String next = ids.get((index + 1 + ids.size()) % ids.size());
        ConfigManager.get().activeTheme = next;
        ConfigManager.save();
        current = THEMES.get(next);
    }

    public static synchronized void reload() {
        load();
    }

    public static void openThemeDirectory() throws IOException {
        Path directory = ConfigManager.themesDirectory();
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

    private static void ensureBuiltInThemes(Path directory) throws IOException {
        // Built-in preset IDs are maintained by Flint Quests so bug-fixes to the presets
        // (for example transparent backgrounds) reach existing installs. Custom themes should
        // use their own file ID and are never touched here.
        writeBuiltInTheme(directory, "default", new QuestTheme());
        writeBuiltInTheme(directory, "light", lightTheme());
        writeBuiltInTheme(directory, "dark", darkTheme());
        writeBuiltInTheme(directory, "amoled", amoledTheme());
        writeBuiltInTheme(directory, "red", redTheme());
        writeBuiltInTheme(directory, "crimson", crimsonTheme());
        writeBuiltInTheme(directory, "brown", brownTheme());
        writeBuiltInTheme(directory, "vanilla", vanillaTheme());
    }

    private static void writeBuiltInTheme(Path directory, String id, QuestTheme theme) throws IOException {
        Path file = directory.resolve(id + ".json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(theme, writer);
        }
    }

    private static QuestTheme lightTheme() {
        QuestTheme theme = new QuestTheme();
        theme.name = "Light";
        theme.background = "#C8F2F4F7";
        theme.canvas = "#B8E9EDF2";
        theme.topBar = "#E8F8FAFC";
        theme.bottomBar = "#E8F8FAFC";
        theme.panel = "#E8F4F6F8";
        theme.panelEdge = "#FFB7C0CA";
        theme.sidebar = "#E8F1F4F7";
        theme.sidebarDivider = "#FF9AA6B2";
        theme.sidebarHandle = "#AA8794A3";
        theme.sidebarHandleInner = "#DD667584";
        theme.nodeBody = "#DDF5F7FA";
        theme.unlocked = "#FFD39C00";
        theme.completed = "#FF2DAD45";
        theme.locked = "#FF858E99";
        theme.linkSource = "#FF1687C9";
        theme.titleText = "#FF17202A";
        theme.bodyText = "#FF293440";
        theme.mutedText = "#FF687583";
        theme.labelText = "#FF44515E";
        theme.accentText = "#FF9D7100";
        theme.errorText = "#FFC62828";
        return theme;
    }

    private static QuestTheme darkTheme() {
        QuestTheme theme = new QuestTheme();
        theme.name = "Dark";
        theme.background = "#B80E1218";
        theme.canvas = "#B0141B23";
        theme.topBar = "#E00B1016";
        theme.bottomBar = "#E00B1016";
        theme.panel = "#F018212B";
        theme.panelEdge = "#FF080C10";
        theme.sidebar = "#F0151E27";
        theme.sidebarDivider = "#FF465362";
        theme.sidebarHandle = "#AA566474";
        theme.sidebarHandleInner = "#DD8190A0";
        theme.nodeBody = "#EE10171F";
        theme.unlocked = "#FFFFCF40";
        theme.completed = "#FF69F06A";
        theme.locked = "#FF68727E";
        theme.linkSource = "#FF4CC5F2";
        theme.titleText = "#FFF4F7FA";
        theme.bodyText = "#FFD9E0E8";
        theme.mutedText = "#FF8795A4";
        theme.labelText = "#FFBAC4CE";
        theme.accentText = "#FFFFD464";
        theme.errorText = "#FFFF6868";
        return theme;
    }

    private static QuestTheme amoledTheme() {
        QuestTheme theme = new QuestTheme();
        theme.name = "AMOLED";
        theme.background = "#C0000000";
        theme.canvas = "#C0000000";
        theme.topBar = "#E0050505";
        theme.bottomBar = "#E0050505";
        theme.panel = "#EE080808";
        theme.panelEdge = "#FF202020";
        theme.sidebar = "#E0050505";
        theme.sidebarDivider = "#FF303030";
        theme.sidebarHandle = "#AA555555";
        theme.sidebarHandleInner = "#DD888888";
        theme.nodeBody = "#E0050505";
        theme.unlocked = "#FFFFD84A";
        theme.completed = "#FF66FF66";
        theme.locked = "#FF666666";
        theme.linkSource = "#FF55CCFF";
        theme.titleText = "#FFFFFFFF";
        theme.bodyText = "#FFE6E6E6";
        theme.mutedText = "#FF8A8A8A";
        theme.labelText = "#FFBDBDBD";
        theme.accentText = "#FFFFD84A";
        theme.errorText = "#FFFF5555";
        return theme;
    }

    private static QuestTheme redTheme() {
        QuestTheme theme = darkTheme();
        theme.name = "Red";
        theme.background = "#B815090B";
        theme.canvas = "#B01E0C0F";
        theme.topBar = "#E018080B";
        theme.bottomBar = "#E018080B";
        theme.panel = "#F0251014";
        theme.panelEdge = "#FF0E0507";
        theme.sidebar = "#F0210D11";
        theme.sidebarDivider = "#FF7B303A";
        theme.sidebarHandle = "#AA8A3742";
        theme.sidebarHandleInner = "#DDBE5664";
        theme.nodeBody = "#EE1B0B0E";
        theme.unlocked = "#FFFFA726";
        theme.completed = "#FF75F06F";
        theme.linkSource = "#FFFF6B6B";
        theme.accentText = "#FFFF9D8E";
        return theme;
    }

    private static QuestTheme crimsonTheme() {
        QuestTheme theme = darkTheme();
        theme.name = "Crimson";
        theme.background = "#B810050A";
        theme.canvas = "#B0190710";
        theme.topBar = "#E012040A";
        theme.bottomBar = "#E012040A";
        theme.panel = "#F0250B17";
        theme.panelEdge = "#FF0B0206";
        theme.sidebar = "#F0200812";
        theme.sidebarDivider = "#FF6D213C";
        theme.sidebarHandle = "#AA7C2947";
        theme.sidebarHandleInner = "#DDB33B61";
        theme.nodeBody = "#EE19070F";
        theme.unlocked = "#FFFFC145";
        theme.completed = "#FF79F276";
        theme.linkSource = "#FFFF4F81";
        theme.accentText = "#FFFF6E9B";
        return theme;
    }

    private static QuestTheme brownTheme() {
        QuestTheme theme = darkTheme();
        theme.name = "Brown";
        theme.background = "#B818120D";
        theme.canvas = "#B0221912";
        theme.topBar = "#E01A120C";
        theme.bottomBar = "#E01A120C";
        theme.panel = "#F02D2118";
        theme.panelEdge = "#FF100B07";
        theme.sidebar = "#F0291E16";
        theme.sidebarDivider = "#FF6F5843";
        theme.sidebarHandle = "#AA796149";
        theme.sidebarHandleInner = "#DDA88B6A";
        theme.nodeBody = "#EE21170F";
        theme.unlocked = "#FFF2C14E";
        theme.completed = "#FF7ED36E";
        theme.linkSource = "#FFD39A63";
        theme.titleText = "#FFFFF4E8";
        theme.bodyText = "#FFE7D8C6";
        theme.mutedText = "#FFA9957E";
        theme.labelText = "#FFCAB9A5";
        theme.accentText = "#FFF2C14E";
        return theme;
    }

    private static QuestTheme vanillaTheme() {
        QuestTheme theme = new QuestTheme();
        theme.name = "Vanilla Minecraft";
        theme.background = "#B8101010";
        theme.canvas = "#B0222222";
        theme.topBar = "#E01B1B1B";
        theme.bottomBar = "#E01B1B1B";
        theme.panel = "#E8373737";
        theme.panelEdge = "#FF0F0F0F";
        theme.sidebar = "#E8303030";
        theme.sidebarDivider = "#FF8A8A8A";
        theme.sidebarHandle = "#AA777777";
        theme.sidebarHandleInner = "#DDB0B0B0";
        theme.nodeBody = "#EE555555";
        theme.unlocked = "#FFFFFF55";
        theme.completed = "#FF55FF55";
        theme.locked = "#FFAAAAAA";
        theme.linkSource = "#FF55FFFF";
        theme.titleText = "#FFFFFFFF";
        theme.bodyText = "#FFE0E0E0";
        theme.mutedText = "#FFAAAAAA";
        theme.labelText = "#FFD0D0D0";
        theme.accentText = "#FFFFFF55";
        theme.errorText = "#FFFF5555";
        return theme;
    }

    public static Path activeThemeFile() {
        if (ConfigManager.isPlayerBundle()) return null;
        return ConfigManager.themesDirectory().resolve(activeId() + ".json");
    }

    private static String idFor(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.length() - 5);
    }
}
