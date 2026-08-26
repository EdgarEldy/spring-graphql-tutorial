package com.edgareldy.springgraphqltutorial.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.repository.CustomerRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
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
 * and there is no such token for List&lt;Role&gt;. Product.category is the
 * opposite shape, a key maps to exactly one Category, so it is registered
 * with forTypePair(Long.class, Category.class): the default loader name it
 * derives from the value type's class name is exactly what
 * ProductCategoryResolver relies on when it declares a plain
 * DataLoader&lt;Long, Category&gt; method parameter instead of looking the
 * loader up by a string name. Order.customer and Order.product are the same
 * one-key-to-one-value shape, so they are registered the same way with
 * forTypePair(Long.class, Customer.class) and forTypePair(Long.class,
 * Product.class), letting OrderFieldResolver declare plain DataLoader&lt;Long,
 * Customer&gt;/DataLoader&lt;Long, Product&gt; parameters exactly like
 * ProductCategoryResolver does. No relationship field is ever allowed to
 * call a repository directly instead of going through a loader registered
 * here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class DataLoaderConfig {

	public DataLoaderConfig(BatchLoaderRegistry registry, UserRepository userRepository,
			RoleRepository roleRepository, CategoryRepository categoryRepository,
			CustomerRepository customerRepository, ProductRepository productRepository) {
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

		registry.forTypePair(Long.class, Category.class).registerMappedBatchLoader((categoryIds, env) ->
				Mono.fromCallable(() -> {
					Map<Long, Category> map = new HashMap<>();
					for (Category category : categoryRepository.findAllById(categoryIds)) {
						map.put(category.getId(), category);
					}
					return map;
				}).subscribeOn(Schedulers.boundedElastic()));

		registry.forTypePair(Long.class, Customer.class).registerMappedBatchLoader((customerIds, env) ->
				Mono.fromCallable(() -> {
					Map<Long, Customer> map = new HashMap<>();
					for (Customer customer : customerRepository.findAllById(customerIds)) {
						map.put(customer.getId(), customer);
					}
					return map;
				}).subscribeOn(Schedulers.boundedElastic()));

		registry.forTypePair(Long.class, Product.class).registerMappedBatchLoader((productIds, env) ->
				Mono.fromCallable(() -> {
					Map<Long, Product> map = new HashMap<>();
					for (Product product : productRepository.findAllById(productIds)) {
						map.put(product.getId(), product);
					}
					return map;
				}).subscribeOn(Schedulers.boundedElastic()));
	}

}
