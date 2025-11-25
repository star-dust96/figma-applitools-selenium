package com.bajajfinserv.tests;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.bajajfinserv.utils.*;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;

import java.time.Duration;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.List;

public class FigmaComparisonTest extends BaseTest {

    protected WebDriver driver;
    protected JavascriptExecutor js;
    protected WebDriverWait wait;
    protected String currentTestName;
    protected int viewportWidth;
    protected int viewportHeight;

    @Test(dataProvider = "excelData", dataProviderClass = ExcelDataProviderMain.class)
    public void compareFigmaWithWebsite(
            String testName,
            String figmaUrl,
            String appUrl,
            String viewport,
            String matchLevel,
            String uploadBaseline,
            List<ExcelDataProviderMain.TestStep> testSteps
    ) {
        this.currentTestName = testName;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 TEST: " + testName);
        System.out.println("=".repeat(80));

        String[] viewportDims = viewport.split("x");
        viewportWidth = Integer.parseInt(viewportDims[0]);
        viewportHeight = Integer.parseInt(viewportDims[1]);

        if ("TRUE".equalsIgnoreCase(uploadBaseline)) {
            System.out.println("\n📸 PHASE 1: Creating Figma Baseline\n");
            uploadFigmaBaseline(testName, figmaUrl, matchLevel);
        } else {
            System.out.println("\n⏭️  PHASE 2: Skipping Figma upload - comparing against existing baseline\n");
        }

        compareFigmaWithApplicationInBrowser(testName, appUrl, matchLevel, uploadBaseline, testSteps);
        System.out.println("✅ Test '" + testName + "' completed!");
        System.out.println("=".repeat(80) + "\n");
    }

    private void compareFigmaWithApplicationInBrowser(String testName, String appUrl, String matchLevel, String uploadBaseline, List<ExcelDataProviderMain.TestStep> testSteps) {
        Eyes seleniumEyes;
        seleniumEyes = initializeTest(testName, viewportWidth, viewportHeight, matchLevel, uploadBaseline);

        navigateToUrl(appUrl);
        if (testSteps != null && !testSteps.isEmpty()) {
            System.out.println("\n📋 EXECUTING " + testSteps.size() + " TEST STEPS");
            System.out.println("-".repeat(80));

            for (ExcelDataProviderMain.TestStep step : testSteps) {
                executeStep(step, seleniumEyes);
            }

            System.out.println("-".repeat(80));
        } else {
            System.out.println("\n📋 NO TEST STEPS - CAPTURING FULL PAGE");
            sleep(3000);
            removeStickyHeaders();
            captureFullPage(testName, seleniumEyes);
        }

        finalizeTest(seleniumEyes, sharedRunnerForEyes, sharedBatchForEyes);
    }

    protected void executeStep(ExcelDataProviderMain.TestStep step, Eyes seleniumEyes) {
        if (step == null || step.action == null || step.action.trim().isEmpty()) {
            System.out.println("   ⚠️  Skipping empty step");
            return;
        }

        String action = step.action.trim().toUpperCase();

        System.out.println("\n📍 Step " + step.stepOrder + ": " + action);

        try {
            switch (action) {
                case "NAVIGATE":
                    System.out.println("   ✓ Already navigated");
                    break;

                case "WAIT":
                    if (step.waitSeconds != null && !step.waitSeconds.isEmpty()) {
                        int seconds = Integer.parseInt(step.waitSeconds);
                        System.out.println("   ⏱️  Waiting " + seconds + " seconds...");
                        sleep(seconds * 1000);
                        System.out.println("   ✓ Wait complete");
                    }
                    break;

                case "REMOVESSTICKYHEADERS":
                case "REMOVESTICKYHEADERS":
                    System.out.println("   🧹 Removing sticky headers...");
                    removeStickyHeaders();
                    System.out.println("   ✓ Done");
                    break;

                case "CAPTUREFULLPAGE":
                    String fullPageName = (step.checkpointName != null && !step.checkpointName.isEmpty())
                                          ? step.checkpointName : this.currentTestName;
                    captureFullPage(fullPageName, seleniumEyes);
                    break;

                case "CAPTURECOMPONENT":
                    String componentName = (step.checkpointName != null && !step.checkpointName.isEmpty())
                                           ? step.checkpointName : "Component";
                    captureComponent(step.locator, componentName, seleniumEyes);
                    break;

                case "SCROLL":
                    System.out.println("   📜 Scroll to: " + step.locator);
                    scrollToElement(step.locator);
                    System.out.println("   ✓ Scrolled");
                    break;

                case "CLICK":
                    System.out.println("   🖱️  Click: " + step.locator);
                    clickElement(step.locator);
                    System.out.println("   ✓ Clicked");
                    break;

                case "TYPE":
                    System.out.println("   ⌨️  Type into: " + step.locator);
                    if (step.checkpointName != null && !step.checkpointName.isEmpty()) {
                        typeText(step.locator, step.checkpointName);
                        System.out.println("   ✓ Text entered");
                    }
                    break;

                default:
                    System.out.println("   ⚠️  Unknown action: " + step.action);
            }
        } catch (Exception e) {
            System.err.println("   ❌ Step failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected Eyes initializeTest(String testName, int width, int height, String matchLevel, String uploadBaseline) {
        System.out.println("\n🚀 INITIALIZING TEST");
        System.out.println("   Test Name: " + testName);
        System.out.println("   Viewport: " + width + "x" + height);

        Eyes seleniumEyes = ApplitoolsManager.getSeleniumEyes(testName, matchLevel, uploadBaseline, sharedBatchForEyes, sharedRunnerForEyes);

        driver = DriverManager.getDriver(width, height);
        js = (JavascriptExecutor) driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        seleniumEyes.open(driver, "Figma Visual Testing", testName, new RectangleSize(width, height));

        System.out.println("✅ Test initialized successfully");
        return seleniumEyes;
    }

    protected void uploadFigmaBaseline(String testName, String figmaUrl, String matchLevel) {
        System.out.println("\n📸 UPLOADING FIGMA BASELINE");

        BufferedImage figmaImage = FigmaAPIClient.getFigmaComponentImage(figmaUrl);
        if (figmaImage == null) {
            throw new RuntimeException("Failed to fetch Figma image");
        }

        System.out.println("   Original Figma: " + figmaImage.getWidth() + "x" + figmaImage.getHeight() + " pixels");

        if (figmaImage.getWidth() != viewportWidth) {
            System.out.println("   🔄 Resizing Figma width to match viewport: " + viewportWidth + "px");
            figmaImage = resizeImageWidth(figmaImage, viewportWidth);
            System.out.println("   ✅ Resized to: " + figmaImage.getWidth() + "x" + figmaImage.getHeight());
        }

        com.applitools.eyes.images.Eyes imagesEyes = null;

        try {
            System.out.println("   🔧 Creating Images Eyes instance for Figma upload...");
            imagesEyes = ApplitoolsManager.getImagesEyes(matchLevel, sharedBatchForFigma);

            System.out.println("   🔑 Setting baselineEnvName: " + testName);
            imagesEyes.setBaselineEnvName(testName);

            System.out.println("   📂 Opening Images Eyes session...");
            imagesEyes.open("Figma Visual Testing", testName,
                            new com.applitools.eyes.RectangleSize(figmaImage.getWidth(), figmaImage.getHeight()));

            System.out.println("   📤 Uploading Figma image directly to Applitools...");
            imagesEyes.checkImage(figmaImage, testName);

        } finally {
            System.out.println("   🔒 Closing Images Eyes session (uploading)...");
            TestResults testResults = imagesEyes.close(false);
            System.out.println("   ✅ Figma image uploaded. Test Results:");
            ApplitoolsManager.displayVisualValidationResults(testResults);

            System.out.println("✅ Figma baseline uploaded successfully!");
        }
    }



    protected BufferedImage resizeImageWidth(BufferedImage original, int targetWidth) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int targetHeight = (int) ((double) originalHeight * targetWidth / originalWidth);

        System.out.println("   📐 Maintaining aspect ratio: " + targetWidth + "x" + targetHeight);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    protected void navigateToUrl(String url) {
        System.out.println("\n🌐 NAVIGATING TO WEBSITE");
        System.out.println("   URL: " + url);
        driver.get(url);
        wait.until(webDriver -> js.executeScript("return document.readyState").equals("complete"));
        System.out.println("   ⏳ Waiting for page to stabilize...");
        sleep(3000);
        System.out.println("✅ Page loaded");
    }

    protected void removeStickyHeaders() {
        js.executeScript(
                "var style = document.createElement('style');" +
                "style.innerHTML = 'header, nav, [class*=\"sticky\"], [class*=\"fixed\"], " +
                "[style*=\"position: fixed\"], [style*=\"position: sticky\"] " +
                "{ position: relative !important; }';" +
                "document.head.appendChild(style);"
        );
        sleep(500);
    }

    protected void scrollToElement(String locator) {
        WebElement element = findElement(locator);
        js.executeScript("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element);
        sleep(1500);
    }

    protected void clickElement(String locator) {
        findElement(locator).click();
        sleep(1000);
    }

    protected void typeText(String locator, String text) {
        WebElement element = findElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void captureFullPage(String checkpointName, Eyes seleniumEyes) {
        System.out.println("   📸 Capturing full page: " + checkpointName);

        System.out.println("   🔑 Setting baselineEnvName: " + this.currentTestName);

        seleniumEyes.checkWindow(checkpointName);
        System.out.println("   ✓ Full page captured");
    }

    protected void captureComponent(String locator, String checkpointName, Eyes seleniumEyes) {
        System.out.println("   📸 Capturing component: " + checkpointName);
        System.out.println("   🔍 Locator: " + locator);

        try {
            System.out.println("   🔑 Setting baselineEnvName: " + this.currentTestName);

            By by = getLocator(locator);
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            sleep(1000);

            seleniumEyes.checkRegion(by, checkpointName);
            System.out.println("   ✓ Component captured");

        } catch (Exception e) {
            System.err.println("   ❌ Component capture failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected WebElement findElement(String locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(getLocator(locator)));
    }

    protected By getLocator(String locator) {
        if (locator.startsWith("//") || locator.startsWith("(//")) {
            return By.xpath(locator);
        } else if (locator.startsWith("#")) {
            return By.id(locator.substring(1));
        } else if (locator.startsWith(".")) {
            return By.className(locator.substring(1));
        } else {
            return By.cssSelector(locator);
        }
    }

    protected void finalizeTest(Eyes seleniumEyes, VisualGridRunner runner, BatchInfo sharedBatch) {
        System.out.println("\n✅ FINALIZING TEST");
        try {
            seleniumEyes.closeAsync();
            System.out.println("   ✅ Test uploaded successfully!");
        } catch (Exception e) {
            System.err.println("   ⚠️  Error during upload: " + e.getMessage());
            try {
                seleniumEyes.abortAsync();
            } catch (Exception ex) {
            }
        }
    }

    @AfterMethod
    protected void cleanup() {
        System.out.println("\n🧹 CLEANUP");
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("✓ Browser closed");
            } catch (Exception e) {
            }
        }
        System.out.println("=".repeat(80) + "\n");
    }

    protected void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}