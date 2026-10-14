package actions.listener;

import actions.utils.LogUtils;
import org.testng.*;
import org.testng.internal.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static actions.common.BasePage.captureScreenshot;

public class TestListener implements ITestListener, ISuiteListener, IInvokedMethodListener {

    private static final AtomicInteger count_totalTCs = new AtomicInteger(0);
    private static final AtomicInteger count_passedTCs = new AtomicInteger(0);
    private static final AtomicInteger count_skippedTCs = new AtomicInteger(0);
    private static final AtomicInteger count_failedTCs = new AtomicInteger(0);
    private static final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    public static String getCurrentTestName() {
        return currentTestName.get();
    }

    public String getTestName(ITestResult result) {
        return result.getTestName() != null ? result.getTestName() : result.getMethod().getConstructorOrMethod().getName();
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            currentTestName.set(getTestName(testResult));
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        try {
            Reporter.setCurrentTestResult(result);
            if (method.isTestMethod()) {
                VerificationFailures allFailures = VerificationFailures.getFailures();
                if (result.getThrowable() != null) {
                    allFailures.addFailureForTest(result, result.getThrowable());
                }

                List<Throwable> failures = allFailures.getFailuresForTest(result);
                int size = failures.size() - 1;
                if (size > 0) {
                    result.setStatus(ITestResult.FAILURE);
                    if (size > 1) {
                        StringBuffer message = new StringBuffer("Multiple failures (").append(size).append("):\n");
                        for (int failure = 0; failure < size; failure++) {
                            message.append("Failure ").append(failure + 1).append(" of ").append(size).append("\n");
                            message.append(Utils.shortStackTrace(failures.get(failure), false)).append("\n");
                        }
                    }
                }
            }
        } finally {
            if (method.isTestMethod()) {
                currentTestName.remove();
            }
        }
    }

    @Override
    public void onStart(ISuite iSuite) {
        count_totalTCs.set(0);
        count_passedTCs.set(0);
        count_skippedTCs.set(0);
        count_failedTCs.set(0);

        String xmlEnv = iSuite.getParameter("env");
        if (xmlEnv == null || xmlEnv.trim().isEmpty()) {
            xmlEnv = iSuite.getParameter("ENVIRONMENT");
        }
        actions.common.ConfigLoader.reload(xmlEnv);
        LogUtils.info("Starting Suite: " + iSuite.getName() + " | Active ENV: [" + actions.common.ConfigLoader.getInstance().getEnv().toUpperCase() + "]");
    }

    @Override
    public void onFinish(ISuite iSuite) {
        String suiteName = iSuite.getName();
        Map<String, String> suiteParameters = iSuite.getXmlSuite().getParameters();
        suiteName += " - " + suiteParameters;
        LogUtils.info("End Suite: " + suiteName);
    }

    @Override
    public void onTestStart(ITestResult iTestResult) {
        count_totalTCs.incrementAndGet();

        ISuite suite = iTestResult.getTestContext().getSuite();
        String className = iTestResult.getTestClass().getRealClass().getSimpleName();
        if ("Default Suite".equals(suite.getName())) {
            // Trường hợp không chạy từ file XML
            String currentTestCaseName = "Run " + getTestName(iTestResult) + "_" + className;
            suite.getXmlSuite().setName(currentTestCaseName);
        }
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        count_passedTCs.incrementAndGet();
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        count_failedTCs.incrementAndGet();
        LogUtils.error(iTestResult.getThrowable());
        captureScreenshot();
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        count_skippedTCs.incrementAndGet();
        LogUtils.error(iTestResult.getThrowable());
    }

    public String getTestDescription(ITestResult result) {
        return result.getMethod().getDescription() != null ? result.getMethod().getDescription() : getTestName(result);
    }

    private String getReportTelegram(ISuite iSuite) {
        String suiteName = iSuite.getName();
        long totalExecutionTime = getTotalExecutionTime(iSuite);
        String formattedDuration = formatDuration(totalExecutionTime);

        int total = count_totalTCs.get();
        int passed = count_passedTCs.get();
        int failed = count_failedTCs.get();
        int skipped = count_skippedTCs.get();

        // ====== TÍNH PASS RATE ======
        double passRate = (total == 0) ? 0 : (passed * 100.0 / total);
        StringBuilder msg = new StringBuilder();

        // ====== HEADER ======
        msg.append("🎯 <b>Promotion Automation Report</b>\n \n");
        msg.append("🔹 <b>Suite:</b> <i>").append(suiteName).append("</i>\n\n");
        msg.append("⌛ <b>Duration</b>: ").append(formattedDuration).append("\n\n");

        // ====== SUMMARY ======
        msg.append("📊 <b>Summary</b>\n");
        msg.append("├─ ✅ <i>Passed: </i><b><i>").append(passed)
                .append("</i></b> | ❌ <i>Failed:</i> <b><i>").append(failed)
                .append("</i></b> | ⚠️ <i>Skipped:</i> <b><i>").append(skipped)
                .append("</i></b> | 📦 <i>Total:</i> <b><i>").append(total).append("</i></b>\n");

        msg.append("\n📈 <b>Pass Rate: </b>").append(String.format("<i>%.2f%%</i>\n", passRate));

        // ====== ALERT ======
        if (failed > 0) {
            msg.append("\n🚨 <b>Test Failed Detected!</b>\n");
        } else if (skipped == total && total > 0) {
            msg.append("\n⚠️ <b>All Test Skipped</b>\n");
        } else {
            msg.append("\n🎉 <b>All Test Passed</b>\n");
        }

        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return msg.toString();
    }

    private String formatDuration(long durationMillis) {
        long totalSeconds = durationMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long millis = durationMillis % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    private long getTotalExecutionTime(ISuite iSuite) {
        long minStartTime = Long.MAX_VALUE;
        long maxEndTime = Long.MIN_VALUE;

        for (ISuiteResult result : iSuite.getResults().values()) {
            ITestContext context = result.getTestContext();

            long startTime = context.getStartDate().getTime();
            long endTime = context.getEndDate().getTime();

            if (startTime < minStartTime) minStartTime = startTime;
            if (endTime > maxEndTime) maxEndTime = endTime;
        }
        return maxEndTime - minStartTime;
    }
}
