package com.edgareldy.springgraphqltutorial.graphql.input;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.User;

/**
 * Java shape of the GraphQL UserPage type, built manually from a Spring Data
 * Page&lt;User&gt; rather than relying on field-name reflection over Page
 * itself, since Page's accessor names (getContent, getTotalElements, ...)
 * only partly line up with this schema's field names (page, size).
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public record UserPage(List<User> content, long totalElements, int totalPages, int page, int size) {
}
