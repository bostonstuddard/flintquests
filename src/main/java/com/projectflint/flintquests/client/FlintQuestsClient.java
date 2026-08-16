package com.projectflint.flintquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.projectflint.flintquests.FlintQuests;
import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.network.QuestCompletedS2CPayload;
import com.projectflint.flintquests.network.QuestProgressS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class FlintQuestsClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FlintQuests.MOD_ID, "controls")
    );

    private static KeyMapping openKey;
    private static KeyMapping toggleEditingKey;
    private static KeyMapping toggleEditingModifierKey;
    private static KeyMapping linkNodesKey;
    private static KeyMapping moveNodesKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.flintquests.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KEY_CATEGORY
        ));

        if (ConfigManager.devEnvironmentAvailable()) {
            toggleEditingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.flintquests.toggle_editing",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_J,
                    KEY_CATEGORY
            ));

            toggleEditingModifierKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.flintquests.toggle_editing_modifier",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_CONTROL,
                    KEY_CATEGORY
            ));

            linkNodesKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.flintquests.link_nodes",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_SHIFT,
                    KEY_CATEGORY
            ));

            moveNodesKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.flintquests.move_nodes",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    KEY_CATEGORY
            ));
        }

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
            boolean editingModifierDown = toggleEditingModifierDown(null);

            if (toggleEditingKey != null) {
                while (toggleEditingKey.consumeClick()) {
                    if (!editingModifierDown || !ConfigManager.devEnvironmentAvailable()) continue;
                    ConfigManager.get().questEditing = !ConfigManager.get().questEditing;
                    ConfigManager.save();
                    if (client.screen instanceof QuestBookScreen book) {
                        book.refreshEditingMode();
                    } else if (client.screen instanceof QuestEditorScreen || client.screen instanceof CategoryEditorScreen) {
                        client.setScreen(new QuestBookScreen(null));
                    }
                }
            }

            while (openKey.consumeClick()) {
                if (editingModifierDown && ConfigManager.devEnvironmentAvailable()) continue;
                if (client.player != null) client.setScreen(new QuestBookScreen(null));
            }
        });
    }


    public static boolean handleEditingToggleKey(KeyEvent event) {
        if (!ConfigManager.devEnvironmentAvailable() || toggleEditingKey == null || event == null) return false;
        if (!toggleEditingKey.matches(event) || !toggleEditingModifierDown(event)) return false;

        ConfigManager.get().questEditing = !ConfigManager.get().questEditing;
        ConfigManager.save();

        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof QuestBookScreen book) {
            book.refreshEditingMode();
        } else if (client.screen instanceof QuestEditorScreen
                || client.screen instanceof CategoryEditorScreen
                || client.screen instanceof SearchSelectScreen) {
            client.setScreen(new QuestBookScreen(null));
        } else if (client.screen instanceof FlintQuestConfigScreen configScreen) {
            configScreen.refreshEditingMode();
        }
        return true;
    }

    private static boolean toggleEditingModifierDown(KeyEvent event) {
        if (toggleEditingModifierKey == null) return false;
        try {
            InputConstants.Key key = InputConstants.getKey(toggleEditingModifierKey.saveString());
            if (key.getType() == InputConstants.Type.KEYSYM) {
                if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key.getValue())) return true;
            }
        } catch (RuntimeException ignored) {
        }
        return toggleEditingModifierKey.isDown() || (event != null && event.hasControlDown());
    }

    public static boolean linkNodesKeyDown() {
        return ConfigManager.devToolsEnabled() && linkNodesKey != null && linkNodesKey.isDown();
    }

    public static boolean moveNodesKeyDown() {
        return ConfigManager.devToolsEnabled() && moveNodesKey != null && moveNodesKey.isDown();
    }

    public static Component linkNodesKeyName() {
        return linkNodesKey == null ? Component.literal("Link key") : linkNodesKey.getTranslatedKeyMessage();
    }

    public static Component moveNodesKeyName() {
        return moveNodesKey == null ? Component.literal("Move key") : moveNodesKey.getTranslatedKeyMessage();
    }

    public static Component toggleEditingKeyName() {
        return toggleEditingKey == null ? Component.literal("J") : toggleEditingKey.getTranslatedKeyMessage();
    }

    public static Component toggleEditingModifierKeyName() {
        return toggleEditingModifierKey == null ? Component.literal("Left Control") : toggleEditingModifierKey.getTranslatedKeyMessage();
    }
}
