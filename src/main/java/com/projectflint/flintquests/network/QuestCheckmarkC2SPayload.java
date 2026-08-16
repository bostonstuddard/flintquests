package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestCheckmarkC2SPayload(String questId, String taskId) implements CustomPacketPayload {
	public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "checkmark_task");
	public static final Type<QuestCheckmarkC2SPayload> ID = new Type<>(PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, QuestCheckmarkC2SPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, QuestCheckmarkC2SPayload::questId,
					ByteBufCodecs.STRING_UTF8, QuestCheckmarkC2SPayload::taskId,
					QuestCheckmarkC2SPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
