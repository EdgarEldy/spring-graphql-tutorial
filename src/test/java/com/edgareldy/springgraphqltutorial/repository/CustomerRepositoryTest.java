package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for CustomerRepository against a real PostgreSQL 16
 * container: existsByEmail and the unique email constraint from
 * V1__init_schema.sql.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CustomerRepositoryTest {

	@Autowired
	private CustomerRepository customerRepository;

	private Customer persistCustomer(String email) {
		return customerRepository.save(Customer.builder()
				.firstName("Jane")
				.lastName("Doe")
				.telephone("555-0100")
				.email(email)
				.address("1 Main Street")
				.build());
	}

	@Test
	void existsByEmailReflectsPersistedState() {
		persistCustomer("jane.doe@example.com");

		assertThat(customerRepository.existsByEmail("jane.doe@example.com")).isTrue();
		assertThat(customerRepository.existsByEmail("missing@example.com")).isFalse();
	}

	@Test
	void emailColumnRejectsDuplicates() {
		persistCustomer("jane.doe@example.com");

		assertThatThrownBy(() -> {
			persistCustomer("jane.doe@example.com");
			customerRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
