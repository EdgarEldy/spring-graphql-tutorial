package com.edgareldy.springgraphqltutorial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for JwtAuthFilter: whether a request carrying a Bearer token
 * ends up with an Authentication in SecurityContextHolder, covering a
 * missing header, a valid token, a blacklisted jti and a token JwtService
 * fails to parse. JwtService and BlacklistedTokenRepository are mocked so
 * this test never touches jjwt's real signature verification.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private BlacklistedTokenRepository blacklistedTokenRepository;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	private JwtAuthFilter jwtAuthFilter;

	@BeforeEach
	void setUp() {
		jwtAuthFilter = new JwtAuthFilter(jwtService, blacklistedTokenRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void skipsAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

		jwtAuthFilter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void skipsAuthenticationWhenHeaderIsNotABearerToken() throws Exception {
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

		jwtAuthFilter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void authenticatesRequestCarryingAValidNonBlacklistedToken() throws Exception {
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
		when(jwtService.extractJti("valid-token")).thenReturn("jti-1");
		when(blacklistedTokenRepository.existsByJti("jti-1")).thenReturn(false);
		when(jwtService.extractEmail("valid-token")).thenReturn("ada@example.com");
		Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
		when(jwtService.extractAuthorities("valid-token")).thenReturn(authorities);

		jwtAuthFilter.doFilterInternal(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("ada@example.com");
		assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ADMIN");
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void rejectsBlacklistedToken() throws Exception {
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer blacklisted-token");
		when(jwtService.extractJti("blacklisted-token")).thenReturn("jti-1");
		when(blacklistedTokenRepository.existsByJti("jti-1")).thenReturn(true);

		jwtAuthFilter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void skipsAuthenticationWhenTokenCannotBeParsed() throws Exception {
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer malformed-token");
		when(jwtService.extractJti("malformed-token")).thenThrow(new MalformedJwtException("bad"));

		jwtAuthFilter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

}
