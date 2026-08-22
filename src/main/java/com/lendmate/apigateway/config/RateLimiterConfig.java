package com.lendmate.apigateway.config;


import com.lendmate.apigateway.service.JwtService;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    private final JwtService jwtService;

    public RateLimiterConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public KeyResolver userKeyResolver(JwtService jwtService) {

        return exchange -> {

            String authHeader =
                    exchange.getRequest()
                            .getHeaders()
                            .getFirst("Authorization");

            if (authHeader == null ||
                    !authHeader.startsWith("Bearer ")) {

                return Mono.just("anonymous");
            }

            String token = authHeader.substring(7);

            if (!jwtService.validateToken(token)) {
                return Mono.just("anonymous");
            }

            return Mono.just(
                    jwtService.extractUsername(token)
            );
        };
    }
}