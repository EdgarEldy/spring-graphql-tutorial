package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springgraphqltutorial.entity.ActivationToken;
import com.edgareldy.springgraphqltutorial.entity.PasswordResetToken;
import com.edgareldy.springgraphqltutorial.repository.ActivationTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for AuthController, run against the real GraphQL
 * endpoint. Covers every authentication mutation/query listed in the
 * README (register, activateAccount, login, logout,
 * requestPasswordReset, resetPassword, me), including the register to
 * activate to login flow the README explicitly asks for, with each
 * BAD_REQUEST/NOT_FOUND branch asserted on
 * extensions.classification rather than just "an error happened".
 * There is no activation email in this project, so tests read the
 * generated token straight from ActivationTokenRepository/
 * PasswordResetTokenRepository instead, exactly the way a real client
 * would receive it out of band.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class AuthControllerTest extends GraphQlIntegrationTestSupport {

	@Autowired
	private ActivationTokenRepository activationTokenRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	private static final String REGISTER_MUTATION = """
			mutation Register($firstName: String!, $lastName: String!, $email: String!, $password: String!) {
			  register(input: { firstName: $firstName, lastName: $lastName, email: $email, password: $password }) {
			    token
			    user {
			      id
			      email
			      enabled
			    }
			  }
			}
			""";

	private String register(String email) {
		graphQlTester.document(REGISTER_MUTATION)
				.variable("firstName", "Ada")
				.variable("lastName", "Lovelace")
				.variable("email", email)
				.variable("password", "secret-password")
				.execute()
				.path("register.user.email")
				.entity(String.class)
				.isEqualTo(email);
		return email;
	}

	private String activationTokenFor(String email) {
		// Compares by user id, not by candidate.getUser().getEmail(): the
		// User association is LAZY and this method runs outside any Hibernate
		// session, so touching a non-identifier field on it would raise a
		// LazyInitializationException. The id is always available on a
		// Hibernate proxy without triggering initialization.
		Long userId = userRepository.findByEmail(email).orElseThrow().getId();
		ActivationToken token = activationTokenRepository.findAll().stream()
				.filter(candidate -> candidate.getUser().getId().equals(userId))
				.findFirst()
				.orElseThrow();
		return token.getToken();
	}

	@Test
	void registerActivateThenLoginSucceeds() {
		String email = uniqueEmail("flow");
		register(email);

		String activationToken = activationTokenFor(email);
		graphQlTester.document("mutation($token: String!) { activateAccount(token: $token) }")
				.variable("token", activationToken)
				.execute()
				.path("activateAccount")
				.entity(Boolean.class)
				.isEqualTo(true);

		String jwt = login(email, "secret-password");
		assertThat(jwt).isNotBlank();

		authenticatedTester(jwt).document("{ me { email enabled } }")
				.execute()
				.path("me.email")
				.entity(String.class)
				.isEqualTo(email);
	}

	@Test
	void registerRejectsDuplicateEmail() {
		String email = uniqueEmail("duplicate");
		register(email);

		graphQlTester.document(REGISTER_MUTATION)
				.variable("firstName", "Second")
				.variable("lastName", "Person")
				.variable("email", email)
				.variable("password", "another-secret")
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void activateAccountRejectsUnknownToken() {
		graphQlTester.document("mutation { activateAccount(token: \"does-not-exist\") }")
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void activateAccountRejectsAlreadyActivatedToken() {
		String email = uniqueEmail("reactivate");
		register(email);
		String activationToken = activationTokenFor(email);
		graphQlTester.document("mutation($token: String!) { activateAccount(token: $token) }")
				.variable("token", activationToken)
				.executeAndVerify();

		graphQlTester.document("mutation($token: String!) { activateAccount(token: $token) }")
				.variable("token", activationToken)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void loginRejectsAccountThatWasNeverActivated() {
		String email = uniqueEmail("inactive");
		register(email);

		graphQlTester.document("mutation($email: String!, $password: String!) { login(input: { email: $email, password: $password }) { token } }")
				.variable("email", email)
				.variable("password", "secret-password")
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void loginRejectsWrongPassword() {
		String email = uniqueEmail("wrong-password");
		createEnabledUser(email, "correct-password");

		graphQlTester.document("mutation($email: String!, $password: String!) { login(input: { email: $email, password: $password }) { token } }")
				.variable("email", email)
				.variable("password", "wrong-password")
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void logoutBlacklistsTokenSoFurtherRequestsAreAnonymous() {
		String email = uniqueEmail("logout");
		createEnabledUser(email, "secret-password");
		String jwt = login(email, "secret-password");

		authenticatedTester(jwt).document("mutation { logout }")
				.execute()
				.path("logout")
				.entity(Boolean.class)
				.isEqualTo(true);

		authenticatedTester(jwt).document("{ me { email } }")
				.execute()
				.path("me")
				.valueIsNull();
	}

	@Test
	void logoutWithoutATokenIsRejected() {
		graphQlTester.document("mutation { logout }")
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void requestPasswordResetCreatesATokenForAKnownEmail() {
		String email = uniqueEmail("reset");
		createEnabledUser(email, "old-password");

		graphQlTester.document("mutation($email: String!) { requestPasswordReset(email: $email) }")
				.variable("email", email)
				.execute()
				.path("requestPasswordReset")
				.entity(Boolean.class)
				.isEqualTo(true);

		Long userId = userRepository.findByEmail(email).orElseThrow().getId();
		assertThat(passwordResetTokenRepository.findAll().stream()
				.anyMatch(token -> token.getUser().getId().equals(userId))).isTrue();
	}

	@Test
	void requestPasswordResetReturnsFalseForUnknownEmail() {
		graphQlTester.document("mutation { requestPasswordReset(email: \"nobody@example.com\") }")
				.execute()
				.path("requestPasswordReset")
				.entity(Boolean.class)
				.isEqualTo(false);
	}

	@Test
	void resetPasswordAllowsLoginWithTheNewPassword() {
		String email = uniqueEmail("reset-flow");
		createEnabledUser(email, "old-password");
		graphQlTester.document("mutation($email: String!) { requestPasswordReset(email: $email) }")
				.variable("email", email)
				.executeAndVerify();
		Long userId = userRepository.findByEmail(email).orElseThrow().getId();
		PasswordResetToken token = passwordResetTokenRepository.findAll().stream()
				.filter(candidate -> candidate.getUser().getId().equals(userId))
				.findFirst()
				.orElseThrow();

		graphQlTester.document("mutation($token: String!, $newPassword: String!) { resetPassword(token: $token, newPassword: $newPassword) }")
				.variable("token", token.getToken())
				.variable("newPassword", "new-password")
				.execute()
				.path("resetPassword")
				.entity(Boolean.class)
				.isEqualTo(true);

		assertThat(login(email, "new-password")).isNotBlank();
	}

	@Test
	void resetPasswordRejectsUnknownToken() {
		graphQlTester.document("mutation { resetPassword(token: \"does-not-exist\", newPassword: \"whatever\") }")
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void meReturnsNullWithoutAuthentication() {
		graphQlTester.document("{ me { email } }").execute().path("me").valueIsNull();
	}

	@Test
	void meReturnsTheAuthenticatedUser() {
		String email = uniqueEmail("me");
		createEnabledUser(email, "secret-password");
		String jwt = login(email, "secret-password");

		HttpGraphQlTester authenticated = authenticatedTester(jwt);
		authenticated.document("{ me { email } }")
				.execute()
				.path("me.email")
				.entity(String.class)
				.isEqualTo(email);
	}

}
