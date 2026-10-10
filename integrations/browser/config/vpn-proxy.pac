function FindProxyForURL(url, host) {
    // Route any hidden service directly through local Tor SOCKS5
    if (shExpMatch(host, "*.onion")) {
        return "SOCKS5 127.0.0.1:9050; SOCKS 127.0.0.1:9050";
    }

    // Default to direct (or through the active system WireGuard/VPN interface)
    return "DIRECT";
}
