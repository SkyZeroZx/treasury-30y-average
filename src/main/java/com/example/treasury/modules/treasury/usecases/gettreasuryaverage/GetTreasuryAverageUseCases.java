package com.example.treasury.modules.treasury.usecases.gettreasuryaverage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.example.treasury.modules.treasury.domain.adapters.TreasuryServiceAdapter;
import com.example.treasury.modules.treasury.domain.models.TreasuryAverage;
import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTreasuryAverageUseCases {

    private final TreasuryServiceAdapter treasuryService;

    public Mono<TreasuryAverage> execute(LocalDate from, LocalDate to) {
        log.info("Execute GetTreasuryAverageUseCases from={} to={}", from, to);

        if (from.isAfter(to)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from must be on or before to"));
        }

        return treasuryService.getObservations()
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
