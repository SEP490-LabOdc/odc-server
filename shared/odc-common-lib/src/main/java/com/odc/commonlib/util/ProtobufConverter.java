package com.odc.commonlib.util;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

public class ProtobufConverter {
    public static <T extends GeneratedMessageV3> byte[] serialize(T message) {
        return message.toByteArray();
    }

    public static <T extends GeneratedMessageV3> T deserialize(byte[] data, Parser<T> parser) throws InvalidProtocolBufferException {
        return parser.parseFrom(data);
    }
}
