package com.edgareldy.springgraphqltutorial.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A JWT that was invalidated before its natural expiration, most commonly by
 * logout. jti is the token's JWT ID claim, checked on every request so a
 * blacklisted token is rejected even while its signature is still valid.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 500)
	private String token;

	@Column(nullable = false, unique = true)
	private String jti;

	@Column(name = "blacklisted_at", nullable = false)
	private LocalDateTime blacklistedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "validated_at")
	private LocalDateTime validatedAt;

}
