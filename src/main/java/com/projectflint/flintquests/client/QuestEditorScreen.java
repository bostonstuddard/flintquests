package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CompletionMode;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestNodeShape;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.data.QuestReward;
import com.projectflint.flintquests.data.QuestTask;
import com.projectflint.flintquests.data.TaskType;
import com.projectflint.flintquests.network.QuestProgressRequestC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;

public final class QuestEditorScreen extends Screen {
	private static final String QUEST_NAMESPACE = "flintquests:";

	private enum EditorPage {
		QUEST("Quest"),
		LOOK("Look"),
		FLOW("Flow"),
		RULES("Rules"),
		TASK("Task"),
		REWARD("Reward");

		private final String title;

		EditorPage(String title) {
			this.title = title;
		}
	}

	private final Screen parent;
	private final QuestDefinition original;
	private final QuestDefinition draft;
	private EditorPage page = EditorPage.QUEST;

	private EditBox idPathBox;
	private EditBox titleBox;
	private MultiLineEditBox descriptionBox;
	private EditBox chapterBox;
	private EditBox iconBox;
	private EditBox dependenciesBox;
	private EditBox xBox;
	private EditBox yBox;

	private EditBox taskIdBox;
	private EditBox taskLabelBox;
	private EditBox taskTargetBox;
	private EditBox taskCountBox;
	private int taskIndex;
	private TaskType currentTaskType = TaskType.OBTAIN_ITEM;
	private boolean currentTaskOptional;
	private Button taskTypeButton;
	private Button taskOptionalButton;
	private Button targetSearchButton;

	private EditBox rewardItemBox;
	private EditBox rewardCountBox;
	private int rewardIndex;
	private String saveError = "";

	private int row0;
	private int row1;
	private int row2;
	private int row3;
	private int row4;

	public QuestEditorScreen(Screen parent, QuestDefinition definition) {
		this(parent, definition, null);
	}

	public QuestEditorScreen(Screen parent, QuestDefinition definition, String defaultCategory) {
		super(Component.literal(definition == null ? "Create Quest" : "Edit Quest"));
		this.parent = parent;
		this.original = definition;
		this.draft = definition == null ? new QuestDefinition() : copy(definition);
		if (definition == null && defaultCategory != null && !defaultCategory.isBlank()) {
			this.draft.chapter = defaultCategory.trim();
		}
		if (this.draft.tasks.isEmpty()) {
			this.draft.tasks.add(new QuestTask("task_0", TaskType.OBTAIN_ITEM, "Obtain a stick", "minecraft:stick", 1, false));
		}
	}

	@Override
	protected void init() {
		if (!ConfigManager.devToolsEnabled()) {
			minecraft.setScreen(parent);
			return;
		}
		EditorLayout layout = layout();
		buildTabs(layout);
		switch (page) {
			case QUEST -> buildQuestPage(layout);
			case LOOK -> buildLookPage(layout);
			case FLOW -> buildFlowPage(layout);
			case RULES -> buildRulesPage(layout);
			case TASK -> buildTaskPage(layout);
			case REWARD -> buildRewardPage(layout);
		}
		buildFooter(layout);
	}

	private void buildTabs(EditorLayout layout) {
		int gap = 2;
		EditorPage[] pages = EditorPage.values();
		int tabWidth = Math.max(34, (layout.panelWidth() - gap * (pages.length - 1)) / pages.length);
		for (int i = 0; i < pages.length; i++) {
			EditorPage target = pages[i];
			String label = target == page ? "[" + target.title + "]" : target.title;
			addRenderableWidget(Button.builder(Component.literal(label), button -> switchPage(target))
					.bounds(layout.left() + i * (tabWidth + gap), 5, tabWidth, 18).build());
		}
	}

	private void buildQuestPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		int namespaceWidth = Math.min(width - 70, Math.max(76, font.width(QUEST_NAMESPACE) + 8));
		row0 = y;
		idPathBox = field(x + namespaceWidth, y + 11, width - namespaceWidth, 18, idPath(draft.id));
		y += 34;
		row1 = y;
		titleBox = field(x, y + 11, width, 18, draft.title);
		y += 34;
		row2 = y;
		int descriptionHeight = Math.max(28, layout.panelBottom() - (y + 12) - 8);
		descriptionBox = MultiLineEditBox.builder()
				.setX(x)
				.setY(y + 12)
				.setPlaceholder(Component.literal("One instruction per line..."))
				.build(font, width, descriptionHeight, Component.literal("Quest description / lore"));
		descriptionBox.setCharacterLimit(4096);
		descriptionBox.setValue(normalizeLegacyNewlines(draft.description), true);
		addRenderableWidget(descriptionBox);
	}

	private void buildLookPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		row0 = y;
		chapterBox = field(x, y + 11, width - 62, 18, draft.chapter);
		addRenderableWidget(Button.builder(Component.literal("Choose"), button -> chooseCategory())
				.bounds(x + width - 56, y + 11, 56, 18).build());
		y += 38;
		row1 = y;
		iconBox = field(x, y + 11, width - 62, 18, draft.icon);
		addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchIcon())
				.bounds(x + width - 56, y + 11, 56, 18).build());
		y += 38;
		row2 = y;
		addRenderableWidget(Button.builder(shapeLabel(), button -> {
			draft.nodeShape = effectiveShape().next();
			button.setMessage(shapeLabel());
		}).bounds(x, y + 11, width, 20).build());
	}

	private void buildFlowPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		row0 = y;
		int half = Math.max(40, (width - 8) / 2);
		int otherHalf = Math.max(40, width - half - 8);
		xBox = field(x, y + 18, half, 18, Integer.toString(draft.x));
		yBox = field(x + half + 8, y + 18, otherHalf, 18, Integer.toString(draft.y));
	}

	private void buildRulesPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		int rowStep = Math.max(31, Math.min(39, (layout.panelBottom() - y - 4) / 4));

		row0 = y;
		dependenciesBox = field(x, y + 11, width - 62, 18, String.join(",", draft.dependencies));
		addRenderableWidget(Button.builder(Component.literal("Add"), button -> addDependency())
				.bounds(x + width - 56, y + 11, 56, 18).build());
		y += rowStep;

		row1 = y;
		addRenderableWidget(Button.builder(Component.literal("Required quests: " + draft.dependencyMode), button -> {
			draft.dependencyMode = draft.dependencyMode == CompletionMode.ALL ? CompletionMode.ANY : CompletionMode.ALL;
			button.setMessage(Component.literal("Required quests: " + draft.dependencyMode));
		}).bounds(x, y + 11, width, 20).build());
		y += rowStep;

		row2 = y;
		addRenderableWidget(Button.builder(Component.literal("Tasks: " + draft.taskMode), button -> {
			draft.taskMode = draft.taskMode == CompletionMode.ALL ? CompletionMode.ANY : CompletionMode.ALL;
			button.setMessage(Component.literal("Tasks: " + draft.taskMode));
		}).bounds(x, y + 11, width, 20).build());
		y += rowStep;

		row3 = y;
		addRenderableWidget(Button.builder(hiddenLabel(), button -> {
			draft.hiddenUntilDependencies = !draft.hiddenUntilDependencies;
			button.setMessage(hiddenLabel());
		}).bounds(x, y + 11, width, 20).build());
	}

	private void buildTaskPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		int rowStep = compactRowStep(layout);

		int navWidth = Math.min(54, Math.max(34, width / 6));
		addRenderableWidget(Button.builder(Component.literal("<"), button -> previousTask())
				.bounds(x, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> nextTask())
				.bounds(x + navWidth + 3, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+"), button -> addTask())
				.bounds(x + width - navWidth * 2 - 3, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("-"), button -> removeTask())
				.bounds(x + width - navWidth, layout.panelTop() + 5, navWidth, 18).build());

		row0 = y;
		taskIdBox = field(x, y + 11, width, 18, "");
		y += rowStep;
		row1 = y;
		taskLabelBox = field(x, y + 11, width, 18, "");
		y += rowStep;
		row2 = y;
		taskTypeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleTaskType())
				.bounds(x, y + 11, width, 20).build());
		y += rowStep;
		row3 = y;
		taskTargetBox = field(x, y + 11, width - 62, 18, "");
		targetSearchButton = addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchTaskTarget())
				.bounds(x + width - 56, y + 11, 56, 18).build());
		y += rowStep;
		row4 = y;
		int countWidth = Math.max(46, Math.min(100, width / 3));
		taskCountBox = field(x, y + 11, countWidth, 18, "1");
		int optionalWidth = Math.max(40, width - countWidth - 8);
		taskOptionalButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
			currentTaskOptional = !currentTaskOptional;
			updateTaskButtonLabels();
		}).bounds(x + countWidth + 8, y + 11, optionalWidth, 18).build());
		loadTaskFields();
	}

	private void buildRewardPage(EditorLayout layout) {
		int x = layout.contentLeft();
		int width = layout.contentWidth();
		int y = layout.contentTop();
		int navWidth = Math.min(62, Math.max(38, width / 6));

		if (draft.rewards.isEmpty()) {
			addRenderableWidget(Button.builder(Component.literal("+ Add Reward"), button -> addReward())
					.bounds(x, y + 28, width, 20).build());
			return;
		}

		addRenderableWidget(Button.builder(Component.literal("<"), button -> previousReward())
				.bounds(x, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> nextReward())
				.bounds(x + navWidth + 3, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+"), button -> addReward())
				.bounds(x + width - navWidth * 2 - 3, layout.panelTop() + 5, navWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("-"), button -> removeReward())
				.bounds(x + width - navWidth, layout.panelTop() + 5, navWidth, 18).build());

		row0 = y;
		rewardItemBox = field(x, y + 11, width - 62, 18, draft.rewards.get(rewardIndex).item);
		addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchRewardItem())
				.bounds(x + width - 56, y + 11, 56, 18).build());
		y += 38;
		row1 = y;
		rewardCountBox = field(x, y + 11, Math.min(120, width), 18, Integer.toString(draft.rewards.get(rewardIndex).count));
	}

	private void addReward() {
		saveRewardFields();
		draft.rewards.add(new QuestReward("minecraft:diamond", 1));
		rewardIndex = draft.rewards.size() - 1;
		rebuildRewardPage();
	}

	private void removeReward() {
		if (draft.rewards.isEmpty()) return;
		draft.rewards.remove(rewardIndex);
		rewardIndex = Math.max(0, Math.min(rewardIndex, draft.rewards.size() - 1));
		rebuildRewardPage();
	}

	private void previousReward() {
		saveRewardFields();
		if (rewardIndex > 0) rewardIndex--;
		rebuildRewardPage();
	}

	private void nextReward() {
		saveRewardFields();
		if (rewardIndex < draft.rewards.size() - 1) rewardIndex++;
		rebuildRewardPage();
	}

	private void rebuildRewardPage() {
		clearWidgets();
		init();
	}

	private void searchRewardItem() {
		saveRewardFields();
		int selectedReward = rewardIndex;
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Reward Item"), SearchSelectScreen.Kind.ITEM,
				id -> {
					if (selectedReward >= 0 && selectedReward < draft.rewards.size()) draft.rewards.get(selectedReward).item = id;
				}));
	}

	private void saveRewardFields() {
		if (draft.rewards.isEmpty() || rewardItemBox == null || rewardCountBox == null) return;
		QuestReward reward = draft.rewards.get(rewardIndex);
		reward.item = rewardItemBox.getValue().trim();
		reward.count = parseInt(rewardCountBox.getValue(), 1);
		reward.normalize();
	}

	private int compactRowStep(EditorLayout layout) {
		int available = Math.max(100, layout.panelBottom() - layout.contentTop() - 2);
		return Math.max(22, Math.min(34, available / 5));
	}

	private void buildFooter(EditorLayout layout) {
		int gap = 3;
		int available = Math.max(160, Math.min(430, width - 8));
		int left = width / 2 - available / 2;
		int count = original == null ? 2 : 3;
		int buttonWidth = Math.max(48, (available - gap * (count - 1)) / count);
		int x = left;
		addRenderableWidget(Button.builder(Component.literal("Save Quest"), button -> saveAndClose())
				.bounds(x, layout.footerY(), buttonWidth, 20).build());
		x += buttonWidth + gap;
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
				.bounds(x, layout.footerY(), buttonWidth, 20).build());
		if (original != null) {
			x += buttonWidth + gap;
			addRenderableWidget(Button.builder(Component.literal("Delete Quest"), button -> deleteAndClose())
					.bounds(x, layout.footerY(), buttonWidth, 20).build());
		}
	}

	private void switchPage(EditorPage target) {
		if (target == page) return;
		saveVisiblePage();
		page = target;
		clearWidgets();
		init();
	}

	private void saveVisiblePage() {
		switch (page) {
			case QUEST -> saveQuestPage();
			case LOOK -> saveLookPage();
			case FLOW -> saveFlowPage();
			case RULES -> saveRulesPage();
			case TASK -> saveTaskFields();
			case REWARD -> saveRewardFields();
		}
	}

	private QuestNodeShape effectiveShape() {
		return draft.nodeShape == null ? QuestNodeShape.SQUARE : draft.nodeShape;
	}

	private Component shapeLabel() {
		return Component.literal("Node shape: " + effectiveShape().displayName());
	}

	private Component hiddenLabel() {
		return Component.literal("Hide until ready: " + (draft.hiddenUntilDependencies ? "Yes" : "No"));
	}

	private EditBox field(int x, int y, int width, int height, String value) {
		EditBox box = new EditBox(font, x, y, Math.max(36, width), height, Component.empty());
		box.setMaxLength(512);
		box.setValue(value == null ? "" : value);
		addRenderableWidget(box);
		return box;
	}

	private void chooseCategory() {
		saveLookPage();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Quest Category"), SearchSelectScreen.Kind.CATEGORY_PAGE,
				id -> draft.chapter = id));
	}

	private void searchIcon() {
		saveLookPage();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Quest Icon"), SearchSelectScreen.Kind.ITEM,
				id -> draft.icon = id));
	}

	private void addDependency() {
		saveRulesPage();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Add Required Quest — any category"), SearchSelectScreen.Kind.QUEST,
				id -> {
					if (!id.equals(draft.id) && !draft.dependencies.contains(id)) draft.dependencies.add(id);
				}));
	}

	private void searchTaskTarget() {
		SearchSelectScreen.Kind kind = switch (currentTaskType) {
			case OBTAIN_ITEM, USE_ITEM -> SearchSelectScreen.Kind.ITEM;
			case BREAK_BLOCK, INTERACT_BLOCK -> SearchSelectScreen.Kind.BLOCK;
			case CUSTOM_EVENT -> SearchSelectScreen.Kind.CUSTOM_EVENT;
			default -> null;
		};
		if (kind == null) return;
		saveTaskFields();
		int selectedTask = taskIndex;
		Component pickerTitle = currentTaskType == TaskType.CUSTOM_EVENT
				? Component.literal("Choose Registered Custom Event")
				: Component.literal("Choose Task Target");
		minecraft.setScreen(new SearchSelectScreen(this, pickerTitle, kind,
				id -> draft.tasks.get(selectedTask).target = id));
	}

	private void cycleTaskType() {
		saveTaskFields();
		TaskType[] values = TaskType.values();
		currentTaskType = values[(currentTaskType.ordinal() + 1) % values.length];
		draft.tasks.get(taskIndex).type = currentTaskType;
		if (currentTaskType == TaskType.CHECKMARK) {
			taskTargetBox.setValue("");
			taskCountBox.setValue("1");
		}
		updateTaskButtonLabels();
	}

	private void previousTask() {
		saveTaskFields();
		if (taskIndex > 0) taskIndex--;
		loadTaskFields();
	}

	private void nextTask() {
		saveTaskFields();
		if (taskIndex < draft.tasks.size() - 1) taskIndex++;
		loadTaskFields();
	}

	private void addTask() {
		saveTaskFields();
		draft.tasks.add(new QuestTask("task_" + draft.tasks.size(), TaskType.OBTAIN_ITEM, "", "minecraft:stick", 1, false));
		taskIndex = draft.tasks.size() - 1;
		loadTaskFields();
	}

	private void removeTask() {
		if (draft.tasks.size() <= 1) return;
		draft.tasks.remove(taskIndex);
		taskIndex = Math.max(0, Math.min(taskIndex, draft.tasks.size() - 1));
		loadTaskFields();
	}

	private void loadTaskFields() {
		if (taskIdBox == null || draft.tasks.isEmpty()) return;
		QuestTask task = draft.tasks.get(taskIndex);
		currentTaskType = task.type;
		currentTaskOptional = task.optional;
		taskIdBox.setValue(task.id);
		taskLabelBox.setValue(task.label == null ? "" : task.label);
		taskTargetBox.setValue(task.target);
		taskCountBox.setValue(Integer.toString(task.count));
		updateTaskButtonLabels();
	}

	private void updateTaskButtonLabels() {
		if (taskTypeButton != null) taskTypeButton.setMessage(Component.literal("Completion: " + friendlyTaskType(currentTaskType)));
		if (taskOptionalButton != null) taskOptionalButton.setMessage(Component.literal(currentTaskOptional ? "Optional" : "Required"));
		boolean searchableTarget = currentTaskType == TaskType.OBTAIN_ITEM || currentTaskType == TaskType.USE_ITEM
				|| currentTaskType == TaskType.BREAK_BLOCK || currentTaskType == TaskType.INTERACT_BLOCK
				|| currentTaskType == TaskType.CUSTOM_EVENT;
		boolean hasTarget = currentTaskType != TaskType.CHECKMARK;
		if (targetSearchButton != null) targetSearchButton.active = searchableTarget;
		if (taskTargetBox != null) taskTargetBox.active = hasTarget;
		if (taskCountBox != null) taskCountBox.active = hasTarget;
	}

	private String friendlyTaskType(TaskType type) {
		return switch (type) {
			case OBTAIN_ITEM -> "Obtain Item";
			case BREAK_BLOCK -> "Break Block";
			case USE_ITEM -> "Use Item";
			case INTERACT_BLOCK -> "Interact With Block";
			case CUSTOM_EVENT -> "Custom Event";
			case CHECKMARK -> "Manual Checkmark";
		};
	}

	private void saveTaskFields() {
		if (draft.tasks.isEmpty() || taskIdBox == null) return;
		QuestTask task = draft.tasks.get(taskIndex);
		task.id = taskIdBox.getValue().trim();
		task.type = currentTaskType;
		task.label = taskLabelBox.getValue().trim();
		task.target = taskTargetBox.getValue().trim();
		task.count = parseInt(taskCountBox.getValue(), 1);
		task.optional = currentTaskOptional;
		task.normalize(taskIndex);
	}

	private void saveQuestPage() {
		if (idPathBox == null) return;
		String path = QuestDefinition.normalizeIdPath(idPathBox.getValue());
		draft.id = QUEST_NAMESPACE + (path.isBlank() ? "new_quest" : path);
		draft.title = titleBox.getValue().trim();
		draft.description = descriptionBox.getValue();
	}

	private void saveLookPage() {
		if (chapterBox == null) return;
		draft.chapter = chapterBox.getValue().trim();
		draft.icon = iconBox.getValue().trim();
	}

	private void saveFlowPage() {
		if (xBox == null || yBox == null) return;
		draft.x = parseInt(xBox.getValue(), 0);
		draft.y = parseInt(yBox.getValue(), 0);
	}

	private void saveRulesPage() {
		if (dependenciesBox == null) return;
		draft.dependencies = new ArrayList<>();
		Arrays.stream(dependenciesBox.getValue().split(","))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.filter(value -> !value.equals(draft.id))
				.forEach(draft.dependencies::add);
	}

	private void saveAndClose() {
		saveError = "";
		saveVisiblePage();
		draft.normalize();
		String previousId = original == null ? "" : original.id;
		QuestDefinition collision = QuestRepository.get(draft.id);
		if (collision != null && !collision.id.equals(previousId)) {
			saveError = "That quest ID is already in use.";
			return;
		}
		try {
			if (original == null) QuestRepository.save(draft);
			else QuestRepository.saveRenamed(previousId, draft);
			QuestRepository.load();
			minecraft.setScreen(parent);
		} catch (IllegalArgumentException exception) {
			saveError = exception.getMessage();
		}
	}

	private void deleteAndClose() {
		if (original != null) {
			String deletedId = original.id;
			QuestRepository.delete(deletedId);
			QuestRepository.load();
			ClientQuestProgress.removeQuest(deletedId);
			if (minecraft != null && minecraft.player != null) {
				ClientPlayNetworking.send(new QuestProgressRequestC2SPayload("quest_deleted"));
			}
		} else {
			QuestRepository.load();
		}
		minecraft.setScreen(parent);
	}

	private int parseInt(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private String idPath(String id) {
		if (id == null || id.isBlank()) return "new_quest";
		int split = id.indexOf(':');
		return split >= 0 ? id.substring(split + 1) : id;
	}

	private String normalizeLegacyNewlines(String value) {
		return value == null ? "" : value.replace("\\n", "\n").replace("\r", "");
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		EditorLayout layout = layout();
				graphics.fill(0, 0, width, height, QuestThemeManager.current().backgroundColor());
		graphics.fill(layout.left(), layout.panelTop(), layout.left() + layout.panelWidth(), layout.panelBottom(), QuestThemeManager.current().panelColor());
		switch (page) {
			case QUEST -> renderQuestLabels(graphics, layout);
			case LOOK -> renderLookLabels(graphics, layout);
			case FLOW -> renderFlowLabels(graphics, layout);
			case RULES -> renderRulesLabels(graphics, layout);
			case TASK -> renderTaskLabels(graphics, layout);
			case REWARD -> renderRewardLabels(graphics, layout);
		}
		if (!saveError.isBlank()) {
			graphics.drawCenteredString(font, Component.literal(saveError), width / 2, Math.max(24, layout.footerY() - 10), QuestThemeManager.current().errorTextColor());
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderQuestLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		int namespaceWidth = Math.min(layout.contentWidth() - 70, Math.max(76, font.width(QUEST_NAMESPACE) + 8));
		label(graphics, "Quest ID", x, row0);
		graphics.fill(x, row0 + 11, x + namespaceWidth, row0 + 29, 0xFF10161D);
		graphics.drawString(font, Component.literal(QUEST_NAMESPACE), x + 4, row0 + 16, QuestThemeManager.current().mutedTextColor(), false);
		label(graphics, "Title shown to players", x, row1);
		label(graphics, "Description / instructions — Enter = new line", x, row2);
	}

	private void renderLookLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		label(graphics, "Category", x, row0);
		label(graphics, "Quest icon", x, row1);
		label(graphics, "Node shape", x, row2);
	}

	private void renderFlowLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		label(graphics, "Canvas node position", x, row0);
		graphics.drawString(font, Component.literal("X"), x, row0 + 10, QuestThemeManager.current().mutedTextColor(), false);
		int half = Math.max(40, (layout.contentWidth() - 8) / 2);
		graphics.drawString(font, Component.literal("Y"), x + half + 8, row0 + 10, QuestThemeManager.current().mutedTextColor(), false);
	}

	private void renderRulesLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		label(graphics, "Required quests — may be from any category", x, row0);
		label(graphics, "Required-quest rule", x, row1);
		label(graphics, "Task rule", x, row2);
		label(graphics, "Visibility", x, row3);
	}

	private void renderTaskLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		graphics.drawCenteredString(font, Component.literal("Task " + (taskIndex + 1) + " / " + draft.tasks.size()), width / 2, layout.panelTop() + 9, QuestThemeManager.current().accentTextColor());
		label(graphics, "Internal task ID", x, row0);
		label(graphics, "Task text shown to players", x, row1);
		label(graphics, "Completion condition", x, row2);
		String targetLabel = currentTaskType == TaskType.CHECKMARK
				? "Manual Checkmark: no target"
				: switch (currentTaskType) {
					case OBTAIN_ITEM, USE_ITEM -> "Target item";
					case BREAK_BLOCK, INTERACT_BLOCK -> "Target block";
					case CUSTOM_EVENT -> "Registered/custom event ID";
					case CHECKMARK -> "Manual Checkmark: no target";
				};
		label(graphics, targetLabel, x, row3);
		label(graphics, currentTaskType == TaskType.CHECKMARK ? "Count fixed at 1" : "Required count", x, row4);
	}

	private void renderRewardLabels(GuiGraphics graphics, EditorLayout layout) {
		int x = layout.contentLeft();
		if (draft.rewards.isEmpty()) {
			graphics.drawCenteredString(font, Component.literal("No reward configured — rewards are optional."), width / 2,
				layout.contentTop(), QuestThemeManager.current().mutedTextColor());
			return;
		}
		graphics.drawCenteredString(font, Component.literal("Reward " + (rewardIndex + 1) + " / " + draft.rewards.size()), width / 2,
				layout.panelTop() + 9, QuestThemeManager.current().accentTextColor());
		label(graphics, "Reward item", x, row0);
		label(graphics, "Amount", x, row1);
	}

	private EditorLayout layout() {
		int margin = Math.max(3, Math.min(14, width / 28));
		int panelWidth = Math.max(180, Math.min(560, width - margin * 2));
		if (panelWidth > width - 4) panelWidth = Math.max(150, width - 4);
		int left = Math.max(2, width / 2 - panelWidth / 2);
		int panelTop = 27;
		int footerY = Math.max(0, height - 22);
		int panelBottom = Math.max(panelTop + 50, footerY - 4);
		int contentLeft = left + Math.max(7, Math.min(14, panelWidth / 24));
		int contentWidth = Math.max(100, panelWidth - (contentLeft - left) * 2);
		int contentTop = panelTop + 26;
		return new EditorLayout(left, panelWidth, panelTop, panelBottom, contentLeft, contentWidth, contentTop, footerY);
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

	private record EditorLayout(int left, int panelWidth, int panelTop, int panelBottom, int contentLeft,
			int contentWidth, int contentTop, int footerY) {
	}

	private static QuestDefinition copy(QuestDefinition source) {
		QuestDefinition copy = new QuestDefinition();
		copy.id = source.id;
		copy.chapter = source.chapter;
		copy.title = source.title;
		copy.description = source.description;
		copy.icon = source.icon;
		copy.nodeShape = source.nodeShape == null ? QuestNodeShape.SQUARE : source.nodeShape;
		copy.x = source.x;
		copy.y = source.y;
		copy.hiddenUntilDependencies = source.hiddenUntilDependencies;
		copy.dependencyMode = source.dependencyMode;
		copy.taskMode = source.taskMode;
		copy.dependencies = new ArrayList<>(source.dependencies);
		copy.tasks = new ArrayList<>();
		for (QuestTask task : source.tasks) {
			copy.tasks.add(new QuestTask(task.id, task.type, task.label, task.target, task.count, task.optional));
		}
		copy.rewards = new ArrayList<>();
		for (QuestReward reward : source.rewards) copy.rewards.add(new QuestReward(reward.item, reward.count));
		return copy;
	}
}
