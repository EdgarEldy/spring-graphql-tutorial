package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CategoryPage;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for CategoryServiceImpl: every public method's nominal path,
 * duplicate category name rejected on create/update, and the business rule
 * that a category still referenced by at least one product cannot be
 * deleted, with CategoryRepository mocked and no Spring context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

	@Mock
	private CategoryRepository categoryRepository;

	private CategoryServiceImpl categoryService;

	@BeforeEach
	void setUp() {
		categoryService = new CategoryServiceImpl(categoryRepository);
	}

	@Test
	void _01_ShouldReturnPageBuiltFromRepositoryPage_WhenAllCategoriesAreRequested() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		when(categoryRepository.findAll(PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(category), PageRequest.of(0, 20), 1));

		CategoryPage page = categoryService.findAll(0, 20);

		assertThat(page.content()).containsExactly(category);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.page()).isEqualTo(0);
		assertThat(page.size()).isEqualTo(20);
	}

	@Test
	void _02_ShouldReturnCategory_WhenCategoryExists() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		assertThat(categoryService.findById(1L)).isEqualTo(category);
	}

	@Test
	void _03_ShouldThrowNotFound_WhenCategoryIsMissing() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _04_ShouldSaveCategory_WhenCategoryIsNew() {
		CategoryInput input = new CategoryInput("Books");
		when(categoryRepository.existsByCategoryName("Books")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Category created = categoryService.create(input);

		assertThat(created.getCategoryName()).isEqualTo("Books");
	}

	@Test
	void _05_ShouldRejectCreation_WhenCategoryNameIsDuplicate() {
		CategoryInput input = new CategoryInput("Books");
		when(categoryRepository.existsByCategoryName("Books")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.create(input)).isInstanceOf(BusinessRuleException.class);

		verify(categoryRepository, never()).save(any(Category.class));
	}

	@Test
	void _06_ShouldChangeCategoryName_WhenCategoryIsUpdated() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		CategoryInput input = new CategoryInput("Comics");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByCategoryName("Comics")).thenReturn(false);
		when(categoryRepository.save(category)).thenReturn(category);

		Category updated = categoryService.update(1L, input);

		assertThat(updated.getCategoryName()).isEqualTo("Comics");
	}

	@Test
	void _07_ShouldThrowNotFound_WhenUpdatedCategoryIsMissing() {
		CategoryInput input = new CategoryInput("Comics");
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.update(99L, input)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _08_ShouldRejectUpdate_WhenNameIsUsedByAnotherCategory() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		CategoryInput input = new CategoryInput("Comics");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByCategoryName("Comics")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.update(1L, input)).isInstanceOf(BusinessRuleException.class);

		verify(categoryRepository, never()).save(any(Category.class));
	}

	@Test
	void _09_ShouldAllowUpdate_WhenNameIsKeptUnchanged() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		CategoryInput input = new CategoryInput("Books");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.save(category)).thenReturn(category);

		Category updated = categoryService.update(1L, input);

		assertThat(updated.getCategoryName()).isEqualTo("Books");
		verify(categoryRepository, never()).existsByCategoryName(any());
	}

	@Test
	void _10_ShouldRemoveCategory_WhenCategoryHasNoProducts() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.countProductsByCategoryId(1L)).thenReturn(0L);

		categoryService.delete(1L);

		verify(categoryRepository).delete(category);
	}

	@Test
	void _11_ShouldThrowNotFound_WhenDeletedCategoryIsMissing() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(categoryRepository, never()).delete(any(Category.class));
	}

	@Test
	void _12_ShouldRejectDeletion_WhenCategoryStillHasProducts() {
		Category category = Category.builder().id(1L).categoryName("Books").build();
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.countProductsByCategoryId(1L)).thenReturn(3L);

		assertThatThrownBy(() -> categoryService.delete(1L)).isInstanceOf(BusinessRuleException.class);

		verify(categoryRepository, never()).delete(any(Category.class));
	}

}
