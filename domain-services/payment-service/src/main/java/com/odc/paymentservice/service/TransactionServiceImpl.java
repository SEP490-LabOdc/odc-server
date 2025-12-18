package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.response.TransactionResponse;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.PaymentRequestRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<TransactionResponse>> getTransactionsByProjectId(
            UUID projectId,
            Pageable pageable) {

        log.info("Getting transactions for projectId: {}", projectId);

        Page<Transaction> transactions = transactionRepository.findByProjectId(projectId, pageable);

        Page<TransactionResponse> responsePage = transactions.map(this::mapToResponse);

        return ApiResponse.success(
                "Lấy danh sách transaction của project thành công",
                responsePage
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<TransactionResponse> getTransactionDetail(UUID transactionId) {
        log.info("Getting transaction detail for transactionId: {}", transactionId);

        Transaction transaction = transactionRepository.findByIdWithPaymentRequest(transactionId)
                .orElseThrow(() -> new BusinessException(
                        "Transaction với ID: '" + transactionId + "' không tồn tại"
                ));

        TransactionResponse response = mapToResponse(transaction);

        return ApiResponse.success("Lấy chi tiết transaction thành công", response);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<TransactionResponse>> getAllTransactions(Pageable pageable) {
        log.info("Getting all transactions");

        Page<Transaction> transactions = transactionRepository.findAllNotDeleted(pageable);

        Page<TransactionResponse> responsePage = transactions.map(this::mapToResponse);

        return ApiResponse.success(
                "Lấy danh sách tất cả transaction thành công",
                responsePage
        );
    }

    @Override
    public ApiResponse<Page<TransactionResponse>> getMyTransactions(Pageable pageable) {

        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() ->
                        new BusinessException("Wallet không tồn tại cho user: " + userId)
                );

        Page<Transaction> transactions =
                transactionRepository.findByWalletId(wallet.getId(), pageable);

        Page<TransactionResponse> responsePage = transactions.map(this::mapToResponse);

        return ApiResponse.success(responsePage);
    }


    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .direction(transaction.getDirection())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .balanceAfter(transaction.getBalanceAfter())
                .walletId(transaction.getWallet() != null ? transaction.getWallet().getId() : null)
                .refId(transaction.getRefId())
                .refType(transaction.getRefType())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt());

        // Nếu transaction liên quan đến PaymentRequest, lấy thông tin project
        if (transaction.getRefId() != null &&
                "PAYMENT_REQUEST".equals(transaction.getRefType())) {

            paymentRequestRepository.findById(transaction.getRefId())
                    .ifPresent(paymentRequest -> {
                        builder.projectId(paymentRequest.getProjectId())
                                .milestoneId(paymentRequest.getMilestoneId())
                                .companyId(paymentRequest.getCompanyId());
                    });
        }

        return builder.build();
    }
}