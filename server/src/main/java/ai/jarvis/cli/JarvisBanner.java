package ai.jarvis.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JarvisBanner {

    // Static so it survives DevTools restarts
    private static volatile boolean displayed = false;

    @EventListener(ApplicationReadyEvent.class)
    public void displayBanner() {
        // Only show once per JVM process
        if (displayed) return;
        displayed = true;

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        System.out.println(
                "\n" +
                        "+==============================================+\n" +
                        "|                                            |\n" +
                        "|       JARVIS AI PLATFORM v0.1.0            |\n" +
                        "|                                            |\n" +
                        "|  Local AI | Spring Boot 4 | Java 21        |\n" +
                        "|                                            |\n" +
                        "+--------------------------------------------+\n" +
                        "|  help      - all commands                  |\n" +
                        "|  login     - authenticate                  |\n" +
                        "|  chat      - talk to Jarvis                |\n" +
                        "|  status    - system health                 |\n" +
                        "+============================================+\n"
        );
    }
}