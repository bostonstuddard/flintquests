package com.projectflint.flintquests.api;

/**
 * Registrar passed to optional Flint Quests integration entrypoints.
 * Provider metadata is filled from the mod that owns the entrypoint when it is omitted by the definition.
 */
public interface QuestEventRegistrar {
    boolean registerEvent(QuestEventDefinition definition);

    default boolean registerEvent(String eventId, String title, String description) {
        return registerEvent(QuestEventDefinition.builder(eventId, title)
                .description(description)
                .build());
    }
}
