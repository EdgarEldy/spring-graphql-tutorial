package com.edgareldy.springgraphqltutorial.graphql.input;

import java.math.BigDecimal;

/**
 * Java shape of the GraphQL ProductInput input type.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
public record ProductInput(String productName, BigDecimal unitPrice, Long categoryId) {
}
