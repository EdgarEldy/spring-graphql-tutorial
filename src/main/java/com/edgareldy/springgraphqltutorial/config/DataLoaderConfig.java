package com.edgareldy.springgraphqltutorial.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Registers every DataLoader used to batch relationship resolution and avoid
 * the N+1 problem. userRoles and rolePermissions back User.roles and
 * Role.permissions: both are one-to-many from the loader's point of view (a
 * key maps to a list), which is why they are registered with forName rather
 * than forTypePair, since forTypePair needs a Class token for the value type
 * and there is no such token for List&lt;Role&gt;. No relationship field is
 * ever allowed to call a repository directly instead of going through a
 * loader registered here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class DataLoaderConfig {

	public DataLoaderConfig(BatchLoaderRegistry registry, UserRepository userRepository,
			RoleRepository roleRepository) {
		// Built with an explicit loop rather than Collectors.toMap: the collector
		// infers ArrayList as the map's value type from the lambda alone, which
		// does not satisfy the Map<Long, List<Role>> the batch loader is declared
		// to return and fails to compile.
		registry.<Long, List<Role>>forName("userRoles").registerMappedBatchLoader((userIds, env) ->
				Mono.fromCallable(() -> {
					Map<Long, List<Role>> map = new HashMap<>();
					for (User user : userRepository.findAllWithRolesByIdIn(userIds)) {
						map.put(user.getId(), new ArrayList<>(user.getRoles()));
					}
					return map;
				}).subscribeOn(Schedulers.boundedElastic()));

		registry.<Long, List<Permission>>forName("rolePermissions").registerMappedBatchLoader((roleIds, env) ->
				Mono.fromCallable(() -> {
					Map<Long, List<Permission>> map = new HashMap<>();
					for (Role role : roleRepository.findAllWithPermissionsByIdIn(roleIds)) {
						map.put(role.getId(), new ArrayList<>(role.getPermissions()));
					}
					return map;
				}).subscribeOn(Schedulers.boundedElastic()));
	}

}
