package com.odc.emailservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.emailservice.service.OtpService;
import com.odc.notification.v1.SendOtpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendOtpEventHandler implements EventHandler {
    private final OtpService otpService;

    @Override
    public String getTopic() {
        return "email.otp.company_verification";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            SendOtpRequest request = ProtobufConverter.deserialize(eventPayload, SendOtpRequest.parser());
            log.info("consume email received : {}", request.getEmail());
            otpService.sendOtpRequest(request.getEmail());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
