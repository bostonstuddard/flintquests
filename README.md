# Flint Quests

Flint Quests is a lightweight Fabric quest framework being developed alongside Project Flint. It is intentionally API-oriented so Project Flint and other mods can integrate custom progression checks without Flint Quests hardcoding their mechanics.

## Target environment

- Minecraft 1.21.11
- Fabric Loader 0.19.3
- Fabric API 0.141.6+1.21.11
- Java 21
- Fabric Loom 1.14.10
- Gradle 9.2.1
- Mojang mappings

## Current v0.1.18-a features

- `J` opens the Flint Quests book.
- FTB-Quests-inspired item-icon quest canvas.
- Compact, collapsible and nested icon-backed category sidebar.
- Bounded wheel panning plus click-and-drag canvas panning.
- Direct editor shortcuts: Shift-click linking and Alt-drag node positioning.
- In-game quest/category editing behind the editing toggle.
- Searchable item/block/icon/category/dependency pickers. Item/block search matches both display names and registry IDs.
- Stable fixed `flintquests:` namespace with safe editable quest-ID paths and progress/dependency migration on rename.
- Multiple tasks, ALL/ANY rules, optional tasks and prerequisite quests.
- Task types: `OBTAIN_ITEM`, `BREAK_BLOCK`, `USE_ITEM`, `INTERACT_BLOCK`, `CUSTOM_EVENT`, and clickable `CHECKMARK`.
- Per-player server-authoritative progress in the world save.
- Client progress synchronization for completed quest/task visualization.
- Quest node state borders: unlocked = glowing yellow, completed = glowing lime, visible-but-locked = gray; hidden locked quests stay hidden.
- Quest completion uses a vanilla advancement-style challenge toast with the quest icon/title and challenge-complete sound instead of a Flint Quests chat message.
- Optional Mod Menu config screen.
- Public custom-event hook: `FlintQuestHooks.trigger(...)`.
- Validation and administrative commands.

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

No Project Flint source changes are required for v0.1.18-a. Generic tasks are detected by Flint Quests itself. Project Flint integration becomes necessary only when quests need to detect bespoke Flint mechanics such as forming a Lumber Processor or completing a custom workstation process. See `MAIN_MOD_CHANGES.md`.

## Metadata

- Developer: **ImKas**
- License: **Flint Quests Custom License**
  - Allowed: decompiling/reading the code, using the API, and bundling/shading the unmodified official `.jar` inside another mod distribution.
  - Forbidden: copying, modifying, or re-uploading the Flint Quests source code or assets as a standalone project under another name.

## Build

Run `build.bat`.

The builder uses an existing wrapper when present or cached Gradle 9.2.1 under `.gradle-dist/`; otherwise it downloads and verifies the official Gradle distribution. It opens `build\libs` and automatically closes on success, while failed builds remain open so compiler output can be copied.

Expected output:

`build/libs/flint-quests-0.1.18-a.jar`

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


## v0.1.22-a authoring notes

- **Import Quest Data ZIP** uses a native file picker that starts in Downloads and rejects archives without the Flint Quests editable-data manifest/schema and required containers.
- Built-in themes are translucent by default. Custom themes should use a unique filename/ID; built-in preset IDs are maintained by Flint Quests.
- The quest book remembers the last opened category.
- The **Rules** tab can add required quests from any category and apply ALL/ANY logic to them.
