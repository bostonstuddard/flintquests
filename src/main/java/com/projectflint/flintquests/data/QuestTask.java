package com.projectflint.flintquests.data;

import java.util.Objects;

public final class QuestTask {
    public String id = "task";
    public TaskType type = TaskType.OBTAIN_ITEM;
    public String label = "";
    public String target = "minecraft:air";
    public int count = 1;
    public boolean optional = false;

    public QuestTask() {
    }

    public QuestTask(String id, TaskType type, String target, int count, boolean optional) {
        this(id, type, "", target, count, optional);
    }

    public QuestTask(String id, TaskType type, String label, String target, int count, boolean optional) {
        this.id = id;
        this.type = type;
        this.label = label == null ? "" : label;
        this.target = target;
        this.count = Math.max(1, count);
        this.optional = optional;
    }

    public void normalize(int index) {
        if (id == null || id.isBlank()) id = "task_" + index;
        if (type == null) type = TaskType.OBTAIN_ITEM;
        if (label == null) label = "";
        if (target == null) target = "";
        if (type == TaskType.CHECKMARK) {
            target = "";
            count = 1;
        } else {
            count = Math.max(1, count);
        }
    }

    public String displayLabel() {
        if (label != null && !label.isBlank()) return label.trim();
        return switch (type) {
            case CHECKMARK -> "Mark this task complete";
            case OBTAIN_ITEM -> "Obtain " + target;
            case BREAK_BLOCK -> "Break " + target;
            case USE_ITEM -> "Use " + target;
            case INTERACT_BLOCK -> "Interact with " + target;
            case CUSTOM_EVENT -> "Complete " + target;
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QuestTask other)) return false;
        return count == other.count && optional == other.optional && Objects.equals(id, other.id)
                && type == other.type && Objects.equals(label, other.label) && Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, label, target, count, optional);
    }
}
