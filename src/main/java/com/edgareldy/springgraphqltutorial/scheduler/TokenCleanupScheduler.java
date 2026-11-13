package com.edgareldy.springgraphqltutorial.scheduler;

import java.time.LocalDateTime;

import com.edgareldy.springgraphqltutorial.repository.ActivationTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a nightly cleanup at 02:00 to delete expired activation tokens,
 * password reset tokens, and blacklisted tokens whose JWT has already
 * expired so the token tables stay bounded over time.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Component
public class TokenCleanupScheduler {

    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenCleanupScheduler(ActivationTokenRepository activationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            BlacklistedTokenRepository blacklistedTokenRepository) {
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        activationTokenRepository.deleteByExpiresAtBefore(now);
        passwordResetTokenRepository.deleteByExpiryDateBefore(now);
        blacklistedTokenRepository.deleteByExpiresAtBefore(now);
    }

}
