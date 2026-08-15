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

## Current v0.1.10-a features

- `J` opens the Flint Quests book.
- FTB-Quests-inspired item-icon quest canvas.
- Nested icon-backed category sidebar that slides in from the left when the pointer reaches the screen edge.
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

No Project Flint source changes are required for v0.1.10-a. Generic tasks are detected by Flint Quests itself. Project Flint integration becomes necessary only when quests need to detect bespoke Flint mechanics such as forming a Lumber Processor or completing a custom workstation process. See `MAIN_MOD_CHANGES.md`.

## Metadata

- Developer: **ImKas**
- License: **All Rights Reserved**

## Build

Run `build.bat`.

The builder uses an existing wrapper when present or cached Gradle 9.2.1 under `.gradle-dist/`; otherwise it downloads and verifies the official Gradle distribution. It opens `build\libs` and automatically closes on success, while failed builds remain open so compiler output can be copied.

Expected output:

`build/libs/flint-quests-0.1.10-a.jar`

See `FLINT_QUESTS.md` for the long-term design contract.


## v0.1.10-a editor shortcuts

When quest editing is enabled on the quest canvas:

The editor form uses a responsive vertical layout so its bottom behavior controls and Save/Cancel/Delete row stay on-screen at smaller GUI heights. The category sidebar is hover-driven rather than manually toggled.

- **Shift + click quest A, then Shift + click quest B**: make B depend on A.
- **Alt + drag a quest node**: move the quest on the canvas and snap it to the quest grid.
- **Left click**: edit a quest.
- **Right click**: preview the normal player-facing quest page.

Node borders communicate state: unlocked = glowing yellow, completed = glowing lime, visible-but-locked = gray. Hidden locked quests remain hidden.

Manual CHECKMARK tasks use an internal Fabric packet and are server-validated; they no longer execute `/flintquests check` commands.

Quest IDs keep the fixed `flintquests:` namespace, but the path can now be safely renamed. Renames rewrite dependencies and migrate existing player progress.
