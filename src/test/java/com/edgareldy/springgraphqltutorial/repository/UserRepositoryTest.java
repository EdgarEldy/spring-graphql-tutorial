package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaTransactionManager;

/**
 * Integration tests for UserRepository against a real PostgreSQL 16
 * container: the derived finder/existence methods, the unique email
 * constraint from V1__init_schema.sql, and findAllWithRolesByIdIn, the
 * JOIN FETCH query the userRoles DataLoader batches through, verified to
 * issue exactly one SQL query regardless of how many user ids are
 * requested.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JpaTransactionManager transactionManager;

	private Statistics statistics;

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
	void findByEmailReturnsPersistedUser() {
		persistUser("ada@example.com");

		Optional<User> found = userRepository.findByEmail("ada@example.com");

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("ada@example.com");
	}

	@Test
	void findByEmailReturnsEmptyWhenNoMatch() {
		assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
	}

	@Test
	void existsByEmailReflectsPersistedState() {
		persistUser("ada@example.com");

		assertThat(userRepository.existsByEmail("ada@example.com")).isTrue();
		assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
	}

	@Test
	void emailColumnRejectsDuplicates() {
		persistUser("ada@example.com");

		assertThatThrownBy(() -> {
			userRepository.save(User.builder()
					.firstName("Second")
					.lastName("Ada")
					.email("ada@example.com")
					.password("encoded")
					.enabled(true)
					.accountLocked(false)
					.build());
			userRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findAllWithRolesByIdInFetchesRolesInOneQueryRegardlessOfUserCount() {
		Role admin = roleRepository.save(Role.builder().roleName("ADMIN").build());
		Role support = roleRepository.save(Role.builder().roleName("SUPPORT").build());

		User first = persistUser("first@example.com");
		first.getRoles().add(admin);
		userRepository.save(first);

		User second = persistUser("second@example.com");
		second.getRoles().add(support);
		userRepository.save(second);

		User third = persistUser("third@example.com");
		third.getRoles().add(admin);
		third.getRoles().add(support);
		userRepository.save(third);

		statistics = entityManagerFactoryStatistics();
		statistics.clear();

		List<User> smallBatch = userRepository.findAllWithRolesByIdIn(List.of(first.getId(), second.getId()));
		long queriesForTwoUsers = statistics.getQueryExecutionCount();

		statistics.clear();
		List<User> fullBatch = userRepository
				.findAllWithRolesByIdIn(List.of(first.getId(), second.getId(), third.getId()));
		long queriesForThreeUsers = statistics.getQueryExecutionCount();

		assertThat(smallBatch).hasSize(2);
		assertThat(fullBatch).hasSize(3);
		assertThat(queriesForTwoUsers).isEqualTo(1L);
		assertThat(queriesForThreeUsers).isEqualTo(1L);
		assertThat(queriesForThreeUsers).isEqualTo(queriesForTwoUsers);
	}

	private Statistics entityManagerFactoryStatistics() {
		SessionFactory sessionFactory = transactionManager.getEntityManagerFactory().unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		return sessionFactory.getStatistics();
	}

}
