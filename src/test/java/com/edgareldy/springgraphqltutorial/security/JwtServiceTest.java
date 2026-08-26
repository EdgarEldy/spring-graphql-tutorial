package com.edgareldy.springgraphqltutorial.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unit tests for JwtService: token generation and every claim extraction
 * method, run against a real JwtService instance (no mocking needed, jjwt
 * does all the work) with a fixed test secret and expiration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class JwtServiceTest {

	private static final String TEST_SECRET = "test-only-secret-key-at-least-256-bits-long-0123456789";
	private static final long TEST_EXPIRATION_MS = 3_600_000L;

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(new JwtProperties(TEST_SECRET, TEST_EXPIRATION_MS));
	}

	private User buildUser(String... roleNames) {
		Set<Role> roles = Set.of(roleNames).stream()
				.map(roleName -> Role.builder().id(1L).roleName(roleName).build())
				.collect(Collectors.toSet());
		return User.builder()
				.id(1L)
				.firstName("Ada")
				.lastName("Lovelace")
				.email("ada@example.com")
				.password("encoded")
				.enabled(true)
				.accountLocked(false)
				.roles(roles)
				.build();
	}

	@Test
	void generateTokenEmbedsEmailAsSubject() {
		User user = buildUser("ADMIN");

		String token = jwtService.generateToken(user);

		assertThat(jwtService.extractEmail(token)).isEqualTo("ada@example.com");
	}

	@Test
	void generateTokenAssignsAUniqueJti() {
		User user = buildUser();

		String firstToken = jwtService.generateToken(user);
		String secondToken = jwtService.generateToken(user);

		assertThat(jwtService.extractJti(firstToken)).isNotBlank().isNotEqualTo(jwtService.extractJti(secondToken));
	}

	@Test
	void generateTokenSetsIssuedAtBeforeExpiration() {
		User user = buildUser();

		String token = jwtService.generateToken(user);

		LocalDateTime issuedAt = jwtService.extractIssuedAt(token);
		LocalDateTime expiration = jwtService.extractExpiration(token);
		assertThat(issuedAt).isBefore(expiration);
	}

	@Test
	void extractAuthoritiesPrefixesEveryRoleNameWithRole() {
		User user = buildUser("ADMIN", "SUPPORT");

		String token = jwtService.generateToken(user);

		assertThat(jwtService.extractAuthorities(token)).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SUPPORT");
	}

	@Test
	void extractAuthoritiesReturnsEmptySetForUserWithoutRoles() {
		User user = buildUser();

		String token = jwtService.generateToken(user);

		assertThat(jwtService.extractAuthorities(token)).isEmpty();
	}

}
