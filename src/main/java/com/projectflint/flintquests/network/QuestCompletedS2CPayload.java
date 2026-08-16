package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestCompletedS2CPayload(String questId) implements CustomPacketPayload {
    public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "quest_completed");
    public static final Type<QuestCompletedS2CPayload> ID = new Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestCompletedS2CPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestCompletedS2CPayload::questId, QuestCompletedS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
