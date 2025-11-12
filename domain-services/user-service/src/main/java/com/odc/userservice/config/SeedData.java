package com.odc.userservice.config;

import com.odc.userservice.entity.Role;
import com.odc.userservice.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SeedData implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting to seed roles...");

        seedRole(com.odc.common.constant.Role.SYSTEM_ADMIN.toString(),
                "System Administrator role with full access",
                createPermissionsForSystemAdmin());

        seedRole(com.odc.common.constant.Role.LAB_ADMIN.toString(),
                "Lab Administrator role with management privileges in assigned labs",
                createPermissionsForLabAdmin());

        seedRole(com.odc.common.constant.Role.SUPERVISOR.toString(),
                "Supervisor role for overseeing users and monitoring tasks",
                createPermissionsForSupervisor());

        seedRole(com.odc.common.constant.Role.USER.toString(),
                "Standard user role with limited access",
                createPermissionsForUser());

        seedRole(com.odc.common.constant.Role.COMPANY.toString(),
                "Company role for managing job postings and reviewing candidates",
                createPermissionsForCompany());

        seedRole(com.odc.common.constant.Role.MENTOR.toString(),
                "Mentor role for managing talents and projects",
                createPermissionsForMentor());

        log.info("Role seeding completed!");
    }

    private void seedRole(String name, String description, Map<String, Set<String>> permissionsMap) {
        if (!roleRepository.existsByName(name)) {
            log.info("Creating role: {}", name);
            Role role = new Role();

            role.setName(name);
            role.setDescription(description);
            role.setPermissions(permissionsMap);

            roleRepository.save(role);
            log.info("Role {} created successfully", name);
        } else {
            log.info("Role {} already exists, skipping", name);
        }
    }

    // ================= Permission sets =================

    private Map<String, Set<String>> createPermissionsForSystemAdmin() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("create", "read", "update", "delete"));
        permissions.put("labs", Set.of("create", "read", "update", "delete"));
        permissions.put("roles", Set.of("create", "read", "update", "delete"));
        permissions.put("permissions", Set.of("read", "update"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForLabAdmin() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("labs", Set.of("read", "update"));
        permissions.put("users", Set.of("read"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForSupervisor() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("read"));
        permissions.put("reports", Set.of("create", "read", "update"));
        permissions.put("tasks", Set.of("read", "update"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForMentor() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("read"));
        permissions.put("reports", Set.of("create", "read", "update"));
        permissions.put("tasks", Set.of("read", "update"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForUser() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("read", "update"));
        permissions.put("tasks", Set.of("read"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForCompany() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("jobs", Set.of("create", "read", "update", "delete"));
        permissions.put("applications", Set.of("read", "update"));
        permissions.put("company-profile", Set.of("read", "update"));
        return permissions;
    }
}
