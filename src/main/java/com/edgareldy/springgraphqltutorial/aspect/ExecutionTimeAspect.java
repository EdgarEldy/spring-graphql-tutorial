package com.edgareldy.springgraphqltutorial.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Measures and logs how long each GraphQL controller method takes to resolve
 * a field, demonstrating a second, independent {@code @Around} advice
 * alongside {@link LoggingAspect}.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Aspect
@Component
public class ExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("execution(* com.edgareldy.springgraphqltutorial.graphql.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMillis = System.currentTimeMillis() - start;
            log.info("{} executed in {} ms", joinPoint.getSignature().toShortString(), elapsedMillis);
        }
    }
}
