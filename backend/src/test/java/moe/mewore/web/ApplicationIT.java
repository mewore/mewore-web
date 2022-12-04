package moe.mewore.web;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import moe.mewore.web.config.TestConfig;

@Import(TestConfig.class)
class ApplicationIT {

    @Test
    void testRun() {
        // The application has started successfully
    }
}
