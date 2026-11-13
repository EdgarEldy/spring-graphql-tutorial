package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.AuthPayload;
import com.edgareldy.springgraphqltutorial.graphql.input.CreateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.LoginInput;
import com.edgareldy.springgraphqltutorial.graphql.input.RegisterInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateProfileInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserRolesInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UserPage;

/**
 * Authentication flows and user administration, including role assignment
 * since assigning a role is a relationship change on the User aggregate.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface UserService {

	/**
	 * Creates a disabled account and an activation token.
	 *
	 * @throws BusinessRuleException if the email is already registered
	 */
	AuthPayload register(RegisterInput input);

	/**
	 * Enables the account behind the given activation token.
	 *
	 * @throws ResourceNotFoundException if the token does not exist
	 * @throws BusinessRuleException if the token was already used or has expired
	 */
	boolean activateAccount(String token);

	/**
	 * Verifies credentials and issues a JWT.
	 *
	 * @throws BusinessRuleException for a wrong email/password, a disabled
	 * account, or a locked account
	 */
	AuthPayload login(LoginInput input);

	/**
	 * Blacklists the given JWT by its jti so it can no longer authenticate,
	 * even though it has not expired yet.
	 */
	boolean logout(String rawToken);

	/**
	 * Issues a password reset token if the email is registered. Returns
	 * false, never throws, for an unknown email.
	 */
	boolean requestPasswordReset(String email);

	/**
	 * Consumes a password reset token and sets the new password.
	 *
	 * @throws ResourceNotFoundException if the token does not exist
	 * @throws BusinessRuleException if the token has expired
	 */
	boolean resetPassword(String token, String newPassword);

	/**
	 * @throws ResourceNotFoundException if no user has this email
	 */
	User findByEmail(String email);

	UserPage findAll(int page, int size);

	/**
	 * @throws ResourceNotFoundException if no user has this id
	 */
	User findById(Long id);

	/**
	 * Creates an already-enabled account, bypassing the public activation
	 * flow.
	 *
	 * @throws BusinessRuleException if the email is already registered
	 */
	User create(CreateUserInput input);

	User update(Long id, UpdateUserInput input);

	User lock(Long id);

	User unlock(Long id);

	void delete(Long id);

	/**
	 * Adds and/or removes roles from a user in a single idempotent operation.
	 *
	 * @throws BusinessRuleException if add and remove are both empty, or an id
	 *     appears in both lists
	 * @throws ResourceNotFoundException listing every unknown role id
	 */
	User updateRoles(Long userId, UpdateUserRolesInput input);

	/**
	 * Issues a new access token and a new refresh token from a valid refresh JWT.
	 *
	 * @throws BusinessRuleException if the token is not a refresh token, is
	 *     blacklisted, or belongs to a disabled/locked account
	 * @throws ResourceNotFoundException if no user matches the token subject
	 */
	AuthPayload refreshToken(String rawRefreshToken);

	/**
	 * Updates the calling user's own profile fields.
	 *
	 * @throws BusinessRuleException if the new email is already taken
	 * @throws ResourceNotFoundException if no user has this email
	 */
	User updateProfile(String email, UpdateProfileInput input);

}
