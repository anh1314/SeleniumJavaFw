package actions.common;

import actions.listener.TestListener;
import actions.drivers.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.*;

import java.io.File;
import java.lang.reflect.Method;

import static actions.common.BasePage.logInfo;

@Listeners(TestListener.class)
public class BaseTest {
    @BeforeMethod
    @Parameters({"BROWSER", "HEADLESS"})
    public void createDriver(Method method, @Optional("chrome") String browserName, @Optional("false") boolean isHeadLess) {
        String testName = method.getName();
        // Điều kiện skip browser
        if (testName.contains("DuyetDataByApi") || testName.contains("CheckNumberOfUse") || (testName.contains("verifyData"))) {
            System.out.println("Skip browser startup for: " + testName);
            return;
        }

        WebDriver driver = setupBrowser(browserName, isHeadLess);
        DriverManager.setDriverThreadLocal(driver);
    }

    public WebDriver setupBrowser(String browserName, boolean isHeadless) {
        WebDriver driver;
        switch (browserName.trim().toLowerCase()) {
            case "chrome":
                driver = initChromeDriver(isHeadless);
                break;
            case "firefox":
                driver = initFirefoxDriver(isHeadless);
                break;
            case "edge":
                driver = initEdgeDriver(isHeadless);
                break;
            default:
                System.out.println("Browser: " + browserName + " is invalid, Launching Chrome as browser of choice...");
                driver = initChromeDriver(isHeadless);
        }
        return driver;
    }


    private WebDriver initChromeDriver(boolean isHeadless) {
        ChromeOptions options = new ChromeOptions();
        if (isHeadless) options.addArguments("--headless=new");

        options.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--disable-extensions",
                "--disable-notifications", "--remote-allow-origins=*",
                "--window-size=1920,1080", "--lang=vi",
                "--disable-blink-features=AutomationControlled");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        return driver;
    }

    private WebDriver initEdgeDriver(boolean isHeadless) {
        EdgeOptions options = new EdgeOptions();
        if (isHeadless) options.addArguments("--headless");
        WebDriver driver = new EdgeDriver(options);
        driver.manage().window().maximize();
        return driver;
    }

    private WebDriver initFirefoxDriver(boolean isHeadless) {
        FirefoxOptions options = new FirefoxOptions();
        if (isHeadless) {
            options.addArguments("--headless");
        }
        WebDriver driver = new FirefoxDriver(options);
        driver.manage().window().maximize();
        return driver;
    }

    @AfterMethod(alwaysRun = true)
    public void closeDriver() {
        DriverManager.quitDriver();
    }

    @BeforeSuite
    public void initializeSuite() {
        deleteAllFileInAllureReportFolder();
        DriverManager.killAllStaleDrivers();
    }

    private void deleteAllFileInAllureReportFolder() {
        logInfo("---Delete file in folder allure report ---");
        String pathFolderDownload = GlobalConstants.PROJECT_PATH + "/allure-results";
        File folder = new File(pathFolderDownload);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) f.delete();
                }
            }
        } else {
            System.out.println("Folder not found, skipping: " + pathFolderDownload);
        }
    }



}