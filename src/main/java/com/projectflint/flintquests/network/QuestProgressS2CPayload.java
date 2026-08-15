package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestProgressS2CPayload(String json) implements CustomPacketPayload {
    public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "progress_sync");
    public static final Type<QuestProgressS2CPayload> ID = new Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressS2CPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestProgressS2CPayload::json, QuestProgressS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
