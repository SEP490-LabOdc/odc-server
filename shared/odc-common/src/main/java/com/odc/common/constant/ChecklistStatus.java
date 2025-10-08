package com.odc.common.constant;

public enum ChecklistStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    INVALID("Invalid");

    private final String displayName;

    ChecklistStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
