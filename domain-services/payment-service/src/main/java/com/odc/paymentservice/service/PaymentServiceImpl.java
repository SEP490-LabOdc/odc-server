package com.odc.paymentservice.service;

import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.payment.v1.PaymentSuccessEvent;
import com.odc.paymentservice.dto.request.CreatePaymentRequest;
import com.odc.paymentservice.dto.response.CreatePaymentResponse;
import com.odc.paymentservice.entity.PaymentRequest;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.PaymentRequestRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void processWebhook(Webhook webhookBody) {
        try {
            // 1. Verify Webhook (v2)
            WebhookData data = payOS.webhooks().verify(webhookBody);
            log.info("Webhook verified for OrderCode: {}", data.getOrderCode());

            // 2. Tìm Payment Request
            PaymentRequest request = paymentRequestRepository.findByOrderCode(data.getOrderCode())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng: " + data.getOrderCode()));

            // 3. Idempotency Check
            if ("PAID".equals(request.getStatus())) {
                return;
            }

            // 4. Update DB
            request.setStatus("PAID");
            request.setTransactionRefId(data.getReference());
            paymentRequestRepository.save(request);

            // 5. Cộng tiền System Wallet
            Wallet systemWallet = getOrCreateSystemWallet();
            BigDecimal amount = BigDecimal.valueOf(data.getAmount());
            systemWallet.setBalance(systemWallet.getBalance().add(amount));
            walletRepository.save(systemWallet);

            // 6. Log Transaction
            Transaction transaction = Transaction.builder()
                    .wallet(systemWallet)
                    .amount(amount)
                    .type("DEPOSIT")
                    .direction("CREDIT")
                    .description("Thanh toán Order " + data.getOrderCode())
                    .refId(request.getId())
                    .refType("PAYMENT_REQUEST")
                    .status("SUCCESS")
                    .balanceAfter(systemWallet.getBalance())
                    .build();
            transactionRepository.save(transaction);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        PaymentSuccessEvent event = PaymentSuccessEvent.newBuilder()
                                .setMilestoneId(request.getMilestoneId().toString())
                                .setProjectId(request.getProjectId().toString())
                                .setAmount(amount.doubleValue())
                                .setOrderCode(String.valueOf(data.getOrderCode()))
                                .setTransactionRefId(data.getReference() != null ? data.getReference() : "")
                                .setPaymentDate(System.currentTimeMillis())
                                .build();

                        eventPublisher.publish("payment.success", event);
                        log.info("Published PaymentSuccessEvent for milestone {}", request.getMilestoneId());
                    } catch (Exception e) {
                        // Nếu gửi Kafka thất bại ở đây, DB đã commit rồi.
                        // Đây là điểm yếu của cách này so với Outbox Pattern.
                        // Tuy nhiên log error ra để có thể retry thủ công hoặc qua job quét.
                        log.error("Failed to publish event after commit", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Webhook processing error", e);
            throw new BusinessException("Webhook error");
        }
    }

    @Override
    @Transactional
    public CreatePaymentResponse createPaymentLink(CreatePaymentRequest req) {
        try {
            // 1. Tạo OrderCode duy nhất (Dùng timestamp để đơn giản)
            long orderCode = System.currentTimeMillis();

            // Giới hạn độ dài description của PayOS
            String description = "Thanh toan Milestone";

            // 2. Tạo Item (v2)
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(req.getMilestoneTitle() != null ? req.getMilestoneTitle() : "Milestone Payment")
                    .quantity(1)
                    .price(req.getAmount())
                    .build();

            // 3. Tạo Request PayOS (v2)
            CreatePaymentLinkRequest payOSRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) req.getAmount())
                    .description(description)
                    .returnUrl(req.getReturnUrl())
                    .cancelUrl(req.getCancelUrl())
                    .item(item)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(payOSRequest);

            // 5. Lưu DB
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderCode(orderCode)
                    .amount(BigDecimal.valueOf(req.getAmount()))
                    .milestoneId(req.getMilestoneId())
                    .projectId(req.getProjectId())
                    .companyId(req.getCompanyId())
                    .status(response.getStatus().toString()) // "PENDING"
                    .checkoutUrl(response.getCheckoutUrl())
                    .qrCodeUrl(response.getQrCode())
                    .build();

            paymentRequestRepository.save(paymentRequest);

            // 6. Trả về Response cho FE
            return CreatePaymentResponse.builder()
                    .bin(response.getBin())
                    .accountNumber(response.getAccountNumber())
                    .orderCode(response.getOrderCode())
                    .amount(response.getAmount().intValue())
                    .description(response.getDescription())
                    .checkoutUrl(response.getCheckoutUrl())
                    .qrCode(response.getQrCode())
                    .status(response.getStatus().toString())
                    .build();

        } catch (Exception e) {
            log.error("Error creating payment link: ", e);
            throw new BusinessException("Không thể tạo link thanh toán: " + e.getMessage());
        }
    }

    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByOwnerType("SYSTEM")
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerType("SYSTEM");
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus("ACTIVE");
                    w.setOwnerId(UUID.randomUUID()); // Hoặc ID cố định
                    return walletRepository.save(w);
                });
    }
}
