package com.edgareldy.springgraphqltutorial.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.BlacklistedToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * Acquires a PESSIMISTIC_WRITE lock on the matching row before returning it.
	 * Used in the logout flow to prevent two concurrent requests from both passing
	 * the existence check and then racing to insert the same jti a second time.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT b FROM BlacklistedToken b WHERE b.jti = :jti")
	Optional<BlacklistedToken> findByJtiForUpdate(@Param("jti") String jti);

	void deleteByExpiresAtBefore(LocalDateTime cutoff);

}
