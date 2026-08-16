package com.projectflint.flintquests.api;

import com.projectflint.flintquests.engine.QuestEngine;
import net.minecraft.server.level.ServerPlayer;

/**
 * Compatibility facade kept for integrations written against Flint Quests 1.0 and earlier.
 * New integrations should use {@link FlintQuestAPI}.
 */
@Deprecated(forRemoval = false)
public final class FlintQuestHooks {
    private FlintQuestHooks() {
    }

    public static void trigger(ServerPlayer player, String eventId) {
        trigger(player, eventId, 1);
    }

    public static void trigger(ServerPlayer player, String eventId, int amount) {
        if (player == null || eventId == null || amount <= 0) return;
        QuestEngine.incrementCustomEvent(player, eventId, amount);
    }

    public static boolean isQuestComplete(ServerPlayer player, String questId) {
        return FlintQuestAPI.isQuestComplete(player, questId);
    }
}
