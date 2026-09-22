package com.example.treasury.modules.treasury.infrastructure.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.treasury.modules.treasury.domain.adapters.TreasuryServiceAdapter;
import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * Exercises the whole application (controller, use case and exception filter) with a stubbed provider.
 */
@SpringBootTest(properties = "alpha-vantage.api-key=test-key")
class TreasuryControllerTest {

    private static final String AVERAGE_PATH = "/api/v1/treasury/30y/average";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StubTreasuryServiceAdapter treasuryService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
        treasuryService.respondWith(Flux.just(
                observation("2026-08-12", "5.24"),
                observation("2026-08-13", "5.25"),
                observation("2026-08-14", "5.27"),
                observation("2026-08-17", "5.28"),
                observation("2026-08-18", "5.26")));
    }

    @Test
    void returnsTheAverageForTheRequestedRange() {
        webTestClient.get().uri(AVERAGE_PATH + "?from=2026-08-12&to=2026-08-18")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().json("""
                        {"from":"2026-08-12","to":"2026-08-18","average":5.26}
                        """, JsonCompareMode.STRICT);
    }

    @Test
    void rejectsReversedRange() {
        expectProblem(AVERAGE_PATH + "?from=2026-08-18&to=2026-08-12", HttpStatus.BAD_REQUEST,
                "from must be on or before to");
    }

    @Test
    void rejectsMalformedDate() {
        expectProblem(AVERAGE_PATH + "?from=2026-13-01&to=2026-08-18", HttpStatus.BAD_REQUEST, "Type mismatch.");
    }

    @Test
    void rejectsMissingParameter() {
        expectProblem(AVERAGE_PATH + "?from=2026-08-12", HttpStatus.BAD_REQUEST,
                "Required query parameter 'to' is not present.");
    }

    @Test
    void returnsNotFoundWhenRangeHasNoData() {
        expectProblem(AVERAGE_PATH + "?from=2026-08-15&to=2026-08-16", HttpStatus.NOT_FOUND,
                "No Treasury 30Y observations available in the requested range");
    }

    @Test
    void returnsBadGatewayWhenProviderFails() {
        treasuryService.respondWith(Flux.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Treasury data provider returned an unavailable or invalid response")));

        expectProblem(AVERAGE_PATH + "?from=2026-08-12&to=2026-08-18", HttpStatus.BAD_GATEWAY,
                "Treasury data provider returned an unavailable or invalid response");
    }

    private void expectProblem(String uri, HttpStatus status, String detail) {
        webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(status.value())
                .jsonPath("$.detail").isEqualTo(detail)
                .jsonPath("$.instance").isEqualTo(AVERAGE_PATH);
    }

    private static TreasuryObservation observation(String date, String value) {
        return new TreasuryObservation(LocalDate.parse(date), new BigDecimal(value));
    }

    @TestConfiguration
    static class StubProviderConfiguration {

        @Bean
        @Primary
        StubTreasuryServiceAdapter stubTreasuryServiceAdapter() {
            return new StubTreasuryServiceAdapter();
        }
    }

    static class StubTreasuryServiceAdapter implements TreasuryServiceAdapter {

        private Flux<TreasuryObservation> observations = Flux.empty();

        void respondWith(Flux<TreasuryObservation> observations) {
            this.observations = observations;
        }

        @Override
        public Flux<TreasuryObservation> getObservations() {
            return observations;
        }
    }
}
