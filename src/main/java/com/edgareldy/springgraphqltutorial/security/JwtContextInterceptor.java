package com.edgareldy.springgraphqltutorial.security;

import java.util.Map;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reads the Authorization header on every GraphQL request, whether it
 * arrives over HTTP POST or a WebSocket subscription handshake (this
 * interceptor runs for both, unlike a Servlet Filter which never sees a
 * message on an already-upgraded WebSocket connection), and places the raw
 * token in the GraphQLContext so AuthController.logout can blacklist the
 * exact token that was used without parsing headers itself. Authenticating
 * the caller for @PreAuthorize is JwtAuthFilter's job, not this
 * interceptor's: @PreAuthorize reads Spring Security's ambient
 * SecurityContextHolder, and only a Servlet Filter integrates correctly with
 * how that context is saved and restored across the Servlet async dispatch
 * Spring for GraphQL's WebMvc handler uses to execute a request, a Filter
 * being the one thing this interceptor cannot fully replace.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Component
public class JwtContextInterceptor implements WebGraphQlInterceptor {

	public static final String JWT_CONTEXT_KEY = "jwt";

	@Override
	public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
		String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			request.configureExecutionInput(
					(executionInput, builder) -> builder.graphQLContext(Map.of(JWT_CONTEXT_KEY, token)).build());
		}
		return chain.next(request);
	}

}
