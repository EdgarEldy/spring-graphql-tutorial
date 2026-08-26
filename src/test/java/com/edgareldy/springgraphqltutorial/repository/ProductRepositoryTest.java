package com.edgareldy.springgraphqltutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.edgareldy.springgraphqltutorial.TestcontainersConfiguration;
import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Integration tests for ProductRepository against a real PostgreSQL 16
 * container: findByCategoryId, the derived query products(categoryId, ...)
 * filters on, and the two column constraints from V1__init_schema.sql that
 * Hibernate/JPA does not enforce on its own: the NOT NULL foreign key to
 * categories and the CHECK (unit_price > 0) constraint.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EntityManager entityManager;

	private Category persistCategory(String categoryName) {
		return categoryRepository.save(Category.builder().categoryName(categoryName).build());
	}

	private Product persistProduct(Category category, String productName, String unitPrice) {
		return productRepository.save(Product.builder().category(category).productName(productName)
				.unitPrice(new BigDecimal(unitPrice)).build());
	}

	@Test
	void findByCategoryIdReturnsOnlyProductsOfThatCategory() {
		Category books = persistCategory("Books");
		Category toys = persistCategory("Toys");
		persistProduct(books, "Clean Code", "39.99");
		persistProduct(books, "Effective Java", "44.99");
		persistProduct(toys, "Building Blocks", "19.99");

		Page<Product> booksPage = productRepository.findByCategoryId(books.getId(), PageRequest.of(0, 20));

		assertThat(booksPage.getTotalElements()).isEqualTo(2);
		assertThat(booksPage.getContent()).extracting(Product::getProductName)
				.containsExactlyInAnyOrder("Clean Code", "Effective Java");
	}

	@Test
	void findByCategoryIdReturnsAnEmptyPageWhenCategoryHasNoProduct() {
		Category empty = persistCategory("Empty Category");

		Page<Product> page = productRepository.findByCategoryId(empty.getId(), PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isZero();
		assertThat(page.getContent()).isEmpty();
	}

	@Test
	void findByCategoryIdHonoursPagination() {
		Category books = persistCategory("Books");
		persistProduct(books, "Book One", "9.99");
		persistProduct(books, "Book Two", "9.99");
		persistProduct(books, "Book Three", "9.99");

		Page<Product> firstPage = productRepository.findByCategoryId(books.getId(), PageRequest.of(0, 2));

		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getContent()).hasSize(2);
	}

	@Test
	void findAllReturnsProductsAcrossEveryCategory() {
		Category books = persistCategory("Books");
		Category toys = persistCategory("Toys");
		persistProduct(books, "Clean Code", "39.99");
		persistProduct(toys, "Building Blocks", "19.99");

		Page<Product> page = productRepository.findAll(PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getProductName)
				.containsExactlyInAnyOrder("Clean Code", "Building Blocks");
	}

	@Test
	void categoryIdColumnRejectsAReferenceToAnUnknownCategory() {
		Category phantom = Category.builder().id(999_999_999L).categoryName("Phantom").build();

		assertThatThrownBy(() -> {
			productRepository.save(Product.builder().category(phantom).productName("Ghost")
					.unitPrice(new BigDecimal("9.99")).build());
			productRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unitPriceColumnRejectsAZeroOrNegativeValue() {
		Category books = persistCategory("Books");

		assertThatThrownBy(() -> {
			productRepository.save(Product.builder().category(books).productName("Free Book")
					.unitPrice(BigDecimal.ZERO).build());
			productRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void productCategoryAssociationIsLazyByDefault() {
		Category books = persistCategory("Books");
		Product saved = persistProduct(books, "Clean Code", "39.99");
		entityManager.flush();
		entityManager.clear();

		Product reloaded = productRepository.findById(saved.getId()).orElseThrow();

		assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
				.isLoaded(reloaded, "category")).isFalse();
	}

}
