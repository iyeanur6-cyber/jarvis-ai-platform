package com.sujankim.jarvis.controller;

import com.sujankim.jarvis.tool.ComfyUIImageTool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gimp")
@CrossOrigin(origins = "*")
public class GimpIntegrationController {

    private final ComfyUIImageTool.RequestRunner imageRunner;

    // Direct invocation of the ComfyUI tool logic built earlier
    public GimpIntegrationController(ComfyUIImageTool comfyUiImageTool) {
        this.imageRunner = comfyUiImageTool.generateImage()::apply;
    }

    public record GimpRequest(String prompt, String negativePrompt) {}
    public record GimpResponse(String status, String filePath, String message) {}

    @PostMapping("/generate")
    public ResponseEntity<GimpResponse> generateLayerImage(@RequestBody GimpRequest request) {
        if (request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new GimpResponse("ERROR", null, "Prompt cannot be empty."));
        }

        var result = imageRunner.run(
                new ComfyUIImageTool.Request(request.prompt(), request.negativePrompt())
        );

        return ResponseEntity.ok(new GimpResponse(result.status(), result.filePath(), result.message()));
    }
}
