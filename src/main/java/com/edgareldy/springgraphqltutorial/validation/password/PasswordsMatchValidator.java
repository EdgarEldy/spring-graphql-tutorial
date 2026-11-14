package com.edgareldy.springgraphqltutorial.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * {@link ConstraintValidator} implementation for {@link PasswordsMatch}.
 * Uses reflection to read the two password fields from the annotated record
 * by name, so the validator stays generic and reusable across any input type
 * that carries a password-confirmation pair.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordsMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            String password = getFieldValue(value, passwordField);
            String confirmPassword = getFieldValue(value, confirmPasswordField);
            if (password == null || confirmPassword == null) {
                return true;
            }
            boolean matches = password.equals(confirmPassword);
            if (!matches) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(confirmPasswordField)
                        .addConstraintViolation();
            }
            return matches;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("@PasswordsMatch: field not found on " + value.getClass().getSimpleName(), e);
        }
    }

    private String getFieldValue(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(object);
    }
}
