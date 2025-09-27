package com.odc.userservice.entity;

import com.odc.common.constant.Gender;
import com.odc.common.constant.Status;
import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    public Gender gender;
    @Column(name = "birth_date")
    public LocalDate birthDate;
    @Column(name = "last_login_at")
    public LocalDateTime lastLoginAt;
    @Column(name = "failed_login_attempts")
    public Integer failedLoginAttempts;
    @Column(name = "avatar_url")
    public String avatarUrl;
    @Column(unique = true, nullable = false)
    @Email
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(length = 30)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Status status;
    @Column
    private String address;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
