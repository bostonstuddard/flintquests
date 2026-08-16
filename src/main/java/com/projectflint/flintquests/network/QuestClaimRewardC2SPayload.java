package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestClaimRewardC2SPayload(String questId) implements CustomPacketPayload {
	public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "claim_reward");
	public static final Type<QuestClaimRewardC2SPayload> ID = new Type<>(PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, QuestClaimRewardC2SPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestClaimRewardC2SPayload::questId, QuestClaimRewardC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
