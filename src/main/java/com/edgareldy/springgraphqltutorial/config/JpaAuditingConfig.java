package com.edgareldy.springgraphqltutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so that @CreatedDate and @LastModifiedDate fields on
 * entity classes are populated automatically by Spring Data on persist and
 * merge. First consumed by Category in feature/categories; prepared here so
 * the config is in place before any entity needs it.
 * <p>
 * Created by Edgar Muhamyangabo on 11/13/26
 * Author : Edgar Muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
