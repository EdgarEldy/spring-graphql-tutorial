package com.edgareldy.springgraphqltutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled support so TokenCleanupScheduler can run its
 * nightly cleanup without additional configuration on the main class.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
