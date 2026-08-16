package com.projectflint.flintquests.engine;

import com.projectflint.flintquests.api.FlintQuestEvents;

import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CompletionMode;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.data.QuestTask;
import com.projectflint.flintquests.data.QuestReward;
import com.projectflint.flintquests.data.TaskType;
import com.projectflint.flintquests.progress.PlayerQuestData;
import com.projectflint.flintquests.progress.ProgressManager;
import com.projectflint.flintquests.progress.QuestProgress;
import com.projectflint.flintquests.progress.TaskProgress;
import com.projectflint.flintquests.network.QuestNetworking;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class QuestEngine {
    private static long ticks;

    private QuestEngine() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;
            int interval = Math.max(1, ConfigManager.get().inventoryScanIntervalTicks);
            if (ticks % interval != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                scanInventory(player);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            incrementMatching(serverPlayer, TaskType.BREAK_BLOCK, id, 1);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                ItemStack stack = player.getItemInHand(hand);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                incrementMatching(serverPlayer, TaskType.USE_ITEM, id, 1);
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                String id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString();
                incrementMatching(serverPlayer, TaskType.INTERACT_BLOCK, id, 1);
            }
            return InteractionResult.PASS;
        });
    }

    public static void scanInventory(ServerPlayer player) {
        PlayerQuestData data = ProgressManager.get(player);
        boolean changed = false;
        for (QuestDefinition quest : QuestRepository.all()) {
            if (isCompleted(data, quest.id) || !dependenciesSatisfied(data, quest)) continue;
            QuestProgress questProgress = data.quests.computeIfAbsent(quest.id, ignored -> new QuestProgress());
            for (QuestTask task : quest.tasks) {
                if (task.type != TaskType.OBTAIN_ITEM) continue;
                int amount = inventoryCount(player, task.target);
                TaskProgress progress = questProgress.tasks.computeIfAbsent(task.id, ignored -> new TaskProgress());
                int oldValue = progress.value;
                boolean oldComplete = progress.complete;
                progress.value = Math.min(amount, task.count);
                progress.complete = amount >= task.count;
                boolean taskChanged = oldValue != progress.value || oldComplete != progress.complete;
                changed |= taskChanged;
                if (taskChanged) FlintQuestEvents.TASK_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChanged(player, quest.id, task.id, progress.value, progress.complete);
            }
            changed |= tryComplete(player, data, quest, questProgress);
        }
        if (changed) {
            ProgressManager.save(player);
            QuestNetworking.sendProgress(player);
        }
    }

    public static void incrementCustomEvent(ServerPlayer player, String eventId, int amount) {
        incrementMatching(player, TaskType.CUSTOM_EVENT, eventId, amount);
    }

    public static boolean checkmark(ServerPlayer player, String questId) {
        PlayerQuestData data = ProgressManager.get(player);
        QuestDefinition quest = QuestRepository.get(questId);
        if (quest == null || !dependenciesSatisfied(data, quest)) return false;
        QuestProgress questProgress = data.quests.computeIfAbsent(quest.id, ignored -> new QuestProgress());
        boolean changed = false;
        for (QuestTask task : quest.tasks) {
            if (task.type != TaskType.CHECKMARK) continue;
            TaskProgress progress = questProgress.tasks.computeIfAbsent(task.id, ignored -> new TaskProgress());
            if (!progress.complete) {
                changed = true;
                progress.value = 1;
                progress.complete = true;
                FlintQuestEvents.TASK_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChanged(player, quest.id, task.id, progress.value, true);
            } else {
                progress.value = 1;
            }
        }
        tryComplete(player, data, quest, questProgress);
        if (changed) {
            ProgressManager.save(player);
            QuestNetworking.sendProgress(player);
        }
        return changed;
    }

    public static boolean checkmark(ServerPlayer player, String questId, String taskId) {
        PlayerQuestData data = ProgressManager.get(player);
        QuestDefinition quest = QuestRepository.get(questId);
        if (quest == null || !dependenciesSatisfied(data, quest)) return false;
        QuestTask task = quest.tasks.stream()
                .filter(candidate -> candidate.type == TaskType.CHECKMARK && candidate.id.equals(taskId))
                .findFirst()
                .orElse(null);
        if (task == null) return false;
        QuestProgress questProgress = data.quests.computeIfAbsent(quest.id, ignored -> new QuestProgress());
        TaskProgress progress = questProgress.tasks.computeIfAbsent(task.id, ignored -> new TaskProgress());
        if (progress.complete) return false;
        progress.value = 1;
        progress.complete = true;
        FlintQuestEvents.TASK_PROGRESS_CHANGED.invoker()
                .onTaskProgressChanged(player, quest.id, task.id, progress.value, true);
        tryComplete(player, data, quest, questProgress);
        ProgressManager.save(player);
        QuestNetworking.sendProgress(player);
        return true;
    }


    public static boolean claimReward(ServerPlayer player, String questId) {
        PlayerQuestData data = ProgressManager.get(player);
        QuestDefinition quest = QuestRepository.get(questId);
        if (quest == null || quest.rewards == null || quest.rewards.isEmpty()) return false;
        QuestProgress progress = data.quests.get(quest.id);
        if (progress == null || !progress.complete || progress.rewardClaimed) return false;

        grantRewards(player, quest);
        progress.rewardClaimed = true;
        FlintQuestEvents.REWARD_CLAIMED.invoker().onRewardClaimed(player, quest.id);
        ProgressManager.save(player);
        QuestNetworking.sendProgress(player);
        return true;
    }

    public static int claimAllRewards(ServerPlayer player, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return 0;
        PlayerQuestData data = ProgressManager.get(player);
        int claimed = 0;
        for (QuestDefinition quest : QuestRepository.all()) {
            if (!categoryId.equals(quest.chapter) || quest.rewards == null || quest.rewards.isEmpty()) continue;
            QuestProgress progress = data.quests.get(quest.id);
            if (progress == null || !progress.complete || progress.rewardClaimed) continue;
            grantRewards(player, quest);
            progress.rewardClaimed = true;
            FlintQuestEvents.REWARD_CLAIMED.invoker().onRewardClaimed(player, quest.id);
            claimed++;
        }
        if (claimed > 0) {
            ProgressManager.save(player);
            QuestNetworking.sendProgress(player);
        }
        return claimed;
    }

    private static void grantRewards(ServerPlayer player, QuestDefinition quest) {
        for (QuestReward reward : quest.rewards) {
            if (reward == null || reward.count <= 0) continue;
            Identifier id = Identifier.tryParse(reward.item == null ? "" : reward.item.trim());
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
            if (item == null || item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item, reward.count);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) player.drop(stack, false);
        }
    }

    public static boolean isCompleted(ServerPlayer player, String questId) {
        return isCompleted(ProgressManager.get(player), questId);
    }

    public static QuestProgress progress(ServerPlayer player, String questId) {
        return ProgressManager.get(player).quests.get(questId);
    }

    private static void incrementMatching(ServerPlayer player, TaskType type, String target, int amount) {
        if (amount <= 0) return;
        PlayerQuestData data = ProgressManager.get(player);
        boolean changed = false;
        for (QuestDefinition quest : QuestRepository.all()) {
            if (isCompleted(data, quest.id) || !dependenciesSatisfied(data, quest)) continue;
            QuestProgress questProgress = data.quests.computeIfAbsent(quest.id, ignored -> new QuestProgress());
            for (QuestTask task : quest.tasks) {
                if (task.type != type || !task.target.equals(target)) continue;
                TaskProgress progress = questProgress.tasks.computeIfAbsent(task.id, ignored -> new TaskProgress());
                int old = progress.value;
                boolean oldComplete = progress.complete;
                progress.value = Math.min(task.count, progress.value + amount);
                progress.complete = progress.value >= task.count;
                boolean taskChanged = old != progress.value || oldComplete != progress.complete;
                changed |= taskChanged;
                if (taskChanged) FlintQuestEvents.TASK_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChanged(player, quest.id, task.id, progress.value, progress.complete);
            }
            changed |= tryComplete(player, data, quest, questProgress);
        }
        if (changed) {
            ProgressManager.save(player);
            QuestNetworking.sendProgress(player);
        }
    }

    private static boolean tryComplete(ServerPlayer player, PlayerQuestData data, QuestDefinition quest, QuestProgress questProgress) {
        if (questProgress.complete) return false;
        long requiredTasks = quest.tasks.stream().filter(task -> !task.optional).count();
        if (requiredTasks == 0) return false;

        boolean complete;
        if (quest.taskMode == CompletionMode.ANY) {
            complete = quest.tasks.stream()
                    .filter(task -> !task.optional)
                    .anyMatch(task -> taskComplete(questProgress, task));
        } else {
            complete = quest.tasks.stream()
                    .filter(task -> !task.optional)
                    .allMatch(task -> taskComplete(questProgress, task));
        }

        if (!complete) return false;
        questProgress.complete = true;
        questProgress.completedAt = System.currentTimeMillis();
        FlintQuestEvents.QUEST_COMPLETED.invoker().onQuestCompleted(player, quest.id);
        if (ConfigManager.get().announceQuestCompletion) {
            QuestNetworking.notifyCompleted(player, quest.id);
        }
        return true;
    }

    private static boolean taskComplete(QuestProgress questProgress, QuestTask task) {
        TaskProgress progress = questProgress.tasks.get(task.id);
        return progress != null && progress.complete;
    }

    private static boolean dependenciesSatisfied(PlayerQuestData data, QuestDefinition quest) {
        if (quest.dependencies.isEmpty()) return true;
        if (quest.dependencyMode == CompletionMode.ANY) {
            return quest.dependencies.stream().anyMatch(id -> isCompleted(data, id));
        }
        return quest.dependencies.stream().allMatch(id -> isCompleted(data, id));
    }

    private static boolean isCompleted(PlayerQuestData data, String questId) {
        QuestProgress progress = data.quests.get(questId);
        return progress != null && progress.complete;
    }

    private static int inventoryCount(ServerPlayer player, String target) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(target)) count += stack.getCount();
        }
        return count;
    }
}
