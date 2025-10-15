package com.odc.notificationservice.repository;

import com.odc.notificationservice.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByToken(String token);

    boolean existsByToken(String token);

    Optional<List<DeviceToken>> findAllByUserId(UUID userId);

    @Query("select d.token from DeviceToken d where d.userId in :userIds")
    List<String> findDeviceTokensByUserIds(Set<UUID> userIds);

    void deleteAllByTokenIn(List<String> tokens);
}
