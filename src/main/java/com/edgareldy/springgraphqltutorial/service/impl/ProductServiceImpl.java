package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductInput;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductPage;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import com.edgareldy.springgraphqltutorial.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	@Override
	public ProductPage findAll(Long categoryId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		Page<Product> result = categoryId != null
				? productRepository.findByCategoryId(categoryId, pageRequest)
				: productRepository.findAll(pageRequest);
		return new ProductPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
	}

	@Override
	public Product findById(Long id) {
		return getProductOrThrow(id);
	}

	@Override
	@Transactional
	public Product create(ProductInput input) {
		Category category = getCategoryOrThrow(input.categoryId());
		Product product = Product.builder()
				.category(category)
				.productName(input.productName())
				.unitPrice(input.unitPrice())
				.build();
		return productRepository.save(product);
	}

	@Override
	@Transactional
	public Product update(Long id, ProductInput input) {
		Product product = getProductOrThrow(id);
		Category category = getCategoryOrThrow(input.categoryId());
		product.setCategory(category);
		product.setProductName(input.productName());
		product.setUnitPrice(input.unitPrice());
		return productRepository.save(product);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Product product = getProductOrThrow(id);
		productRepository.delete(product);
	}

	private Product getProductOrThrow(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	private Category getCategoryOrThrow(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
	}

}
