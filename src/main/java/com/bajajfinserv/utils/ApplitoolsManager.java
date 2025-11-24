package com.bajajfinserv.utils;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.StitchMode;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.bajajfinserv.config.ConfigReader;

public class ApplitoolsManager {

    // For Selenium Eyes (Website screenshots)
    public static com.applitools.eyes.selenium.Eyes getSeleniumEyes(String testName, String matchLevel, String uploadBaseline, BatchInfo batch, VisualGridRunner runner) {
        com.applitools.eyes.selenium.Eyes eyes = new com.applitools.eyes.selenium.Eyes(runner);
        Configuration config = new Configuration();

        config.setApiKey(ConfigReader.getProperty("applitools.api.key"));
        config.setServerUrl(ConfigReader.getProperty("applitools.server.url"));
        config.setBatch(batch);
        config.setStitchMode(StitchMode.CSS);
        config.setForceFullPageScreenshot(true);
        config.setHideScrollbars(true);
        config.setBaselineEnvName(testName);

        if ("TRUE".equalsIgnoreCase(uploadBaseline)) {
            config.setBaselineBranchName("figma");
        }

        switch (matchLevel.toUpperCase()) {
            case "STRICT":
                config.setMatchLevel(MatchLevel.STRICT);
                break;
            case "CONTENT":
                config.setMatchLevel(MatchLevel.CONTENT);
                break;
            case "LAYOUT":
                config.setMatchLevel(MatchLevel.LAYOUT);
                break;
            default:
                config.setMatchLevel(MatchLevel.STRICT);
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
            case "STRICT":
                level = MatchLevel.STRICT;
                break;
            case "CONTENT":
                level = MatchLevel.CONTENT;
                break;
            case "LAYOUT":
                level = MatchLevel.LAYOUT;
                break;
            default:
                level = MatchLevel.STRICT;
        }
        imagesEyes.setMatchLevel(level);

        return imagesEyes;
    }

    public static synchronized BatchInfo createSharedBatch(String batchNameSuffix) {
        BatchInfo batchInfo = new BatchInfo("Figma Visual Testing - Bajaj Finserv" + batchNameSuffix);
        batchInfo.setNotifyOnCompletion(true);
        System.out.println("✅ Created batch: " + batchInfo.getName());
        return batchInfo;
    }

    public static synchronized VisualGridRunner createRunner() {
        VisualGridRunner runner = new VisualGridRunner(new RunnerOptions().testConcurrency(5));
        runner.setDontCloseBatches(true);
        return runner;
    }
}