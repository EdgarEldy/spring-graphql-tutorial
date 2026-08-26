package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerPage;
import com.edgareldy.springgraphqltutorial.repository.CustomerRepository;
import com.edgareldy.springgraphqltutorial.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@Service
public class CustomerServiceImpl implements CustomerService {

	private final CustomerRepository customerRepository;

	public CustomerServiceImpl(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@Override
	public CustomerPage findAll(int page, int size) {
		Page<Customer> result = customerRepository.findAll(PageRequest.of(page, size));
		return new CustomerPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
	}

	@Override
	public Customer findById(Long id) {
		return getCustomerOrThrow(id);
	}

	@Override
	@Transactional
	public Customer create(CustomerInput input) {
		if (customerRepository.existsByEmail(input.email())) {
			throw new BusinessRuleException("Email already in use: " + input.email());
		}
		Customer customer = Customer.builder()
				.firstName(input.firstName())
				.lastName(input.lastName())
				.telephone(input.telephone())
				.email(input.email())
				.address(input.address())
				.build();
		return customerRepository.save(customer);
	}

	@Override
	@Transactional
	public Customer update(Long id, CustomerInput input) {
		Customer customer = getCustomerOrThrow(id);
		if (!customer.getEmail().equals(input.email()) && customerRepository.existsByEmail(input.email())) {
			throw new BusinessRuleException("Email already in use: " + input.email());
		}
		customer.setFirstName(input.firstName());
		customer.setLastName(input.lastName());
		customer.setTelephone(input.telephone());
		customer.setEmail(input.email());
		customer.setAddress(input.address());
		return customerRepository.save(customer);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Customer customer = getCustomerOrThrow(id);
		customerRepository.delete(customer);
	}

	private Customer getCustomerOrThrow(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
	}

}
