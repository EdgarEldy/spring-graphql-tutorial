package com.edgareldy.springgraphqltutorial.repository;

import java.time.LocalDateTime;

import com.edgareldy.springgraphqltutorial.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for BlacklistedToken.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

	boolean existsByJti(String jti);

	void deleteByExpiresAtBefore(LocalDateTime cutoff);

}
