package com.salesbot.messaging.infrastructure;

import com.salesbot.messaging.OutboundMessageSender;
import com.salesbot.messaging.simulator.SimulatorOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Local-dev sender: prints the reply instead of calling WhatsApp, and feeds the simulator. */
@Component
@ConditionalOnProperty(name = "salesbot.whatsapp.sender", havingValue = "log", matchIfMissing = true)
class LoggingMessageSender implements OutboundMessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageSender.class);

    private final SimulatorOutbox simulatorOutbox;

    LoggingMessageSender(SimulatorOutbox simulatorOutbox) {
        this.simulatorOutbox = simulatorOutbox;
    }

    @Override
    public void sendText(String phoneNumberId, String to, String text) {
        log.info("[WHATSAPP-OUT] from={} to={} text={}", phoneNumberId, to, text);
        if (simulatorOutbox.accepts(phoneNumberId)) {
            simulatorOutbox.record(to, text);
        }
    }
}
