/**
 * Generates the bot's replies. Public API: {@link com.salesbot.ai.AiReplyProvider}.
 * Providers (echo, gemini, ...) live in sub-packages and are picked by
 * {@code salesbot.ai.provider}, so swapping the model never touches callers.
 */
@ApplicationModule(displayName = "AI")
package com.salesbot.ai;

import org.springframework.modulith.ApplicationModule;
