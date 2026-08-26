package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryPage;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@Service
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryServiceImpl(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Override
	public CategoryPage findAll(int page, int size) {
		Page<Category> result = categoryRepository.findAll(PageRequest.of(page, size));
		return new CategoryPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
	}

	@Override
	public Category findById(Long id) {
		return getCategoryOrThrow(id);
	}

	@Override
	@Transactional
	public Category create(CategoryInput input) {
		if (categoryRepository.existsByCategoryName(input.categoryName())) {
			throw new BusinessRuleException("Category name already in use: " + input.categoryName());
		}
		return categoryRepository.save(Category.builder().categoryName(input.categoryName()).build());
	}

	@Override
	@Transactional
	public Category update(Long id, CategoryInput input) {
		Category category = getCategoryOrThrow(id);
		if (!category.getCategoryName().equals(input.categoryName())
				&& categoryRepository.existsByCategoryName(input.categoryName())) {
			throw new BusinessRuleException("Category name already in use: " + input.categoryName());
		}
		category.setCategoryName(input.categoryName());
		return categoryRepository.save(category);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Category category = getCategoryOrThrow(id);
		if (categoryRepository.countProductsByCategoryId(id) > 0) {
			throw new BusinessRuleException("Cannot delete category " + id + " because it still has products");
		}
		categoryRepository.delete(category);
	}

	private Category getCategoryOrThrow(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
	}

}
