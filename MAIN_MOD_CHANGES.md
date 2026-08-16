# Project Flint Main-Mod Changes — Flint Quests v1.1.0

## Does Project Flint have to change?

**Existing Project Flint custom-event triggers continue to work without changes.** `FlintQuestHooks` is retained as a compatibility facade.

However, to make Project Flint's custom events **searchable/discoverable in the Flint Quests editor**, Project Flint should register event metadata through the new general v1.1 API. This is the same integration path every other mod uses; Flint Quests contains no Project-Flint-specific event catalog.

## Recommended Project Flint integration

Add an optional Flint Quests entrypoint in Project Flint's `fabric.mod.json`:

```json
"entrypoints": {
  "flintquests": [
    "com.projectflint.integration.ProjectFlintQuestsIntegration"
  ]
},
"suggests": {
  "flintquests": "*"
}
```

If Project Flint already has an `entrypoints` object, merge the `flintquests` key into it rather than replacing the existing keys.

Create a small isolated integration class:

```java
package com.projectflint.integration;

import com.projectflint.flintquests.api.FlintQuestIntegration;
import com.projectflint.flintquests.api.QuestEventDefinition;
import com.projectflint.flintquests.api.QuestEventRegistrar;

public final class ProjectFlintQuestsIntegration implements FlintQuestIntegration {
    @Override
    public void register(QuestEventRegistrar registrar) {
        registrar.registerEvent(QuestEventDefinition.builder(
                        "projectflint:formed_lumber_processor",
                        "Form Lumber Processor")
                .description("Triggered when the Lumber Processor structure successfully forms.")
                .group("Lumber Processing")
                .icon("projectflint:bound_wood_processor")
                .build());
    }
}
```

Register one metadata definition for each Project Flint-specific event that should be discoverable in the editor.

## Trigger calls

New code should prefer:

```java
FlintQuestAPI.trigger(serverPlayer, "projectflint:formed_lumber_processor");
```

Existing calls remain valid:

```java
FlintQuestHooks.trigger(serverPlayer, "projectflint:formed_lumber_processor");
```

Only fire an event from the successful **server-side** gameplay path.

## Dependency rule

Keep Flint Quests optional while the mods remain separate. Project Flint should use Flint Quests as a compile-only/optional integration and should not add it as a hard runtime `depends` requirement merely for event discovery.

If Flint Quests is absent, Project Flint must still launch normally.

## What Project Flint does NOT need

Project Flint does not need to implement quest progress storage, reward claiming, generic item/block tasks, editor UI, categories, themes, or quest networking. Those remain Flint Quests responsibilities.
