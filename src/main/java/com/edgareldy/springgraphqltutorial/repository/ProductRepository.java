package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Product.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	// Derived query navigating the category association (Product.category.id),
	// used by products(categoryId, ...) to filter the paginated list.
	Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

}
