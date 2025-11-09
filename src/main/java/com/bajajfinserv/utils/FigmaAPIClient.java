package com.bajajfinserv.utils;

import com.bajajfinserv.config.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    
    public static BufferedImage getFigmaComponentImage(String figmaUrl) throws Exception {
        System.out.println("🔗 Figma URL: " + figmaUrl);
        
        FigmaNodeData nodeData = parseFigmaUrl(figmaUrl);
        if (nodeData == null) {
            throw new Exception("❌ Invalid Figma URL format");
        }
        
        System.out.println("📁 File Key: " + nodeData.fileKey);
        System.out.println("🎯 Node ID: " + nodeData.nodeId);
        
        String apiToken = ConfigReader.getProperty("figma.apiToken");
        if (apiToken == null || apiToken.isEmpty()) {
            throw new Exception("❌ Figma API token not found in config.properties");
        }
        
        String imageUrl = getImageUrl(nodeData.fileKey, nodeData.nodeId, apiToken);
        if (imageUrl == null) {
            throw new Exception("❌ Failed to get image URL from Figma API");
        }
        
        System.out.println("🖼️ Image URL obtained");
        
        // Disable SSL verification for image download too
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        
        BufferedImage image = ImageIO.read(new URL(imageUrl));
        if (image == null) {
            throw new Exception("❌ Failed to download image from Figma");
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
        try {
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
                return null;
            }
            
            JsonNode root = objectMapper.readTree(response.getBody().asString());
            JsonNode imagesNode = root.get("images");
            
            if (imagesNode != null && imagesNode.has(nodeId)) {
                return imagesNode.get(nodeId).asText();
            }
            
            System.err.println("❌ Image URL not found in response");
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error calling Figma API: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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