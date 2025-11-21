# Figma Applitools Visual Testing Framework

Automated visual testing framework that compares Figma designs with live website implementations using Applitools Eyes and Selenium.

## 🎯 Features

- 📸 Upload Figma designs as baselines to Applitools
- 🌐 Capture website screenshots using Selenium WebDriver
- ✅ Visual comparison between Figma designs and website
- 📊 Excel-driven test data management
- 🎨 Support for multiple viewports and match levels
- 📋 Comprehensive test reporting

## 🛠️ Tech Stack

- **Java 21**
- **Selenium WebDriver** - Browser automation
- **Applitools Eyes SDK** - Visual testing
- **TestNG** - Test framework
- **Apache POI** - Excel data handling
- **Maven** - Build management

## 📂 Project Structure
```
figma-applitools-selenium/
├── src/main/java/com/bajajfinserv/
│   └── utils/
│       ├── ApplitoolsManager.java
│       ├── DriverManager.java
│       ├── FigmaAPIClient.java
│       └── ExcelDataProvider.java
├── src/test/java/com/bajajfinserv/
│   ├── tests/
│   │   ├── BaseTest.java
│   │   └── FigmaComparisonTest.java
│   ├── listeners/
│   │   ├── TestListener.java
│   │   └── ExtentManager.java
│   └── utils/
│       └── ExcelDataProviderMain.java
├── src/test/resources/
│   ├── testdata.xlsx
│   └── testng.xml
└── pom.xml
```

## ⚙️ Setup

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Chrome browser

### Installation

1. **Clone the repository:**
```bash
   git clone https://github.com/star-dust96/figma-applitools-selenium.git
   cd figma-applitools-selenium
```

2. **Install dependencies:**
```bash
   mvn clean install
```

3. **Configure API keys:**
   
   Create `src/main/resources/config.properties`:
```properties
   APPLITOOLS_API_KEY=your_applitools_api_key_here
   FIGMA_ACCESS_TOKEN=your_figma_access_token_here
```

4. **Update test data:**
   - Edit: `src/test/resources/testdata.xlsx`
   - Configure TestCases and TestSteps sheets

## 🚀 Running Tests

### Phase 1: Upload Figma Baselines

Set `uploadBaseline=TRUE` in Excel, then run:
```bash
mvn clean test
```

### Phase 2: Compare Website vs Baselines

Set `uploadBaseline=FALSE` in Excel, then run:
```bash
mvn clean test
```

## 📊 Test Data Configuration

### TestCases Sheet

| Column | Description |
|--------|-------------|
| testName | Unique test identifier |
| figmaUrl | Figma design URL with node-id |
| appUrl | Website URL to test |
| viewport | Browser dimensions (e.g., 375x740) |
| matchLevel | STRICT, LAYOUT, CONTENT, or EXACT |
| uploadBaseline | TRUE/FALSE - Upload Figma or compare |
| enabled | TRUE/FALSE - Run this test |

### TestSteps Sheet

| Column | Description |
|--------|-------------|
| testName | Links to TestCases |
| stepOrder | Execution sequence |
| action | NAVIGATE, WAIT, CLICK, SCROLL, etc. |
| locator | Element selector (CSS/XPath) |
| checkpointName | Screenshot name in Applitools |
| waitSeconds | Delay duration |

## 📈 Reports

- **Test reports:** `test-output/Figma-Visual-Testing-Report-[timestamp].html`
- **Applitools Dashboard:** https://eyes.applitools.com

## 🔒 Security

- Never commit `config.properties` files
- Keep API keys secure
- Use `.gitignore` to protect sensitive data

## 👥 Contributing

This is a private project for Bajaj Finserv DCX team.

## 📝 License

Private - Bajaj Finserv

---

**Developed by QA Team - DCX**
