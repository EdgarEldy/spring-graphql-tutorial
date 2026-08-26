package com.edgareldy.springgraphqltutorial.exception;

/**
 * Thrown when an operation violates a business rule (for example deleting a
 * category that still has products). Mapped by GraphQlExceptionResolver to
 * the BAD_REQUEST classification, with the exception message surfaced
 * verbatim since it is meant to be read by the client.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
