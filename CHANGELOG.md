# Flint Quests Changelog

## v0.1.10-a — responsive editor, reliable Alt-drag, hover-slide categories

- Reworked the quest editor layout so its controls are positioned from the current scaled screen height instead of assuming a tall GUI. The behavior controls and Save/Cancel/Delete row now remain separated at smaller GUI heights.
- Removed the extra quest-ID helper sentence from the editor form to free vertical space; the fixed `flintquests:` prefix remains visually distinct and non-editable.
- Removed clickable quest-node widgets from the canvas and moved node interaction into `QuestBookScreen` itself. This prevents normal button handling from swallowing modifier gestures.
- Alt movement now tracks the left/right Alt key directly in addition to reading the mouse-event modifier, making **Alt + left-drag** the authoritative node-move gesture.
- Replaced the manual category-sidebar arrow with an FTB-Quests-style hover reveal. Moving the pointer to the left screen edge slides the nested category panel in; moving away slides it back out.
- The hover sidebar overlays the quest canvas instead of shrinking/recentering the canvas whenever it opens.
- Category rows, icons, nested expand buttons, and category editor controls move with the sidebar animation.
- Expanded/collapsed nested-category state continues to persist; the old manual sidebar-open state is no longer used by the quest-book UI.
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
