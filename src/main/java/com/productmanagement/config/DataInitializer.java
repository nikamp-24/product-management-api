package com.productmanagement.config;

import com.productmanagement.entity.Role;
import com.productmanagement.entity.RoleName;
import com.productmanagement.entity.User;
import com.productmanagement.repository.RoleRepository;
import com.productmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
    }

    private void initRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Initialized role: {}", roleName);
            }
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin") && !userRepository.existsByEmail("admin@example.com")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Initialized default admin user: admin");
        }
    }

}
