package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductInput;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductPage;

/**
 * Product administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public interface ProductService {

	/**
	 * @param categoryId optional filter, null returns products from every category
	 */
	ProductPage findAll(Long categoryId, int page, int size);

	/**
	 * @throws ResourceNotFoundException if no product has this id
	 */
	Product findById(Long id);

	/**
	 * @throws ResourceNotFoundException if no category has the given categoryId
	 */
	Product create(ProductInput input);

	/**
	 * @throws ResourceNotFoundException if no product has this id, or no category has the given categoryId
	 */
	Product update(Long id, ProductInput input);

	/**
	 * @throws ResourceNotFoundException if no product has this id
	 */
	void delete(Long id);

}
