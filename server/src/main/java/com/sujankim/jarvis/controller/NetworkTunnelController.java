package com.sujankim.jarvis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/network")
@CrossOrigin(origins = "*")
public class NetworkTunnelController {

    private boolean vpnActive = false;
    private boolean torActive = false;

    public record TunnelStatus(boolean vpnActive, boolean torActive, String ip) {}

    @GetMapping("/status")
    public ResponseEntity<TunnelStatus> getStatus() {
        return ResponseEntity.ok(new TunnelStatus(vpnActive, torActive, getCurrentExitIp()));
    }

    @PostMapping("/vpn/toggle")
    public ResponseEntity<Map<String, Object>> toggleVpn(@RequestParam boolean enable) {
        try {
            // Using WireGuard (wg0) or a local OpenVPN configuration
            String command = enable ? "sudo wg-quick up wg0" : "sudo wg-quick down wg0";
            Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            int exitCode = proc.waitFor();

            if (exitCode == 0 || !enable) {
                this.vpnActive = enable;
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "vpnActive", this.vpnActive,
                        "message", enable ? "VPN Connected" : "VPN Disconnected"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to toggle WireGuard interface"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/tor/toggle")
    public ResponseEntity<Map<String, Object>> toggleTor(@RequestParam boolean enable) {
        try {
            String command = enable ? "sudo systemctl start tor" : "sudo systemctl stop tor";
            Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            proc.waitFor();
            this.torActive = enable;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "torActive", this.torActive,
                    "message", enable ? "Tor SOCKS5 listening on 127.0.0.1:9050" : "Tor stopped"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private String getCurrentExitIp() {
        try {
            Process p = Runtime.getRuntime().exec("curl -s --max-time 2 https://api.ipify.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return in.readLine();
        } catch (Exception e) {
            return "Unavailable (Offline)";
        }
    }
}
