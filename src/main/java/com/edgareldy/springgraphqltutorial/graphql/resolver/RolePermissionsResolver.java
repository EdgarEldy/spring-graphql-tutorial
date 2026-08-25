package com.edgareldy.springgraphqltutorial.graphql.resolver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Resolves Role.permissions through the rolePermissions DataLoader
 * registered in DataLoaderConfig, never through PermissionRepository
 * directly, so listing N roles with their permissions issues one batched
 * query, not N.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class RolePermissionsResolver {

	@SchemaMapping(typeName = "Role", field = "permissions")
	public CompletableFuture<List<Permission>> permissions(Role role, DataFetchingEnvironment env) {
		DataLoader<Long, List<Permission>> loader = env.getDataLoaderRegistry().getDataLoader("rolePermissions");
		return loader.load(role.getId());
	}

}
