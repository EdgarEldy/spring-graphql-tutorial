package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderInput;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderPage;
import com.edgareldy.springgraphqltutorial.repository.CustomerRepository;
import com.edgareldy.springgraphqltutorial.repository.OrderRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/**
 * Unit tests for OrderServiceImpl: the total = quantity * unitPrice
 * computation, the NOT_FOUND rejections for an unknown customerId/productId,
 * and the emission of every newly created Order into the shared
 * Sinks.Many&lt;Order&gt;, with every repository and the sink itself mocked
 * or replaced by a real (but isolated) sink so no Spring context is needed.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private ProductRepository productRepository;

	private Sinks.Many<Order> orderSink;

	private OrderServiceImpl orderService;

	@BeforeEach
	void setUp() {
		// A real sink rather than a mock: verifying what a mutation on
		// Sinks.Many actually publishes is more meaningful, and more resistant
		// to a refactor, than asserting a mock recorded a tryEmitNext call.
		// Must match OrderEventsConfig's directBestEffort() exactly: an
		// onBackpressureBuffer() sink here would replay a pre-subscription
		// emission to whichever subscriber attaches first, the exact
		// production bug this test class exists to guard against.
		orderSink = Sinks.many().multicast().directBestEffort();
		orderService = new OrderServiceImpl(orderRepository, customerRepository, productRepository, orderSink);
	}

	private Customer customer() {
		return Customer.builder().id(1L).firstName("Ada").lastName("Lovelace").telephone("555-0100")
				.email("ada@example.com").address("1 Analytical Engine Way").build();
	}

	private Product product(String unitPrice) {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		return Product.builder().id(1L).category(category).productName("Clean Code")
				.unitPrice(new BigDecimal(unitPrice)).build();
	}

	private Order order(Customer customer, Product product) {
		return Order.builder().id(1L).customer(customer).product(product).quantity(2)
				.total(new BigDecimal("79.98")).build();
	}

	@Test
	void findAllWithoutCustomerFilterDelegatesToFindAll() {
		Order order = order(customer(), product("39.99"));
		when(orderRepository.findAll(PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

		OrderPage page = orderService.findAll(null, 0, 20);

		assertThat(page.content()).containsExactly(order);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.page()).isEqualTo(0);
		assertThat(page.size()).isEqualTo(20);
		verify(orderRepository, never()).findByCustomerId(any(), any());
	}

	@Test
	void findAllWithCustomerFilterDelegatesToFindByCustomerId() {
		Order order = order(customer(), product("39.99"));
		when(orderRepository.findByCustomerId(1L, PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

		OrderPage page = orderService.findAll(1L, 0, 20);

		assertThat(page.content()).containsExactly(order);
		verify(orderRepository, never()).findAll(any(PageRequest.class));
	}

	@Test
	void findByIdReturnsOrder() {
		Order order = order(customer(), product("39.99"));
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

		assertThat(orderService.findById(1L)).isEqualTo(order);
	}

	@Test
	void findByIdThrowsWhenOrderMissing() {
		when(orderRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createComputesTotalFromQuantityAndUnitPriceThenSaves() {
		Customer customer = customer();
		Product product = product("19.99");
		OrderInput input = new OrderInput(1L, 1L, 3);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Order created = orderService.create(input);

		assertThat(created.getCustomer()).isEqualTo(customer);
		assertThat(created.getProduct()).isEqualTo(product);
		assertThat(created.getQuantity()).isEqualTo(3);
		assertThat(created.getTotal()).isEqualByComparingTo("59.97");
	}

	@Test
	void createThrowsWhenCustomerMissing() {
		OrderInput input = new OrderInput(99L, 1L, 1);
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.create(input)).isInstanceOf(ResourceNotFoundException.class);

		verify(productRepository, never()).findById(any());
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void createThrowsWhenProductMissing() {
		OrderInput input = new OrderInput(1L, 99L, 1);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.create(input)).isInstanceOf(ResourceNotFoundException.class);

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void createEmitsTheSavedOrderIntoTheSink() {
		Customer customer = customer();
		Product product = product("19.99");
		OrderInput input = new OrderInput(1L, 1L, 2);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		StepVerifier.create(orderSink.asFlux().take(1))
				.then(() -> orderService.create(input))
				.assertNext(emitted -> {
					assertThat(emitted.getQuantity()).isEqualTo(2);
					assertThat(emitted.getTotal()).isEqualByComparingTo("39.98");
				})
				.verifyComplete();
	}

	@Test
	void createDoesNotFailWhenNoSubscriberIsListening() {
		Customer customer = customer();
		Product product = product("19.99");
		OrderInput input = new OrderInput(1L, 1L, 1);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// directBestEffort() never buffers: with nobody subscribed yet,
		// tryEmitNext resolves to FAIL_ZERO_SUBSCRIBER and the order is simply
		// not broadcast, but create() itself must still succeed since the
		// order was already persisted before this emission was attempted.
		Order created = orderService.create(input);

		assertThat(created.getQuantity()).isEqualTo(1);

		// A subscriber attaching only after create() ran must not receive
		// that earlier order: directBestEffort has no replay/warm-up buffer,
		// unlike onBackpressureBuffer, which is exactly the bug this project
		// hit when the two were confused (see OrderEventsConfig's Javadoc).
		StepVerifier.create(orderSink.asFlux().take(1).timeout(Duration.ofMillis(200)))
				.verifyError();
	}

}
