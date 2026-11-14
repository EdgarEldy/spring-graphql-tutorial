package com.edgareldy.springgraphqltutorial.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator endpoint exposing basic runtime information beyond what
 * the built-in {@code health}/{@code info}/{@code metrics} endpoints provide,
 * reachable at {@code /actuator/appinfo} once exposed via
 * {@code management.endpoints.web.exposure.include} in the active profile.
 * <p>
 * Created by Edgar Muhamyangabo on 11/14/26
 * Author : Edgar Muhamyangabo
 * Date : 11/14/26
 * Project : spring-graphql-tutorial
 */
@Component
@Endpoint(id = "appinfo")
@RequiredArgsConstructor
public class AppInfoEndpoint {

    private final Environment environment;

    @ReadOperation
    public AppInfo appInfo() {
        return new AppInfo(
                environment.getProperty("spring.application.name"),
                environment.getActiveProfiles(),
                Runtime.version().toString());
    }

    /**
     * Payload returned by {@link #appInfo()}.
     * <p>
     * Created by Edgar Muhamyangabo on 11/14/26
     * Author : Edgar Muhamyangabo
     * Date : 11/14/26
     * Project : spring-graphql-tutorial
     */
    public record AppInfo(String applicationName, String[] activeProfiles, String javaVersion) {
    }
}
