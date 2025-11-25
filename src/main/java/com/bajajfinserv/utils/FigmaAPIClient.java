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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // Retry config
    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 1000L; // 1s, 2s, 4s, 8s, 16s...
    private static final int MAX_RETRY_AFTER_SECONDS = 120; // hard cap for Retry-After usage

    // Simple global throttle so we don't hammer Figma too fast
    private static final Object RATE_LIMIT_LOCK = new Object();
    private static long nextAllowedTimeMs = 0L;
    private static final long MIN_INTERVAL_BETWEEN_CALLS_MS = 1200L; // ~1.2s => ~50 calls/min

    // Cache for (fileKey|nodeId) -> image URL
    private static final Map<String, String> IMAGE_URL_CACHE = new ConcurrentHashMap<>();

    // --- Public API (with retry & backoff) ---

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
                String imageUrl = getImageUrl(nodeData.fileKey, nodeData.nodeId, apiToken);
                if (imageUrl == null || imageUrl.isEmpty()) {
                    throw new RuntimeException("❌ Failed to get image URL from Figma API");
                }

                System.out.println("🖼️ Image URL obtained: " + imageUrl);

                BufferedImage image = ImageIO.read(new URL(imageUrl));
                if (image == null) {
                    throw new IOException("❌ ImageIO.read returned null for downloaded image");
                }

                return image; // ✅ success

            } catch (FigmaRateLimitException e) {
                if (retries >= MAX_RETRIES) {
                    throw new RuntimeException("❌ Failed due to repeated Figma rate limit errors.", e);
                }

                retries++;

                long waitMs;

                int retryAfterSeconds = e.getRetryAfterSeconds();
                // 👇 Only trust Retry-After if it's >0 AND reasonably small (e.g. <= 120 seconds)
                if (retryAfterSeconds > 0 && retryAfterSeconds <= MAX_RETRY_AFTER_SECONDS) {
                    waitMs = retryAfterSeconds * 1000L;
                    System.out.println("⚠️ Rate limit hit. Using Retry-After header: "
                                       + retryAfterSeconds + "s");
                } else {
                    // Fallback to exponential backoff if header is missing or looks insane
                    waitMs = (long) Math.pow(2, retries - 1) * BASE_BACKOFF_MS;
                    System.out.println("⚠️ Rate limit hit. Ignoring Retry-After ("
                                       + retryAfterSeconds + "s). Using exponential backoff.");
                }

                System.out.println("⏳ Retry " + retries + " of " + MAX_RETRIES
                                   + " – waiting " + (waitMs / 1000) + " seconds...");

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted while waiting after rate limit.", ie);
                }

            } catch (Exception e) {
                throw new RuntimeException("❌ Failed to download image from Figma", e);
            }
        }
    }

    // --- Old variant without retry (unchanged, if you still want it) ---

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

        System.out.println("🖼️ Image URL obtained: " + imageUrl);

        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        BufferedImage image;
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

    // --- Core helpers ---

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

    /**
     * Get image URL for a single node.
     * Includes:
     *  - global throttling
     *  - caching
     *  - explicit 429 handling with optional Retry-After
     */
    private static String getImageUrl(String fileKey, String nodeId, String apiToken) {
        String cacheKey = fileKey + "|" + nodeId;
        String cachedUrl = IMAGE_URL_CACHE.get(cacheKey);
        if (cachedUrl != null && !cachedUrl.isEmpty()) {
            System.out.println("💾 Using cached image URL");
            return cachedUrl;
        }

        throttleFigmaCall();

        String endpoint = FIGMA_API_BASE + "/images/" + fileKey;

        Response response = RestAssured.given()
                .relaxedHTTPSValidation()
                .header("X-Figma-Token", apiToken)
                .queryParam("ids", nodeId)
                .queryParam("format", "png")
                .queryParam("scale", "2")
                .get(endpoint);

        int statusCode = response.getStatusCode();
        System.out.println("📡 Figma API Response Code: " + statusCode);

        if (statusCode == 429) {
            String retryAfterHeader = response.getHeader("Retry-After");
            int retryAfterSeconds = 0;
            try {
                if (retryAfterHeader != null) {
                    retryAfterSeconds = Integer.parseInt(retryAfterHeader.trim());
                }
            } catch (NumberFormatException ignored) {
                // If it's a date or something weird, we'll just leave it as 0
            }

            System.err.println("❌ Figma rate limit hit. Raw Retry-After header: " + retryAfterHeader);
            throw new FigmaRateLimitException("Figma rate limit exceeded", retryAfterSeconds);
        }

        if (statusCode != 200) {
            String body = response.getBody().asString();
            System.err.println("❌ Figma API error: " + statusCode);
            System.err.println("Response: " + body);
            throw new RuntimeException("Figma API returned non-200 status: " + statusCode + ". Body: " + body);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody().asString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode imagesNode = root.get("images");
        if (imagesNode != null && imagesNode.has(nodeId)) {
            String url = imagesNode.get(nodeId).asText();
            if (url != null && !url.isEmpty()) {
                IMAGE_URL_CACHE.put(cacheKey, url);
            }
            return url;
        }

        System.err.println("❌ Image URL not found in response");
        throw new RuntimeException("Image URL not found in Figma API response");
    }

    /**
     * Simple global throttle: ensures at least MIN_INTERVAL_BETWEEN_CALLS_MS
     * between Figma API calls across all threads.
     */
    private static void throttleFigmaCall() {
        synchronized (RATE_LIMIT_LOCK) {
            long now = System.currentTimeMillis();
            if (now < nextAllowedTimeMs) {
                long sleepMs = nextAllowedTimeMs - now;
                try {
                    System.out.println("⏳ Throttling Figma call, sleeping for " + sleepMs + " ms");
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            nextAllowedTimeMs = System.currentTimeMillis() + MIN_INTERVAL_BETWEEN_CALLS_MS;
        }
    }

    // --- Helper types ---

    private static class FigmaNodeData {
        String fileKey;
        String nodeId;

        FigmaNodeData(String fileKey, String nodeId) {
            this.fileKey = fileKey;
            this.nodeId = nodeId;
        }
    }

    public static class FigmaRateLimitException extends RuntimeException {
        private final int retryAfterSeconds;

        public FigmaRateLimitException(String message, int retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
