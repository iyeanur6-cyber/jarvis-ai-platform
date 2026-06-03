package ai.jarvis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Provide required secrets
        "jarvis.security.jwt.secret="
                + "test-secret-key-minimum-32-characters-long-enough",

        // Disable Spring Shell interactive mode in tests
        // Without this: Shell tries to start terminal
        // in CI where there is no terminal → canceled
        "spring.shell.interactive.enabled=false",

        // Disable the banner in tests (cleaner output)
        "spring.main.banner-mode=off",

        // Empty Gemini key = GeminiProvider marks unavailable
        "spring.ai.google.api-key=",

        // Use test profile
        "spring.profiles.active=test"
})
class JarvisApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts without errors.
        // All beans wire correctly.
        // Security, R2DBC, Flyway all initialize.
        // Spring Shell does NOT start interactive mode.
    }
}