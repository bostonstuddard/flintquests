package com.projectflint.flintquests.client;

import com.google.gson.Gson;
import com.projectflint.flintquests.progress.PlayerQuestData;
import com.projectflint.flintquests.progress.QuestProgress;
import com.projectflint.flintquests.progress.TaskProgress;

public final class ClientQuestProgress {
    private static final Gson GSON = new Gson();
    private static PlayerQuestData data = new PlayerQuestData();

    private ClientQuestProgress() {
    }

    public static void applyJson(String json) {
        try {
            PlayerQuestData parsed = GSON.fromJson(json, PlayerQuestData.class);
            data = parsed == null ? new PlayerQuestData() : parsed;
        } catch (Exception ignored) {
            data = new PlayerQuestData();
        }
    }

    public static boolean questComplete(String questId) {
        QuestProgress progress = data.quests.get(questId);
        return progress != null && progress.complete;
    }

    public static boolean taskComplete(String questId, String taskId) {
        QuestProgress quest = data.quests.get(questId);
        if (quest == null) return false;
        TaskProgress task = quest.tasks.get(taskId);
        return task != null && task.complete;
    }

    public static void markQuestComplete(String questId) {
        QuestProgress progress = data.quests.computeIfAbsent(questId, ignored -> new QuestProgress());
        progress.complete = true;
    }

    public static void clear() {
        data = new PlayerQuestData();
    }
}
