package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Category;

/**
 * Java shape of the GraphQL CategoryPage type, built manually from a Spring
 * Data Page&lt;Category&gt;, same reasoning as UserPage.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public record CategoryPage(List<Category> content, long totalElements, int totalPages, int page, int size) {
}
