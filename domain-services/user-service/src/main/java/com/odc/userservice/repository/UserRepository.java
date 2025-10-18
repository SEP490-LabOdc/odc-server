package com.odc.userservice.repository;

import com.odc.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u.id FROM User u WHERE u.status = :status")
    List<UUID> findAllActiveUserIds(String status);

    @Query("SELECT u.id FROM User u")
    List<UUID> findAllUserIds();

    @Query("SELECT u.id FROM User u JOIN u.role r WHERE r.name IN :roles")
    List<UUID> findUserIdsByRoles(List<String> roles);
}
