package com.salesbot.messaging.simulator;

import com.salesbot.messaging.InboundMessageReceived;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat with the bot without WhatsApp (dev/demo only, {@code salesbot.simulator.enabled=true}).
 * The message goes through exactly the same pipeline as a real webhook message;
 * only the final "send" is captured instead of hitting WhatsApp.
 */
@RestController
@RequestMapping("/dev/simulator")
@ConditionalOnProperty(name = "salesbot.simulator.enabled", havingValue = "true")
class SimulatorController {

    /** Must exceed the AI timeout so slow replies still come back. */
    private static final Duration REPLY_TIMEOUT = Duration.ofSeconds(60);

    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final SimulatorOutbox outbox;

    SimulatorController(ApplicationEventPublisher events, TransactionTemplate tx, SimulatorOutbox outbox) {
        this.events = events;
        this.tx = tx;
        this.outbox = outbox;
    }

    @PostMapping("/messages")
    ResponseEntity<SimulatorReply> send(@Valid @RequestBody SimulatorMessage message) throws InterruptedException {
        outbox.clear(message.customerId());

        // Published inside a transaction, exactly like the real webhook, so listeners fire after commit
        tx.executeWithoutResult(status -> events.publishEvent(new InboundMessageReceived(
                "sim." + UUID.randomUUID(),
                SimulatorOutbox.SIMULATOR_NUMBER,
                message.customerId(),
                message.customerName(),
                message.text(),
                Instant.now())));

        return outbox.await(message.customerId(), REPLY_TIMEOUT)
                .map(reply -> ResponseEntity.ok(new SimulatorReply(reply)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build());
    }

    record SimulatorMessage(
            @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9_-]+") String customerId,
            @Size(max = 100) String customerName,
            @NotBlank @Size(max = 2000) String text) {
    }

    record SimulatorReply(String reply) {
    }
}
