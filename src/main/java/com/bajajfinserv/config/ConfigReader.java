package com.bajajfinserv.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    
    private static Properties properties = new Properties();
    
    static {
        try {
            // Load config.properties from test resources
            String configPath = "src/test/resources/config.properties";
            InputStream input = new FileInputStream(configPath);
            properties.load(input);
            input.close();
            System.out.println("✅ Configuration loaded successfully");
        } catch (IOException e) {
            System.err.println("❌ Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get property value by key
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get property value with default fallback
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
