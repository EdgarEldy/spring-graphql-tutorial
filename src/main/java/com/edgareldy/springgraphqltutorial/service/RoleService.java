package com.edgareldy.springgraphqltutorial.service;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
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

	/**
	 * @throws ResourceNotFoundException if no role has this id
	 */
	Role findById(Long id);

	/**
	 * @throws BusinessRuleException if the role name is already in use
	 */
	Role create(RoleInput input);

	/**
	 * @throws ResourceNotFoundException if no role has this id
	 */
	Role update(Long id, RoleInput input);

	/**
	 * @throws ResourceNotFoundException if no role has this id
	 */
	void delete(Long id);

	/**
	 * @throws ResourceNotFoundException if the role or the permission does not exist
	 */
	Role assignPermissionToRole(Long roleId, Long permissionId);

	/**
	 * @throws ResourceNotFoundException if the role or the permission does not exist
	 */
	Role removePermissionFromRole(Long roleId, Long permissionId);

}
