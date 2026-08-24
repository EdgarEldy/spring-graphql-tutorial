package com.edgareldy.springgraphqltutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers custom GraphQL scalars and other runtime wiring customizations.
 * <p>
 * No custom scalar is needed yet: the schema only uses the default GraphQL
 * scalars (ID, String, Int, Boolean). This bean is a no-op placeholder ready
 * for a future scalar (for example a Date/DateTime type once timestamp
 * fields are exposed) to be registered without changing how wiring itself is
 * configured.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class GraphQlConfig {

	@Bean
	public RuntimeWiringConfigurer runtimeWiringConfigurer() {
		return wiringBuilder -> {
		};
	}

}
