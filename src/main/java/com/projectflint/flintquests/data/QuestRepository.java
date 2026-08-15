package com.projectflint.flintquests.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.projectflint.flintquests.config.ConfigManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestRepository {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MIGRATION_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
	private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();
	private static final Map<String, String> ID_MIGRATIONS = new LinkedHashMap<>();

	private QuestRepository() {
	}

	public static synchronized void load() {
		QUESTS.clear();
		loadMigrations();
		Path directory = ConfigManager.questsDirectory();
		try {
			Files.createDirectories(directory);
			ensureExample(directory);
			try (var paths = Files.list(directory)) {
				paths.filter(path -> path.getFileName().toString().endsWith(".json"))
						.sorted()
						.forEach(QuestRepository::loadFile);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load Flint Quests definitions", exception);
		}
	}

	private static void loadFile(Path path) {
		try (Reader reader = Files.newBufferedReader(path)) {
			QuestDefinition definition = GSON.fromJson(reader, QuestDefinition.class);
			if (definition == null) return;
			definition.normalize();
			QUESTS.put(definition.id, definition);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to read quest file: " + path, exception);
		}
	}

	public static synchronized void save(QuestDefinition definition) {
		definition.normalize();
		writeDefinition(definition);
		QUESTS.put(definition.id, definition);
	}

	/**
	 * Saves a quest and safely handles an ID-path change. The namespace remains fixed to flintquests:.
	 * Dependencies are rewritten immediately, and a persistent migration alias is recorded so existing
	 * player progress can follow the renamed quest instead of becoming orphaned.
	 */
	public static synchronized void saveRenamed(String previousId, QuestDefinition definition) {
		definition.normalize();
		String oldId = canonicalId(previousId);
		String newId = definition.id;

		if (oldId.isBlank() || oldId.equals(newId)) {
			save(definition);
			return;
		}
		if (QUESTS.containsKey(newId) && !newId.equals(oldId)) {
			throw new IllegalArgumentException("A quest already uses ID " + newId);
		}

		List<QuestDefinition> changedReferences = new ArrayList<>();
		for (QuestDefinition quest : QUESTS.values()) {
			if (quest.id.equals(oldId)) continue;
			boolean changed = false;
			List<String> rewritten = new ArrayList<>();
			for (String dependency : quest.dependencies) {
				if (dependency.equals(oldId)) {
					rewritten.add(newId);
					changed = true;
				} else {
					rewritten.add(dependency);
				}
			}
			if (changed) {
				quest.dependencies = rewritten.stream().distinct().toList();
				changedReferences.add(quest);
			}
		}

		deleteFileOnly(oldId);
		QUESTS.remove(oldId);
		writeDefinition(definition);
		QUESTS.put(newId, definition);

		for (QuestDefinition changed : changedReferences) {
			writeDefinition(changed);
			QUESTS.put(changed.id, changed);
		}

		recordMigration(oldId, newId);
	}

	public static synchronized boolean delete(String id) {
		String canonical = canonicalId(id);
		QuestDefinition removed = QUESTS.remove(canonical);
		if (removed == null) return false;

		boolean fileDeleted = deleteFileOnly(canonical);
		for (QuestDefinition quest : QUESTS.values()) {
			if (!quest.dependencies.contains(canonical)) continue;
			quest.dependencies = new ArrayList<>(quest.dependencies.stream()
					.filter(dependency -> !dependency.equals(canonical))
					.toList());
			writeDefinition(quest);
		}
		return fileDeleted;
	}

	public static synchronized QuestDefinition get(String id) {
		return QUESTS.get(id);
	}

	public static synchronized List<QuestDefinition> all() {
		return QUESTS.values().stream()
				.sorted(Comparator.comparing((QuestDefinition q) -> q.chapter).thenComparing(q -> q.title))
				.toList();
	}

	public static synchronized Collection<String> ids() {
		return List.copyOf(QUESTS.keySet());
	}

	public static synchronized String resolveRenamedId(String id) {
		String current = canonicalId(id);
		if (current.isBlank()) return current;
		Set<String> seen = new HashSet<>();
		while (seen.add(current)) {
			String next = ID_MIGRATIONS.get(current);
			if (next == null || next.isBlank() || next.equals(current)) break;
			current = canonicalId(next);
		}
		return current;
	}

	public static synchronized List<String> validate() {
		List<String> problems = new ArrayList<>();
		for (QuestDefinition quest : QUESTS.values()) {
			if (quest.tasks.isEmpty()) problems.add("WARNING: " + quest.id + " has no tasks.");
			for (String dependency : quest.dependencies) {
				if (!QUESTS.containsKey(dependency)) {
					problems.add("ERROR: " + quest.id + " depends on missing quest " + dependency + ".");
				}
				if (dependency.equals(quest.id)) {
					problems.add("ERROR: " + quest.id + " depends on itself.");
				}
			}
		}
		for (String id : QUESTS.keySet()) detectCycle(id, id, new ArrayList<>(), problems);
		return problems.stream().distinct().toList();
	}

	private static void detectCycle(String root, String current, List<String> path, List<String> problems) {
		if (path.contains(current)) {
			int start = path.indexOf(current);
			List<String> cycle = new ArrayList<>(path.subList(start, path.size()));
			cycle.add(current);
			problems.add("ERROR: Circular dependency: " + String.join(" -> ", cycle));
			return;
		}
		QuestDefinition quest = QUESTS.get(current);
		if (quest == null) return;
		if (path.size() > QUESTS.size()) return;
		List<String> nextPath = new ArrayList<>(path);
		nextPath.add(current);
		for (String dependency : quest.dependencies) {
			if (QUESTS.containsKey(dependency)) detectCycle(root, dependency, nextPath, problems);
		}
	}

	private static void writeDefinition(QuestDefinition definition) {
		Path path = questFile(definition.id);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(definition, writer);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to save quest " + definition.id, exception);
		}
	}

	private static boolean deleteFileOnly(String id) {
		try {
			return Files.deleteIfExists(questFile(id));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to delete quest " + id, exception);
		}
	}

	private static Path questFile(String id) {
		String safeName = id.replace(':', '_').replace('/', '_');
		return ConfigManager.questsDirectory().resolve(safeName + ".json");
	}

	private static Path migrationsFile() {
		return ConfigManager.root().resolve("quest_id_migrations.json");
	}

	private static void loadMigrations() {
		ID_MIGRATIONS.clear();
		Path file = migrationsFile();
		if (Files.notExists(file)) return;
		try (Reader reader = Files.newBufferedReader(file)) {
			Map<String, String> loaded = GSON.fromJson(reader, MIGRATION_MAP_TYPE);
			if (loaded == null) return;
			for (Map.Entry<String, String> entry : loaded.entrySet()) {
				String from = canonicalId(entry.getKey());
				String to = canonicalId(entry.getValue());
				if (!from.isBlank() && !to.isBlank() && !from.equals(to)) ID_MIGRATIONS.put(from, to);
			}
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to load Flint Quests quest ID migrations", exception);
		}
	}

	private static void recordMigration(String oldId, String newId) {
		oldId = canonicalId(oldId);
		newId = canonicalId(newId);
		if (oldId.isBlank() || newId.isBlank() || oldId.equals(newId)) return;

		ID_MIGRATIONS.remove(newId);
		List<String> keys = new ArrayList<>(ID_MIGRATIONS.keySet());
		for (String key : keys) {
			if (resolveRenamedId(key).equals(oldId)) ID_MIGRATIONS.put(key, newId);
		}
		ID_MIGRATIONS.put(oldId, newId);
		ID_MIGRATIONS.entrySet().removeIf(entry -> entry.getKey().equals(entry.getValue()));
		saveMigrations();
	}

	private static void saveMigrations() {
		Path file = migrationsFile();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(ID_MIGRATIONS, writer);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to save Flint Quests quest ID migrations", exception);
		}
	}

	private static String canonicalId(String value) {
		if (value == null || value.isBlank()) return "";
		String path = value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
		path = QuestDefinition.normalizeIdPath(path);
		return path.isBlank() ? "" : "flintquests:" + path;
	}

	private static void ensureExample(Path directory) throws IOException {
		try (var paths = Files.list(directory)) {
			if (paths.anyMatch(path -> path.getFileName().toString().endsWith(".json"))) return;
		}
		QuestDefinition first = new QuestDefinition();
		first.id = "flintquests:first_steps";
		first.chapter = "introduction";
		first.title = "First Steps";
		first.description = "Pick up a stick. This example quest can be edited or deleted.";
		first.icon = "minecraft:stick";
		first.tasks.add(new QuestTask("get_stick", TaskType.OBTAIN_ITEM, "minecraft:stick", 1, false));
		save(first);
	}
}
