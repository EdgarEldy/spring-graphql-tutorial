package com.edgareldy.springgraphqltutorial.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates every request carrying a Bearer JWT before it reaches
 * GraphQL execution. Registered as a Servlet Filter (not left to
 * JwtContextInterceptor alone) because @PreAuthorize on controller methods
 * reads Spring Security's ambient SecurityContextHolder, and only a Filter
 * integrates correctly with how Spring Security's SecurityContextRepository
 * saves and restores that context across a Servlet async dispatch, which is
 * how Spring for GraphQL's WebMvc handler executes a request. A
 * WebGraphQlInterceptor runs after that dispatch machinery already resolved
 * its context for the async continuation, too late for a plain
 * SecurityContextHolder write there to still be visible when a data fetcher
 * actually runs, as verified empirically while wiring this up. This filter
 * also covers the WebSocket subscription handshake, since that is still a
 * normal HTTP request passing through the Servlet filter chain before the
 * connection is upgraded.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final BlacklistedTokenRepository blacklistedTokenRepository;

	public JwtAuthFilter(JwtService jwtService, BlacklistedTokenRepository blacklistedTokenRepository) {
		this.jwtService = jwtService;
		this.blacklistedTokenRepository = blacklistedTokenRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			resolveAuthentication(header.substring(7))
					.ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
		}
		filterChain.doFilter(request, response);
	}

	private Optional<Authentication> resolveAuthentication(String token) {
		try {
			String jti = jwtService.extractJti(token);
			if (blacklistedTokenRepository.existsByJti(jti)) {
				return Optional.empty();
			}
			String email = jwtService.extractEmail(token);
			Collection<GrantedAuthority> authorities = jwtService.extractAuthorities(token);
			return Optional.of(new UsernamePasswordAuthenticationToken(email, null, authorities));
		} catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

}
