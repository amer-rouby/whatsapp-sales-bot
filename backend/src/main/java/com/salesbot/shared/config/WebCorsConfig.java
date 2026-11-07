package com.salesbot.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The dashboard runs on another origin (Next.js), so it needs CORS for the APIs it calls.
 * One place for every browser-facing path; the WhatsApp webhook is server-to-server and excluded.
 */
@Configuration
class WebCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    WebCorsConfig(@Value("${salesbot.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
        registry.addMapping("/dev/simulator/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST");
    }
}
