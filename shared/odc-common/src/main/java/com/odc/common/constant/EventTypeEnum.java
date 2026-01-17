package com.odc.common.constant;

// Naming convention: <BOUNDED_CONTEXT>.<AGGREGATE>.<ACTION>.v<version>
// Example: OPERATION.UPDATE_REQUEST.APPROVED.v1
public enum EventTypeEnum {

    /**
     * ===== UPDATE REQUEST LIFECYCLE =====
     */

    // Operation -> Target service
    UPDATE_REQUEST_CREATED("OPERATION.UPDATE_REQUEST.CREATED.v1"),
    UPDATE_REQUEST_APPROVED("OPERATION.UPDATE_REQUEST.APPROVED.v1"),
    UPDATE_REQUEST_REJECTED("OPERATION.UPDATE_REQUEST.REJECTED.v1"),

    /**
     * ===== TARGET SERVICE RESPONSES =====
     */

    // Target service -> Operation
    UPDATE_REQUEST_SNAPSHOT_RESPONSE("TARGET.UPDATE_REQUEST.SNAPSHOT.RESPONSE.v1"),
    UPDATE_REQUEST_APPLIED("TARGET.UPDATE_REQUEST.APPLIED.v1"),
    UPDATE_REQUEST_APPLY_FAILED("TARGET.UPDATE_REQUEST.APPLY_FAILED.v1");

    private final String value;

    EventTypeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
