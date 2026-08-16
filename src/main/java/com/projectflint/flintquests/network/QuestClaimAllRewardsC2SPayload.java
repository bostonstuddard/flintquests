package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestClaimAllRewardsC2SPayload(String categoryId) implements CustomPacketPayload {
	public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "claim_all_rewards");
	public static final Type<QuestClaimAllRewardsC2SPayload> ID = new Type<>(PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, QuestClaimAllRewardsC2SPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestClaimAllRewardsC2SPayload::categoryId, QuestClaimAllRewardsC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
