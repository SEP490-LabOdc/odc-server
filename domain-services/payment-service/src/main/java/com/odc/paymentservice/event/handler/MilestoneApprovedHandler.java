package com.odc.paymentservice.event.handler;

import com.odc.commonlib.event.EventHandler;
import com.odc.commonlib.util.ProtobufConverter;
import com.odc.paymentservice.entity.PaymentRequest;
import com.odc.paymentservice.repository.PaymentRequestRepository;
import com.odc.project.v1.MilestoneApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MilestoneApprovedHandler implements EventHandler {

    private final PayOS payOS;
    private final PaymentRequestRepository paymentRequestRepository;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Override
    public String getTopic() {
        return "project.milestone.approved";
    }

    @Override
    @Transactional
    public void handle(byte[] eventPayload) {
        try {
            MilestoneApprovedEvent event = ProtobufConverter.deserialize(eventPayload, MilestoneApprovedEvent.parser());
            log.info("Processing MilestoneApprovedEvent for milestone: {}", event.getMilestoneId());

            // 1. Tạo OrderCode (Unique)
            long orderCode = System.currentTimeMillis();
            long amount = (long) event.getAmount();

            // Lưu ý: description của PayOS có giới hạn ký tự và không nên chứa ký tự đặc biệt
            String description = "Thanh toan Milestone";

            // 2. Tạo Item (v2)
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(event.getMilestoneTitle())
                    .quantity(1)
                    .price(amount)
                    .build();

            // 3. Tạo Request (v2)
            CreatePaymentLinkRequest payOSRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item) // Thêm item vào danh sách
                    .build();

            // 4. Gọi PayOS API (v2)
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(payOSRequest);

            // 5. Lưu thông tin vào DB
            PaymentRequest request = PaymentRequest.builder()
                    .orderCode(orderCode)
                    .amount(BigDecimal.valueOf(amount))
                    .milestoneId(UUID.fromString(event.getMilestoneId()))
                    .projectId(UUID.fromString(event.getProjectId()))
                    .companyId(UUID.fromString(event.getCompanyId()))
                    .status(response.getStatus().name()) // Thường là "PENDING"
                    .checkoutUrl(response.getCheckoutUrl())
                    .qrCodeUrl(response.getQrCode())
                    .build();

            paymentRequestRepository.save(request);

            log.info("Payment Link Created: {}", response.getCheckoutUrl());

            // TODO: Bắn Notification cho Company

        } catch (Exception e) {
            log.error("Error creating PayOS payment link", e);
        }
    }
}