package com.odc.paymentservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.user.v1.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserCreatedEventHandler implements EventHandler {

    private final WalletRepository walletRepository;

    @Override
    public String getTopic() {
        return "user.created"; // Phải khớp với topic bên publisher
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            UserCreatedEvent event = ProtobufConverter.deserialize(eventPayload, UserCreatedEvent.parser());
            log.info("Received UserCreatedEvent for userId: {}", event.getUserId());

            UUID ownerId = UUID.fromString(event.getUserId());

            // Kiểm tra xem ví đã tồn tại chưa (Idempotency)
            if (walletRepository.existsByOwnerId(ownerId)) {
                log.warn("Wallet already exists for user: {}", ownerId);
                return;
            }

            // Tạo ví mới
            Wallet wallet = Wallet.builder()
                    .ownerId(ownerId)
                    .ownerType(event.getRole()) // Lưu Role (USER, COMPANY, MENTOR) làm ownerType
                    .balance(BigDecimal.ZERO)
                    .heldBalance(BigDecimal.ZERO)
                    .currency("VND")
                    .status("ACTIVE")
                    .build();

            walletRepository.save(wallet);
            log.info("Successfully initialized wallet for user: {}", ownerId);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to deserialize UserCreatedEvent", e);
        } catch (Exception e) {
            log.error("Error processing UserCreatedEvent", e);
            // Có thể ném exception để Kafka retry nếu cần
        }
    }
}