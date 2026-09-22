package com.example.treasury.modules.treasury.usecases.gettreasuryaverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.treasury.modules.treasury.domain.models.TreasuryAverage;
import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class GetTreasuryAverageUseCasesTest {

    private final GetTreasuryAverageUseCases useCases = new GetTreasuryAverageUseCases(() -> Flux.just(
            observation("2026-08-11", "5.10"),
            observation("2026-08-12", "5.24"),
            observation("2026-08-13", "5.25"),
            observation("2026-08-14", "5.27"),
            observation("2026-08-17", "5.30")));

    @Test
    void averagesOnlyObservationsInsideTheInclusiveRange() {
        StepVerifier.create(useCases.execute(date("2026-08-12"), date("2026-08-14")))
                .expectNext(new TreasuryAverage(date("2026-08-12"), date("2026-08-14"), new BigDecimal("5.25")))
                .verifyComplete();
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // (5.24 + 5.25) / 2 = 5.245
        StepVerifier.create(useCases.execute(date("2026-08-12"), date("2026-08-13")))
                .expectNext(new TreasuryAverage(date("2026-08-12"), date("2026-08-13"), new BigDecimal("5.25")))
                .verifyComplete();
    }

    @Test
    void supportsSingleDayRange() {
        StepVerifier.create(useCases.execute(date("2026-08-14"), date("2026-08-14")))
                .expectNext(new TreasuryAverage(date("2026-08-14"), date("2026-08-14"), new BigDecimal("5.27")))
                .verifyComplete();
    }

    @Test
    void returnsNotFoundWhenRangeHasNoObservations() {
        StepVerifier.create(useCases.execute(date("2026-08-15"), date("2026-08-16")))
                .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.NOT_FOUND,
                        "No Treasury 30Y observations available in the requested range"))
                .verify();
    }

    @Test
    void rejectsReversedRangeWithoutCallingTheProvider() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        GetTreasuryAverageUseCases useCases = new GetTreasuryAverageUseCases(() -> {
            providerCalled.set(true);
            return Flux.empty();
        });

        StepVerifier.create(useCases.execute(date("2026-08-18"), date("2026-08-12")))
                .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.BAD_REQUEST,
                        "from must be on or before to"))
                .verify();
        assertThat(providerCalled).isFalse();
    }

    @Test
    void propagatesProviderFailures() {
        ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "provider down");
        GetTreasuryAverageUseCases useCases = new GetTreasuryAverageUseCases(() -> Flux.error(failure));

        StepVerifier.create(useCases.execute(date("2026-08-12"), date("2026-08-18")))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                .verify();
    }

    private static void assertStatus(Throwable error, HttpStatus status, String reason) {
        assertThat(error).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).isEqualTo(reason);
        });
    }

    private static TreasuryObservation observation(String date, String value) {
        return new TreasuryObservation(date(date), new BigDecimal(value));
    }

    private static LocalDate date(String date) {
        return LocalDate.parse(date);
    }
}
