package com.salesbot.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Lets {@code @ApplicationModuleListener} handlers run off the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
