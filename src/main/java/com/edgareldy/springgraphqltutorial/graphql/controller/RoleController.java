package com.edgareldy.springgraphqltutorial.graphql.controller;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.graphql.input.PermissionInput;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;
import com.edgareldy.springgraphqltutorial.service.PermissionService;
import com.edgareldy.springgraphqltutorial.service.RoleService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Role and permission administration: every operation here requires
 * ROLE_ADMIN, enforced by the class-level @PreAuthorize rather than
 * repeating it on each method.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

	private final RoleService roleService;
	private final PermissionService permissionService;

	public RoleController(RoleService roleService, PermissionService permissionService) {
		this.roleService = roleService;
		this.permissionService = permissionService;
	}

	@QueryMapping
	public List<Role> roles() {
		return roleService.findAll();
	}

	@QueryMapping
	public Role role(@Argument Long id) {
		return roleService.findById(id);
	}

	@QueryMapping
	public List<Permission> permissions() {
		return permissionService.findAll();
	}

	@MutationMapping
	public Role createRole(@Argument RoleInput input) {
		return roleService.create(input);
	}

	@MutationMapping
	public Role updateRole(@Argument Long id, @Argument RoleInput input) {
		return roleService.update(id, input);
	}

	@MutationMapping
	public boolean deleteRole(@Argument Long id) {
		roleService.delete(id);
		return true;
	}

	@MutationMapping
	public Permission createPermission(@Argument PermissionInput input) {
		return permissionService.create(input);
	}

	@MutationMapping
	public boolean deletePermission(@Argument Long id) {
		permissionService.delete(id);
		return true;
	}

	@MutationMapping
	public Role assignPermissionToRole(@Argument Long roleId, @Argument Long permissionId) {
		return roleService.assignPermissionToRole(roleId, permissionId);
	}

	@MutationMapping
	public Role removePermissionFromRole(@Argument Long roleId, @Argument Long permissionId) {
		return roleService.removePermissionFromRole(roleId, permissionId);
	}

}
