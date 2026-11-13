package com.edgareldy.springgraphqltutorial.graphql.exception;

import java.util.List;
import java.util.Map;

import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Central mapping from a Throwable raised anywhere in a data fetcher to a
 * structured GraphQL error. Every exception ends up with a consistent
 * extensions.classification value (NOT_FOUND, BAD_REQUEST, FORBIDDEN,
 * CONFLICT, INTERNAL_ERROR) so clients can branch on it the same way they
 * would branch on an HTTP status code in a REST API. An unmapped exception is
 * logged server side and never surfaces its raw message to the client.
 * OptimisticLockingFailureException surfaces as BAD_REQUEST at the GraphQL
 * transport level (HTTP 200, errors array) with classification CONFLICT in
 * extensions, since GraphQL has no native CONFLICT error type.
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
		ErrorType errorType = classify(exception);
		String classificationLabel = classificationLabel(exception);
		String message = errorType == ErrorType.INTERNAL_ERROR ? GENERIC_INTERNAL_ERROR_MESSAGE
				: exception.getMessage();

		if (errorType == ErrorType.INTERNAL_ERROR) {
			log.error("Unhandled exception in a GraphQL data fetcher", exception);
		}

		return Mono.just(List.of(GraphqlErrorBuilder.newError(env)
				.message(message)
				.errorType(errorType)
				.extensions(Map.of("classification", classificationLabel))
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
		if (exception instanceof OptimisticLockingFailureException) {
			return ErrorType.BAD_REQUEST;
		}
		return ErrorType.INTERNAL_ERROR;
	}

	// GraphQL has no native CONFLICT error type. OptimisticLockingFailureException
	// is surfaced as BAD_REQUEST at the transport level but with classification
	// CONFLICT in extensions so clients can distinguish it from other 400-like errors.
	private String classificationLabel(Throwable exception) {
		if (exception instanceof OptimisticLockingFailureException) {
			return "CONFLICT";
		}
		return classify(exception).toString();
	}

}
