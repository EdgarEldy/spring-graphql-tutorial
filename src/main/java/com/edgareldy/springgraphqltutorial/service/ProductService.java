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
	 * Returns a page of products. When productName, minPrice or maxPrice is not
	 * null the Specification path is taken (dynamic filtering). Otherwise
	 * categoryId-only uses a derived query and no-filter uses plain findAll.
	 *
	 * @param categoryId  optional category filter
	 * @param productName optional case-insensitive substring match
	 * @param minPrice    optional inclusive lower bound on unit price
	 * @param maxPrice    optional inclusive upper bound on unit price
	 */
	ProductPage findAll(Long categoryId, String productName, Double minPrice, Double maxPrice, int page, int size);

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
