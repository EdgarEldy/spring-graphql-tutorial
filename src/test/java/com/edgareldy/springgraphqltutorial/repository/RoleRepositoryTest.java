package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
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
 * Integration tests for RoleRepository against a real PostgreSQL 16
 * container: the derived finder/existence methods, the unique role_name
 * constraint from V1__init_schema.sql, and findAllWithPermissionsByIdIn,
 * the JOIN FETCH query the rolePermissions DataLoader batches through,
 * verified to issue exactly one SQL query regardless of how many role ids
 * are requested.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RoleRepositoryTest {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private JpaTransactionManager transactionManager;

	@Test
	void findByRoleNameReturnsPersistedRole() {
		roleRepository.save(Role.builder().roleName("ADMIN").build());

		Optional<Role> found = roleRepository.findByRoleName("ADMIN");

		assertThat(found).isPresent();
		assertThat(found.get().getRoleName()).isEqualTo("ADMIN");
	}

	@Test
	void findByRoleNameReturnsEmptyWhenNoMatch() {
		assertThat(roleRepository.findByRoleName("MISSING")).isEmpty();
	}

	@Test
	void existsByRoleNameReflectsPersistedState() {
		roleRepository.save(Role.builder().roleName("ADMIN").build());

		assertThat(roleRepository.existsByRoleName("ADMIN")).isTrue();
		assertThat(roleRepository.existsByRoleName("MISSING")).isFalse();
	}

	@Test
	void roleNameColumnRejectsDuplicates() {
		roleRepository.save(Role.builder().roleName("ADMIN").build());

		assertThatThrownBy(() -> {
			roleRepository.save(Role.builder().roleName("ADMIN").build());
			roleRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findAllWithPermissionsByIdInFetchesPermissionsInOneQueryRegardlessOfRoleCount() {
		Permission readProduct = permissionRepository
				.save(Permission.builder().resource("product").action("read").build());
		Permission deleteProduct = permissionRepository
				.save(Permission.builder().resource("product").action("delete").build());

		Role first = roleRepository.save(Role.builder().roleName("VIEWER").build());
		first.getPermissions().add(readProduct);
		roleRepository.save(first);

		Role second = roleRepository.save(Role.builder().roleName("EDITOR").build());
		second.getPermissions().add(readProduct);
		second.getPermissions().add(deleteProduct);
		roleRepository.save(second);

		Role third = roleRepository.save(Role.builder().roleName("ADMIN").build());
		third.getPermissions().add(deleteProduct);
		roleRepository.save(third);

		Statistics statistics = entityManagerFactoryStatistics();
		statistics.clear();

		List<Role> smallBatch = roleRepository.findAllWithPermissionsByIdIn(List.of(first.getId(), second.getId()));
		long queriesForTwoRoles = statistics.getQueryExecutionCount();

		statistics.clear();
		List<Role> fullBatch = roleRepository
				.findAllWithPermissionsByIdIn(List.of(first.getId(), second.getId(), third.getId()));
		long queriesForThreeRoles = statistics.getQueryExecutionCount();

		assertThat(smallBatch).hasSize(2);
		assertThat(fullBatch).hasSize(3);
		assertThat(queriesForTwoRoles).isEqualTo(1L);
		assertThat(queriesForThreeRoles).isEqualTo(1L);
		assertThat(queriesForThreeRoles).isEqualTo(queriesForTwoRoles);
	}

	private Statistics entityManagerFactoryStatistics() {
		SessionFactory sessionFactory = transactionManager.getEntityManagerFactory().unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		return sessionFactory.getStatistics();
	}

}
