package com.salesbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Starts the app wired the way it runs for real customers (Gemini + WhatsApp Cloud API) with fake
 * credentials. Nothing is called; this only proves every bean of that mode can be created —
 * the echo/log setup used by the other tests would not notice a missing one.
 */
@SpringBootTest(properties = {
        "salesbot.ai.provider=gemini",
        "salesbot.ai.gemini.api-key=fake-key",
        "salesbot.whatsapp.sender=cloud",
        "salesbot.whatsapp.access-token=fake-token",
        "salesbot.whatsapp.app-secret=fake-secret",
        "salesbot.simulator.enabled=false",
        "salesbot.security.jwt-secret=" + IntegrationTest.JWT_SECRET
})
@Import(TestcontainersConfiguration.class)
class ProductionModeContextTest {

    @Test
    void contextLoadsWithRealProviders() {
    }
}
