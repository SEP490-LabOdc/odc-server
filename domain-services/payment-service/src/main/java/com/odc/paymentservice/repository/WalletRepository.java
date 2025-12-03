package com.odc.paymentservice.repository;

import com.odc.paymentservice.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    Optional<Wallet> findByOwnerType(String system);
}