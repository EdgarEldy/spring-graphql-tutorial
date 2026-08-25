package com.edgareldy.springgraphqltutorial.service;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.graphql.input.PermissionInput;

/**
 * Permission administration. Permissions are only ever created, listed and
 * deleted, never updated: a resource/action pair that needs to change is a
 * different permission, not an edit of an existing one.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface PermissionService {

	List<Permission> findAll();

	Permission create(PermissionInput input);

	void delete(Long id);

}
