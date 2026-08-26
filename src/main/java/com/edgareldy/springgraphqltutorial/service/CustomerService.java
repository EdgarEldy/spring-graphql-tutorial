package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerPage;

/**
 * Customer administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public interface CustomerService {

	CustomerPage findAll(int page, int size);

	/**
	 * @throws ResourceNotFoundException if no customer has this id
	 */
	Customer findById(Long id);

	/**
	 * @throws BusinessRuleException if the email is already in use
	 */
	Customer create(CustomerInput input);

	/**
	 * @throws ResourceNotFoundException if no customer has this id
	 * @throws BusinessRuleException if the email is already in use
	 */
	Customer update(Long id, CustomerInput input);

	/**
	 * @throws ResourceNotFoundException if no customer has this id
	 */
	void delete(Long id);

}
