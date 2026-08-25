package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for Category.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByCategoryName(String categoryName);

	// Native query against the products table rather than a JPA relationship:
	// no Product entity exists yet on this branch (feature/products adds it),
	// but the table itself already does, created by feature/core-architecture's
	// V1 migration.
	@Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
	long countProductsByCategoryId(@Param("categoryId") Long categoryId);

}
