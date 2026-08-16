package com.projectflint.flintquests.api;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Human-readable metadata for a custom event that can be targeted by a Flint Quests CUSTOM_EVENT task.
 *
 * <p>Registration is optional for execution: an unregistered event ID may still be triggered and manually
 * entered into a quest. Registration exists so editors can discover, search and understand an integration.</p>
 */
public final class QuestEventDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final String providerId;
    private final String providerName;
    private final String group;
    private final String icon;
    private final List<String> tags;

    private QuestEventDefinition(Builder builder) {
        String rawId = normalize(builder.id);
        Identifier parsed = Identifier.tryParse(rawId);
        if (parsed == null || !rawId.contains(":") || parsed.getPath().isBlank()) {
            throw new IllegalArgumentException("Flint Quests event IDs must be valid namespaced IDs (example: mymod:activated_machine): " + builder.id);
        }
        this.id = parsed.toString();
        this.title = normalize(builder.title).isBlank() ? humanize(parsed.getPath()) : normalize(builder.title);
        this.description = normalize(builder.description);
        this.providerId = normalize(builder.providerId).toLowerCase(Locale.ROOT);
        this.providerName = normalize(builder.providerName);
        this.group = normalize(builder.group);
        this.icon = normalize(builder.icon);
        List<String> cleanedTags = new ArrayList<>();
        for (String tag : builder.tags) {
            String cleaned = normalize(tag);
            if (!cleaned.isBlank() && !cleanedTags.contains(cleaned)) cleanedTags.add(cleaned);
        }
        this.tags = Collections.unmodifiableList(cleanedTags);
    }

    public static Builder builder(String id, String title) {
        return new Builder(id, title);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** Mod id responsible for this event when known. */
    public String providerId() {
        return providerId;
    }

    /** Friendly mod/provider name shown in the editor. */
    public String providerName() {
        return providerName;
    }

    /** Optional editor grouping such as "Machines" or "Progression". */
    public String group() {
        return group;
    }

    /** Optional item identifier used as the event icon in editor search results. */
    public String icon() {
        return icon;
    }

    /** Optional extra search terms. */
    public List<String> tags() {
        return tags;
    }

    QuestEventDefinition withProviderDefaults(String fallbackProviderId, String fallbackProviderName) {
        if (!providerId.isBlank() && !providerName.isBlank()) return this;
        Builder builder = copyBuilder();
        if (providerId.isBlank()) builder.provider(fallbackProviderId, providerName.isBlank() ? fallbackProviderName : providerName);
        else if (providerName.isBlank()) builder.provider(providerId, fallbackProviderName);
        return builder.build();
    }

    private Builder copyBuilder() {
        return new Builder(id, title)
                .description(description)
                .provider(providerId, providerName)
                .group(group)
                .icon(icon)
                .tags(tags);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String humanize(String path) {
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1));
        }
        return result.isEmpty() ? path : result.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof QuestEventDefinition other)) return false;
        return id.equals(other.id)
                && title.equals(other.title)
                && description.equals(other.description)
                && providerId.equals(other.providerId)
                && providerName.equals(other.providerName)
                && group.equals(other.group)
                && icon.equals(other.icon)
                && tags.equals(other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, providerId, providerName, group, icon, tags);
    }

    public static final class Builder {
        private final String id;
        private final String title;
        private String description = "";
        private String providerId = "";
        private String providerName = "";
        private String group = "";
        private String icon = "";
        private final List<String> tags = new ArrayList<>();

        private Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder provider(String modId, String displayName) {
            this.providerId = modId;
            this.providerName = displayName;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder icon(String itemId) {
            this.icon = itemId;
            return this;
        }

        public Builder tag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder tags(List<String> tags) {
            if (tags != null) this.tags.addAll(tags);
            return this;
        }

        public QuestEventDefinition build() {
            return new QuestEventDefinition(this);
        }
    }
}
