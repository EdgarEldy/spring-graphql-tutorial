package com.edgareldy.springgraphqltutorial.graphql.exception;

import java.util.List;
import java.util.Map;

import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Central mapping from a Throwable raised anywhere in a data fetcher to a
 * structured GraphQL error. Every exception ends up with a consistent
 * extensions.classification value (NOT_FOUND, BAD_REQUEST, FORBIDDEN,
 * INTERNAL_ERROR) so clients can branch on it the same way they would branch
 * on an HTTP status code in a REST API. An unmapped exception is logged
 * server side and never surfaces its raw message to the client. Bean
 * validation is not wired into any input type yet, so BAD_REQUEST is only
 * reachable through BusinessRuleException for now; a validation exception
 * type can be added to classify() once one is actually thrown.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Slf4j
@Component
public class GraphQlExceptionResolver implements DataFetcherExceptionResolver {

	private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "An unexpected error occurred.";

	@Override
	public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment env) {
		ErrorType classification = classify(exception);
		String message = classification == ErrorType.INTERNAL_ERROR ? GENERIC_INTERNAL_ERROR_MESSAGE
				: exception.getMessage();

		if (classification == ErrorType.INTERNAL_ERROR) {
			log.error("Unhandled exception in a GraphQL data fetcher", exception);
		}

		return Mono.just(List.of(GraphqlErrorBuilder.newError(env)
				.message(message)
				.errorType(classification)
				.extensions(Map.of("classification", classification.toString()))
				.build()));
	}

	private ErrorType classify(Throwable exception) {
		if (exception instanceof ResourceNotFoundException) {
			return ErrorType.NOT_FOUND;
		}
		if (exception instanceof BusinessRuleException) {
			return ErrorType.BAD_REQUEST;
		}
		if (exception instanceof AccessDeniedException) {
			return ErrorType.FORBIDDEN;
		}
		return ErrorType.INTERNAL_ERROR;
	}

}
