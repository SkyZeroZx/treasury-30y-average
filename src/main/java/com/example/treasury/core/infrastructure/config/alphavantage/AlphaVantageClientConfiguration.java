package com.example.treasury.core.infrastructure.config.alphavantage;

import com.example.treasury.core.domain.config.alphavantage.AlphaVantageConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AlphaVantageClientConfiguration {

    @Bean
    WebClient alphaVantageWebClient(WebClient.Builder builder, AlphaVantageConfig alphaVantageConfig) {
        return builder.baseUrl(alphaVantageConfig.baseUrl())
                // The daily endpoint returns the complete historical series.
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
