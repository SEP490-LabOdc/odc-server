package com.odc.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {
    }

    public static byte[] toBytes(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return mapper.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize payload", e);
        }
    }
}
