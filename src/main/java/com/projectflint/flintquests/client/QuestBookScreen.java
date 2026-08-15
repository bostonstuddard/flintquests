package com.projectflint.flintquests.client;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.CompletionMode;
import com.projectflint.flintquests.data.QuestCategoryDefinition;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.network.QuestProgressRequestC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestBookScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 112;
	private static final int SIDEBAR_HOVER_ZONE = 10;
	private static final int SIDEBAR_GRACE = 10;
	private static final float SIDEBAR_SLIDE_SPEED = 0.34F;
	private static final int NODE_SIZE = 30;
	private static final int GRID_SIZE = 54;
	private static final int TOP_BAR_HEIGHT = 28;
	private static final int BOTTOM_BAR_HEIGHT = 24;
	private static final int CATEGORY_ROW_HEIGHT = 18;
	private static final int CATEGORY_INDENT = 10;
	private static final int PAN_MARGIN = 46;

	private static final int COLOR_UNLOCKED = 0xFFFFD84A;
	private static final int COLOR_COMPLETED = 0xFF72FF63;
	private static final int COLOR_LOCKED = 0xFF747D88;
	private static final int COLOR_LINK_SOURCE = 0xFF61D6FF;

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
	private boolean draggingCanvas;
	private boolean altHeld;

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
		altHeld = false;
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

	private void loadUiStateOnce() {
		if (uiStateLoaded) return;
		expandedCategories.clear();
		expandedCategories.addAll(ConfigManager.get().expandedQuestCategories);
		sidebarSlide = 0.0F;
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
		if (selectedCategory == null || CategoryRepository.get(selectedCategory) == null) {
			selectedCategory = categories.getFirst().id;
		}
		expandParents(selectedCategory);
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

		int canvasLeft = 3;
		int canvasRight = width - 3;
		int canvasTop = TOP_BAR_HEIGHT + 3;
		int canvasBottom = height - BOTTOM_BAR_HEIGHT - 3;
		int centerX = canvasLeft + Math.max(0, canvasRight - canvasLeft) / 2 + panX;
		int centerY = canvasTop + Math.max(0, canvasBottom - canvasTop) / 2 + panY;

		buildSidebar();

		for (QuestDefinition quest : quests) {
			int nodeX = centerX + quest.x * GRID_SIZE - NODE_SIZE / 2;
			int nodeY = centerY + quest.y * GRID_SIZE - NODE_SIZE / 2;
			if (nodeX + NODE_SIZE < canvasLeft || nodeX > canvasRight || nodeY + NODE_SIZE < canvasTop || nodeY > canvasBottom) {
				continue;
			}

			NodeView view = new NodeView(quest, nodeX, nodeY);
			visibleNodes.add(view);
			nodesById.put(quest.id, view);
		}

		int bottomY = height - 21;
		if (ConfigManager.get().questEditing) {
			addRenderableWidget(Button.builder(Component.literal("+"), button ->
					minecraft.setScreen(new QuestEditorScreen(this, null, selectedCategory))
			).bounds(width - 48, bottomY, 20, 18).build());
		}
		addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
				.bounds(width - 24, bottomY, 20, 18).build());
		positionSidebarWidgets();
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

		if (ConfigManager.get().questEditing) {
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

		if (rawY + CATEGORY_ROW_HEIGHT > availableTop && rawY < availableBottom) {
			int indent = Math.min(depth, 6) * CATEGORY_INDENT;
			int rowX = 3 + indent;
			if (hasChildren) {
				Button expand = Button.builder(Component.literal(expandedCategories.contains(category.id) ? "v" : ">"), button -> {
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
			Button categoryButton = Button.builder(Component.literal(label), button -> selectCategory(category.id))
					.bounds(categoryX, rawY + 1, categoryWidth, 16).build();
			addSidebarWidget(categoryButton, categoryX);
			visibleCategoryViews.add(new CategoryView(category, categoryX, rawY + 1, categoryWidth, 16,
					categoryX + 2, rawY + 1, category.id.equals(selectedCategory)));
		}

		if (hasChildren && expandedCategories.contains(category.id)) {
			for (QuestCategoryDefinition child : children) {
				addCategoryTree(child, depth + 1, rowIndex, visited, availableTop, availableBottom);
			}
		}
	}

	private void selectCategory(String categoryId) {
		selectedCategory = categoryId;
		dependencySourceId = null;
		draggingNodeId = null;
		panX = 0;
		panY = 0;
		expandParents(categoryId);
		saveSidebarState();
		refreshQuestWidgets();
	}

	private void openQuestForPrimaryClick(QuestDefinition quest) {
		if (ConfigManager.get().questEditing) {
			minecraft.setScreen(new QuestEditorScreen(this, quest, normalizeCategory(quest.chapter)));
		} else {
			minecraft.setScreen(new QuestDetailScreen(this, quest));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int mouseX = (int) Math.round(click.x());
		int mouseY = (int) Math.round(click.y());
		boolean editing = ConfigManager.get().questEditing;
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
		if (editing && click.button() == 0 && click.hasShiftDown() && node != null) {
			handleDependencyClick(node.quest());
			return true;
		}

		if (editing && click.button() == 0 && (altHeld || click.hasAltDown()) && node != null) {
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
			int gridX = (int) Math.round((desiredCenterX - centerX) / GRID_SIZE);
			int gridY = (int) Math.round((desiredCenterY - centerY) / GRID_SIZE);
			if (quest.x != gridX || quest.y != gridY) {
				quest.x = gridX;
				quest.y = gridY;
				refreshQuestWidgets();
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
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == 342 || event.key() == 346) {
			altHeld = true;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (event.key() == 342 || event.key() == 346) {
			altHeld = false;
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int sidebarWidth = currentSidebarWidth();
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
			refreshQuestWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
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
			if (inside(mouseX, mouseY, node.x(), node.y(), NODE_SIZE, NODE_SIZE)) return node;
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

		int minRelX = Integer.MAX_VALUE;
		int maxRelX = Integer.MIN_VALUE;
		int minRelY = Integer.MAX_VALUE;
		int maxRelY = Integer.MIN_VALUE;
		for (QuestDefinition quest : quests) {
			int relX = quest.x * GRID_SIZE;
			int relY = quest.y * GRID_SIZE;
			minRelX = Math.min(minRelX, relX - NODE_SIZE / 2);
			maxRelX = Math.max(maxRelX, relX + NODE_SIZE / 2);
			minRelY = Math.min(minRelY, relY - NODE_SIZE / 2);
			maxRelY = Math.max(maxRelY, relY + NODE_SIZE / 2);
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

		graphics.fill(0, 0, width, height, 0xB818202A);
		graphics.fill(0, 0, width, TOP_BAR_HEIGHT, 0xE01A222D);
		graphics.fill(0, canvasBottom, width, height, 0xE01A222D);
		drawCanvasGrid(graphics, canvasLeft, canvasTop, width, canvasBottom);
		drawDependencyLines(graphics);

		QuestCategoryDefinition selected = CategoryRepository.get(selectedCategory);
		String selectedTitle = selected == null ? selectedCategory : selected.title;
		graphics.drawCenteredString(font, Component.literal(selectedTitle), width / 2, 9, 0xFFF2F5F8);

		for (NodeView node : visibleNodes) {
			boolean completed = ClientQuestProgress.questComplete(node.quest().id);
			boolean unlocked = isQuestUnlocked(node.quest());
			int stateColor = completed ? COLOR_COMPLETED : unlocked ? COLOR_UNLOCKED : COLOR_LOCKED;
			drawNodeState(graphics, node, stateColor, completed || unlocked);

			if (!unlocked && !completed) {
				graphics.fill(node.x() + 1, node.y() + 1, node.x() + NODE_SIZE - 1, node.y() + NODE_SIZE - 1, 0x550A0D11);
			}

			ItemStack stack = QuestIconHelper.stackFor(node.quest());
			graphics.renderItem(stack, node.x() + 7, node.y() + 7);

			if (node.quest().id.equals(dependencySourceId)) {
				drawBorder(graphics, node.x() - 5, node.y() - 5, NODE_SIZE + 10, NODE_SIZE + 10, COLOR_LINK_SOURCE, 2);
			}

			if (inside(mouseX, mouseY, node.x(), node.y(), NODE_SIZE, NODE_SIZE)
					&& !(sidebarWidth > 0 && mouseX < sidebarWidth + 2)) {
				String tooltip;
				if (ConfigManager.get().questEditing) {
					tooltip = node.quest().title + "  |  Left-click: edit  |  Right-click: view  |  Shift-click: link  |  Alt + drag: move";
				} else {
					tooltip = node.quest().title;
				}
				graphics.setTooltipForNextFrame(Component.literal(tooltip), mouseX, mouseY);
			}
		}

		if (sidebarWidth > 0) {
			graphics.fill(0, TOP_BAR_HEIGHT, sidebarWidth, canvasBottom, 0xF0222C38);
			graphics.fill(sidebarWidth, TOP_BAR_HEIGHT, sidebarWidth + 1, canvasBottom, 0xFF566273);
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		if (sidebarSlide > 0.03F) {
			int offset = sidebarOffset();
			for (CategoryView category : visibleCategoryViews) {
				int categoryX = category.baseX() + offset;
				int iconX = category.baseIconX() + offset;
				if (category.selected()) {
					drawBorder(graphics, categoryX - 1, category.y() - 1,
							category.width() + 2, category.height() + 2, COLOR_UNLOCKED, 1);
				}
				ItemStack stack = QuestIconHelper.stackFor(category.category().icon);
				graphics.renderItem(stack, iconX, category.iconY());
				if (ConfigManager.get().questEditing && inside(mouseX, mouseY, categoryX, category.y(), category.width(), category.height())) {
					graphics.setTooltipForNextFrame(Component.literal(category.category().title + "  |  Right-click: edit category"), mouseX, mouseY);
				}
			}
		}

		if (ConfigManager.get().questEditing && !editorNotice.isBlank() && System.currentTimeMillis() < editorNoticeUntil) {
			graphics.drawString(font, Component.literal(editorNotice), 6, height - 16, 0xFFB7C7D8, false);
		}
	}

	private void drawNodeState(GuiGraphics graphics, NodeView node, int color, boolean glow) {
		if (glow) {
			int glowColor = (0x44 << 24) | (color & 0x00FFFFFF);
			drawBorder(graphics, node.x() - 4, node.y() - 4, NODE_SIZE + 8, NODE_SIZE + 8, glowColor, 2);
		}
		drawBorder(graphics, node.x() - 2, node.y() - 2, NODE_SIZE + 4, NODE_SIZE + 4, color, 2);
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
		int spacing = 18;
		int offsetX = Math.floorMod(panX, spacing);
		int offsetY = Math.floorMod(panY, spacing);
		for (int x = left + offsetX; x < right; x += spacing) {
			for (int y = top + offsetY; y < bottom; y += spacing) {
				graphics.fill(x, y, x + 1, y + 1, 0x4D778393);
			}
		}
	}

	private void drawDependencyLines(GuiGraphics graphics) {
		for (NodeView node : visibleNodes) {
			int endX = node.centerX();
			int endY = node.centerY();
			for (String dependencyId : node.quest().dependencies) {
				NodeView dependency = nodesById.get(dependencyId);
				if (dependency == null) continue;
				int startX = dependency.centerX();
				int startY = dependency.centerY();
				int midX = startX + (endX - startX) / 2;
				int lineColor = ClientQuestProgress.questComplete(dependency.quest().id) ? COLOR_COMPLETED : COLOR_LOCKED;
				graphics.fill(Math.min(startX, midX), startY - 1, Math.max(startX, midX) + 1, startY + 1, lineColor);
				graphics.fill(midX - 1, Math.min(startY, endY), midX + 1, Math.max(startY, endY) + 1, lineColor);
				graphics.fill(Math.min(midX, endX), endY - 1, Math.max(midX, endX) + 1, endY + 1, lineColor);
			}
		}
	}

	private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private record NodeView(QuestDefinition quest, int x, int y) {
		int centerX() {
			return x + NODE_SIZE / 2;
		}

		int centerY() {
			return y + NODE_SIZE / 2;
		}
	}

	private record CategoryView(QuestCategoryDefinition category, int baseX, int y, int width, int height,
			int baseIconX, int iconY, boolean selected) {
	}

	private record SlidingWidget(AbstractWidget widget, int baseX) {
	}
}
