package com.bajajfinserv.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelDataProviderMain {
    
    public static Object[][] getTestData() {
        System.out.println("=".repeat(60));
        System.out.println("🔍 Reading Excel Test Data...");
        System.out.println("=".repeat(60));
        
        List<Object[]> testData = new ArrayList<>();
        FileInputStream fis = null;
        Workbook workbook = null;
        
        try {
            // Excel file path
            String excelPath = "src/test/resources/testdata.xlsx";
            File excelFile = new File(excelPath);
            
            System.out.println("📁 Excel path: " + excelFile.getAbsolutePath());
            System.out.println("📄 File exists: " + excelFile.exists());
            
            if (!excelFile.exists()) {
                System.err.println("❌ ERROR: Excel file not found!");
                return new Object[0][0];
            }
            
            // Open workbook
            fis = new FileInputStream(excelFile);
            workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            
            System.out.println("📊 Sheet name: " + sheet.getSheetName());
            System.out.println("📏 Total rows: " + sheet.getLastRowNum());
            
            // Read header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                System.err.println("❌ ERROR: Header row is empty!");
                return new Object[0][0];
            }
            
            int totalColumns = headerRow.getLastCellNum();
            
            // Find column indices
            int testNameCol = -1, figmaUrlCol = -1, appUrlCol = -1;
            int componentSelectorCol = -1, viewportCol = -1, matchLevelCol = -1;
            int uploadBaselineCol = -1, enabledCol = -1;
            
            for (int i = 0; i < totalColumns; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = cell.getStringCellValue().trim().toLowerCase();
                    switch (header) {
                        case "testname":
                            testNameCol = i;
                            break;
                        case "figmaurl":
                            figmaUrlCol = i;
                            break;
                        case "appurl":
                            appUrlCol = i;
                            break;
                        case "componentselector":
                        case "componentselector ":
                        case "component selector":
                            componentSelectorCol = i;
                            break;
                        case "viewport":
                        case "viewport ":
                            viewportCol = i;
                            break;
                        case "matchlevel":
                        case "matchlevel ":
                        case "match level":
                            matchLevelCol = i;
                            break;
                        case "uploadbaseline":
                        case "uploadbaseline ":
                        case "upload baseline":
                            uploadBaselineCol = i;
                            break;
                        case "enabled":
                        case "enabled ":
                            enabledCol = i;
                            break;
                    }
                }
            }
            
            System.out.println("📋 Column mapping:");
            System.out.println("   testName: " + testNameCol);
            System.out.println("   figmaUrl: " + figmaUrlCol);
            System.out.println("   appUrl: " + appUrlCol);
            System.out.println("   componentSelector: " + componentSelectorCol);
            System.out.println("   viewport: " + viewportCol);
            System.out.println("   matchLevel: " + matchLevelCol);
            System.out.println("   uploadBaseline: " + uploadBaselineCol);
            System.out.println("   enabled: " + enabledCol);
            
            if (enabledCol == -1) {
                System.err.println("❌ ERROR: 'enabled' column not found!");
                return new Object[0][0];
            }
            
            // Read data rows
            int enabledCount = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                // Check if row is empty
                boolean isEmpty = true;
                for (int j = 0; j < totalColumns; j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        isEmpty = false;
                        break;
                    }
                }
                
                if (isEmpty) {
                    continue;
                }
                
                // Check if enabled
                Cell enabledCell = row.getCell(enabledCol);
                if (enabledCell == null) {
                    System.out.println("⏭️  Skipping row " + (i + 1) + " - enabled cell is empty");
                    continue;
                }
                
                // Get enabled value (handles formulas, booleans, strings, numbers)
                String enabledValue = getCellValue(enabledCell).trim().toUpperCase();
                
                if (!"TRUE".equals(enabledValue)) {
                    System.out.println("⏭️  Skipping row " + (i + 1) + " - enabled = " + enabledValue);
                    continue;
                }
                
                enabledCount++;
                
                // Read all column values
                String testName = getCellValue(row.getCell(testNameCol));
                String figmaUrl = getCellValue(row.getCell(figmaUrlCol));
                String appUrl = getCellValue(row.getCell(appUrlCol));
                String componentSelector = getCellValue(row.getCell(componentSelectorCol));
                String viewport = getCellValue(row.getCell(viewportCol));
                String matchLevel = getCellValue(row.getCell(matchLevelCol));
                String uploadBaseline = getCellValue(row.getCell(uploadBaselineCol));
                
                System.out.println("✅ Row " + (i + 1) + ": " + testName + " (enabled)");
                
                testData.add(new Object[]{
                    testName, figmaUrl, appUrl, componentSelector,
                    viewport, matchLevel, uploadBaseline
                });
            }
            
            System.out.println("=".repeat(60));
            System.out.println("📊 Total enabled tests found: " + enabledCount);
            System.out.println("=".repeat(60));
            
            if (testData.isEmpty()) {
                System.err.println("❌ WARNING: No enabled tests found in Excel!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR reading Excel: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (workbook != null) workbook.close();
                if (fis != null) fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Convert List to Object[][]
        Object[][] result = new Object[testData.size()][];
        for (int i = 0; i < testData.size(); i++) {
            result[i] = testData.get(i);
        }
        
        return result;
    }
    
    /**
     * Get cell value as String - handles all cell types including formulas
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
                
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // Excel boolean: 1=TRUE, 0=FALSE
                    if (numValue == 1.0) {
                        return "TRUE";
                    } else if (numValue == 0.0) {
                        return "FALSE";
                    } else if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
                
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
                
            case FORMULA:
                // Get the evaluated result of the formula
                try {
                    switch (cell.getCachedFormulaResultType()) {
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
                        case STRING:
                            return cell.getStringCellValue().trim();
                        case NUMERIC:
                            double numValue = cell.getNumericCellValue();
                            // Handle Excel boolean (1=TRUE, 0=FALSE)
                            if (numValue == 1.0) {
                                return "TRUE";
                            } else if (numValue == 0.0) {
                                return "FALSE";
                            } else if (numValue == Math.floor(numValue)) {
                                return String.valueOf((long) numValue);
                            } else {
                                return String.valueOf(numValue);
                            }
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    // If evaluation fails, return empty string
                    return "";
                }
                
            case BLANK:
                return "";
                
            default:
                return "";
        }
    }
}