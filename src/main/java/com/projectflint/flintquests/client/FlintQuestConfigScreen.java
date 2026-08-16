package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.export.QuestBundleExporter;
import com.projectflint.flintquests.export.QuestDataZipTransfer;
import com.projectflint.flintquests.theme.QuestTheme;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Settings screen used both by Mod Menu and the in-book Settings button. */
public final class FlintQuestConfigScreen extends Screen {
    private final Screen parent;
    private String exportStatus = "";
    private boolean exportFailed;

    public FlintQuestConfigScreen(Screen parent) {
        super(Component.literal("Flint Quests Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigManager.load();
        QuestThemeManager.reload();

        int center = width / 2;
        int buttonWidth = Math.min(300, Math.max(190, width - 24));
        int left = center - buttonWidth / 2;
        int top = 50;
        int step = 24;
        int row = 0;

        addRenderableWidget(Button.builder(announceLabel(), button -> {
            ConfigManager.get().announceQuestCompletion = !ConfigManager.get().announceQuestCompletion;
            ConfigManager.save();
            button.setMessage(announceLabel());
        }).bounds(left, top + row++ * step, buttonWidth, 19).build());

        if (!ConfigManager.isPlayerBundle()) {
            addRenderableWidget(Button.builder(themeLabel(), button -> {
                QuestThemeManager.cycle();
                button.setMessage(themeLabel());
            }).bounds(left, top + row++ * step, buttonWidth, 19).build());

            addRenderableWidget(Button.builder(Component.literal("Open Theme Folder"), button -> {
                exportFailed = false;
                try {
                    QuestThemeManager.openThemeDirectory();
                    exportStatus = "Opened config/flintquests/themes";
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Could not open themes: " + compactMessage(exception);
                }
            }).bounds(left, top + row++ * step, buttonWidth, 19).build());
        }

        if (ConfigManager.devEnvironmentAvailable()) {
            addRenderableWidget(Button.builder(editingLabel(), button -> {
                ConfigManager.get().questEditing = !ConfigManager.get().questEditing;
                ConfigManager.save();
                button.setMessage(editingLabel());
            }).bounds(left, top + row++ * step, buttonWidth, 19).build());

            int gap = 6;
            int half = (buttonWidth - gap) / 2;
            int rowY = top + row++ * step;
            addRenderableWidget(Button.builder(Component.literal("Build Nestable .jar"), button -> {
                button.active = false;
                exportStatus = "Building quest-pack jar...";
                exportFailed = false;
                try {
                    Path built = QuestBundleExporter.buildNestableJar();
                    exportStatus = "Built: " + built.getFileName();
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Export failed: " + compactMessage(exception);
                } finally {
                    button.active = true;
                }
            }).bounds(left, rowY, half, 19).build());
            addRenderableWidget(Button.builder(Component.literal("Open Built Jars"), button -> {
                exportFailed = false;
                try {
                    QuestBundleExporter.openExportDirectory();
                    exportStatus = "Opened flintquests-exports";
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Could not open exports: " + compactMessage(exception);
                }
            }).bounds(left + half + gap, rowY, half, 19).build());

            rowY = top + row++ * step;
            addRenderableWidget(Button.builder(Component.literal("Export Quest Data ZIP"), button -> {
                exportFailed = false;
                try {
                    Path built = QuestDataZipTransfer.exportDataZip();
                    exportStatus = "Exported: " + built.getFileName();
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Data export failed: " + compactMessage(exception);
                }
            }).bounds(left, rowY, half, 19).build());
            addRenderableWidget(Button.builder(Component.literal("Import Quest Data ZIP"), button -> {
                exportFailed = false;
                try {
                    Path backup = QuestDataZipTransfer.chooseAndImportDataZip();
                    if (backup == null) {
                        exportStatus = "Import cancelled";
                    } else {
                        QuestRepository.load();
                        CategoryRepository.load();
                        QuestThemeManager.reload();
                        exportStatus = "Imported quest data. Backup: " + backup.getFileName();
                    }
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Data import failed: " + compactMessage(exception);
                }
            }).bounds(left + half + gap, rowY, half, 19).build());

            addRenderableWidget(Button.builder(Component.literal("Open Quest Data ZIP Folder"), button -> {
                exportFailed = false;
                try {
                    QuestDataZipTransfer.openTransferDirectory();
                    exportStatus = "Opened flintquests-exports/quest-data";
                } catch (Exception exception) {
                    exportFailed = true;
                    exportStatus = "Could not open quest-data folder: " + compactMessage(exception);
                }
            }).bounds(left, top + row++ * step, buttonWidth, 19).build());

            addRenderableWidget(Button.builder(Component.literal("Developer Environment: ON (disable)"), button -> {
                ConfigManager.get().devEnvironment = false;
                ConfigManager.get().questEditing = false;
                ConfigManager.save();
                minecraft.setScreen(new FlintQuestConfigScreen(parent));
            }).bounds(left, top + row++ * step, buttonWidth, 19).build());
        }

        int doneY = Math.min(height - 24, top + row * step + 6);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 50, doneY, 100, 19).build());
    }

    private Component editingLabel() {
        return Component.literal("Quest Editing: " + enabled(ConfigManager.get().questEditing));
    }

    private Component announceLabel() {
        return Component.literal("Quest Completion Popups: " + enabled(ConfigManager.get().announceQuestCompletion));
    }

    private Component themeLabel() {
        return Component.literal("Theme: " + QuestThemeManager.activeName() + "  (click to cycle)");
    }

    private String enabled(boolean value) {
        return value ? "ON" : "OFF";
    }

    public void refreshEditingMode() {
        rebuildWidgets();
    }

    private String compactMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() > 92 ? message.substring(0, 89) + "..." : message;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        QuestTheme theme = QuestThemeManager.current();
        graphics.fill(0, 0, width, height, theme.backgroundColor());
        graphics.drawCenteredString(font, title, width / 2, 18, theme.titleTextColor());
        if (ConfigManager.isPlayerBundle()) {
            graphics.drawCenteredString(font,
                    Component.literal("Player quest-pack distribution — authoring tools are disabled"),
                    width / 2, 32, theme.mutedTextColor());
        } else if (ConfigManager.devEnvironmentAvailable()) {
            graphics.drawCenteredString(font,
                    Component.literal("Developer Environment ON"),
                    width / 2, 32, theme.mutedTextColor());
        } else {
            graphics.drawCenteredString(font,
                    Component.literal("Developer tools are disabled. Re-enable devEnvironment in flintquests.json."),
                    width / 2, 32, theme.mutedTextColor());
        }

        if (!exportStatus.isBlank()) {
            int color = exportFailed ? theme.errorTextColor() : theme.completedColor();
            graphics.drawCenteredString(font, Component.literal(exportStatus), width / 2, height - 10, color);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FlintQuestsClient.handleEditingToggleKey(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
