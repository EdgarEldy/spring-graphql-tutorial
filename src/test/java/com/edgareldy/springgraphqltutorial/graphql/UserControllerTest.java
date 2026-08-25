package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springgraphqltutorial.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for UserController, run against the real GraphQL
 * endpoint. Covers every operation from the README's "user
 * administration (ADMIN)" table: users/user/createUser/updateUser/
 * lockUser/unlockUser/deleteUser/assignRoleToUser/removeRoleFromUser,
 * each called with an ADMIN JWT obtained through the real login
 * mutation, plus the class-level @PreAuthorize("hasRole('ADMIN')")
 * rejecting both an anonymous caller and an authenticated caller
 * without the ADMIN role.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class UserControllerTest extends GraphQlIntegrationTestSupport {

	private static final String CREATE_USER_MUTATION = """
			mutation($email: String!) {
			  createUser(input: { firstName: "Test", lastName: "User", email: $email, password: "secret-password" }) {
			    id
			    email
			    enabled
			  }
			}
			""";

	private long createUserAsAdmin(HttpGraphQlTester admin, String email) {
		return admin.document(CREATE_USER_MUTATION)
				.variable("email", email)
				.execute()
				.path("createUser.id")
				.entity(Long.class)
				.get();
	}

	@Test
	void anonymousCallerIsForbidden() {
		graphQlTester.document("{ users(page: 0, size: 5) { totalElements } }")
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void authenticatedNonAdminCallerIsForbidden() {
		String email = uniqueEmail("non-admin");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token).document(CREATE_USER_MUTATION)
				.variable("email", uniqueEmail("should-not-be-created"))
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createUserEnablesTheAccountImmediately() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String email = uniqueEmail("created-by-admin");

		admin.document(CREATE_USER_MUTATION)
				.variable("email", email)
				.execute()
				.path("createUser.enabled")
				.entity(Boolean.class)
				.isEqualTo(true);
	}

	@Test
	void createUserRejectsDuplicateEmail() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String email = uniqueEmail("duplicate-admin-created");
		createUserAsAdmin(admin, email);

		admin.document(CREATE_USER_MUTATION)
				.variable("email", email)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void usersListsCreatedUsers() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String email = uniqueEmail("listed");
		createUserAsAdmin(admin, email);

		admin.document("{ users(page: 0, size: 500) { content { email } totalElements } }")
				.execute()
				.path("users.content[*].email")
				.entityList(String.class)
				.satisfies(emails -> assertThat(emails).contains(email));
	}

	@Test
	void userReturnsDetailById() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String email = uniqueEmail("detail");
		long id = createUserAsAdmin(admin, email);

		admin.document("query($id: ID!) { user(id: $id) { email } }")
				.variable("id", id)
				.execute()
				.path("user.email")
				.entity(String.class)
				.isEqualTo(email);
	}

	@Test
	void userReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("query($id: ID!) { user(id: $id) { email } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateUserChangesProfileFields() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String originalEmail = uniqueEmail("before-update");
		long id = createUserAsAdmin(admin, originalEmail);
		String updatedEmail = uniqueEmail("after-update");

		admin.document("""
				mutation($id: ID!, $email: String!) {
				  updateUser(id: $id, input: { firstName: "Updated", lastName: "Name", email: $email }) {
				    firstName
				    email
				  }
				}
				""")
				.variable("id", id)
				.variable("email", updatedEmail)
				.execute()
				.path("updateUser.email")
				.entity(String.class)
				.isEqualTo(updatedEmail);
	}

	@Test
	void updateUserReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("""
				mutation($id: ID!, $email: String!) {
				  updateUser(id: $id, input: { firstName: "Updated", lastName: "Name", email: $email }) {
				    email
				  }
				}
				""")
				.variable("id", 999_999_999L)
				.variable("email", uniqueEmail("unknown"))
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void lockThenUnlockUserRoundTrips() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createUserAsAdmin(admin, uniqueEmail("lockable"));

		admin.document("mutation($id: ID!) { lockUser(id: $id) { accountLocked } }")
				.variable("id", id)
				.execute()
				.path("lockUser.accountLocked")
				.entity(Boolean.class)
				.isEqualTo(true);

		admin.document("mutation($id: ID!) { unlockUser(id: $id) { accountLocked } }")
				.variable("id", id)
				.execute()
				.path("unlockUser.accountLocked")
				.entity(Boolean.class)
				.isEqualTo(false);
	}

	@Test
	void lockUserReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { lockUser(id: $id) { accountLocked } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteUserRemovesTheAccount() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createUserAsAdmin(admin, uniqueEmail("deletable"));

		admin.document("mutation($id: ID!) { deleteUser(id: $id) }")
				.variable("id", id)
				.execute()
				.path("deleteUser")
				.entity(Boolean.class)
				.isEqualTo(true);

		admin.document("query($id: ID!) { user(id: $id) { email } }")
				.variable("id", id)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteUserReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { deleteUser(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void assignThenRemoveRoleFromUserRoundTrips() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long userId = createUserAsAdmin(admin, uniqueEmail("role-assignee"));
		Role role = roleRepository.save(Role.builder().roleName("REPORTS_VIEWER_" + userId).build());

		admin.document("mutation($userId: ID!, $roleId: ID!) { assignRoleToUser(userId: $userId, roleId: $roleId) { roles { roleName } } }")
				.variable("userId", userId)
				.variable("roleId", role.getId())
				.execute()
				.path("assignRoleToUser.roles[*].roleName")
				.entityList(String.class)
				.satisfies(roleNames -> assertThat(roleNames).contains(role.getRoleName()));

		admin.document("mutation($userId: ID!, $roleId: ID!) { removeRoleFromUser(userId: $userId, roleId: $roleId) { roles { roleName } } }")
				.variable("userId", userId)
				.variable("roleId", role.getId())
				.execute()
				.path("removeRoleFromUser.roles[*].roleName")
				.entityList(String.class)
				.satisfies(roleNames -> assertThat(roleNames).doesNotContain(role.getRoleName()));
	}

	@Test
	void assignRoleToUserReturnsNotFoundForUnknownRole() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long userId = createUserAsAdmin(admin, uniqueEmail("role-target"));

		admin.document("mutation($userId: ID!, $roleId: ID!) { assignRoleToUser(userId: $userId, roleId: $roleId) { id } }")
				.variable("userId", userId)
				.variable("roleId", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

}
