package com.edgareldy.springgraphqltutorial.graphql.input;

import com.edgareldy.springgraphqltutorial.entity.User;

/**
 * Java shape of the GraphQL AuthPayload type. token is null after register
 * (the account is disabled until activated) and always set after login.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public record AuthPayload(String token, User user) {
}
