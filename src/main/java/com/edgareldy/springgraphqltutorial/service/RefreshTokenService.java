package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.RefreshToken;
import com.edgareldy.springgraphqltutorial.entity.User;

/**
 * Contract for managing DB-backed refresh tokens. A refresh token is stored
 * on login and deleted on logout so that server-side revocation is immediate,
 * regardless of the token's remaining JWT lifetime.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
public interface RefreshTokenService {

    /**
     * Persists a new refresh token for the given user with its expiry derived
     * from the JWT claims embedded in {@code rawToken}.
     */
    RefreshToken create(User user, String rawToken);

    /**
     * Returns the stored refresh token matching {@code token}, or throws
     * {@code ResourceNotFoundException} if it does not exist (revoked or expired).
     */
    RefreshToken findByToken(String token);

    /**
     * Deletes all stored refresh tokens belonging to {@code user}. Called on
     * logout so subsequent refresh attempts are rejected immediately.
     */
    void deleteByUser(User user);
}
