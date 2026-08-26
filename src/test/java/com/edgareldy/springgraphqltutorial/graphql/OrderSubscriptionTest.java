package com.edgareldy.springgraphqltutorial.graphql;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * End-to-end test for the orderCreated subscription, run against the real
 * GraphQL endpoint over an actual WebSocket connection (StandardWebSocketClient,
 * backed by the JSR-356 container tomcat-embed-websocket puts on the
 * classpath), not a mocked transport: a subscriber connects and issues the
 * orderCreated subscription, then a createOrder mutation is sent over a
 * separate HTTP connection, and the event the subscriber receives is
 * asserted to be that same order. This is exactly the round trip
 * OrderServiceImpl.create/Sinks.Many/OrderController.orderCreated exist to
 * support, so only observing it end to end over the wire counts as having
 * verified it, reading the code is not enough.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
class OrderSubscriptionTest extends GraphQlIntegrationTestSupport {

	private WebSocketGraphQlTester subscriptionTester;

	@BeforeEach
	void setUpSubscriptionTester() {
		WebSocketClient client = new StandardWebSocketClient();
		subscriptionTester = WebSocketGraphQlTester.builder("ws://localhost:" + port + "/graphql", client).build();
	}

	@AfterEach
	void tearDownSubscriptionTester() {
		subscriptionTester.stop().block(Duration.ofSeconds(5));
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

	@Test
	void orderCreatedStreamsANewOrderToAConnectedSubscriberOverWebSocket() {
		HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
		long categoryId = createCategoryAsAdmin(admin, "sub-books-" + UUID.randomUUID());
		long productId = createProductAsAdmin(admin, "sub-product-" + UUID.randomUUID(), 12.5, categoryId);
		long customerId = createCustomer(uniqueEmail("subscription-customer"));

		Flux<Long> quantities = subscriptionTester.document("subscription { orderCreated { id quantity } }")
				.executeSubscription()
				.toFlux("orderCreated.quantity", Long.class);

		StepVerifier.create(quantities)
				// Gives the server time to actually register the GraphQL
				// subscription over the WebSocket connection (the "subscribe"
				// protocol message) before the mutation below runs: without this,
				// the multicast sink could already have emitted before this
				// subscriber attached, and multicast (unlike replay) never
				// re-delivers a past event to a late subscriber.
				.thenAwait(Duration.ofMillis(500))
				.then(() -> graphQlTester.document("""
						mutation($customerId: ID!, $productId: ID!) {
						  createOrder(input: { customerId: $customerId, productId: $productId, quantity: 4 }) {
						    id
						  }
						}
						""")
						.variable("customerId", customerId)
						.variable("productId", productId)
						.executeAndVerify())
				.expectNext(4L)
				.thenCancel()
				.verify(Duration.ofSeconds(10));
	}

}
