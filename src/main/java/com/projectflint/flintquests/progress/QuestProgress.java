package com.projectflint.flintquests.progress;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestProgress {
    public boolean complete = false;
    public long completedAt = 0L;
    public Map<String, TaskProgress> tasks = new LinkedHashMap<>();
}
