package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;
import com.edgareldy.springgraphqltutorial.repository.PermissionRepository;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for RoleServiceImpl: every public method's nominal path plus
 * the business rules it enforces (duplicate role name, missing role or
 * permission on assignment), with RoleRepository/PermissionRepository
 * mocked and no Spring context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PermissionRepository permissionRepository;

	private RoleServiceImpl roleService;

	@BeforeEach
	void setUp() {
		roleService = new RoleServiceImpl(roleRepository, permissionRepository);
	}

	@Test
	void findAllReturnsEveryRole() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
		when(roleRepository.findAll()).thenReturn(List.of(role));

		assertThat(roleService.findAll()).containsExactly(role);
	}

	@Test
	void findByIdReturnsRole() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

		assertThat(roleService.findById(1L)).isEqualTo(role);
	}

	@Test
	void findByIdThrowsWhenRoleMissing() {
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createSavesNewRole() {
		RoleInput input = new RoleInput("ADMIN");
		when(roleRepository.existsByRoleName("ADMIN")).thenReturn(false);
		when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Role created = roleService.create(input);

		assertThat(created.getRoleName()).isEqualTo("ADMIN");
	}

	@Test
	void createRejectsDuplicateRoleName() {
		RoleInput input = new RoleInput("ADMIN");
		when(roleRepository.existsByRoleName("ADMIN")).thenReturn(true);

		assertThatThrownBy(() -> roleService.create(input)).isInstanceOf(BusinessRuleException.class);

		verify(roleRepository, never()).save(any(Role.class));
	}

	@Test
	void updateChangesRoleName() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
		RoleInput input = new RoleInput("SUPER_ADMIN");
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(roleRepository.save(role)).thenReturn(role);

		Role updated = roleService.update(1L, input);

		assertThat(updated.getRoleName()).isEqualTo("SUPER_ADMIN");
	}

	@Test
	void updateThrowsWhenRoleMissing() {
		RoleInput input = new RoleInput("SUPER_ADMIN");
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.update(99L, input)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void deleteRemovesRole() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

		roleService.delete(1L);

		verify(roleRepository).delete(role);
	}

	@Test
	void deleteThrowsWhenRoleMissing() {
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(roleRepository, never()).delete(any(Role.class));
	}

	@Test
	void assignPermissionToRoleAddsPermissionToRolePermissionSet() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
		Permission permission = Permission.builder().id(2L).resource("product").action("delete").build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
		when(roleRepository.save(role)).thenReturn(role);

		Role result = roleService.assignPermissionToRole(1L, 2L);

		assertThat(result.getPermissions()).contains(permission);
	}

	@Test
	void assignPermissionToRoleThrowsWhenRoleMissing() {
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.assignPermissionToRole(99L, 2L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void assignPermissionToRoleThrowsWhenPermissionMissing() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.assignPermissionToRole(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void removePermissionFromRoleRemovesPermissionFromRolePermissionSet() {
		Permission permission = Permission.builder().id(2L).resource("product").action("delete").build();
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new java.util.HashSet<>(Set.of(permission)))
				.build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
		when(roleRepository.save(role)).thenReturn(role);

		Role result = roleService.removePermissionFromRole(1L, 2L);

		assertThat(result.getPermissions()).doesNotContain(permission);
	}

	@Test
	void removePermissionFromRoleThrowsWhenRoleMissing() {
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.removePermissionFromRole(99L, 2L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void removePermissionFromRoleThrowsWhenPermissionMissing() {
		Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roleService.removePermissionFromRole(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
