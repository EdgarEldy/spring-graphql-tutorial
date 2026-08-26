package com.edgareldy.springgraphqltutorial.graphql.resolver;

import java.util.concurrent.CompletableFuture;

import com.edgareldy.springgraphqltutorial.entity.Category;
import com.edgareldy.springgraphqltutorial.entity.Product;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Resolves Product.category through the DataLoader registered in
 * DataLoaderConfig for the Long -> Category pair, never through
 * CategoryRepository directly, so listing N products with their category
 * issues one batched query, not N.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@Controller
public class ProductCategoryResolver {

	@SchemaMapping(typeName = "Product", field = "category")
	public CompletableFuture<Category> category(Product product, DataLoader<Long, Category> categoryLoader) {
		// Category is a real @ManyToOne(LAZY) on Product, so this reads the id
		// off the Hibernate proxy without triggering a load: only loader.load
		// below actually fetches data, batched across every Product resolved
		// in this request.
		return categoryLoader.load(product.getCategory().getId());
	}

}
