package com.edgareldy.springgraphqltutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

/**
 * Registers every DataLoader used to batch relationship resolution and avoid
 * the N+1 problem. Empty for now: no @SchemaMapping resolver exists yet.
 * Later branches register their own batch loader here, for example
 * registry.forTypePair(Long.class, Category.class).registerMappedBatchLoader(...)
 * for Product.category in feature/products. No relationship field is ever
 * allowed to call a repository directly instead of going through a loader
 * registered on this class.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class DataLoaderConfig {

	public DataLoaderConfig(BatchLoaderRegistry registry) {
	}

}
