package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.request.AdminHandleWithdrawalRequest;
import com.odc.paymentservice.dto.request.WithdrawalFilterRequest;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.entity.WithdrawalRequest;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.paymentservice.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalAdminServiceImpl implements WithdrawalAdminService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<WithdrawalResponse>> list(WithdrawalFilterRequest filter) {
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize());
        LocalDateTime from = filter.getFromDate() == null ? null : LocalDateTime.parse(filter.getFromDate() + "T00:00:00");
        LocalDateTime to = filter.getToDate() == null ? null : LocalDateTime.parse(filter.getToDate() + "T23:59:59");
        Page<WithdrawalRequest> page = withdrawalRequestRepository.search(
                emptyToNull(filter.getStatus()), from, to, pageable);
        return ApiResponse.success("Danh sách yêu cầu rút", page.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<WithdrawalResponse> detail(UUID id) {
        WithdrawalRequest wr = getOrThrow(id);
        return ApiResponse.success("Chi tiết yêu cầu rút", mapToResponse(wr));
    }

    @Override
    @Transactional
    public ApiResponse<WithdrawalResponse> approve(UUID id, AdminHandleWithdrawalRequest req) {
        WithdrawalRequest wr = getOrThrow(id);
        if (!Status.PENDING.toString().equals(wr.getStatus())) {
            throw new BusinessException("Trạng thái không hợp lệ để duyệt");
        }
        Wallet wallet = wr.getWallet();
        BigDecimal amount = wr.getAmount();

        // Giảm heldBalance (balance đã trừ khi create)
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));
        walletRepository.save(wallet);

        wr.setStatus(Status.APPROVED.toString());
        wr.setAdminNote(req.getAdminNote());
        wr.setProcessedAt(req.getProcessedAt() != null
                ? LocalDateTime.parse(req.getProcessedAt())
                : LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        // Log chi trả
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(PaymentConstant.WITHDRAWAL)
                .direction(PaymentConstant.DEBIT)
                .description("Rút tiền đã duyệt")
                .refId(wr.getId())
                .refType(PaymentConstant.WITHDRAWAL)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);

        return ApiResponse.success("Duyệt rút tiền thành công", mapToResponse(wr));
    }

    @Override
    @Transactional
    public ApiResponse<WithdrawalResponse> reject(UUID id, AdminHandleWithdrawalRequest req) {
        WithdrawalRequest wr = getOrThrow(id);
        if (!"PENDING".equals(wr.getStatus())) {
            throw new BusinessException("Trạng thái không hợp lệ để từ chối");
        }
        Wallet wallet = wr.getWallet();
        BigDecimal amount = wr.getAmount();

        // Hoàn tiền: trả từ heldBalance về balance
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        wr.setStatus(Status.REJECTED.toString());
        wr.setAdminNote(req.getAdminNote());
        wr.setProcessedAt(req.getProcessedAt() != null
                ? LocalDateTime.parse(req.getProcessedAt())
                : LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        // Log hoàn tiền
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(PaymentConstant.WITHDRAWAL)
                .direction(PaymentConstant.CREDIT)
                .description("Hoàn tiền do từ chối rút")
                .refId(wr.getId())
                .refType(PaymentConstant.WITHDRAWAL)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);

        return ApiResponse.success("Từ chối rút tiền thành công", mapToResponse(wr));
    }

    private WithdrawalRequest getOrThrow(UUID id) {
        return withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu rút tiền"));
    }

    private WithdrawalResponse mapToResponse(WithdrawalRequest wr) {
        return WithdrawalResponse.builder()
                .id(wr.getId())
                .userId(wr.getUserId())
                .walletId(wr.getWallet().getId())
                .amount(wr.getAmount())
                .bankInfo(wr.getBankInfo())
                .status(wr.getStatus())
                .adminNote(wr.getAdminNote())
                .scheduledAt(wr.getScheduledAt())
                .processedAt(wr.getProcessedAt())
                .createdAt(wr.getCreatedAt())
                .updatedAt(wr.getUpdatedAt())
                .build();
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}