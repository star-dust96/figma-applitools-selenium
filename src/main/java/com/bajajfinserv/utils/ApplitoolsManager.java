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
