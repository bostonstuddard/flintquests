# Flint Quests v1.0.0 — Release Notes

Flint Quests v1.0.0 is the first stable release of the standalone, API-oriented quest framework developed for Project Flint and other Fabric mods.

## Release highlights

- Visual item-icon quest canvas with categories/groups, dependency branches, zooming, panning, node shapes, themes, and player-facing completion states.
- In-game authoring with quest/category/task/reward editing, searchable registry pickers, safe quest-ID renames, explicit cross-category prerequisites, multiline/color-formatted text, and dev-only controls.
- Server-authoritative task/progress storage and manual-checkmark security.
- Optional item rewards with per-quest **Claim Reward** and category-wide **Claim All**.
- Completion toasts using the configured quest title/icon.
- Public custom-event hook API for Project Flint and third-party mods.
- Editable quest-data ZIP import/export for collaboration.
- Nestable player quest-pack JAR export for shipping finished quest books.
- Built-in theme presets plus custom JSON theme support.

## Reward behavior

Rewards are optional. A quest may contain any number of item-stack rewards. Completing a quest unlocks the reward but does not grant it automatically. The player claims it manually. Claim state is stored per player and validated by the server to prevent duplicate claims.

**Claim All** only processes the category currently open in the quest book and only grants rewards for quests that are complete, reward-bearing, and not already claimed.

## Compatibility

Existing 0.x quest definitions remain compatible. Quests that do not contain a `rewards` field simply have no rewards. Existing player progress also loads normally; missing `rewardClaimed` state defaults to unclaimed.

## Project Flint

Project Flint does not require source changes for Flint Quests v1.0.0 itself. Custom Project Flint mechanics only need API hooks later when those bespoke mechanics should directly satisfy custom-event quest tasks. See `MAIN_MOD_CHANGES.md`.
