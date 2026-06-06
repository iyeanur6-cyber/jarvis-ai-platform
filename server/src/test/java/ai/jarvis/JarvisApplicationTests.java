package ai.jarvis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jarvis.security.jwt.secret="
                + "test-secret-key-minimum-32-characters-long-enough",
        "spring.shell.interactive.enabled=false",
        "spring.ai.google.api-key=",
        "spring.profiles.active=test",
        // Use same port as CI service
        "spring.r2dbc.url="
                + "r2dbc:postgresql://localhost:5432/jarvis",
        "spring.datasource.url="
                + "jdbc:postgresql://localhost:5432/jarvis",
        "spring.flyway.url="
                + "jdbc:postgresql://localhost:5432/jarvis"
})
class JarvisApplicationTests {

    @Test
    void contextLoads() {
        // Verifies full Spring context starts
        // including pgvector, Redis, Flyway
    }
}