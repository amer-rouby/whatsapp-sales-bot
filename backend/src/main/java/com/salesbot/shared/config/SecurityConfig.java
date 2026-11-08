package com.salesbot.shared.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.salesbot.shared.security.TokenClaims;

/**
 * Who may call what.
 * Open: the WhatsApp webhook (guarded by Meta's HMAC signature), health, and login.
 * Everything else needs a valid dashboard token (HS256 JWT, stateless — no server sessions, no cookies here).
 */
@Configuration
class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a 256-bit key

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${salesbot.security.expose-api-docs:false}") boolean exposeApiDocs)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // bearer tokens only, nothing the browser sends automatically
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/webhook", "/webhook/**").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    if (exposeApiDocs) {
                        auth.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll(); // dev: npm run gen:api
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    SecretKey jwtSigningKey(@Value("${salesbot.security.jwt-secret:}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("SALESBOT_JWT_SECRET must be set to at least " + MIN_SECRET_BYTES
                    + " characters (generate one with: openssl rand -hex 32)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        // expiry + issuer: tokens not issued by this backend are rejected
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenClaims.ISSUER));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
