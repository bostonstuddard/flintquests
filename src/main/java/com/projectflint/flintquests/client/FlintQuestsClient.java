package com.projectflint.flintquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.network.QuestCompletedS2CPayload;
import com.projectflint.flintquests.network.QuestProgressS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class FlintQuestsClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.flintquests.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.MISC
        ));

        ClientPlayNetworking.registerGlobalReceiver(QuestProgressS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientQuestProgress.applyJson(payload.json());
                    if (context.client().screen instanceof QuestBookScreen book) book.refreshFromProgress();
                    if (context.client().screen instanceof QuestDetailScreen detail) detail.refreshFromProgress();
                }));

        ClientPlayNetworking.registerGlobalReceiver(QuestCompletedS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientQuestProgress.markQuestComplete(payload.questId());
                    QuestDefinition quest = QuestRepository.get(payload.questId());
                    if (quest != null) QuestCompletionNotifier.show(context.client(), quest);
                    if (context.client().screen instanceof QuestBookScreen book) book.refreshFromProgress();
                    if (context.client().screen instanceof QuestDetailScreen detail) detail.refreshFromProgress();
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null) client.setScreen(new QuestBookScreen(null));
            }
        });
    }
}
