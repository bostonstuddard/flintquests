package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.CompletionMode;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.data.QuestTask;
import com.projectflint.flintquests.data.TaskType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;

public final class QuestEditorScreen extends Screen {
	private static final String QUEST_NAMESPACE = "flintquests:";

	private final Screen parent;
	private final QuestDefinition original;
	private final QuestDefinition draft;

	private EditBox idPathBox;
	private EditBox titleBox;
	private EditBox descriptionBox;
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
	private String saveError = "";

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
		EditorLayout layout = layout();
		int fieldHeight = 18;
		int namespaceWidth = Math.min(layout.fieldWidth() - 60, Math.max(78, font.width(QUEST_NAMESPACE) + 10));

		String idPath = draft.id == null ? "new_quest" : draft.id.substring(draft.id.indexOf(':') + 1);
		idPathBox = field(layout.left() + 12 + namespaceWidth, layout.row(0) + 11,
			layout.fieldWidth() - namespaceWidth, fieldHeight, idPath);

		titleBox = field(layout.left() + 12, layout.row(1) + 11, layout.fieldWidth(), fieldHeight, draft.title);
		descriptionBox = field(layout.left() + 12, layout.row(2) + 11, layout.fieldWidth(), fieldHeight, draft.description);
		chapterBox = field(layout.left() + 12, layout.row(3) + 11, layout.fieldWidth() - 66, fieldHeight, draft.chapter);
		iconBox = field(layout.left() + 12, layout.row(4) + 11, layout.fieldWidth() - 66, fieldHeight, draft.icon);
		dependenciesBox = field(layout.left() + 12, layout.row(5) + 11, layout.fieldWidth() - 66, fieldHeight, String.join(",", draft.dependencies));

		addRenderableWidget(Button.builder(Component.literal("Choose"), button -> chooseCategory())
				.bounds(layout.left() + layout.panelWidth() - 66, layout.row(3) + 11, 54, fieldHeight).build());
		addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchIcon())
				.bounds(layout.left() + layout.panelWidth() - 66, layout.row(4) + 11, 54, fieldHeight).build());
		addRenderableWidget(Button.builder(Component.literal("Add"), button -> addDependency())
				.bounds(layout.left() + layout.panelWidth() - 66, layout.row(5) + 11, 54, fieldHeight).build());

		xBox = field(layout.left() + 12, layout.row(6) + 18, 64, fieldHeight, Integer.toString(draft.x));
		yBox = field(layout.left() + 86, layout.row(6) + 18, 64, fieldHeight, Integer.toString(draft.y));

		addRenderableWidget(Button.builder(Component.literal("Prerequisites: " + draft.dependencyMode), button -> {
			draft.dependencyMode = draft.dependencyMode == CompletionMode.ALL ? CompletionMode.ANY : CompletionMode.ALL;
			button.setMessage(Component.literal("Prerequisites: " + draft.dependencyMode));
		}).bounds(layout.left() + 12, layout.row(7) + 8, layout.fieldWidth(), 18).build());

		int half = (layout.fieldWidth() - 4) / 2;
		addRenderableWidget(Button.builder(Component.literal("Tasks: " + draft.taskMode), button -> {
			draft.taskMode = draft.taskMode == CompletionMode.ALL ? CompletionMode.ANY : CompletionMode.ALL;
			button.setMessage(Component.literal("Tasks: " + draft.taskMode));
		}).bounds(layout.left() + 12, layout.row(8) + 5, half, 18).build());

		addRenderableWidget(Button.builder(hiddenLabel(), button -> {
			draft.hiddenUntilDependencies = !draft.hiddenUntilDependencies;
			button.setMessage(hiddenLabel());
		}).bounds(layout.left() + 16 + half, layout.row(8) + 5, half, 18).build());

		taskIdBox = field(layout.right() + 12, layout.row(0) + 11, layout.fieldWidth(), fieldHeight, "");
		taskLabelBox = field(layout.right() + 12, layout.row(1) + 11, layout.fieldWidth(), fieldHeight, "");
		taskTypeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleTaskType())
				.bounds(layout.right() + 12, layout.row(2) + 11, layout.fieldWidth(), 18).build());
		taskTargetBox = field(layout.right() + 12, layout.row(3) + 11, layout.fieldWidth() - 66, fieldHeight, "");
		targetSearchButton = addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchTaskTarget())
				.bounds(layout.right() + layout.panelWidth() - 66, layout.row(3) + 11, 54, fieldHeight).build());
		taskCountBox = field(layout.right() + 12, layout.row(4) + 11, 64, fieldHeight, "1");
		taskOptionalButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
			currentTaskOptional = !currentTaskOptional;
			updateTaskButtonLabels();
		}).bounds(layout.right() + 86, layout.row(4) + 11, 96, 18).build());

		int taskButtonWidth = (layout.fieldWidth() - 6) / 2;
		addRenderableWidget(Button.builder(Component.literal("< Previous"), button -> previousTask())
				.bounds(layout.right() + 12, layout.row(6) + 8, taskButtonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Next >"), button -> nextTask())
				.bounds(layout.right() + 18 + taskButtonWidth, layout.row(6) + 8, taskButtonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+ Add task"), button -> addTask())
				.bounds(layout.right() + 12, layout.row(7) + 8, taskButtonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Delete task"), button -> removeTask())
				.bounds(layout.right() + 18 + taskButtonWidth, layout.row(7) + 8, taskButtonWidth, 18).build());

		int bottom = layout.bottomButtonY();
		addRenderableWidget(Button.builder(Component.literal("Save Quest"), button -> saveAndClose())
				.bounds(width / 2 - 128, bottom, 92, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
				.bounds(width / 2 - 30, bottom, 72, 20).build());
		if (original != null) {
			addRenderableWidget(Button.builder(Component.literal("Delete Quest"), button -> deleteAndClose())
					.bounds(width / 2 + 48, bottom, 92, 20).build());
		}

		loadTaskFields();
	}

	private Component hiddenLabel() {
		return Component.literal("Hide until ready: " + (draft.hiddenUntilDependencies ? "Yes" : "No"));
	}

	private EditBox field(int x, int y, int width, int height, String value) {
		EditBox box = new EditBox(font, x, y, width, height, Component.empty());
		box.setMaxLength(512);
		box.setValue(value == null ? "" : value);
		addRenderableWidget(box);
		return box;
	}

	private void chooseCategory() {
		saveQuestFieldsToDraft();
		saveTaskFields();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Quest Category"), SearchSelectScreen.Kind.CATEGORY,
				id -> draft.chapter = id));
	}

	private void searchIcon() {
		saveQuestFieldsToDraft();
		saveTaskFields();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Quest Icon"), SearchSelectScreen.Kind.ITEM,
				id -> draft.icon = id));
	}

	private void addDependency() {
		saveQuestFieldsToDraft();
		saveTaskFields();
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Add Quest Dependency"), SearchSelectScreen.Kind.QUEST,
				id -> {
					if (!id.equals(draft.id) && !draft.dependencies.contains(id)) draft.dependencies.add(id);
				}));
	}

	private void searchTaskTarget() {
		SearchSelectScreen.Kind kind = switch (currentTaskType) {
			case OBTAIN_ITEM, USE_ITEM -> SearchSelectScreen.Kind.ITEM;
			case BREAK_BLOCK, INTERACT_BLOCK -> SearchSelectScreen.Kind.BLOCK;
			default -> null;
		};
		if (kind == null) return;
		saveQuestFieldsToDraft();
		saveTaskFields();
		int selectedTask = taskIndex;
		minecraft.setScreen(new SearchSelectScreen(this, Component.literal("Choose Task Target"), kind,
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
		if (taskTypeButton != null) taskTypeButton.setMessage(Component.literal("Completion condition: " + friendlyTaskType(currentTaskType)));
		if (taskOptionalButton != null) taskOptionalButton.setMessage(Component.literal(currentTaskOptional ? "Optional" : "Required"));
		boolean searchableTarget = currentTaskType == TaskType.OBTAIN_ITEM || currentTaskType == TaskType.USE_ITEM
				|| currentTaskType == TaskType.BREAK_BLOCK || currentTaskType == TaskType.INTERACT_BLOCK;
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
			case CUSTOM_EVENT -> "Flint/Custom Event";
			case CHECKMARK -> "Manual Checkmark";
		};
	}

	private void saveTaskFields() {
		if (draft.tasks.isEmpty()) return;
		QuestTask task = draft.tasks.get(taskIndex);
		task.id = taskIdBox.getValue().trim();
		task.type = currentTaskType;
		task.label = taskLabelBox.getValue().trim();
		task.target = taskTargetBox.getValue().trim();
		task.count = parseInt(taskCountBox.getValue(), 1);
		task.optional = currentTaskOptional;
		task.normalize(taskIndex);
	}

	private void saveQuestFieldsToDraft() {
		if (idPathBox != null) {
			String path = QuestDefinition.normalizeIdPath(idPathBox.getValue());
			draft.id = QUEST_NAMESPACE + (path.isBlank() ? "new_quest" : path);
		}
		draft.title = titleBox.getValue().trim();
		draft.description = descriptionBox.getValue().trim();
		draft.chapter = chapterBox.getValue().trim();
		draft.icon = iconBox.getValue().trim();
		draft.x = parseInt(xBox.getValue(), 0);
		draft.y = parseInt(yBox.getValue(), 0);
		draft.dependencies = new ArrayList<>();
		Arrays.stream(dependenciesBox.getValue().split(","))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.filter(value -> !value.equals(draft.id))
				.forEach(draft.dependencies::add);
	}

	private void saveAndClose() {
		saveError = "";
		saveTaskFields();
		saveQuestFieldsToDraft();
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
		if (original != null) QuestRepository.delete(original.id);
		QuestRepository.load();
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
		EditorLayout layout = layout();
		int namespaceWidth = Math.min(layout.fieldWidth() - 60, Math.max(78, font.width(QUEST_NAMESPACE) + 10));

		graphics.fill(0, 0, width, height, 0xC818202A);
		graphics.fill(layout.left(), 4, layout.left() + layout.panelWidth(), layout.panelBottom(), 0xF0232D39);
		graphics.fill(layout.right(), 4, layout.right() + layout.panelWidth(), layout.panelBottom(), 0xF0232D39);

		graphics.drawString(font, Component.literal("QUEST"), layout.left() + 12, 10, 0xFFFFD46A, false);
		graphics.drawString(font, Component.literal("TASK " + (taskIndex + 1) + " / " + draft.tasks.size()), layout.right() + 12, 10, 0xFFFFD46A, false);

		label(graphics, "Quest ID", layout.left() + 12, layout.row(0));
		graphics.fill(layout.left() + 12, layout.row(0) + 11, layout.left() + 12 + namespaceWidth, layout.row(0) + 29, 0xFF10161D);
		graphics.drawString(font, Component.literal(QUEST_NAMESPACE), layout.left() + 17, layout.row(0) + 16, 0xFF8FA0B2, false);

		label(graphics, "Title shown to players", layout.left() + 12, layout.row(1));
		label(graphics, "Description / instructions", layout.left() + 12, layout.row(2));
		label(graphics, "Category", layout.left() + 12, layout.row(3));
		label(graphics, "Quest icon", layout.left() + 12, layout.row(4));
		label(graphics, "Prerequisite quests", layout.left() + 12, layout.row(5));
		label(graphics, "Canvas node position", layout.left() + 12, layout.row(6));
		graphics.drawString(font, Component.literal("X"), layout.left() + 12, layout.row(6) + 10, 0xFF8FA0B2, false);
		graphics.drawString(font, Component.literal("Y"), layout.left() + 86, layout.row(6) + 10, 0xFF8FA0B2, false);

		label(graphics, "Internal task ID", layout.right() + 12, layout.row(0));
		label(graphics, "Task text shown to players", layout.right() + 12, layout.row(1));
		label(graphics, "Completion condition", layout.right() + 12, layout.row(2));
		String targetLabel = currentTaskType == TaskType.CHECKMARK
				? "Manual Checkmark: no target needed"
				: switch (currentTaskType) {
					case OBTAIN_ITEM, USE_ITEM -> "Target item";
					case BREAK_BLOCK, INTERACT_BLOCK -> "Target block";
					case CUSTOM_EVENT -> "Custom API event ID";
					case CHECKMARK -> "Manual Checkmark: no target needed";
				};
		label(graphics, targetLabel, layout.right() + 12, layout.row(3));
		label(graphics, currentTaskType == TaskType.CHECKMARK ? "Count fixed at 1" : "Required count", layout.right() + 12, layout.row(4));

		if (!saveError.isBlank()) {
			graphics.drawCenteredString(font, Component.literal(saveError), width / 2, layout.bottomButtonY() - 11, 0xFFFF6B6B);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private EditorLayout layout() {
		int gap = 14;
		int panelWidth = Math.min(330, Math.max(180, (width - gap * 3) / 2));
		int left = width / 2 - panelWidth - gap / 2;
		int right = width / 2 + gap / 2;
		int fieldWidth = panelWidth - 24;
		int contentTop = 25;
		int bottomButtonY = Math.max(0, height - 25);
		int maxStepForHeight = Math.max(28, (bottomButtonY - 24 - contentTop) / 8);
		int rowStep = Math.min(34, maxStepForHeight);
		int panelBottom = Math.max(4, bottomButtonY - 6);
		return new EditorLayout(panelWidth, left, right, fieldWidth, contentTop, rowStep, bottomButtonY, panelBottom);
	}

	private void label(GuiGraphics graphics, String text, int x, int y) {
		graphics.drawString(font, Component.literal(text), x, y, 0xFFB7C1CC, false);
	}

	private record EditorLayout(int panelWidth, int left, int right, int fieldWidth, int contentTop,
			int rowStep, int bottomButtonY, int panelBottom) {
		int row(int index) {
			return contentTop + index * rowStep;
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private static QuestDefinition copy(QuestDefinition source) {
		QuestDefinition copy = new QuestDefinition();
		copy.id = source.id;
		copy.chapter = source.chapter;
		copy.title = source.title;
		copy.description = source.description;
		copy.icon = source.icon;
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
		return copy;
	}
}
