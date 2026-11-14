package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.entity.RefreshToken;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.repository.RefreshTokenRepository;
import com.edgareldy.springgraphqltutorial.security.JwtService;
import com.edgareldy.springgraphqltutorial.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages the lifecycle of DB-backed refresh tokens: creation on login,
 * lookup on token refresh, and deletion on logout. Storing refresh tokens
 * in the database allows the server to revoke them immediately on logout,
 * unlike access tokens (which remain valid until their short TTL expires
 * and are only blacklisted by jti).
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RefreshToken create(User user, String rawToken) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(jwtService.extractExpiration(rawToken))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
