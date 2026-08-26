package com.edgareldy.springgraphqltutorial.service.impl;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;
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
	public Role assignPermissionToRole(Long roleId, Long permissionId) {
		Role role = getRoleOrThrow(roleId);
		Permission permission = getPermissionOrThrow(permissionId);
		role.getPermissions().add(permission);
		return roleRepository.save(role);
	}

	@Override
	@Transactional
	public Role removePermissionFromRole(Long roleId, Long permissionId) {
		Role role = getRoleOrThrow(roleId);
		Permission permission = getPermissionOrThrow(permissionId);
		role.getPermissions().remove(permission);
		return roleRepository.save(role);
	}

	private Role getRoleOrThrow(Long id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
	}

	private Permission getPermissionOrThrow(Long id) {
		return permissionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
	}

}
