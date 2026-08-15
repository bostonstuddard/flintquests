package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CategoryEditorScreen extends Screen {
    private final Screen parent;
    private final QuestCategoryDefinition original;
    private final QuestCategoryDefinition draft;

    private EditBox idBox;
    private EditBox titleBox;
    private EditBox iconBox;
    private EditBox parentBox;
    private EditBox orderBox;

    public CategoryEditorScreen(Screen parent, QuestCategoryDefinition category) {
        super(Component.literal(category == null ? "Create Quest Category" : "Edit Quest Category"));
        this.parent = parent;
        this.original = category == null ? null : category.copy();
        this.draft = category == null ? new QuestCategoryDefinition() : category.copy();
        if (category == null) {
            draft.id = "new_category";
            draft.title = "New Category";
            draft.icon = "minecraft:book";
            draft.parent = "";
        }
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(380, width - 60);
        int left = width / 2 - panelWidth / 2;
        int fieldWidth = panelWidth - 24;
        int top = Math.max(38, height / 2 - 110);

        if (original == null) {
            idBox = field(left + 12, top + 28, fieldWidth, 18, draft.id);
        }
        titleBox = field(left + 12, top + 66, fieldWidth, 18, draft.title);
        iconBox = field(left + 12, top + 104, fieldWidth - 66, 18, draft.icon);
        parentBox = field(left + 12, top + 142, fieldWidth - 66, 18, draft.parent);
        orderBox = field(left + 12, top + 180, 72, 18, Integer.toString(draft.order));

        addRenderableWidget(Button.builder(Component.literal("Search"), button -> {
                saveFieldsToDraft();
                minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Category Icon"), SearchSelectScreen.Kind.ITEM,
                        id -> draft.icon = id));
            })
                .bounds(left + panelWidth - 66, top + 104, 54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Choose"), button -> {
                saveFieldsToDraft();
                minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Parent Category"), SearchSelectScreen.Kind.CATEGORY,
                        id -> draft.parent = id));
            })
                .bounds(left + panelWidth - 66, top + 142, 54, 18).build());

        int bottom = height - 30;
        addRenderableWidget(Button.builder(Component.literal("Save Category"), button -> saveAndClose())
                .bounds(width / 2 - 104, bottom, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(width / 2 + 6, bottom, 72, 20).build());
    }

    private EditBox field(int x, int y, int width, int height, String value) {
        EditBox box = new EditBox(font, x, y, width, height, Component.empty());
        box.setMaxLength(256);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void saveFieldsToDraft() {
        if (original == null && idBox != null) draft.id = idBox.getValue().trim();
        else if (original != null) draft.id = original.id;
        draft.title = titleBox.getValue().trim();
        draft.icon = iconBox.getValue().trim();
        draft.parent = parentBox.getValue().trim();
        draft.order = parseInt(orderBox.getValue(), 0);
    }

    private void saveAndClose() {
        saveFieldsToDraft();
        if (draft.parent.equals(draft.id)) draft.parent = "";
        draft.normalize();
        CategoryRepository.save(draft);
        CategoryRepository.load();
        minecraft.setScreen(parent);
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(380, width - 60);
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(38, height / 2 - 110);
        graphics.fill(0, 0, width, height, 0xB818202A);
        graphics.fill(left, top - 18, left + panelWidth, top + 216, 0xF0232D39);
        graphics.drawCenteredString(font, title, width / 2, top - 10, 0xFFF3F5F7);

        if (original == null) label(graphics, "Category ID", left + 12, top + 16);
        else label(graphics, "Category ID (locked): " + original.id, left + 12, top + 20);
        label(graphics, "Display name", left + 12, top + 54);
        label(graphics, "Icon item", left + 12, top + 92);
        label(graphics, "Parent category (blank = top level)", left + 12, top + 130);
        label(graphics, "Sort order", left + 12, top + 168);
        graphics.drawString(font, Component.literal("Use Search/Choose instead of memorizing registry IDs."), left + 12, top + 206, 0xFF8FA0B2, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void label(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(font, Component.literal(text), x, y, 0xFFB7C1CC, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
