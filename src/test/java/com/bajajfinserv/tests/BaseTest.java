package com.bajajfinserv.tests;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.bajajfinserv.listeners.TestListener;
import com.bajajfinserv.utils.ApplitoolsManager;
import org.testng.annotations.*;

import java.util.Arrays;

@Listeners(TestListener.class)
public class BaseTest {
    
    protected static BatchInfo sharedBatchForEyes;
    protected static BatchInfo sharedBatchForFigma;
    protected static VisualGridRunner sharedRunnerForEyes;

    @BeforeSuite
    public void setupSuite() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 STARTING TEST SUITE: Figma Visual Testing Suite");
        System.out.println("=".repeat(80));
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🚀 Starting Figma Visual Testing Suite");
        System.out.println("=".repeat(70));
        
        sharedBatchForEyes = ApplitoolsManager.createSharedBatch("- compare with Figma");
        sharedBatchForFigma = ApplitoolsManager.createSharedBatch("- upload from Figma");
        System.out.println("=".repeat(70) + "\n");

        sharedRunnerForEyes = ApplitoolsManager.createRunner();
    }
    
    @AfterSuite
    public void teardownSuite() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("⏳ WAITING FOR ALL VISUAL TESTS TO COMPLETE...");
        System.out.println("=".repeat(70));

        sharedBatchForFigma.setCompleted(true);
        sharedBatchForEyes.setCompleted(true);

        try {
            // CRITICAL: Get all results from VisualGridRunner
            TestResultsSummary allTestResults = sharedRunnerForEyes.getAllTestResults(false);

            System.out.println("\n✅ ALL VISUAL TESTS COMPLETED!");
            System.out.println("📊 Results Summary:");
            System.out.println("   Total: " + allTestResults.size());

            // Count results
            long passed = Arrays.stream(allTestResults.getAllResults())
                .filter(r -> r.getTestResults().isPassed()).count();
            long failed = Arrays.stream(allTestResults.getAllResults())
                .filter(r -> !r.getTestResults().isPassed() && !r.getTestResults().isNew()).count();
            long newTests = Arrays.stream(allTestResults.getAllResults())
                .filter(r -> r.getTestResults().isNew()).count();

            System.out.println("   Passed: " + passed);
            System.out.println("   Failed: " + failed);
            System.out.println("   New: " + newTests);

            System.out.println("\n📊 View results at: https://eyes.applitools.com");

        } finally {
            sharedRunnerForEyes.close();
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏁 Figma Visual Testing Suite Completed");
        System.out.println("=".repeat(70) + "\n");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ TEST SUITE COMPLETED: Figma Visual Testing Suite");
        System.out.println("=".repeat(80) + "\n");
    }
}