package com.edgareldy.springgraphqltutorial.email;

import lombok.Getter;

/**
 * Enum mapping each transactional email type to its Thymeleaf template name.
 * The {@code name} field matches the HTML file name (without extension) under
 * {@code src/main/resources/templates/}, so Thymeleaf can resolve it directly
 * via {@code SpringTemplateEngine.process(template.getName(), context)}.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    RESET_PASSWORD("reset_password");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
