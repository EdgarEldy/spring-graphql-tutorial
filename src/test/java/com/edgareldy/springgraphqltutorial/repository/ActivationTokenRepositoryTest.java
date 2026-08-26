package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.ActivationToken;
import com.edgareldy.springgraphqltutorial.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for ActivationTokenRepository against a real
 * PostgreSQL 16 container: findByToken, the unique token constraint, the
 * NOT NULL user_id foreign key from V1__init_schema.sql, and the LAZY
 * User association (accessing it after clearing the persistence context
 * triggers a second SELECT instead of being loaded eagerly with the
 * token).
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ActivationTokenRepositoryTest {

	@Autowired
	private ActivationTokenRepository activationTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User persistUser(String email) {
		return userRepository.save(User.builder()
				.firstName("Ada")
				.lastName("Lovelace")
				.email(email)
				.password("encoded")
				.enabled(false)
				.accountLocked(false)
				.build());
	}

	@Test
	void findByTokenReturnsPersistedToken() {
		User user = persistUser("ada@example.com");
		activationTokenRepository.save(ActivationToken.builder()
				.user(user)
				.token("activation-token")
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(24))
				.build());

		Optional<ActivationToken> found = activationTokenRepository.findByToken("activation-token");

		assertThat(found).isPresent();
		assertThat(found.get().getUser().getEmail()).isEqualTo("ada@example.com");
	}

	@Test
	void findByTokenReturnsEmptyWhenNoMatch() {
		assertThat(activationTokenRepository.findByToken("missing")).isEmpty();
	}

	@Test
	void tokenColumnRejectsDuplicates() {
		User first = persistUser("first@example.com");
		User second = persistUser("second@example.com");
		activationTokenRepository.save(ActivationToken.builder()
				.user(first)
				.token("shared-token")
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(24))
				.build());

		assertThatThrownBy(() -> {
			activationTokenRepository.save(ActivationToken.builder()
					.user(second)
					.token("shared-token")
					.createdAt(LocalDateTime.now())
					.expiresAt(LocalDateTime.now().plusHours(24))
					.build());
			activationTokenRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void userAssociationIsLazyAndResolvedOnDemand() {
		User user = persistUser("ada@example.com");
		ActivationToken saved = activationTokenRepository.save(ActivationToken.builder()
				.user(user)
				.token("activation-token")
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(24))
				.build());
		Long tokenId = saved.getId();
		entityManager.flush();
		entityManager.clear();

		ActivationToken reloaded = activationTokenRepository.findById(tokenId).orElseThrow();

		assertThat(Hibernate.isInitialized(reloaded.getUser())).isFalse();
		assertThat(reloaded.getUser().getEmail()).isEqualTo("ada@example.com");
		assertThat(Hibernate.isInitialized(reloaded.getUser())).isTrue();
	}

}
