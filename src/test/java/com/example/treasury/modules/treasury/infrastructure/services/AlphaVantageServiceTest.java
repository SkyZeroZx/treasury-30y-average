package com.example.treasury.modules.treasury.infrastructure.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.example.treasury.core.domain.config.alphavantage.AlphaVantageConfig;
import com.example.treasury.core.infrastructure.config.environment.alphavantage.AlphaVantageEnvironment;
import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AlphaVantageServiceTest {

    private static final AlphaVantageConfig CONFIG =
            new AlphaVantageEnvironment("https://alpha-vantage.test", "test-key", Duration.ofMillis(200));

    private final AtomicReference<ClientRequest> lastRequest = new AtomicReference<>();

    @Test
    void requestsTheDailyThirtyYearSeriesAndSkipsUnavailableValues() {
        AlphaVantageService service = serviceResponding(json(HttpStatus.OK, """
                {"data":[{"date":"2026-08-12","value":"5.24"},{"date":"2026-08-11","value":"."}]}
                """));

        StepVerifier.create(service.getObservations())
                .expectNext(new TreasuryObservation(LocalDate.parse("2026-08-12"), new BigDecimal("5.24")))
                .verifyComplete();
        assertThat(lastRequest.get().url()).hasToString("https://alpha-vantage.test/query"
                + "?function=TREASURY_YIELD&interval=daily&maturity=30year&apikey=test-key");
    }

    static Stream<Arguments> invalidResponses() {
        return Stream.of(
                arguments("rate limit reported with HTTP 200",
                        json(HttpStatus.OK, "{\"Information\":\"Rate limit reached\"}")),
                arguments("server error", json(HttpStatus.INTERNAL_SERVER_ERROR, "{}")),
                arguments("malformed value",
                        json(HttpStatus.OK, "{\"data\":[{\"date\":\"2026-08-12\",\"value\":\"abc\"}]}")),
                arguments("incomplete observation", json(HttpStatus.OK, "{\"data\":[{\"value\":\"5.24\"}]}")),
                arguments("empty body", json(HttpStatus.OK, "")),
                arguments("invalid JSON", json(HttpStatus.OK, "{not json")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidResponses")
    void mapsInvalidProviderResponsesToBadGateway(String scenario, Mono<ClientResponse> response) {
        StepVerifier.create(serviceResponding(response).getObservations())
                .expectErrorSatisfies(error -> assertProviderFailure(error, HttpStatus.BAD_GATEWAY,
                        "Treasury data provider returned an unavailable or invalid response"))
                .verify();
    }

    @Test
    void mapsSlowProviderToGatewayTimeout() {
        StepVerifier.create(serviceResponding(Mono.never()).getObservations())
                .expectErrorSatisfies(error -> assertProviderFailure(error, HttpStatus.GATEWAY_TIMEOUT,
                        "Treasury data provider timed out"))
                .verify(Duration.ofSeconds(5));
    }

    private AlphaVantageService serviceResponding(Mono<ClientResponse> response) {
        WebClient webClient = WebClient.builder()
                .baseUrl(CONFIG.baseUrl())
                .exchangeFunction(request -> {
                    lastRequest.set(request);
                    return response;
                })
                .build();
        return new AlphaVantageService(webClient, CONFIG);
    }

    private static Mono<ClientResponse> json(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private static void assertProviderFailure(Throwable error, HttpStatus status, String reason) {
        assertThat(error).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).isEqualTo(reason);
            assertThat(exception.getCause()).isNotNull();
        });
    }
}
