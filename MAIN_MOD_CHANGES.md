# Project Flint Main-Mod Changes — Flint Quests v0.1.10-a

## Required Project Flint changes for this patch

**None.**

v0.1.10-a only changes Flint Quests client/editor presentation and input handling. Project Flint can remain completely untouched.

## What changed inside Flint Quests

- quest-editor fields and behavior buttons now use a responsive vertical layout so the bottom controls remain on-screen;
- Alt + left-drag node movement was moved out of normal quest button handling and now tracks Alt directly;
- the category sidebar now slides in when the pointer reaches the left edge and retracts when the pointer leaves;
- the sidebar overlays the canvas instead of changing the quest-canvas width.

None of these features require Project Flint hooks.

## Existing future integration contract

Project Flint only needs code changes later when its custom mechanics should directly advance Flint Quests tasks.

```java
FlintQuestHooks.trigger(serverPlayer, "projectflint:formed_lumber_processor");
```

Counted form:

```java
FlintQuestHooks.trigger(serverPlayer, "projectflint:processed_wood", 2);
```

When that integration pass begins, hooks should fire only from the successful server-side completion path of the relevant Project Flint mechanic.
