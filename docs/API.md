# Flint Quests 1.1 Developer API

Flint Quests is a general quest framework. Project Flint influenced its requirements, but the API is intentionally mod-agnostic: **Flint Quests does not need to know another mod's implementation classes in order to quest against that mod.**

## API goals

- integrations remain optional;
- a mod can expose its custom progression actions to the editor without modifying Flint Quests;
- quest authors can search friendly event names instead of memorizing arbitrary strings;
- manually entered custom event IDs remain supported;
- the server stays authoritative for quest progress;
- API consumers receive read-only progress views rather than mutable save objects;
- the old 1.0 `FlintQuestHooks` surface remains compatible.

## Recommended integration: optional Fabric entrypoint

Add Flint Quests as a compile-only development dependency in your mod project, then declare an optional integration entrypoint:

```json
{
  "entrypoints": {
    "flintquests": [
      "com.example.mymod.MyFlintQuestsIntegration"
    ]
  },
  "suggests": {
    "flintquests": "*"
  }
}
```

The `flintquests` entrypoint is queried by Flint Quests itself. If Flint Quests is absent, Fabric does not ask for this entrypoint, so it is suitable for an optional integration. Keep Flint-Quests-specific classes isolated from code that must load without Flint Quests.

```java
package com.example.mymod;

import com.projectflint.flintquests.api.FlintQuestIntegration;
import com.projectflint.flintquests.api.QuestEventDefinition;
import com.projectflint.flintquests.api.QuestEventRegistrar;

public final class MyFlintQuestsIntegration implements FlintQuestIntegration {
    @Override
    public void register(QuestEventRegistrar registrar) {
        registrar.registerEvent(QuestEventDefinition.builder(
                        "mymod:activated_machine",
                        "Activate Machine")
                .description("Triggered after the machine successfully activates.")
                .group("Machines")
                .icon("mymod:machine")
                .tag("activation")
                .tag("machine")
                .build());
    }
}
```

The owning Fabric mod's id/name are automatically used as provider metadata when `provider(...)` is omitted.

## Triggering an event

Trigger events from the successful **server-side** code path:

```java
FlintQuestAPI.trigger(player, "mymod:activated_machine");
```

For countable actions:

```java
FlintQuestAPI.trigger(player, "mymod:processed_item", amountProcessed);
```

`Identifier` overloads are also available.

The event ID must be a namespaced identifier such as `mymod:activated_machine`.

### Registration is discovery metadata, not a permission check

A `CUSTOM_EVENT` quest can still target an unregistered ID if the author enters it manually. Likewise, `trigger(...)` does not require prior registration. Registration adds:

- friendly title;
- description;
- provider/mod grouping;
- optional subgroup;
- optional item icon;
- extra search tags;
- in-editor search/discovery.

This keeps simple integrations simple and prevents Flint Quests from becoming a hardcoded catalog of other mods.

## Direct registration

Mods that do not want the entrypoint pattern may register directly during initialization:

```java
FlintQuestAPI.registerEvent(
    QuestEventDefinition.builder("mymod:formed_structure", "Form Structure")
        .description("Triggered when structure validation succeeds.")
        .group("Structures")
        .build()
);
```

When direct registration omits provider metadata, Flint Quests infers the provider from the event namespace when possible.

## Event metadata builder

```java
QuestEventDefinition.builder("mymod:event_id", "Friendly Name")
    .description("What causes this event to fire.")
    .group("Optional Group")
    .icon("minecraft:diamond")
    .tag("extra search term")
    .provider("mymod", "My Mod") // normally unnecessary with the entrypoint registrar
    .build();
```

Conflicting duplicate registrations keep the first definition and log a warning rather than unpredictably replacing another mod's metadata.

## Searching registered events in the editor

For a `CUSTOM_EVENT` task, **Search** opens the registered-event picker. Entries are sorted by provider, group and title and can be searched by:

- event ID;
- title;
- description;
- provider id/name;
- group;
- tags.

Hovering a result shows its provider, event ID and description.

## Read-only progress queries

```java
boolean done = FlintQuestAPI.isQuestComplete(player, "flintquests:my_quest");
boolean taskDone = FlintQuestAPI.isTaskComplete(player, "flintquests:my_quest", "task_0");
int value = FlintQuestAPI.getTaskProgressValue(player, "flintquests:my_quest", "task_0");
boolean rewardClaimed = FlintQuestAPI.isRewardClaimed(player, "flintquests:my_quest");
```

For a snapshot:

```java
Optional<FlintQuestAPI.TaskProgressSnapshot> progress =
    FlintQuestAPI.getTaskProgress(player, "flintquests:my_quest", "task_0");
```

Snapshots are immutable so integrations cannot accidentally corrupt Flint Quests save state.

## Lifecycle callbacks

Mods can react without polling:

```java
FlintQuestEvents.QUEST_COMPLETED.register((player, questId) -> {
    // react to completion
});

FlintQuestEvents.TASK_PROGRESS_CHANGED.register((player, questId, taskId, value, complete) -> {
    // react to task progress
});

FlintQuestEvents.REWARD_CLAIMED.register((player, questId) -> {
    // react after the reward has been granted
});
```

These callbacks are server-side.

## 1.0 compatibility

`FlintQuestHooks` remains available and delegates to the 1.x engine. Existing code such as:

```java
FlintQuestHooks.trigger(player, "mymod:event");
```

continues to work. New integrations should use `FlintQuestAPI`.

## Naming rules

- Use your own mod namespace: `mymod:action_name`.
- Name an event after a successful gameplay fact, not an input attempt.
- Trigger only after the action actually succeeds.
- Prefer one reusable event with a count over many numbered variants.
- Do not create custom events for generic facts Flint Quests already detects with `OBTAIN_ITEM`, `BREAK_BLOCK`, `USE_ITEM`, or `INTERACT_BLOCK`.

Good examples:

- `mymod:formed_multiblock`
- `mymod:completed_machine_recipe`
- `mymod:filled_special_container`

Avoid events that merely duplicate possession of an ordinary registered item.

## API stability

`com.projectflint.flintquests.api` is the supported public integration package. Flint Quests will attempt to keep source compatibility within the 1.x line. Implementation packages such as `engine`, `progress`, `client`, and `data` are not the public API contract unless explicitly documented otherwise.
