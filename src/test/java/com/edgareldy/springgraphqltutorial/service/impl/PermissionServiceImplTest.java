package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.PermissionInput;
import com.edgareldy.springgraphqltutorial.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PermissionServiceImpl: the nominal path of findAll/create/
 * delete plus the two business rules it enforces (a duplicate
 * resource/action pair on create, a missing permission on delete), with
 * PermissionRepository mocked and no Spring context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

	@Mock
	private PermissionRepository permissionRepository;

	private PermissionServiceImpl permissionService;

	@BeforeEach
	void setUp() {
		permissionService = new PermissionServiceImpl(permissionRepository);
	}

	@Test
	void findAllReturnsEveryPermission() {
		Permission permission = Permission.builder().id(1L).resource("product").action("delete").build();
		when(permissionRepository.findAll()).thenReturn(List.of(permission));

		assertThat(permissionService.findAll()).containsExactly(permission);
	}

	@Test
	void createSavesNewPermission() {
		PermissionInput input = new PermissionInput("product", "delete");
		when(permissionRepository.existsByResourceAndAction("product", "delete")).thenReturn(false);
		when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Permission created = permissionService.create(input);

		assertThat(created.getResource()).isEqualTo("product");
		assertThat(created.getAction()).isEqualTo("delete");
	}

	@Test
	void createRejectsDuplicateResourceActionPair() {
		PermissionInput input = new PermissionInput("product", "delete");
		when(permissionRepository.existsByResourceAndAction("product", "delete")).thenReturn(true);

		assertThatThrownBy(() -> permissionService.create(input)).isInstanceOf(BusinessRuleException.class);

		verify(permissionRepository, never()).save(any(Permission.class));
	}

	@Test
	void deleteRemovesPermission() {
		Permission permission = Permission.builder().id(1L).resource("product").action("delete").build();
		when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

		permissionService.delete(1L);

		verify(permissionRepository).delete(permission);
	}

	@Test
	void deleteThrowsWhenPermissionMissing() {
		when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> permissionService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(permissionRepository, never()).delete(any(Permission.class));
	}

}
