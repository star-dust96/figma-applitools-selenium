# Figma to Applitools Visual Testing Framework

## 🎯 Overview
Automated visual regression testing framework that compares Figma designs with live websites using Applitools Eyes and Selenium.

## 🏗️ Architecture
- **Figma API**: Fetches design screenshots
- **Selenium WebDriver**: Captures live website screenshots  
- **Applitools Eyes**: Compares and reports visual differences
- **TestNG**: Test execution framework
- **Excel**: Test data management

## 📋 Prerequisites
- Java 21+
- Maven 3.8+
- Chrome Browser
- Applitools Account
- Figma Access Token

## ⚙️ Setup

### 1. Clone Repository
```bash
git clone <your-repo-url>
cd figma-applitools-selenium
```

### 2. Configure API Keys
```bash
# Copy template
cp src/main/resources/config.properties.template src/main/resources/config.properties
cp src/test/resources/config.properties.template src/test/resources/config.properties

# Edit and add your keys
nano src/main/resources/config.properties
```

Add your credentials:
- `applitools.api.key`: Get from https://eyes.applitools.com
- `figma.apiToken`: Get from Figma Settings → Personal Access Tokens

### 3. Install Dependencies
```bash
mvn clean install
```

## 🚀 Running Tests

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test
```bash
mvn test -Dtest=FigmaComparisonTest
```

## 📊 Test Data
Edit test cases in: `src/test/resources/testdata/testdata.xlsx`

**Sheets:**
- **TestCases**: Main test definitions
- **TestSteps**: Step-by-step actions for each test

## 📁 Project Structure
```
figma-applitools-selenium/
├── src/
│   ├── main/
│   │   ├── java/com/bajajfinserv/
│   │   │   ├── config/         # Configuration readers
│   │   │   ├── reports/        # Extent Reports
│   │   │   └── utils/          # Utilities (Figma, Applitools, Driver)
│   │   └── resources/
│   │       └── config.properties.template
│   └── test/
│       ├── java/com/bajajfinserv/tests/
│       │   ├── BaseTest.java
│       │   └── FigmaComparisonTest.java
│       └── resources/
│           ├── config.properties.template
│           ├── testdata/testdata.xlsx
│           └── testng.xml
├── pom.xml
└── README.md
```

## 🐛 Troubleshooting

### Issue: Zscaler Blocking Uploads
**Solution:** Run on non-enterprise network or request IT to whitelist:
- `*.applitools.com`
- `eyesapi.applitools.com`
- `ufg-wus.applitools.com`

### Issue: Tests Not Executing
**Solution:** Check Excel for:
- Proper action names (case-sensitive)
- Valid step orders
- Correct locators

## 📸 Viewing Results
Dashboard: https://eyes.applitools.com

## 👥 Team
DCX QA Team - Bajaj Finserv

## 📄 License
Internal Use Only
