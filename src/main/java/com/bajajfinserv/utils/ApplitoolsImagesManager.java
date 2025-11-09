package com.bajajfinserv.utils;

import com.applitools.eyes.images.Eyes;
import com.bajajfinserv.config.ConfigReader;

public class ApplitoolsImagesManager {
    
    private static Eyes eyes;
    
    public static Eyes getEyes() {
        if (eyes == null) {
            eyes = new Eyes();
            
            String batchName = ConfigReader.getProperty("applitools.batchName", "Figma Visual Tests");
            String apiKey = ConfigReader.getProperty("applitools.apiKey");
            String serverUrl = ConfigReader.getProperty("applitools.serverUrl");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                eyes.setApiKey(apiKey);
            }
            
            if (serverUrl != null && !serverUrl.isEmpty()) {
                eyes.setServerUrl(serverUrl);
            }
            
            eyes.setBatch(new com.applitools.eyes.BatchInfo(batchName));
        }
        
        return eyes;
    }
    
    public static void closeEyes() {
        if (eyes != null) {
            eyes.close();
            eyes = null;
        }
    }
}