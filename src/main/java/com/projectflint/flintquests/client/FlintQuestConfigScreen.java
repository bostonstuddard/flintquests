package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lightweight settings screen used by the optional Mod Menu integration.
 * It intentionally avoids calling renderBackground(...) directly because
 * Minecraft 1.21.11 already performs the screen blur/background pass.
 */
public final class FlintQuestConfigScreen extends Screen {
    private final Screen parent;

    public FlintQuestConfigScreen(Screen parent) {
        super(Component.literal("Flint Quests Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigManager.load();

        int center = width / 2;
        int top = Math.max(64, height / 2 - 48);
        int buttonWidth = 240;
        int left = center - buttonWidth / 2;

        addRenderableWidget(Button.builder(editingLabel(), button -> {
            ConfigManager.get().questEditing = !ConfigManager.get().questEditing;
            ConfigManager.save();
            button.setMessage(editingLabel());
        }).bounds(left, top, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(announceLabel(), button -> {
            ConfigManager.get().announceQuestCompletion = !ConfigManager.get().announceQuestCompletion;
            ConfigManager.save();
            button.setMessage(announceLabel());
        }).bounds(left, top + 28, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 50, top + 72, 100, 20).build());
    }

    private Component editingLabel() {
        return Component.literal("Quest Editing: " + enabled(ConfigManager.get().questEditing));
    }

    private Component announceLabel() {
        return Component.literal("Quest Completion Popups: " + enabled(ConfigManager.get().announceQuestCompletion));
    }

    private String enabled(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("Settings save immediately"),
                width / 2, 40, 0xA0A0A0);
        graphics.drawCenteredString(font,
                Component.literal("Editing OFF = player/read-only quest mode"),
                width / 2, 50, 0x808080);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
