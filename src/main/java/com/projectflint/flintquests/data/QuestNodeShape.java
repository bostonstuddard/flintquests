package com.projectflint.flintquests.data;

public enum QuestNodeShape {
	SQUARE("Square"),
	CIRCLE("Circle"),
	HEXAGON("Hexagon"),
	DIAMOND("Diamond");

	private final String displayName;

	QuestNodeShape(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public QuestNodeShape next() {
		QuestNodeShape[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
