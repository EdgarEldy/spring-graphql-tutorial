package com.edgareldy.springgraphqltutorial.graphql.input;

import com.edgareldy.springgraphqltutorial.entity.User;

/**
 * Java shape of the GraphQL AuthPayload type. accessToken and refreshToken are
 * null after register (the account is disabled until activated). After login
 * both tokens are set, tokenType is "Bearer", and expiresIn carries the access
 * token lifetime in milliseconds.
 * <p>
 * Created by edgar.muhamyangabo on 8/24/26
 * Author : edgar.muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public record AuthPayload(String accessToken, String tokenType, String refreshToken, long expiresIn, User user) {

	@Override
	public String toString() {
		return "AuthPayload[accessToken=***, tokenType=" + tokenType
				+ ", refreshToken=***, expiresIn=" + expiresIn
				+ ", user=" + user + "]";
	}

}
