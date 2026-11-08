/**
 * What the dashboard alerts the shop about: new customer messages and new orders.
 * Reads {@code conversation} and {@code orders} through their public APIs; nothing depends on it.
 */
@ApplicationModule(displayName = "Notifications")
package com.salesbot.notifications;

import org.springframework.modulith.ApplicationModule;
