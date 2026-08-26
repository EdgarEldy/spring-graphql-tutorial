package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductInput;
import com.edgareldy.springgraphqltutorial.graphql.input.ProductPage;
import com.edgareldy.springgraphqltutorial.repository.CategoryRepository;
import com.edgareldy.springgraphqltutorial.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for ProductServiceImpl: every public method's nominal path, and
 * the rule that create/update reject a categoryId that does not resolve to
 * an existing Category, with ProductRepository and CategoryRepository
 * mocked and no Spring context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	private ProductServiceImpl productService;

	@BeforeEach
	void setUp() {
		productService = new ProductServiceImpl(productRepository, categoryRepository);
	}

	private Category books() {
		return Category.builder().id(1L).categoryName("Books").build();
	}

	private Product cleanCode(Category category) {
		return Product.builder().id(1L).category(category).productName("Clean Code")
				.unitPrice(new BigDecimal("39.99")).build();
	}

	@Test
	void findAllWithoutCategoryFilterDelegatesToFindAll() {
		Product product = cleanCode(books());
		when(productRepository.findAll(PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

		ProductPage page = productService.findAll(null, 0, 20);

		assertThat(page.content()).containsExactly(product);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.page()).isEqualTo(0);
		assertThat(page.size()).isEqualTo(20);
		verify(productRepository, never()).findByCategoryId(any(), any());
	}

	@Test
	void findAllWithCategoryFilterDelegatesToFindByCategoryId() {
		Product product = cleanCode(books());
		when(productRepository.findByCategoryId(1L, PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

		ProductPage page = productService.findAll(1L, 0, 20);

		assertThat(page.content()).containsExactly(product);
		verify(productRepository, never()).findAll(any(PageRequest.class));
	}

	@Test
	void findByIdReturnsProduct() {
		Product product = cleanCode(books());
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThat(productService.findById(1L)).isEqualTo(product);
	}

	@Test
	void findByIdThrowsWhenProductMissing() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createSavesNewProductAttachedToItsCategory() {
		Category category = books();
		ProductInput input = new ProductInput("Clean Code", new BigDecimal("39.99"), 1L);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Product created = productService.create(input);

		assertThat(created.getProductName()).isEqualTo("Clean Code");
		assertThat(created.getUnitPrice()).isEqualByComparingTo("39.99");
		assertThat(created.getCategory()).isEqualTo(category);
	}

	@Test
	void createThrowsWhenCategoryMissing() {
		ProductInput input = new ProductInput("Clean Code", new BigDecimal("39.99"), 99L);
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.create(input)).isInstanceOf(ResourceNotFoundException.class);

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void updateChangesProductNamePriceAndCategory() {
		Category books = books();
		Category toys = Category.builder().id(2L).categoryName("Toys").build();
		Product product = cleanCode(books);
		ProductInput input = new ProductInput("Building Blocks", new BigDecimal("19.99"), 2L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(toys));
		when(productRepository.save(product)).thenReturn(product);

		Product updated = productService.update(1L, input);

		assertThat(updated.getProductName()).isEqualTo("Building Blocks");
		assertThat(updated.getUnitPrice()).isEqualByComparingTo("19.99");
		assertThat(updated.getCategory()).isEqualTo(toys);
	}

	@Test
	void updateThrowsWhenProductMissing() {
		ProductInput input = new ProductInput("Building Blocks", new BigDecimal("19.99"), 1L);
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(99L, input)).isInstanceOf(ResourceNotFoundException.class);

		verify(categoryRepository, never()).findById(any());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void updateThrowsWhenCategoryMissing() {
		Product product = cleanCode(books());
		ProductInput input = new ProductInput("Clean Code", new BigDecimal("39.99"), 99L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(1L, input)).isInstanceOf(ResourceNotFoundException.class);

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void deleteRemovesProduct() {
		Product product = cleanCode(books());
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.delete(1L);

		verify(productRepository).delete(product);
	}

	@Test
	void deleteThrowsWhenProductMissing() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(productRepository, never()).delete(any(Product.class));
	}

}
