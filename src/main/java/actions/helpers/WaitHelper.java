package actions.helpers;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static actions.common.BasePage.logError;

/**
 * Utility class cung cấp các hàm polling và chờ đợi bất đồng bộ tập trung dựa trên Awaitility.
 */
public class WaitHelper {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);

    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        return millis >= 1000 ? (millis / 1000.0) + "s" : millis + "ms";
    }

    /**
     * Chờ một điều kiện boolean thỏa mãn với timeout (giây) và pollInterval (giây).
     */
    public static boolean waitUntil(Callable<Boolean> condition, long timeoutSecond, long pollIntervalSecond) {
        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(timeoutSecond))
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofSeconds(pollIntervalSecond))
                    .ignoreExceptions()
                    .until(condition);
            return true;
        } catch (ConditionTimeoutException e) {
            logError("⏳ Timeout sau {}s khi chờ điều kiện.", timeoutSecond);
            return false;
        }
    }

    /**
     * Chờ một điều kiện boolean thỏa mãn với timeout (giây), pollInterval mặc định 2 giây.
     */
    public static boolean waitUntil(Callable<Boolean> condition, long timeoutSecond) {
        return waitUntil(condition, timeoutSecond, 2);
    }

    /**
     * Chờ một điều kiện boolean thỏa mãn với timeout và pollInterval dạng Duration.
     */
    public static boolean waitUntil(Callable<Boolean> condition, Duration timeout, Duration pollInterval) {
        try {
            Awaitility.await()
                    .atMost(timeout)
                    .pollDelay(Duration.ZERO)
                    .pollInterval(pollInterval)
                    .ignoreExceptions()
                    .until(condition);
            return true;
        } catch (ConditionTimeoutException e) {
            logError("⏳ Timeout sau {} khi chờ điều kiện.", formatDuration(timeout));
            return false;
        }
    }

    /**
     * Chờ một điều kiện boolean thỏa mãn với pollInterval mặc định (2 giây).
     */
    public static boolean waitUntil(Callable<Boolean> condition, Duration timeout) {
        return waitUntil(condition, timeout, DEFAULT_POLL_INTERVAL);
    }

    /**
     * Chờ một điều kiện boolean thỏa mãn với timeout (10s) và pollInterval (2s) mặc định.
     */
    public static boolean waitUntil(Callable<Boolean> condition) {
        return waitUntil(condition, 10, 2);
    }

    /**
     * Polling dữ liệu từ Supplier cho đến khi thỏa mãn successCondition hoặc hết timeout.
     */
    public static <T> T pollUntil(Supplier<T> supplier, Predicate<T> successCondition, Duration timeout, Duration pollInterval) {
        AtomicReference<T> resultRef = new AtomicReference<>();
        try {
            Awaitility.await()
                    .atMost(timeout)
                    .pollDelay(Duration.ZERO)
                    .pollInterval(pollInterval)
                    .ignoreExceptions()
                    .until(() -> {
                        T result = supplier.get();
                        if (result != null && successCondition.test(result)) {
                            resultRef.set(result);
                            return true;
                        }
                        return false;
                    });
        } catch (ConditionTimeoutException e) {
            logError("⏳ Timeout sau {} khi polling dữ liệu.", formatDuration(timeout));
        }
        return resultRef.get();
    }

    /**
     * Polling dữ liệu từ Supplier với pollInterval mặc định (2 giây).
     */
    public static <T> T pollUntil(Supplier<T> supplier, Predicate<T> successCondition, Duration timeout) {
        return pollUntil(supplier, successCondition, timeout, DEFAULT_POLL_INTERVAL);
    }
}
