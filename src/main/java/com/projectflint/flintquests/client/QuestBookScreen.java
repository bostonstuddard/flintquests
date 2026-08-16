package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.CompletionMode;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestNodeShape;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.network.QuestProgressRequestC2SPayload;
import com.projectflint.flintquests.network.QuestClaimAllRewardsC2SPayload;
import com.projectflint.flintquests.theme.QuestTheme;
import com.projectflint.flintquests.theme.QuestThemeManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestBookScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 160;
	private static final int SIDEBAR_HOVER_ZONE = 30;
	private static final int SIDEBAR_GRACE = 28;
	private static final float SIDEBAR_SLIDE_SPEED = 0.05F;
	private static final int NODE_SIZE = 31;
	private static final int GRID_SIZE = 54;
	private static final int TOP_BAR_HEIGHT = 28;
	private static final int BOTTOM_BAR_HEIGHT = 24;
	private static final int CATEGORY_ROW_HEIGHT = 20;
	private static final int CATEGORY_INDENT = 10;
	private static final int PAN_MARGIN = 46;
	private static final double MIN_ZOOM = 0.55D;
	private static final double MAX_ZOOM = 1.80D;
	private static final double ZOOM_STEP = 0.10D;


	private final Screen parent;
	private final Set<String> expandedCategories = new HashSet<>();
	private final List<NodeView> visibleNodes = new ArrayList<>();
	private final Map<String, NodeView> nodesById = new HashMap<>();
	private final List<CategoryView> visibleCategoryViews = new ArrayList<>();
	private final List<SlidingWidget> sidebarWidgets = new ArrayList<>();

	private boolean uiStateLoaded;
	private String selectedCategory;
	private int panX;
	private int panY;
	private int sidebarScroll;
	private int sidebarMaxScroll;
	private float sidebarSlide;
	private double zoom = 1.0D;
	private boolean draggingCanvas;
	
	private String dependencySourceId;
	private String draggingNodeId;
	private int dragNodeOffsetX;
	private int dragNodeOffsetY;
	private String editorNotice = "";
	private long editorNoticeUntil;

	public QuestBookScreen(Screen parent) {
		super(Component.literal("Flint Quests"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ConfigManager.load();
		QuestRepository.load();
		CategoryRepository.load();
		draggingCanvas = false;
		draggingNodeId = null;
		loadUiStateOnce();
		ensureSelectedCategory();
		refreshQuestWidgets();
		if (minecraft != null && minecraft.player != null) {
			ClientPlayNetworking.send(new QuestProgressRequestC2SPayload("quest_book_open"));
		}
	}

	public void refreshFromProgress() {
		refreshQuestWidgets();
	}

	public void refreshEditingMode() {
		dependencySourceId = null;
		draggingNodeId = null;
		draggingCanvas = false;
		refreshQuestWidgets();
	}

	private void loadUiStateOnce() {
		if (uiStateLoaded) return;
		expandedCategories.clear();
		expandedCategories.addAll(ConfigManager.get().expandedQuestCategories);
		selectedCategory = ConfigManager.get().lastQuestCategory;
		sidebarSlide = 0.0F;
		zoom = clampZoom(ConfigManager.get().questBookZoom);
		uiStateLoaded = true;
	}

	private void saveSidebarState() {
		ConfigManager.get().expandedQuestCategories = new ArrayList<>(expandedCategories);
		ConfigManager.save();
	}

	private void ensureSelectedCategory() {
		List<QuestCategoryDefinition> categories = CategoryRepository.all();
		if (categories.isEmpty()) {
			selectedCategory = "introduction";
			return;
		}

		QuestCategoryDefinition selected = selectedCategory == null ? null : CategoryRepository.get(selectedCategory);
		if (selected == null || !selected.selectable) {
			selectedCategory = categories.stream()
					.filter(category -> category.selectable)
					.map(category -> category.id)
					.findFirst()
					.orElse(categories.getFirst().id);
		}
		ConfigManager.get().lastQuestCategory = selectedCategory;
		expandParents(selectedCategory);
	}

	private boolean categoryHasRewards() {
		return QuestRepository.all().stream()
				.anyMatch(quest -> normalizeCategory(quest.chapter).equals(selectedCategory)
						&& quest.rewards != null && !quest.rewards.isEmpty());
	}

	private boolean categoryHasClaimableRewards() {
		return QuestRepository.all().stream()
				.anyMatch(quest -> normalizeCategory(quest.chapter).equals(selectedCategory)
						&& quest.rewards != null && !quest.rewards.isEmpty()
						&& ClientQuestProgress.questComplete(quest.id)
						&& !ClientQuestProgress.rewardClaimed(quest.id));
	}

	private boolean editingEnabled() {
		return ConfigManager.devToolsEnabled();
	}

	private void expandParents(String categoryId) {
		Set<String> seen = new HashSet<>();
		QuestCategoryDefinition category = CategoryRepository.get(categoryId);
		while (category != null && !category.parent.isBlank() && seen.add(category.id)) {
			expandedCategories.add(category.parent);
			category = CategoryRepository.get(category.parent);
		}
	}

	private String normalizeCategory(String category) {
		return category == null || category.isBlank() ? "introduction" : category.trim();
	}

	private List<QuestDefinition> selectedQuests() {
		return QuestRepository.all().stream()
				.filter(quest -> normalizeCategory(quest.chapter).equals(selectedCategory))
				.filter(quest -> !quest.hiddenUntilDependencies || isQuestUnlocked(quest))
				.sorted(Comparator.comparingInt((QuestDefinition quest) -> quest.y)
						.thenComparingInt(quest -> quest.x)
						.thenComparing(quest -> quest.title))
				.toList();
	}

	private boolean isQuestUnlocked(QuestDefinition quest) {
		if (quest.dependencies.isEmpty()) return true;
		if (quest.dependencyMode == CompletionMode.ANY) {
			return quest.dependencies.stream().anyMatch(ClientQuestProgress::questComplete);
		}
		return quest.dependencies.stream().allMatch(ClientQuestProgress::questComplete);
	}

	private void refreshQuestWidgets() {
		clearWidgets();
		visibleNodes.clear();
		nodesById.clear();
		visibleCategoryViews.clear();
		sidebarWidgets.clear();
		ensureSelectedCategory();

		List<QuestDefinition> quests = selectedQuests();
		if (draggingNodeId == null) clampPan(quests);

		buildSidebar();
		rebuildNodeViews();

		if (categoryHasRewards()) {
			Button claimAll = Button.builder(Component.literal("Claim All"), button -> {
				if (minecraft != null && minecraft.player != null) {
					ClientPlayNetworking.send(new QuestClaimAllRewardsC2SPayload(selectedCategory));
					button.active = false;
				}
			}).bounds(width / 2 - 42, height - 21, 84, 18).build();
			claimAll.active = categoryHasClaimableRewards();
			addRenderableWidget(claimAll);
		}

		addRenderableWidget(Button.builder(Component.literal("⚙"), button ->
				minecraft.setScreen(new FlintQuestConfigScreen(this)))
				.bounds(width - 24, 5, 20, 18).build());

		int bottomY = height - 21;
		if (editingEnabled()) {
			addRenderableWidget(Button.builder(Component.literal("+"), button ->
					minecraft.setScreen(new QuestEditorScreen(this, null, selectedCategory))
			).bounds(width - 48, bottomY, 20, 18).build());
		}
		addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
				.bounds(width - 24, bottomY, 20, 18).build());
		positionSidebarWidgets();
	}

	private void rebuildNodeViews() {
		visibleNodes.clear();
		nodesById.clear();
		int canvasLeft = 3;
		int canvasRight = width - 3;
		int canvasTop = TOP_BAR_HEIGHT + 3;
		int canvasBottom = height - BOTTOM_BAR_HEIGHT - 3;
		int centerX = canvasLeft + Math.max(0, canvasRight - canvasLeft) / 2 + panX;
		int centerY = canvasTop + Math.max(0, canvasBottom - canvasTop) / 2 + panY;
		int nodeSize = scaledNodeSize();
		int gridSize = scaledGridSize();
		for (QuestDefinition quest : selectedQuests()) {
			int nodeX = centerX + quest.x * gridSize - nodeSize / 2;
			int nodeY = centerY + quest.y * gridSize - nodeSize / 2;
			NodeView view = new NodeView(quest, nodeX, nodeY, nodeSize);
			nodesById.put(quest.id, view);
			if (nodeX + nodeSize < canvasLeft || nodeX > canvasRight || nodeY + nodeSize < canvasTop || nodeY > canvasBottom) continue;
			visibleNodes.add(view);
		}
	}

	private void buildSidebar() {
		int availableTop = TOP_BAR_HEIGHT + 3;
		int availableBottom = height - BOTTOM_BAR_HEIGHT - 3;
		int availableHeight = Math.max(0, availableBottom - availableTop);

		List<QuestCategoryDefinition> roots = CategoryRepository.roots();
		if (roots.isEmpty()) roots = CategoryRepository.all();

		int totalRows = countVisibleRows(roots, new HashSet<>());
		int contentHeight = totalRows * CATEGORY_ROW_HEIGHT;
		sidebarMaxScroll = Math.max(0, contentHeight - availableHeight);
		sidebarScroll = clamp(sidebarScroll, 0, sidebarMaxScroll);

		int[] rowIndex = {0};
		Set<String> visited = new HashSet<>();
		for (QuestCategoryDefinition root : roots) {
			addCategoryTree(root, 0, rowIndex, visited, availableTop, availableBottom);
		}

		if (editingEnabled()) {
			int bottomY = height - 21;
			Button addCategory = Button.builder(Component.literal("+C"), button ->
					minecraft.setScreen(new CategoryEditorScreen(this, null)))
					.bounds(3, bottomY, 30, 18).build();
			addSidebarWidget(addCategory, 3);
			QuestCategoryDefinition selected = CategoryRepository.get(selectedCategory);
			if (selected != null) {
				Button editCategory = Button.builder(Component.literal("Edit"), button ->
						minecraft.setScreen(new CategoryEditorScreen(this, selected)))
						.bounds(36, bottomY, 38, 18).build();
				addSidebarWidget(editCategory, 36);
			}
		}
	}

	private <T extends AbstractWidget> T addSidebarWidget(T widget, int baseX) {
		addRenderableWidget(widget);
		sidebarWidgets.add(new SlidingWidget(widget, baseX));
		return widget;
	}

	private int countVisibleRows(List<QuestCategoryDefinition> roots, Set<String> visited) {
		int count = 0;
		for (QuestCategoryDefinition category : roots) {
			if (!visited.add(category.id)) continue;
			count++;
			if (expandedCategories.contains(category.id)) {
				count += countVisibleRows(CategoryRepository.childrenOf(category.id), visited);
			}
		}
		return count;
	}

	private void addCategoryTree(QuestCategoryDefinition category, int depth, int[] rowIndex, Set<String> visited,
			int availableTop, int availableBottom) {
		if (!visited.add(category.id)) return;

		int rawY = availableTop + rowIndex[0] * CATEGORY_ROW_HEIGHT - sidebarScroll;
		rowIndex[0]++;
		List<QuestCategoryDefinition> children = CategoryRepository.childrenOf(category.id);
		boolean hasChildren = !children.isEmpty();
		boolean expanded = expandedCategories.contains(category.id);

		if (rawY + CATEGORY_ROW_HEIGHT > availableTop && rawY < availableBottom) {
			int indent = Math.min(depth, 6) * CATEGORY_INDENT;
			int rowX = 3 + indent;

			if (!category.selectable) {
				if (hasChildren) {
					Button expand = Button.builder(Component.literal(expanded ? "v" : ">"), button -> {
						if (expandedCategories.contains(category.id)) expandedCategories.remove(category.id);
						else expandedCategories.add(category.id);
						saveSidebarState();
						refreshQuestWidgets();
					}).bounds(rowX, rawY + 1, 12, 16).build();
					addSidebarWidget(expand, rowX);
				}

				int groupX = rowX + (hasChildren ? 13 : 2);
				int groupWidth = Math.max(28, SIDEBAR_WIDTH - groupX - 3);
				Button groupLabel = Button.builder(LegacyText.parse("   " + category.title), button -> {
					// Group headers are labels only; the dedicated arrow controls expansion.
				}).bounds(groupX, rawY + 1, groupWidth, 16).build();
				addSidebarWidget(groupLabel, groupX);
				visibleCategoryViews.add(new CategoryView(category, groupX, rawY + 1, groupWidth, 16,
						groupX + 2, rawY + 1, false));
			} else {
				if (hasChildren) {
					Button expand = Button.builder(Component.literal(expanded ? "v" : ">"), button -> {
						if (expandedCategories.contains(category.id)) expandedCategories.remove(category.id);
						else expandedCategories.add(category.id);
						saveSidebarState();
						refreshQuestWidgets();
					}).bounds(rowX, rawY + 1, 12, 16).build();
					addSidebarWidget(expand, rowX);
				}

				int categoryX = rowX + (hasChildren ? 13 : 2);
				int categoryWidth = Math.max(28, SIDEBAR_WIDTH - categoryX - 3);
				String label = "   " + category.title;
				Button categoryButton = Button.builder(LegacyText.parse(label), button -> selectCategory(category.id))
						.bounds(categoryX, rawY + 1, categoryWidth, 16).build();
				addSidebarWidget(categoryButton, categoryX);
				visibleCategoryViews.add(new CategoryView(category, categoryX, rawY + 1, categoryWidth, 16,
						categoryX + 2, rawY + 1, category.id.equals(selectedCategory)));
			}
		}

		if (hasChildren && expanded) {
			for (QuestCategoryDefinition child : children) {
				addCategoryTree(child, depth + 1, rowIndex, visited, availableTop, availableBottom);
			}
		}
	}

	private void selectCategory(String categoryId) {
		QuestCategoryDefinition category = CategoryRepository.get(categoryId);
		if (category == null || !category.selectable) return;
		selectedCategory = categoryId;
		ConfigManager.get().lastQuestCategory = categoryId;
		dependencySourceId = null;
		draggingNodeId = null;
		panX = 0;
		panY = 0;
		expandParents(categoryId);
		saveSidebarState();
		refreshQuestWidgets();
	}

	private void openQuestForPrimaryClick(QuestDefinition quest) {
		if (editingEnabled()) {
			minecraft.setScreen(new QuestEditorScreen(this, quest, normalizeCategory(quest.chapter)));
		} else {
			minecraft.setScreen(new QuestDetailScreen(this, quest));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int mouseX = (int) Math.round(click.x());
		int mouseY = (int) Math.round(click.y());
		boolean editing = editingEnabled();
		int sidebarWidth = currentSidebarWidth();

		if (sidebarWidth > 0 && mouseX < sidebarWidth + 2) {
			if (editing && click.button() == 1) {
				for (CategoryView category : visibleCategoryViews) {
					int categoryX = category.baseX() + sidebarOffset();
					if (inside(mouseX, mouseY, categoryX, category.y(), category.width(), category.height())) {
						minecraft.setScreen(new CategoryEditorScreen(this, category.category()));
						return true;
					}
				}
			}
			if (super.mouseClicked(click, doubled)) return true;
			return true;
		}

		NodeView node = findNode(mouseX, mouseY);
		if (editing && click.button() == 0 && (FlintQuestsClient.linkNodesKeyDown() || click.hasShiftDown()) && node != null) {
			handleDependencyClick(node.quest());
			return true;
		}

		if (editing && click.button() == 0 && (FlintQuestsClient.moveNodesKeyDown() || click.hasAltDown()) && node != null) {
			draggingNodeId = node.quest().id;
			dragNodeOffsetX = mouseX - node.centerX();
			dragNodeOffsetY = mouseY - node.centerY();
			setEditorNotice("Moving " + node.quest().title + " — release to place");
			return true;
		}

		if (editing && click.button() == 1 && node != null) {
			minecraft.setScreen(new QuestDetailScreen(this, node.quest()));
			return true;
		}

		if (click.button() == 0 && node != null) {
			openQuestForPrimaryClick(node.quest());
			return true;
		}

		if ((click.button() == 0 || click.button() == 2)
				&& click.y() > TOP_BAR_HEIGHT
				&& click.y() < height - BOTTOM_BAR_HEIGHT) {
			draggingCanvas = true;
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingNodeId != null && event.button() == 0) {
			QuestDefinition quest = QuestRepository.get(draggingNodeId);
			if (quest == null) {
				draggingNodeId = null;
				return true;
			}

			int canvasLeft = 3;
			int canvasRight = width - 3;
			int canvasTop = TOP_BAR_HEIGHT + 3;
			int canvasBottom = height - BOTTOM_BAR_HEIGHT - 3;
			int centerX = canvasLeft + Math.max(0, canvasRight - canvasLeft) / 2 + panX;
			int centerY = canvasTop + Math.max(0, canvasBottom - canvasTop) / 2 + panY;
			double desiredCenterX = event.x() - dragNodeOffsetX;
			double desiredCenterY = event.y() - dragNodeOffsetY;
			int gridSize = scaledGridSize();
			int gridX = (int) Math.round((desiredCenterX - centerX) / gridSize);
			int gridY = (int) Math.round((desiredCenterY - centerY) / gridSize);
			if (quest.x != gridX || quest.y != gridY) {
				quest.x = gridX;
				quest.y = gridY;
				rebuildNodeViews();
			}
			return true;
		}

		if (draggingCanvas && (event.button() == 0 || event.button() == 2)) {
			panX += (int) Math.round(dragX);
			panY += (int) Math.round(dragY);
			clampPan(selectedQuests());
			refreshQuestWidgets();
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingNodeId != null && event.button() == 0) {
			QuestDefinition quest = QuestRepository.get(draggingNodeId);
			if (quest != null) {
				QuestRepository.save(quest);
				setEditorNotice("Moved " + quest.title + " to " + quest.x + ", " + quest.y);
			}
			draggingNodeId = null;
			QuestRepository.load();
			refreshQuestWidgets();
			return true;
		}
		draggingCanvas = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int sidebarWidth = currentSidebarWidth();
		boolean overCanvas = mouseY > TOP_BAR_HEIGHT && mouseY < height - BOTTOM_BAR_HEIGHT
				&& !(sidebarWidth > 0 && mouseX < sidebarWidth + 2);
		if (overCanvas && controlKeyDown() && vertical != 0.0D) {
			zoomAt(mouseX, mouseY, vertical > 0.0D ? ZOOM_STEP : -ZOOM_STEP);
			return true;
		}
		if (sidebarWidth > 0 && mouseX < sidebarWidth + 2 && mouseY > TOP_BAR_HEIGHT && mouseY < height - BOTTOM_BAR_HEIGHT) {
			sidebarScroll -= (int) Math.round(vertical * CATEGORY_ROW_HEIGHT);
			sidebarScroll = clamp(sidebarScroll, 0, sidebarMaxScroll);
			refreshQuestWidgets();
			return true;
		}
		if (mouseY > TOP_BAR_HEIGHT && mouseY < height - BOTTOM_BAR_HEIGHT) {
			panY += (int) Math.round(vertical * 24.0D);
			panX += (int) Math.round(horizontal * 24.0D);
			clampPan(selectedQuests());
			rebuildNodeViews();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	private boolean controlKeyDown() {
		if (minecraft == null) return false;
		return InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_LCONTROL)
				|| InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_RCONTROL);
	}

	private void zoomAt(double mouseX, double mouseY, double amount) {
		double oldZoom = zoom;
		double newZoom = clampZoom(Math.round((zoom + amount) * 100.0D) / 100.0D);
		if (Math.abs(newZoom - oldZoom) < 0.0001D) return;

		int canvasLeft = 3;
		int canvasRight = width - 3;
		int canvasTop = TOP_BAR_HEIGHT + 3;
		int canvasBottom = height - BOTTOM_BAR_HEIGHT - 3;
		double baseCenterX = canvasLeft + Math.max(0, canvasRight - canvasLeft) / 2.0D;
		double baseCenterY = canvasTop + Math.max(0, canvasBottom - canvasTop) / 2.0D;
		double worldOffsetX = mouseX - baseCenterX - panX;
		double worldOffsetY = mouseY - baseCenterY - panY;
		double ratio = newZoom / oldZoom;
		zoom = newZoom;
		panX = (int) Math.round(mouseX - baseCenterX - worldOffsetX * ratio);
		panY = (int) Math.round(mouseY - baseCenterY - worldOffsetY * ratio);
		clampPan(selectedQuests());
		rebuildNodeViews();

		ConfigManager.get().questBookZoom = zoom;
		ConfigManager.save();
	}

	private double clampZoom(double value) {
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
	}

	private int scaledGridSize() {
		return Math.max(28, (int) Math.round(GRID_SIZE * zoom));
	}

	private int scaledNodeSize() {
		int size = Math.max(17, (int) Math.round(NODE_SIZE * zoom));
		return (size & 1) == 0 ? size + 1 : size;
	}

	private void handleDependencyClick(QuestDefinition clicked) {
		if (dependencySourceId == null) {
			dependencySourceId = clicked.id;
			setEditorNotice("Link start: " + clicked.title + " — Shift-click the quest that should come after it");
			return;
		}

		String sourceId = dependencySourceId;
		dependencySourceId = null;
		if (sourceId.equals(clicked.id)) {
			setEditorNotice("Link selection cancelled");
			return;
		}

		QuestDefinition source = QuestRepository.get(sourceId);
		QuestDefinition target = QuestRepository.get(clicked.id);
		if (source == null || target == null) return;
		if (target.dependencies.contains(sourceId)) {
			setEditorNotice(source.title + " → " + target.title + " is already linked");
			return;
		}
		if (dependsOn(sourceId, target.id, new HashSet<>())) {
			setEditorNotice("Link blocked: that would create a dependency cycle");
			return;
		}

		target.dependencies = new ArrayList<>(target.dependencies);
		target.dependencies.add(sourceId);
		target.normalize();
		QuestRepository.save(target);
		QuestRepository.load();
		setEditorNotice("Linked " + source.title + " → " + target.title);
		refreshQuestWidgets();
	}

	private boolean dependsOn(String questId, String possibleDependencyId, Set<String> visited) {
		if (!visited.add(questId)) return false;
		QuestDefinition quest = QuestRepository.get(questId);
		if (quest == null) return false;
		if (quest.dependencies.contains(possibleDependencyId)) return true;
		for (String dependency : quest.dependencies) {
			if (dependsOn(dependency, possibleDependencyId, visited)) return true;
		}
		return false;
	}

	private NodeView findNode(int mouseX, int mouseY) {
		for (NodeView node : visibleNodes) {
			if (inside(mouseX, mouseY, node.x(), node.y(), node.size(), node.size())) return node;
		}
		return null;
	}

	private void setEditorNotice(String notice) {
		editorNotice = notice == null ? "" : notice;
		editorNoticeUntil = System.currentTimeMillis() + 3000L;
	}

	private void clampPan(List<QuestDefinition> quests) {
		if (quests.isEmpty()) {
			panX = 0;
			panY = 0;
			return;
		}

		int canvasLeft = 3;
		int canvasRight = width - 3;
		int canvasTop = TOP_BAR_HEIGHT + 3;
		int canvasBottom = height - BOTTOM_BAR_HEIGHT - 3;
		int baseCenterX = canvasLeft + Math.max(0, canvasRight - canvasLeft) / 2;
		int baseCenterY = canvasTop + Math.max(0, canvasBottom - canvasTop) / 2;
		int nodeSize = scaledNodeSize();
		int gridSize = scaledGridSize();

		int minRelX = Integer.MAX_VALUE;
		int maxRelX = Integer.MIN_VALUE;
		int minRelY = Integer.MAX_VALUE;
		int maxRelY = Integer.MIN_VALUE;
		for (QuestDefinition quest : quests) {
			int relX = quest.x * gridSize;
			int relY = quest.y * gridSize;
			minRelX = Math.min(minRelX, relX - nodeSize / 2);
			maxRelX = Math.max(maxRelX, relX + nodeSize / 2);
			minRelY = Math.min(minRelY, relY - nodeSize / 2);
			maxRelY = Math.max(maxRelY, relY + nodeSize / 2);
		}

		int xMargin = Math.min(PAN_MARGIN, Math.max(12, (canvasRight - canvasLeft) / 4));
		int yMargin = Math.min(PAN_MARGIN, Math.max(12, (canvasBottom - canvasTop) / 4));
		int minPanX = canvasLeft + xMargin - baseCenterX - maxRelX;
		int maxPanX = canvasRight - xMargin - baseCenterX - minRelX;
		int minPanY = canvasTop + yMargin - baseCenterY - maxRelY;
		int maxPanY = canvasBottom - yMargin - baseCenterY - minRelY;

		panX = clamp(panX, Math.min(minPanX, maxPanX), Math.max(minPanX, maxPanX));
		panY = clamp(panY, Math.min(minPanY, maxPanY), Math.max(minPanY, maxPanY));
	}

	private void updateSidebarAnimation(int mouseX, int mouseY) {
		boolean nearEdge = mouseX >= 0 && mouseX <= SIDEBAR_HOVER_ZONE;
		boolean insideOpenPanel = sidebarSlide > 0.01F
				&& mouseX >= 0 && mouseX <= SIDEBAR_WIDTH + SIDEBAR_GRACE
				&& mouseY >= 0 && mouseY < height;
		float target = nearEdge || insideOpenPanel ? 1.0F : 0.0F;
		float delta = target - sidebarSlide;
		if (Math.abs(delta) < 0.01F) sidebarSlide = target;
		else sidebarSlide += delta * SIDEBAR_SLIDE_SPEED;
		sidebarSlide = Math.max(0.0F, Math.min(1.0F, sidebarSlide));
		positionSidebarWidgets();
	}

	private void positionSidebarWidgets() {
		int offset = sidebarOffset();
		boolean show = sidebarSlide > 0.03F;
		for (SlidingWidget sliding : sidebarWidgets) {
			sliding.widget().setX(sliding.baseX() + offset);
			sliding.widget().visible = show;
		}
	}

	private int sidebarOffset() {
		return Math.round((sidebarSlide - 1.0F) * SIDEBAR_WIDTH);
	}

	private int currentSidebarWidth() {
		return Math.max(0, Math.round(sidebarSlide * SIDEBAR_WIDTH));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		updateSidebarAnimation(mouseX, mouseY);
		int sidebarWidth = currentSidebarWidth();
		int canvasLeft = 3;
		int canvasTop = TOP_BAR_HEIGHT;
		int canvasBottom = height - BOTTOM_BAR_HEIGHT;

		QuestTheme theme = QuestThemeManager.current();
		graphics.fill(0, 0, width, height, theme.backgroundColor());
		graphics.fill(0, 0, width, TOP_BAR_HEIGHT, theme.topBarColor());
		graphics.fill(0, canvasTop, width, canvasBottom, theme.canvasColor());
		graphics.fill(0, canvasBottom, width, height, theme.bottomBarColor());
		graphics.enableScissor(0, canvasTop, width, canvasBottom);
		drawDependencyLines(graphics);
		graphics.disableScissor();

		QuestCategoryDefinition selected = CategoryRepository.get(selectedCategory);
		String selectedTitle = selected == null ? selectedCategory : selected.title;
		graphics.drawCenteredString(font, LegacyText.parse(selectedTitle), width / 2, 9, theme.titleTextColor());

		for (NodeView node : visibleNodes) {
			boolean completed = ClientQuestProgress.questComplete(node.quest().id);
			boolean unlocked = isQuestUnlocked(node.quest());
			int stateColor = completed ? theme.completedColor() : unlocked ? theme.unlockedColor() : theme.lockedColor();
			drawNodeState(graphics, node, stateColor, completed || unlocked);


			ItemStack stack = QuestIconHelper.stackFor(node.quest());
			renderScaledNodeItem(graphics, stack, node.centerX(), node.centerY());

			if (node.quest().id.equals(dependencySourceId)) {
				QuestNodeShape sourceShape = node.quest().nodeShape == null ? QuestNodeShape.SQUARE : node.quest().nodeShape;
				int sourceMargin = Math.max(3, (int) Math.round(5 * zoom));
				drawShapeBorder(graphics, sourceShape, node.x() - sourceMargin, node.y() - sourceMargin, node.size() + sourceMargin * 2, theme.linkSourceColor(), 2);
			}

			if (inside(mouseX, mouseY, node.x(), node.y(), node.size(), node.size())
					&& !(sidebarWidth > 0 && mouseX < sidebarWidth + 2)) {
				graphics.setTooltipForNextFrame(font, buildQuestTooltip(node.quest()), mouseX, mouseY);
			}
		}

		if (sidebarSlide < 0.30F) {
			int handleHeight = Math.min(96, Math.max(56, canvasBottom - TOP_BAR_HEIGHT - 36));
			int handleTop = TOP_BAR_HEIGHT + Math.max(10, (canvasBottom - TOP_BAR_HEIGHT - handleHeight) / 2);
			graphics.fill(0, handleTop, 10, handleTop + handleHeight, theme.sidebarHandleColor());
			graphics.fill(2, handleTop + 3, 8, handleTop + handleHeight - 3, theme.sidebarHandleInnerColor());
		}

		if (sidebarWidth > 0) {
			graphics.fill(0, TOP_BAR_HEIGHT, sidebarWidth, canvasBottom, theme.sidebarColor());
			graphics.fill(sidebarWidth, TOP_BAR_HEIGHT, sidebarWidth + 1, canvasBottom, theme.sidebarDividerColor());
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		if (sidebarSlide > 0.03F) {
			int offset = sidebarOffset();
			for (CategoryView category : visibleCategoryViews) {
				int categoryX = category.baseX() + offset;
				int iconX = category.baseIconX() + offset;
				if (category.selected()) {
					drawBorder(graphics, categoryX - 1, category.y() - 1,
							category.width() + 2, category.height() + 2, theme.unlockedColor(), 1);
				}
				ItemStack stack = QuestIconHelper.stackFor(category.category().icon);
				graphics.renderItem(stack, iconX, category.iconY());
				if (inside(mouseX, mouseY, categoryX, category.y(), category.width(), category.height())) {
					graphics.setTooltipForNextFrame(font, buildCategoryTooltip(category.category()), mouseX, mouseY);
				}
			}
		}

		if (editingEnabled() && !editorNotice.isBlank() && System.currentTimeMillis() < editorNoticeUntil) {
			graphics.drawString(font, Component.literal(editorNotice), 6, height - 16, theme.labelTextColor(), false);
		}
	}

	private void renderScaledNodeItem(GuiGraphics graphics, ItemStack stack, int centerX, int centerY) {
		float scale = (float) zoom;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY);
		graphics.pose().scale(scale, scale);
		graphics.renderItem(stack, -8, -8);
		graphics.pose().popMatrix();
	}

	private List<FormattedCharSequence> buildQuestTooltip(QuestDefinition quest) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		appendTooltipText(lines, quest.title, 260);

		String lore = quest.description == null ? "" : quest.description.replace("\\n", "\n").replace("\r", "");
		if (!lore.isBlank()) {
			lines.add(Component.literal(" ").getVisualOrderText());
			appendTooltipText(lines, lore, 260);
		}

		if (editingEnabled()) {
			lines.add(Component.literal(" ").getVisualOrderText());
			lines.add(Component.literal("Left-click: edit").getVisualOrderText());
			lines.add(Component.literal("Right-click: view").getVisualOrderText());
			lines.add(Component.literal(FlintQuestsClient.linkNodesKeyName().getString() + " + click: link").getVisualOrderText());
			lines.add(Component.literal(FlintQuestsClient.moveNodesKeyName().getString() + " + drag: move").getVisualOrderText());
		}
		return lines;
	}

	private List<FormattedCharSequence> buildCategoryTooltip(QuestCategoryDefinition category) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		appendTooltipText(lines, category.title, 220);
		if (editingEnabled()) {
			lines.add(Component.literal(" ").getVisualOrderText());
			if (category.selectable) lines.add(Component.literal("Left-click: open category").getVisualOrderText());
			else lines.add(Component.literal("Dropdown arrow: expand / collapse").getVisualOrderText());
			lines.add(Component.literal("Right-click: edit category").getVisualOrderText());
		}
		return lines;
	}

	private void appendTooltipText(List<FormattedCharSequence> lines, String raw, int maxWidth) {
		for (Component component : LegacyText.lines(raw)) {
			if (component.getString().isBlank()) {
				lines.add(Component.literal(" ").getVisualOrderText());
				continue;
			}
			lines.addAll(font.split(component, maxWidth));
		}
	}

	private void drawNodeState(GuiGraphics graphics, NodeView node, int color, boolean glow) {
		QuestNodeShape shape = node.quest().nodeShape == null ? QuestNodeShape.SQUARE : node.quest().nodeShape;
		int size = node.size();
		drawShapeFill(graphics, shape, node.x(), node.y(), size, QuestThemeManager.current().nodeBodyColor());
		if (glow) {
			int glowColor = (0x44 << 24) | (color & 0x00FFFFFF);
			int glowMargin = Math.max(2, (int) Math.round(4 * zoom));
			drawShapeBorder(graphics, shape, node.x() - glowMargin, node.y() - glowMargin, size + glowMargin * 2, glowColor, 2);
		}
		int borderMargin = Math.max(1, (int) Math.round(2 * zoom));
		drawShapeBorder(graphics, shape, node.x() - borderMargin, node.y() - borderMargin, size + borderMargin * 2, color, 2);
	}

	private void drawShapeFill(GuiGraphics graphics, QuestNodeShape shape, int x, int y, int size, int color) {
		switch (shape) {
			case SQUARE -> graphics.fill(x, y, x + size, y + size, color);
			case DIAMOND -> {
				double center = (size - 1) / 2.0D;
				for (int row = 0; row < size; row++) {
					double halfWidth = center - Math.abs(row - center);
					int left = (int) Math.ceil(center - halfWidth);
					int right = (int) Math.floor(center + halfWidth);
					graphics.fill(x + left, y + row, x + right + 1, y + row + 1, color);
				}
			}
			case HEXAGON -> {
				int inset = Math.max(4, size / 5);
				int shoulder = Math.max(1, size / 4);
				for (int row = 0; row < size; row++) {
					int edgeDistance = Math.min(row, size - 1 - row);
					int rowInset = edgeDistance >= shoulder ? 0 : inset - (inset * edgeDistance / shoulder);
					graphics.fill(x + rowInset, y + row, x + size - rowInset, y + row + 1, color);
				}
			}
			case CIRCLE -> {
				double radius = (size - 1) / 2.0D;
				double center = radius;
				for (int row = 0; row < size; row++) {
					double dy = row - center;
					double halfWidth = Math.sqrt(Math.max(0.0D, radius * radius - dy * dy));
					int left = (int) Math.ceil(center - halfWidth);
					int right = (int) Math.floor(center + halfWidth);
					graphics.fill(x + left, y + row, x + right + 1, y + row + 1, color);
				}
			}
		}
	}

	private void drawShapeBorder(GuiGraphics graphics, QuestNodeShape shape, int x, int y, int size, int color, int thickness) {
		switch (shape) {
			case SQUARE -> drawBorder(graphics, x, y, size, size, color, thickness);
			case DIAMOND -> drawPolygonBorder(graphics,
					new int[]{x + size / 2, x + size - 1, x + size / 2, x},
					new int[]{y, y + size / 2, y + size - 1, y + size / 2}, color, thickness);
			case HEXAGON -> {
				int inset = Math.max(4, size / 5);
				drawPolygonBorder(graphics,
						new int[]{x + inset, x + size - inset - 1, x + size - 1, x + size - inset - 1, x + inset, x},
						new int[]{y, y, y + size / 2, y + size - 1, y + size - 1, y + size / 2}, color, thickness);
			}
			case CIRCLE -> drawCircleBorder(graphics, x, y, size, color, thickness);
		}
	}

	private void drawCircleBorder(GuiGraphics graphics, int x, int y, int size, int color, int thickness) {
		int points = 32;
		int[] xs = new int[points];
		int[] ys = new int[points];
		double radius = (size - 1) / 2.0D;
		double centerX = x + radius;
		double centerY = y + radius;
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0D * i / points - Math.PI / 2.0D;
			xs[i] = (int) Math.round(centerX + Math.cos(angle) * radius);
			ys[i] = (int) Math.round(centerY + Math.sin(angle) * radius);
		}
		drawPolygonBorder(graphics, xs, ys, color, thickness);
	}

	private void drawPolygonBorder(GuiGraphics graphics, int[] xs, int[] ys, int color, int thickness) {
		if (xs.length < 2 || xs.length != ys.length) return;
		for (int i = 0; i < xs.length; i++) {
			int next = (i + 1) % xs.length;
			drawPixelLine(graphics, xs[i], ys[i], xs[next], ys[next], color, thickness);
		}
	}

	private void drawPixelLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color, int thickness) {
		int dx = Math.abs(x1 - x0);
		int sx = x0 < x1 ? 1 : -1;
		int dy = -Math.abs(y1 - y0);
		int sy = y0 < y1 ? 1 : -1;
		int error = dx + dy;
		int half = Math.max(0, thickness - 1);
		while (true) {
			graphics.fill(x0 - half, y0 - half, x0 + half + 1, y0 + half + 1, color);
			if (x0 == x1 && y0 == y1) break;
			int twice = 2 * error;
			if (twice >= dy) { error += dy; x0 += sx; }
			if (twice <= dx) { error += dx; y0 += sy; }
		}
	}

	private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color, int thickness) {
		for (int i = 0; i < thickness; i++) {
			graphics.fill(x + i, y + i, x + width - i, y + i + 1, color);
			graphics.fill(x + i, y + height - i - 1, x + width - i, y + height - i, color);
			graphics.fill(x + i, y + i, x + i + 1, y + height - i, color);
			graphics.fill(x + width - i - 1, y + i, x + width - i, y + height - i, color);
		}
	}

	private void drawCanvasGrid(GuiGraphics graphics, int left, int top, int right, int bottom) {
	}

	private void drawDependencyLines(GuiGraphics graphics) {
		Map<String, List<NodeView>> dependentsByParent = new HashMap<>();
		for (NodeView child : nodesById.values()) {
			for (String dependencyId : child.quest().dependencies) {
				if (!nodesById.containsKey(dependencyId)) continue;
				dependentsByParent.computeIfAbsent(dependencyId, ignored -> new ArrayList<>()).add(child);
			}
		}

		for (Map.Entry<String, List<NodeView>> entry : dependentsByParent.entrySet()) {
			NodeView parentNode = nodesById.get(entry.getKey());
			if (parentNode == null) continue;
			QuestTheme theme = QuestThemeManager.current();
			int color = ClientQuestProgress.questComplete(parentNode.quest().id) ? theme.completedColor() : theme.lockedColor();

			List<NodeView> below = entry.getValue().stream()
					.filter(child -> child.centerY() > parentNode.centerY() + parentNode.size() / 2)
					.toList();
			List<NodeView> above = entry.getValue().stream()
					.filter(child -> child.centerY() < parentNode.centerY() - parentNode.size() / 2)
					.toList();
			List<NodeView> side = entry.getValue().stream()
					.filter(child -> !below.contains(child) && !above.contains(child))
					.toList();

			drawVerticalBranchGroup(graphics, parentNode, below, true, color);
			drawVerticalBranchGroup(graphics, parentNode, above, false, color);
			for (NodeView child : side) drawSideBranch(graphics, parentNode, child, color);
		}
	}

	private void drawVerticalBranchGroup(GuiGraphics graphics, NodeView parent, List<NodeView> children, boolean below, int color) {
		if (children.isEmpty()) return;
		int trunkX = parent.centerX();
		int startY = below ? parent.y() + parent.size() + 2 : parent.y() - 2;
		int branchOffset = Math.max(8, (int) Math.round(18 * zoom));
		int busY = below ? parent.y() + parent.size() + branchOffset : parent.y() - branchOffset;

		int minX = trunkX;
		int maxX = trunkX;
		for (NodeView child : children) {
			minX = Math.min(minX, child.centerX());
			maxX = Math.max(maxX, child.centerX());
		}

		drawVerticalLine(graphics, trunkX, startY, busY, color);
		drawHorizontalLine(graphics, minX, maxX, busY, color);

		for (NodeView child : children) {
			int targetY = below ? child.y() - 2 : child.y() + child.size() + 2;
			drawVerticalLine(graphics, child.centerX(), busY, targetY, color);
		}
	}

	private void drawSideBranch(GuiGraphics graphics, NodeView parent, NodeView child, int color) {
		boolean right = child.centerX() >= parent.centerX();
		int startX = right ? parent.x() + parent.size() + 3 : parent.x() - 3;
		int endX = right ? child.x() - 3 : child.x() + child.size() + 3;
		int midX = startX + (endX - startX) / 2;
		drawHorizontalLine(graphics, startX, midX, parent.centerY(), color);
		drawVerticalLine(graphics, midX, parent.centerY(), child.centerY(), color);
		drawHorizontalLine(graphics, midX, endX, child.centerY(), color);
	}

	private void drawHorizontalLine(GuiGraphics graphics, int x1, int x2, int y, int color) {
		int left = Math.min(x1, x2) - 1;
		int right = Math.max(x1, x2) + 2;
		graphics.fill(left, y - 1, right, y + 2, color);
	}

	private void drawVerticalLine(GuiGraphics graphics, int x, int y1, int y2, int color) {
		int top = Math.min(y1, y2) - 1;
		int bottom = Math.max(y1, y2) + 2;
		graphics.fill(x - 1, top, x + 2, bottom, color);
	}

	private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
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

	private record NodeView(QuestDefinition quest, int x, int y, int size) {
		int centerX() {
			return x + size / 2;
		}

		int centerY() {
			return y + size / 2;
		}
	}

	private record CategoryView(QuestCategoryDefinition category, int baseX, int y, int width, int height,
			int baseIconX, int iconY, boolean selected) {
	}

	private record SlidingWidget(AbstractWidget widget, int baseX) {
	}
}
