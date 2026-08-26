package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for CategoryRepository against a real PostgreSQL 16
 * container: existsByCategoryName, the unique category_name constraint
 * from V1__init_schema.sql, and countProductsByCategoryId, the native
 * query the category deletion business rule relies on. No Product entity
 * exists yet on this branch, so rows are inserted directly into the
 * products table with a native query, exactly like CategoryRepository
 * itself reads from it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTest {

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EntityManager entityManager;

	private Category persistCategory(String categoryName) {
		return categoryRepository.save(Category.builder().categoryName(categoryName).build());
	}

	private void insertProduct(Long categoryId, String productName) {
		entityManager.createNativeQuery(
				"INSERT INTO products (category_id, product_name, unit_price) VALUES (:categoryId, :productName, 9.99)")
				.setParameter("categoryId", categoryId)
				.setParameter("productName", productName)
				.executeUpdate();
	}

	@Test
	void existsByCategoryNameReflectsPersistedState() {
		persistCategory("Books");

		assertThat(categoryRepository.existsByCategoryName("Books")).isTrue();
		assertThat(categoryRepository.existsByCategoryName("Missing")).isFalse();
	}

	@Test
	void categoryNameColumnRejectsDuplicates() {
		persistCategory("Books");

		assertThatThrownBy(() -> {
			persistCategory("Books");
			categoryRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void countProductsByCategoryIdReturnsZeroWhenNoProductReferencesTheCategory() {
		Category category = persistCategory("Empty Category");

		assertThat(categoryRepository.countProductsByCategoryId(category.getId())).isZero();
	}

	@Test
	void countProductsByCategoryIdCountsOnlyProductsReferencingThatCategory() {
		Category books = persistCategory("Books");
		Category toys = persistCategory("Toys");
		insertProduct(books.getId(), "Clean Code");
		insertProduct(books.getId(), "Effective Java");
		insertProduct(toys.getId(), "Building Blocks");
		entityManager.flush();

		assertThat(categoryRepository.countProductsByCategoryId(books.getId())).isEqualTo(2L);
		assertThat(categoryRepository.countProductsByCategoryId(toys.getId())).isEqualTo(1L);
	}

}
