package com.projectflint.flintquests.client;

import com.projectflint.flintquests.data.QuestDefinition;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class QuestCompletionNotifier {
    private QuestCompletionNotifier() {
    }

    public static void show(Minecraft minecraft, QuestDefinition quest) {
        if (minecraft == null || quest == null) return;

        ItemStack icon = QuestIconHelper.stackFor(quest);
        Identifier toastId = Identifier.fromNamespaceAndPath("flintquests", "toast/" + safePath(quest.id));
        AdvancementHolder advancement = Advancement.Builder.advancement()
                .display(
                        icon,
                        Component.literal(quest.title),
                        Component.literal("Flint Quest completed"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        false,
                        true
                )
                .build(toastId);

        minecraft.getToastManager().addToast(new AdvancementToast(advancement));
    }

    private static String safePath(String questId) {
        String value = questId == null ? "quest" : questId;
        int colon = value.indexOf(':');
        if (colon >= 0 && colon + 1 < value.length()) value = value.substring(colon + 1);
        value = value.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        return value.isBlank() ? "quest" : value;
    }
}
