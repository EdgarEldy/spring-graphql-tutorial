package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for ProductController, run against the real GraphQL
 * endpoint. Covers every operation from the README's feature/products table
 * (products/product/createProduct/updateProduct/deleteProduct), the
 * categoryId filter on the paginated list, the per-method
 * @PreAuthorize("hasRole('ADMIN')") rejecting a non-admin caller, and the
 * rule that create/update reject a categoryId that does not resolve to an
 * existing Category.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class ProductControllerTest extends GraphQlIntegrationTestSupport {

	private String uniqueName(String label) {
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

	private long createProductAsAdmin(HttpGraphQlTester admin, String productName, double unitPrice, long categoryId) {
		return admin.document("""
				mutation($productName: String!, $unitPrice: Float!, $categoryId: ID!) {
				  createProduct(input: { productName: $productName, unitPrice: $unitPrice, categoryId: $categoryId }) {
				    id
				  }
				}
				""")
				.variable("productName", productName)
				.variable("unitPrice", unitPrice)
				.variable("categoryId", categoryId)
				.execute()
				.path("createProduct.id")
				.entity(Long.class)
				.get();
	}

	@Test
	void productsReturnsAPaginatedListIncludingACreatedProduct() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		String productName = uniqueName("clean-code");
		createProductAsAdmin(admin, productName, 39.99, categoryId);

		graphQlTester.document("query($page: Int, $size: Int) { products(page: $page, size: $size) { content { productName } totalElements page size } }")
				.variable("page", 0)
				.variable("size", 500)
				.execute()
				.path("products.content[*].productName")
				.entityList(String.class)
				.satisfies(names -> assertThat(names).contains(productName));
	}

	@Test
	void productsFiltersByCategoryId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long booksId = createCategoryAsAdmin(admin, uniqueName("books"));
		long toysId = createCategoryAsAdmin(admin, uniqueName("toys"));
		String bookName = uniqueName("clean-code");
		String toyName = uniqueName("building-blocks");
		createProductAsAdmin(admin, bookName, 39.99, booksId);
		createProductAsAdmin(admin, toyName, 19.99, toysId);

		graphQlTester.document("query($categoryId: ID, $size: Int) { products(categoryId: $categoryId, size: $size) { content { productName } } }")
				.variable("categoryId", booksId)
				.variable("size", 500)
				.execute()
				.path("products.content[*].productName")
				.entityList(String.class)
				.satisfies(names -> {
					assertThat(names).contains(bookName);
					assertThat(names).doesNotContain(toyName);
				});
	}

	@Test
	void productReturnsDetailByIdIncludingItsCategory() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		String categoryName = uniqueName("books");
		long categoryId = createCategoryAsAdmin(admin, categoryName);
		String productName = uniqueName("clean-code");
		long productId = createProductAsAdmin(admin, productName, 39.99, categoryId);

		graphQlTester.document("query($id: ID!) { product(id: $id) { productName unitPrice category { categoryName } } }")
				.variable("id", productId)
				.execute()
				.path("product.productName")
				.entity(String.class)
				.isEqualTo(productName)
				.path("product.category.categoryName")
				.entity(String.class)
				.isEqualTo(categoryName);
	}

	@Test
	void productReturnsNotFoundForUnknownId() {
		graphQlTester.document("query($id: ID!) { product(id: $id) { productName } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createProductAsAdminSucceeds() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		String productName = uniqueName("clean-code");

		admin.document("""
				mutation($productName: String!, $categoryId: ID!) {
				  createProduct(input: { productName: $productName, unitPrice: 39.99, categoryId: $categoryId }) {
				    productName
				    unitPrice
				  }
				}
				""")
				.variable("productName", productName)
				.variable("categoryId", categoryId)
				.execute()
				.path("createProduct.productName")
				.entity(String.class)
				.isEqualTo(productName)
				.path("createProduct.unitPrice")
				.entity(Double.class)
				.isEqualTo(39.99);
	}

	@Test
	void createProductReturnsNotFoundWhenCategoryIdDoesNotExist() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("""
				mutation($productName: String!) {
				  createProduct(input: { productName: $productName, unitPrice: 9.99, categoryId: 999999999 }) {
				    id
				  }
				}
				""")
				.variable("productName", uniqueName("orphan"))
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createProductIsForbiddenForNonAdminCaller() {
		String email = uniqueEmail("non-admin-product");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token)
				.document("""
						mutation($productName: String!) {
						  createProduct(input: { productName: $productName, unitPrice: 9.99, categoryId: 1 }) {
						    id
						  }
						}
						""")
				.variable("productName", uniqueName("forbidden"))
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateProductChangesNamePriceAndCategory() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long booksId = createCategoryAsAdmin(admin, uniqueName("books"));
		long toysId = createCategoryAsAdmin(admin, uniqueName("toys"));
		long productId = createProductAsAdmin(admin, uniqueName("before-update"), 9.99, booksId);
		String newName = uniqueName("after-update");

		admin.document("""
				mutation($id: ID!, $productName: String!, $categoryId: ID!) {
				  updateProduct(id: $id, input: { productName: $productName, unitPrice: 29.99, categoryId: $categoryId }) {
				    productName
				    unitPrice
				    category { categoryName }
				  }
				}
				""")
				.variable("id", productId)
				.variable("productName", newName)
				.variable("categoryId", toysId)
				.execute()
				.path("updateProduct.productName")
				.entity(String.class)
				.isEqualTo(newName)
				.path("updateProduct.unitPrice")
				.entity(Double.class)
				.isEqualTo(29.99);
	}

	@Test
	void updateProductReturnsNotFoundForUnknownProductId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));

		admin.document("""
				mutation($id: ID!, $categoryId: ID!) {
				  updateProduct(id: $id, input: { productName: "Unknown", unitPrice: 9.99, categoryId: $categoryId }) {
				    id
				  }
				}
				""")
				.variable("id", 999_999_999L)
				.variable("categoryId", categoryId)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateProductReturnsNotFoundWhenCategoryIdDoesNotExist() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("existing"), 9.99, categoryId);

		admin.document("""
				mutation($id: ID!) {
				  updateProduct(id: $id, input: { productName: "Existing", unitPrice: 9.99, categoryId: 999999999 }) {
				    id
				  }
				}
				""")
				.variable("id", productId)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateProductIsForbiddenForNonAdminCaller() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("protected"), 9.99, categoryId);
		String email = uniqueEmail("non-admin-update-product");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token)
				.document("""
						mutation($id: ID!, $categoryId: ID!) {
						  updateProduct(id: $id, input: { productName: "Hacked", unitPrice: 1.0, categoryId: $categoryId }) {
						    id
						  }
						}
						""")
				.variable("id", productId)
				.variable("categoryId", categoryId)
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteProductRemovesIt() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("deletable"), 9.99, categoryId);

		admin.document("mutation($id: ID!) { deleteProduct(id: $id) }")
				.variable("id", productId)
				.execute()
				.path("deleteProduct")
				.entity(Boolean.class)
				.isEqualTo(true);

		graphQlTester.document("query($id: ID!) { product(id: $id) { productName } }")
				.variable("id", productId)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteProductReturnsNotFoundForUnknownId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());

		admin.document("mutation($id: ID!) { deleteProduct(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteProductIsForbiddenForNonAdminCaller() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("protected-delete"), 9.99, categoryId);
		String email = uniqueEmail("non-admin-delete-product");
		createEnabledUser(email, "secret-password");
		String token = login(email, "secret-password");

		authenticatedTester(token)
				.document("mutation($id: ID!) { deleteProduct(id: $id) }")
				.variable("id", productId)
				.execute()
				.errors()
				.expect(error -> "FORBIDDEN".equals(error.getExtensions().get("classification")))
				.verify();
	}

}
