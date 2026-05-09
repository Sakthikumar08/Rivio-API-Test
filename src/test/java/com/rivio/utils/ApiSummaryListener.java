package com.rivio.utils;

import org.testng.IExecutionListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints a clean per-API (per <test> block in testng.xml) pass/fail summary
 * after the suite finishes, plus an overall total.
 *
 * Example output:
 *   ╔══════════════════════════════════════════════════════════════════╗
 *   ║                  RIVIO HRMS API TEST SUMMARY                     ║
 *   ╠══════════════════════════════════════════════════════════════════╣
 *   ║ API Module                                  Pass  Fail  Skip Tot ║
 *   ╠══════════════════════════════════════════════════════════════════╣
 *   ║ TC01 - Authentication and Users               7     1     0   8 ║
 *   ║ ...                                                              ║
 *   ╠══════════════════════════════════════════════════════════════════╣
 *   ║ TOTAL                                        58     4     2  64 ║
 *   ╚══════════════════════════════════════════════════════════════════╝
 */
public class ApiSummaryListener implements ITestListener, ISuiteListener, IExecutionListener {

    private static final Map<String, int[]> STATS = new ConcurrentHashMap<>();
    // index: 0=pass, 1=fail, 2=skip

    @Override
    public void onTestSuccess(ITestResult result) {
        bump(result.getTestContext().getName(), 0);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        bump(result.getTestContext().getName(), 1);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        bump(result.getTestContext().getName(), 2);
    }

    private static void bump(String testName, int idx) {
        STATS.computeIfAbsent(testName, k -> new int[3])[idx]++;
    }

    @Override
    public void onFinish(ISuite suite) {
        printSummary();
    }

    @Override
    public void onExecutionFinish() {
        // Fallback in case onFinish(ISuite) was already invoked — still safe.
    }

    private void printSummary() {
        // Preserve insertion order from testng.xml as best as possible
        Map<String, int[]> ordered = new LinkedHashMap<>(STATS);
        int totPass = 0, totFail = 0, totSkip = 0;

        StringBuilder out = new StringBuilder();
        out.append('\n');
        out.append("╔══════════════════════════════════════════════════════════════════════════╗\n");
        out.append("║                     RIVIO HRMS API TEST SUMMARY                          ║\n");
        out.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
        out.append(String.format("║ %-50s %5s %5s %5s %5s ║%n",
                "API Module", "Pass", "Fail", "Skip", "Tot"));
        out.append("╠══════════════════════════════════════════════════════════════════════════╣\n");

        for (Map.Entry<String, int[]> e : ordered.entrySet()) {
            int[] s = e.getValue();
            int total = s[0] + s[1] + s[2];
            totPass += s[0];
            totFail += s[1];
            totSkip += s[2];
            String name = e.getKey();
            if (name.length() > 50) name = name.substring(0, 47) + "...";
            String marker = s[1] == 0 ? "✓" : "✗";
            out.append(String.format("║ %s %-48s %5d %5d %5d %5d ║%n",
                    marker, name, s[0], s[1], s[2], total));
        }

        int totAll = totPass + totFail + totSkip;
        out.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
        out.append(String.format("║ %-50s %5d %5d %5d %5d ║%n",
                "TOTAL", totPass, totFail, totSkip, totAll));
        out.append("╚══════════════════════════════════════════════════════════════════════════╝\n");

        if (totAll > 0) {
            double rate = (totPass * 100.0) / totAll;
            out.append(String.format("Pass rate: %.1f%%   |   %d passed, %d failed, %d skipped, %d total%n",
                    rate, totPass, totFail, totSkip, totAll));
        } else {
            out.append("No tests were executed.\n");
        }

        System.out.println(out.toString());
    }

    // Unused defaults
    @Override public void onTestStart(ITestResult result) { }
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }
    @Override public void onStart(ITestContext context) { }
    @Override public void onFinish(ITestContext context) { }
    @Override public void onStart(ISuite suite) { }
}
