package com.material.gateway.filter;

import com.material.common.constant.AuthConstants;
import com.material.common.constant.RedisConstants;
import com.material.gateway.handler.GatewayExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAuthGlobalFilterTest {
    @Mock
    private ReactiveStringRedisTemplate redisTemplate;
    @Mock
    private ReactiveHashOperations<String, String, String> hashOperations;

    private TokenAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TokenAuthGlobalFilter(redisTemplate, new GatewayExceptionHandler());
    }

    @Test
    void loginPathIsAllowedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login").build()
        );
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = next -> {
            filteredExchange.set(next);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(filteredExchange.get()).isSameAs(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void optionsPreflightIsAllowedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/suppliers/catalog").build()
        );
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = next -> {
            filteredExchange.set(next);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(filteredExchange.get()).isSameAs(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void protectedPathWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build()
        );

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString())
                .isEqualTo("application/json");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"code\":401,\"message\":\"unauthorized\",\"data\":null}");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void protectedPathWithMalformedAuthorizationHeaderReturnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Token abc")
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void protectedPathWithValidTokenAddsUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
                        .build()
        );
        Map<String, String> loginUser = loginUser();
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123"))
                .thenReturn(Flux.fromIterable(loginUser.entrySet()));
        when(redisTemplate.expire(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123", RedisConstants.LOGIN_TOKEN_TTL))
                .thenReturn(Mono.just(Boolean.TRUE));

        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = next -> {
            filteredExchange.set(next);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = filteredExchange.get().getRequest().getHeaders();
        assertThat(headers.getFirst(AuthConstants.HEADER_USER_ID)).isEqualTo("7");
        assertThat(headers.getFirst(AuthConstants.HEADER_USER_TYPE)).isEqualTo("SUPPLIER");
        assertThat(headers.getFirst(AuthConstants.HEADER_USERNAME)).isEqualTo("supplier-a");
        assertThat(headers.getFirst(AuthConstants.HEADER_DISPLAY_NAME)).isEqualTo("Acme Materials");
    }

    @Test
    void protectedPathWithValidTokenRefreshesTtl() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
                        .build()
        );
        Map<String, String> loginUser = loginUser();
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUser.entrySet()));
        when(redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL)).thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(filter.filter(exchange, next -> Mono.empty()))
                .verifyComplete();

        verify(redisTemplate).expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL);
    }

    @Test
    void protectedPathWithExpiredTokenRefreshFailureReturnsUnauthorized() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        MockServerWebExchange exchange = authenticatedExchange("token-123");
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUser().entrySet()));
        when(redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL)).thenReturn(Mono.just(Boolean.FALSE));

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
    }

    @Test
    void protectedPathWithRedisExpireErrorReturnsUnauthorized() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        MockServerWebExchange exchange = authenticatedExchange("token-123");
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUser().entrySet()));
        when(redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL))
                .thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
    }

    @Test
    void protectedPathWithSpoofedUserHeadersForwardsTrustedRedisHeadersOnly() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
                        .header(AuthConstants.HEADER_USER_ID, "spoofed-id")
                        .header(AuthConstants.HEADER_USER_TYPE, "spoofed-type")
                        .header(AuthConstants.HEADER_USERNAME, "spoofed-username")
                        .header(AuthConstants.HEADER_DISPLAY_NAME, "spoofed-name")
                        .build()
        );
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUserWithoutDisplayName().entrySet()));
        when(redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL)).thenReturn(Mono.just(Boolean.TRUE));

        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = next -> {
            filteredExchange.set(next);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = filteredExchange.get().getRequest().getHeaders();
        assertThat(headers.get(AuthConstants.HEADER_USER_ID)).containsExactly("7");
        assertThat(headers.get(AuthConstants.HEADER_USER_TYPE)).containsExactly("SUPPLIER");
        assertThat(headers.get(AuthConstants.HEADER_USERNAME)).containsExactly("supplier-a");
        assertThat(headers.get(AuthConstants.HEADER_DISPLAY_NAME)).isNull();
    }

    @Test
    void protectedPathWithEmptyRedisHashReturnsUnauthorized() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        MockServerWebExchange exchange = authenticatedExchange("token-123");
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.empty());

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
    }

    @Test
    void protectedPathWithMissingRequiredUserFieldReturnsUnauthorized() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        Map<String, String> loginUser = loginUser();
        loginUser.remove("username");
        MockServerWebExchange exchange = authenticatedExchange("token-123");
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUser.entrySet()));

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
    }

    @Test
    void protectedPathWithBlankRequiredUserFieldReturnsUnauthorized() {
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123";
        Map<String, String> loginUser = loginUser();
        loginUser.put("username", " ");
        MockServerWebExchange exchange = authenticatedExchange("token-123");
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(redisKey)).thenReturn(Flux.fromIterable(loginUser.entrySet()));

        StepVerifier.create(filter.filter(exchange, notCalledChain()))
                .verifyComplete();

        assertUnauthorized(exchange);
    }

    private static GatewayFilterChain notCalledChain() {
        return exchange -> Mono.error(new AssertionError("filter chain should not be called"));
    }

    private static MockServerWebExchange authenticatedExchange(String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );
    }

    private static void assertUnauthorized(MockServerWebExchange exchange) {
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"code\":401,\"message\":\"unauthorized\",\"data\":null}");
    }

    private static Map<String, String> loginUser() {
        Map<String, String> loginUser = new LinkedHashMap<>();
        loginUser.put("id", "7");
        loginUser.put("userType", "SUPPLIER");
        loginUser.put("username", "supplier-a");
        loginUser.put("displayName", "Acme Materials");
        return loginUser;
    }

    private static Map<String, String> loginUserWithoutDisplayName() {
        Map<String, String> loginUser = loginUser();
        loginUser.remove("displayName");
        return loginUser;
    }
}
