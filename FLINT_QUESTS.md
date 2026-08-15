# Flint Quests — Premise, Sources, Architecture, and Continuation Notes

This file is the persistent design document for Flint Quests. Future work should read this document before changing the quest format or integration API.

## Premise

Project Flint needs an explicit, quest-heavy progression guide that can tell the player exactly what to do while still supporting Project Flint mechanics that vanilla advancements do not naturally understand.

Flint Quests exists to provide that system as a small, Project-Flint-focused companion mod first. Once the data format, editor, networking and player experience are stable, the intention is to merge the package cleanly into the main Project Flint mod.

The core idea is inspired by the workflow of FTB Quests:

- a visual quest book;
- quests grouped into categories, with categories optionally nested under expandable groups;
- multiple conditions/tasks per quest;
- dependencies between quests;
- editor mode available in game;
- normal players see a read-only progression book;
- quest definitions are separate from player progress;
- modpack/mod-specific systems can provide custom task hooks.

Flint Quests is not intended to copy FTB Quests source code or reproduce every feature. It is an independent, much smaller implementation specialized for Project Flint.

## Primary design rules

1. **Quest definitions and player progress must remain separate.**
   Editing wording, layout or dependencies must not inherently rewrite a player's save data.

2. **Quest IDs are stable API identifiers, but the editor must support safe renames.**
   The `flintquests:` namespace is fixed. The path may be changed deliberately through the editor, but the rename must rewrite prerequisite references and preserve existing player progress through an ID-migration alias.

3. **The JSON format should remain readable and migration-friendly.**
   Do not serialize arbitrary Java class names or implementation details into quest files.

4. **Project Flint integration should use a tiny public hook surface.**
   The preferred integration is `FlintQuestHooks.trigger(player, eventId, amount)` rather than Flint Quests reaching into Project Flint classes.

5. **Editing is a development feature.**
   It must be possible to disable editing completely in config for release builds.

6. **The server is the authority for progress.**
   Client UI may display and edit definitions during development, but completion must never rely on a client claiming a task is complete.

7. **Generic tasks should not require changes to Project Flint.**
   Item possession, block breaking, item use and similar generic Minecraft behavior should be detected by Flint Quests itself.

8. **Project-Flint-specific mechanics should use custom events.**
   Examples: forming the Lumber Processor, completing a Sanding Station recipe, filling the Water Pot, successfully lighting a permanent campfire, completing a grinder operation, or finishing another custom multiblock/process.

9. **Flint Quests is an API framework, not a Project Flint hardcode layer.**
   Any installed mod should be able to use the public event/task-registration surface without editing Flint Quests source. Project Flint is the primary consumer, not a special-case dependency baked into the engine.

10. **Quest IDs are always in the fixed `flintquests:` namespace.**
    The editor exposes only the path. Existing quest paths may be renamed, but never by simply saving a second unrelated ID: Flint Quests must use the safe rename path, rewrite dependencies, and preserve progress via `quest_id_migrations.json`.


## Editor canvas interaction rules (v0.1.10-a+)

Editor-mode canvas controls should favor direct manipulation instead of forcing every change through a form:

- **Left-click quest:** open the quest editor.
- **Right-click quest:** preview the normal player-facing quest page.
- **Shift-click quest A, then Shift-click quest B:** create `A -> B`, meaning B gains A as a prerequisite. The editor must reject a link that would create a dependency cycle.
- **Alt-drag a quest node:** reposition that quest on the canvas and snap to the quest grid. Save the new node coordinates when the drag ends.
- **Drag empty canvas:** pan the quest view.

The node border is a player-readable state indicator and should remain consistent:

- **glowing yellow:** quest is unlocked/available;
- **glowing lime:** quest is completed;
- **gray:** quest is visible but still locked by prerequisites;
- quests configured as hidden-until-ready are not rendered while locked.

Do not add a tiny unicode completion badge back onto the main quest node. The state border is the completion indicator because it is cleaner and does not fight the item icon.

The selected category should use a complete row outline/highlight, not a narrow single-pixel selection stripe.

### Hover sidebar rule

The category panel is not a permanent width reservation and should not require a manual open/close arrow. The quest canvas keeps its full width. Moving the pointer to the left edge reveals the category panel with a short slide animation; keeping the pointer over the revealed panel keeps it open; moving away retracts it. Nested-category expansion state remains persistent.

The sidebar animation must move the actual category controls and icons together, not merely animate a decorative background over stationary/invisible widgets.

### Responsive editor rule

The quest editor must be laid out from the current scaled GUI height. Do not place lower behavior buttons at hard-coded Y positions that can collide with the Save/Cancel/Delete row on smaller GUI scales. Prefer compact labeled rows and remove redundant explanatory copy before allowing controls to go off-screen.

### Modifier drag reliability rule

Quest nodes should not rely on a normal `Button` consuming the same left-click used for editor gestures. Canvas node clicks are handled at the screen level so Shift-link and Alt-drag can take priority. Alt state is tracked from keyboard events as well as mouse modifier flags so **Alt + left-drag** remains reliable on the Minecraft 1.21.11 input path.

## Manual CHECKMARK security rule

A player-facing manual checkmark is a UI action, not a chat/command action. Flint Quests must **not** implement it by making the client execute `/flintquests check ...`.

The client sends the dedicated Flint Quests checkmark C2S payload containing only the quest/task IDs. The server then verifies:

- the quest exists;
- its prerequisites are satisfied;
- the requested task exists;
- that task is actually `CHECKMARK`;
- the task is not already complete.

Only that manual CHECKMARK task can be advanced by this packet. Generic item/block/custom-event tasks remain server-observed and cannot be completed through this UI request. The public player `check` command should remain absent.

## Quest ID rename rule

The literal `flintquests:` namespace is fixed and must never be editable. The path after it is editable even for an existing quest.

When an existing quest is renamed:

1. rewrite other quest dependency references from the old ID to the new ID;
2. rename the quest definition file;
3. record the old -> new mapping in `config/flintquests/quest_id_migrations.json`;
4. when player progress is loaded, migrate progress stored under the old quest ID to the resolved new ID;
5. merge progress rather than discarding it if both IDs somehow exist in a player save.

Deleting a quest should also remove that quest from other quests' prerequisite lists so the editor does not immediately leave missing-dependency errors behind.

## Minecraft 1.21.11 GUI compatibility rule

Current Flint Quests screens must **not** manually call `Screen#renderBackground(...)` from their `render(...)` overrides. In the tested Minecraft 1.21.11 client rendering path, the background/blur pass is already handled for the screen; requesting it again can throw `IllegalStateException: Can only blur once per frame`.

This applies to the current:

- `QuestBookScreen`;
- `QuestDetailScreen`;
- `QuestEditorScreen`.

When adding future screens, keep the normal widget rendering via `super.render(...)` and only add custom foreground/panel drawing needed by Flint Quests. If a future Minecraft version changes this behavior, retest before changing this invariant.

## Sources and architectural references

These sources are references for concepts, Fabric APIs, or build tooling. Flint Quests source code is independently written.

### FTB Quests

- CurseForge project supplied as the initial reference by the Project Flint developer:
  - https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge
- FTB Quests source repository:
  - https://github.com/FTBTeam/FTB-Quests
- FTB documentation for quests/editor concepts:
  - https://docs.feed-the-beast.com/mod-docs/mods/suite/Quests/Developer/Quests/
- FTB quest settings/dependency concepts:
  - https://docs.feed-the-beast.com/mod-docs/mods/suite/Quests/Developer/Quests/Settings
- Current FTB Quests UI/source structure used only as a conceptual reference for this redesign:
  - https://github.com/FTBTeam/FTB-Quests/blob/main/common/src/main/java/dev/ftb/mods/ftbquests/client/gui/quests/QuestPanel.java
  - https://github.com/FTBTeam/FTB-Quests/blob/main/common/src/main/java/dev/ftb/mods/ftbquests/client/gui/quests/QuestButton.java
  - https://github.com/FTBTeam/FTB-Quests/blob/main/common/src/main/java/dev/ftb/mods/ftbquests/client/gui/quests/ChapterPanel.java
  - https://github.com/FTBTeam/FTB-Quests/blob/main/common/src/main/java/dev/ftb/mods/ftbquests/quest/ChapterGroup.java

Ideas taken at the conceptual level include chapters, dependency-driven progression, task-based completion, a development editor and a separate player-facing quest book.

### Fabric

- Fabric developer documentation:
  - https://docs.fabricmc.net/develop/
- Fabric events:
  - https://docs.fabricmc.net/develop/events
- Fabric commands:
  - https://docs.fabricmc.net/develop/commands/basics
- Fabric custom screens:
  - https://docs.fabricmc.net/develop/rendering/gui/custom-screens
- Fabric networking used for server-authoritative progress/completion synchronization:
  - https://docs.fabricmc.net/develop/networking
- Fabric 1.21.11 networking reference sources used to verify custom payload registration/send/receive patterns:
  - https://github.com/FabricMC/fabric-docs/tree/main/reference/1.21.11/src/main/java/com/example/docs/networking/basic
  - https://github.com/FabricMC/fabric-docs/tree/main/reference/1.21.11/src/client/java/com/example/docs/network/basic
- Fabric example mod:
  - https://github.com/FabricMC/fabric-example-mod
- Fabric API release used by Project Flint / this source baseline:
  - https://github.com/FabricMC/fabric-api/releases
- Fabric Maven:
  - https://maven.fabricmc.net/


### Mod Menu

- Mod Menu repository / API documentation:
  - https://github.com/TerraformersMC/ModMenu
- Terraformers Maven used only for the compile-time API dependency:
  - https://maven.terraformersmc.com/releases/

Flint Quests treats Mod Menu as an optional presentation/integration layer. The `modmenu` entrypoint supplies `FlintQuestConfigScreen` through `ModMenuApi#getModConfigScreenFactory()`. Do not make Mod Menu a required Fabric dependency merely to expose configuration.

### Minecraft mappings reference

- Minecraft 1.21.11 Mojang/intermediary/Yarn cross-reference used to verify current class and GUI naming:
  - https://mappings.dev/1.21.11/
- Yarn 1.21.11 advancement-toast mappings used to verify the vanilla toast carrier/API shape:
  - https://github.com/FabricMC/yarn/blob/1.21.11/mappings/net/minecraft/client/toast/AdvancementToast.mapping
  - https://github.com/FabricMC/yarn/blob/1.21.11/mappings/net/minecraft/advancement/Advancement.mapping

### Gradle

- Official Gradle distribution used by `build.bat`:
  - https://services.gradle.org/distributions/gradle-9.2.1-bin.zip



## Ownership / release metadata

Standalone Flint Quests releases identify **ImKas** as the developer and use an **All Rights Reserved** license. Do not change Flint Quests back to MIT or another permissive license unless ImKas explicitly requests that change.

## Branding / icon source

During standalone development, Flint Quests intentionally reuses Project Flint's existing mod icon instead of creating a second temporary brand asset. The source of truth is Project Flint's `assets/projectflint/icon.png`; Flint Quests carries an exact copy at `assets/flintquests/icon.png`. If Project Flint changes its icon, review Flint Quests in the same branding pass.


## Quest-book UI direction (v0.1.4-a+)

The player-facing quest book should follow the successful interaction model of FTB Quests without copying its source or art. The important UX concepts are:

- quests are **small visual nodes**, not full-width text buttons;
- each quest node renders the configured Minecraft item icon;
- dependencies are visible as connector lines on the canvas;
- the left side contains the progression/category navigation;
- the category panel can be collapsed to give the quest canvas more space;
- categories can belong to expandable/collapsible groups;
- scrolling over the canvas pans the view;
- editor mode uses the same canvas instead of switching to a separate list-oriented quest browser.

### Category/group compatibility convention

Do not replace the serialized `chapter` field merely to rename it. To avoid a quest-file migration, Flint Quests v0.1.4-a interprets it as a category path:

- `Introduction` -> ungrouped category `Introduction`;
- `Stone Age/Getting Started` -> group `Stone Age`, category `Getting Started`;
- `Wood Age/Processing` -> group `Wood Age`, category `Processing`.

The final path segment is the category name. Everything before the final `/` is the group label. This permits descriptive group names while keeping all v0.1.x quest files readable.

The next UI improvements should build on this canvas rather than returning to paged full-width quest buttons. Progress/status coloring, compact category metadata controls, player preview, drag-to-pan, direct Shift-linking, and Alt-drag node positioning are now implemented. Remaining improvements include zoom, richer node shapes/context actions, multi-select/bulk editing, and additional editor polish.


## Windows builder behavior

`build.bat` should retain the Project Flint-style console UI, Java/project checks, cached Gradle bootstrap, checksum verification, and automatic `build\libs` opening. On a **successful** build the batch file should close automatically. On a **failed** build it should pause so the compiler/Gradle error remains visible for copying.

## Current quest definition model

A quest contains:

- stable `id`;
- `chapter` (retained as the serialized field name, now interpreted as a category path);
- player-facing `title`;
- `description`;
- item registry ID used as `icon` metadata;
- editor `x` and `y` coordinates;
- dependency list;
- dependency completion mode (`ALL` or `ANY`);
- task completion mode (`ALL` or `ANY`);
- zero or more tasks.

A task contains:

- stable task ID within that quest;
- task type;
- target registry/event ID;
- count;
- optional flag.

## Current task types

### OBTAIN_ITEM

State-based. Periodically scans the server player's inventory and marks progress according to the amount currently held.

Target example:

`minecraft:stick`

Future option: allow either "currently possess" or "ever obtained" semantics.

### BREAK_BLOCK

Cumulative event task.

Target example:

`minecraft:stone`

### USE_ITEM

Cumulative event task for using/right-clicking an item.

Target example:

`projectflint:flint_firestarter`

### INTERACT_BLOCK

Cumulative event task for interacting with a target block.

Target example:

`projectflint:sanding_station`

### CUSTOM_EVENT

Integration point for Project Flint systems.

Target examples:

- `projectflint:formed_lumber_processor`
- `projectflint:lit_permanent_campfire`
- `projectflint:filled_water_pot`
- `projectflint:sanding_station_recipe/sanded_wood_slab`

Project Flint should trigger these from the successful server-side code path, not from client input.

### CHECKMARK

Manual acknowledgement task. Player-facing quest details render a real clickable `[ ]` control. Clicking it sends the existing checkmark request to the server; the server validates/completes the task, persists progress, and synchronizes the authoritative result back to the client. Completed checkmarks render as `[✓]` and become inactive.

## Player progress model

Progress is stored per player UUID inside the world save:

`<world>/flintquests/playerdata/<uuid>.json`

Each quest progress record stores:

- completion state;
- completion timestamp;
- progress by stable task ID.

This deliberately prevents global config edits from becoming player-save edits.

## Configuration

Generated at:

`config/flintquests/flintquests.json`

Current fields:

- `questEditing`
- `questEditingRequiresOperator`
- `inventoryScanIntervalTicks`
- `announceQuestCompletion`

`questEditingRequiresOperator` is part of the intended security contract. In v0.1.0-a, command-side editing controls are permission-gated while the local GUI editor is explicitly a development/singleplayer-oriented tool. Before dedicated-server editor support is considered complete, all GUI writes must become server-authoritative and enforce this value.


### Mod Menu editing toggle

Beginning with v0.1.3-a, when Mod Menu is installed its Flint Quests **Config** button opens a small native Flint Quests settings screen. The screen writes the same existing JSON config used by the rest of the mod; it is not a second configuration system.

The primary development switch is `questEditing`:

- `ON` — quest book exposes creation/editing controls.
- `OFF` — quest book behaves as the player-facing/read-only book.

Mod Menu is optional. The integration is compile-only and the `modmenu` entrypoint is only consumed when Mod Menu exists. Keep a non-Mod-Menu configuration path available so Flint Quests never gains a hard runtime dependency on it.

The Mod Menu settings screen must follow the same Minecraft 1.21.11 GUI rule as the quest screens: do not manually request a second background blur.

## Commands

- `/flintquests`
- `/flintquests list`
- `/flintquests progress`
- `/flintquests check <quest id>`
- `/flintquests reload`
- `/flintquests validate`
- `/flintquests editing <true|false>`
- `/flintquests reset`

Administrative commands are permission-gated.

## Project Flint integration contract

The public class is:

`com.projectflint.flintquests.api.FlintQuestHooks`

Preferred usage from a successful server-side Project Flint action:

```java
FlintQuestHooks.trigger(serverPlayer, "projectflint:formed_lumber_processor");
```

For countable events:

```java
FlintQuestHooks.trigger(serverPlayer, "projectflint:processed_wood", 2);
```

Quest definitions can then use a `CUSTOM_EVENT` task with the same string target.

Keep event IDs stable once released publicly.

## Planned continuation

### v0.2 — finish multiplayer networking and player book authority

Progress/completion synchronization, completed-state rendering, and player-facing checkmark controls are now implemented. Remaining highest-priority networking work is:

- server sends quest/category definitions to clients instead of assuming matching local config files;
- locked/startable state is synchronized/presented consistently on the quest canvas;
- dedicated servers no longer rely on local config access for viewing/editing quest definitions;
- config/editor permissions are enforced server-side;
- editor writes become server-authoritative before dedicated-server editing is considered production-ready.

### v0.3 — finish the canvas editor

v0.1.4-a delivered the first usable canvas foundation ahead of schedule:

- category sidebar;
- expandable category groups;
- collapsible sidebar;
- pannable quest canvas;
- quest nodes using item icons;
- dependency lines;
- category-aware new quest creation.

Remaining v0.3 work:

- drag nodes to reposition;
- right-click blank canvas to create a quest;
- right-click node context actions;
- zoom;
- duplicate/copy/paste quest support;
- validation errors shown directly in editor.

The stored `x` and `y` fields already exist so this work should not require a quest-file migration.

### v0.4 — expanded generic task library

Candidates:

- craft item;
- place block;
- kill entity;
- enter biome;
- enter dimension;
- reach location/radius;
- statistic threshold;
- advancement completion;
- obtain/use fluid;
- observe block/entity;
- optional tasks;
- hidden/secret quests;
- repeatable quests if Project Flint ever needs them.

### v0.5 — Project Flint custom task integration

Add explicit custom events at important Project Flint server-side success points, especially:

- tool assembly;
- permanent campfire creation/lighting;
- kiln construction/process completion;
- grinder operations;
- Sanding Station recipes;
- Water Bearing Pot fill/use;
- Water Pot fill state;
- Lumber Processor formation;
- Lumber Processor processing stages;
- aquifer/well systems when implemented;
- future era transitions.

### v0.6 — migration of Project Flint progression

Convert the existing detailed Project Flint advancement/progression instructions into Flint Quests chapters while retaining the explicit "tell the player exactly what to do" philosophy.

Prefer an automated/import-assisted conversion rather than manually rewriting every entry.

### v1.0 — merge-ready

Before merging into Project Flint:

- networking stable;
- editor permissions safe;
- quest data migrations versioned;
- progress migrations versioned;
- editor comfortable enough for routine development;
- player UI polished;
- Project Flint custom hooks complete;
- no standalone-only assumptions in paths/package layout that would make integration difficult.

At that point the standalone packages can be moved into Project Flint and the external `flintquests` mod dependency removed.

## Things not to do

- Do not replace stable quest IDs just to improve naming.
- Do not store player completion inside the quest definition JSON.
- Do not let clients authoritatively complete tasks.
- Do not hardcode Project Flint implementation classes into the generic quest engine when a custom event can represent the same thing.
- Do not make the editor mandatory in release builds.
- Do not turn the format into an FTB Quests clone if a smaller Project-Flint-specific feature is sufficient.


## Minecraft 1.21.11 source-compatibility notes

- Flint Quests uses Mojang mappings. In the 1.21.11 source set, resource IDs are represented by `net.minecraft.resources.Identifier`; do not reintroduce the older `ResourceLocation` class name into new client/editor code.
- Avoid declaring a private helper named `rebuildWidgets()` on `Screen` subclasses. Minecraft already owns a protected method with that name. Flint Quests uses names such as `refreshQuestWidgets()` for internal widget reconstruction.
- `build.bat` should continue to auto-close only after a successful build; failure paths must remain visible/paused for diagnostics.

## v0.1.6-a UI/category rules

The following are now permanent Flint Quests UI expectations unless intentionally redesigned later:

- Do not display a permanent `PLAYER MODE` label. Player-facing mode should look like the normal quest book, not a debug/development state.
- Do not waste a footer on permanent scroll instructions. Interaction hints belong in contextual tooltips when needed.
- Quest-canvas panning must be bounded by the current category's quest content; never allow infinite empty-space scrolling.
- The category sidebar open/closed state must persist across reopening the quest book and game restarts.
- Nested category expansion state should also persist.
- The category panel should stay compact rather than consuming a large fraction of the screen.
- Categories have their own metadata and are no longer only inferred display strings from quests.
- Category metadata fields are: stable `id`, player-facing `title`, item `icon`, optional `parent`, and integer `order`.
- `parent` allows arbitrary nested category/group trees. A category may contain quests and child categories at the same time.
- Category icons are Minecraft item IDs and use the same item-rendering fallback rules as quest icons.
- Existing quest `chapter` values remain the category reference field for backwards compatibility. A referenced category with no explicit metadata file becomes an implicit runtime category.
- Explicit category files live in `config/flintquests/categories/`.
- In edit mode, primary/left click on a quest edits it; right click opens the normal player-facing quest view for quick testing.
- In edit mode, right click on a category opens its category editor.
- Player-facing quest details must not expose raw dependency IDs. Dependency relationships may still be represented visually by connector lines on the quest canvas.
- FTB Quests remains a UI/workflow reference, not a source to clone wholesale. The Flint Quests implementation should stay smaller and Project-Flint-focused.

## v0.1.7-a editor/usability rules

- The quest canvas must support click-and-drag panning so a normal mouse does not require horizontal wheel hardware or a trackpad.
- Search/picker buttons should be used instead of requiring authors to memorize registry IDs.
- Item/block selectors should search both human-readable names and full registry IDs while still saving the registry ID.
- Quest icons use the same item picker as item task targets.
- The quest editor should clearly distinguish player-facing text from internal IDs/targets.
- `flintquests:` is a fixed quest namespace. Authors edit only the quest path during creation, and existing quest IDs are locked.
- Source patch ZIPs must place changed project files/directories directly at the archive root. Do not wrap them in a version-named parent directory.

## v0.1.8-a progress/completion presentation rules

- Server progress remains authoritative. Client progress is a synchronized presentation cache only.
- Flint Quests uses Fabric play payloads to request/send progress and to announce a newly completed quest to the client.
- Quest completion presentation should not use a Flint Quests chat line. The default presentation is a Minecraft advancement-style toast using the quest title and configured item icon.
- The completion toast currently uses the vanilla challenge advancement frame so Minecraft supplies its standard challenge-complete toast sound automatically.
- The synthetic advancement object used by the toast is a client-side presentation carrier only; completing a Flint Quest does not need to award/create a real persistent vanilla advancement.
- Completed quest nodes should be visibly different on the canvas. The current style is a gold outline plus checkmark badge; completed prerequisite connector lines also turn gold.
- Player-facing quest details show completion state for the quest and individual tasks, but do not expose raw dependency IDs.
- Literal `\n` inside a single `Component.literal(...)` tooltip is not a valid multiline-tooltip strategy for this UI and produced an unwanted glyph in testing. Use separate tooltip components when true multiline text is needed, or use a clear single-line separator.
- Editor labels and controls must have dedicated vertical space; do not place labels on top of text fields/buttons. The v0.1.8-a layout is the minimum clarity baseline for future editor work.
- The existing `announceQuestCompletion` config key is retained for file compatibility, but its player-facing meaning is now **Quest Completion Popups**.

