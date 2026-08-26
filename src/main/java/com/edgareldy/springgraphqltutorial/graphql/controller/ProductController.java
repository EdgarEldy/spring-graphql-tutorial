package com.edgareldy.springgraphqltutorial.graphql.controller;

import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductInput;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductPage;
import com.edgareldy.springgraphqltutorial.service.ProductService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Product queries and mutations. Reads stay public; writes require
 * ROLE_ADMIN, applied per method rather than at class level since this
 * controller, like CategoryController, mixes public and protected
 * operations.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class ProductController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@QueryMapping
	public ProductPage products(@Argument Long categoryId, @Argument Integer page, @Argument Integer size) {
		int resolvedPage = page != null ? page : DEFAULT_PAGE;
		int resolvedSize = size != null ? size : DEFAULT_SIZE;
		return productService.findAll(categoryId, resolvedPage, resolvedSize);
	}

	@QueryMapping
	public Product product(@Argument Long id) {
		return productService.findById(id);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Product createProduct(@Argument ProductInput input) {
		return productService.create(input);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Product updateProduct(@Argument Long id, @Argument ProductInput input) {
		return productService.update(id, input);
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public boolean deleteProduct(@Argument Long id) {
		productService.delete(id);
		return true;
	}

}
