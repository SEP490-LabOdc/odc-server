package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.exception.BusinessException;
import com.odc.commonlib.event.EventPublisher;
import com.odc.payment.v1.PaymentSuccessEvent;
import com.odc.paymentservice.dto.request.CreatePaymentForMilestoneRequest;
import com.odc.paymentservice.dto.request.CreatePaymentRequest;
import com.odc.paymentservice.dto.response.CreatePaymentResponse;
import com.odc.paymentservice.entity.PaymentRequest;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.PaymentRequestRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.projectservice.v1.GetMilestoneByIdRequest;
import com.odc.projectservice.v1.ProjectServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Qualifier("projectServiceChannel")
    private final ManagedChannel projectServiceChannel;

    // --- XỬ LÝ NẠP TIỀN (DEPOSIT) ---

    @Override
    @Transactional
    public CreatePaymentResponse createDepositLink(UUID userId, CreatePaymentRequest req) {
        try {
            long orderCode = System.currentTimeMillis();
            String description = "Nap tien vao vi";

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Nap tien tai khoan")
                    .quantity(1)
                    .price(req.getAmount())
                    .build();

            CreatePaymentLinkRequest payOSRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) req.getAmount())
                    .description(description)
                    .returnUrl(req.getReturnUrl())
                    .cancelUrl(req.getCancelUrl())
                    .item(item)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(payOSRequest);

            // Lưu Payment Request gắn với UserId
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .userId(userId)
                    .orderCode(orderCode)
                    .amount(BigDecimal.valueOf(req.getAmount()))
                    .status(response.getStatus().toString())
                    .checkoutUrl(Status.PENDING.toString())
                    .qrCodeUrl(response.getQrCode())
                    .type(PaymentConstant.DEPOSIT) // Đánh dấu là nạp tiền
                    .build();

            paymentRequestRepository.save(paymentRequest);

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
            log.error("Error creating deposit link: ", e);
            throw new BusinessException("Không thể tạo link nạp tiền: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processWebhook(Webhook webhookBody) {
        try {
            WebhookData data = payOS.webhooks().verify(webhookBody);
            log.info("Webhook verified for OrderCode: {}", data.getOrderCode());

            PaymentRequest request = paymentRequestRepository.findByOrderCode(data.getOrderCode())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng: " + data.getOrderCode()));

            if (Status.PAID.toString().equals(request.getStatus())) {
                return;
            }

            request.setStatus("PAID");
            request.setTransactionRefId(data.getReference());
            paymentRequestRepository.save(request);

            // LOGIC MỚI: Cộng tiền vào ví USER (người nạp)
            if ("DEPOSIT".equals(request.getType())) {
                Wallet userWallet = getOrCreateWallet(request.getUserId());
                BigDecimal amount = BigDecimal.valueOf(data.getAmount());

                // Cộng số dư
                userWallet.setBalance(userWallet.getBalance().add(amount));
                walletRepository.save(userWallet);

                // Lưu lịch sử giao dịch
                createTransaction(userWallet, amount, "DEPOSIT", "CREDIT",
                        "Nạp tiền vào ví", request.getId(), "PAYMENT_REQUEST",
                        null, null, null, null);

                log.info("Deposited {} to wallet of user {}", amount, request.getUserId());
            }
        } catch (Exception e) {
            log.error("Webhook processing error", e);
            throw new BusinessException("Webhook error");
        }
    }

    @Override
    @Transactional
    public void payMilestoneWithWallet(UUID userId, CreatePaymentForMilestoneRequest req) {
        // Validate gRPC
        var stub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        var milestone = stub.getMilestoneById(
                GetMilestoneByIdRequest.newBuilder().setMilestoneId(req.getMilestoneId().toString()).build()
        );

        if (BigDecimal.valueOf(milestone.getAmount()).compareTo(BigDecimal.valueOf(req.getAmount())) != 0) {
            throw new BusinessException("Số tiền thanh toán không khớp với hệ thống!");
        }

        // Trừ tiền ví User (Company)
        Wallet userWallet = getOrCreateWallet(userId);
        BigDecimal amount = BigDecimal.valueOf(req.getAmount());
        if (userWallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Số dư không đủ!");
        }
        userWallet.setBalance(userWallet.getBalance().subtract(amount));
        walletRepository.save(userWallet);

        // Lưu Transaction DEBIT cho Company (Để Company thấy tiền bị trừ)
        createTransaction(userWallet, amount, PaymentConstant.MILESTONE_PAYMENT, PaymentConstant.DEBIT,
                "Thanh toán cho Milestone: " + milestone.getTitle(),
                UUID.fromString(milestone.getId()), PaymentConstant.MILESTONE,
                UUID.fromString(milestone.getProjectId()), UUID.fromString(milestone.getId()), null, userId);

        // Cộng tiền ví System (Giữ tiền)
//        Wallet systemWallet = getOrCreateSystemWallet();
//        systemWallet.setBalance(systemWallet.getBalance().add(amount));
//        walletRepository.save(systemWallet);

        // Lưu Transaction CREDIT cho System (Để Admin audit)
//        createTransaction(systemWallet, amount, "MILESTONE_PAYMENT_HOLDING", "CREDIT",
//                "Giữ tiền thanh toán từ user " + userId,
//                UUID.fromString(milestone.getId()), "MILESTONE",
//                UUID.fromString(milestone.getProjectId()), UUID.fromString(milestone.getId()), null, userId);

        // Cộng tiền vào ví Milestone
        UUID milestoneId = UUID.fromString(milestone.getId());
        Wallet milestoneWallet = getOrCreateMilestoneWallet(milestoneId);

        milestoneWallet.setBalance(milestoneWallet.getBalance().add(amount));
        walletRepository.save(milestoneWallet);

        createTransaction(
                milestoneWallet,
                amount,
                PaymentConstant.MILESTONE_PAYMENT,
                PaymentConstant.CREDIT,
                "Nhận tiền thanh toán milestone",
                milestoneId,
                PaymentConstant.MILESTONE,
                UUID.fromString(milestone.getProjectId()),
                milestoneId,
                null,
                userId
        );

        // Bắn Event cập nhật trạng thái Milestone
        PaymentSuccessEvent event = PaymentSuccessEvent.newBuilder()
                .setMilestoneId(milestone.getId())
                .setProjectId(milestone.getProjectId())
                .setAmount(amount.doubleValue())
                .setPaymentDate(System.currentTimeMillis())
                .build();
        eventPublisher.publish("payment.success", event);
        log.info("Publish event successfully: {}", event);
    }

    // --- HELPERS ---

    private Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByOwnerId(userId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(userId);
                    w.setOwnerType(Role.COMPANY.toString()); // Mặc định là COMPANY nếu là người thanh toán
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByOwnerType("SYSTEM")
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerType("SYSTEM");
                    w.setOwnerId(UUID.randomUUID()); // ID ảo
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String direction,
                                   String desc, UUID refId, String refType,
                                   UUID projectId, UUID milestoneId, UUID companyId, UUID relatedUserId) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .direction(direction)
                .description(desc)
                .refId(refId).refType(refType)
                .projectId(projectId).milestoneId(milestoneId).companyId(companyId).relatedUserId(relatedUserId)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);
    }

    private Wallet getOrCreateMilestoneWallet(UUID milestoneId) {
        return walletRepository
                .findByOwnerId(milestoneId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(milestoneId);
                    w.setOwnerType("MILESTONE");
                    w.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
    }
}