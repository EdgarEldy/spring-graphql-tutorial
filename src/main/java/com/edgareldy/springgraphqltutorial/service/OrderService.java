package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderInput;
import com.edgareldy.springgraphqltutorial.graphql.input.OrderPage;

/**
 * Order administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public interface OrderService {

	/**
	 * @param customerId optional filter, null returns orders from every customer
	 */
	OrderPage findAll(Long customerId, int page, int size);

	/**
	 * @throws ResourceNotFoundException if no order has this id
	 */
	Order findById(Long id);

	/**
	 * Computes total = quantity * product.unitPrice, persists the order, and
	 * emits it into the Sinks.Many&lt;Order&gt; that feeds the orderCreated
	 * subscription.
	 * @throws ResourceNotFoundException if no customer has the given customerId, or no product has the given productId
	 */
	Order create(OrderInput input);

}
