package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestTask;
import com.projectflint.flintquests.data.TaskType;
import com.projectflint.flintquests.network.QuestCheckmarkC2SPayload;
import com.projectflint.flintquests.network.QuestProgressRequestC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestDetailScreen extends Screen {
	private final Screen parent;
	private final QuestDefinition quest;
	private final Map<String, Button> checkmarkButtons = new LinkedHashMap<>();

	public QuestDetailScreen(Screen parent, QuestDefinition quest) {
		super(Component.literal(quest.title));
		this.parent = parent;
		this.quest = quest;
	}

	@Override
	protected void init() {
		checkmarkButtons.clear();

		int panelWidth = Math.min(430, width - 40);
		int panelHeight = Math.min(300, height - 44);
		int left = width / 2 - panelWidth / 2;
		int top = height / 2 - panelHeight / 2 - 4;
		int taskY = top + 112;

		for (QuestTask task : quest.tasks) {
			if (taskY > top + panelHeight - 54) break;
			if (task.type == TaskType.CHECKMARK) {
				boolean complete = ClientQuestProgress.taskComplete(quest.id, task.id);
				Button check = Button.builder(checkmarkLabel(task, complete), button -> submitCheckmark(task, button))
						.bounds(left + 22, taskY - 3, panelWidth - 44, 20)
						.build();
				check.active = !complete && !ClientQuestProgress.questComplete(quest.id);
				addRenderableWidget(check);
				checkmarkButtons.put(task.id, check);
				taskY += 26;
			} else {
				taskY += 18;
			}
		}

		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(width / 2 - 40, height - 34, 80, 20).build());

		if (minecraft != null && minecraft.player != null) {
			ClientPlayNetworking.send(new QuestProgressRequestC2SPayload("quest_detail_open"));
		}
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
		return Component.literal((complete ? "[✓]  " : "[ ]  ") + task.displayLabel());
	}

	private void submitCheckmark(QuestTask task, Button button) {
		if (minecraft.player == null || ClientQuestProgress.taskComplete(quest.id, task.id)) return;
		ClientPlayNetworking.send(new QuestCheckmarkC2SPayload(quest.id, task.id));
		button.active = false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int panelWidth = Math.min(430, width - 40);
		int panelHeight = Math.min(300, height - 44);
		int left = width / 2 - panelWidth / 2;
		int top = height / 2 - panelHeight / 2 - 4;
		boolean questComplete = ClientQuestProgress.questComplete(quest.id);

		graphics.fill(0, 0, width, height, 0xB818202A);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0232D39);
		graphics.fill(left, top, left + panelWidth, top + 1, questComplete ? 0xFF72FF63 : 0xFF748196);
		graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, 0xFF111820);

		graphics.renderItem(QuestIconHelper.stackFor(quest), left + 18, top + 18);
		graphics.drawString(font, title, left + 44, top + 18, 0xFFF3F5F7, false);
		QuestCategoryDefinition category = CategoryRepository.get(quest.chapter);
		String categoryName = category == null ? quest.chapter : category.title;
		graphics.drawString(font, Component.literal(categoryName), left + 44, top + 31, 0xFF8FA0B2, false);

		if (questComplete) {
			graphics.drawString(font, Component.literal("✓ COMPLETED"), left + panelWidth - 86, top + 20, 0xFF72FF63, false);
		}

		int textY = top + 56;
		graphics.drawString(font, Component.literal(quest.description), left + 18, textY, 0xFFD5DCE5, false);
		textY += 36;
		graphics.drawString(font, Component.literal("Tasks"), left + 18, textY, 0xFFFFD46A, false);
		textY += 18;

		for (QuestTask task : quest.tasks) {
			if (textY > top + panelHeight - 48) break;
			if (task.type == TaskType.CHECKMARK) {
				textY += 26;
				continue;
			}

			boolean complete = ClientQuestProgress.taskComplete(quest.id, task.id);
			String state = complete ? "✓ " : "• ";
			String suffix = task.count > 1 ? "  x" + task.count : "";
			String optional = task.optional ? "  (optional)" : "";
			int color = complete ? 0xFF72FF63 : 0xFFE6EAF0;
			graphics.drawString(font, Component.literal(state + task.displayLabel() + suffix + optional), left + 24, textY, color, false);
			textY += 18;
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
