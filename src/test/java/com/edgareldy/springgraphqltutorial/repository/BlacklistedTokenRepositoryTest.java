package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.BlacklistedToken;
import com.edgareldy.springgraphqltutorial.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for BlacklistedTokenRepository against a real
 * PostgreSQL 16 container: existsByJti, the method JwtAuthFilter and
 * UserServiceImpl.logout both rely on, and the unique jti constraint from
 * V1__init_schema.sql that guarantees the same token can never be
 * blacklisted twice.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BlacklistedTokenRepositoryTest {

	@Autowired
	private BlacklistedTokenRepository blacklistedTokenRepository;

	@Autowired
	private UserRepository userRepository;

	private User persistUser(String email) {
		return userRepository.save(User.builder()
				.firstName("Ada")
				.lastName("Lovelace")
				.email(email)
				.password("encoded")
				.enabled(true)
				.accountLocked(false)
				.build());
	}

	private BlacklistedToken buildToken(User user, String jti) {
		return BlacklistedToken.builder()
				.user(user)
				.token("raw-jwt-value")
				.jti(jti)
				.blacklistedAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now().minusMinutes(5))
				.expiresAt(LocalDateTime.now().plusHours(1))
				.build();
	}

	@Test
	void existsByJtiReflectsPersistedState() {
		User user = persistUser("ada@example.com");
		blacklistedTokenRepository.save(buildToken(user, "jti-1"));

		assertThat(blacklistedTokenRepository.existsByJti("jti-1")).isTrue();
		assertThat(blacklistedTokenRepository.existsByJti("jti-missing")).isFalse();
	}

	@Test
	void jtiColumnRejectsDuplicates() {
		User user = persistUser("ada@example.com");
		blacklistedTokenRepository.save(buildToken(user, "jti-1"));

		assertThatThrownBy(() -> {
			blacklistedTokenRepository.save(buildToken(user, "jti-1"));
			blacklistedTokenRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
