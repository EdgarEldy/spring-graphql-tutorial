package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderInput;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderPage;
import com.edgareldy.springgraphqltutorial.service.OrderService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Order queries, mutation and the orderCreated subscription. createOrder
 * delegates entirely to OrderService, including the total computation and
 * the emission into the shared sink: this controller never touches the sink
 * for writing, only for reading it as a Flux to serve the subscription.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class OrderController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final OrderService orderService;
	private final Sinks.Many<Order> orderSink;

	public OrderController(OrderService orderService, Sinks.Many<Order> orderSink) {
		this.orderService = orderService;
		this.orderSink = orderSink;
	}

	@QueryMapping
	public OrderPage orders(@Argument Long customerId, @Argument Integer page, @Argument Integer size) {
		int resolvedPage = page != null ? page : DEFAULT_PAGE;
		int resolvedSize = size != null ? size : DEFAULT_SIZE;
		return orderService.findAll(customerId, resolvedPage, resolvedSize);
	}

	@QueryMapping
	public Order order(@Argument Long id) {
		return orderService.findById(id);
	}

	@MutationMapping
	public Order createOrder(@Argument OrderInput input) {
		return orderService.create(input);
	}

	@SubscriptionMapping
	public Flux<Order> orderCreated() {
		return orderSink.asFlux();
	}

}
