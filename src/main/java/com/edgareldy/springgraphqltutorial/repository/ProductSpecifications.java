package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Product;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA {@link Specification} builders for dynamic Product filtering.
 * Each factory method returns a Specification whose predicate is null
 * when its argument is null, making it a no-op. Callers can compose
 * them freely with {@code Specification.where().and()} without
 * null-checking each argument individually.
 * <p>
 * N+1 note: {@code JpaSpecificationExecutor.findAll(Specification,
 * Pageable)} does not apply any {@code @EntityGraph} declared on the
 * repository, so {@code Product.category} stays lazy on this path.
 * That is intentional: the {@code ProductCategoryResolver} DataLoader
 * batches all category loads anyway, so no extra round-trips arise.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    /** Case-insensitive substring match on {@code product_name}. */
    public static Specification<Product> hasProductNameLike(String productName) {
        return (root, query, cb) -> productName == null ? null
                : cb.like(cb.lower(root.get("productName")), "%" + productName.toLowerCase() + "%");
    }

    /** Inclusive lower bound on {@code unit_price}. */
    public static Specification<Product> hasMinPrice(Double minPrice) {
        return (root, query, cb) -> minPrice == null ? null
                : cb.greaterThanOrEqualTo(root.get("unitPrice"), BigDecimal.valueOf(minPrice));
    }

    /** Inclusive upper bound on {@code unit_price}. */
    public static Specification<Product> hasMaxPrice(Double maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null
                : cb.lessThanOrEqualTo(root.get("unitPrice"), BigDecimal.valueOf(maxPrice));
    }

}
