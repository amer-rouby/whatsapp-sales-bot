/**
 * Conversation history and the reply loop: inbound message -> store -> ask AI -> send.
 * Depends on {@code messaging} (events + sender), {@code ai}, {@code catalog} and {@code orders};
 * nothing depends on it yet.
 */
@ApplicationModule(displayName = "Conversation")
package com.salesbot.conversation;

import org.springframework.modulith.ApplicationModule;
