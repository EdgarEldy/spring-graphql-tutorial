package com.edgareldy.springgraphqltutorial.service;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.RoleInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateRolePermissionsInput;

/**
 * Role administration, including permission assignment since assigning a
 * permission is a relationship change on the Role aggregate.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
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
	 * Adds and/or removes permissions from a role in a single idempotent operation.
	 * Adding an already-present permission or removing an absent one is a no-op.
	 *
	 * @throws BusinessRuleException if add and remove are both empty, or an id
	 *     appears in both lists
	 * @throws ResourceNotFoundException listing every unknown permission id
	 */
	Role updatePermissions(Long roleId, UpdateRolePermissionsInput input);

}
