package org.qatracker.service;

import org.qatracker.model.AutomatedTestCase;
import org.qatracker.model.ExecutionReport;
import org.qatracker.model.ManualTestCase;
import org.qatracker.model.TestCase;
import org.qatracker.model.TestStatus;
import org.qatracker.model.TestType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Business rules; no terminal input and no file operations. */
public final class TestCaseService {
    private final List<TestCase> testCases = new ArrayList<>();
    private long nextId = 1;

    public TestCaseService(List<TestCase> initialCases) {
        var ids = new HashSet<Integer>();
        for (TestCase testCase : initialCases) {
            Objects.requireNonNull(testCase, "Test case cannot be null.");
            if (!ids.add(testCase.getId())) {
                throw new IllegalArgumentException("Duplicate test case ID: " + testCase.getId());
            }
            testCases.add(testCase);
            nextId = Math.max(nextId, (long) testCase.getId() + 1);
        }
    }

    public TestCase add(String title, String description, String preconditions,
                        String expectedResult, TestType type, String detail) {
        if (type == null) {
            throw new IllegalArgumentException("Type must be Manual or Automated.");
        }
        if (nextId > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("No more test case IDs are available.");
        }
        int id = (int) nextId;
        TestCase testCase = switch (type) {
            case MANUAL -> new ManualTestCase(id, title, description, preconditions,
                    expectedResult, TestStatus.NOT_RUN, detail);
            case AUTOMATED -> new AutomatedTestCase(id, title, description, preconditions,
                    expectedResult, TestStatus.NOT_RUN, detail);
        };
        testCases.add(testCase);
        nextId++; // Only consume an ID after successful validation and creation.
        return testCase;
    }

    public List<TestCase> getAll() {
        return List.copyOf(testCases);
    }

    public TestCase findById(int id) {
        for (TestCase testCase : testCases) {
            if (testCase.getId() == id) {
                return testCase;
            }
        }
        throw new IllegalArgumentException("No test case exists with ID " + id + ".");
    }

    public void updateStatus(int id, TestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Choose one of the four defined statuses.");
        }
        findById(id).updateStatus(status);
    }

    public List<TestCase> search(String keyword) {
        String query = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        var results = new ArrayList<TestCase>();
        for (TestCase testCase : testCases) {
            if (testCase.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || testCase.getDescription().toLowerCase(Locale.ROOT).contains(query)) {
                results.add(testCase);
            }
        }
        return results;
    }

    public ExecutionReport generateReport() {
        int manual = 0;
        int automated = 0;
        int passed = 0;
        int failed = 0;
        int blocked = 0;
        int notRun = 0;
        for (TestCase testCase : testCases) {
            if (testCase.getType() == TestType.MANUAL) {
                manual++;
            } else {
                automated++;
            }
            switch (testCase.getStatus()) {
                case PASS -> passed++;
                case FAIL -> failed++;
                case BLOCKED -> blocked++;
                case NOT_RUN -> notRun++;
            }
        }
        return new ExecutionReport(testCases.size(), manual, automated,
                passed, failed, blocked, notRun);
    }
}
