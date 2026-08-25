package com.edgareldy.springgraphqltutorial.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 * Signs and validates the JWTs this project uses for authentication. The
 * roles claim embeds each role name prefixed with ROLE_, matching what
 * Spring Security's hasRole()/@PreAuthorize expressions expect.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Service
public class JwtService {

	private static final String ROLES_CLAIM = "roles";

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + properties.expirationMs());
		List<String> roleNames = user.getRoles().stream().map(Role::getRoleName).toList();

		return Jwts.builder()
				.subject(user.getEmail())
				.id(UUID.randomUUID().toString())
				.claim(ROLES_CLAIM, roleNames)
				.issuedAt(now)
				.expiration(expiration)
				.signWith(key)
				.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	public String extractEmail(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractJti(String token) {
		return parseClaims(token).getId();
	}

	public LocalDateTime extractExpiration(String token) {
		return LocalDateTime.ofInstant(parseClaims(token).getExpiration().toInstant(), ZoneId.systemDefault());
	}

	public LocalDateTime extractIssuedAt(String token) {
		return LocalDateTime.ofInstant(parseClaims(token).getIssuedAt().toInstant(), ZoneId.systemDefault());
	}

	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> extractAuthorities(String token) {
		List<String> roleNames = (List<String>) parseClaims(token).get(ROLES_CLAIM, List.class);
		Set<GrantedAuthority> authorities = roleNames.stream()
				.map(roleName -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleName))
				.collect(Collectors.toSet());
		return authorities;
	}

}
