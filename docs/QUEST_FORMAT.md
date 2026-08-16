# Quest JSON Format — v0.1

Quest files live in:

`config/flintquests/quests/`

Each `.json` file contains one quest.

Example:

```json
{
  "id": "projectflint:sanding_station",
  "chapter": "wood_age",
  "title": "Smooth Operator",
  "description": "Construct a Sanding Station and use it successfully.",
  "icon": "projectflint:sanding_station",
  "nodeShape": "SQUARE",
  "x": 12,
  "y": 5,
  "hiddenUntilDependencies": false,
  "dependencyMode": "ALL",
  "taskMode": "ALL",
  "dependencies": [
    "projectflint:wooden_grinder"
  ],
  "tasks": [
    {
      "id": "obtain_station",
      "type": "OBTAIN_ITEM",
      "target": "projectflint:sanding_station",
      "count": 1,
      "optional": false
    },
    {
      "id": "use_station",
      "type": "CUSTOM_EVENT",
      "target": "projectflint:sanding_station_craft",
      "count": 1,
      "optional": false
    }
  ]
}
```

## Node shape — v0.1.15-a

Each quest may specify `nodeShape`:

- `SQUARE` — default and backwards-compatible fallback;
- `CIRCLE`;
- `HEXAGON`;
- `DIAMOND`.

Older quest files that omit `nodeShape` load as `SQUARE`. The shape affects the canvas node state border/glow and editor link-selection outline; it does not change quest completion logic or the quest ID.

## Stable IDs

Treat `id` and every task's `id` as save-data identifiers. Titles/descriptions can be freely edited; released IDs should not be casually renamed.

## Completion modes

`dependencyMode`:

- `ALL` — every listed dependency must be complete.
- `ANY` — at least one listed dependency must be complete.

`taskMode`:

- `ALL` — every non-optional task must be complete.
- `ANY` — at least one non-optional task must be complete.

## Task targets

For registry-backed task types, use full registry IDs such as `minecraft:stick` or `projectflint:sanding_station`.

For `CUSTOM_EVENT`, use a stable namespaced event ID agreed on by the emitting mod and the quest definition. Flint Quests 1.1 lets mods register friendly metadata for these IDs through `FlintQuestAPI` / the optional `flintquests` integration entrypoint; registered events become searchable in the editor. Registration is discovery metadata only, so manually entered unregistered namespaced IDs remain valid. See `docs/API.md`.

## Category metadata — v0.1.6-a

Optional category files live in:

`config/flintquests/categories/`

Each file contains one category:

```json
{
  "id": "stone_tools",
  "title": "Stone Tools",
  "icon": "projectflint:crude_stone_hammer",
  "parent": "stone_age",
  "order": 20
}
```

Fields:

- `id` — stable category identifier referenced by a quest's existing `chapter` field.
- `title` — player-facing category name.
- `icon` — Minecraft item registry ID rendered in the sidebar.
- `parent` — another category ID; blank means top level.
- `order` — lower values sort first among siblings.

Categories can be nested to arbitrary practical depth by chaining `parent` IDs.

Existing quest files do not require migration. If a quest references a category with no explicit category file, Flint Quests creates an implicit runtime category. Older path-style values such as `Stone Age/Tools` are still interpreted as nested implicit categories until explicitly edited and saved.

## v0.1.7-a ID and task-label rules

Quest IDs are always canonicalized into the `flintquests:` namespace. The in-game editor exposes only the path portion when creating a quest and locks an existing quest ID after creation.

Tasks may now include an optional player-facing `label` string:

```json
{
  "id": "confirm_reading",
  "type": "CHECKMARK",
  "label": "I have read the campfire instructions",
  "target": "",
  "count": 1,
  "optional": false
}
```

For `CHECKMARK` tasks, `target` is unused and `count` is normalized to `1`. The player sees a clickable checkbox using `label`. Older task JSON without `label` remains valid.


## Quest ID rename migrations

Quest IDs always use the `flintquests:` namespace. The editor exposes only the path portion. Existing quests may be renamed through the editor; Flint Quests writes `config/flintquests/quest_id_migrations.json`, rewrites prerequisite references, and migrates player progress from the old ID to the new ID when that progress is loaded.

Do not manually delete `quest_id_migrations.json` while worlds still contain progress for renamed quest IDs.


## Category metadata

Explicit category files live in `config/flintquests/categories/`.

```json
{
  "id": "humble_beginnings",
  "title": "Humble Beginnings",
  "icon": "minecraft:book",
  "parent": "",
  "selectable": false,
  "order": 0
}
```

`selectable: true` means the category is an actual quest page/canvas that quests may be assigned to. `selectable: false` means the entry is a grouping header only: it can contain child categories and expand/collapse in the sidebar, but players cannot open it as a quest page. Quest editor category selection only lists selectable categories.


## Player quest-pack bundle — v0.1.16-a

The development build can export the current authored quest set as a nestable/player-distribution JAR. Bundled definitions are stored inside the exported JAR under:

- `assets/flintquests/bundled/quests/`
- `assets/flintquests/bundled/categories/`
- `assets/flintquests/bundled/quest_id_migrations.json` when migrations exist
- `assets/flintquests/bundled/player-bundle.json` as the bundle manifest/index

When the bundle manifest is present with `playerOnly: true`, Flint Quests treats the JAR as a finished player distribution: local authoring quest/category files are not used as the definition source and in-game developer tools are hard-disabled.


## Player-facing text formatting

String fields intended for players (`title`, `description`, task display text, and category display names) may use legacy Minecraft formatting codes. Flint Quests accepts either the normal section-sign prefix or the easier-to-type ampersand prefix.

Examples:

```text
&aGreen text
&6&lGold bold text
&7Normal gray &cRed
&rReset formatting
```

Supported codes are colors `0-9` and `a-f`, formatting `k-o`, and reset `r`. Editor text boxes preserve the literal source codes; rendering converts them into styled Minecraft Components.


## Rewards

Rewards are optional. Omitting `rewards` or using an empty list means the quest has no claimable reward.

```json
"rewards": [
  {
    "item": "minecraft:diamond",
    "count": 2
  },
  {
    "item": "minecraft:bread",
    "count": 4
  }
]
```

Reward completion is stored separately from quest completion in player progress. A completed quest with rewards remains claimable until its reward is claimed.
