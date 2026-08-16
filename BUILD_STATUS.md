# Flint Quests v1.1.0 — Compile Fix

## Fixed compiler error

`SearchSelectScreen` passed a `List<Component>` to `GuiGraphics#setTooltipForNextFrame(...)`, but Minecraft 1.21.11's mapped overload expects formatted character sequences for that overload.

The custom-event search tooltip now uses Minecraft's dedicated component-list method:

```java
graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
```

This preserves multi-line `Component` tooltips and matches the 1.21.11 GUI API.

## Version

The release remains **v1.1.0** because this is a compile correction for the not-yet-successfully-built v1.1.0 source, not a new feature release.

## Project Flint changes

None required for this compile fix.
