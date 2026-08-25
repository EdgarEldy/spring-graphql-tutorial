package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.graphql.input.AuthPayload;
import com.edgareldy.springgraphqltutorial.graphql.input.LoginInput;
import com.edgareldy.springgraphqltutorial.graphql.input.RegisterInput;
import com.edgareldy.springgraphqltutorial.security.JwtContextInterceptor;
import com.edgareldy.springgraphqltutorial.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * Authentication mutations and the current-user query, delegating to
 * UserService for everything beyond binding GraphQL arguments and reading
 * the request-scoped Authentication/token.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@MutationMapping
	public AuthPayload register(@Argument RegisterInput input) {
		return userService.register(input);
	}

	@MutationMapping
	public boolean activateAccount(@Argument String token) {
		return userService.activateAccount(token);
	}

	@MutationMapping
	public AuthPayload login(@Argument LoginInput input) {
		return userService.login(input);
	}

	@MutationMapping
	public boolean logout(
			@ContextValue(name = JwtContextInterceptor.JWT_CONTEXT_KEY, required = false) String token) {
		if (token == null) {
			throw new BusinessRuleException("No authentication token present on this request");
		}
		return userService.logout(token);
	}

	@MutationMapping
	public boolean requestPasswordReset(@Argument String email) {
		return userService.requestPasswordReset(email);
	}

	@MutationMapping
	public boolean resetPassword(@Argument String token, @Argument String newPassword) {
		return userService.resetPassword(token, newPassword);
	}

	@QueryMapping
	public User me() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// Spring Security always populates an AnonymousAuthenticationToken when no
		// real principal is authenticated, so getAuthentication() itself is never
		// null: an unauthenticated request has to be detected this way instead.
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken
				|| !authentication.isAuthenticated()) {
			return null;
		}
		return userService.findByEmail(authentication.getName());
	}

}
