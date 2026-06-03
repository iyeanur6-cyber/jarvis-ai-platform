package ai.jarvis.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class JarvisBanner implements ApplicationRunner {

    private static final String BANNER_SHOWN_KEY =
            "jarvis.banner.shown";

    private final Environment environment;

    public JarvisBanner(Environment environment) {
        this.environment = environment;
    }

    private static final String BANNER = """
            
            +==============================================+
            |                                            |
            |       JARVIS AI PLATFORM v0.1.0            |
            |                                            |
            |  Local AI | Spring Boot 4 | Java 21        |
            |                                            |
            +--------------------------------------------+
            |  help      - all commands                  |
            |  login     - authenticate                  |
            |  chat      - talk to Jarvis                |
            |  status    - system health                 |
            +============================================+
            """;

    @Override
    public void run(ApplicationArguments args) {

        // Do NOT show banner during tests
        // Tests set spring.shell.interactive.enabled=false
        // and do not need the banner
        String interactive = environment.getProperty(
                "spring.shell.interactive.enabled",
                "true");
        if ("false".equals(interactive)) {
            return;
        }

        // Only show once per JVM process
        if (System.getProperty(BANNER_SHOWN_KEY)
                == null) {
            System.setProperty(
                    BANNER_SHOWN_KEY, "true");
            System.out.println(BANNER);
        }
    }
}