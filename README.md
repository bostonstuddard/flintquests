# Flint Quests

Flint Quests is a lightweight, developer-friendly Fabric quest framework for any mod or modpack that wants an in-game quest editor and player-facing quest book. It was designed with Project Flint's progression needs in mind, but the core framework contains no Project Flint-specific gameplay logic. Integrations happen through a public, optional API.

## Target environment

- Minecraft 1.21.11
- Fabric Loader 0.19.3
- Fabric API 0.141.6+1.21.11
- Java 21
- Fabric Loom 1.14.10
- Gradle 9.2.1
- Mojang mappings

## Flint Quests v1.1.0 features

- `J` opens the Flint Quests book.
- FTB-Quests-inspired item-icon quest canvas.
- Compact, collapsible and nested icon-backed category sidebar.
- Bounded wheel panning plus click-and-drag canvas panning.
- Direct editor shortcuts: Shift-click linking and Alt-drag node positioning.
- In-game quest/category editing behind the editing toggle.
- Searchable item/block/icon/category/dependency pickers. Item/block search matches both display names and registry IDs.
- Stable fixed `flintquests:` namespace with safe editable quest-ID paths and progress/dependency migration on rename.
- Multiple tasks, ALL/ANY rules, optional tasks and prerequisite quests.
- Optional claimable item rewards with multiple reward stacks per quest, server-authoritative claim state, per-quest **Claim Reward**, and category-wide **Claim All**.
- Ctrl + mouse-wheel quest-canvas zoom scales node shapes, borders, spacing, dependency anchors, hitboxes, and item icons together.
- Task types: `OBTAIN_ITEM`, `BREAK_BLOCK`, `USE_ITEM`, `INTERACT_BLOCK`, `CUSTOM_EVENT`, and clickable `CHECKMARK`.
- Per-player server-authoritative progress in the world save.
- Client progress synchronization for completed quest/task visualization.
- Quest node state borders: unlocked = glowing yellow, completed = glowing lime, visible-but-locked = gray; hidden locked quests stay hidden.
- Quest completion uses a vanilla advancement-style challenge toast with the quest icon/title and challenge-complete sound instead of a Flint Quests chat message.
- Optional Mod Menu config screen.
- Public integration API: `FlintQuestAPI`, searchable custom-event registration, optional `flintquests` Fabric integration entrypoint, read-only progress queries, and lifecycle callbacks.
- Existing `FlintQuestHooks` integrations remain source-compatible as a deprecated 1.0 compatibility facade.
- Validation and administrative commands.

## Developer API

Flint Quests 1.1 adds a discoverable custom-event registry. A mod can register friendly metadata once and then trigger the same event ID whenever the gameplay action succeeds. Registered events become searchable from the in-game `CUSTOM_EVENT` task picker, grouped/sorted by provider and optional event group. Unregistered namespaced IDs still work when entered manually.

Recommended optional integration uses the Fabric entrypoint key `flintquests`:

```json
"entrypoints": {
  "flintquests": [
    "com.example.mymod.MyFlintQuestsIntegration"
  ]
},
"suggests": {
  "flintquests": "*"
}
```

```java
public final class MyFlintQuestsIntegration implements FlintQuestIntegration {
    @Override
    public void register(QuestEventRegistrar registrar) {
        registrar.registerEvent(QuestEventDefinition.builder(
                        "mymod:activated_machine",
                        "Activate Machine")
                .description("Triggered when the player successfully activates the machine.")
                .group("Machines")
                .icon("mymod:machine")
                .tag("activation")
                .build());
    }
}
```

At the actual successful server-side gameplay point:

```java
FlintQuestAPI.trigger(player, "mymod:activated_machine");
```

See `docs/API.md` for the full contract, optional-dependency pattern, query methods and lifecycle callbacks.

## Editing configuration

When Mod Menu is installed, use **Mods -> Flint Quests -> Config**. Flint Quests does not require Mod Menu at runtime.

Current presentation settings include:

- Quest Editing: ON/OFF
- Quest Completion Popups: ON/OFF

The second setting retains the original internal config field name for backwards compatibility but now controls the completion toast rather than a chat announcement.

## Progress and quest data

Quest definitions:

`config/flintquests/quests/`

Category metadata:

`config/flintquests/categories/`

Per-player progress:

`<world>/flintquests/playerdata/<uuid>.json`

## Project Flint integration

Project Flint uses the exact same public API as any other mod. Existing v1.0 `FlintQuestHooks.trigger(...)` calls remain compatible. To make Project Flint-specific events searchable in the v1.1 editor, Project Flint should register friendly event metadata through the optional `flintquests` integration entrypoint. See `MAIN_MOD_CHANGES.md`.

## Metadata

- Developer: **ImKas**
- License: **Flint Quests Custom License**
  - Allowed: decompiling/reading the code, using the API, and bundling/shading the unmodified official `.jar` inside another mod distribution.
  - Forbidden: copying, modifying, or re-uploading the Flint Quests source code or assets as a standalone project under another name.

## Build

Run `build.bat`.

The builder uses an existing wrapper when present or cached Gradle 9.2.1 under `.gradle-dist/`; otherwise it downloads and verifies the official Gradle distribution. It opens `build\libs` and automatically closes on success, while failed builds remain open so compiler output can be copied.

Expected output:

`build/libs/flint-quests-1.1.0.jar`

See `FLINT_QUESTS.md` for the long-term design contract.


## v0.1.9-a editor shortcuts

When quest editing is enabled on the quest canvas:

- **Shift + click quest A, then Shift + click quest B**: make B depend on A.
- **Alt + drag a quest node**: move the quest on the canvas and snap it to the quest grid.
- **Left click**: edit a quest.
- **Right click**: preview the normal player-facing quest page.

Node borders communicate state: unlocked = glowing yellow, completed = glowing lime, visible-but-locked = gray. Hidden locked quests remain hidden.

Manual CHECKMARK tasks use an internal Fabric packet and are server-validated; they no longer execute `/flintquests check` commands.

Quest IDs keep the fixed `flintquests:` namespace, but the path can now be safely renamed. Renames rewrite dependencies and migrate existing player progress.


### v0.1.14-a editor notes

Quest descriptions/lore now use a real multiline editor. Press **Enter** in the description field to create a new line. Node hover lore renders those lines separately instead of flattening everything into one tooltip row. Existing categories can also be deleted from the category editor; child categories and assigned quests are migrated safely.


### v0.1.15-a responsive editor + node shapes

The quest editor is now split into compact **Quest / Look / Flow / Rules / Task** pages instead of rendering two large form columns at once. This prevents button/label overlap and makes the editor usable at smaller GUI/window sizes. Quest hover controls are one action per line. The Look page also adds saved node-shape selection: Square, Circle, Hexagon, or Diamond.


## Author text formatting

Player-facing quest titles, descriptions, task text, and category names support Minecraft legacy formatting codes. Both `&` and `§` prefixes work, for example `&aGreen`, `&6&lGold Bold`, and `&rReset`. The editor intentionally displays the raw codes so authors can edit them directly.

When Developer Environment is enabled, the settings screen also provides **Build Nestable Quest-Pack .jar** and **Open Built Jars** for the `flintquests-exports/` folder.


### v0.1.18-a quest-detail pagination

Player-facing quest pages now paginate automatically when descriptions/tasks exceed the available content area. Manual CHECKMARK buttons are positioned by the same content flow, so they stay attached to their task instead of floating independently. Deleted quest IDs and removed task IDs are also pruned from saved player progress during synchronization. Diamond nodes now render as a proper filled diamond.

## Themes and collaboration

Flint Quests supports JSON color themes in `config/flintquests/themes/`. The settings screen can cycle installed themes and open the theme directory. Nestable player quest-pack JARs embed the active theme. See `docs/THEMES.md`.

With Developer Environment enabled, authors can export/import editable quest project ZIPs from the settings screen. These ZIPs contain quests, categories, quest-ID migrations and the active theme, but never player progress. Imports create an automatic backup first. See `docs/QUEST_DATA_ZIP.md`.


## Quest canvas controls

- Mouse drag on empty canvas: pan.
- Mouse wheel: vertical pan.
- Horizontal wheel/trackpad: horizontal pan.
- **Control + mouse wheel:** zoom in/out around the mouse cursor.
- In Developer Environment, the configurable editing-toggle shortcut works even while Flint Quests editor screens are open.


## Built-in themes

Flint Quests creates several editable baseline themes in `config/flintquests/themes/`: Default, Light, Dark, AMOLED, Red, Crimson, Brown, and Vanilla Minecraft. Additional JSON themes can be added beside them.

## Quest-data collaboration ZIPs

Developer Environment users can export/import editable quest-data ZIPs. Import starts in the user's Downloads folder and validates the Flint Quests marker/schema before replacing the active quest project. See `docs/QUEST_DATA_ZIP.md`.


## Rewards (v1.0)

Rewards are optional. A quest may define no rewards or one/more item rewards. Once the quest is complete, the player can claim the reward from the quest detail page. Reward claims are server-authoritative and saved per player.

The currently viewed category also exposes **Claim All** whenever that category contains rewards. Claim All only processes quests that are complete, have rewards, and have not already been claimed. Inventory overflow is dropped at the player rather than discarded.

Quest authors configure rewards from the quest editor's **Reward** tab using the searchable item picker and amount field.
