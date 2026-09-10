package com.example.treasury;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TreasuryService {

    private final AlphaVantageClient client;

    public Mono<TreasuryAverage> average(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from must be on or before to"));
        }

        return client.observations()
                .filter(observation -> !observation.date().isBefore(from)
                        && !observation.date().isAfter(to))
                .collectList()
                .map(observations -> {
                    if (observations.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No Treasury 30Y observations available in the requested range");
                    }
                    BigDecimal sum = observations.stream()
                            .map(TreasuryObservation::value)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = sum.divide(BigDecimal.valueOf(observations.size()),
                            2, RoundingMode.HALF_UP);
                    return new TreasuryAverage(from, to, average);
                });
    }
}
