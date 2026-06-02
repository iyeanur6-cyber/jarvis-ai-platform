package ai.jarvis.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Displays the Jarvis banner on startup.
 *
 * WHY ApplicationRunner:
 * → Runs AFTER Spring context is ready
 * → Runs BEFORE Spring Shell takes the thread
 * → Correct lifecycle hook for pre-shell output
 *
 * WHY System.setProperty (not static boolean):
 * → Spring DevTools creates new ClassLoader on restart
 * → Static fields RESET with each ClassLoader
 * → System properties survive ClassLoader restarts
 * → JVM-level storage = truly shown only once
 */
@Slf4j
@Component
@Order(1)
public class JarvisBanner implements ApplicationRunner {

    private static final String BANNER_SHOWN_KEY =
            "jarvis.banner.shown";

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

        // System.getProperty survives DevTools
        // ClassLoader restarts — JVM level storage
        if (System.getProperty(BANNER_SHOWN_KEY)
                == null) {

            // Mark as shown at JVM level
            System.setProperty(
                    BANNER_SHOWN_KEY, "true");

            // Print banner
            System.out.println(BANNER);
        }
    }
}