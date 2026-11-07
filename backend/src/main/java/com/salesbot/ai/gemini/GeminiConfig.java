package com.salesbot.ai.gemini;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
class GeminiConfig {

    @Bean
    @ConditionalOnProperty(name = "salesbot.ai.provider", havingValue = "gemini")
    RestClient geminiRestClient(RestClient.Builder builder, GeminiProperties properties) {
        return configure(builder, properties).build();
    }

    /** Base URL, API key header and timeouts. Shared with tests so they exercise the same setup. */
    static RestClient.Builder configure(RestClient.Builder builder, GeminiProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("salesbot.ai.gemini.api-key is required when salesbot.ai.provider=gemini");
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());

        return builder
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-goog-api-key", properties.apiKey());
    }
}
