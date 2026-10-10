package com.sujankim.jarvis.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.function.Function;

@Configuration
public class ComfyUIImageTool {

    private static final Logger log = LoggerFactory.getLogger(ComfyUIImageTool.class);
    private static final String COMFYUI_URL = "http://127.0.0.1:8188";

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Detailed text prompt describing the image to generate")
        String prompt,

        @JsonProperty(required = false)
        @JsonPropertyDescription("Negative prompt specifying what to avoid (e.g. blurry, low quality)")
        String negativePrompt
    ) {}

    public record Response(String status, String filePath, String message) {}

    @Bean
    @Description("Generate an image using the local ComfyUI Stable Diffusion backend and return the local file path")
    public Function<Request, Response> generateImage() {
        return request -> {
            try {
                RestClient restClient = RestClient.builder().baseUrl(COMFYUI_URL).build();
                ObjectMapper mapper = new ObjectMapper();

                long seed = Math.abs(new Random().nextLong() % 1_000_000_000L);
                String neg = (request.negativePrompt() != null && !request.negativePrompt().isBlank())
                        ? request.negativePrompt()
                        : "blurry, low quality, distorted, extra limbs, bad anatomy";

                // Build ComfyUI node graph JSON
                String workflowJson = String.format("""
                {
                  "prompt": {
                    "3": {
                      "class_type": "KSampler",
                      "inputs": {
                        "cfg": 7,
                        "denoise": 1,
                        "latent_image": ["5", 0],
                        "model": ["4", 0],
                        "negative": ["7", 0],
                        "positive": ["6", 0],
                        "sampler_name": "euler_ancestral",
                        "scheduler": "normal",
                        "seed": %d,
                        "steps": 20
                      }
                    },
                    "4": {
                      "class_type": "CheckpointLoaderSimple",
                      "inputs": {
                        "ckpt_name": "v1-5-pruned-emaonly.safetensors"
                      }
                    },
                    "5": {
                      "class_type": "EmptyLatentImage",
                      "inputs": {
                        "batch_size": 1,
                        "height": 512,
                        "width": 512
                      }
                    },
                    "6": {
                      "class_type": "CLIPTextEncode",
                      "inputs": {
                        "clip": ["4", 1],
                        "text": %s
                      }
                    },
                    "7": {
                      "class_type": "CLIPTextEncode",
                      "inputs": {
                        "clip": ["4", 1],
                        "text": %s
                      }
                    },
                    "8": {
                      "class_type": "VAEDecode",
                      "inputs": {
                        "samples": ["3", 0],
                        "vae": ["4", 2]
                      }
                    },
                    "9": {
                      "class_type": "SaveImage",
                      "inputs": {
                        "filename_prefix": "Jarvis_GIMP",
                        "images": ["8", 0]
                      }
                    }
                  }
                }
                """, seed, mapper.writeValueAsString(request.prompt()), mapper.writeValueAsString(neg));

                // 1. Submit prompt to ComfyUI
                String postResponse = restClient.post()
                        .uri("/prompt")
                        .header("Content-Type", "application/json")
                        .body(workflowJson)
                        .retrieve()
                        .body(String.class);

                JsonNode root = mapper.readTree(postResponse);
                String promptId = root.path("prompt_id").asText();
                log.info("ComfyUI job queued with prompt ID: {}", promptId);

                // 2. Poll /history/{prompt_id} until generation completes
                String filename = null;
                String subfolder = "";
                int attempts = 0;

                while (attempts < 60) {
                    Thread.sleep(1000);
                    attempts++;

                    String historyResp = restClient.get()
                            .uri("/history/" + promptId)
                            .retrieve()
                            .body(String.class);

                    JsonNode historyNode = mapper.readTree(historyResp);
                    if (historyNode.has(promptId)) {
                        JsonNode outputs = historyNode.path(promptId).path("outputs").path("9").path("images");
                        if (outputs.isArray() && !outputs.isEmpty()) {
                            filename = outputs.get(0).path("filename").asText();
                            subfolder = outputs.get(0).path("subfolder").asText();
                            break;
                        }
                    }
                }

                if (filename == null) {
                    return new Response("FAILED", null, "Image generation timed out after 60 seconds.");
                }

                // 3. Download the generated image to a local workspace folder
                String viewUrl = COMFYUI_URL + "/view?filename=" + filename + "&subfolder=" + subfolder + "&type=output";
                Path localOutputDir = Path.of(System.getProperty("user.home"), "jarvis_generated_images");
                Files.createDirectories(localOutputDir);
                Path localFile = localOutputDir.resolve(filename);

                try (InputStream in = URI.create(viewUrl).toURL().openStream()) {
                    Files.copy(in, localFile, StandardCopyOption.REPLACE_EXISTING);
                }

                log.info("Image successfully saved to: {}", localFile.toAbsolutePath());
                return new Response(
                    "SUCCESS",
                    localFile.toAbsolutePath().toString(),
                    "Image successfully created at: " + localFile.toAbsolutePath()
                );

            } catch (Exception e) {
                log.error("ComfyUI tool error", e);
                return new Response("ERROR", null, "Failed to generate image: " + e.getMessage());
            }
        };
    }
}
