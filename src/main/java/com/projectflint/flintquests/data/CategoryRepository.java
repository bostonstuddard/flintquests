package com.projectflint.flintquests.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectflint.flintquests.config.ConfigManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CategoryRepository {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, QuestCategoryDefinition> EXPLICIT = new LinkedHashMap<>();

	private CategoryRepository() {
	}

	public static synchronized void load() {
		EXPLICIT.clear();
		Path directory = ConfigManager.categoriesDirectory();
		try {
			Files.createDirectories(directory);
			try (var paths = Files.list(directory)) {
				paths.filter(path -> path.getFileName().toString().endsWith(".json"))
						.sorted()
						.forEach(CategoryRepository::loadFile);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load Flint Quests categories", exception);
		}
	}

	private static void loadFile(Path path) {
		try (Reader reader = Files.newBufferedReader(path)) {
			QuestCategoryDefinition definition = GSON.fromJson(reader, QuestCategoryDefinition.class);
			if (definition == null) return;
			definition.normalize();
			EXPLICIT.put(definition.id, definition);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to read category file: " + path, exception);
		}
	}

	public static synchronized void save(QuestCategoryDefinition definition) {
		definition.normalize();
		String safeName = safeFileName(definition.id);
		Path path = ConfigManager.categoriesDirectory().resolve(safeName + ".json");
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(definition, writer);
			}
			EXPLICIT.put(definition.id, definition);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to save category " + definition.id, exception);
		}
	}

	public static synchronized boolean delete(String id) {
		EXPLICIT.remove(id);
		try {
			return Files.deleteIfExists(ConfigManager.categoriesDirectory().resolve(safeFileName(id) + ".json"));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to delete category " + id, exception);
		}
	}

	public static synchronized QuestCategoryDefinition get(String id) {
		QuestCategoryDefinition explicit = EXPLICIT.get(id);
		if (explicit != null) return explicit;
		return resolvedMap().get(id);
	}

	public static synchronized boolean isExplicit(String id) {
		return EXPLICIT.containsKey(id);
	}

	public static synchronized List<QuestCategoryDefinition> all() {
		return resolvedMap().values().stream()
				.sorted(Comparator.comparingInt((QuestCategoryDefinition category) -> category.order)
						.thenComparing(category -> category.title, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(category -> category.id, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	public static synchronized List<QuestCategoryDefinition> roots() {
		Map<String, QuestCategoryDefinition> resolved = resolvedMap();
		return resolved.values().stream()
				.filter(category -> category.parent.isBlank() || !resolved.containsKey(category.parent))
				.sorted(categoryComparator())
				.toList();
	}

	public static synchronized List<QuestCategoryDefinition> childrenOf(String parentId) {
		return resolvedMap().values().stream()
				.filter(category -> category.parent.equals(parentId))
				.sorted(categoryComparator())
				.toList();
	}

	public static synchronized Collection<String> ids() {
		return List.copyOf(resolvedMap().keySet());
	}

	private static Comparator<QuestCategoryDefinition> categoryComparator() {
		return Comparator.comparingInt((QuestCategoryDefinition category) -> category.order)
				.thenComparing(category -> category.title, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(category -> category.id, String.CASE_INSENSITIVE_ORDER);
	}

	private static Map<String, QuestCategoryDefinition> resolvedMap() {
		Map<String, QuestCategoryDefinition> resolved = new LinkedHashMap<>();
		for (QuestCategoryDefinition category : EXPLICIT.values()) {
			resolved.put(category.id, category.copy());
		}

		for (QuestDefinition quest : QuestRepository.all()) {
			ensureImplicitPath(resolved, normalizeQuestCategory(quest.chapter));
		}

		if (resolved.isEmpty()) ensureImplicitPath(resolved, "introduction");

		List<String> missingParents = new ArrayList<>();
		for (QuestCategoryDefinition category : resolved.values()) {
			if (!category.parent.isBlank() && !resolved.containsKey(category.parent)) {
				missingParents.add(category.parent);
			}
		}
		for (String parent : missingParents) ensureImplicitPath(resolved, parent);
		return resolved;
	}

	private static void ensureImplicitPath(Map<String, QuestCategoryDefinition> resolved, String id) {
		if (id == null || id.isBlank()) id = "introduction";
		id = id.trim();
		if (resolved.containsKey(id)) return;

		String parent = "";
		int split = id.lastIndexOf('/');
		if (split > 0) {
			parent = id.substring(0, split).trim();
			ensureImplicitPath(resolved, parent);
		}

		QuestCategoryDefinition category = new QuestCategoryDefinition();
		category.id = id;
		category.title = QuestCategoryDefinition.displayName(id);
		category.parent = parent;
		category.icon = "minecraft:book";
		category.order = 0;
		category.normalize();
		resolved.put(id, category);
	}

	private static String normalizeQuestCategory(String category) {
		return category == null || category.isBlank() ? "introduction" : category.trim();
	}

	private static String safeFileName(String id) {
		return id.replace(':', '_').replace('/', '_').replace('\\', '_');
	}
}
