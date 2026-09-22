package com.example.treasury.core.domain.config.alphavantage;

import java.time.Duration;

public interface AlphaVantageConfig {

    String baseUrl();

    String apiKey();

    Duration timeout();
}
