package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.paymentservice.dto.response.WalletResponse;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;

    @Override
    public ApiResponse<WalletResponse> getMyWallet(UUID userId) {
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ví không tồn tại"));

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
}