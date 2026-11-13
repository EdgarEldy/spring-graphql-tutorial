package com.edgareldy.springgraphqltutorial.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for ActivationToken.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {

	Optional<ActivationToken> findByToken(String token);

	void deleteByExpiresAtBefore(LocalDateTime cutoff);

}
