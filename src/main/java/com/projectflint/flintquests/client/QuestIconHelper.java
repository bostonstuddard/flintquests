package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.QuestDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class QuestIconHelper {
	private QuestIconHelper() {
	}

	static ItemStack stackFor(QuestDefinition quest) {
		return stackFor(quest == null ? null : quest.icon);
	}

	static ItemStack stackFor(String itemId) {
		Identifier id = Identifier.tryParse(itemId == null ? "" : itemId.trim());
		Item item = id == null ? Items.BOOK : BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) item = Items.BOOK;
		return new ItemStack(item);
	}
}
