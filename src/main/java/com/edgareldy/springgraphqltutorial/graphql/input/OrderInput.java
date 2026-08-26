package com.edgareldy.springgraphqltutorial.graphql.input;

/**
 * Java shape of the GraphQL OrderInput input type. total is deliberately
 * absent here: it is never supplied by the client, OrderServiceImpl always
 * computes it from quantity and the resolved product's unitPrice.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public record OrderInput(Long customerId, Long productId, Integer quantity) {
}
