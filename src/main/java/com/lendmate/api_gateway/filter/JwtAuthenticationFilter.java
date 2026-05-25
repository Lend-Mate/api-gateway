package com.lendmate.api_gateway.filter;

import com.lendmate.api_gateway.config.SecurityPaths;
import com.lendmate.api_gateway.service.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("request.getURI().getPath() " + request.getURI().getPath());
        boolean isPublic = SecurityPaths.PUBLIC_PATHS.stream()
                .anyMatch(path -> request.getURI().getPath().startsWith(path));
        System.out.println("isPublic" + isPublic);

        if (!isPublic) {
            if (!request.getHeaders().containsKey("Authorization")) {
                return this.onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().get("Authorization").get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (token.isEmpty() || jwtService.validateToken(token) == false) {
                        return this.onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                    }

                } catch (Exception e) {
                    return this.onError(exchange, "JWT token is not valid!", HttpStatus.UNAUTHORIZED);
                }
            } else {
                return this.onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}