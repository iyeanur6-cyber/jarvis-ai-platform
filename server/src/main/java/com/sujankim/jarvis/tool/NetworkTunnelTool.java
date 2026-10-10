package com.sujankim.jarvis.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class NetworkTunnelTool {

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("Action: 'vpn_on', 'vpn_off', 'tor_on', 'tor_off', or 'launch_browser'")
            String action
    ) {}

    public record Response(String status, String message) {}

    @Bean
    @Description("Manages network privacy tunnels (VPN toggle, Tor routing) and launches the hardened Chromium browser")
    public Function<Request, Response> manageNetworkPrivacy() {
        return req -> {
            try {
                String cmd;
                switch (req.action()) {
                    case "vpn_on" -> cmd = "sudo wg-quick up wg0";
                    case "vpn_off" -> cmd = "sudo wg-quick down wg0";
                    case "tor_on" -> cmd = "sudo systemctl start tor";
                    case "tor_off" -> cmd = "sudo systemctl stop tor";
                    case "launch_browser" -> cmd = "bash integrations/browser/jarvis-browser.sh &";
                    default -> {
                        return new Response("FAILED", "Unknown network action: " + req.action());
                    }
                }
                Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
                return new Response("SUCCESS", "Executed action: " + req.action());
            } catch (Exception e) {
                return new Response("ERROR", e.getMessage());
            }
        };
    }
}
