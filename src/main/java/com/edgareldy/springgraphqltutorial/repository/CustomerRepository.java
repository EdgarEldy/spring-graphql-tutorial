package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Customer.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	boolean existsByEmail(String email);

}
