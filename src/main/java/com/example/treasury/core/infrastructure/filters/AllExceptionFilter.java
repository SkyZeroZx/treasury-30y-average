package com.example.treasury.core.infrastructure.filters;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Renders handled exceptions as Problem Details and logs how each failed request ended.
 */
@Slf4j
@RestControllerAdvice
public class AllExceptionFilter extends ResponseEntityExceptionHandler {

    @Override
    protected Mono<ResponseEntity<Object>> handleExceptionInternal(Exception exception,
            @Nullable Object body, @Nullable HttpHeaders headers, HttpStatusCode status,
            ServerWebExchange exchange) {
        logMessage(exchange.getRequest(), status, exception);
        return super.handleExceptionInternal(exception, body, headers, status, exchange);
    }

    private void logMessage(ServerHttpRequest request, HttpStatusCode status, Exception exception) {
        if (status.is5xxServerError()) {
            log.error("End Request for {} method={} status={} message={}", request.getPath(),
                    request.getMethod(), status.value(), exception.getMessage(), exception);
            return;
        }
        log.warn("End Request for {} method={} status={} message={}", request.getPath(),
                request.getMethod(), status.value(), exception.getMessage());
    }
}
