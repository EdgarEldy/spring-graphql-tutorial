package com.edgareldy.springgraphqltutorial.graphql.input;

/**
 * Input for the updateProfile mutation, allowing an authenticated user to
 * update their own first name, last name, and email address.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
public record UpdateProfileInput(String firstName, String lastName, String email) {
}
