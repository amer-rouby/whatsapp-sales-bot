package com.salesbot.messaging.simulator;

import com.salesbot.messaging.SimulatorChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Collects what the shop's number sends to simulator chats (the bot's replies and the shop's own replies),
 * so the simulator can show them. Only messages sent from {@link #SIMULATOR_NUMBER} land here; real
 * customers never do.
 */
@Component
public class SimulatorOutbox {

    /** Fake business number used by every simulator chat. */
    public static final String SIMULATOR_NUMBER = SimulatorChannel.PHONE_NUMBER_ID;

    /** A chat nobody reads anymore stops collecting after this many messages. */
    private static final int MAX_QUEUED = 50;

    private final ConcurrentHashMap<String, BlockingQueue<String>> replies = new ConcurrentHashMap<>();

    public boolean accepts(String phoneNumberId) {
        return SIMULATOR_NUMBER.equals(phoneNumberId);
    }

    public void record(String customerId, String text) {
        queue(customerId).offer(text);
    }

    /** Waits for the next message to this chat. */
    Optional<String> await(String customerId, Duration timeout) throws InterruptedException {
        return Optional.ofNullable(queue(customerId).poll(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    /** Everything sent to this chat since the last call, without waiting. */
    List<String> drain(String customerId) {
        BlockingQueue<String> queue = replies.get(customerId);
        List<String> drained = new ArrayList<>();
        if (queue != null) {
            queue.drainTo(drained);
        }
        return drained;
    }

    private BlockingQueue<String> queue(String customerId) {
        return replies.computeIfAbsent(customerId, id -> new LinkedBlockingQueue<>(MAX_QUEUED));
    }
}
