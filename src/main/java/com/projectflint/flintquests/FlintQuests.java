package com.projectflint.flintquests;

import com.projectflint.flintquests.command.FlintQuestCommands;
import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.engine.QuestEngine;
import com.projectflint.flintquests.progress.ProgressManager;
import com.projectflint.flintquests.network.QuestNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlintQuests implements ModInitializer {
    public static final String MOD_ID = "flintquests";
    public static final Logger LOGGER = LoggerFactory.getLogger("Flint Quests");

    @Override
    public void onInitialize() {
        ConfigManager.load();
        QuestRepository.load();
        CategoryRepository.load();
        QuestNetworking.register();
        QuestEngine.register();
        FlintQuestCommands.register();

        ServerLifecycleEvents.SERVER_STOPPING.register(ProgressManager::saveAll);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ProgressManager.clear());

        LOGGER.info("Flint Quests initialized with {} quest(s).", QuestRepository.all().size());
    }
}
