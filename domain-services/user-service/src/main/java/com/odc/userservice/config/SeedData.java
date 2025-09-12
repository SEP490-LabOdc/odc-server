package com.odc.userservice.config;


import com.odc.userservice.entity.Role;
import com.odc.userservice.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SeedData implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRole("Admin", "Administrator role with full access", createPermissionsForAdmin());
    }

    /**
     * Seed role method
     * @param name
     * @param description
     * @param permissionsMap
     */
    private void seedRole(String name, String description, Map<String, Set<String>> permissionsMap) {
        if(!roleRepository.existsByName(name)) {
            Role role = new Role();

            role.setName(name);
            role.setDescription(description);
            role.setPermissions(permissionsMap);

            roleRepository.save(role);
        }
    }

    private Map<String, Set<String>> createPermissionsForAdmin() {
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("users", Set.of("create", "read", "update", "delete"));
        return permissions;
    }
}
