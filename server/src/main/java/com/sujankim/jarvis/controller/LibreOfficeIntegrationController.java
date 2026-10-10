package com.sujankim.jarvis.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/libreoffice")
@CrossOrigin(origins = "*")
public class LibreOfficeIntegrationController {

    private final ChatClient chatClient;

    public LibreOfficeIntegrationController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public record OfficeRequest(String action, String text) {}
    public record OfficeResponse(String result) {}

    @PostMapping("/process")
    public ResponseEntity<OfficeResponse> processDocumentText(@RequestBody OfficeRequest req) {
        if (req.text() == null || req.text().isBlank()) {
            return ResponseEntity.badRequest().body(new OfficeResponse("No text provided"));
        }

        String promptPrefix = switch (req.action() != null ? req.action() : "rewrite") {
            case "summarize" -> "Summarize the following text clearly and concisely:\n\n";
            case "grammar" -> "Fix all grammar, punctuation, and spelling errors in the following text without altering its meaning:\n\n";
            case "expand" -> "Elaborate and provide detailed development on the following passage:\n\n";
            default -> "Rewrite and polish the following text for readability and professional tone:\n\n";
        };

        String output = chatClient.prompt()
                .user(promptPrefix + req.text())
                .call()
                .content();

        return ResponseEntity.ok(new OfficeResponse(output));
    }
}
