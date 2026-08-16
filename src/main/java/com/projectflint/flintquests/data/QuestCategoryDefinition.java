package com.projectflint.flintquests.data;

public final class QuestCategoryDefinition {
	public String id = "introduction";
	public String title = "Introduction";
	public String icon = "minecraft:book";
	public String parent = "";
	/** True = opens a quest canvas. False = grouping/dropdown header only. */
	public boolean selectable = true;
	public int order = 0;

	public void normalize() {
		if (id == null || id.isBlank()) id = "introduction";
		id = id.trim();
		if (title == null || title.isBlank()) title = displayName(id);
		if (icon == null || icon.isBlank()) icon = "minecraft:book";
		if (parent == null) parent = "";
		parent = parent.trim();
		if (parent.equals(id)) parent = "";
	}

	public QuestCategoryDefinition copy() {
		QuestCategoryDefinition copy = new QuestCategoryDefinition();
		copy.id = id;
		copy.title = title;
		copy.icon = icon;
		copy.parent = parent;
		copy.selectable = selectable;
		copy.order = order;
		return copy;
	}

	public static String displayName(String id) {
		if (id == null || id.isBlank()) return "Introduction";
		String value = id.trim();
		int split = value.lastIndexOf('/');
		String name = split >= 0 ? value.substring(split + 1) : value;
		return name.isBlank() ? value : name;
	}
}
