package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for CategoryController, run against the real GraphQL
 * endpoint. Covers every operation from the README's feature/categories
 * table (categories/category/createCategory/updateCategory/
 * deleteCategory), the per-method @PreAuthorize("hasRole('ADMIN')")
 * rejecting a non-admin caller, and the business rule that a category
 * still referenced by a product cannot be deleted. No Product entity
 * exists yet on this branch, so the product row backing that last case is
 * inserted with a raw JDBC insert against the already-existing products
 * table, committed on its own connection so the real server thread
 * handling the GraphQL request can see it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class CategoryControllerTest extends GraphQlIntegrationTestSupport {

	@Autowired
	private DataSource dataSource;

	private String uniqueCategoryName(String label) {
		return label + "-" + UUID.randomUUID();
	}

	private long createCategoryAsAdmin(HttpGraphQlTester admin, String categoryName) {
		return admin
				.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { id } }")
				.variable("categoryName", categoryName)
				.execute()
				.path("createCategory.id")
				.entity(Long.class)
				.get();
	}

	/** Inserts a product row referencing the given category on its own, immediately committed connection. */
	private void insertProductReferencingCategory(long categoryId, String productName) {
		String sql = "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, ?, 9.99)";
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, categoryId);
			statement.setString(2, productName);
			statement.executeUpdate();
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to insert test product", exception);
		}
	}

	@Test
	void categoriesReturnsAPaginatedListIncludingACreatedCategory() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = uniqueCategoryName("books");
		createCategoryAsAdmin(admin, categoryName);

		graphQlTester.document("query($page: Int, $size: Int) { categories(page: $page, size: $size) { content { categoryName } totalElements page size } }")
				.variable("page", 0)
				.variable("size", 100)
				.execute()
				.path("categories.content[*].categoryName")
				.entityList(String.class)
				.satisfies(names -> assertThat(names).contains(categoryName));
	}

	@Test
	void categoryReturnsDetailById() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = uniqueCategoryName("detail");
		long id = createCategoryAsAdmin(admin, categoryName);

		graphQlTester.document("query($id: ID!) { category(id: $id) { categoryName } }")
				.variable("id", id)
				.execute()
				.path("category.categoryName")
				.entity(String.class)
				.isEqualTo(categoryName);
	}

	@Test
	void categoryReturnsNotFoundForUnknownId() {
		graphQlTester.document("query($id: ID!) { category(id: $id) { categoryName } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createCategoryAsAdminSucceeds() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = uniqueCategoryName("new-category");

		admin.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { categoryName } }")
				.variable("categoryName", categoryName)
				.execute()
				.path("createCategory.categoryName")
				.entity(String.class)
				.isEqualTo(categoryName);
	}

	@Test
	void createCategoryRejectsDuplicateName() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = uniqueCategoryName("duplicate");
		createCategoryAsAdmin(admin, categoryName);

		admin.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { id } }")
				.variable("categoryName", categoryName)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createCategoryIsForbiddenForNonAdminCaller() {
		String email = uniqueEmail("non-admin-category");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token)
				.document("mutation($categoryName: String!) { createCategory(input: { categoryName: $categoryName }) { id } }")
				.variable("categoryName", uniqueCategoryName("forbidden"))
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateCategoryChangesTheName() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createCategoryAsAdmin(admin, uniqueCategoryName("before-update"));
		String newName = uniqueCategoryName("after-update");

		admin.document("mutation($id: ID!, $categoryName: String!) { updateCategory(id: $id, input: { categoryName: $categoryName }) { categoryName } }")
				.variable("id", id)
				.variable("categoryName", newName)
				.execute()
				.path("updateCategory.categoryName")
				.entity(String.class)
				.isEqualTo(newName);
	}

	@Test
	void updateCategoryReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!, $categoryName: String!) { updateCategory(id: $id, input: { categoryName: $categoryName }) { categoryName } }")
				.variable("id", 999_999_999L)
				.variable("categoryName", uniqueCategoryName("unknown"))
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateCategoryRejectsNameAlreadyUsedByAnotherCategory() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String takenName = uniqueCategoryName("taken");
		createCategoryAsAdmin(admin, takenName);
		long id = createCategoryAsAdmin(admin, uniqueCategoryName("to-rename"));

		admin.document("mutation($id: ID!, $categoryName: String!) { updateCategory(id: $id, input: { categoryName: $categoryName }) { id } }")
				.variable("id", id)
				.variable("categoryName", takenName)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteCategoryRemovesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createCategoryAsAdmin(admin, uniqueCategoryName("deletable"));

		admin.document("mutation($id: ID!) { deleteCategory(id: $id) }")
				.variable("id", id)
				.execute()
				.path("deleteCategory")
				.entity(Boolean.class)
				.isEqualTo(true);

		graphQlTester.document("query($id: ID!) { category(id: $id) { categoryName } }")
				.variable("id", id)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteCategoryReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { deleteCategory(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteCategoryIsRejectedWhenItStillHasProducts() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long id = createCategoryAsAdmin(admin, uniqueCategoryName("still-has-products"));
		insertProductReferencingCategory(id, "Product-" + UUID.randomUUID());

		admin.document("mutation($id: ID!) { deleteCategory(id: $id) }")
				.variable("id", id)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

}
