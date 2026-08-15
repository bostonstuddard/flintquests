package com.projectflint.flintquests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestProgressRequestC2SPayload(String request) implements CustomPacketPayload {
    public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("flintquests", "request_progress");
    public static final Type<QuestProgressRequestC2SPayload> ID = new Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressRequestC2SPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestProgressRequestC2SPayload::request, QuestProgressRequestC2SPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
