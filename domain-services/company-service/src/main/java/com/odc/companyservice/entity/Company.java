package com.odc.companyservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", unique = true)
    private String phone;

    @Column(name = "tax_code", unique = true)
    private String taxCode;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website")
    private String website;

    @Column(name = "logo")
    private String logo; // URL to the logo image

    @Column(name = "banner")
    private String banner; // URL to the banner image

    @Column(name = "status")
    private String status; // e.g., "ACTIVE", "INACTIVE", "PENDING_APPROVAL"

    @Column(name = "user_id", nullable = true)
    private UUID userId; // The ID of the user who owns/created this company

    @Column(name = "domain", unique = false)
    private String domain;

    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    @Column(name = "contact_person_email", nullable = false)
    private String contactPersonEmail;

    @Column(name = "contact_person_phone", nullable = false)
    private String contactPersonPhone;

    // If you want to establish a relationship with CompanyDocument
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompanyDocument> documents;
}
