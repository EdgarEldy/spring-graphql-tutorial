package com.edgareldy.springgraphqltutorial.config;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with an initial ADMIN and USER role and a bootstrap
 * admin account when the application starts in the dev profile and no roles
 * exist yet. The idempotence guard on roleRepository.count() means subsequent
 * restarts are no-ops.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.save(Role.builder().roleName("ADMIN").build());
        roleRepository.save(Role.builder().roleName("USER").build());

        if (userRepository.count() == 0) {
            User admin = userRepository.save(User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin1234!"))
                    .enabled(true)
                    .accountLocked(false)
                    .build());
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }
    }

}
