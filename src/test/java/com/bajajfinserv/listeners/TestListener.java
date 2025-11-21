package com.bajajfinserv.listeners;

import com.aventstack.extentreports.ExtentReports;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.testng.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener, ISuiteListener {
    
    private static ExtentReports extent;
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private int skippedTests = 0;
    
    @Override
    public void onStart(ISuite suite) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 STARTING TEST SUITE: " + suite.getName());
        System.out.println("=".repeat(80));
        
        extent = ExtentManager.createInstance();
        
        System.out.println("✅ Report initialization complete");
        System.out.println("=".repeat(80) + "\n");
    }
    
    @Override
    public void onFinish(ISuite suite) {
        if (extent != null) {
            extent.flush();
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📊 TEST SUITE COMPLETED: " + suite.getName());
            System.out.println("=".repeat(80));
            System.out.println("📈 TEST SUMMARY:");
            System.out.println("   Total Tests:   " + totalTests);
            System.out.println("   ✅ Passed:     " + passedTests);
            System.out.println("   ❌ Failed:     " + failedTests);
            System.out.println("   ⏭️  Skipped:    " + skippedTests);
            System.out.println("=".repeat(80));
            System.out.println("📄 REPORT LOCATION:");
            System.out.println("   " + ExtentManager.getReportPath());
            System.out.println("=".repeat(80));
            System.out.println("🔗 APPLITOOLS DASHBOARD:");
            System.out.println("   https://eyes.applitools.com");
            System.out.println("=".repeat(80) + "\n");
        }
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        totalTests++;
        
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        
        if (description == null || description.isEmpty()) {
            description = "Figma visual comparison test";
        }
        
        ExtentTest test = extent.createTest(testName, description);
        ExtentManager.setTest(test);
        
        // Get test parameters
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            test.info("<b>Test Name:</b> " + parameters[0]);
            if (parameters.length > 1) {
                test.info("<b>Figma URL:</b> " + parameters[1]);
            }
            if (parameters.length > 2) {
                test.info("<b>Website URL:</b> " + parameters[2]);
            }
            if (parameters.length > 3) {
                test.info("<b>Viewport:</b> " + parameters[3]);
            }
            if (parameters.length > 4) {
                test.info("<b>Match Level:</b> " + parameters[4]);
            }
        }
        
        test.info("⏰ Test started at: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        passedTests++;
        
        ExtentTest test = ExtentManager.getTest();
        
        test.log(Status.PASS, MarkupHelper.createLabel("TEST PASSED", ExtentColor.GREEN));
        test.pass("✅ All visual comparisons matched successfully");
        test.pass("🎨 Figma design matches website implementation");
        
        // Add Applitools link
        test.info("🔗 <a href='https://eyes.applitools.com/app/test-results/' target='_blank'>" +
                 "View detailed results in Applitools Dashboard</a>");
        
        long duration = (result.getEndMillis() - result.getStartMillis()) / 1000;
        test.info("⏱️ Duration: " + duration + " seconds");
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        failedTests++;
        
        ExtentTest test = ExtentManager.getTest();
        
        test.log(Status.FAIL, MarkupHelper.createLabel("TEST FAILED", ExtentColor.RED));
        test.fail("❌ Visual comparison failed");
        
        // Log the exception
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            test.fail("<pre>" + throwable.getMessage() + "</pre>");
            
            // Add stack trace in expandable section
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : throwable.getStackTrace()) {
                stackTrace.append(element.toString()).append("<br>");
            }
            test.fail("<details><summary>📋 Stack Trace</summary>" + stackTrace.toString() + "</details>");
        }
        
        // Add Applitools link for reviewing differences
        test.fail("🔗 <a href='https://eyes.applitools.com/app/test-results/' target='_blank'>" +
                 "Review visual differences in Applitools Dashboard</a>");
        
        long duration = (result.getEndMillis() - result.getStartMillis()) / 1000;
        test.info("⏱️ Duration: " + duration + " seconds");
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        skippedTests++;
        
        ExtentTest test = extent.createTest(result.getMethod().getMethodName());
        ExtentManager.setTest(test);
        
        test.log(Status.SKIP, MarkupHelper.createLabel("TEST SKIPPED", ExtentColor.YELLOW));
        test.skip("⏭️ Test was skipped");
        
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            test.skip("Reason: " + throwable.getMessage());
        }
    }
    
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Not used
    }
    
    @Override
    public void onStart(ITestContext context) {
        // Not used
    }
    
    @Override
    public void onFinish(ITestContext context) {
        // Test context finished
    }
}
