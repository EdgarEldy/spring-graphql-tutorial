package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryPage;

/**
 * Category administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public interface CategoryService {

	CategoryPage findAll(int page, int size);

	/**
	 * @throws ResourceNotFoundException if no category has this id
	 */
	Category findById(Long id);

	/**
	 * @throws BusinessRuleException if the category name is already in use
	 */
	Category create(CategoryInput input);

	/**
	 * @throws ResourceNotFoundException if no category has this id
	 * @throws BusinessRuleException if the category name is already in use
	 */
	Category update(Long id, CategoryInput input);

	/**
	 * @throws ResourceNotFoundException if no category has this id
	 * @throws BusinessRuleException if the category still has products
	 */
	void delete(Long id);

}
