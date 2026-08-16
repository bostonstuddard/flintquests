package com.projectflint.flintquests.data;

public final class QuestReward {
	public String item = "minecraft:air";
	public int count = 1;

	public QuestReward() {
	}

	public QuestReward(String item, int count) {
		this.item = item == null ? "minecraft:air" : item;
		this.count = Math.max(1, count);
	}

	public void normalize() {
		if (item == null || item.isBlank()) item = "minecraft:air";
		count = Math.max(1, count);
	}
}
