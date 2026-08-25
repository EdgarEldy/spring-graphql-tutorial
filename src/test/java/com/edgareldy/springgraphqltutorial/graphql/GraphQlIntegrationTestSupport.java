package com.edgareldy.springgraphqltutorial.graphql;

import java.util.UUID;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared setup for every GraphQL end-to-end test in this package: starts
 * the full application (real embedded server, real Servlet filter chain,
 * real Testcontainers PostgreSQL) and exposes a plain HttpGraphQlTester
 * bound to it, built by hand from a WebTestClient rather than through
 * Boot's spring-boot-graphql-test autoconfiguration, which is not on this
 * project's test classpath (see the pom.xml/CLAUDE.md note added
 * alongside the first repository test on this branch). RANDOM_PORT and a
 * real HTTP round trip are required, not a shortcut like MockMvc: the two
 * production bugs already fixed on this branch (Flyway silently not
 * running, @PreAuthorize seeing anonymousUser) were both invisible except
 * against a really running server, so these tests exercise the same path.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
abstract class GraphQlIntegrationTestSupport {

	@LocalServerPort
	protected int port;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected RoleRepository roleRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	protected HttpGraphQlTester graphQlTester;

	@BeforeEach
	void setUpGraphQlTester() {
		WebTestClient.Builder webTestClientBuilder = WebTestClient.bindToServer(new JdkClientHttpConnector());
		graphQlTester = HttpGraphQlTester.builder(webTestClientBuilder)
				.url("http://localhost:" + port + "/graphql")
				.build();
	}

	/**
	 * Every test that needs a unique identity uses this instead of a fixed
	 * literal, since @SpringBootTest reuses the same cached application
	 * context (and therefore the same database rows) across every test
	 * method and every test class in this package.
	 */
	protected String uniqueEmail(String label) {
		return label + "-" + UUID.randomUUID() + "@example.com";
	}

	protected HttpGraphQlTester authenticatedTester(String token) {
		return graphQlTester.mutate().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
	}

	/** Persists an already-enabled user directly, bypassing the public register/activate flow. */
	protected User createEnabledUser(String email, String rawPassword) {
		return userRepository.save(User.builder()
				.firstName("Test")
				.lastName("User")
				.email(email)
				.password(passwordEncoder.encode(rawPassword))
				.enabled(true)
				.accountLocked(false)
				.build());
	}

	/**
	 * Ensures the ADMIN role exists, creates a fresh enabled user carrying it,
	 * and logs in through the real GraphQL login mutation so the returned
	 * token is a genuine JWT with a ROLE_ADMIN authority, exactly as
	 * JwtService.generateToken would embed it in production.
	 */
	protected String bootstrapAdminToken() {
		Role adminRole = roleRepository.findByRoleName("ADMIN")
				.orElseGet(() -> roleRepository.save(Role.builder().roleName("ADMIN").build()));
		String email = uniqueEmail("admin");
		String password = "admin-secret";
		User admin = createEnabledUser(email, password);
		admin.getRoles().add(adminRole);
		userRepository.save(admin);
		return login(email, password);
	}

	protected String login(String email, String rawPassword) {
		return graphQlTester.document("""
				mutation Login($email: String!, $password: String!) {
				  login(input: { email: $email, password: $password }) {
				    token
				  }
				}
				""")
				.variable("email", email)
				.variable("password", rawPassword)
				.execute()
				.path("login.token")
				.entity(String.class)
				.get();
	}

}
