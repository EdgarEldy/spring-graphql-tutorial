package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * End-to-end tests for OrderController, run against the real GraphQL
 * endpoint. Covers every operation from the README's feature/orders table
 * (orders/order/createOrder), the customerId filter on the paginated list,
 * the total computation (quantity * product.unitPrice), and the NOT_FOUND
 * rejection of an unknown customerId/productId on createOrder. No
 * @PreAuthorize is exercised here: the README's feature/orders table does
 * not mark any operation as admin only, same reasoning as
 * CustomerControllerTest on feature/customers.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
class OrderControllerTest extends GraphQlIntegrationTestSupport {

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

	private long createCustomer(String email) {
		return graphQlTester.document("""
				mutation($email: String!) {
				  createCustomer(input: { firstName: "Ada", lastName: "Lovelace", telephone: "555-0100", email: $email, address: "1 Analytical Engine Way" }) {
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

	private long createOrder(long customerId, long productId, int quantity) {
		return graphQlTester.document("""
				mutation($customerId: ID!, $productId: ID!, $quantity: Int!) {
				  createOrder(input: { customerId: $customerId, productId: $productId, quantity: $quantity }) {
				    id
				  }
				}
				""")
				.variable("customerId", customerId)
				.variable("productId", productId)
				.variable("quantity", quantity)
				.execute()
				.path("createOrder.id")
				.entity(Long.class)
				.get();
	}

	@Test
	void createOrderComputesTotalFromQuantityAndProductUnitPrice() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("clean-code"), 19.99, categoryId);
		long customerId = createCustomer(uniqueEmail("order-customer"));

		graphQlTester.document("""
				mutation($customerId: ID!, $productId: ID!) {
				  createOrder(input: { customerId: $customerId, productId: $productId, quantity: 3 }) {
				    quantity
				    total
				    customer { email }
				    product { productName }
				  }
				}
				""")
				.variable("customerId", customerId)
				.variable("productId", productId)
				.execute()
				.path("createOrder.quantity")
				.entity(Integer.class)
				.isEqualTo(3)
				.path("createOrder.total")
				.entity(Double.class)
				.isEqualTo(59.97);
	}

	@Test
	void createOrderReturnsNotFoundWhenCustomerIdDoesNotExist() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("orphan-customer"), 9.99, categoryId);

		graphQlTester.document("""
				mutation($productId: ID!) {
				  createOrder(input: { customerId: 999999999, productId: $productId, quantity: 1 }) {
				    id
				  }
				}
				""")
				.variable("productId", productId)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createOrderReturnsNotFoundWhenProductIdDoesNotExist() {
		long customerId = createCustomer(uniqueEmail("order-orphan-product"));

		graphQlTester.document("""
				mutation($customerId: ID!) {
				  createOrder(input: { customerId: $customerId, productId: 999999999, quantity: 1 }) {
				    id
				  }
				}
				""")
				.variable("customerId", customerId)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void orderReturnsDetailByIdIncludingCustomerAndProduct() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("effective-java"), 44.99, categoryId);
		String customerEmail = uniqueEmail("order-detail");
		long customerId = createCustomer(customerEmail);
		long orderId = createOrder(customerId, productId, 2);

		graphQlTester.document("query($id: ID!) { order(id: $id) { quantity total customer { email } product { productName } } }")
				.variable("id", orderId)
				.execute()
				.path("order.quantity")
				.entity(Integer.class)
				.isEqualTo(2)
				.path("order.customer.email")
				.entity(String.class)
				.isEqualTo(customerEmail);
	}

	@Test
	void orderReturnsNotFoundForUnknownId() {
		graphQlTester.document("query($id: ID!) { order(id: $id) { id } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void ordersReturnsAPaginatedListIncludingACreatedOrder() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("paginated-book"), 9.99, categoryId);
		long customerId = createCustomer(uniqueEmail("order-list"));
		long orderId = createOrder(customerId, productId, 1);

		graphQlTester.document("query($page: Int, $size: Int) { orders(page: $page, size: $size) { content { id } totalElements page size } }")
				.variable("page", 0)
				.variable("size", 500)
				.execute()
				.path("orders.content[*].id")
				.entityList(String.class)
				.satisfies(ids -> assertThat(ids).contains(String.valueOf(orderId)));
	}

	@Test
	void ordersFiltersByCustomerId() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, uniqueName("books"));
		long productId = createProductAsAdmin(admin, uniqueName("filter-book"), 9.99, categoryId);
		long aliceId = createCustomer(uniqueEmail("order-filter-alice"));
		long bobId = createCustomer(uniqueEmail("order-filter-bob"));
		long aliceOrderId = createOrder(aliceId, productId, 1);
		long bobOrderId = createOrder(bobId, productId, 1);

		graphQlTester.document("query($customerId: ID, $size: Int) { orders(customerId: $customerId, size: $size) { content { id } } }")
				.variable("customerId", aliceId)
				.variable("size", 500)
				.execute()
				.path("orders.content[*].id")
				.entityList(String.class)
				.satisfies(ids -> {
					assertThat(ids).contains(String.valueOf(aliceOrderId));
					assertThat(ids).doesNotContain(String.valueOf(bobOrderId));
				});
	}

}
