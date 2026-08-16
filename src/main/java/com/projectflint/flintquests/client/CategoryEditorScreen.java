package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
    private Button typeButton;

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
            draft.selectable = true;
        }
    }

    @Override
    protected void init() {
        if (!ConfigManager.devToolsEnabled()) {
            minecraft.setScreen(parent);
            return;
        }
        Layout layout = layout();
        int fieldWidth = layout.panelWidth() - 24;
        int fieldHeight = 18;

        if (original == null) {
            idBox = field(layout.left() + 12, layout.row(0) + 11, fieldWidth, fieldHeight, draft.id);
        }
        titleBox = field(layout.left() + 12, layout.row(1) + 11, fieldWidth, fieldHeight, draft.title);

        typeButton = addRenderableWidget(Button.builder(typeLabel(), button -> {
                    draft.selectable = !draft.selectable;
                    button.setMessage(typeLabel());
                })
                .bounds(layout.left() + 12, layout.row(2) + 11, fieldWidth, fieldHeight).build());

        iconBox = field(layout.left() + 12, layout.row(3) + 11, fieldWidth - 66, fieldHeight, draft.icon);
        parentBox = field(layout.left() + 12, layout.row(4) + 11, fieldWidth - 66, fieldHeight, draft.parent);
        orderBox = field(layout.left() + 12, layout.row(5) + 11, 72, fieldHeight, Integer.toString(draft.order));

        addRenderableWidget(Button.builder(Component.literal("Search"), button -> {
                    saveFieldsToDraft();
                    minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Category Icon"), SearchSelectScreen.Kind.ITEM,
                            id -> draft.icon = id));
                })
                .bounds(layout.left() + layout.panelWidth() - 66, layout.row(3) + 11, 54, fieldHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Choose"), button -> {
                    saveFieldsToDraft();
                    minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Parent Category"), SearchSelectScreen.Kind.CATEGORY_ANY,
                            id -> draft.parent = id));
                })
                .bounds(layout.left() + layout.panelWidth() - 66, layout.row(4) + 11, 54, fieldHeight).build());

        int bottom = layout.bottomButtonY();
        addRenderableWidget(Button.builder(Component.literal("Save Category"), button -> saveAndClose())
                .bounds(width / 2 - 130, bottom, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(width / 2 - 28, bottom, 72, 20).build());
        if (original != null) {
            addRenderableWidget(Button.builder(Component.literal("Delete Category"), button -> deleteAndClose())
                    .bounds(width / 2 + 50, bottom, 104, 20).build());
        }
    }

    private Component typeLabel() {
        return Component.literal(draft.selectable
                ? "Type: Quest Page (opens a quest canvas)"
                : "Type: Group Header (dropdown only)");
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
        String previousId = original == null ? "" : original.id;
        if (draft.parent.equals(draft.id)) draft.parent = "";
        draft.normalize();

        if (original != null && !previousId.equals(draft.id)) {
            for (QuestCategoryDefinition category : CategoryRepository.all()) {
                if (!previousId.equals(category.parent)) continue;
                QuestCategoryDefinition updatedChild = category.copy();
                updatedChild.parent = draft.id;
                CategoryRepository.save(updatedChild);
            }
            for (QuestDefinition quest : QuestRepository.all()) {
                if (!previousId.equals(quest.chapter)) continue;
                quest.chapter = draft.id;
                QuestRepository.save(quest);
            }
            if (CategoryRepository.isExplicit(previousId)) CategoryRepository.delete(previousId);
        }

        CategoryRepository.save(draft);
        CategoryRepository.load();
        minecraft.setScreen(parent);
    }

    private void deleteAndClose() {
        if (original == null) {
            onClose();
            return;
        }

        String deletedId = original.id;
        String replacementParent = original.parent == null ? "" : original.parent.trim();

        for (QuestCategoryDefinition category : CategoryRepository.all()) {
            if (!deletedId.equals(category.parent)) continue;
            QuestCategoryDefinition updated = category.copy();
            updated.parent = replacementParent;
            CategoryRepository.save(updated);
        }

        String fallbackPage = findFallbackQuestPage(deletedId, replacementParent);
        for (QuestDefinition quest : QuestRepository.all()) {
            if (!deletedId.equals(quest.chapter)) continue;
            quest.chapter = fallbackPage;
            QuestRepository.save(quest);
        }

        CategoryRepository.delete(deletedId);
        CategoryRepository.load();
        QuestRepository.load();
        minecraft.setScreen(parent);
    }

    private String findFallbackQuestPage(String deletedId, String preferredParent) {
        QuestCategoryDefinition preferred = CategoryRepository.get(preferredParent);
        if (preferred != null && preferred.selectable && !preferred.id.equals(deletedId)) return preferred.id;

        for (QuestCategoryDefinition category : CategoryRepository.all()) {
            if (category.selectable && !category.id.equals(deletedId)) return category.id;
        }

        QuestCategoryDefinition fallback = new QuestCategoryDefinition();
        fallback.id = deletedId.equals("introduction") ? "uncategorized" : "introduction";
        fallback.title = deletedId.equals("introduction") ? "Uncategorized" : "Introduction";
        fallback.icon = "minecraft:book";
        fallback.parent = "";
        fallback.selectable = true;
        fallback.order = 0;
        CategoryRepository.save(fallback);
        return fallback.id;
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
        Layout layout = layout();
                graphics.fill(0, 0, width, height, QuestThemeManager.current().backgroundColor());
        graphics.fill(layout.left(), layout.panelTop(), layout.left() + layout.panelWidth(), layout.panelBottom(), QuestThemeManager.current().panelColor());
        graphics.drawCenteredString(font, title, width / 2, layout.panelTop() + 7, QuestThemeManager.current().titleTextColor());

        if (original == null) label(graphics, "Category ID", layout.left() + 12, layout.row(0));
        else label(graphics, "Category ID: " + original.id, layout.left() + 12, layout.row(0));
        label(graphics, "Display name", layout.left() + 12, layout.row(1));
        label(graphics, "Category behavior", layout.left() + 12, layout.row(2));
        label(graphics, "Icon item", layout.left() + 12, layout.row(3));
        label(graphics, "Parent category (blank = top level)", layout.left() + 12, layout.row(4));
        label(graphics, "Sort order", layout.left() + 12, layout.row(5));

        int noteY = Math.min(layout.panelBottom() - 14, layout.row(6));
        String note = draft.selectable
                ? "Quest Page: players can select this row and see quests assigned to it."
                : "Group Header: this row only expands/collapses its child categories.";
        graphics.drawString(font, Component.literal(note), layout.left() + 12, noteY, QuestThemeManager.current().mutedTextColor(), false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Layout layout() {
        int panelWidth = Math.min(430, Math.max(260, width - 60));
        int left = width / 2 - panelWidth / 2;
        int panelTop = 12;
        int bottomButtonY = Math.max(0, height - 26);
        int panelBottom = Math.max(panelTop + 220, bottomButtonY - 6);
        int contentTop = panelTop + 27;
        int available = Math.max(180, panelBottom - contentTop - 22);
        int rowStep = Math.min(36, Math.max(29, available / 7));
        return new Layout(panelWidth, left, panelTop, panelBottom, contentTop, rowStep, bottomButtonY);
    }

    private void label(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(font, Component.literal(text), x, y, QuestThemeManager.current().labelTextColor(), false);
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

    private record Layout(int panelWidth, int left, int panelTop, int panelBottom, int contentTop,
                          int rowStep, int bottomButtonY) {
        int row(int index) {
            return contentTop + index * rowStep;
        }
    }
}
