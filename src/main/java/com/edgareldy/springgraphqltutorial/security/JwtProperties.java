package com.edgareldy.springgraphqltutorial.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the jwt.* properties (secret, expiration-ms) from application.yml.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {
}
