package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Customer;

/**
 * Java shape of the GraphQL CustomerPage type, built manually from a Spring
 * Data Page&lt;Customer&gt;, same reasoning as CategoryPage/ProductPage.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public record CustomerPage(List<Customer> content, long totalElements, int totalPages, int page, int size) {
}
