package com.bajajfinserv.utils;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.bajajfinserv.config.ConfigReader;

public class ApplitoolsManager {
    
    private static BatchInfo sharedBatch;
    private static VisualGridRunner runner;
    
    // For Selenium Eyes (Website screenshots)
    public static Eyes getEyes(String matchLevel, String uploadBaseline, BatchInfo batch) {
        Eyes eyes = new Eyes(getRunner());
        Configuration config = new Configuration();
        
        config.setApiKey(ConfigReader.getProperty("applitools.api.key"));
        config.setServerUrl(ConfigReader.getProperty("applitools.server.url"));
        config.setBatch(batch);
        
        if ("TRUE".equalsIgnoreCase(uploadBaseline)) {
            config.setBaselineBranchName("figma");
        }
        
        switch (matchLevel.toUpperCase()) {
            case "STRICT": config.setMatchLevel(MatchLevel.STRICT); break;
            case "CONTENT": config.setMatchLevel(MatchLevel.CONTENT); break;
            case "LAYOUT": config.setMatchLevel(MatchLevel.LAYOUT); break;
            default: config.setMatchLevel(MatchLevel.STRICT);
        }
        
        eyes.setConfiguration(config);
        return eyes;
    }
    
    // ⭐ NEW: For Images Eyes (Figma image upload)
    public static com.applitools.eyes.images.Eyes getImagesEyes(String matchLevel, BatchInfo batch) {
        com.applitools.eyes.images.Eyes imagesEyes = new com.applitools.eyes.images.Eyes();
        
        // Set properties directly on the Eyes instance
        imagesEyes.setApiKey(ConfigReader.getProperty("applitools.api.key"));
        imagesEyes.setServerUrl(ConfigReader.getProperty("applitools.server.url"));
        imagesEyes.setBatch(batch);
        
        // Set match level
        MatchLevel level;
        switch (matchLevel.toUpperCase()) {
            case "STRICT": level = MatchLevel.STRICT; break;
            case "CONTENT": level = MatchLevel.CONTENT; break;
            case "LAYOUT": level = MatchLevel.LAYOUT; break;
            default: level = MatchLevel.STRICT;
        }
        imagesEyes.setMatchLevel(level);
        
        return imagesEyes;
    }
    
    public static synchronized BatchInfo getSharedBatch() {
        if (sharedBatch == null) {
            sharedBatch = new BatchInfo("Figma Visual Testing - Bajaj Finserv");
        }
        return sharedBatch;
    }
    
    public static synchronized VisualGridRunner getRunner() {
        if (runner == null) {
            runner = new VisualGridRunner(new RunnerOptions().testConcurrency(5));
        }
        return runner;
    }
}