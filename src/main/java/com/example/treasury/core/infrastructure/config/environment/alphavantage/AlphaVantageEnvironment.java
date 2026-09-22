package com.example.treasury.core.infrastructure.config.environment.alphavantage;

import java.time.Duration;

import com.example.treasury.core.domain.config.alphavantage.AlphaVantageConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties("alpha-vantage")
public record AlphaVantageEnvironment(String baseUrl, String apiKey, Duration timeout)
        implements AlphaVantageConfig {

    public AlphaVantageEnvironment {
        Assert.hasText(apiKey, "ALPHA_VANTAGE_API_KEY must be configured");
    }
}
