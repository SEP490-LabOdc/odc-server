package com.odc.common.constant;

public enum Status {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    CANCELED("Cancelled"),
    REVIEWING("Reviewing"),;

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
