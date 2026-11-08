package com.salesbot.messaging.simulator;

import com.salesbot.messaging.BotAvailability;
import com.salesbot.messaging.InboundMessageReceived;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private static final String CUSTOMER_ID = "[A-Za-z0-9_-]{1,32}";

    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final SimulatorOutbox outbox;
    private final BotAvailability bot;

    SimulatorController(ApplicationEventPublisher events, TransactionTemplate tx, SimulatorOutbox outbox, BotAvailability bot) {
        this.events = events;
        this.tx = tx;
        this.outbox = outbox;
        this.bot = bot;
    }

    /**
     * Sends the customer's message and waits for the answer. When the shop has taken the chat over the bot
     * stays silent, so this returns at once with no reply; the shop's own answer arrives through {@link #pending}.
     */
    @PostMapping("/messages")
    ResponseEntity<SimulatorReply> send(@Valid @RequestBody SimulatorMessage message) throws InterruptedException {
        boolean botAnswers = bot.botAnswers(SimulatorOutbox.SIMULATOR_NUMBER, message.customerId());

        // Published inside a transaction, exactly like the real webhook, so listeners fire after commit
        tx.executeWithoutResult(status -> events.publishEvent(new InboundMessageReceived(
                "sim." + UUID.randomUUID(),
                SimulatorOutbox.SIMULATOR_NUMBER,
                message.customerId(),
                message.customerName(),
                message.text(),
                Instant.now())));

        if (!botAnswers) {
            return ResponseEntity.ok(new SimulatorReply(null));
        }
        return outbox.await(message.customerId(), REPLY_TIMEOUT)
                .map(reply -> ResponseEntity.ok(new SimulatorReply(reply)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build());
    }

    /** Messages the shop sent to this chat that the simulator hasn't shown yet (e.g. replies from the dashboard). */
    @GetMapping("/messages/{customerId}")
    PendingReplies pending(@PathVariable @Pattern(regexp = CUSTOMER_ID) String customerId) {
        return new PendingReplies(outbox.drain(customerId));
    }

    record SimulatorMessage(
            @NotBlank @Pattern(regexp = CUSTOMER_ID) String customerId,
            @Size(max = 100) String customerName,
            @NotBlank @Size(max = 2000) String text) {
    }

    /** {@code reply} is null when the bot is paused in this chat. */
    record SimulatorReply(String reply) {
    }

    record PendingReplies(List<String> replies) {
    }
}
