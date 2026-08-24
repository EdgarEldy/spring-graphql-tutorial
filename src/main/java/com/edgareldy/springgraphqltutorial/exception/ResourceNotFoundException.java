package com.edgareldy.springgraphqltutorial.exception;

/**
 * Thrown when a requested entity does not exist. Mapped by
 * GraphQlExceptionResolver to the NOT_FOUND classification.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

}
