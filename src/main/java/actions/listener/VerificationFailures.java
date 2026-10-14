package actions.listener;

import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe collection holding verification failures per test execution.
 * Backed by ConcurrentHashMap and CopyOnWriteArrayList to prevent ConcurrentModificationException in parallel runs.
 */
public class VerificationFailures extends ConcurrentHashMap<ITestResult, List<Throwable>> {
    private static final long serialVersionUID = 1L;
    private static final VerificationFailures INSTANCE = new VerificationFailures();

    private VerificationFailures() {
        super();
    }

    public static VerificationFailures getFailures() {
        return INSTANCE;
    }

    public List<Throwable> getFailuresForTest(ITestResult result) {
        if (result == null) return new ArrayList<>();
        List<Throwable> exceptions = get(result);
        return exceptions == null ? new ArrayList<>() : exceptions;
    }

    public void addFailureForTest(ITestResult result, Throwable throwable) {
        if (result == null || throwable == null) return;
        computeIfAbsent(result, k -> new CopyOnWriteArrayList<>()).add(throwable);
    }
}
