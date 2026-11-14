package com.edgareldy.springgraphqltutorial.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * {@link ConstraintValidator} implementation for {@link ValidPassword}.
 * Accepts {@code null} (let {@code @NotBlank} handle that) and validates
 * non-null values against a single regex that checks all complexity rules
 * in one pass.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])" +
            "(?=.*\\d)" +
            "(?=.*[@#$%^&+=!])" +
            "\\S{8,}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return PASSWORD_PATTERN.matcher(value).matches();
    }
}
