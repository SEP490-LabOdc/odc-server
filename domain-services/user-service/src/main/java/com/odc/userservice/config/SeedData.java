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

        seedRole(com.odc.common.constant.Role.ADMIN.toString(), "Administrator role with full access", createPermissionsForAdmin());
        seedRole(com.odc.common.constant.Role.USER.toString(), "Standard user role with limited access", createPermissionsForUser());

        log.info("Role seeding completed!");
    }

    /**
     * Seed role method
     *
     * @param name
     * @param description
     * @param permissionsMap
     */
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

    private Map<String, Set<String>> createPermissionsForAdmin() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("create", "read", "update", "delete"));
        return permissions;
    }

    private Map<String, Set<String>> createPermissionsForUser() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("read", "update"));
        return permissions;
    }
}
