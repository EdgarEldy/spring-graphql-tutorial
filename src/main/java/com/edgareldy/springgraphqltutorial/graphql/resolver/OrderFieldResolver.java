package com.edgareldy.springgraphqltutorial.graphql.resolver;

import java.util.concurrent.CompletableFuture;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.entity.Order;
import com.edgareldy.springgraphqltutorial.entity.Product;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Resolves Order.customer and Order.product through the DataLoaders
 * registered in DataLoaderConfig for the Long -> Customer and Long ->
 * Product pairs, never through CustomerRepository/ProductRepository
 * directly, so listing N orders with both relations issues one batched
 * query per relation, not two per order.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class OrderFieldResolver {

	@SchemaMapping(typeName = "Order", field = "customer")
	public CompletableFuture<Customer> customer(Order order, DataLoader<Long, Customer> customerLoader) {
		// Customer is a real @ManyToOne(LAZY) on Order, so this reads the id off
		// the Hibernate proxy without triggering a load: only loader.load below
		// actually fetches data, batched across every Order resolved in this
		// request.
		return customerLoader.load(order.getCustomer().getId());
	}

	@SchemaMapping(typeName = "Order", field = "product")
	public CompletableFuture<Product> product(Order order, DataLoader<Long, Product> productLoader) {
		return productLoader.load(order.getProduct().getId());
	}

}
