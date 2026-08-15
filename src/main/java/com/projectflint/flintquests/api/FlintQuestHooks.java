package com.projectflint.flintquests.api;

import com.projectflint.flintquests.engine.QuestEngine;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public integration surface for Project Flint and other mods.
 * Keep this class source-compatible whenever possible.
 */
public final class FlintQuestHooks {
    private FlintQuestHooks() {
    }

    public static void trigger(ServerPlayer player, String eventId) {
        trigger(player, eventId, 1);
    }

    public static void trigger(ServerPlayer player, String eventId, int amount) {
        QuestEngine.incrementCustomEvent(player, eventId, amount);
    }

    public static boolean isQuestComplete(ServerPlayer player, String questId) {
        return QuestEngine.isCompleted(player, questId);
    }
}
