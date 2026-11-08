package com.salesbot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Full application on a throw-away PostgreSQL.
 * Pins the settings a developer's local {@code .env} may change (inline test properties win over
 * every config file), so tests never call the real Gemini or WhatsApp.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "salesbot.ai.provider=echo",
        "salesbot.whatsapp.sender=log",
        "salesbot.simulator.enabled=false",
        "salesbot.security.jwt-secret=" + IntegrationTest.JWT_SECRET,
        "salesbot.admin.email=" + IntegrationTest.ADMIN_EMAIL,
        "salesbot.admin.name=Test Owner",
        "salesbot.admin.password=" + IntegrationTest.ADMIN_PASSWORD
})
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {

    /** Test-only values; real ones live in backend/.env. */
    String JWT_SECRET = "test-secret-for-integration-tests-only-0123456789";
    String ADMIN_EMAIL = "owner@test.local";
    String ADMIN_PASSWORD = "correct-horse-battery";
}
