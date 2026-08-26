package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Integration tests for OrderRepository against a real PostgreSQL 16
 * container: findByCustomerId, the derived query orders(customerId, ...)
 * filters on, and the column constraints from V1__init_schema.sql that
 * Hibernate/JPA does not enforce on its own: the NOT NULL foreign keys to
 * customers/products and the CHECK (quantity > 0)/CHECK (total > 0)
 * constraints.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EntityManager entityManager;

	private Customer persistCustomer(String email) {
		return customerRepository.save(Customer.builder().firstName("Ada").lastName("Lovelace")
				.telephone("555-0100").email(email).address("1 Analytical Engine Way").build());
	}

	private Product persistProduct(String productName, String unitPrice) {
		Category category = categoryRepository.save(Category.builder().categoryName("Books-" + productName).build());
		return productRepository.save(Product.builder().category(category).productName(productName)
				.unitPrice(new BigDecimal(unitPrice)).build());
	}

	private Order persistOrder(Customer customer, Product product, int quantity, String total) {
		return orderRepository.save(Order.builder().customer(customer).product(product).quantity(quantity)
				.total(new BigDecimal(total)).build());
	}

	@Test
	void findByCustomerIdReturnsOnlyOrdersOfThatCustomer() {
		Customer ada = persistCustomer("ada@example.com");
		Customer grace = persistCustomer("grace@example.com");
		Product product = persistProduct("Clean Code", "39.99");
		persistOrder(ada, product, 1, "39.99");
		persistOrder(ada, product, 2, "79.98");
		persistOrder(grace, product, 1, "39.99");

		Page<Order> adaOrders = orderRepository.findByCustomerId(ada.getId(), PageRequest.of(0, 20));

		assertThat(adaOrders.getTotalElements()).isEqualTo(2);
		assertThat(adaOrders.getContent()).extracting(Order::getQuantity).containsExactlyInAnyOrder(1, 2);
	}

	@Test
	void findByCustomerIdReturnsAnEmptyPageWhenCustomerHasNoOrder() {
		Customer customer = persistCustomer("empty@example.com");

		Page<Order> page = orderRepository.findByCustomerId(customer.getId(), PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isZero();
		assertThat(page.getContent()).isEmpty();
	}

	@Test
	void findAllReturnsOrdersAcrossEveryCustomer() {
		Customer ada = persistCustomer("ada2@example.com");
		Customer grace = persistCustomer("grace2@example.com");
		Product product = persistProduct("Effective Java", "44.99");
		persistOrder(ada, product, 1, "44.99");
		persistOrder(grace, product, 1, "44.99");

		Page<Order> page = orderRepository.findAll(PageRequest.of(0, 20));

		assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	void customerIdColumnRejectsAReferenceToAnUnknownCustomer() {
		Product product = persistProduct("Ghost Book", "9.99");
		Customer phantom = Customer.builder().id(999_999_999L).firstName("Phantom").lastName("Customer")
				.telephone("000").email("phantom@example.com").address("Nowhere").build();

		assertThatThrownBy(() -> {
			orderRepository.save(Order.builder().customer(phantom).product(product).quantity(1)
					.total(new BigDecimal("9.99")).build());
			orderRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void quantityColumnRejectsAZeroOrNegativeValue() {
		Customer customer = persistCustomer("negative-quantity@example.com");
		Product product = persistProduct("Negative Quantity Book", "9.99");

		assertThatThrownBy(() -> {
			orderRepository.save(Order.builder().customer(customer).product(product).quantity(0)
					.total(new BigDecimal("9.99")).build());
			orderRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void totalColumnRejectsAZeroOrNegativeValue() {
		Customer customer = persistCustomer("negative-total@example.com");
		Product product = persistProduct("Negative Total Book", "9.99");

		assertThatThrownBy(() -> {
			orderRepository.save(Order.builder().customer(customer).product(product).quantity(1)
					.total(BigDecimal.ZERO).build());
			orderRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void orderCustomerAndProductAssociationsAreLazyByDefault() {
		Customer customer = persistCustomer("lazy@example.com");
		Product product = persistProduct("Lazy Book", "9.99");
		Order saved = persistOrder(customer, product, 1, "9.99");
		entityManager.flush();
		entityManager.clear();

		Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();

		assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
				.isLoaded(reloaded, "customer")).isFalse();
		assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
				.isLoaded(reloaded, "product")).isFalse();
	}

}
