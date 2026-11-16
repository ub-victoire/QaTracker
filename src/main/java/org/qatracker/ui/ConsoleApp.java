package org.qatracker.ui;

import org.qatracker.model.TestCase;
import org.qatracker.model.TestStatus;
import org.qatracker.model.TestType;
import org.qatracker.service.TestCaseService;
import org.qatracker.storage.TestCaseFileStore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Terminal interaction only: prompts, input parsing, and formatted output. */
public final class ConsoleApp {
    private final TestCaseService service;
    private final TestCaseFileStore store;
    private final Scanner input;

    public ConsoleApp(TestCaseService service, TestCaseFileStore store, Scanner input) {
        this.service = service;
        this.store = store;
        this.input = input;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            try {
                int choice = readNumber("Enter your choice: ");
                switch (choice) {
                    case 1 -> addTestCase();
                    case 2 -> displayCases(service.getAll(), "No test cases yet.");
                    case 3 -> updateStatus();
                    case 4 -> search();
                    case 5 -> printReport();
                    case 6 -> running = !saveAndExit();
                    default -> System.out.println("Please choose a menu option from 1 to 6.");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println("Error: " + exception.getMessage());
            } catch (NoSuchElementException exception) {
                System.out.println("\nInput closed. Saving completed test cases before exit.");
                if (!saveAndExit()) {
                    System.out.println("Warning: unsaved changes could not be written.");
                }
                running = false;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n==============================");
        System.out.println("       QA TRACKER v1.0");
        System.out.println("==============================");
        System.out.println("1. Add Test Case");
        System.out.println("2. View All Test Cases");
        System.out.println("3. Update Test Status");
        System.out.println("4. Search Test Cases");
        System.out.println("5. Generate Report");
        System.out.println("6. Save & Exit");
        System.out.println("==============================");
    }

    private void addTestCase() {
        String title = TestCase.requireText(readLine("Title: "), "Title");
        String description = readLine("Description (optional): ");
        String preconditions = readLine("Preconditions (optional): ");
        String expected = TestCase.requireText(readLine("Expected result: "), "Expected result");
        TestType type = readType();
        String detail = type == TestType.MANUAL
                ? readLine("Tester name: ") : readLine("Script path: ");
        TestCase testCase = service.add(title, description, preconditions, expected, type, detail);
        System.out.println("Created test case #" + testCase.getId() + " with status NOT_RUN.");
    }

    private TestType readType() {
        while (true) {
            String value = readLine("Type (Manual or Automated): ");
            if (value.equalsIgnoreCase("Manual")) {
                return TestType.MANUAL;
            }
            if (value.equalsIgnoreCase("Automated")) {
                return TestType.AUTOMATED;
            }
            System.out.println("Invalid type. Enter Manual or Automated.");
        }
    }

    private void updateStatus() {
        int id = readNumber("Test case ID: ");
        TestCase testCase = service.findById(id);
        System.out.println("Current status: " + testCase.getStatus());
        System.out.println("1. PASS\n2. FAIL\n3. BLOCKED\n4. NOT_RUN");
        while (true) {
            int choice;
            try {
                choice = readNumber("New status (1-4): ");
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
                continue;
            }
            TestStatus status = switch (choice) {
                case 1 -> TestStatus.PASS;
                case 2 -> TestStatus.FAIL;
                case 3 -> TestStatus.BLOCKED;
                case 4 -> TestStatus.NOT_RUN;
                default -> null;
            };
            if (status == null) {
                System.out.println("Invalid status. Choose a number from 1 to 4.");
                continue;
            }
            service.updateStatus(id, status);
            System.out.println("Updated test case #" + id + " to " + status + ".");
            return;
        }
    }

    private void search() {
        String keyword = readLine("Keyword (empty = all test cases): ");
        displayCases(service.search(keyword), "No matching test cases.");
    }

    private void displayCases(List<TestCase> testCases, String emptyMessage) {
        if (testCases.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        System.out.println();
        System.out.printf("%-10s | %-32s | %-9s | %-7s | %s%n",
                "ID", "TITLE", "TYPE", "STATUS", "TYPE-SPECIFIC DETAIL");
        System.out.println("-".repeat(90));
        for (TestCase testCase : testCases) {
            System.out.printf("%-10d | %-32s | %-9s | %-7s | %s: %s%n",
                    testCase.getId(), cell(testCase.getTitle()), testCase.getType(),
                    testCase.getStatus(), testCase.getDetailLabel(), cell(testCase.getDetail()));
        }
    }

    private String cell(String value) {
        // Keep persisted multiline/control characters from breaking terminal rows.
        return value.replaceAll("[\\p{Cntrl}]", " ");
    }

    private void printReport() {
        var report = service.generateReport();
        System.out.println("\n================================");
        System.out.println("      TEST EXECUTION REPORT");
        System.out.println("================================");
        System.out.println("Total Test Cases : " + report.total());
        System.out.println("Manual           : " + report.manual());
        System.out.println("Automated        : " + report.automated());
        System.out.println("--------------------------------");
        System.out.println("PASS             : " + report.passed());
        System.out.println("FAIL             : " + report.failed());
        System.out.println("BLOCKED          : " + report.blocked());
        System.out.println("NOT_RUN          : " + report.notRun());
        System.out.println("--------------------------------");
        System.out.printf(Locale.ROOT, "Pass Rate        : %.1f%%%n", report.passRate());
        System.out.println("================================");
    }

    private boolean saveAndExit() {
        try {
            store.save(service.getAll());
            System.out.println("Saved " + service.getAll().size() + " test case(s). Goodbye!");
            return true;
        } catch (IOException exception) {
            System.out.println("Could not save: " + exception.getMessage());
            System.out.println("Check the data path and permissions, then try Save & Exit again.");
            return false;
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return input.nextLine().strip();
    }

    private int readNumber(String prompt) {
        try {
            return Integer.parseInt(readLine(prompt));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a whole number within the supported integer range.");
        }
    }
}
