/**
 * Dashboard users and login (email + password → signed JWT).
 * Nothing in the bot pipeline depends on it; the security filter chain lives in {@code shared}.
 */
@ApplicationModule(displayName = "Identity")
package com.salesbot.identity;

import org.springframework.modulith.ApplicationModule;
