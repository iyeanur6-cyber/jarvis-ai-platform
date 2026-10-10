#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <unordered_set>
#include <regex>
#include <algorithm>

class NativeAdBlocker {
private:
    std::unordered_set<std::string> blocked_domains;
    std::vector<std::regex> path_patterns;

public:
    NativeAdBlocker() {
        // Essential tracker/telemetry signatures
        blocked_domains = {
            "google-analytics.com", "doubleclick.net", "adservice.google.com",
            "facebook.net", "connect.facebook.net", "scorecardresearch.com",
            "quantserve.com", "criteo.com", "outbrain.com", "taboola.com"
        };
    }

    void load_rules_file(const std::string& rule_path) {
        std::ifstream file(rule_path);
        std::string line;
        while (std::getline(file, line)) {
            if (line.empty() || line[0] == '!') continue; // Comments

            // Parse domain blocking rule: ||domain.com^
            if (line.rfind("||", 0) == 0) {
                size_t end_pos = line.find('^');
                if (end_pos != std::string::npos) {
                    blocked_domains.insert(line.substr(2, end_pos - 2));
                }
            }
        }
    }

    bool should_block(const std::string& url, const std::string& host) {
        // Fast O(1) domain lookup
        if (blocked_domains.find(host) != blocked_domains.end()) {
            return true;
        }

        // Subdomain resolution
        for (const auto& domain : blocked_domains) {
            if (host.length() > domain.length() &&
                host.rfind("." + domain) == (host.length() - domain.length() - 1)) {
                return true;
            }
        }
        return false;
    }
};

// C-linkage export for dynamic linking into Chromium wrappers or Jarvis JNI
extern "C" {
    NativeAdBlocker* engine_create() { return new NativeAdBlocker(); }
    void engine_load_rules(NativeAdBlocker* engine, const char* path) { engine->load_rules_file(path); }
    bool engine_should_block(NativeAdBlocker* engine, const char* url, const char* host) {
        return engine->should_block(url, host);
    }
    void engine_destroy(NativeAdBlocker* engine) { delete engine; }
}
