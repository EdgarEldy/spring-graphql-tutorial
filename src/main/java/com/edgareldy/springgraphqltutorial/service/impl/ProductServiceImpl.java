package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductInput;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductPage;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductSpecifications;
import com.edgareldy.springgraphqltutorial.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
	public ProductPage findAll(Long categoryId, String productName, Double minPrice, Double maxPrice, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		boolean hasSpecFilters = productName != null || minPrice != null || maxPrice != null;
		if (!hasSpecFilters) {
			Page<Product> result = categoryId != null
					? productRepository.findByCategoryId(categoryId, pageRequest)
					: productRepository.findAll(pageRequest);
			return toProductPage(result, page, size);
		}
		Specification<Product> spec = Specification
				.where(ProductSpecifications.hasProductNameLike(productName))
				.and(ProductSpecifications.hasMinPrice(minPrice))
				.and(ProductSpecifications.hasMaxPrice(maxPrice));
		if (categoryId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
		}
		return toProductPage(productRepository.findAll(spec, pageRequest), page, size);
	}

	@Override
	@Cacheable(value = "products", key = "#id")
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
	@CacheEvict(value = "products", key = "#id")
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
	@CacheEvict(value = "products", key = "#id")
	public void delete(Long id) {
		Product product = getProductOrThrow(id);
		productRepository.delete(product);
	}

	private ProductPage toProductPage(Page<Product> result, int page, int size) {
		return new ProductPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
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
