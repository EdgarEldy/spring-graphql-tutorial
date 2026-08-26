package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerPage;
import com.edgareldy.springgraphqltutorial.service.CustomerService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Customer queries and mutations. Unlike CategoryController/
 * ProductController, no operation here is restricted to ROLE_ADMIN: the
 * README's feature/customers table does not mark any mutation as admin
 * only, so the full CRUD stays open to any authenticated caller under the
 * schema's default access rules.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class CustomerController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@QueryMapping
	public CustomerPage customers(@Argument Integer page, @Argument Integer size) {
		int resolvedPage = page != null ? page : DEFAULT_PAGE;
		int resolvedSize = size != null ? size : DEFAULT_SIZE;
		return customerService.findAll(resolvedPage, resolvedSize);
	}

	@QueryMapping
	public Customer customer(@Argument Long id) {
		return customerService.findById(id);
	}

	@MutationMapping
	public Customer createCustomer(@Argument CustomerInput input) {
		return customerService.create(input);
	}

	@MutationMapping
	public Customer updateCustomer(@Argument Long id, @Argument CustomerInput input) {
		return customerService.update(id, input);
	}

	@MutationMapping
	public boolean deleteCustomer(@Argument Long id) {
		customerService.delete(id);
		return true;
	}

}
