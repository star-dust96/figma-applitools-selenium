package com.bajajfinserv.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelDataProviderMain {
    
    private static final String EXCEL_PATH = "src/test/resources/testdata.xlsx";
    
    public static class TestStep {
        public int stepOrder;
        public String action;
        public String locator;
        public String checkpointName;
        public String waitSeconds;
        
        public TestStep(int stepOrder, String action, String locator, String checkpointName, String waitSeconds) {
            this.stepOrder = stepOrder;
            this.action = action;
            this.locator = locator;
            this.checkpointName = checkpointName;
            this.waitSeconds = waitSeconds;
        }
    }
    
    @DataProvider(name = "excelData")
    public static Object[][] getTestData() {
        System.out.println("=".repeat(60));
        System.out.println("🔍 Reading Excel Test Data...");
        System.out.println("=".repeat(60));
        
        List<Object[]> testData = new ArrayList<>();
        
        try {
            File excelFile = new File(EXCEL_PATH);
            System.out.println("📁 Excel path: " + excelFile.getAbsolutePath());
            System.out.println("📄 File exists: " + excelFile.exists());
            
            if (!excelFile.exists()) {
                System.err.println("❌ ERROR: Excel file not found!");
                return new Object[0][0];
            }
            
            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = new XSSFWorkbook(fis);
            
            Sheet testCasesSheet = workbook.getSheet("TestCases");
            if (testCasesSheet == null) {
                System.err.println("❌ ERROR: 'TestCases' sheet not found!");
                workbook.close();
                fis.close();
                return new Object[0][0];
            }
            
            System.out.println("📊 TestCases sheet found");
            
            Sheet testStepsSheet = workbook.getSheet("TestSteps");
            Map<String, List<TestStep>> testStepsMap = new HashMap<>();
            
            if (testStepsSheet != null) {
                System.out.println("📋 TestSteps sheet found - reading steps...");
                testStepsMap = readTestSteps(testStepsSheet);
                System.out.println("✅ Loaded steps for " + testStepsMap.size() + " tests");
            } else {
                System.out.println("⚠️  TestSteps sheet not found - tests will run without steps");
            }
            
            Row headerRow = testCasesSheet.getRow(0);
            Map<String, Integer> columnMap = getColumnMap(headerRow);
            
            System.out.println("📋 Column mapping: " + columnMap);
            
            int enabledCount = 0;
            for (int i = 1; i <= testCasesSheet.getLastRowNum(); i++) {
                Row row = testCasesSheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                
                String enabledValue = getCellValue(row.getCell(columnMap.get("enabled"))).trim().toUpperCase();
                if (!"TRUE".equals(enabledValue)) {
                    System.out.println("⏭️  Skipping row " + (i + 1) + " - not enabled");
                    continue;
                }
                
                enabledCount++;
                
                String testName = getCellValue(row.getCell(columnMap.get("testname")));
                String figmaUrl = getCellValue(row.getCell(columnMap.get("figmaurl")));
                String appUrl = getCellValue(row.getCell(columnMap.get("appurl")));
                String viewport = getCellValue(row.getCell(columnMap.get("viewport")));
                String matchLevel = getCellValue(row.getCell(columnMap.get("matchlevel")));
                String uploadBaseline = getCellValue(row.getCell(columnMap.get("uploadbaseline")));
                
                List<TestStep> steps = testStepsMap.getOrDefault(testName, new ArrayList<>());
                
                System.out.println("✅ Row " + (i + 1) + ": " + testName + " (" + steps.size() + " steps)");
                
                testData.add(new Object[]{
                    testName, figmaUrl, appUrl, viewport, matchLevel, uploadBaseline, steps
                });
            }
            
            System.out.println("=".repeat(60));
            System.out.println("📊 Total enabled tests: " + enabledCount);
            System.out.println("=".repeat(60));
            
            workbook.close();
            fis.close();
            
        } catch (Exception e) {
            System.err.println("❌ ERROR reading Excel: " + e.getMessage());
            e.printStackTrace();
        }
        
        return testData.toArray(new Object[0][0]);
    }
    
    private static Map<String, List<TestStep>> readTestSteps(Sheet sheet) {
        Map<String, List<TestStep>> stepsMap = new HashMap<>();
        
        try {
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = getColumnMap(headerRow);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                
                String testName = getCellValue(row.getCell(columnMap.get("testname")));
                if (testName.isEmpty()) {
                    continue;
                }
                
                int stepOrder = getNumericValue(row.getCell(columnMap.get("steporder")));
                String action = getCellValue(row.getCell(columnMap.get("action")));
                String locator = getCellValue(row.getCell(columnMap.get("locator")));
                String checkpointName = getCellValue(row.getCell(columnMap.get("checkpointname")));
                String waitSeconds = getCellValue(row.getCell(columnMap.get("waitseconds")));
                
                TestStep step = new TestStep(stepOrder, action, locator, checkpointName, waitSeconds);
                
                stepsMap.computeIfAbsent(testName, k -> new ArrayList<>()).add(step);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error reading TestSteps: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stepsMap;
    }
    
    private static Map<String, Integer> getColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = cell.getStringCellValue().trim().toLowerCase();
                map.put(header, i);
            }
        }
        return map;
    }
    
    private static boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == 1.0) return "TRUE";
                if (numValue == 0.0) return "FALSE";
                if (numValue == Math.floor(numValue)) return String.valueOf((long) numValue);
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
            case FORMULA:
                try {
                    return getCellValueFromFormula(cell);
                } catch (Exception e) {
                    return "";
                }
            default:
                return "";
        }
    }
    
    private static String getCellValueFromFormula(Cell cell) {
        switch (cell.getCachedFormulaResultType()) {
            case BOOLEAN: 
                return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
            case STRING: 
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == 1.0) return "TRUE";
                if (numValue == 0.0) return "FALSE";
                if (numValue == Math.floor(numValue)) return String.valueOf((long) numValue);
                return String.valueOf(numValue);
            default: 
                return "";
        }
    }
    
    private static int getNumericValue(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        try {
            return Integer.parseInt(getCellValue(cell));
        } catch (Exception e) {
            return 0;
        }
    }
}