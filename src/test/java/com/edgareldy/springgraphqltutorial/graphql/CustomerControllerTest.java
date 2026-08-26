package com.edgareldy.springgraphqltutorial.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for CustomerController, run against the real GraphQL
 * endpoint. Covers every operation from the README's feature/customers
 * table (customers/customer/createCustomer/updateCustomer/deleteCustomer)
 * and the NOT_FOUND classification for unknown ids. Unlike
 * CategoryControllerTest/ProductControllerTest, no operation here needs an
 * authenticated tester: the schema does not restrict any customer mutation
 * to ROLE_ADMIN.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
class CustomerControllerTest extends GraphQlIntegrationTestSupport {

	private String uniqueName(String label) {
		return label + "-" + UUID.randomUUID();
	}

	private long createCustomer(String firstName, String email) {
		return graphQlTester.document("""
				mutation($firstName: String!, $email: String!) {
				  createCustomer(input: { firstName: $firstName, lastName: "Doe", telephone: "555-0100", email: $email, address: "1 Main Street" }) {
				    id
				  }
				}
				""")
				.variable("firstName", firstName)
				.variable("email", email)
				.execute()
				.path("createCustomer.id")
				.entity(Long.class)
				.get();
	}

	@Test
	void customersReturnsAPaginatedListIncludingACreatedCustomer() {
		String firstName = uniqueName("jane");
		String email = uniqueEmail("customers-list");
		createCustomer(firstName, email);

		graphQlTester.document("query($page: Int, $size: Int) { customers(page: $page, size: $size) { content { firstName } totalElements page size } }")
				.variable("page", 0)
				.variable("size", 500)
				.execute()
				.path("customers.content[*].firstName")
				.entityList(String.class)
				.satisfies(names -> assertThat(names).contains(firstName));
	}

	@Test
	void customerReturnsDetailById() {
		String firstName = uniqueName("detail");
		String email = uniqueEmail("customer-detail");
		long id = createCustomer(firstName, email);

		graphQlTester.document("query($id: ID!) { customer(id: $id) { firstName lastName telephone email address } }")
				.variable("id", id)
				.execute()
				.path("customer.firstName")
				.entity(String.class)
				.isEqualTo(firstName)
				.path("customer.email")
				.entity(String.class)
				.isEqualTo(email);
	}

	@Test
	void customerReturnsNotFoundForUnknownId() {
		graphQlTester.document("query($id: ID!) { customer(id: $id) { firstName } }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void createCustomerSucceeds() {
		String firstName = uniqueName("new-customer");
		String email = uniqueEmail("create-customer");

		graphQlTester.document("""
				mutation($firstName: String!, $email: String!) {
				  createCustomer(input: { firstName: $firstName, lastName: "Doe", telephone: "555-0100", email: $email, address: "1 Main Street" }) {
				    firstName
				    email
				  }
				}
				""")
				.variable("firstName", firstName)
				.variable("email", email)
				.execute()
				.path("createCustomer.firstName")
				.entity(String.class)
				.isEqualTo(firstName)
				.path("createCustomer.email")
				.entity(String.class)
				.isEqualTo(email);
	}

	@Test
	void createCustomerRejectsDuplicateEmail() {
		String email = uniqueEmail("duplicate-customer");
		createCustomer(uniqueName("first"), email);

		graphQlTester.document("""
				mutation($email: String!) {
				  createCustomer(input: { firstName: "Someone Else", lastName: "Doe", telephone: "555-0100", email: $email, address: "1 Main Street" }) {
				    id
				  }
				}
				""")
				.variable("email", email)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateCustomerChangesFields() {
		long id = createCustomer(uniqueName("before-update"), uniqueEmail("before-update"));
		String newFirstName = uniqueName("after-update");
		String newEmail = uniqueEmail("after-update");

		graphQlTester.document("""
				mutation($id: ID!, $firstName: String!, $email: String!) {
				  updateCustomer(id: $id, input: { firstName: $firstName, lastName: "Doe", telephone: "555-0199", email: $email, address: "2 Elm Street" }) {
				    firstName
				    telephone
				    email
				    address
				  }
				}
				""")
				.variable("id", id)
				.variable("firstName", newFirstName)
				.variable("email", newEmail)
				.execute()
				.path("updateCustomer.firstName")
				.entity(String.class)
				.isEqualTo(newFirstName)
				.path("updateCustomer.email")
				.entity(String.class)
				.isEqualTo(newEmail)
				.path("updateCustomer.address")
				.entity(String.class)
				.isEqualTo("2 Elm Street");
	}

	@Test
	void updateCustomerReturnsNotFoundForUnknownId() {
		graphQlTester.document("""
				mutation($id: ID!) {
				  updateCustomer(id: $id, input: { firstName: "Unknown", lastName: "Doe", telephone: "555-0100", email: "unknown@example.com", address: "1 Main Street" }) {
				    id
				  }
				}
				""")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void updateCustomerRejectsEmailAlreadyUsedByAnotherCustomer() {
		String takenEmail = uniqueEmail("taken");
		createCustomer(uniqueName("taken-owner"), takenEmail);
		long id = createCustomer(uniqueName("to-rename"), uniqueEmail("to-rename"));

		graphQlTester.document("""
				mutation($id: ID!, $email: String!) {
				  updateCustomer(id: $id, input: { firstName: "Someone", lastName: "Doe", telephone: "555-0100", email: $email, address: "1 Main Street" }) {
				    id
				  }
				}
				""")
				.variable("id", id)
				.variable("email", takenEmail)
				.execute()
				.errors()
				.expect(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteCustomerRemovesIt() {
		long id = createCustomer(uniqueName("deletable"), uniqueEmail("deletable"));

		graphQlTester.document("mutation($id: ID!) { deleteCustomer(id: $id) }")
				.variable("id", id)
				.execute()
				.path("deleteCustomer")
				.entity(Boolean.class)
				.isEqualTo(true);

		graphQlTester.document("query($id: ID!) { customer(id: $id) { firstName } }")
				.variable("id", id)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

	@Test
	void deleteCustomerReturnsNotFoundForUnknownId() {
		graphQlTester.document("mutation($id: ID!) { deleteCustomer(id: $id) }")
				.variable("id", 999_999_999L)
				.execute()
				.errors()
				.expect(error -> "NOT_FOUND".equals(error.getExtensions().get("classification")))
				.verify();
	}

}
