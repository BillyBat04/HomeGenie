package com.homegenie.gateway.filter;

import com.homegenie.gateway.config.PublicRoutesConfig;
import com.homegenie.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PublicRoutesConfig publicRoutesConfig;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, publicRoutesConfig);
    }

    @Test
    void apply_publicRoute_skipsJwtAndDelegatesChain() {
        when(publicRoutesConfig.isPublicRoute("/api/users/login")).thenReturn(true);
        when(chain.filter(any())).thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/login").build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void apply_missingAuthorizationHeader_returns401() {
        when(publicRoutesConfig.isPublicRoute(any())).thenReturn(false);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments").build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apply_bearerPrefixMissing_returns401() {
        when(publicRoutesConfig.isPublicRoute(any())).thenReturn(false);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Token some-token")
                        .build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apply_invalidToken_returns401() {
        when(publicRoutesConfig.isPublicRoute(any())).thenReturn(false);
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apply_validToken_propagatesUserIdentityHeaders() {
        when(publicRoutesConfig.isPublicRoute(any())).thenReturn(false);
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn("user-123");
        when(jwtTokenProvider.getRoleFromToken("valid-token")).thenReturn("USER");
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("test@example.com");
        when(chain.filter(any())).thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        HttpHeaders downstreamHeaders = captor.getValue().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst("X-User-Id")).isEqualTo("user-123");
        assertThat(downstreamHeaders.getFirst("X-User-Role")).isEqualTo("USER");
        assertThat(downstreamHeaders.getFirst("X-User-Email")).isEqualTo("test@example.com");
        assertThat(downstreamHeaders.getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    void apply_validToken_nullClaimsDefaultToEmptyHeader() {
        when(publicRoutesConfig.isPublicRoute(any())).thenReturn(false);
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(null);
        when(jwtTokenProvider.getRoleFromToken("valid-token")).thenReturn(null);
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn(null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        HttpHeaders downstreamHeaders = captor.getValue().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst("X-User-Id")).isEmpty();
        assertThat(downstreamHeaders.getFirst("X-User-Role")).isEmpty();
        assertThat(downstreamHeaders.getFirst("X-User-Email")).isEmpty();
    }

    @Test
    void apply_publicRoute_addsRequestId() {
        when(publicRoutesConfig.isPublicRoute("/actuator/health")).thenReturn(true);
        when(chain.filter(any())).thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }
}
