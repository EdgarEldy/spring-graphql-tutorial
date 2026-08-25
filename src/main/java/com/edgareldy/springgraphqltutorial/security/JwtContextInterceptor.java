package com.edgareldy.springgraphqltutorial.security;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reads the Authorization header on every GraphQL request, whether it
 * arrives over HTTP POST or a WebSocket subscription handshake (this
 * interceptor runs for both, unlike a Servlet Filter which never sees the WS
 * frames), resolves the caller, and puts an Authentication in the
 * SecurityContextHolder so @PreAuthorize on controller methods can evaluate
 * it. A blacklisted jti (logged-out token) is treated as unauthenticated
 * even if the signature and expiration are still otherwise valid. The raw
 * token is also placed in the GraphQLContext so AuthController.logout can
 * blacklist the exact token that was used, without parsing headers itself.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Component
public class JwtContextInterceptor implements WebGraphQlInterceptor {

	public static final String JWT_CONTEXT_KEY = "jwt";

	private final JwtService jwtService;
	private final BlacklistedTokenRepository blacklistedTokenRepository;

	public JwtContextInterceptor(JwtService jwtService, BlacklistedTokenRepository blacklistedTokenRepository) {
		this.jwtService = jwtService;
		this.blacklistedTokenRepository = blacklistedTokenRepository;
	}

	@Override
	public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
		String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			resolveAuthentication(token).ifPresent(this::setSecurityContext);
			request.configureExecutionInput(
					(executionInput, builder) -> builder.graphQLContext(Map.of(JWT_CONTEXT_KEY, token)).build());
		}
		return chain.next(request);
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

	private void setSecurityContext(Authentication authentication) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

}
