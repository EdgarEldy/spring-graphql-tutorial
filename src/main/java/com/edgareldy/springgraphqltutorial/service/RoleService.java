package com.edgareldy.springgraphqltutorial.service;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;

/**
 * Role administration, including permission assignment since assigning a
 * permission is a relationship change on the Role aggregate.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface RoleService {

	List<Role> findAll();

	Role findById(Long id);

	Role create(RoleInput input);

	Role update(Long id, RoleInput input);

	void delete(Long id);

	Role assignPermissionToRole(Long roleId, Long permissionId);

	Role removePermissionFromRole(Long roleId, Long permissionId);

}
