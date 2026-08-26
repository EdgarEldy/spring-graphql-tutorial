package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.PasswordResetToken;
import com.edgareldy.springgraphqltutorial.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for PasswordResetTokenRepository against a real
 * PostgreSQL 16 container: findByToken and the unique token constraint
 * from V1__init_schema.sql.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PasswordResetTokenRepositoryTest {

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

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

	@Test
	void findByTokenReturnsPersistedToken() {
		User user = persistUser("ada@example.com");
		passwordResetTokenRepository.save(PasswordResetToken.builder()
				.user(user)
				.token("reset-token")
				.type("PASSWORD_RESET")
				.expiryDate(LocalDateTime.now().plusHours(1))
				.build());

		Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("reset-token");

		assertThat(found).isPresent();
		assertThat(found.get().getUser().getEmail()).isEqualTo("ada@example.com");
	}

	@Test
	void findByTokenReturnsEmptyWhenNoMatch() {
		assertThat(passwordResetTokenRepository.findByToken("missing")).isEmpty();
	}

	@Test
	void tokenColumnRejectsDuplicates() {
		User first = persistUser("first@example.com");
		User second = persistUser("second@example.com");
		passwordResetTokenRepository.save(PasswordResetToken.builder()
				.user(first)
				.token("shared-token")
				.type("PASSWORD_RESET")
				.expiryDate(LocalDateTime.now().plusHours(1))
				.build());

		assertThatThrownBy(() -> {
			passwordResetTokenRepository.save(PasswordResetToken.builder()
					.user(second)
					.token("shared-token")
					.type("PASSWORD_RESET")
					.expiryDate(LocalDateTime.now().plusHours(1))
					.build());
			passwordResetTokenRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
