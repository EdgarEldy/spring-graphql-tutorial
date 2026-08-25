package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryPage;
import com.edgareldy.springgraphqltutorial.service.CategoryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Category queries and mutations. Reads stay public; writes require
 * ROLE_ADMIN, applied per method rather than at class level since this
 * controller (unlike UserController/RoleController) mixes public and
 * protected operations.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class CategoryController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@QueryMapping
	public CategoryPage categories(@Argument Integer page, @Argument Integer size) {
		int resolvedPage = page != null ? page : DEFAULT_PAGE;
		int resolvedSize = size != null ? size : DEFAULT_SIZE;
		return categoryService.findAll(resolvedPage, resolvedSize);
	}

	@QueryMapping
	public Category category(@Argument Long id) {
		return categoryService.findById(id);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Category createCategory(@Argument CategoryInput input) {
		return categoryService.create(input);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Category updateCategory(@Argument Long id, @Argument CategoryInput input) {
		return categoryService.update(id, input);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public boolean deleteCategory(@Argument Long id) {
		categoryService.delete(id);
		return true;
	}

}
