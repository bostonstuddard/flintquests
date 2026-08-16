package com.projectflint.flintquests.api;

import com.projectflint.flintquests.FlintQuests;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Internal registry backing the public event-discovery API. */
public final class QuestEventRegistry {
    private static final Map<String, QuestEventDefinition> EVENTS = new LinkedHashMap<>();
    private static boolean integrationsLoaded;

    private QuestEventRegistry() {
    }

    public static synchronized boolean register(QuestEventDefinition definition) {
        return register(definition, inferProviderId(definition.id()), inferProviderName(inferProviderId(definition.id())));
    }

    static synchronized boolean register(QuestEventDefinition definition, String fallbackProviderId, String fallbackProviderName) {
        if (definition == null) return false;
        QuestEventDefinition resolved = definition.withProviderDefaults(fallbackProviderId, fallbackProviderName);
        QuestEventDefinition existing = EVENTS.get(resolved.id());
        if (existing != null) {
            if (!existing.equals(resolved)) {
                FlintQuests.LOGGER.warn("Ignoring conflicting Flint Quests event registration for {}. Existing provider: {}; attempted provider: {}.",
                        resolved.id(), providerLabel(existing), providerLabel(resolved));
            }
            return false;
        }
        EVENTS.put(resolved.id(), resolved);
        FlintQuests.LOGGER.debug("Registered Flint Quests event {} from {}.", resolved.id(), providerLabel(resolved));
        return true;
    }

    public static synchronized Optional<QuestEventDefinition> get(String eventId) {
        if (eventId == null) return Optional.empty();
        Identifier id = Identifier.tryParse(eventId.trim());
        return id == null ? Optional.empty() : Optional.ofNullable(EVENTS.get(id.toString()));
    }

    public static synchronized List<QuestEventDefinition> all() {
        List<QuestEventDefinition> copy = new ArrayList<>(EVENTS.values());
        copy.sort(Comparator
                .comparing((QuestEventDefinition event) -> event.providerName().isBlank() ? event.providerId() : event.providerName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(QuestEventDefinition::group, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(QuestEventDefinition::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(QuestEventDefinition::id));
        return List.copyOf(copy);
    }

    public static synchronized Collection<String> ids() {
        return List.copyOf(EVENTS.keySet());
    }

    public static synchronized void loadIntegrationEntrypoints() {
        if (integrationsLoaded) return;
        integrationsLoaded = true;

        List<EntrypointContainer<FlintQuestIntegration>> containers = FabricLoader.getInstance()
                .getEntrypointContainers(FlintQuestAPI.ENTRYPOINT_KEY, FlintQuestIntegration.class);
        for (EntrypointContainer<FlintQuestIntegration> container : containers) {
            ModContainer provider = container.getProvider();
            String providerId = provider.getMetadata().getId();
            String providerName = provider.getMetadata().getName();
            QuestEventRegistrar registrar = definition -> register(definition, providerId, providerName);
            try {
                container.getEntrypoint().register(registrar);
            } catch (Throwable throwable) {
                FlintQuests.LOGGER.error("Flint Quests integration entrypoint from {} failed while registering events.", providerId, throwable);
            }
        }
        FlintQuests.LOGGER.info("Flint Quests event discovery loaded {} registered custom event(s) from {} integration entrypoint(s).",
                EVENTS.size(), containers.size());
    }

    private static String inferProviderId(String eventId) {
        Identifier id = Identifier.tryParse(eventId == null ? "" : eventId.trim());
        return id == null ? "" : id.getNamespace();
    }

    private static String inferProviderName(String providerId) {
        if (providerId == null || providerId.isBlank()) return "";
        return FabricLoader.getInstance().getModContainer(providerId)
                .map(container -> container.getMetadata().getName())
                .orElse(providerId);
    }

    private static String providerLabel(QuestEventDefinition definition) {
        if (!definition.providerName().isBlank()) return definition.providerName();
        if (!definition.providerId().isBlank()) return definition.providerId();
        return "unknown provider";
    }
}
