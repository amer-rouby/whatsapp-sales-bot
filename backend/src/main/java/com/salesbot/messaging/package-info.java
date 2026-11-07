/**
 * WhatsApp channel: receives webhooks and sends messages.
 * Public API (this package): {@link com.salesbot.messaging.InboundMessageReceived}
 * and {@link com.salesbot.messaging.OutboundMessageSender}. Sub-packages are internal.
 * Knows nothing about conversations or AI — it only moves messages in and out.
 */
@ApplicationModule(displayName = "Messaging")
package com.salesbot.messaging;

import org.springframework.modulith.ApplicationModule;
