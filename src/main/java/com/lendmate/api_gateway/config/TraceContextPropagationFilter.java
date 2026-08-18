package com.lendmate.api_gateway.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceContextPropagationFilter implements GlobalFilter, Ordered {

    private final OpenTelemetry openTelemetry;

    public TraceContextPropagationFilter(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.deferContextual(contextView -> {
            Context otelContext = Context.current();

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            openTelemetry.getPropagators().getTextMapPropagator()
                    .inject(otelContext, requestBuilder,
                            (carrier, key, value) -> carrier.header(key, value));

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(requestBuilder.build())
                    .build();

            return chain.filter(mutatedExchange);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}