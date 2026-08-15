package com.projectflint.flintquests.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu integration. Fabric only asks for this entrypoint when
 * Mod Menu is installed, so Flint Quests does not require Mod Menu at runtime.
 */
public final class FlintQuestsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FlintQuestConfigScreen::new;
    }
}
