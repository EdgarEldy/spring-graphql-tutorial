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
 * over a different connection.
 * <p>
 * multicast().directBestEffort() is used rather than
 * multicast().onBackpressureBuffer(): onBackpressureBuffer's "warm up"
 * behaviour remembers every element pushed before the very first subscriber
 * ever registers and replays all of it to that first subscriber. On this
 * project's own test suite that meant whichever order-creating test ran
 * before the WebSocket subscription test made its first connection got
 * silently replayed to it instead of the order actually created during that
 * test, the exact bug a passing local run hid and a differently ordered CI
 * run caught. The same "warm up" replay would happen just as much in
 * production: any order created between server startup and the first ever
 * client connecting to orderCreated would be dumped in bulk onto that first
 * client. directBestEffort never buffers or replays: a subscriber only ever
 * receives orders created after it subscribed, and a slow or absent
 * subscriber simply misses events rather than piling up a backlog, which is
 * the behaviour an "order was just created" live feed is actually meant to
 * have.
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
		return Sinks.many().multicast().directBestEffort();
	}

}
