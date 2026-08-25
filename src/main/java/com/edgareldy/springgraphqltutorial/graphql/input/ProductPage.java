package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Product;

/**
 * Java shape of the GraphQL ProductPage type, built manually from a Spring
 * Data Page&lt;Product&gt;, same reasoning as CategoryPage.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public record ProductPage(List<Product> content, long totalElements, int totalPages, int page, int size) {
}
