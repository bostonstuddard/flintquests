package com.projectflint.flintquests.progress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProgressManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<UUID, PlayerQuestData> CACHE = new HashMap<>();

	private ProgressManager() {
	}

	public static synchronized PlayerQuestData get(ServerPlayer player) {
		PlayerQuestData data = CACHE.computeIfAbsent(player.getUUID(), uuid -> load(player.level().getServer(), uuid));
		boolean changed = migrateRenamedQuestIds(data);
		changed |= pruneRemovedDefinitions(data);
		if (changed) save(player.level().getServer(), player.getUUID(), data);
		return data;
	}

	public static synchronized void save(ServerPlayer player) {
		PlayerQuestData data = CACHE.get(player.getUUID());
		if (data != null) save(player.level().getServer(), player.getUUID(), data);
	}

	public static synchronized void saveAll(MinecraftServer server) {
		for (Map.Entry<UUID, PlayerQuestData> entry : CACHE.entrySet()) {
			save(server, entry.getKey(), entry.getValue());
		}
	}

	public static synchronized void clear() {
		CACHE.clear();
	}

	public static synchronized void reset(ServerPlayer player) {
		CACHE.put(player.getUUID(), new PlayerQuestData());
		save(player);
	}

	private static PlayerQuestData load(MinecraftServer server, UUID uuid) {
		Path file = file(server, uuid);
		if (Files.notExists(file)) return new PlayerQuestData();
		try (Reader reader = Files.newBufferedReader(file)) {
			PlayerQuestData data = GSON.fromJson(reader, PlayerQuestData.class);
			return data == null ? new PlayerQuestData() : data;
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to load Flint Quests progress for " + uuid, exception);
		}
	}

	private static void save(MinecraftServer server, UUID uuid, PlayerQuestData data) {
		Path file = file(server, uuid);
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to save Flint Quests progress for " + uuid, exception);
		}
	}


	private static boolean pruneRemovedDefinitions(PlayerQuestData data) {
		boolean changed = false;
		for (String questId : new ArrayList<>(data.quests.keySet())) {
			QuestDefinition definition = QuestRepository.get(questId);
			if (definition == null) {
				data.quests.remove(questId);
				changed = true;
				continue;
			}

			QuestProgress progress = data.quests.get(questId);
			if (progress == null) continue;
			java.util.Set<String> validTaskIds = definition.tasks.stream()
					.map(task -> task.id)
					.collect(java.util.stream.Collectors.toSet());
			for (String taskId : new ArrayList<>(progress.tasks.keySet())) {
				if (validTaskIds.contains(taskId)) continue;
				progress.tasks.remove(taskId);
				changed = true;
			}
		}
		return changed;
	}

	private static boolean migrateRenamedQuestIds(PlayerQuestData data) {
		boolean changed = false;
		for (String oldId : new ArrayList<>(data.quests.keySet())) {
			String newId = QuestRepository.resolveRenamedId(oldId);
			if (newId.isBlank() || newId.equals(oldId)) continue;

			QuestProgress moved = data.quests.remove(oldId);
			if (moved == null) continue;
			QuestProgress existing = data.quests.get(newId);
			if (existing == null) {
				data.quests.put(newId, moved);
			} else {
				merge(existing, moved);
			}
			changed = true;
		}
		return changed;
	}

	private static void merge(QuestProgress target, QuestProgress source) {
		target.complete |= source.complete;
		target.rewardClaimed |= source.rewardClaimed;
		if (target.completedAt == 0L || (source.completedAt > 0L && source.completedAt < target.completedAt)) {
			target.completedAt = source.completedAt;
		}
		for (Map.Entry<String, TaskProgress> entry : source.tasks.entrySet()) {
			TaskProgress sourceTask = entry.getValue();
			TaskProgress targetTask = target.tasks.computeIfAbsent(entry.getKey(), ignored -> new TaskProgress());
			targetTask.value = Math.max(targetTask.value, sourceTask.value);
			targetTask.complete |= sourceTask.complete;
		}
	}

	private static Path file(MinecraftServer server, UUID uuid) {
		return server.getWorldPath(LevelResource.ROOT)
				.resolve("flintquests")
				.resolve("playerdata")
				.resolve(uuid + ".json");
	}
}
