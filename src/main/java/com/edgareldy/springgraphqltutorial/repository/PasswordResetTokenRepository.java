package com.edgareldy.springgraphqltutorial.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for PasswordResetToken.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByToken(String token);

	void deleteByExpiryDateBefore(LocalDateTime cutoff);

}
