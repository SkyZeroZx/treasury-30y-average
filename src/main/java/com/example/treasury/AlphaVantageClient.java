package com.example.treasury;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AlphaVantageClient {

    private final WebClient webClient;
    private final String apiKey;
    private final Duration timeout;

    public AlphaVantageClient(WebClient.Builder builder,
            @Value("${alpha-vantage.base-url}") String baseUrl,
            @Value("${alpha-vantage.api-key}") String apiKey,
            @Value("${alpha-vantage.timeout}") Duration timeout) {
        Assert.hasText(apiKey, "ALPHA_VANTAGE_API_KEY must be configured");
        this.webClient = builder.baseUrl(baseUrl)
                // The daily endpoint returns the complete historical series.
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.apiKey = apiKey;
        this.timeout = timeout;
    }

    public Flux<TreasuryObservation> observations() {
        return webClient.get()
                .uri(uri -> uri.path("/query")
                        .queryParam("function", "TREASURY_YIELD")
                        .queryParam("interval", "daily")
                        .queryParam("maturity", "30year")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(YieldResponse.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty upstream response")))
                .timeout(timeout)
                .flatMapMany(response -> {
                    // Alpha Vantage also reports API errors and rate limits with HTTP 200.
                    if (response.data() == null) {
                        return Flux.error(new IllegalStateException("Missing upstream data"));
                    }
                    return Flux.fromIterable(response.data());
                })
                .map(observation -> {
                    if (observation.date() == null || observation.value() == null) {
                        throw new IllegalStateException("Incomplete upstream observation");
                    }
                    return observation;
                })
                .filter(observation -> !".".equals(observation.value()))
                .map(observation -> new TreasuryObservation(observation.date(),
                        new BigDecimal(observation.value())))
                .onErrorMap(error -> new ResponseStatusException(
                        error instanceof TimeoutException ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                        error instanceof TimeoutException
                                ? "Treasury data provider timed out"
                                : "Treasury data provider returned an unavailable or invalid response"));
    }

    private record YieldResponse(List<YieldObservation> data) {
    }

    private record YieldObservation(LocalDate date, String value) {
    }
}
