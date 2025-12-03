package com.odc.paymentservice.service;

import com.odc.common.exception.BusinessException;
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
import vn.payos.PayOS;
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

    @Override
    @Transactional
    public void processWebhook(Webhook webhookBody) {
        try {
            // 1. Verify Webhook Data (v2 API)
            // payOS.webhooks().verify() sẽ throw exception nếu chữ ký không khớp
            WebhookData data = payOS.webhooks().verify(webhookBody);

            log.info("Webhook verified for OrderCode: {}", data.getOrderCode());

            // 2. Tìm Payment Request
            PaymentRequest request = paymentRequestRepository.findByOrderCode(data.getOrderCode())
                    .orElseThrow(() -> new BusinessException("Payment Request not found for order code: " + data.getOrderCode()));

            // 3. Idempotency Check
            if ("PAID".equals(request.getStatus())) {
                log.info("Order {} already paid, skipping.", data.getOrderCode());
                return;
            }

            // 4. Cập nhật trạng thái
            request.setStatus("PAID");
            // Có thể lưu thêm transaction reference nếu PayOS trả về (data.getReference())
            paymentRequestRepository.save(request);

            // 5. Cộng tiền System Wallet
            Wallet systemWallet = getOrCreateSystemWallet();
            BigDecimal amount = BigDecimal.valueOf(data.getAmount());

            systemWallet.setBalance(systemWallet.getBalance().add(amount));
            walletRepository.save(systemWallet);

            // 6. Tạo Transaction Log
            Transaction transaction = Transaction.builder()
                    .wallet(systemWallet)
                    .amount(amount)
                    .type("DEPOSIT")
                    .direction("CREDIT")
                    .description("Thanh toán Milestone Order " + data.getOrderCode())
                    .refId(request.getId())
                    .refType("PAYMENT_REQUEST")
                    .status("SUCCESS")
                    .balanceAfter(systemWallet.getBalance())
                    .build();

            transactionRepository.save(transaction);

            log.info("Payment processed. System Wallet +{}", amount);

            // 7. TODO: Trigger Disbursement (Chia tiền cho Leader)
            // disbursementService.processDisbursement(request);

        } catch (Exception e) {
            log.error("Error verifying/processing webhook", e);
            throw new BusinessException("Webhook failed: " + e.getMessage());
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
