package com.edgareldy.springgraphqltutorial;

import org.springframework.boot.SpringApplication;

/**
 * Convenience entry point for running the application locally against a
 * throwaway Testcontainers PostgreSQL instance instead of a real database.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public class TestSpringGraphqlTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringGraphqlTutorialApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
