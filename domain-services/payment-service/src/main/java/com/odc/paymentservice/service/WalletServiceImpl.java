package com.odc.paymentservice.service;

import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;

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
                .build();

        return ApiResponse.success("Lấy thông tin ví thành công", response);
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