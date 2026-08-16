package com.projectflint.flintquests.api;

import com.projectflint.flintquests.engine.QuestEngine;
import com.projectflint.flintquests.progress.QuestProgress;
import com.projectflint.flintquests.progress.TaskProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Stable, general-purpose public API for integrating any Fabric mod with Flint Quests.
 *
 * <p>Project Flint is an important consumer of this API, but no Project Flint-specific behavior belongs here.</p>
 */
public final class FlintQuestAPI {
    /** Fabric entrypoint key for optional integrations. */
    public static final String ENTRYPOINT_KEY = "flintquests";
    public static final String API_VERSION = "1.1";

    private FlintQuestAPI() {
    }

    // ---- Custom event discovery ----

    public static boolean registerEvent(String eventId, String title, String description) {
        return registerEvent(QuestEventDefinition.builder(eventId, title)
                .description(description)
                .build());
    }

    public static boolean registerEvent(QuestEventDefinition definition) {
        return QuestEventRegistry.register(definition);
    }

    public static Optional<QuestEventDefinition> getRegisteredEvent(String eventId) {
        return QuestEventRegistry.get(eventId);
    }

    public static boolean isEventRegistered(String eventId) {
        return getRegisteredEvent(eventId).isPresent();
    }

    public static List<QuestEventDefinition> getRegisteredEvents() {
        return QuestEventRegistry.all();
    }

    // ---- Event triggering ----

    public static void trigger(ServerPlayer player, String eventId) {
        trigger(player, eventId, 1);
    }

    public static void trigger(ServerPlayer player, Identifier eventId) {
        if (eventId != null) trigger(player, eventId.toString(), 1);
    }

    public static void trigger(ServerPlayer player, String eventId, int amount) {
        if (player == null || amount <= 0) return;
        String rawId = eventId == null ? "" : eventId.trim();
        Identifier parsed = Identifier.tryParse(rawId);
        if (parsed == null || !rawId.contains(":") || parsed.getPath().isBlank()) {
            throw new IllegalArgumentException("Flint Quests event IDs must be valid namespaced IDs (example: mymod:activated_machine): " + eventId);
        }
        QuestEngine.incrementCustomEvent(player, parsed.toString(), amount);
    }

    public static void trigger(ServerPlayer player, Identifier eventId, int amount) {
        if (eventId != null) trigger(player, eventId.toString(), amount);
    }

    // ---- Read-only progress queries ----

    public static boolean isQuestComplete(ServerPlayer player, String questId) {
        return player != null && QuestEngine.isCompleted(player, questId);
    }

    public static boolean isTaskComplete(ServerPlayer player, String questId, String taskId) {
        return getTaskProgress(player, questId, taskId).map(TaskProgressSnapshot::complete).orElse(false);
    }

    public static int getTaskProgressValue(ServerPlayer player, String questId, String taskId) {
        return getTaskProgress(player, questId, taskId).map(TaskProgressSnapshot::value).orElse(0);
    }

    public static boolean isRewardClaimed(ServerPlayer player, String questId) {
        QuestProgress progress = player == null ? null : QuestEngine.progress(player, questId);
        return progress != null && progress.rewardClaimed;
    }

    public static Optional<TaskProgressSnapshot> getTaskProgress(ServerPlayer player, String questId, String taskId) {
        if (player == null || questId == null || taskId == null) return Optional.empty();
        QuestProgress questProgress = QuestEngine.progress(player, questId);
        if (questProgress == null) return Optional.empty();
        TaskProgress taskProgress = questProgress.tasks.get(taskId);
        if (taskProgress == null) return Optional.empty();
        return Optional.of(new TaskProgressSnapshot(taskProgress.value, taskProgress.complete));
    }

    /** Immutable API view; callers cannot mutate Flint Quests' saved progress. */
    public record TaskProgressSnapshot(int value, boolean complete) {
    }
}
