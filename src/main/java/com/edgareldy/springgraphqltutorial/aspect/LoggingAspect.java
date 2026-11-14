package com.edgareldy.springgraphqltutorial.aspect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs entry, exit, arguments, and thrown exceptions for every method on
 * every {@code @Service} bean, demonstrating {@code @Around} advice kept
 * entirely outside the business logic it observes.
 * <p>
 * Sensitive values are redacted before logging: a record component named
 * "password"/"token"/"jwt"/"jti"/"secret" (case insensitive), a method
 * argument whose declared parameter name matches the same keywords, and
 * the return value of any {@code String}-returning method declared on a
 * class whose simple name contains "Token".
 * <p>
 * <strong>Compilation requirement:</strong> parameter-name masking relies
 * on {@link java.lang.reflect.Parameter#getName()} returning source names.
 * This requires the {@code -parameters} javac flag, which
 * {@code spring-boot-starter-parent} activates by default.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.edgareldy.springgraphqltutorial.service..*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String arguments = describeArguments(method, joinPoint.getArgs());
        log.info("Entering {} with arguments [{}]", signature, arguments);
        try {
            Object result = joinPoint.proceed();
            log.info("Exiting {} with result {}", signature, describeResult(method, result));
            return result;
        } catch (Throwable ex) {
            log.error("Exception in {}: {}", signature, ex.getMessage());
            throw ex;
        }
    }

    private String describeArguments(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        return IntStream.range(0, args.length)
                .mapToObj(i -> isSensitiveName(parameters[i].getName()) ? "***" : describe(args[i]))
                .collect(Collectors.joining(", "));
    }

    private String describeResult(Method method, Object result) {
        if (method.getReturnType() == String.class
                && method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT).contains("token")) {
            return "***";
        }
        return describe(result);
    }

    private String describe(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> type = value.getClass();
        if (!type.isRecord()) {
            return value.toString();
        }
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName() + "=" + describeComponent(component, value))
                .collect(Collectors.joining(", ", type.getSimpleName() + "[", "]"));
    }

    private Object describeComponent(RecordComponent component, Object record) {
        if (isSensitiveName(component.getName())) {
            return "***";
        }
        try {
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException ex) {
            return "?";
        }
    }

    private boolean isSensitiveName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("token") || lower.contains("jwt")
                || lower.contains("jti") || lower.contains("secret");
    }
}
