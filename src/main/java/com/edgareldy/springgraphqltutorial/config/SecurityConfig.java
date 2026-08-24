package com.edgareldy.springgraphqltutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration. Permits every request and disables CSRF
 * for now: no authentication mechanism exists yet, so locking anything down
 * would only break GraphiQL and the /graphql endpoint without protecting
 * anything real. feature/auth replaces this with JWT authentication and
 * per-operation @PreAuthorize rules once there is something to authenticate
 * against.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}

}
