package com.salesbot;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architecture guard: fails the build if a module reaches into another module's
 * internal packages or if module dependencies form a cycle.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BackendApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDiagrams() {
        new Documenter(modules).writeModulesAsPlantUml();
    }
}
