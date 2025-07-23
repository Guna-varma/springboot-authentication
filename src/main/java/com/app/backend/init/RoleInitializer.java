package com.app.backend.init;

import com.app.backend.entity.Role;
import com.app.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        List<String> roles = List.of("ADMIN", "TUTOR", "VOLUNTEER", "REGISTERED_USER", "HEALTHCARE_PROVIDER");

        for (String roleName : roles) {
            roleRepository.findByName(roleName).ifPresentOrElse(
                    r -> {}, // already exists
                    () -> roleRepository.save(new Role(null, roleName))
            );
        }

        System.out.println("Default roles initialized Successfullly.");
    }
}