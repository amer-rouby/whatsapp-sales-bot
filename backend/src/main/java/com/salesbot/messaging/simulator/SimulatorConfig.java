package com.salesbot.messaging.simulator;

/*
 * Intentionally empty: the simulator now works with both senders. Every sender hands simulator
 * replies (phoneNumberId = SIMULATOR) to SimulatorOutbox instead of WhatsApp, so the dashboard
 * chat and real WhatsApp can run side by side. CORS lives in shared WebCorsConfig.
 */
