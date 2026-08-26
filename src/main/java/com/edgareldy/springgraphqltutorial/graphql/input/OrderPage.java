package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Order;

/**
 * Java shape of the GraphQL OrderPage type, built manually from a Spring
 * Data Page&lt;Order&gt;, same reasoning as ProductPage/CustomerPage.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public record OrderPage(List<Order> content, long totalElements, int totalPages, int page, int size) {
}
