package com.edgareldy.springgraphqltutorial.config;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with sample data when the application starts in the
 * dev profile. Two independent idempotence guards keep this safe across
 * restarts: roles/users are seeded when roleRepository.count() == 0;
 * categories/products are seeded when categoryRepository.count() == 0.
 * <p>
 * Created by Edgar Muhamyangabo on 11/13/26
 * Author : Edgar Muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
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

        if (categoryRepository.count() == 0) {
            Category electronics = categoryRepository.save(
                    Category.builder().categoryName("Electronics").build());
            Category books = categoryRepository.save(
                    Category.builder().categoryName("Books").build());

            productRepository.save(Product.builder()
                    .category(electronics).productName("Laptop")
                    .unitPrice(new BigDecimal("999.99")).build());
            productRepository.save(Product.builder()
                    .category(electronics).productName("Wireless Mouse")
                    .unitPrice(new BigDecimal("29.99")).build());
            productRepository.save(Product.builder()
                    .category(books).productName("Learning Java")
                    .unitPrice(new BigDecimal("49.99")).build());
        }
    }

}
