package com.odc.userservice.repository;

import com.odc.userservice.entity.UserOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserOutboxRepository extends JpaRepository<UserOutbox, UUID> {
}
