package com.edgareldy.springgraphqltutorial.graphql.input;

/**
 * Java shape of the GraphQL CustomerInput input type.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
public record CustomerInput(String firstName, String lastName, String telephone, String email, String address) {
}
