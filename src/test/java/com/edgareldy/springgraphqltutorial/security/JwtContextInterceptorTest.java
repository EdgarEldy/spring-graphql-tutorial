package com.edgareldy.springgraphqltutorial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

import graphql.ExecutionInput;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * Unit tests for JwtContextInterceptor: whether the raw bearer token ends up
 * in the GraphQLContext under JWT_CONTEXT_KEY once the request is turned
 * into an ExecutionInput, which is exactly what AuthController.logout reads.
 * A real WebGraphQlRequest is used (it is a plain constructible class, not
 * an interface) so configureExecutionInput/toExecutionInput behave exactly
 * as they do at runtime; only the interceptor Chain is mocked.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
class JwtContextInterceptorTest {

	private final JwtContextInterceptor interceptor = new JwtContextInterceptor();

	private WebGraphQlRequest buildRequest(String authorizationHeaderValue) {
		HttpHeaders headers = new HttpHeaders();
		if (authorizationHeaderValue != null) {
			headers.add(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
		}
		Map<String, Object> body = Map.of("query", "query { me { id } }");
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		return new WebGraphQlRequest(URI.create("/graphql"), headers, cookies, null, Map.of(), body, "1",
				Locale.ENGLISH);
	}

	@Test
	void placesRawTokenInGraphQlContextWhenBearerHeaderPresent() {
		WebGraphQlRequest request = buildRequest("Bearer raw-jwt-value");
		WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
		when(chain.next(request)).thenReturn(Mono.just(mock(WebGraphQlResponse.class)));

		interceptor.intercept(request, chain).block();

		ExecutionInput executionInput = request.toExecutionInput();
		assertThat(executionInput.getGraphQLContext().<String>get(JwtContextInterceptor.JWT_CONTEXT_KEY))
				.isEqualTo("raw-jwt-value");
	}

	@Test
	void leavesGraphQlContextEmptyWhenNoAuthorizationHeader() {
		WebGraphQlRequest request = buildRequest(null);
		WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
		when(chain.next(request)).thenReturn(Mono.just(mock(WebGraphQlResponse.class)));

		interceptor.intercept(request, chain).block();

		ExecutionInput executionInput = request.toExecutionInput();
		assertThat(executionInput.getGraphQLContext().<String>get(JwtContextInterceptor.JWT_CONTEXT_KEY)).isNull();
	}

	@Test
	void leavesGraphQlContextEmptyWhenHeaderIsNotABearerToken() {
		WebGraphQlRequest request = buildRequest("Basic dXNlcjpwYXNz");
		WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
		when(chain.next(request)).thenReturn(Mono.just(mock(WebGraphQlResponse.class)));

		interceptor.intercept(request, chain).block();

		ExecutionInput executionInput = request.toExecutionInput();
		assertThat(executionInput.getGraphQLContext().<String>get(JwtContextInterceptor.JWT_CONTEXT_KEY)).isNull();
	}

}
