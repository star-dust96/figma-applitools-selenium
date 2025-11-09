package com.bajajfinserv.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ExcelDataProvider {
    
    public static Object[][] getTestData() {
        System.out.println("=".repeat(60));
        System.out.println("🔍 Reading Excel Test Data...");
        System.out.println("=".repeat(60));
        
        List<Object[]> testData = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream("src/test/resources/testdata.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet testCasesSheet = workbook.getSheet("TestCases");
            if (testCasesSheet == null) {
                System.err.println("❌ 'TestCases' sheet not found!");
                return new Object[0][0];
            }
            
            Sheet testStepsSheet = workbook.getSheet("TestSteps");
            Map<String, List<TestStep>> allTestSteps = readTestSteps(testStepsSheet);
            
            for (int i = 1; i <= testCasesSheet.getLastRowNum(); i++) {
                Row row = testCasesSheet.getRow(i);
                if (row == null) continue;
                
                String testName = getCellValue(row.getCell(0));
                if (testName.isEmpty()) continue;
                
                String enabled = getCellValue(row.getCell(6)).trim().toUpperCase();
                if (!"TRUE".equals(enabled)) {
                    System.out.println("⏭️  Skipping: " + testName);
                    continue;
                }
                
                testData.add(new Object[]{
                    testName,
                    getCellValue(row.getCell(1)), // figmaUrl
                    getCellValue(row.getCell(2)), // appUrl
                    getCellValue(row.getCell(3)), // viewport
                    getCellValue(row.getCell(4)), // matchLevel
                    getCellValue(row.getCell(5)), // uploadBaseline
                    allTestSteps.get(testName)     // steps
                });
                
                System.out.println("✅ " + testName);
            }
            
            System.out.println("=".repeat(60));
            System.out.println("📊 Total: " + testData.size());
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        return testData.toArray(new Object[0][]);
    }
    
    private static Map<String, List<TestStep>> readTestSteps(Sheet sheet) {
        Map<String, List<TestStep>> map = new HashMap<>();
        if (sheet == null) return map;
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String testName = getCellValue(row.getCell(0));
            if (testName.isEmpty()) continue;
            
            String stepOrderStr = getCellValue(row.getCell(1)).trim();
            if (stepOrderStr.isEmpty()) continue;
            
            int stepOrder;
            try {
                stepOrder = Integer.parseInt(stepOrderStr);
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Row " + (i+1) + ": Bad stepOrder '" + stepOrderStr + "'");
                continue;
            }
            
            String action = getCellValue(row.getCell(2));
            if (action.isEmpty()) continue;
            
            TestStep step = new TestStep(
                stepOrder,
                action,
                getCellValue(row.getCell(3)), // locator
                getCellValue(row.getCell(4)), // checkpointName
                getCellValue(row.getCell(5))  // waitSeconds
            );
            
            map.computeIfAbsent(testName, k -> new ArrayList<>()).add(step);
        }
        
        map.values().forEach(steps -> steps.sort(Comparator.comparingInt(s -> s.stepOrder)));
        return map;
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == 1.0) return "TRUE";
                if (num == 0.0) return "FALSE";
                return num == Math.floor(num) ? String.valueOf((long) num) : String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
            case FORMULA:
                return getFormulaValue(cell);
            default:
                return "";
        }
    }
    
    private static String getFormulaValue(Cell cell) {
        try {
            switch (cell.getCachedFormulaResultType()) {
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
                case STRING: return cell.getStringCellValue().trim();
                case NUMERIC:
                    double n = cell.getNumericCellValue();
                    if (n == 1.0) return "TRUE";
                    if (n == 0.0) return "FALSE";
                    return n == Math.floor(n) ? String.valueOf((long) n) : String.valueOf(n);
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    public static class TestStep {
        public int stepOrder;
        public String action;
        public String locator;
        public String checkpointName;
        public String waitSeconds;
        
        public TestStep(int stepOrder, String action, String locator, 
                       String checkpointName, String waitSeconds) {
            this.stepOrder = stepOrder;
            this.action = action;
            this.locator = locator;
            this.checkpointName = checkpointName;
            this.waitSeconds = waitSeconds;
        }
    }
}