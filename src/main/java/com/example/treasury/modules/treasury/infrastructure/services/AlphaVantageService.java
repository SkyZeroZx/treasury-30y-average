package com.example.treasury.modules.treasury.infrastructure.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.example.treasury.core.domain.config.alphavantage.AlphaVantageConfig;
import com.example.treasury.modules.treasury.domain.adapters.TreasuryServiceAdapter;
import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AlphaVantageService implements TreasuryServiceAdapter {

    private final WebClient webClient;
    private final AlphaVantageConfig alphaVantageConfig;

    @Override
    public Flux<TreasuryObservation> getObservations() {
        return webClient.get()
                .uri(uri -> uri.path("/query")
                        .queryParam("function", "TREASURY_YIELD")
                        .queryParam("interval", "daily")
                        .queryParam("maturity", "30year")
                        .queryParam("apikey", alphaVantageConfig.apiKey())
                        .build())
                .retrieve()
                .bodyToMono(YieldResponse.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty upstream response")))
                .timeout(alphaVantageConfig.timeout())
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
                                : "Treasury data provider returned an unavailable or invalid response",
                        error));
    }

    private record YieldResponse(List<YieldObservation> data) {
    }

    private record YieldObservation(LocalDate date, String value) {
    }
}
