package com.projectflint.flintquests.client;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class SearchSelectScreen extends Screen {
    public enum Kind {
        ITEM,
        BLOCK,
        CATEGORY_PAGE,
        CATEGORY_ANY,
        QUEST
    }

    private static final int VISIBLE_ROWS = 10;

    private final Screen parent;
    private final Kind kind;
    private final Consumer<String> onSelect;
    private final List<Entry> allEntries = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private final List<Button> resultButtons = new ArrayList<>();

    private EditBox searchBox;
    private int scrollOffset;

    public SearchSelectScreen(Screen parent, Component title, Kind kind, Consumer<String> onSelect) {
        super(title);
        this.parent = parent;
        this.kind = kind;
        this.onSelect = onSelect;
        loadEntries();
    }

    private void loadEntries() {
        allEntries.clear();
        switch (kind) {
            case ITEM -> BuiltInRegistries.ITEM.stream().forEach(item -> {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                String name = new ItemStack(item).getHoverName().getString();
                allEntries.add(new Entry(id, name + "  (" + id + ")"));
            });
            case BLOCK -> BuiltInRegistries.BLOCK.stream().forEach(block -> {
                String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                String name = block.getName().getString();
                allEntries.add(new Entry(id, name + "  (" + id + ")"));
            });
            case CATEGORY_PAGE -> CategoryRepository.all().stream()
                    .filter(category -> category.selectable)
                    .forEach(category -> allEntries.add(new Entry(category.id, category.title + "  (" + category.id + ")")));
            case CATEGORY_ANY -> CategoryRepository.all().forEach(category ->
                    allEntries.add(new Entry(category.id, category.title + "  (" + category.id + ")")));
            case QUEST -> QuestRepository.all().forEach(quest ->
                    allEntries.add(new Entry(quest.id, quest.title + "  (" + quest.id + ")")));
        }
        allEntries.sort(Comparator.comparing(Entry::display, String.CASE_INSENSITIVE_ORDER));
        filtered.addAll(allEntries);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(440, width - 48);
        int left = width / 2 - panelWidth / 2;
        searchBox = new EditBox(font, left + 12, 40, panelWidth - 24, 20, Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setResponder(value -> applyFilter());
        addRenderableWidget(searchBox);

        resultButtons.clear();
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int row = i;
            Button button = Button.builder(Component.empty(), ignored -> selectRow(row))
                    .bounds(left + 12, 68 + i * 22, panelWidth - 24, 20)
                    .build();
            resultButtons.add(button);
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(width / 2 - 40, height - 30, 80, 20).build());
        refreshRows();
    }

    private void applyFilter() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        if (query.isBlank()) {
            filtered.addAll(allEntries);
        } else {
            for (Entry entry : allEntries) {
                if (entry.id().toLowerCase(Locale.ROOT).contains(query)
                        || entry.display().toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }
            }
        }
        scrollOffset = 0;
        refreshRows();
    }

    private void refreshRows() {
        int maxOffset = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
        for (int i = 0; i < resultButtons.size(); i++) {
            int index = scrollOffset + i;
            Button button = resultButtons.get(i);
            if (index < filtered.size()) {
                button.active = true;
                button.setMessage(Component.literal(filtered.get(index).display()));
            } else {
                button.active = false;
                button.setMessage(Component.empty());
            }
        }
    }

    private void selectRow(int row) {
        int index = scrollOffset + row;
        if (index < 0 || index >= filtered.size()) return;
        onSelect.accept(filtered.get(index).id());
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0D) {
            scrollOffset -= (int) Math.signum(vertical);
            refreshRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(440, width - 48);
        int left = width / 2 - panelWidth / 2;
        int panelTop = 18;
        int panelBottom = Math.min(height - 38, 68 + VISIBLE_ROWS * 22 + 10);
                graphics.fill(0, 0, width, height, QuestThemeManager.current().backgroundColor());
        graphics.fill(left, panelTop, left + panelWidth, panelBottom, QuestThemeManager.current().panelColor());
        graphics.drawCenteredString(font, title, width / 2, 25, QuestThemeManager.current().titleTextColor());
        graphics.drawString(font, Component.literal(filtered.size() + " result(s)"), left + 12, panelBottom - 13, QuestThemeManager.current().mutedTextColor(), false);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (kind == Kind.ITEM || kind == Kind.BLOCK) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scrollOffset + i;
                if (index >= filtered.size()) break;
                graphics.renderItem(QuestIconHelper.stackFor(filtered.get(index).id()), left + 16, 70 + i * 22);
            }
        } else if (kind == Kind.CATEGORY_PAGE || kind == Kind.CATEGORY_ANY) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scrollOffset + i;
                if (index >= filtered.size()) break;
                QuestCategoryDefinition category = CategoryRepository.get(filtered.get(index).id());
                if (category != null) graphics.renderItem(QuestIconHelper.stackFor(category.icon), left + 16, 70 + i * 22);
            }
        } else if (kind == Kind.QUEST) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scrollOffset + i;
                if (index >= filtered.size()) break;
                QuestDefinition quest = QuestRepository.get(filtered.get(index).id());
                if (quest != null) graphics.renderItem(QuestIconHelper.stackFor(quest), left + 16, 70 + i * 22);
            }
        }
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

    private record Entry(String id, String display) {
    }
}
