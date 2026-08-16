package com.projectflint.flintquests.theme;

/** JSON-backed color theme for Flint Quests screens. Colors accept #RRGGBB or #AARRGGBB. */
public final class QuestTheme {
    public String name = "Default";
    public String background = "#B818202A";
    public String canvas = "#B0212A36";
    public String topBar = "#E01A222D";
    public String bottomBar = "#E01A222D";
    public String panel = "#F0232D39";
    public String panelEdge = "#FF111820";
    public String sidebar = "#F0222C38";
    public String sidebarDivider = "#FF566273";
    public String sidebarHandle = "#884A5562";
    public String sidebarHandleInner = "#CC7B8796";
    public String nodeBody = "#CC17212B";
    public String unlocked = "#FFFFD84A";
    public String completed = "#FF72FF63";
    public String locked = "#FF747D88";
    public String linkSource = "#FF61D6FF";
    public String titleText = "#FFF3F5F7";
    public String bodyText = "#FFD5DCE5";
    public String mutedText = "#FF8FA0B2";
    public String labelText = "#FFB7C1CC";
    public String accentText = "#FFFFD46A";
    public String errorText = "#FFFF6B6B";

    public void normalize() {
        if (name == null || name.isBlank()) name = "Unnamed Theme";
    }

    public int backgroundColor() { return color(background, 0xB818202A); }
    public int canvasColor() { return color(canvas, 0xB0212A36); }
    public int topBarColor() { return color(topBar, 0xE01A222D); }
    public int bottomBarColor() { return color(bottomBar, 0xE01A222D); }
    public int panelColor() { return color(panel, 0xF0232D39); }
    public int panelEdgeColor() { return color(panelEdge, 0xFF111820); }
    public int sidebarColor() { return color(sidebar, 0xF0222C38); }
    public int sidebarDividerColor() { return color(sidebarDivider, 0xFF566273); }
    public int sidebarHandleColor() { return color(sidebarHandle, 0x884A5562); }
    public int sidebarHandleInnerColor() { return color(sidebarHandleInner, 0xCC7B8796); }
    public int nodeBodyColor() { return color(nodeBody, 0xCC17212B); }
    public int unlockedColor() { return color(unlocked, 0xFFFFD84A); }
    public int completedColor() { return color(completed, 0xFF72FF63); }
    public int lockedColor() { return color(locked, 0xFF747D88); }
    public int linkSourceColor() { return color(linkSource, 0xFF61D6FF); }
    public int titleTextColor() { return color(titleText, 0xFFF3F5F7); }
    public int bodyTextColor() { return color(bodyText, 0xFFD5DCE5); }
    public int mutedTextColor() { return color(mutedText, 0xFF8FA0B2); }
    public int labelTextColor() { return color(labelText, 0xFFB7C1CC); }
    public int accentTextColor() { return color(accentText, 0xFFFFD46A); }
    public int errorTextColor() { return color(errorText, 0xFFFF6B6B); }

    private static int color(String raw, int fallback) {
        if (raw == null) return fallback;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            long parsed = Long.parseUnsignedLong(value, 16);
            if (value.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
