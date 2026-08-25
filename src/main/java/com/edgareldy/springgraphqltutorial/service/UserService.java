package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.graphql.input.AuthPayload;
import com.edgareldy.springgraphqltutorial.graphql.input.CreateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.LoginInput;
import com.edgareldy.springgraphqltutorial.graphql.input.RegisterInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UserPage;

/**
 * Authentication flows and user administration, including role assignment
 * since assigning a role is a relationship change on the User aggregate.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface UserService {

	AuthPayload register(RegisterInput input);

	boolean activateAccount(String token);

	AuthPayload login(LoginInput input);

	boolean logout(String rawToken);

	boolean requestPasswordReset(String email);

	boolean resetPassword(String token, String newPassword);

	User findByEmail(String email);

	UserPage findAll(int page, int size);

	User findById(Long id);

	User create(CreateUserInput input);

	User update(Long id, UpdateUserInput input);

	User lock(Long id);

	User unlock(Long id);

	void delete(Long id);

	User assignRoleToUser(Long userId, Long roleId);

	User removeRoleFromUser(Long userId, Long roleId);

}
