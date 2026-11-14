package com.edgareldy.springgraphqltutorial.graphql.input;

import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.validation.password.PasswordsMatch;
import com.edgareldy.springgraphqltutorial.validation.password.ValidPassword;
import com.edgareldy.springgraphqltutorial.validation.unique.Unique;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Java shape of the GraphQL RegisterInput input type. Carries the full set of
 * Jakarta Bean Validation constraints used for programmatic validation in
 * {@code UserServiceImpl.register()} - Spring for GraphQL does not trigger
 * {@code @Valid} on {@code @Argument} parameters automatically.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@PasswordsMatch
public record RegisterInput(
        @NotBlank(message = "First name must not be blank")
        String firstName,
        @NotBlank(message = "Last name must not be blank")
        String lastName,
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        @Unique(entityClass = User.class, fieldName = "email", caseSensitive = false,
                message = "Email is already in use")
        String email,
        @NotBlank(message = "Password must not be blank")
        @ValidPassword
        String password,
        @NotBlank(message = "Confirm password must not be blank")
        String confirmPassword) {

    @Override
    public String toString() {
        return "RegisterInput[firstName=" + firstName + ", lastName=" + lastName
                + ", email=" + email + ", password=***, confirmPassword=***]";
    }
}
