package com.edgareldy.springgraphqltutorial.config;

import com.edgareldy.springgraphqltutorial.entity.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

/**
 * Provides the single Sinks.Many&lt;Order&gt; that powers the orderCreated
 * subscription: OrderServiceImpl emits every newly persisted Order into it,
 * OrderController exposes sink.asFlux() as the subscription's Flux&lt;Order&gt;.
 * Sharing one Spring-managed bean between the two is what lets a mutation on
 * one WebSocket/HTTP request push data to every client currently subscribed
 * over a different connection. multicast().onBackpressureBuffer() is used
 * rather than replay() so a client subscribing after an order was created
 * does not receive stale past orders, only ones created from that point on,
 * and slow subscribers get buffered events instead of dropped ones.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class OrderEventsConfig {

	@Bean
	public Sinks.Many<Order> orderSink() {
		return Sinks.many().multicast().onBackpressureBuffer();
	}

}
