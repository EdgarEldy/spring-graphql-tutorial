package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.entity.Role;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end DataLoader batching tests: resolving User.roles for a page of
 * users, Role.permissions for the full role list, or Product.category for a
 * page of products, through the real GraphQL endpoint issues a constant
 * number of SQL queries, not one per parent, which is exactly what
 * CLAUDE.md requires a test for on every relation the DataLoaderConfig
 * batches (userRoles/rolePermissions on feature/auth, the
 * forTypePair(Long.class, Category.class) loader added on
 * feature/products, and the forTypePair(Long.class, Customer.class)/
 * forTypePair(Long.class, Product.class) loaders added on feature/orders
 * for Order.customer/Order.product). Hibernate Statistics on the running
 * application's
 * SessionFactory count real SQL statements, the same mechanism used by
 * the repository-level DataLoader tests (UserRepositoryTest,
 * RoleRepositoryTest) so this project has a single query-counting
 * mechanism, not two.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class DataLoaderBatchingTest extends GraphQlIntegrationTestSupport {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private Statistics statistics() {
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		return sessionFactory.getStatistics();
	}

	private long createUserWithRole(HttpGraphQlTester admin, long roleId) {
		long userId = admin.document("""
				mutation($email: String!) {
				  createUser(input: { firstName: "Batch", lastName: "User", email: $email, password: "secret-password" }) {
				    id
				  }
				}
				""")
				.variable("email", uniqueEmail("batched"))
				.execute()
				.path("createUser.id")
				.entity(Long.class)
				.get();

		admin.document("mutation($userId: ID!, $roleId: ID!) { assignRoleToUser(userId: $userId, roleId: $roleId) { id } }")
				.variable("userId", userId)
				.variable("roleId", roleId)
				.executeAndVerify();

		return userId;
	}

	@Test
	void userRolesResolvesThroughAConstantNumberOfQueriesRegardlessOfUserCount() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		Role role = roleRepository
				.save(Role.builder().roleName("BATCH_ROLE_" + UUID.randomUUID().toString().substring(0, 8)).build());

		for (int i = 0; i < 3; i++) {
			createUserWithRole(admin, role.getId());
		}

		Statistics statistics = statistics();
		statistics.clear();
		List<String> firstRunRoleNames = fetchAllUsersWithRoles(admin);
		long queriesForFirstBatch = statistics.getQueryExecutionCount();

		for (int i = 0; i < 3; i++) {
			createUserWithRole(admin, role.getId());
		}

		statistics.clear();
		List<String> secondRunRoleNames = fetchAllUsersWithRoles(admin);
		long queriesForSecondBatch = statistics.getQueryExecutionCount();

		assertThat(firstRunRoleNames).contains(role.getRoleName());
		assertThat(secondRunRoleNames).contains(role.getRoleName());
		assertThat(queriesForSecondBatch).isEqualTo(queriesForFirstBatch);
		assertThat(queriesForSecondBatch).isLessThanOrEqualTo(5L);
	}

	private List<String> fetchAllUsersWithRoles(HttpGraphQlTester admin) {
		return admin.document("{ users(page: 0, size: 5000) { content { roles { roleName } } } }")
				.execute()
				.path("users.content[*].roles[*].roleName")
				.entityList(String.class)
				.get();
	}

	private long createRoleWithPermission(HttpGraphQlTester admin, long permissionId) {
		long roleId = admin.document("""
				mutation($roleName: String!) { createRole(input: { roleName: $roleName }) { id } }
				""")
				.variable("roleName", "BATCH_ROLE_" + UUID.randomUUID().toString().substring(0, 8))
				.execute()
				.path("createRole.id")
				.entity(Long.class)
				.get();

		admin.document("mutation($roleId: ID!, $permissionId: ID!) { assignPermissionToRole(roleId: $roleId, permissionId: $permissionId) { id } }")
				.variable("roleId", roleId)
				.variable("permissionId", permissionId)
				.executeAndVerify();

		return roleId;
	}

	@Test
	void rolePermissionsResolvesThroughAConstantNumberOfQueriesRegardlessOfRoleCount() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		Permission permission = permissionForBatchTest(admin);

		for (int i = 0; i < 3; i++) {
			createRoleWithPermission(admin, permission.getId());
		}

		Statistics statistics = statistics();
		statistics.clear();
		List<String> firstRunResources = fetchAllRolesWithPermissions(admin);
		long queriesForFirstBatch = statistics.getQueryExecutionCount();

		for (int i = 0; i < 3; i++) {
			createRoleWithPermission(admin, permission.getId());
		}

		statistics.clear();
		List<String> secondRunResources = fetchAllRolesWithPermissions(admin);
		long queriesForSecondBatch = statistics.getQueryExecutionCount();

		assertThat(firstRunResources).contains(permission.getResource());
		assertThat(secondRunResources).contains(permission.getResource());
		assertThat(queriesForSecondBatch).isEqualTo(queriesForFirstBatch);
		assertThat(queriesForSecondBatch).isLessThanOrEqualTo(5L);
	}

	private Permission permissionForBatchTest(HttpGraphQlTester admin) {
		String resource = "batch-resource-" + UUID.randomUUID();
		long permissionId = admin.document("""
				mutation($resource: String!) { createPermission(input: { resource: $resource, action: "batch" }) { id } }
				""")
				.variable("resource", resource)
				.execute()
				.path("createPermission.id")
				.entity(Long.class)
				.get();
		return Permission.builder().id(permissionId).resource(resource).action("batch").build();
	}

	private List<String> fetchAllRolesWithPermissions(HttpGraphQlTester admin) {
		return admin.document("{ roles { permissions { resource } } }")
				.execute()
				.path("roles[*].permissions[*].resource")
				.entityList(String.class)
				.get();
	}

	private long createProductInCategory(HttpGraphQlTester admin, long categoryId) {
		return admin.document("""
				mutation($productName: String!, $categoryId: ID!) {
				  createProduct(input: { productName: $productName, unitPrice: 9.99, categoryId: $categoryId }) {
				    id
				  }
				}
				""")
				.variable("productName", "batch-product-" + UUID.randomUUID())
				.variable("categoryId", categoryId)
				.execute()
				.path("createProduct.id")
				.entity(Long.class)
				.get();
	}

	private List<String> fetchAllProductsWithCategory(HttpGraphQlTester admin) {
		return admin.document("{ products(size: 5000) { content { category { categoryName } } } }")
				.execute()
				.path("products.content[*].category.categoryName")
				.entityList(String.class)
				.get();
	}

	@Test
	void productCategoryResolvesThroughAConstantNumberOfQueriesRegardlessOfProductCount() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = "batch-category-" + UUID.randomUUID();
		long categoryId = admin
				.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { id } }")
				.variable("categoryName", categoryName)
				.execute()
				.path("createCategory.id")
				.entity(Long.class)
				.get();

		for (int i = 0; i < 5; i++) {
			createProductInCategory(admin, categoryId);
		}

		Statistics statistics = statistics();
		statistics.clear();
		List<String> firstRunCategoryNames = fetchAllProductsWithCategory(admin);
		long queriesForFirstBatch = statistics.getQueryExecutionCount();

		for (int i = 0; i < 5; i++) {
			createProductInCategory(admin, categoryId);
		}

		statistics.clear();
		List<String> secondRunCategoryNames = fetchAllProductsWithCategory(admin);
		long queriesForSecondBatch = statistics.getQueryExecutionCount();

		assertThat(firstRunCategoryNames).contains(categoryName);
		assertThat(secondRunCategoryNames).contains(categoryName);
		assertThat(queriesForSecondBatch).isEqualTo(queriesForFirstBatch);
		assertThat(queriesForSecondBatch).isLessThanOrEqualTo(5L);
	}

	private long createCustomer(String email) {
		return graphQlTester.document("""
				mutation($email: String!) {
				  createCustomer(input: { firstName: "Batch", lastName: "Customer", telephone: "555-0100", email: $email, address: "1 Batch Way" }) {
				    id
				  }
				}
				""")
				.variable("email", email)
				.execute()
				.path("createCustomer.id")
				.entity(Long.class)
				.get();
	}

	private long createOrder(long customerId, long productId) {
		return graphQlTester.document("""
				mutation($customerId: ID!, $productId: ID!) {
				  createOrder(input: { customerId: $customerId, productId: $productId, quantity: 1 }) {
				    id
				  }
				}
				""")
				.variable("customerId", customerId)
				.variable("productId", productId)
				.execute()
				.path("createOrder.id")
				.entity(Long.class)
				.get();
	}

	private List<String> fetchAllOrdersWithCustomerAndProduct() {
		return graphQlTester.document("{ orders(size: 5000) { content { customer { email } product { productName } } } }")
				.execute()
				.path("orders.content[*].customer.email")
				.entityList(String.class)
				.get();
	}

	@Test
	void orderCustomerAndProductResolveThroughAConstantNumberOfQueriesRegardlessOfOrderCount() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = "batch-order-category-" + UUID.randomUUID();
		long categoryId = admin
				.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { id } }")
				.variable("categoryName", categoryName)
				.execute()
				.path("createCategory.id")
				.entity(Long.class)
				.get();
		long productId = createProductInCategory(admin, categoryId);
		String customerEmail = uniqueEmail("batch-order-customer");
		long customerId = createCustomer(customerEmail);

		for (int i = 0; i < 5; i++) {
			createOrder(customerId, productId);
		}

		Statistics statistics = statistics();
		statistics.clear();
		List<String> firstRunCustomerEmails = fetchAllOrdersWithCustomerAndProduct();
		long queriesForFirstBatch = statistics.getQueryExecutionCount();

		for (int i = 0; i < 5; i++) {
			createOrder(customerId, productId);
		}

		statistics.clear();
		List<String> secondRunCustomerEmails = fetchAllOrdersWithCustomerAndProduct();
		long queriesForSecondBatch = statistics.getQueryExecutionCount();

		assertThat(firstRunCustomerEmails).contains(customerEmail);
		assertThat(secondRunCustomerEmails).contains(customerEmail);
		assertThat(queriesForSecondBatch).isEqualTo(queriesForFirstBatch);
		assertThat(queriesForSecondBatch).isLessThanOrEqualTo(6L);
	}

}
