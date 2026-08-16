package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestTask;
import com.projectflint.flintquests.data.TaskType;
import com.projectflint.flintquests.network.QuestCheckmarkC2SPayload;
import com.projectflint.flintquests.network.QuestProgressRequestC2SPayload;
import com.projectflint.flintquests.theme.QuestTheme;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestDetailScreen extends Screen {
	private static final int TEXT_LINE_HEIGHT = 10;
	private static final int CHECKMARK_BLOCK_HEIGHT = 26;

	private final Screen parent;
	private final QuestDefinition quest;
	private final Map<String, Button> checkmarkButtons = new LinkedHashMap<>();
	private List<DetailPage> pages = List.of();
	private int pageIndex;

	public QuestDetailScreen(Screen parent, QuestDefinition quest) {
		super(Component.literal(quest.title));
		this.parent = parent;
		this.quest = quest;
	}

	@Override
	protected void init() {
		refreshPageWidgets();
		if (minecraft != null && minecraft.player != null) {
			ClientPlayNetworking.send(new QuestProgressRequestC2SPayload("quest_detail_open"));
		}
	}

	private void refreshPageWidgets() {
		clearWidgets();
		checkmarkButtons.clear();

		DetailLayout layout = layout();
		pages = buildPages(layout);
		if (pages.isEmpty()) pages = List.of(new DetailPage(List.of()));
		pageIndex = Math.max(0, Math.min(pageIndex, pages.size() - 1));

		DetailPage page = pages.get(pageIndex);
		for (PlacedBlock placed : page.blocks()) {
			if (placed.block().type() != BlockType.CHECKMARK || placed.block().task() == null) continue;
			QuestTask task = placed.block().task();
			boolean complete = ClientQuestProgress.taskComplete(quest.id, task.id);
			Button check = Button.builder(checkmarkLabel(task, complete), button -> submitCheckmark(task, button))
					.bounds(layout.left() + 18, layout.bodyTop() + placed.yOffset(), layout.panelWidth() - 36, 20)
					.build();
			check.active = !complete && !ClientQuestProgress.questComplete(quest.id);
			addRenderableWidget(check);
			checkmarkButtons.put(task.id, check);
		}

		int navY = layout.panelBottom() - 25;
		if (pages.size() > 1) {
			Button previous = Button.builder(Component.literal("< Previous"), button -> changePage(-1))
					.bounds(layout.left() + 16, navY, 82, 20).build();
			previous.active = pageIndex > 0;
			addRenderableWidget(previous);

			Button next = Button.builder(Component.literal("Next >"), button -> changePage(1))
					.bounds(layout.left() + layout.panelWidth() - 98, navY, 82, 20).build();
			next.active = pageIndex < pages.size() - 1;
			addRenderableWidget(next);
		}

		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(width / 2 - 40, navY, 80, 20).build());
	}

	private void changePage(int direction) {
		int next = Math.max(0, Math.min(pageIndex + direction, pages.size() - 1));
		if (next == pageIndex) return;
		pageIndex = next;
		refreshPageWidgets();
	}

	public void refreshFromProgress() {
		for (QuestTask task : quest.tasks) {
			if (task.type != TaskType.CHECKMARK) continue;
			Button button = checkmarkButtons.get(task.id);
			if (button == null) continue;
			boolean complete = ClientQuestProgress.taskComplete(quest.id, task.id);
			button.setMessage(checkmarkLabel(task, complete));
			button.active = !complete && !ClientQuestProgress.questComplete(quest.id);
		}
	}

	private Component checkmarkLabel(QuestTask task, boolean complete) {
		return Component.empty()
				.append(Component.literal(complete ? "[✓]  " : "[ ]  "))
				.append(LegacyText.parse(task.displayLabel()));
	}

	private void submitCheckmark(QuestTask task, Button button) {
		if (minecraft.player == null || ClientQuestProgress.taskComplete(quest.id, task.id)) return;
		ClientPlayNetworking.send(new QuestCheckmarkC2SPayload(quest.id, task.id));
		button.active = false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		DetailLayout layout = layout();
		boolean questComplete = ClientQuestProgress.questComplete(quest.id);

		QuestTheme theme = QuestThemeManager.current();
		graphics.fill(0, 0, width, height, theme.backgroundColor());
		graphics.fill(layout.left(), layout.top(), layout.left() + layout.panelWidth(), layout.panelBottom(), theme.panelColor());
		graphics.fill(layout.left(), layout.top(), layout.left() + layout.panelWidth(), layout.top() + 1,
				questComplete ? theme.completedColor() : theme.lockedColor());
		graphics.fill(layout.left(), layout.panelBottom() - 1, layout.left() + layout.panelWidth(), layout.panelBottom(), theme.panelEdgeColor());

		graphics.renderItem(QuestIconHelper.stackFor(quest), layout.left() + 18, layout.top() + 18);
		graphics.drawString(font, LegacyText.parse(quest.title), layout.left() + 44, layout.top() + 18, theme.titleTextColor(), false);
		QuestCategoryDefinition category = CategoryRepository.get(quest.chapter);
		String categoryName = category == null ? quest.chapter : category.title;
		graphics.drawString(font, LegacyText.parse(categoryName), layout.left() + 44, layout.top() + 31, theme.mutedTextColor(), false);

		if (questComplete) {
			graphics.drawString(font, Component.literal("✓ COMPLETED"), layout.left() + layout.panelWidth() - 86,
					layout.top() + 20, theme.completedColor(), false);
		}

		if (pages.size() > 1) {
			String pageText = "Page " + (pageIndex + 1) + " / " + pages.size();
			graphics.drawCenteredString(font, Component.literal(pageText), width / 2, layout.panelBottom() - 39, theme.mutedTextColor());
		}

		if (!pages.isEmpty()) {
			DetailPage page = pages.get(Math.max(0, Math.min(pageIndex, pages.size() - 1)));
			for (PlacedBlock placed : page.blocks()) {
				ContentBlock block = placed.block();
				if (block.type() != BlockType.TEXT || block.line() == null) continue;
				graphics.drawString(font, block.line(), layout.left() + block.xOffset(),
						layout.bodyTop() + placed.yOffset(), block.color(), false);
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private List<DetailPage> buildPages(DetailLayout layout) {
		List<ContentBlock> blocks = buildContentBlocks(layout);
		List<DetailPage> result = new ArrayList<>();
		List<PlacedBlock> current = new ArrayList<>();
		int used = 0;
		int capacity = Math.max(30, layout.bodyBottom() - layout.bodyTop());

		for (int index = 0; index < blocks.size(); index++) {
			ContentBlock block = blocks.get(index);
			int height = block.height();
			int required = height;
			if (block.keepWithNext() && index + 1 < blocks.size()) required += blocks.get(index + 1).height();
			if (!current.isEmpty() && (used + height > capacity || used + required > capacity)) {
				result.add(new DetailPage(List.copyOf(current)));
				current.clear();
				used = 0;
			}
			current.add(new PlacedBlock(block, used));
			used += height;
		}

		if (!current.isEmpty()) result.add(new DetailPage(List.copyOf(current)));
		if (result.isEmpty()) result.add(new DetailPage(List.of()));
		return result;
	}

	private List<ContentBlock> buildContentBlocks(DetailLayout layout) {
		List<ContentBlock> blocks = new ArrayList<>();
		int textWidth = layout.panelWidth() - 36;

		appendWrappedBlocks(blocks, quest.description, textWidth, 18, QuestThemeManager.current().bodyTextColor());
		blocks.add(ContentBlock.spacer(8));
		blocks.add(ContentBlock.heading(Component.literal("Tasks").getVisualOrderText(), 18, QuestThemeManager.current().accentTextColor(), 18));

		for (QuestTask task : quest.tasks) {
			if (task.type == TaskType.CHECKMARK) {
				blocks.add(ContentBlock.checkmark(task));
				continue;
			}

			boolean complete = ClientQuestProgress.taskComplete(quest.id, task.id);
			String state = complete ? "✓ " : "• ";
			String suffix = task.count > 1 ? "  x" + task.count : "";
			String optional = task.optional ? "  (optional)" : "";
			int color = complete ? QuestThemeManager.current().completedColor() : QuestThemeManager.current().titleTextColor();
			appendWrappedBlocks(blocks, state + task.displayLabel() + suffix + optional, textWidth - 6, 24, color);
			blocks.add(ContentBlock.spacer(4));
		}
		return blocks;
	}

	private void appendWrappedBlocks(List<ContentBlock> blocks, String rawText, int width, int xOffset, int color) {
		for (Component paragraph : LegacyText.lines(rawText)) {
			if (paragraph.getString().isBlank()) {
				blocks.add(ContentBlock.spacer(TEXT_LINE_HEIGHT));
				continue;
			}
			for (FormattedCharSequence line : font.split(paragraph, width)) {
				blocks.add(ContentBlock.text(line, xOffset, color, TEXT_LINE_HEIGHT));
			}
		}
	}

	private DetailLayout layout() {
		int panelWidth = Math.min(520, Math.max(280, width - 32));
		int panelHeight = Math.min(380, Math.max(220, height - 30));
		int left = width / 2 - panelWidth / 2;
		int top = Math.max(8, height / 2 - panelHeight / 2);
		int panelBottom = Math.min(height - 8, top + panelHeight);
		int bodyTop = top + 56;
		int bodyBottom = panelBottom - 52;
		return new DetailLayout(left, top, panelWidth, panelBottom, bodyTop, bodyBottom);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private enum BlockType {
		TEXT,
		CHECKMARK,
		SPACER
	}

	private record ContentBlock(BlockType type, FormattedCharSequence line, QuestTask task, int xOffset, int color, int height, boolean keepWithNext) {
		static ContentBlock text(FormattedCharSequence line, int xOffset, int color, int height) {
			return new ContentBlock(BlockType.TEXT, line, null, xOffset, color, height, false);
		}

		static ContentBlock heading(FormattedCharSequence line, int xOffset, int color, int height) {
			return new ContentBlock(BlockType.TEXT, line, null, xOffset, color, height, true);
		}

		static ContentBlock checkmark(QuestTask task) {
			return new ContentBlock(BlockType.CHECKMARK, null, task, 18, 0xFFFFFFFF, CHECKMARK_BLOCK_HEIGHT, false);
		}

		static ContentBlock spacer(int height) {
			return new ContentBlock(BlockType.SPACER, null, null, 0, 0, height, false);
		}
	}

	private record PlacedBlock(ContentBlock block, int yOffset) {
	}

	private record DetailPage(List<PlacedBlock> blocks) {
	}

	private record DetailLayout(int left, int top, int panelWidth, int panelBottom, int bodyTop, int bodyBottom) {
	}
}
