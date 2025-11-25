package com.bajajfinserv.utils;

import com.bajajfinserv.config.ConfigReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FigmaAPIClient {

    private static final String FIGMA_API_BASE = "https://api.figma.com/v1";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure RestAssured to accept all SSL certificates (for corporate networks)
        RestAssured.config = RestAssured.config().sslConfig(
                SSLConfig.sslConfig().relaxedHTTPSValidation()
        );
    }

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 1000L; // 1s, 2s, 4s, 8s, 16s...

    public static BufferedImage getFigmaComponentImage(String figmaUrl) {
        System.out.println("🔗 Figma URL: " + figmaUrl);

        FigmaNodeData nodeData = parseFigmaUrl(figmaUrl);
        if (nodeData == null) {
            throw new RuntimeException("❌ Invalid Figma URL format");
        }

        System.out.println("📁 File Key: " + nodeData.fileKey);
        System.out.println("🎯 Node ID: " + nodeData.nodeId);

        String apiToken = ConfigReader.getProperty("figma.apiToken");
        if (apiToken == null || apiToken.isEmpty()) {
            throw new RuntimeException("❌ Figma API token not found in config.properties");
        }

        // Disable SSL verification for image download too
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        int retries = 0;

        while (true) {
            try {
                // 1) Get image URL from Figma (this is where 429 usually happens)
                String imageUrl = getImageUrl(nodeData.fileKey, nodeData.nodeId, apiToken);
                if (imageUrl == null || imageUrl.isEmpty()) {
                    throw new RuntimeException("❌ Failed to get image URL from Figma API");
                }

                System.out.println("🖼️ Image URL obtained");

                // 2) Download image
                BufferedImage image = ImageIO.read(new URL(imageUrl));
                if (image == null) {
                    throw new IOException("❌ ImageIO.read returned null for downloaded image");
                }

                return image; // ✅ success

            } catch (Exception e) {
                // Check if this looks like a Figma rate-limit error (429)
                if (isRateLimitError(e) && retries < MAX_RETRIES) {
                    long waitMs = (long) Math.pow(2, retries) * BASE_BACKOFF_MS;
                    System.out.println("⚠️ Rate limit hit (429). Retry " + (retries + 1)
                                       + " of " + MAX_RETRIES + " – waiting " + (waitMs / 1000) + " seconds...");
                    retries++;

                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted while waiting after rate limit.", ie);
                    }

                    // loop again
                    continue;
                } else {

                    // Not a rate limit issue OR retries exhausted → fail fast
                    throw new RuntimeException("❌ Failed to download image from Figma", e);
                }
            }
        }
    }

    /**
     * Best-effort detection of Figma 429 "Rate limit exceeded" errors.
     * Works whether the error is coming from your HTTP layer or bubbled up as text.
     */
    private static boolean isRateLimitError(Exception e) {
        if (e == null) {
            return false;
        }
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }

        // Match common patterns from Figma:
        // e.g. "Response: {"status":429,"err":"Rate limit exceeded"}"
        return msg.contains("429")
               || msg.toLowerCase().contains("rate limit")
               || msg.toLowerCase().contains("rate-limit");
    }

    public static BufferedImage getFigmaComponentImage_dnd(String figmaUrl) {
        System.out.println("🔗 Figma URL: " + figmaUrl);

        FigmaNodeData nodeData = parseFigmaUrl(figmaUrl);
        if (nodeData == null) {
            throw new RuntimeException("❌ Invalid Figma URL format");
        }

        System.out.println("📁 File Key: " + nodeData.fileKey);
        System.out.println("🎯 Node ID: " + nodeData.nodeId);

        String apiToken = ConfigReader.getProperty("figma.apiToken");
        if (apiToken == null || apiToken.isEmpty()) {
            throw new RuntimeException("❌ Figma API token not found in config.properties");
        }

        String imageUrl = getImageUrl(nodeData.fileKey, nodeData.nodeId, apiToken);
        if (imageUrl == null) {
            throw new RuntimeException("❌ Failed to get image URL from Figma API");
        }

        System.out.println("🖼️ Image URL obtained");

        // Disable SSL verification for image download too
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        BufferedImage image = null;
        try {
            image = ImageIO.read(new URL(imageUrl));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (image == null) {
            throw new RuntimeException("❌ Failed to download image from Figma");
        }

        return image;
    }

    private static FigmaNodeData parseFigmaUrl(String figmaUrl) {
        Pattern pattern = Pattern.compile("figma\\.com/design/([^/]+)/[^?]*\\?node-id=([^&]+)");
        Matcher matcher = pattern.matcher(figmaUrl);

        if (matcher.find()) {
            String fileKey = matcher.group(1);
            String nodeId = matcher.group(2).replace("-", ":");
            return new FigmaNodeData(fileKey, nodeId);
        }

        pattern = Pattern.compile("figma\\.com/file/([^/]+)/[^?]*\\?node-id=([^&]+)");
        matcher = pattern.matcher(figmaUrl);

        if (matcher.find()) {
            String fileKey = matcher.group(1);
            String nodeId = matcher.group(2).replace("-", ":");
            return new FigmaNodeData(fileKey, nodeId);
        }

        return null;
    }

    private static String getImageUrl(String fileKey, String nodeId, String apiToken) {
        String endpoint = FIGMA_API_BASE + "/images/" + fileKey;

        Response response = RestAssured.given()
                .relaxedHTTPSValidation() // Additional SSL bypass
                .header("X-Figma-Token", apiToken)
                .queryParam("ids", nodeId)
                .queryParam("format", "png")
                .queryParam("scale", "2")
                .get(endpoint);

        System.out.println("📡 Figma API Response Code: " + response.getStatusCode());

        if (response.getStatusCode() != 200) {
            System.err.println("❌ Figma API error: " + response.getStatusCode());
            System.err.println("Response: " + response.getBody().asString());
            throw new RuntimeException("Figma API returned non-200 status: " + response.getStatusCode() + ". Body: " + response.getBody().asString());
        }

        JsonNode root = null;
        try {
            root = objectMapper.readTree(response.getBody().asString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        JsonNode imagesNode = root.get("images");

        if (imagesNode != null && imagesNode.has(nodeId)) {
            return imagesNode.get(nodeId).asText();
        }

        System.err.println("❌ Image URL not found in response");
        throw new RuntimeException("Image URL not found in Figma API response");
    }

    private static class FigmaNodeData {
        String fileKey;
        String nodeId;

        FigmaNodeData(String fileKey, String nodeId) {
            this.fileKey = fileKey;
            this.nodeId = nodeId;
        }
    }
}