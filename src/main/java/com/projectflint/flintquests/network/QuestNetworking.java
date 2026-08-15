package com.projectflint.flintquests.network;

import com.google.gson.Gson;
import com.projectflint.flintquests.engine.QuestEngine;
import com.projectflint.flintquests.progress.ProgressManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class QuestNetworking {
	private static final Gson GSON = new Gson();

	private QuestNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(QuestProgressS2CPayload.ID, QuestProgressS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(QuestCompletedS2CPayload.ID, QuestCompletedS2CPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(QuestProgressRequestC2SPayload.ID, QuestProgressRequestC2SPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(QuestCheckmarkC2SPayload.ID, QuestCheckmarkC2SPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(QuestProgressRequestC2SPayload.ID, (payload, context) ->
				sendProgress(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(QuestCheckmarkC2SPayload.ID, (payload, context) -> {
			boolean changed = QuestEngine.checkmark(context.player(), payload.questId(), payload.taskId());
			if (!changed) sendProgress(context.player());
		});
	}

	public static void sendProgress(ServerPlayer player) {
		ServerPlayNetworking.send(player, new QuestProgressS2CPayload(GSON.toJson(ProgressManager.get(player))));
	}

	public static void notifyCompleted(ServerPlayer player, String questId) {
		ServerPlayNetworking.send(player, new QuestCompletedS2CPayload(questId));
	}
}
