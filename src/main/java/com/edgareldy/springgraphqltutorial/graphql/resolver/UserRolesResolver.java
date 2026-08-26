package com.edgareldy.springgraphqltutorial.graphql.resolver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Resolves User.roles through the userRoles DataLoader registered in
 * DataLoaderConfig, never through RoleRepository/UserRepository directly, so
 * listing N users with their roles issues one batched query, not N.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class UserRolesResolver {

	@SchemaMapping(typeName = "User", field = "roles")
	public CompletableFuture<List<Role>> roles(User user, DataFetchingEnvironment env) {
		DataLoader<Long, List<Role>> loader = env.getDataLoaderRegistry().getDataLoader("userRoles");
		return loader.load(user.getId());
	}

}
