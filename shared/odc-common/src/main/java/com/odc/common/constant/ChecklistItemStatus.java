package com.odc.common.constant;

public enum ChecklistItemStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    NOT_APPLICABLE("Not Applicable");

    private final String displayName;

    ChecklistItemStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
