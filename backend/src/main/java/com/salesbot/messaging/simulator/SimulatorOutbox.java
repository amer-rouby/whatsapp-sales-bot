package com.salesbot.messaging.simulator;

import com.salesbot.messaging.OutboundImage;
import com.salesbot.messaging.SimulatorChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Collects what the shop's number sends to simulator chats (the bot's replies, its product photos and the shop's
 * own replies), so the simulator can show them. Only messages sent from {@link #SIMULATOR_NUMBER} land here; real
 * customers never do.
 */
@Component
public class SimulatorOutbox {

    /** Fake business number used by every simulator chat. */
    public static final String SIMULATOR_NUMBER = SimulatorChannel.PHONE_NUMBER_ID;

    /** A chat nobody reads anymore stops collecting after this many messages. */
    private static final int MAX_QUEUED = 50;

    private final ConcurrentHashMap<String, BlockingQueue<Sent>> replies = new ConcurrentHashMap<>();

    public boolean accepts(String phoneNumberId) {
        return SIMULATOR_NUMBER.equals(phoneNumberId);
    }

    public void record(String customerId, String text) {
        queue(customerId).offer(new Sent(text, null));
    }

    /** The photo travels as a data URL: the simulator is a dev tool and this keeps it free of catalog details. */
    public void recordImage(String customerId, OutboundImage image) {
        String dataUrl = "data:" + image.contentType() + ";base64," + Base64.getEncoder().encodeToString(image.data());
        queue(customerId).offer(new Sent(image.caption() == null ? "" : image.caption(), dataUrl));
    }

    /** Waits for the next message to this chat. */
    Optional<Sent> await(String customerId, Duration timeout) throws InterruptedException {
        return Optional.ofNullable(queue(customerId).poll(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    /** Everything sent to this chat since the last call, without waiting. */
    List<Sent> drain(String customerId) {
        BlockingQueue<Sent> queue = replies.get(customerId);
        List<Sent> drained = new ArrayList<>();
        if (queue != null) {
            queue.drainTo(drained);
        }
        return drained;
    }

    private BlockingQueue<Sent> queue(String customerId) {
        return replies.computeIfAbsent(customerId, id -> new LinkedBlockingQueue<>(MAX_QUEUED));
    }

    /** One message to the customer: text, or a photo (data URL) with the text as its caption. */
    record Sent(String text, String imageUrl) {
    }
}
