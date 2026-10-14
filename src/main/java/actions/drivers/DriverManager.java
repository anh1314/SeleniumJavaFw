package actions.drivers;

import org.openqa.selenium.WebDriver;

import java.util.concurrent.TimeUnit;

import static actions.common.BasePage.*;

public class DriverManager {
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static void setDriverThreadLocal(WebDriver driverThreadLocal) {
        DriverManager.driverThreadLocal.set(driverThreadLocal);
    }

    public static void quitDriver() {
        WebDriver driver = getDriver();
        try {
            if (driver != null) driver.quit();
        } catch (Exception e) {
            errorMsg(("⚠️ Error while quitting driver: " + e.getMessage()));
        } finally {
            driverThreadLocal.remove();
        }
    }

    // Chỉ dùng ở @BeforeSuite để kill process drivers cũ
    // Lưu ý: không chạy song song 2 xml tạo scheme
    public static void killAllStaleDrivers() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;
            if (os.contains("win")) {
                process = new ProcessBuilder("cmd.exe", "/c", "taskkill /F /IM chromedriver.exe /T").start();
            } else {
                process = new ProcessBuilder("bash", "-c", "pkill -x chromedriver").start();
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logInfo("⚠️ Kill process timeout");
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                logInfo("💥 Killed stale drivers successfully");
            } else {
                logInfo("⚠️ No stale drivers found (exitCode={})", exitCode);
            }

        } catch (Exception e) {
            errorMsg("Failed to clean stale drivers: {}", e.getStackTrace());
        }
    }

}

