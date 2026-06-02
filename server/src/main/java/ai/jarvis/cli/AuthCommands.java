package ai.jarvis.cli;

import ai.jarvis.security.auth.request.LoginRequest;
import ai.jarvis.security.auth.response.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthCommands {

    private final CliStateManager state;
    private final CliHttpClient http;
    private final LineReader lineReader;

    /**
     * Single constructor — Spring Framework 7
     * automatically uses this for DI.
     *
     * @Lazy on LineReader breaks the circular
     * dependency with CommandRegistry:
     * authCommands → lineReader → commandCompleter
     * → commandRegistry → authCommands
     */
    public AuthCommands(
            CliStateManager state,
            CliHttpClient http,
            @Lazy LineReader lineReader) {
        this.state = state;
        this.http = http;
        this.lineReader = lineReader;
    }

    @Command(
            name = "login",
            description = "Login to Jarvis"
    )
    public String login() {

        if (state.isLoggedIn()) {
            return "Already logged in as: "
                    + state.getUsername()
                    + ". Type 'logout' first.";
        }

        if (!http.isServerReachable()) {
            return "Cannot reach server. "
                    + "Is Jarvis running?";
        }

        String username = lineReader.readLine(
                "Username: ");
        String password = lineReader.readLine(
                "Password: ", '*');

        try {
            TokenResponse response = http.post(
                    "/api/v1/auth/login",
                    new LoginRequest(
                            username.trim(),
                            password.trim()),
                    TokenResponse.class);

            if (response != null
                    && response.user() != null) {
                state.setAccessToken(
                        response.accessToken());
                state.setUsername(
                        response.user().username());
                state.setRole(
                        response.user().role().name());
                state.setUserId(
                        response.user().userId());

                return "Welcome back, "
                        + response.user().displayName()
                        + "! ("
                        + response.user().role().name()
                        + ")";
            }
            return "Login failed";

        } catch (Exception e) {
            return "Login failed: "
                    + extractMessage(e);
        }
    }

    @Command(
            name = "logout",
            description = "Logout from Jarvis"
    )
    public String logout() {
        if (!state.isLoggedIn()) {
            return "Not logged in.";
        }
        String name = state.getUsername();
        state.clear();
        return "Goodbye, " + name + "!";
    }

    @Command(
            name = "whoami",
            description = "Show current user info"
    )
    public String whoami() {
        if (!state.isLoggedIn()) {
            return "Not logged in. Type: login";
        }
        return "\nCurrent User\n"
                + "--------------------------\n"
                + "Username: "
                + state.getUsername() + "\n"
                + "Role:     "
                + state.getRole() + "\n"
                + "ID:       "
                + state.getUserId().toString()
                .substring(0, 8) + "...\n";
    }

    private String extractMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("401")) {
            return "Invalid username or password";
        }
        if (msg != null
                && msg.contains("Connection refused")) {
            return "Server not running on port 8080";
        }
        return msg != null ? msg : "Unknown error";
    }
}