package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for RoleController, run against the real GraphQL
 * endpoint. Covers every operation from the README's "role/permission
 * administration (ADMIN)" table (roles/role/permissions/createRole/
 * updateRole/deleteRole/createPermission/deletePermission/
 * assignPermissionToRole/removePermissionFromRole), the class-level
 * @PreAuthorize("hasRole('ADMIN')") rejecting a non-admin caller, and
 * the full admin flow the README explicitly asks for: create a role,
 * create a permission, assign the permission to the role, create a
 * user, assign the role to the user.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class RoleControllerTest extends GraphQlIntegrationTestSupport {

	private String uniqueRoleName(String label) {
		// role_name is VARCHAR(50) in V1__init_schema.sql, so only a short
		// random suffix is appended, not a full UUID.
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		return (label + "_" + suffix).toUpperCase();
	}

	private long createRoleAsAdmin(HttpGraphQlTester admin, String roleName) {
		return admin.document("mutation($roleName: String!) { createRole(input: { roleName: $roleName }) { id } }")
				.variable("roleName", roleName)
				.execute()
				.path("createRole.id")
				.entity(Long.class)
				.get();
	}

	private long createPermissionAsAdmin(HttpGraphQlTester admin, String resource, String action) {
		return admin.document("""
				mutation($resource: String!, $action: String!) {
				  createPermission(input: { resource: $resource, action: $action }) { id }
				}
				""")
				.variable("resource", resource)
				.variable("action", action)
				.execute()
				.path("createPermission.id")
				.entity(Long.class)
				.get();
	}

	@Test
	void authenticatedNonAdminCallerIsForbidden() {
		String email = uniqueEmail("non-admin-role");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token).document("{ roles { roleName } }")
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createRoleThenListingRolesIncludesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String roleName = uniqueRoleName("viewer");
		createRoleAsAdmin(admin, roleName);

		admin.document("{ roles { roleName } }")
				.execute()
				.path("roles[*].roleName")
				.entityList(String.class)
				.satisfies(names -> assertThat(names).contains(roleName));
	}

	@Test
	void createRoleRejectsDuplicateName() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String roleName = uniqueRoleName("duplicate");
		createRoleAsAdmin(admin, roleName);

		admin.document("mutation($roleName: String!) { createRole(input: { roleName: $roleName }) { id } }")
				.variable("roleName", roleName)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void roleReturnsDetailById() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String roleName = uniqueRoleName("detail");
		long id = createRoleAsAdmin(admin, roleName);

		admin.document("query($id: ID!) { role(id: $id) { roleName } }")
				.variable("id", id)
				.execute()
				.path("role.roleName")
				.entity(String.class)
				.isEqualTo(roleName);
	}

	@Test
	void roleReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("query($id: ID!) { role(id: $id) { roleName } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateRoleChangesTheName() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createRoleAsAdmin(admin, uniqueRoleName("before-update"));
		String newName = uniqueRoleName("after-update");

		admin.document("mutation($id: ID!, $roleName: String!) { updateRole(id: $id, input: { roleName: $roleName }) { roleName } }")
				.variable("id", id)
				.variable("roleName", newName)
				.execute()
				.path("updateRole.roleName")
				.entity(String.class)
				.isEqualTo(newName);
	}

	@Test
	void updateRoleReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!, $roleName: String!) { updateRole(id: $id, input: { roleName: $roleName }) { roleName } }")
				.variable("id", 999_999_999L)
				.variable("roleName", uniqueRoleName("unknown"))
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteRoleRemovesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createRoleAsAdmin(admin, uniqueRoleName("deletable"));

		admin.document("mutation($id: ID!) { deleteRole(id: $id) }")
				.variable("id", id)
				.execute()
				.path("deleteRole")
				.entity(Boolean.class)
				.isEqualTo(true);

		admin.document("query($id: ID!) { role(id: $id) { roleName } }")
				.variable("id", id)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteRoleReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { deleteRole(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createPermissionThenListingPermissionsIncludesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String resource = "resource-" + UUID.randomUUID();
		createPermissionAsAdmin(admin, resource, "read");

		admin.document("{ permissions { resource action } }")
				.execute()
				.path("permissions[*].resource")
				.entityList(String.class)
				.satisfies(resources -> assertThat(resources).contains(resource));
	}

	@Test
	void createPermissionRejectsDuplicateResourceActionPair() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String resource = "resource-" + UUID.randomUUID();
		createPermissionAsAdmin(admin, resource, "delete");

		admin.document("""
				mutation($resource: String!, $action: String!) {
				  createPermission(input: { resource: $resource, action: $action }) { id }
				}
				""")
				.variable("resource", resource)
				.variable("action", "delete")
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deletePermissionRemovesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String resource = "resource-" + UUID.randomUUID();
		long id = createPermissionAsAdmin(admin, resource, "read");

		admin.document("mutation($id: ID!) { deletePermission(id: $id) }")
				.variable("id", id)
				.execute()
				.path("deletePermission")
				.entity(Boolean.class)
				.isEqualTo(true);
	}

	@Test
	void deletePermissionReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { deletePermission(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void assignThenRemovePermissionFromRoleRoundTrips() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long roleId = createRoleAsAdmin(admin, uniqueRoleName("permission-holder"));
		String resource = "resource-" + UUID.randomUUID();
		long permissionId = createPermissionAsAdmin(admin, resource, "read");

		admin.document("mutation($roleId: ID!, $permissionId: ID!) { assignPermissionToRole(roleId: $roleId, permissionId: $permissionId) { permissions { resource } } }")
				.variable("roleId", roleId)
				.variable("permissionId", permissionId)
				.execute()
				.path("assignPermissionToRole.permissions[*].resource")
				.entityList(String.class)
				.satisfies(resources -> assertThat(resources).contains(resource));

		admin.document("mutation($roleId: ID!, $permissionId: ID!) { removePermissionFromRole(roleId: $roleId, permissionId: $permissionId) { permissions { resource } } }")
				.variable("roleId", roleId)
				.variable("permissionId", permissionId)
				.execute()
				.path("removePermissionFromRole.permissions[*].resource")
				.entityList(String.class)
				.satisfies(resources -> assertThat(resources).doesNotContain(resource));
	}

	@Test
	void assignPermissionToRoleReturnsNotFoundForUnknownPermission() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long roleId = createRoleAsAdmin(admin, uniqueRoleName("permission-target"));

		admin.document("mutation($roleId: ID!, $permissionId: ID!) { assignPermissionToRole(roleId: $roleId, permissionId: $permissionId) { id } }")
				.variable("roleId", roleId)
				.variable("permissionId", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	/**
	 * The full administration flow the README asks for explicitly: create a
	 * role, create a permission, assign the permission to the role, create a
	 * user, assign the role to the user. Verified end to end through the
	 * real GraphQL mutations and queries (UserController's user query is
	 * involved too, since assignRoleToUser lives there), not by asserting
	 * repository state directly.
	 */
	@Test
	void fullAdministrationFlowCreatesRolePermissionAndUserThenWiresThemTogether() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		String roleName = uniqueRoleName("editor");
		long roleId = createRoleAsAdmin(admin, roleName);

		String resource = "article-" + UUID.randomUUID();
		long permissionId = createPermissionAsAdmin(admin, resource, "edit");

		admin.document("mutation($roleId: ID!, $permissionId: ID!) { assignPermissionToRole(roleId: $roleId, permissionId: $permissionId) { permissions { resource action } } }")
				.variable("roleId", roleId)
				.variable("permissionId", permissionId)
				.execute()
				.path("assignPermissionToRole.permissions[*].resource")
				.entityList(String.class)
				.satisfies(resources -> assertThat(resources).contains(resource));

		String userEmail = uniqueEmail("flow-user");
		long userId = admin.document("mutation($email: String!) { createUser(input: { firstName: \"Flow\", lastName: \"User\", email: $email, password: \"secret-password\" }) { id } }")
				.variable("email", userEmail)
				.execute()
				.path("createUser.id")
				.entity(Long.class)
				.get();

		admin.document("mutation($userId: ID!, $roleId: ID!) { assignRoleToUser(userId: $userId, roleId: $roleId) { roles { roleName } } }")
				.variable("userId", userId)
				.variable("roleId", roleId)
				.execute()
				.path("assignRoleToUser.roles[*].roleName")
				.entityList(String.class)
				.satisfies(roleNames -> assertThat(roleNames).contains(roleName));

		admin.document("query($id: ID!) { user(id: $id) { email roles { roleName permissions { resource action } } } }")
				.variable("id", userId)
				.execute()
				.path("user.roles[*].roleName")
				.entityList(String.class)
				.satisfies(roleNames -> assertThat(roleNames).contains(roleName));
	}

}
