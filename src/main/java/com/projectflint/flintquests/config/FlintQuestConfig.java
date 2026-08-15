package com.projectflint.flintquests.config;

import java.util.ArrayList;
import java.util.List;

public final class FlintQuestConfig {
	public boolean questEditing = true;
	public boolean questEditingRequiresOperator = true;
	public int inventoryScanIntervalTicks = 10;
	public boolean announceQuestCompletion = true;
	public boolean questSidebarOpen = true;
	public List<String> expandedQuestCategories = new ArrayList<>();

	public void normalize() {
		if (inventoryScanIntervalTicks < 1) inventoryScanIntervalTicks = 1;
		if (expandedQuestCategories == null) expandedQuestCategories = new ArrayList<>();
		expandedQuestCategories.removeIf(value -> value == null || value.isBlank());
	}
}
