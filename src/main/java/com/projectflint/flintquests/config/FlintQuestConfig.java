package com.projectflint.flintquests.config;

import java.util.ArrayList;
import java.util.List;

public final class FlintQuestConfig {
	/**
	 * Master in-game development gate. When false, editor UI, editor shortcuts,
	 * and developer commands are unavailable in-game. The player quest system
	 * and public API remain active.
	 */
	public boolean devEnvironment = true;
	public boolean questEditing = true;
	public boolean questEditingRequiresOperator = true;
	public int inventoryScanIntervalTicks = 10;
	public boolean announceQuestCompletion = true;
	public boolean questSidebarOpen = true;
	public String activeTheme = "default";
	public double questBookZoom = 1.0D;
	public String lastQuestCategory = "introduction";
	public List<String> expandedQuestCategories = new ArrayList<>();

	public void normalize() {
		if (inventoryScanIntervalTicks < 1) inventoryScanIntervalTicks = 1;
		if (activeTheme == null || activeTheme.isBlank()) activeTheme = "default";
		if (lastQuestCategory == null || lastQuestCategory.isBlank()) lastQuestCategory = "introduction";
		questBookZoom = Math.max(0.55D, Math.min(1.80D, questBookZoom));
		if (expandedQuestCategories == null) expandedQuestCategories = new ArrayList<>();
		expandedQuestCategories.removeIf(value -> value == null || value.isBlank());
	}
}
