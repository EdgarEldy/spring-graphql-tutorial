package com.edgareldy.springgraphqltutorial.service.impl;

import java.math.BigDecimal;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderInput;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderPage;
import com.edgareldy.springgraphqltutorial.repository.CustomerRepository;
import com.edgareldy.springgraphqltutorial.repository.OrderRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import com.edgareldy.springgraphqltutorial.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Sinks;

/**
 * Order administration. create resolves both the target Customer and Product
 * before persisting, since total depends on the product's unitPrice at
 * order time, then pushes the freshly saved Order into the shared
 * Sinks.Many so every orderCreated subscriber is notified in the same
 * request that persisted it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Service
public class OrderServiceImpl implements OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	private final OrderRepository orderRepository;
	private final CustomerRepository customerRepository;
	private final ProductRepository productRepository;
	private final Sinks.Many<Order> orderSink;

	public OrderServiceImpl(OrderRepository orderRepository, CustomerRepository customerRepository,
			ProductRepository productRepository, Sinks.Many<Order> orderSink) {
		this.orderRepository = orderRepository;
		this.customerRepository = customerRepository;
		this.productRepository = productRepository;
		this.orderSink = orderSink;
	}

	@Override
	public OrderPage findAll(Long customerId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		Page<Order> result = customerId != null
				? orderRepository.findByCustomerId(customerId, pageRequest)
				: orderRepository.findAll(pageRequest);
		return new OrderPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
	}

	@Override
	public Order findById(Long id) {
		return getOrderOrThrow(id);
	}

	@Override
	@Transactional
	public Order create(OrderInput input) {
		Customer customer = getCustomerOrThrow(input.customerId());
		Product product = getProductOrThrow(input.productId());
		BigDecimal total = product.getUnitPrice().multiply(BigDecimal.valueOf(input.quantity()));
		Order order = Order.builder()
				.customer(customer)
				.product(product)
				.quantity(input.quantity())
				.total(total)
				.build();
		Order saved = orderRepository.save(order);
		emit(saved);
		return saved;
	}

	private void emit(Order order) {
		// tryEmitNext never blocks and its result never fails the mutation
		// itself: createOrder already succeeded and was persisted, only
		// orderCreated subscribers are affected by what happens here.
		// FAIL_ZERO_SUBSCRIBER is the ordinary state of a directBestEffort sink
		// whenever no client currently has an open orderCreated subscription,
		// not an anomaly worth a warning. Any other failure means a
		// subscriber that was actually listening missed this event.
		Sinks.EmitResult result = orderSink.tryEmitNext(order);
		if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
			log.debug("No orderCreated subscriber connected, order {} was not broadcast", order.getId());
		} else if (result.isFailure()) {
			log.warn("Failed to emit order {} to orderCreated subscribers: {}", order.getId(), result);
		}
	}

	private Order getOrderOrThrow(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
	}

	private Customer getCustomerOrThrow(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
	}

	private Product getProductOrThrow(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

}
