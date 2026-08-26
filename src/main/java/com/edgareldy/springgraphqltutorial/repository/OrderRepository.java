package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Order.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

	// Derived query navigating the customer association (Order.customer.id),
	// used by orders(customerId, ...) to filter the paginated list.
	Page<Order> findByCustomerId(Long customerId, Pageable pageable);

}
