package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.graphql.input.CreateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UserPage;
import com.edgareldy.springgraphqltutorial.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * User administration: every operation here requires ROLE_ADMIN, enforced by
 * the class-level @PreAuthorize rather than repeating it on each method.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@QueryMapping
	public UserPage users(@Argument Integer page, @Argument Integer size) {
		int resolvedPage = page != null ? page : DEFAULT_PAGE;
		int resolvedSize = size != null ? size : DEFAULT_SIZE;
		return userService.findAll(resolvedPage, resolvedSize);
	}

	@QueryMapping
	public User user(@Argument Long id) {
		return userService.findById(id);
	}

	@MutationMapping
	public User createUser(@Argument CreateUserInput input) {
		return userService.create(input);
	}

	@MutationMapping
	public User updateUser(@Argument Long id, @Argument UpdateUserInput input) {
		return userService.update(id, input);
	}

	@MutationMapping
	public User lockUser(@Argument Long id) {
		return userService.lock(id);
	}

	@MutationMapping
	public User unlockUser(@Argument Long id) {
		return userService.unlock(id);
	}

	@MutationMapping
	public boolean deleteUser(@Argument Long id) {
		userService.delete(id);
		return true;
	}

	@MutationMapping
	public User assignRoleToUser(@Argument Long userId, @Argument Long roleId) {
		return userService.assignRoleToUser(userId, roleId);
	}

	@MutationMapping
	public User removeRoleFromUser(@Argument Long userId, @Argument Long roleId) {
		return userService.removeRoleFromUser(userId, roleId);
	}

}
