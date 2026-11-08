package com.salesbot.messaging.simulator;

import com.salesbot.messaging.SimulatorChannel;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Collects the bot's replies to simulator chats so the simulator endpoint can return them.
 * Only messages sent from {@link #SIMULATOR_NUMBER} land here; real customers never do.
 */
@Component
public class SimulatorOutbox {

    /** Fake business number used by every simulator chat. */
    public static final String SIMULATOR_NUMBER = SimulatorChannel.PHONE_NUMBER_ID;

    private final ConcurrentHashMap<String, BlockingQueue<String>> replies = new ConcurrentHashMap<>();

    public boolean accepts(String phoneNumberId) {
        return SIMULATOR_NUMBER.equals(phoneNumberId);
    }

    public void record(String customerId, String text) {
        queue(customerId).add(text);
    }

    void clear(String customerId) {
        queue(customerId).clear();
    }

    Optional<String> await(String customerId, Duration timeout) throws InterruptedException {
        String reply = queue(customerId).poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (reply != null) {
            replies.remove(customerId); // nothing else is waiting on this chat
        }
        return Optional.ofNullable(reply);
    }

    private BlockingQueue<String> queue(String customerId) {
        return replies.computeIfAbsent(customerId, id -> new LinkedBlockingQueue<>());
    }
}
