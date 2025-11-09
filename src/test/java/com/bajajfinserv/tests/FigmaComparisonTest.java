package com.bajajfinserv.tests;

import com.bajajfinserv.utils.*;
import com.bajajfinserv.utils.ExcelDataProvider.TestStep;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.StitchMode;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import java.time.Duration;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class FigmaComparisonTest extends BaseTest {
    
    protected WebDriver driver;
    protected Eyes eyes;
    protected JavascriptExecutor js;
    protected WebDriverWait wait;
    
    @DataProvider(name = "figmaTestData")
    public Object[][] getFigmaTestData() {
        return ExcelDataProvider.getTestData();
    }
    
    @Test(dataProvider = "figmaTestData")
    public void compareFigmaWithWebsite(String testName, String figmaUrl, String appUrl, 
                                       String viewport, String matchLevel, String uploadBaseline,
                                       List<TestStep> testSteps) throws Exception {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 TEST: " + testName);
        System.out.println("=".repeat(80));
        
        try {
            String[] viewportParts = viewport.split("x");
            int width = Integer.parseInt(viewportParts[0].trim());
            int height = Integer.parseInt(viewportParts[1].trim());
            
            initializeTest(testName, width, height, matchLevel, uploadBaseline);
            uploadFigmaBaseline(testName, figmaUrl);
            navigateToUrl(appUrl);
            
            if (testSteps != null && !testSteps.isEmpty()) {
                executeTestSteps(testSteps, testName);
            } else {
                System.out.println("\n⚠️  No test steps found - using default flow");
                removeStickyHeaders();
                captureFullPage(testName);
            }
            
            finalizeTest();
            
        } catch (Exception e) {
            handleTestFailure(e);
            throw e;
        } finally {
            cleanup();
        }
    }
    
    protected void initializeTest(String testName, int width, int height, 
                                   String matchLevel, String uploadBaseline) {
        System.out.println("\n🚀 INITIALIZING TEST");
        System.out.println("   Test Name: " + testName);
        System.out.println("   Viewport: " + width + "x" + height);
        
        eyes = ApplitoolsManager.getEyes(matchLevel, uploadBaseline, getSharedBatch());
        eyes.setStitchMode(StitchMode.CSS);
        eyes.setForceFullPageScreenshot(true);
        eyes.setHideScrollbars(true);
        
        driver = DriverManager.getDriver(width, height);
        js = (JavascriptExecutor) driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        
        eyes.open(driver, "Figma Visual Testing", testName, new RectangleSize(width, height));
        
        System.out.println("✅ Test initialized successfully");
    }
    
    protected void uploadFigmaBaseline(String testName, String figmaUrl) throws Exception {
        System.out.println("\n📸 UPLOADING FIGMA BASELINE");
        
        BufferedImage figmaImage = FigmaAPIClient.getFigmaComponentImage(figmaUrl);
        if (figmaImage == null) {
            throw new Exception("Failed to fetch Figma image");
        }
        
        System.out.println("   Figma image: " + figmaImage.getWidth() + "x" + 
                          figmaImage.getHeight() + " pixels");
        
        File tempFile = File.createTempFile("figma-", ".png");
        ImageIO.write(figmaImage, "png", tempFile);
        driver.get("file:///" + tempFile.getAbsolutePath().replace("\\", "/"));
        Thread.sleep(2000);
        
        eyes.checkWindow(testName);
        
        System.out.println("✅ Figma baseline queued");
        tempFile.delete();
    }
    
    protected void navigateToUrl(String url) {
        System.out.println("\n🌐 NAVIGATING TO WEBSITE");
        System.out.println("   URL: " + url);
        
        driver.get(url);
        wait.until(webDriver -> 
            js.executeScript("return document.readyState").equals("complete"));
        sleep(2000);
        
        System.out.println("✅ Page loaded successfully");
    }
    
    protected void executeTestSteps(List<TestStep> steps, String testName) throws Exception {
        System.out.println("\n🎬 EXECUTING " + steps.size() + " TEST STEPS");
        System.out.println("-".repeat(80));
        
        for (TestStep step : steps) {
            System.out.println("\n📌 Step " + step.stepOrder + ": " + step.action.toUpperCase());
            
            try {
                switch (step.action.toLowerCase()) {
                    case "navigate":
                        System.out.println("   ✓ Already navigated");
                        break;
                    case "wait":
                        if (!step.waitSeconds.isEmpty()) {
                            int waitTime = Integer.parseInt(step.waitSeconds) * 1000;
                            System.out.println("   ⏱️  Waiting " + step.waitSeconds + " seconds...");
                            sleep(waitTime);
                            System.out.println("   ✓ Wait complete");
                        }
                        break;
                    case "removestickyheaders":
                        removeStickyHeaders();
                        break;
                    case "click":
                        clickElement(step.locator);
                        break;
                    case "scroll":
                        scrollToElement(step.locator);
                        break;
                    case "capturefullpage":
                        String checkpointName = step.checkpointName.isEmpty() ? testName : step.checkpointName;
                        captureFullPage(checkpointName);
                        break;
                    case "capturecomponent":
                        String componentName = step.checkpointName.isEmpty() ? testName : step.checkpointName;
                        captureComponent(step.locator, componentName);
                        break;
                    default:
                        System.out.println("   ⚠️  Unknown action: " + step.action);
                }
            } catch (Exception e) {
                System.err.println("   ❌ Step failed: " + e.getMessage());
            }
        }
        
        System.out.println("\n" + "-".repeat(80));
    }
    
    protected void sleep(int milliseconds) {
        try { Thread.sleep(milliseconds); } catch (InterruptedException e) {}
    }
    
    protected void removeStickyHeaders() {
        System.out.println("   🧹 Removing sticky headers...");
        js.executeScript(
            "var style = document.createElement('style');" +
            "style.innerHTML = 'header, nav, [class*=\"sticky\"], [class*=\"fixed\"], " +
            "[style*=\"position: fixed\"], [style*=\"position: sticky\"] " +
            "{ position: relative !important; }';" +
            "document.head.appendChild(style);"
        );
        sleep(500);
        System.out.println("   ✓ Done");
    }
    
    protected void clickElement(String locator) {
        System.out.println("   🖱️  Click: " + locator);
        findElement(locator).click();
        sleep(1000);
        System.out.println("   ✓ Clicked");
    }
    
    protected void scrollToElement(String locator) {
        System.out.println("   📜 Scroll to: " + locator);
        WebElement element = findElement(locator);
        js.executeScript("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element);
        sleep(1500);
        System.out.println("   ✓ Scrolled");
    }
    
    protected void captureFullPage(String checkpointName) {
        System.out.println("   📸 Capturing: " + checkpointName);
        eyes.checkWindow(checkpointName);
        System.out.println("   ✓ Captured");
    }
    
    protected void captureComponent(String locator, String checkpointName) {
        System.out.println("   📸 Capturing component: " + checkpointName);
        By by = getLocator(locator);
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        sleep(1000);
        eyes.checkRegion(by, checkpointName);
        System.out.println("   ✓ Captured");
    }
    
    protected WebElement findElement(String locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(getLocator(locator)));
    }
    
    protected By getLocator(String locator) {
        return (locator.startsWith("//") || locator.startsWith("(//")) 
            ? By.xpath(locator) : By.cssSelector(locator);
    }
    
    protected void finalizeTest() {
        System.out.println("\n✅ FINALIZING TEST");
        System.out.println("   📤 Queuing test for upload...");
        
        try {
            // Queue test - actual upload happens in @AfterSuite
            eyes.closeAsync();
            System.out.println("   ✅ Test queued!");
        } catch (Exception e) {
            System.err.println("   ⚠️  Error: " + e.getMessage());
            try { eyes.abortAsync(); } catch (Exception ex) {}
        }
        
        System.out.println("✅ Test completed!");
    }
    
    protected void handleTestFailure(Exception e) {
        System.err.println("\n❌ TEST FAILED: " + e.getMessage());
        if (eyes != null) {
            try { eyes.abortAsync(); } catch (Exception ex) {}
        }
    }
    
    protected void cleanup() {
        System.out.println("\n🧹 CLEANUP");
        if (driver != null) {
            driver.quit();
            System.out.println("✓ Browser closed");
        }
        System.out.println("=".repeat(80) + "\n");
    }
}
