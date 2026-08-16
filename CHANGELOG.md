# Flint Quests Changelog

## v0.1.22-a — native ZIP picker, translucent presets, remembered category, cross-category requirements

- Replaced the Swing/JFileChooser quest-data importer with Minecraft/LWJGL's native file dialog path, avoiding `HeadlessException` while still starting in the user's Downloads folder.
- Kept strict editable-data ZIP validation: imports must contain the Flint Quests manifest/schema plus required quest/category containers before any live data is touched.
- Adjusted all built-in theme presets so their main background/canvas surfaces remain translucent instead of covering the world with opaque blocks.
- Built-in preset files are now refreshed by Flint Quests so preset bug-fixes reach existing installs; custom theme files with their own IDs remain untouched.
- Added more vertical breathing room above and between settings buttons.
- The quest book now remembers the last selectable category/page and reopens it next time.
- Moved explicit required-quest selection into the Rules tab and clarified that required quests may come from any category.
- Required quests still use ALL/ANY behavior and continue to participate in normal quest unlocking/progression.
- No Project Flint source changes are required.

## v0.1.21-a — Downloads-first import validation + built-in theme presets

- Changed **Import Quest Data ZIP** so its file chooser opens in the current user's `Downloads` folder by default when that folder exists.
- Added a stricter editable-data ZIP contract. New exports use format `2` with the required schema marker `flintquests.quest-data.v2`.
- v2 imports require `flintquests-data.json`, the correct Flint Quests data type/schema, and explicit `quests/` + `categories/` containers before any live quest data is replaced.
- Invalid/random ZIP files fail before the pre-import backup or replacement stage.
- Kept backwards import compatibility with Flint Quests v1 editable-data ZIPs when they contain the valid v1 manifest and at least one recognized Flint Quests payload file.
- Added automatically generated base themes: **Default, Light, Dark, AMOLED, Red, Crimson, Brown, and Vanilla Minecraft**.
- Built-in theme files are only created when missing, so authors can edit their local copies without Flint Quests overwriting them on launch.
- No Project Flint source changes are required.

## v0.1.20-a — canvas zoom + in-screen editor toggle

- Added FTB-style **Control + mouse-wheel zoom** to the quest canvas.
- Scroll up while holding Control to zoom in; scroll down while holding Control to zoom out.
- Zoom is centered around the mouse cursor so the point under the cursor stays anchored instead of the whole graph jumping toward the screen center.
- Quest-node spacing, node geometry, dependency routing, panning bounds, dragging math, hitboxes, and branch offsets now all respect the current zoom level.
- The current quest-book zoom is saved in `flintquests.json` and restored the next time the quest book is opened.
- Fixed the editing-mode toggle shortcut while Flint Quests screens are open. The configurable toggle action/modifier now works from the quest book, quest editor, category editor, search selector, and Flint Quests settings screen instead of only during gameplay.
- The screen-level shortcut checks the actual bound modifier key state, so the modifier remains configurable rather than hardcoding Left Control for the editing toggle.
- No Project Flint main-mod changes are required.

## v0.1.19-a — true diamond geometry, custom themes, editable quest-data ZIPs

- Fixed Diamond quest nodes by switching the quest node canvas to an odd 31px geometry so the diamond has a true center point and its normal/glow borders share the exact same center instead of visually stepping inward.
- Added JSON-based custom theme support under `config/flintquests/themes/`.
- Added a generated `default.json` theme and a settings control for cycling installed themes.
- Added **Open Theme Folder** for direct author access to theme files.
- Themeable colors now cover the main quest canvas, headers, sidebar, nodes/states, quest detail panels, editor panels, search UI, settings UI, and common text/accent/error colors.
- Nestable player quest-pack JAR exports now embed the author's currently active theme.
- Added **Export Quest Data ZIP** for sharing editable quest/category data with collaborators.
- Added **Import Quest Data ZIP** with a desktop file picker.
- Added **Open Quest Data ZIP Folder**.
- Editable data ZIPs include quest definitions, categories, quest-ID migrations, and the active theme; player progress is intentionally excluded.
- Import automatically creates a pre-import backup ZIP before replacing editable quest/category data.
- ZIP import validates extraction paths and only accepts the Flint Quests editable-data manifest format.
- Added `docs/THEMES.md` and `docs/QUEST_DATA_ZIP.md`.
- No Project Flint main-mod changes are required.

## v0.1.18-a — quest detail pagination, progress cleanup, diamond rendering

- Reworked Diamond quest-node rendering with an actual diamond-shaped filled body and matching border instead of the previously broken-looking outline.
- Deleted quests now purge their per-player quest progress, completion state, and task-completion entries when progress is next synchronized.
- Stale progress for task IDs removed from an existing quest is also pruned automatically.
- Quest deletion immediately clears the local client copy and requests a fresh authoritative progress sync.
- Rebuilt the player-facing quest detail layout around a paginated content flow. Long descriptions and large task lists can no longer render beyond the panel bounds.
- Description lines, task lines, spacers, and manual CHECKMARK controls all participate in the same pagination flow.
- Manual CHECKMARK buttons now sit exactly where their task appears in the current page instead of using a separate fixed Y-position calculation.
- Added Previous / Next controls and a Page X / Y indicator when a quest needs more than one page.
- Preserved legacy color-code and multiline support across paginated text.
- No Project Flint main-mod changes are required.

## v0.1.17-a — exporter folder shortcut, reliable node dragging, legacy text formatting

- Added **Open Built Jars** to the dev-only Flint Quests settings screen. It creates/opens `flintquests-exports/` so finished player quest-pack jars are immediately accessible after export.
- Restored reliable editor node movement. Node dragging no longer rebuilds the entire screen/widget tree every drag step, and the default Alt modifier is also recognized directly from the mouse event as a fallback while preserving the configurable move-node keybind.
- Added legacy Minecraft text formatting support to player-facing quest/category text. Both `&` and `§` prefixes are accepted.
- Supported formatting codes: colors `0-9`, `a-f`; obfuscated `k`; bold `l`; strikethrough `m`; underline `n`; italic `o`; reset `r`.
- Formatting is now applied to quest titles/lore, task text, category labels/tooltips, quest detail text, checkmark labels, selected category titles, and quest-completion toast titles.
- Editor fields intentionally keep the raw author-entered codes visible so they remain editable.
- Bumped the source version to `0.1.17-a`.
- No Project Flint source changes are required.

## v0.1.15-a — responsive editor + node shapes + one-action-per-line tooltip

- Rebuilt the quest editor into five compact pages (`Quest`, `Look`, `Flow`, `Rules`, `Task`) so controls no longer overlap each other or the footer.
- The editor now scales down much more safely on narrow/small GUI sizes instead of trying to keep two full-width panels visible at once.
- The multiline description editor remains on the Quest page and dynamically uses the remaining vertical space.
- Quest hover tooltips now put **every editor action on its own line**: left-click edit, right-click view, link key + click, and move key + drag.
- Added persistent per-quest node shapes. Available shapes are **Square** (default), **Circle**, **Hexagon**, and **Diamond**.
- Node shapes are selectable from the new `Look` editor page and saved directly in quest JSON as `nodeShape`.
- Existing quest JSON without `nodeShape` automatically defaults to Square.
- Completed/unlocked/locked node borders and dependency-link selection outlines now follow the selected node shape.
- No Project Flint main-mod changes are required.

## v0.1.14-a — true multiline lore, stable group dropdowns, category deletion

- Replaced the quest editor's one-line description field with a real multiline text box. Pressing Enter now stores actual newline characters in quest descriptions/lore.
- Quest-node hover lore now renders as a real multiline tooltip: quest title first, then each description/lore line on its own row, followed by editor controls on separate rows when developer tools are enabled.
- Legacy descriptions containing literal `\n` text are interpreted as line breaks when shown/edited so old test quests do not need to be recreated.
- Group-header categories now keep a dedicated expand/collapse arrow button separate from the category-label button, preventing the dropdown control from disappearing when a Quest Page is switched to a Group Header.
- Added **Delete Category** to the category editor for existing categories.
- Deleting a category safely reparents child categories, moves quests assigned to the deleted category onto a valid remaining Quest Page, and creates a fallback page only if absolutely necessary.
- Preserved the custom Flint Quests license and the user-supplied Flint Quests logo from the previous release.
- Bumped the patch version to `0.1.14-a`.
- No Project Flint main-mod changes are required.

## v0.1.13-a — sidebar polish, category-rename fix, multiline text, new logo

- Reworked the left sidebar hover handle so it is larger, cleaner, and no longer draws the awkward extra mini-bar beside it.
- Slowed the sidebar slide animation slightly and widened the hover trigger zone so opening the sidebar feels less twitchy.
- Removed the dotted canvas background for a cleaner quest-book look.
- Adjusted dependency-line rendering to use a cleaner branch path and thicker overlaps so the previous single-pixel connector gap is no longer left behind.
- Added the newly supplied custom Flint Quests logo as the mod icon asset.
- Quest descriptions and non-checkmark task text now honor real newline breaks and wrap across multiple lines instead of rendering as one long line-spam block.
- Fixed category renames so child categories and quests assigned to that category follow the new category ID instead of being orphaned, which also prevents group dropdowns from mysteriously disappearing after edits.
- Bumped the patch version to `0.1.13-a`.
- No Project Flint main-mod changes are required.

## v0.1.9-a — editor canvas shortcuts, secure checkmarks, quest-state borders, safe ID renames

- Removed the buggy tiny completed-quest checkmark badge from canvas nodes.
- Added state-driven quest borders: unlocked quests glow yellow, completed quests glow lime, and visible-but-locked quests use a flat gray border.
- Hidden-until-ready quests are now actually omitted from the canvas until their prerequisite rule is satisfied.
- Added **Shift-click dependency linking** in editor mode: Shift-click the earlier quest, then Shift-click the later quest to create the prerequisite branch automatically.
- Dependency-link creation blocks circular dependency edges.
- Added **Alt-drag node movement** in editor mode; moved nodes snap to the Flint Quests canvas grid and save on release.
- Replaced the sidebar's thin selected-category line with a full selected-row outline.
- Removed the redundant `Edit Quest` heading and moved the editor panels upward.
- Re-spaced the editor again so lower rule/task buttons no longer collide with helper/footer text.
- Quest IDs are no longer permanently locked after creation. The `flintquests:` namespace stays fixed, while the path can be edited.
- Safe quest renames now rewrite dependency references and persist `quest_id_migrations.json` so existing player progress follows the renamed ID.
- Deleting a quest now removes that quest from other quests' prerequisite lists instead of leaving broken references.
- Manual CHECKMARK tasks no longer run `/flintquests check ...` as a player command.
- Added a dedicated C2S checkmark payload; the server validates that the requested task is actually a CHECKMARK task and that its quest prerequisites are satisfied.
- Removed the public `/flintquests check` command from the player command tree.
- Kept Project Flint integration unchanged: **no Project Flint source changes are required**.

## v0.1.8-a — completion feedback + editor cleanup

- Reworked quest completion feedback so completed quests no longer rely on a chat message.
- Added server-to-client quest progress synchronization using Fabric custom payloads.
- Added a vanilla advancement-style completion toast that uses the quest title and configured quest item icon.
- Completion toasts use the vanilla challenge-style advancement frame, which also supplies Minecraft's challenge-complete toast sound.
- Added visible completed-state styling to quest nodes: gold border plus a checkmark badge.
- Dependency connector lines become gold once their prerequisite quest is complete.
- Quest detail screens now show completed tasks and completed quests visually.
- Kept manual CHECKMARK tasks as actual clickable player buttons and made their checked state reflect synchronized server progress.
- Removed raw dependency metadata from the player-facing quest detail screen.
- Fixed the strange hover-tooltip glyph caused by embedding a newline inside a literal tooltip component; editor hints now use a clean single-line separator.
- Re-spaced and relabeled the quest editor to stop labels/fields from overlapping and make the purpose of each field clearer.
- Search pickers now index both human-readable Minecraft item/block names and registry IDs.
- Renamed the Mod Menu setting from `Quest Completion Messages` to `Quest Completion Popups` while preserving the existing config field for compatibility.
- No Project Flint main-mod changes are required.

## v0.1.7-a — editor/search/checkmark usability

- Added real clickable manual CHECKMARK task controls to the player quest-detail screen.
- Added click-and-drag canvas panning in addition to wheel/trackpad panning.
- Reworked the quest editor into clearer quest/task sections.
- Added searchable selectors for quest icons, item targets, block targets, categories and dependencies.
- Added player-facing task text separate from internal task IDs/targets.
- Fixed the quest namespace to `flintquests:` and made an existing quest ID immutable after creation.
- Continued to package source patches with project files directly at the ZIP root.
- No Project Flint main-mod changes are required.

## v0.1.6-a — sidebar/category UX + bounded canvas

- Removed the permanent `PLAYER MODE` / `EDIT MODE` footer label and the always-visible scrolling instruction from the quest book.
- Canvas panning is now bounded from the actual quest-node extents, preventing infinite scrolling into empty space.
- The category sidebar open/closed state now persists in `config/flintquests/flintquests.json`.
- Expanded/collapsed nested-category state is also persisted.
- Reduced the open sidebar width from 174px to 112px and reduced category row height for a much more compact FTB-style layout.
- Added icon-backed category entries. Category icons use normal Minecraft item IDs.
- Added explicit category metadata files under `config/flintquests/categories/`.
- Categories can now have a `parent`, allowing arbitrary nested category/group trees instead of only a single `Group/Category` path.
- Added an in-game category editor for display name, icon item ID, parent category ID, and sort order.
- Existing path-style quest categories remain backwards-compatible and are converted into implicit runtime category trees until explicitly edited/saved.
- Added sidebar scrolling for large category trees.
- In editor mode, left-clicking a quest still edits it while right-clicking opens its normal player-facing quest view.
- In editor mode, right-clicking a category opens its category editor.
- Removed the raw `Dependencies: ...` line from the player-facing quest detail screen. Dependency connector lines on the quest canvas remain.
- `/flintquests reload` now reloads category definitions as well as config and quests.
- Preserved the successful-build auto-close behavior in `build.bat`.
- No Project Flint main-mod changes are required.

## v0.1.5-a — 1.21.11 node-UI compile fix

- Fixed `QuestIconHelper` failing to compile because Minecraft 1.21.11 uses `net.minecraft.resources.Identifier` rather than `ResourceLocation` in the Mojang mappings used by Flint Quests.
- Kept safe item-ID parsing through `Identifier.tryParse(...)` and the existing book fallback for invalid/air item IDs.
- Renamed Flint Quests' private `rebuildWidgets()` helper to `refreshQuestWidgets()` so it no longer accidentally attempts to override Minecraft `Screen#rebuildWidgets()` with weaker access.
- Preserved the v0.1.4-a FTB-style quest canvas, grouped sidebar, ARR metadata, ImKas developer metadata, and successful-build auto-close behavior.
- No Project Flint main-mod changes are required.

## v0.1.4-a — FTB-style node canvas + grouped category sidebar

- Replaced the full-width paged quest list with a visual quest canvas inspired by FTB Quests' node-based layout.
- Quests now render as compact clickable buttons with their configured Minecraft item icon centered on the node.
- Added quest-title hover tooltips for icon nodes.
- Added dependency connector lines between quests visible in the current category.
- Added a left category sidebar that can be collapsed/reopened.
- Added expandable category groups without breaking existing quest JSON: `chapter` values now support `Group/Category` paths.
- Added selected-category highlighting and grouped child indentation in the sidebar.
- Added vertical canvas panning with the scroll wheel and horizontal panning with horizontal wheel/trackpad.
- New quests created from the canvas inherit the currently selected category.
- Updated the editor label from `Chapter` to `Category path (Group/Category)`.
- Restyled the player quest-detail screen to match the darker canvas UI.
- Updated Flint Quests developer metadata to `ImKas`.
- Changed Flint Quests license metadata and LICENSE file to All Rights Reserved.
- Updated `build.bat` to automatically close after a successful build while retaining pauses on failures.
- No Project Flint main-mod changes are required.

## v0.1.3-a — Project Flint icon + optional Mod Menu settings

- Reused Project Flint's existing `assets/projectflint/icon.png` as the Flint Quests mod icon.
- Added `icon` metadata to Flint Quests so Mod Menu no longer falls back to the broken/default icon.
- Added optional Mod Menu API integration through a `modmenu` entrypoint.
- Added a Flint Quests settings screen reachable from Mod Menu's Config button when Mod Menu is installed.
- Added an immediate `Quest Editing: ON/OFF` toggle. Turning editing off puts the quest book into its read-only/player-facing mode.
- Added a `Quest Completion Messages: ON/OFF` toggle.
- Settings save immediately to the existing `config/flintquests/flintquests.json`.
- Mod Menu remains optional at runtime; the API is a compile-only dependency.
- Added the Terraformers Maven repository and Mod Menu 17.0.0 compile-only API dependency for the Minecraft 1.21.11 target.
- Preserved the Minecraft 1.21.11 no-double-blur rule in the new settings screen.
- No Project Flint main-mod code changes are required.

## v0.1.2-a — Minecraft 1.21.11 screen blur crash fix

- Fixed a client crash when rendering the Flint Quests quest book: `IllegalStateException: Can only blur once per frame`.
- Removed redundant `renderBackground(...)` calls from `QuestBookScreen`, `QuestDetailScreen`, and `QuestEditorScreen`.
- Minecraft 1.21.11 already performs the screen background/blur pass for these screens; explicitly requesting it again caused the second-blur exception.
- Patched all Flint Quests screens together so opening a quest or editor does not simply move the crash to the next screen.
- No Project Flint main-mod changes are required.

## v0.1.1-a — first compile patch + builder UI

- Fixed Minecraft 1.21.11 compile failure in `ProgressManager` caused by nonexistent `ServerPlayer#getServer()` calls.
- Progress loading/saving now resolves the server through `player.level().getServer()`.
- Reworked `build.bat` to match the established Project Flint builder UI and behavior.
- Added Java/project preflight checks, cached Gradle detection, official SHA-256 verification, clear success/failure panels, expected JAR checks, and automatic output-folder opening.
- Preserved `.gradle-dist` as the bootstrap directory so an existing Gradle 9.2.1 download is reused.
- No Project Flint main-mod changes are required.

## v0.1.0-a — initial foundation

- Created standalone Fabric 1.21.11 quest framework.
- Added JSON quest repository.
- Added separate per-player world progress storage.
- Added dependency modes: ALL / ANY.
- Added task completion modes: ALL / ANY.
- Added task types:
  - OBTAIN_ITEM
  - BREAK_BLOCK
  - USE_ITEM
  - INTERACT_BLOCK
  - CUSTOM_EVENT
  - CHECKMARK
- Added server-side completion engine.
- Added Project Flint public custom-event hook API.
- Added `J` quest-book keybind.
- Added local development editor for creating, editing and deleting quests.
- Added multiple-task editing.
- Added quest validation for missing/self/circular dependencies.
- Added config editing toggle.
- Added administrative commands.
- Added self-contained Windows `build.bat` using Gradle 9.2.1.
- Added `FLINT_QUESTS.md` persistent architecture/roadmap/source document.
- Added mandatory `MAIN_MOD_CHANGES.md` integration handoff document.

### Known limitations

- Dedicated-server GUI editing is not server-authoritative yet.
- Detailed player progress is not yet synchronized/rendered in the quest GUI.
- Current editor is form-based; the pannable FTB-style node canvas is planned.
- More generic task types are planned.
