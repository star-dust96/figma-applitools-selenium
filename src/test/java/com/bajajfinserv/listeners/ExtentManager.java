package com.bajajfinserv.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {
    
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    
    private static String reportFileName = "Figma-Visual-Testing-Report-" + 
                                           new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".html";
    private static String reportFilePath = System.getProperty("user.dir") + "/test-output/" + reportFileName;

    public static ExtentReports createInstance() {
        // Create test-output directory if it doesn't exist
        File directory = new File(System.getProperty("user.dir") + "/test-output");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportFilePath);
        
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("Figma Visual Testing Report");
        sparkReporter.config().setReportName("Bajaj Finserv - DCX Team");
        sparkReporter.config().setEncoding("utf-8");
        sparkReporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm a '('zzz')'");
        
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Organization", "Bajaj Finserv");
        extent.setSystemInfo("Team", "Design (DCX) QA");
        extent.setSystemInfo("Test Type", "Visual Regression Testing");
        extent.setSystemInfo("Framework", "Selenium + Applitools + TestNG");
        extent.setSystemInfo("Browser", "Chrome");
        extent.setSystemInfo("Environment", "QA");
        
        return extent;
    }
    
    public static ExtentReports getInstance() {
        if (extent == null) {
            createInstance();
        }
        return extent;
    }
    
    // Both method names for compatibility
    public static String getReportFilePath() {
        return reportFilePath;
    }
    
    public static String getReportPath() {
        return reportFilePath;
    }
    
    // ⭐ Thread-safe test management
    public static synchronized ExtentTest getTest() {
        return extentTest.get();
    }
    
    public static synchronized void setTest(ExtentTest test) {
        extentTest.set(test);
    }
    
    public static synchronized void removeTest() {
        extentTest.remove();
    }
}