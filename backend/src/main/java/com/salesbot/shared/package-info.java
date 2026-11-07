/**
 * Cross-cutting building blocks (persistence base types, error handling, config).
 * Open module: every other module may use it, it must never depend on them.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.salesbot.shared;

import org.springframework.modulith.ApplicationModule;
