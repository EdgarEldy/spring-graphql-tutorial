package com.edgareldy.springgraphqltutorial.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateRolePermissionsInput;
import com.edgareldy.springgraphqltutorial.repository.PermissionRepository;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Service
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
	}

	@Override
	public List<Role> findAll() {
		return roleRepository.findAll();
	}

	@Override
	public Role findById(Long id) {
		return getRoleOrThrow(id);
	}

	@Override
	@Transactional
	public Role create(RoleInput input) {
		if (roleRepository.existsByRoleName(input.roleName())) {
			throw new BusinessRuleException("Role name already in use: " + input.roleName());
		}
		return roleRepository.save(Role.builder().roleName(input.roleName()).build());
	}

	@Override
	@Transactional
	public Role update(Long id, RoleInput input) {
		Role role = getRoleOrThrow(id);
		role.setRoleName(input.roleName());
		return roleRepository.save(role);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		roleRepository.delete(getRoleOrThrow(id));
	}

	@Override
	@Transactional
	public Role updatePermissions(Long roleId, UpdateRolePermissionsInput input) {
		Role role = getRoleOrThrow(roleId);

		List<Long> addIds = input.add() != null ? input.add() : List.of();
		List<Long> removeIds = input.remove() != null ? input.remove() : List.of();
		if (addIds.isEmpty() && removeIds.isEmpty()) {
			throw new BusinessRuleException("At least one permission id must be specified in add or remove");
		}

		Set<Long> addSet = new HashSet<>(addIds);
		for (Long id : removeIds) {
			if (addSet.contains(id)) {
				throw new BusinessRuleException("Permission id " + id + " appears in both add and remove");
			}
		}

		List<Long> allIds = new ArrayList<>(addIds);
		allIds.addAll(removeIds);
		List<Permission> found = permissionRepository.findAllById(allIds);
		if (found.size() != allIds.size()) {
			Set<Long> foundIds = found.stream().map(Permission::getId).collect(Collectors.toSet());
			List<Long> unknown = allIds.stream().filter(id -> !foundIds.contains(id)).toList();
			throw new ResourceNotFoundException("Permission(s) not found: " + unknown);
		}

		Map<Long, Permission> permMap = found.stream().collect(Collectors.toMap(Permission::getId, p -> p));
		addIds.forEach(id -> role.getPermissions().add(permMap.get(id)));
		removeIds.forEach(id -> role.getPermissions().remove(permMap.get(id)));
		return roleRepository.save(role);
	}

	private Role getRoleOrThrow(Long id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
	}

}
