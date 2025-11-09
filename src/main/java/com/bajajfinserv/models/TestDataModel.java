package com.bajajfinserv.models;

public class TestDataModel {
    private String testName;
    private String figmaUrl;
    private String appUrl;
    private String componentSelector;
    private String viewport;
    private String matchLevel;
    private boolean uploadBaseline;
    private boolean enabled;

    public TestDataModel(String testName, String figmaUrl, String appUrl, 
                         String componentSelector, String viewport, 
                         String matchLevel, boolean uploadBaseline, boolean enabled) {
        this.testName = testName;
        this.figmaUrl = figmaUrl;
        this.appUrl = appUrl;
        this.componentSelector = componentSelector;
        this.viewport = viewport;
        this.matchLevel = matchLevel;
        this.uploadBaseline = uploadBaseline;
        this.enabled = enabled;
    }

    public String getTestName() {
        return testName;
    }

    public String getFigmaUrl() {
        return figmaUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public String getComponentSelector() {
        return componentSelector;
    }

    public String getViewport() {
        return viewport;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public boolean shouldUploadBaseline() {
        return uploadBaseline;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasComponentSelector() {
        return componentSelector != null && !componentSelector.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TestData{" +
                "testName='" + testName + '\'' +
                ", figmaUrl='" + figmaUrl + '\'' +
                ", appUrl='" + appUrl + '\'' +
                ", componentSelector='" + componentSelector + '\'' +
                ", viewport='" + viewport + '\'' +
                ", matchLevel='" + matchLevel + '\'' +
                ", uploadBaseline=" + uploadBaseline +
                ", enabled=" + enabled +
                '}';
    }
}