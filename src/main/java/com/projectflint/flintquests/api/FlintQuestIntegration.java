package com.projectflint.flintquests.api;

/**
 * Optional Fabric entrypoint for mods that integrate with Flint Quests.
 *
 * <p>Declare an implementation under the {@code flintquests} entrypoint key in fabric.mod.json. Flint Quests
 * only asks Fabric Loader for these entrypoints when Flint Quests itself is installed, so this is suitable for
 * optional integrations when the API is used as a compile-only dependency.</p>
 */
@FunctionalInterface
public interface FlintQuestIntegration {
    void register(QuestEventRegistrar registrar);
}
