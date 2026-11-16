package org.qatracker;

import org.qatracker.model.AutomatedTestCase;
import org.qatracker.model.ManualTestCase;
import org.qatracker.model.TestCase;
import org.qatracker.model.TestStatus;
import org.qatracker.model.TestType;
import org.qatracker.service.TestCaseService;
import org.qatracker.storage.TestCaseFileStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/** Dependency-free checks: failures throw AssertionError, even without -ea. */
public final class QaTrackerTests {
    private static int checks;

    private QaTrackerTests() {
    }

    public static void main(String[] args) throws Exception {
        testValidationAndSearch();
        testReport();
        Path directory = Files.createTempDirectory("qa-tracker-tests-");
        try {
            testPersistence(directory);
            testCorruptData(directory);
            testSaveFailure(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("PASS: " + checks + " automated checks.");
    }

    private static void testValidationAndSearch() {
        var service = new TestCaseService(List.of());
        rejects(IllegalArgumentException.class,
                () -> service.add("  ", "", "", "Expected", TestType.MANUAL, ""));
        rejects(IllegalArgumentException.class,
                () -> service.add("Title", "", "", "\t", TestType.MANUAL, ""));
        rejects(IllegalArgumentException.class,
                () -> service.add("Title", "", "", "Expected", null, ""));
        check(service.getAll().isEmpty(), "Invalid additions must not create records");
        TestCase manual = service.add(" Login works ", "Valid credentials", "Account exists",
                " Dashboard opens ", TestType.MANUAL, "Alex");
        TestCase automated = service.add("Search", "Find the LOGIN field", "",
                "Found", TestType.AUTOMATED, "tests/search.java");
        check(manual.getId() == 1 && automated.getId() == 2, "IDs are sequential");
        check(manual.getTitle().equals("Login works"), "Title is trimmed");
        check(manual.getExpectedResult().equals("Dashboard opens"), "Expected is trimmed");
        check(manual.getStatus() == TestStatus.NOT_RUN, "Default status");
        check(manual instanceof ManualTestCase, "Manual subclass");
        check(automated instanceof AutomatedTestCase, "Automated subclass");
        check(service.search("lOgIn").size() == 2, "Case-insensitive title/description search");
        check(service.search("  ").size() == 2, "Empty search returns all");
        check(service.search("missing").isEmpty(), "No search results");
        rejects(IllegalArgumentException.class, () -> service.updateStatus(999, TestStatus.PASS));
        rejects(IllegalArgumentException.class, () -> service.updateStatus(1, null));
        for (TestStatus status : TestStatus.values()) {
            service.updateStatus(1, status);
            check(manual.getStatus() == status, "Status update " + status);
        }
        rejects(UnsupportedOperationException.class, () -> service.getAll().clear());
        rejects(IllegalArgumentException.class,
                () -> new TestCaseService(List.of(manual, manual)));
        rejects(IllegalArgumentException.class,
                () -> new ManualTestCase(0, "T", "", "", "E", TestStatus.NOT_RUN, ""));
        var full = new TestCaseService(List.of(new ManualTestCase(Integer.MAX_VALUE,
                "T", "", "", "E", TestStatus.PASS, "")));
        rejects(IllegalArgumentException.class,
                () -> full.add("T", "", "", "E", TestType.MANUAL, ""));
    }

    private static void testReport() {
        var service = new TestCaseService(List.of());
        var empty = service.generateReport();
        check(empty.total() == 0 && empty.manual() == 0 && empty.automated() == 0
                && empty.passed() == 0 && empty.failed() == 0 && empty.blocked() == 0
                && empty.notRun() == 0 && empty.passRate() == 0.0, "Zero report");
        for (int i = 1; i <= 8; i++) {
            service.add("Case " + i, "", "", "Expected",
                    i <= 5 ? TestType.MANUAL : TestType.AUTOMATED, "");
            service.updateStatus(i, i <= 5 ? TestStatus.PASS
                    : i <= 7 ? TestStatus.FAIL : TestStatus.BLOCKED);
        }
        var report = service.generateReport();
        check(report.total() == 8 && report.manual() == 5 && report.automated() == 3,
                "Report type counts");
        check(report.passed() == 5 && report.failed() == 2 && report.blocked() == 1
                && report.notRun() == 0, "Report status counts");
        check(report.passRate() == 62.5, "Pass rate denominator includes all cases");
        service.add("Not run", "", "", "E", TestType.MANUAL, "");
        check(service.generateReport().notRun() == 1, "NOT_RUN count");
    }

    private static void testPersistence(Path directory) throws IOException {
        Path path = directory.resolve("nested/testcases.xml");
        var store = new TestCaseFileStore(path);
        check(store.load().isEmpty(), "Missing file starts empty");
        store.save(List.of());
        check(store.load().isEmpty(), "Empty round trip");
        var service = new TestCaseService(List.of());
        String special = "Victoire & Aine \u00e9 < > \" quotes | pipe\nsecond line\tvalue";
        TestCase first = service.add("Login <&>", special, "User exists",
                "Dashboard & welcome", TestType.MANUAL, "Tester \u00e9");
        service.updateStatus(first.getId(), TestStatus.BLOCKED);
        service.add("API", "Description", "", "200", TestType.AUTOMATED,
                "scripts/path with spaces/test.java");
        service.add("Third", "", "", "E", TestType.MANUAL, "");
        store.save(service.getAll());
        var loaded = store.load();
        check(loaded.size() == 3, "Three-record round trip");
        TestCase restored = loaded.get(0);
        check(restored.getId() == 1, "ID survives");
        check(restored.getTitle().equals("Login <&>"), "Title survives");
        check(restored.getDescription().equals(special), "Unicode and delimiters survive");
        check(restored.getPreconditions().equals("User exists"), "Preconditions survive");
        check(restored.getExpectedResult().equals("Dashboard & welcome"), "Expected survives");
        check(restored instanceof ManualTestCase && restored.getDetail().equals("Tester \u00e9"),
                "Manual type/detail survive");
        check(restored.getStatus() == TestStatus.BLOCKED, "Status survives");
        check(loaded.get(1) instanceof AutomatedTestCase
                && loaded.get(1).getDetail().equals("scripts/path with spaces/test.java"),
                "Automated type/detail survive");
        check(loaded.get(2).getDetail().isEmpty(), "Optional detail survives");
        var restarted = new TestCaseService(loaded);
        check(restarted.add("Next", "", "", "E", TestType.MANUAL, "").getId() == 4,
                "ID continues after reload");
        store.save(restarted.getAll());
        check(store.load().size() == 4, "Replace existing save");
        var gapped = new TestCaseService(List.of(new ManualTestCase(42,
                "T", "", "", "E", TestStatus.PASS, "")));
        check(gapped.add("T", "", "", "E", TestType.MANUAL, "").getId() == 43,
                "ID uses maximum, not list size");
    }

    private static void testCorruptData(Path directory) throws IOException {
        Path path = directory.resolve("corrupt.xml");
        var store = new TestCaseFileStore(path);
        Files.writeString(path, "not valid XML");
        rejects(IOException.class, store::load);
        check(Files.readString(path).equals("not valid XML"), "Load failure preserves file");
        var service = new TestCaseService(List.of());
        service.add("T", "", "", "E", TestType.MANUAL, "");
        service.add("T2", "", "", "E", TestType.AUTOMATED, "");
        store.save(service.getAll());
        var original = new Properties();
        try (var input = Files.newInputStream(path)) {
            original.loadFromXML(input);
        }
        for (String[] change : new String[][] {
                {"case.0.status", "INVALID"}, {"case.0.type", "INVALID"},
                {"case.0.title", " "}, {"case.0.expectedResult", " "},
                {"case.0.id", "0"}, {"case.0.id", "xyz"},
                {"case.1.id", "1"}, {"count", "-1"}, {"format.version", "99"}}) {
            var corrupt = new Properties();
            corrupt.putAll(original);
            corrupt.setProperty(change[0], change[1]);
            try (var output = Files.newOutputStream(path)) {
                corrupt.storeToXML(output, "Invalid test fixture", "UTF-8");
            }
            rejects(IOException.class, store::load);
        }
        original.remove("case.0.detail");
        try (var output = Files.newOutputStream(path)) {
            original.storeToXML(output, "Missing field fixture", "UTF-8");
        }
        rejects(IOException.class, store::load);
    }

    private static void testSaveFailure(Path directory) throws IOException {
        Path blocker = directory.resolve("not-a-directory");
        Files.writeString(blocker, "keep this");
        var store = new TestCaseFileStore(blocker.resolve("testcases.xml"));
        rejects(IOException.class, () -> store.save(new ArrayList<>()));
        check(Files.readString(blocker).equals("keep this"), "Save failure preserves existing file");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static void rejects(Class<? extends Exception> expected, CheckedAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            check(expected.isInstance(exception), "Expected " + expected + " but got " + exception);
            return;
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
