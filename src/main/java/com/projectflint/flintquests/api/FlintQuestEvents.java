package com.projectflint.flintquests.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side lifecycle callbacks for mods that want to react to Flint Quests without polling progress.
 */
public final class FlintQuestEvents {
    private FlintQuestEvents() {
    }

    public static final Event<TaskProgressChanged> TASK_PROGRESS_CHANGED = EventFactory.createArrayBacked(
            TaskProgressChanged.class,
            listeners -> (player, questId, taskId, value, complete) -> {
                for (TaskProgressChanged listener : listeners) listener.onTaskProgressChanged(player, questId, taskId, value, complete);
            });

    public static final Event<QuestCompleted> QUEST_COMPLETED = EventFactory.createArrayBacked(
            QuestCompleted.class,
            listeners -> (player, questId) -> {
                for (QuestCompleted listener : listeners) listener.onQuestCompleted(player, questId);
            });

    public static final Event<RewardClaimed> REWARD_CLAIMED = EventFactory.createArrayBacked(
            RewardClaimed.class,
            listeners -> (player, questId) -> {
                for (RewardClaimed listener : listeners) listener.onRewardClaimed(player, questId);
            });

    @FunctionalInterface
    public interface TaskProgressChanged {
        void onTaskProgressChanged(ServerPlayer player, String questId, String taskId, int value, boolean complete);
    }

    @FunctionalInterface
    public interface QuestCompleted {
        void onQuestCompleted(ServerPlayer player, String questId);
    }

    @FunctionalInterface
    public interface RewardClaimed {
        void onRewardClaimed(ServerPlayer player, String questId);
    }
}
