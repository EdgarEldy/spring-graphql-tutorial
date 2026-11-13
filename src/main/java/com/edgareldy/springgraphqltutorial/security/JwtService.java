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
 * Signs and validates the JWTs used for authentication and token refresh.
 * Access tokens embed roles (ROLE_*) and permissions (resource:action) so
 * JwtAuthFilter can populate the SecurityContext without a database round-trip.
 * Refresh tokens carry only a token_type claim; JwtAuthFilter rejects them so
 * they cannot be used as access credentials.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Service
public class JwtService {

	private static final String ROLES_CLAIM = "roles";
	private static final String PERMISSIONS_CLAIM = "permissions";
	private static final String TOKEN_TYPE_CLAIM = "token_type";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Issues a short-lived access token embedding the user's roles and permissions.
	 * The caller must ensure roles and their permissions are already loaded (no lazy
	 * fetch triggered here).
	 */
	public String generateToken(User user) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + properties.expirationMs());
		List<String> roleNames = user.getRoles().stream().map(Role::getRoleName).toList();
		List<String> permissionNames = user.getRoles().stream()
				.flatMap(r -> r.getPermissions().stream())
				.map(p -> p.getResource() + ":" + p.getAction())
				.distinct()
				.toList();

		return Jwts.builder()
				.subject(user.getEmail())
				.id(UUID.randomUUID().toString())
				.claim(ROLES_CLAIM, roleNames)
				.claim(PERMISSIONS_CLAIM, permissionNames)
				.issuedAt(now)
				.expiration(expiration)
				.signWith(key)
				.compact();
	}

	/**
	 * Issues a long-lived refresh token. It carries a token_type claim so
	 * JwtAuthFilter can reject it as an access credential.
	 */
	public String generateRefreshToken(User user) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + properties.refreshExpirationMs());
		return Jwts.builder()
				.subject(user.getEmail())
				.id(UUID.randomUUID().toString())
				.claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
				.issuedAt(now)
				.expiration(expiration)
				.signWith(key)
				.compact();
	}

	/** Returns true when the token carries the refresh token_type claim. */
	public boolean isRefreshToken(String token) {
		return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM));
	}

	/** Exposes the access token lifetime in milliseconds for the AuthPayload expiresIn field. */
	public long getExpirationMs() {
		return properties.expirationMs();
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

	/**
	 * Extracts both ROLE_* and plain permission authorities from the token claims.
	 * Roles become ROLE_{name} authorities; permissions stay as resource:action.
	 */
	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> extractAuthorities(String token) {
		Claims claims = parseClaims(token);
		Set<GrantedAuthority> authorities = new java.util.HashSet<>();
		List<String> roleNames = (List<String>) claims.get(ROLES_CLAIM, List.class);
		if (roleNames != null) {
			roleNames.stream()
					.map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
					.forEach(authorities::add);
		}
		List<String> permissionNames = (List<String>) claims.get(PERMISSIONS_CLAIM, List.class);
		if (permissionNames != null) {
			permissionNames.stream()
					.map(perm -> (GrantedAuthority) new SimpleGrantedAuthority(perm))
					.forEach(authorities::add);
		}
		return authorities;
	}

}
