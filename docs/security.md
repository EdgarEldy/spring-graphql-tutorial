# Security

## Two-layer security design

Spring for GraphQL routes all operations through a single path (`/graphql`), so the
standard Spring Security approach of matching paths to roles does not work. The project
uses two complementary mechanisms instead:

| Mechanism | Type | Responsibility |
|---|---|---|
| `JwtAuthFilter` | Servlet `OncePerRequestFilter` | Authenticates the principal (populates `SecurityContextHolder`) |
| `JwtContextInterceptor` | `WebGraphQlInterceptor` | Stores the raw JWT string in the GraphQL context for `logout` |

### Why both?

`JwtAuthFilter` is a Servlet filter. It runs before every HTTP request and sets the
`Authentication` object in `SecurityContextHolder`, which `@PreAuthorize` expressions
read at method-dispatch time. This is the standard bearer of identity in Spring Security.

`JwtContextInterceptor` implements `WebGraphQlInterceptor`, Spring for GraphQL's own
interceptor chain. It runs at the GraphQL layer, after the Servlet filter, and places
the raw JWT string in the `GraphQLContext` under a known key. The `logout` mutation
needs the raw token to extract the `jti` and add it to the blacklist. Reading the token
back from `SecurityContextHolder` is fragile (the `Authentication` holds a parsed
principal, not the original string), so the interceptor stores it once where the
mutation can retrieve it cleanly via `@ContextValue`.

A secondary benefit of `JwtContextInterceptor`: it handles both HTTP requests and
WebSocket connections, while Servlet filters only see HTTP. This keeps token propagation
consistent across the two transports.

## `SecurityConfig`

```java
http.csrf(AbstractHttpConfigurer::disable)
    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

All HTTP requests are `permitAll()` at the `HttpSecurity` level. Access control is
enforced entirely at the **method level** via `@PreAuthorize` on individual controller
operations. This design is the correct approach for GraphQL: restricting the `/graphql`
path at the security filter level would either block every operation or allow them all.

`@EnableMethodSecurity` is declared on `SecurityConfig` to enable `@PreAuthorize`.

## `JwtAuthFilter`

The filter rejects the request early in two cases:
- The token cannot be parsed or its signature is invalid.
- `jwtService.isRefreshToken(token)` returns `true` - refresh tokens must not be used
  as access credentials (they carry a `token_type: "refresh"` claim that the access
  token does not).

For every other valid token, the filter:
1. Extracts the `jti` and checks the blacklist (via `BlacklistedTokenService`).
2. Loads the `UserDetails` by username (`sub` claim).
3. Builds a `UsernamePasswordAuthenticationToken` from the loaded user's authorities
   and sets it on `SecurityContextHolder`.

If a `DataAccessException` is thrown while checking the blacklist (database unavailable),
the filter logs it and falls through without setting an `Authentication`, causing the
request to be treated as anonymous.

## `JwtContextInterceptor`

```java
public Mono<WebOutput> intercept(WebInput input, Chain chain) {
    String token = /* extract from Authorization header */;
    if (token != null) {
        return chain.next(input.transform(builder ->
            builder.context(ctx -> ctx.put(JWT_CONTEXT_KEY, token))));
    }
    return chain.next(input);
}
```

The interceptor is transparent: it only adds to the context and passes control to the
next interceptor. The `logout` mutation reads the stored token with
`@ContextValue(name = JwtContextInterceptor.JWT_CONTEXT_KEY)`.

## JWT structure

**Access token** (1 hour by default):

| Claim | Content |
|---|---|
| `sub` | User email |
| `jti` | UUID (used for blacklisting on logout) |
| `roles` | List of role names (`ADMIN`, `USER`, ...) |
| `permissions` | List of `resource:action` strings |

**Refresh token** (7 days by default):

| Claim | Content |
|---|---|
| `sub` | User email |
| `jti` | UUID |
| `token_type` | `"refresh"` |

Refresh tokens are stateless signed JWTs, not stored in the database.
`JwtAuthFilter` rejects them as access credentials by checking the `token_type` claim.

## Authorization rules

Access level per domain (see [api-reference.md](api-reference.md) for per-operation detail):

| Resource | Public | Authenticated | ADMIN |
|---|---|---|---|
| Auth (register, login, activate, resetPassword) | yes | | |
| Auth (logout, me, updateProfile) | | yes | |
| Categories (read) | yes | | |
| Categories (write) | | | yes |
| Products (read) | yes | | |
| Products (write) | | | yes |
| Customers | | | yes |
| Orders (read) | | yes | |
| Orders (write) | | | yes |
| Subscription | | yes | |
| Users, Roles, Permissions | | | yes |

## Token lifecycle

```
register  -> enabled=false, activation email sent
activateAccount(token) -> enabled=true
login     -> accessToken (1h) + refreshToken (7d)
refreshToken(refreshToken) -> new accessToken + new refreshToken
logout    -> jti added to blacklisted_tokens
```

Password reset:

```
requestPasswordReset(email) -> email with reset token (always returns true)
resetPassword(token, newPassword) -> token consumed, password updated
```

## `@EnableConfigurationProperties(JwtProperties.class)`

JWT configuration is typed via `JwtProperties` (a `record` annotated
`@ConfigurationProperties(prefix = "jwt")`), registered on `SecurityConfig`. This
replaces the two `@Value` fields that an equivalent SBT-era service might use, and
provides compile-time safety for `jwt.secret`, `jwt.expiration-ms`, and
`jwt.refresh-expiration-ms`.
