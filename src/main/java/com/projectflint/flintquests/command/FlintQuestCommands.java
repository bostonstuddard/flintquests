package com.projectflint.flintquests.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.projectflint.flintquests.config.ConfigManager;
import com.projectflint.flintquests.data.CategoryRepository;
import com.projectflint.flintquests.data.QuestDefinition;
import com.projectflint.flintquests.data.QuestRepository;
import com.projectflint.flintquests.engine.QuestEngine;
import com.projectflint.flintquests.progress.ProgressManager;
import com.projectflint.flintquests.progress.QuestProgress;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class FlintQuestCommands {
	private FlintQuestCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("flintquests")
						.requires(source -> ConfigManager.devEnvironmentAvailable())
						.executes(context -> help(context.getSource().getPlayerOrException()))
						.then(Commands.literal("list")
								.executes(context -> list(context.getSource().getPlayerOrException())))
						.then(Commands.literal("progress")
								.executes(context -> progress(context.getSource().getPlayerOrException())))
						.then(Commands.literal("reload")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> reload(context.getSource().getPlayerOrException())))
						.then(Commands.literal("validate")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> validate(context.getSource().getPlayerOrException())))
						.then(Commands.literal("editing")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("enabled", BoolArgumentType.bool())
										.executes(context -> editing(
												context.getSource().getPlayerOrException(),
												BoolArgumentType.getBool(context, "enabled")
										))))
						.then(Commands.literal("reset")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> reset(context.getSource().getPlayerOrException())))
		));
	}

	private static int help(ServerPlayer player) {
		player.displayClientMessage(Component.literal("Flint Quests commands: list, progress, reload, validate, editing <true|false>, reset"), false);
		return 1;
	}

	private static int list(ServerPlayer player) {
		List<QuestDefinition> quests = QuestRepository.all();
		player.displayClientMessage(Component.literal("Flint Quests: " + quests.size() + " loaded quest(s)."), false);
		for (QuestDefinition quest : quests) {
			String state = QuestEngine.isCompleted(player, quest.id) ? "[COMPLETE] " : "[ ] ";
			player.displayClientMessage(Component.literal(state + quest.id + " - " + quest.title), false);
		}
		return quests.size();
	}

	private static int progress(ServerPlayer player) {
		int shown = 0;
		for (QuestDefinition quest : QuestRepository.all()) {
			QuestProgress progress = QuestEngine.progress(player, quest.id);
			if (progress == null) continue;
			player.displayClientMessage(Component.literal(
					(progress.complete ? "[COMPLETE] " : "[ACTIVE] ") + quest.title
			), false);
			shown++;
		}
		if (shown == 0) player.displayClientMessage(Component.literal("No Flint Quests progress recorded yet."), false);
		return shown;
	}

	private static int reload(ServerPlayer player) {
		ConfigManager.load();
		QuestRepository.load();
		CategoryRepository.load();
		player.displayClientMessage(Component.literal("Reloaded Flint Quests config, categories, and quest definitions."), false);
		return 1;
	}

	private static int validate(ServerPlayer player) {
		List<String> problems = QuestRepository.validate();
		if (problems.isEmpty()) {
			player.displayClientMessage(Component.literal("Flint Quests validation passed."), false);
			return 1;
		}
		player.displayClientMessage(Component.literal("Flint Quests validation found " + problems.size() + " issue(s):"), false);
		for (String problem : problems) player.displayClientMessage(Component.literal(problem), false);
		return problems.stream().anyMatch(value -> value.startsWith("ERROR")) ? 0 : 1;
	}

	private static int editing(ServerPlayer player, boolean enabled) {
		ConfigManager.get().questEditing = enabled;
		ConfigManager.save();
		player.displayClientMessage(Component.literal("Flint Quests editing = " + enabled), false);
		return 1;
	}

	private static int reset(ServerPlayer player) {
		ProgressManager.reset(player);
		player.displayClientMessage(Component.literal("Your Flint Quests progress was reset."), false);
		return 1;
	}
}
