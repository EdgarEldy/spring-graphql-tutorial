package com.edgareldy.springgraphqltutorial.validation.unique;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation constraint that rejects a value if a row already
 * exists in the database for the given entity field. The check uses JPQL
 * via {@link jakarta.persistence.EntityManager} so it works with any JPA
 * entity without a dedicated repository method.
 * <p>
 * The {@link #caseSensitive()} attribute (default {@code true}) controls
 * whether the JPQL comparison uses {@code LOWER()} on both sides, useful
 * for fields like email where the uniqueness rule is case-insensitive.
 * <p>
 * Note: this validator runs before the service transaction opens, so it
 * cannot prevent the race condition between the check and the INSERT. The
 * database {@code UNIQUE} constraint remains the authoritative guard; this
 * annotation improves the developer experience by surfacing the conflict as
 * a 400 field error instead of a 500 DataIntegrityViolation.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Documented
@Constraint(validatedBy = UniqueValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {

    String message() default "Value is already in use";

    Class<?> entityClass();

    String fieldName();

    boolean caseSensitive() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
