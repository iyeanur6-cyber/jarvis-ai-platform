package ai.jarvis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Provide JWT secret for test context
        "jarvis.security.jwt.secret="
                + "test-secret-key-minimum-32-characters-long-enough",
        // Empty Gemini key = GeminiProvider marks itself unavailable
        "spring.ai.google.api-key=",
        // Use a test DB URL if needed
        "spring.flyway.enabled=true"
})
class JarvisApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts without errors
        // All beans wire correctly
        // Security, R2DBC, Flyway all initialize
    }
}