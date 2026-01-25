package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.exception.BusinessException;
import com.odc.common.util.DateTimeUtil;
import com.odc.paymentservice.dto.request.AdminHandleWithdrawalRequest;
import com.odc.paymentservice.dto.request.WithdrawalFilterRequest;
import com.odc.paymentservice.dto.response.WithdrawalResponse;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.entity.WithdrawalRequest;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.paymentservice.repository.WithdrawalRequestRepository;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalAdminServiceImpl implements WithdrawalAdminService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ManagedChannel userServiceChannel1;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PaginatedResult<WithdrawalResponse>> list(WithdrawalFilterRequest filter) {
        int normalizedPage = filter.getPage() == null || filter.getPage() <= 0
                ? 0
                : filter.getPage() - 1;

        int normalizedSize = filter.getSize() == null || filter.getSize() <= 0
                ? 20
                : filter.getSize();

        LocalDateTime from = filter.getFromDate() == null ? null : LocalDateTime.parse(filter.getFromDate() + "T00:00:00");
        LocalDateTime to = filter.getToDate() == null ? null : LocalDateTime.parse(filter.getToDate() + "T23:59:59");

        PageRequest pageable = PageRequest.of(normalizedPage, normalizedSize);
        Page<WithdrawalRequest> page = withdrawalRequestRepository.search(
                emptyToNull(filter.getStatus()), from, to, pageable);

        List<UUID> userIds = page.getContent().stream()
                .map(WithdrawalRequest::getUserId)
                .distinct()
                .toList();

        Map<String, UserInfo> userMap = userIds.isEmpty()
                ? Map.of()
                : getMapUserInfo(userIds);

        Page<WithdrawalResponse> responsePage = page.map(wr ->
                mapToResponse(wr, userMap.get(wr.getUserId().toString()))
        );

        return ApiResponse.success(
                "Danh sách yêu cầu rút",
                PaginatedResult.from(responsePage)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<WithdrawalResponse> detail(UUID id) {
        WithdrawalRequest wr = getOrThrow(id);
        Map<String, UserInfo> userMap = getMapUserInfo(List.of(wr.getUserId()));
        UserInfo userInfo = userMap.get(wr.getUserId().toString());

        return ApiResponse.success("Chi tiết yêu cầu rút", mapToResponse(wr, userInfo));
    }

    @Override
    @Transactional
    public ApiResponse<WithdrawalResponse> approve(UUID id, AdminHandleWithdrawalRequest req) {
        WithdrawalRequest wr = getOrThrow(id);
        if (!Status.PENDING.toString().equals(wr.getStatus())) {
            throw new BusinessException("Trạng thái không hợp lệ để duyệt");
        }

        SystemConfig config = systemConfigRepository.findByName(
                        PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình: " + PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME));
        ;

        String cronExpression = null;
        if (config.getProperties() != null) {
            cronExpression = (String) config.getProperties().get(PaymentConstant.SYSTEM_CONFIG_CRON_EXPRESSION_KEY);
        }

        Wallet wallet = wr.getWallet();
        BigDecimal amount = wr.getAmount();

        // Giảm heldBalance (balance đã trừ khi create)
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount));
        walletRepository.save(wallet);

        wr.setStatus(Status.APPROVED.toString());
        wr.setAdminNote(req.getAdminNote());
        LocalDate scheduledDate = DateTimeUtil.calculateNextScheduledDate(cronExpression);
        wr.setScheduledAt(scheduledDate);

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

        Map<String, UserInfo> userMap = getMapUserInfo(List.of(wr.getUserId()));
        UserInfo userInfo = userMap.get(wr.getUserId().toString());

        return ApiResponse.success("Duyệt rút tiền thành công", mapToResponse(wr, userInfo));
    }

    @Override
    @Transactional
    public ApiResponse<WithdrawalResponse> reject(UUID id, AdminHandleWithdrawalRequest req) {
        WithdrawalRequest wr = getOrThrow(id);
        if (!Status.PENDING.toString().equals(wr.getStatus())) {
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
        withdrawalRequestRepository.save(wr);

        // Log hoàn tiền
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(PaymentConstant.WITHDRAWAL)
                .direction(PaymentConstant.CREDIT)
                .description("Hoàn tiền do từ chối rút")
                .refId(wr.getId())
                .refType(PaymentConstant.WITHDRAWAL_REQUEST)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);

        Map<String, UserInfo> userMap = getMapUserInfo(List.of(wr.getUserId()));
        UserInfo userInfo = userMap.get(wr.getUserId().toString());

        return ApiResponse.success("Từ chối rút tiền thành công", mapToResponse(wr, userInfo));
    }

    private WithdrawalRequest getOrThrow(UUID id) {
        return withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu rút tiền"));
    }

    private WithdrawalResponse mapToResponse(WithdrawalRequest wr, UserInfo userInfo) {
        return WithdrawalResponse.builder()
                .id(wr.getId())
                .userId(wr.getUserId())
                .email(userInfo.getEmail())
                .avatarUrl(userInfo.getAvatarUrl())
                .fullName(userInfo.getFullName())
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

    private Map<String, UserInfo> getMapUserInfo(List<UUID> userIds) {
        Map<String, UserInfo> userIdToUserInfoMap = new HashMap<>();
        List<String> allUserIds = userIds.stream()
                .map(UUID::toString)
                .distinct()
                .toList();

        UserServiceGrpc.UserServiceBlockingStub userStub = UserServiceGrpc.newBlockingStub(userServiceChannel1);
        GetUsersByIdsResponse usersResponse = userStub.getUsersByIds(
                GetUsersByIdsRequest.newBuilder()
                        .addAllUserId(allUserIds)
                        .build()
        );

        log.debug("[USER_SERVICE][GET_USERS] size={}", allUserIds.size());

        userIdToUserInfoMap = usersResponse.getUsersList().stream()
                .collect(Collectors.toMap(
                        UserInfo::getUserId,
                        Function.identity(),
                        (v1, v2) -> v1
                ));

        return userIdToUserInfoMap;
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}