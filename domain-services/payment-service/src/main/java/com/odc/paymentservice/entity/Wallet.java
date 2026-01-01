package com.odc.paymentservice.entity;

import com.odc.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_owner", columnList = "owner_id")
})
public class Wallet extends BaseEntity {
    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId; // userId hoặc companyId

    @Column(name = "owner_type", nullable = false)
    private String ownerType; // USER, COMPANY, SYSTEM

    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Số dư khả dụng

    @Column(name = "held_balance", precision = 19, scale = 2)
    private BigDecimal heldBalance = BigDecimal.ZERO; // Tiền đang bị giữ (chờ rút)

    @Column(name = "currency")
    @Builder.Default
    private String currency = "VND"; // "VND"

    @Column(name = "status")
    private String status; // ACTIVE, LOCKED

    @Type(JsonType.class)
    @Column(name = "bank_infos", columnDefinition = "jsonb")
    private List<BankInfo> bankInfos = new ArrayList<>();
}
