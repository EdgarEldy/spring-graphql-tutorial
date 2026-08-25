package com.edgareldy.springgraphqltutorial.graphql.input;

/**
 * Java shape of the GraphQL CreateUserInput input type.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public record CreateUserInput(String firstName, String lastName, String email, String password) {
}
