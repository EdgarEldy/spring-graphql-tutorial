package com.edgareldy.springgraphqltutorial.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for GraphQlExceptionResolver, verifying that every branch of its
 * private classify(Throwable) logic produces the expected
 * extensions.classification and the expected message: passthrough of the
 * original message for the three known exception types, and a generic,
 * non leaking message for anything else. DataFetchingEnvironment is mocked
 * directly since GraphqlErrorBuilder.newError(env) only reads
 * env.getField().getSourceLocation() and
 * env.getExecutionStepInfo().getPath(), both of which are stubbed here; no
 * Spring context is needed to exercise this plain DataFetcherExceptionResolver.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class GraphQlExceptionResolverTest {

	private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "An unexpected error occurred.";

	private GraphQlExceptionResolver resolver;

	private DataFetchingEnvironment env;

	@BeforeEach
	void setUp() {
		resolver = new GraphQlExceptionResolver();

		Field field = mock(Field.class);
		when(field.getSourceLocation()).thenReturn(null);

		ExecutionStepInfo executionStepInfo = mock(ExecutionStepInfo.class);
		when(executionStepInfo.getPath()).thenReturn(ResultPath.rootPath());

		env = mock(DataFetchingEnvironment.class);
		when(env.getField()).thenReturn(field);
		when(env.getExecutionStepInfo()).thenReturn(executionStepInfo);
	}

	@Test
	void resolveExceptionMapsResourceNotFoundExceptionToNotFound() {
		ResourceNotFoundException exception = new ResourceNotFoundException("Category with id 42 was not found");

		GraphQLError error = resolveSingleError(exception);

		assertThat(error.getMessage()).isEqualTo("Category with id 42 was not found");
		assertThat(error.getExtensions().get("classification")).isEqualTo(ErrorType.NOT_FOUND.toString());
	}

	@Test
	void resolveExceptionMapsBusinessRuleExceptionToBadRequest() {
		BusinessRuleException exception = new BusinessRuleException("Category still has products, it cannot be deleted");

		GraphQLError error = resolveSingleError(exception);

		assertThat(error.getMessage()).isEqualTo("Category still has products, it cannot be deleted");
		assertThat(error.getExtensions().get("classification")).isEqualTo(ErrorType.BAD_REQUEST.toString());
	}

	@Test
	void resolveExceptionMapsAccessDeniedExceptionToForbidden() {
		AccessDeniedException exception = new AccessDeniedException("Access is denied");

		GraphQLError error = resolveSingleError(exception);

		assertThat(error.getMessage()).isEqualTo("Access is denied");
		assertThat(error.getExtensions().get("classification")).isEqualTo(ErrorType.FORBIDDEN.toString());
	}

	@Test
	void resolveExceptionMapsUnrecognizedExceptionToInternalErrorWithGenericMessage() {
		IllegalStateException exception = new IllegalStateException("Column customers.email does not exist");

		GraphQLError error = resolveSingleError(exception);

		assertThat(error.getMessage()).isEqualTo(GENERIC_INTERNAL_ERROR_MESSAGE);
		assertThat(error.getMessage()).doesNotContain("customers.email");
		assertThat(error.getExtensions().get("classification")).isEqualTo(ErrorType.INTERNAL_ERROR.toString());
	}

	private GraphQLError resolveSingleError(Throwable exception) {
		List<GraphQLError> errors = resolver.resolveException(exception, env).block();

		assertThat(errors).hasSize(1);
		return errors.get(0);
	}

}
