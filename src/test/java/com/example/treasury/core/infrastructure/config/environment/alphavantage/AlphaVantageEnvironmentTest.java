package com.example.treasury.core.infrastructure.config.environment.alphavantage;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AlphaVantageEnvironmentTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void requiresAnApiKey(String apiKey) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AlphaVantageEnvironment("https://www.alphavantage.co", apiKey,
                        Duration.ofSeconds(10)))
                .withMessage("ALPHA_VANTAGE_API_KEY must be configured");
    }
}
