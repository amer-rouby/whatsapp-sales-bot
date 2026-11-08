/**
 * Orders the bot closed with customers. Public API: {@link com.salesbot.orders.OrderPlacement},
 * {@link com.salesbot.orders.PlaceOrder} and {@link com.salesbot.orders.RecentOrders}; sub-packages are internal.
 * Prices come from {@code catalog}, never from the AI.
 */
@ApplicationModule(displayName = "Orders")
package com.salesbot.orders;

import org.springframework.modulith.ApplicationModule;
