package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for PermissionRepository against a real PostgreSQL 16
 * container: existsByResourceAndAction, and the composite unique
 * constraint on (resource, action) from V1__init_schema.sql, which two
 * separate single-column unique constraints could not express.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PermissionRepositoryTest {

	@Autowired
	private PermissionRepository permissionRepository;

	@Test
	void existsByResourceAndActionReflectsPersistedState() {
		permissionRepository.save(Permission.builder().resource("product").action("delete").build());

		assertThat(permissionRepository.existsByResourceAndAction("product", "delete")).isTrue();
		assertThat(permissionRepository.existsByResourceAndAction("product", "read")).isFalse();
		assertThat(permissionRepository.existsByResourceAndAction("category", "delete")).isFalse();
	}

	@Test
	void sameResourceWithDifferentActionIsAllowed() {
		permissionRepository.save(Permission.builder().resource("product").action("delete").build());

		Permission readPermission = permissionRepository
				.save(Permission.builder().resource("product").action("read").build());

		assertThat(readPermission.getId()).isNotNull();
	}

	@Test
	void resourceActionPairColumnsRejectDuplicates() {
		permissionRepository.save(Permission.builder().resource("product").action("delete").build());

		assertThatThrownBy(() -> {
			permissionRepository.save(Permission.builder().resource("product").action("delete").build());
			permissionRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
