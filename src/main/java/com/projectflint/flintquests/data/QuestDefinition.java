package com.projectflint.flintquests.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuestDefinition {
	public String id = "flintquests:new_quest";
	public String chapter = "introduction";
	public String title = "New Quest";
	public String description = "";
	public String icon = "minecraft:book";
	public QuestNodeShape nodeShape = QuestNodeShape.SQUARE;
	public int x = 0;
	public int y = 0;
	public boolean hiddenUntilDependencies = false;
	public CompletionMode dependencyMode = CompletionMode.ALL;
	public CompletionMode taskMode = CompletionMode.ALL;
	public List<String> dependencies = new ArrayList<>();
	public List<QuestTask> tasks = new ArrayList<>();

	public void normalize() {
		if (id == null || id.isBlank()) id = "flintquests:new_quest";
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		path = sanitizePath(path);
		id = "flintquests:" + (path.isBlank() ? "new_quest" : path);
		if (chapter == null || chapter.isBlank()) chapter = "introduction";
		if (title == null || title.isBlank()) title = id;
		if (description == null) description = "";
		if (icon == null || icon.isBlank()) icon = "minecraft:book";
		if (nodeShape == null) nodeShape = QuestNodeShape.SQUARE;
		if (dependencyMode == null) dependencyMode = CompletionMode.ALL;
		if (taskMode == null) taskMode = CompletionMode.ALL;
		if (dependencies == null) dependencies = new ArrayList<>();
		List<String> normalizedDependencies = new ArrayList<>();
		for (String dependency : dependencies) {
			if (dependency == null || dependency.isBlank()) continue;
			String dependencyPath = dependency.contains(":") ? dependency.substring(dependency.indexOf(':') + 1) : dependency;
			dependencyPath = sanitizePath(dependencyPath);
			if (!dependencyPath.isBlank()) {
				String normalizedDependency = "flintquests:" + dependencyPath;
				if (!normalizedDependency.equals(id)) normalizedDependencies.add(normalizedDependency);
			}
		}
		dependencies = normalizedDependencies.stream().distinct().toList();
		if (tasks == null) tasks = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			QuestTask task = tasks.get(i);
			if (task != null) task.normalize(i);
		}
		tasks.removeIf(task -> task == null);
	}

	public static String normalizeIdPath(String value) {
		return sanitizePath(value == null ? "" : value);
	}

	private static String sanitizePath(String value) {
		String cleaned = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		cleaned = cleaned.replaceAll("[^a-z0-9_./-]", "_");
		while (cleaned.contains("__")) cleaned = cleaned.replace("__", "_");
		while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
		while (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length() - 1);
		return cleaned;
	}
}
