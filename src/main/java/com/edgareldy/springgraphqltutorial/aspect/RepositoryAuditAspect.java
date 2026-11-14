package com.edgareldy.springgraphqltutorial.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs every repository call, demonstrating the four non-{@code @Around}
 * advice types the project's other aspects do not use:
 * {@code @Before}/{@code @After} on every repository method,
 * {@code @AfterReturning} on {@code save} calls specifically, and
 * {@code @AfterThrowing} on any repository exception. Kept on the repository
 * layer so each layer (graphql.controller, service, repository) has its own,
 * distinctly purposed aspect: {@link ExecutionTimeAspect} times resolver
 * calls, {@link LoggingAspect} logs business calls, this one audits
 * persistence.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Aspect
@Component
public class RepositoryAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(RepositoryAuditAspect.class);

    @Before("execution(* com.edgareldy.springgraphqltutorial.repository..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.debug("About to call {}", joinPoint.getSignature().toShortString());
    }

    @After("execution(* com.edgareldy.springgraphqltutorial.repository..*(..))")
    public void logAfter(JoinPoint joinPoint) {
        log.debug("Finished calling {}", joinPoint.getSignature().toShortString());
    }

    @AfterReturning(pointcut = "execution(* com.edgareldy.springgraphqltutorial.repository..*.save(..))", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("{} persisted {}", joinPoint.getSignature().toShortString(), result);
    }

    @AfterThrowing(pointcut = "execution(* com.edgareldy.springgraphqltutorial.repository..*(..))", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        log.warn("{} threw {}: {}", joinPoint.getSignature().toShortString(),
                ex.getClass().getSimpleName(), ex.getMessage());
    }
}
