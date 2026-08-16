# Flint Quests Themes

Flint Quests supports JSON color themes stored in:

```text
config/flintquests/themes/
```

Flint Quests maintains the built-in preset files below. Preset files are refreshed so bug-fixes to the stock themes (such as transparency fixes) reach existing installs:

- `default.json` — Flint Quests' normal appearance
- `light.json` — bright/light UI
- `dark.json` — darker opaque UI
- `amoled.json` — true-black focused theme
- `red.json` — red/black theme
- `crimson.json` — deeper crimson/maroon theme
- `brown.json` — earthy brown theme
- `vanilla.json` — Minecraft-like gray UI colors

Use **Flint Quests Settings → Theme** to cycle installed themes. **Open Theme Folder** opens the directory directly. Any additional `.json` file placed beside the built-in themes becomes another selectable theme.

Built-in files are created **only when missing**. If an author edits one of those JSON files, Flint Quests does not overwrite their changes on the next launch. Delete a built-in file if you want Flint Quests to regenerate its default version.

## Example

```json
{
  "name": "Default",
  "background": "#B818202A",
  "canvas": "#B0212A36",
  "topBar": "#E01A222D",
  "bottomBar": "#E01A222D",
  "panel": "#F0232D39",
  "panelEdge": "#FF111820",
  "sidebar": "#F0222C38",
  "sidebarDivider": "#FF566273",
  "sidebarHandle": "#884A5562",
  "sidebarHandleInner": "#CC7B8796",
  "nodeBody": "#CC17212B",
  "unlocked": "#FFFFD84A",
  "completed": "#FF72FF63",
  "locked": "#FF747D88",
  "linkSource": "#FF61D6FF",
  "titleText": "#FFF3F5F7",
  "bodyText": "#FFD5DCE5",
  "mutedText": "#FF8FA0B2",
  "labelText": "#FFB7C1CC",
  "accentText": "#FFFFD46A",
  "errorText": "#FFFF6B6B"
}
```

Colors accept `#RRGGBB` or `#AARRGGBB`. Six-digit values are treated as fully opaque.

The active theme ID is the theme filename without `.json` and is stored as `activeTheme` in `flintquests.json`.

## Player quest-pack export

When **Build Nestable .jar** is used, Flint Quests embeds the currently active theme into the player-facing quest-pack JAR. This lets the distributed quest book retain the author's intended appearance without requiring players to copy theme files manually.


Custom theme files should use a unique ID/filename rather than replacing a built-in preset ID. Flint Quests does not overwrite non-built-in theme IDs. The stock presets intentionally use translucent background/canvas surfaces so the game world remains visible behind the quest UI.
