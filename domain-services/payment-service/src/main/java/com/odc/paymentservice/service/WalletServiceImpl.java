package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.request.CreateWithdrawalRequest;
import com.odc.paymentservice.dto.request.UpdateBankInfoRequest;
import com.odc.paymentservice.dto.response.SystemWalletStatisticResponse;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import com.odc.paymentservice.entity.BankInfo;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.entity.WithdrawalRequest;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.paymentservice.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public ApiResponse<WalletResponse> getMyWallet(UUID userId) {
        // Tìm ví, nếu không thấy thì tạo mới
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseGet(() -> createDefaultWallet(userId));

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .ownerId(wallet.getOwnerId())
                .ownerType(wallet.getOwnerType())
                .balance(wallet.getBalance())
                .heldBalance(wallet.getHeldBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .bankInfos(wallet.getBankInfos())
                .build();

        return ApiResponse.success("Lấy thông tin ví thành công", response);
    }

    @Override
    @Transactional
    public ApiResponse<WalletResponse> addBankInfo(UUID userId, UpdateBankInfoRequest request) {
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() -> new BusinessException("Ví không tồn tại. Vui lòng liên hệ quản trị viên."));

        if (Status.LOCKED.toString().equals(wallet.getStatus())) {
            throw new BusinessException("Ví của bạn đã bị khóa");
        }

        List<BankInfo> currentBankInfos = wallet.getBankInfos();
        if (currentBankInfos == null) {
            currentBankInfos = new ArrayList<>();
        } else {
            currentBankInfos = new ArrayList<>(currentBankInfos);
        }

        boolean exists = currentBankInfos.stream().anyMatch(info ->
                info.getAccountNumber().equals(request.getAccountNumber()) &&
                        info.getBankName().equalsIgnoreCase(request.getBankName()));

        if (exists) {
            throw new BusinessException("Tài khoản ngân hàng này đã tồn tại trong danh sách của bạn");
        }

        // Thêm mới
        BankInfo newBankInfo = new BankInfo(
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolderName().toUpperCase()
        );
        currentBankInfos.add(newBankInfo);

        wallet.setBankInfos(currentBankInfos);
        Wallet savedWallet = walletRepository.save(wallet);

        WalletResponse response = WalletResponse.builder()
                .bankInfos(savedWallet.getBankInfos())
                .build();

        return ApiResponse.success("Thêm thông tin ngân hàng thành công", response);
    }

    @Override
    @Transactional
    public ApiResponse<WithdrawalResponse> createWithdrawalRequest(UUID userId, CreateWithdrawalRequest request) {
        // Bước 1: Lấy thông tin Wallet của user
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy ví của người dùng"));

        // Kiểm tra ví bị khóa
        if (Status.LOCKED.toString().equals(wallet.getStatus())) {
            throw new BusinessException("Ví của bạn đã bị khóa, không thể thực hiện rút tiền");
        }

        // Bước 2: Kiểm tra số dư khả dụng
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Số dư không đủ để thực hiện yêu cầu rút tiền");
        }

        // Bước 3: Cập nhật số dư ví (Transaction)
        BigDecimal withdrawalAmount = request.getAmount();
        wallet.setBalance(wallet.getBalance().subtract(withdrawalAmount));
        wallet.setHeldBalance(wallet.getHeldBalance().add(withdrawalAmount));
        walletRepository.save(wallet);

        // Bước 4: Tạo và lưu WithdrawalRequest
        Map<String, String> bankInfo = new HashMap<>();
        bankInfo.put("bankName", request.getBankName());
        bankInfo.put("accountNumber", request.getAccountNumber());
        bankInfo.put("accountName", request.getAccountName());

        // Tính scheduledAt: ngày 15 hàng tháng
        LocalDate scheduledAt = calculateScheduledAt();

        WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
                .userId(userId)
                .wallet(wallet)
                .amount(withdrawalAmount)
                .bankInfo(bankInfo)
                .status(Status.PENDING.toString())
                .scheduledAt(scheduledAt)
                .build();

        withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);

        // Bước 5: Tạo Transaction để ghi log
        createTransaction(
                wallet,
                withdrawalAmount,
                "WITHDRAWAL",
                PaymentConstant.DEBIT,
                "Yêu cầu rút tiền",
                withdrawalRequest.getId(),
                "WITHDRAWAL_REQUEST",
                null,
                null,
                null,
                null
        );

        log.info("Created withdrawal request {} for user {}", withdrawalRequest.getId(), userId);

        // Tạo response
        WithdrawalResponse response = WithdrawalResponse.builder()
                .id(withdrawalRequest.getId())
                .userId(withdrawalRequest.getUserId())
                .walletId(withdrawalRequest.getWallet().getId())
                .amount(withdrawalRequest.getAmount())
                .bankInfo(withdrawalRequest.getBankInfo())
                .status(withdrawalRequest.getStatus())
                .adminNote(withdrawalRequest.getAdminNote())
                .scheduledAt(withdrawalRequest.getScheduledAt())
                .processedAt(withdrawalRequest.getProcessedAt())
                .createdAt(withdrawalRequest.getCreatedAt())
                .updatedAt(withdrawalRequest.getUpdatedAt())
                .build();

        return ApiResponse.success("Tạo yêu cầu rút tiền thành công", response);
    }

    @Override
    public ApiResponse<SystemWalletStatisticResponse> getSystemWalletStatistic() {
        Wallet systemWallet = walletRepository
                .findByOwnerType(Role.SYSTEM.toString())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ví hệ thống"));

        BigDecimal totalRevenue =
                transactionRepository.sumTotalRevenue(systemWallet.getId());

        SystemWalletStatisticResponse response =
                SystemWalletStatisticResponse.builder()
                        .currentBalance(systemWallet.getBalance())
                        .totalRevenue(totalRevenue)
                        .build();

        return ApiResponse.success(response);
    }

    /**
     * Tính ngày scheduledAt: ngày 15 hàng tháng
     * Nếu hôm nay đã qua ngày 15 thì lấy ngày 15 tháng sau
     * Nếu chưa qua ngày 15 thì lấy ngày 15 tháng này
     */
    private LocalDate calculateScheduledAt() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        if (currentDay >= 15) {
            // Đã qua ngày 15, lấy ngày 15 tháng sau
            if (currentMonth == 12) {
                return LocalDate.of(currentYear + 1, 1, 15);
            } else {
                return LocalDate.of(currentYear, currentMonth + 1, 15);
            }
        } else {
            // Chưa qua ngày 15, lấy ngày 15 tháng này
            return LocalDate.of(currentYear, currentMonth, 15);
        }
    }

    /**
     * Helper method để tạo Transaction
     */
    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String direction,
                                   String desc, UUID refId, String refType,
                                   UUID projectId, UUID milestoneId, UUID companyId, UUID relatedUserId) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .direction(direction)
                .description(desc)
                .refId(refId)
                .refType(refType)
                .projectId(projectId)
                .milestoneId(milestoneId)
                .companyId(companyId)
                .relatedUserId(relatedUserId)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);
    }

    private Wallet createDefaultWallet(UUID userId) {
        log.info("Creating default wallet for user: {}", userId);

        // Lấy Role từ SecurityContext để set cho ownerType
        String ownerType = Role.USER.toString(); // Giá trị mặc định
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            // Lấy role đầu tiên tìm thấy (ví dụ: "MENTOR", "COMPANY")
            ownerType = authentication.getAuthorities().iterator().next().getAuthority();
        }

        Wallet newWallet = Wallet.builder()
                .ownerId(userId)
                .ownerType(ownerType)
                .balance(BigDecimal.ZERO)
                .heldBalance(BigDecimal.ZERO)
                .currency("VND")
                .status(Status.ACTIVE.toString())
                .build();

        return walletRepository.save(newWallet);
    }
}