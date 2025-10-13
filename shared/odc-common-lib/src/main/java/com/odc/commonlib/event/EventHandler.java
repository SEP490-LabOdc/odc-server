package com.odc.commonlib.event;

public interface EventHandler {
    String getTopic();
    void handle(byte[] eventPayload);
}
