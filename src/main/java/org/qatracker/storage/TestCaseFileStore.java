package org.qatracker.storage;

import org.qatracker.model.AutomatedTestCase;
import org.qatracker.model.ManualTestCase;
import org.qatracker.model.TestCase;
import org.qatracker.model.TestStatus;
import org.qatracker.model.TestType;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/** JDK XML properties handle escaping, Unicode, and multiline fields for us. */
public final class TestCaseFileStore {
    private final Path file;

    public TestCaseFileStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    public List<TestCase> load() throws IOException {
        // A genuinely missing file is normal; other read errors must not be hidden.
        var properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.loadFromXML(input);
        } catch (java.nio.file.NoSuchFileException exception) {
            return List.of();
        }

        try {
            if (!"1".equals(required(properties, "format.version"))) {
                throw new IllegalArgumentException("Unsupported file format version.");
            }
            int count = Integer.parseInt(required(properties, "count"));
            if (count < 0 || count > (properties.size() - 2) / 8) {
                throw new IllegalArgumentException("Invalid record count.");
            }
            var result = new ArrayList<TestCase>();
            var ids = new HashSet<Integer>();
            for (int i = 0; i < count; i++) {
                String prefix = "case." + i + ".";
                int id = Integer.parseInt(required(properties, prefix + "id"));
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("Duplicate ID " + id + ".");
                }
                String title = required(properties, prefix + "title");
                String description = required(properties, prefix + "description");
                String preconditions = required(properties, prefix + "preconditions");
                String expected = required(properties, prefix + "expectedResult");
                TestType type = TestType.valueOf(required(properties, prefix + "type"));
                TestStatus status = TestStatus.valueOf(required(properties, prefix + "status"));
                String detail = required(properties, prefix + "detail");
                TestCase testCase = switch (type) {
                    case MANUAL -> new ManualTestCase(id, title, description, preconditions,
                            expected, status, detail);
                    case AUTOMATED -> new AutomatedTestCase(id, title, description, preconditions,
                            expected, status, detail);
                };
                result.add(testCase);
            }
            return result;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid saved data: " + exception.getMessage(), exception);
        }
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field " + key + ".");
        }
        return value;
    }

    public void save(List<TestCase> testCases) throws IOException {
        var properties = new Properties();
        properties.setProperty("format.version", "1");
        properties.setProperty("count", Integer.toString(testCases.size()));
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            String prefix = "case." + i + ".";
            properties.setProperty(prefix + "id", Integer.toString(testCase.getId()));
            properties.setProperty(prefix + "title", testCase.getTitle());
            properties.setProperty(prefix + "description", testCase.getDescription());
            properties.setProperty(prefix + "preconditions", testCase.getPreconditions());
            properties.setProperty(prefix + "expectedResult", testCase.getExpectedResult());
            properties.setProperty(prefix + "type", testCase.getType().name());
            properties.setProperty(prefix + "status", testCase.getStatus().name());
            properties.setProperty(prefix + "detail", testCase.getDetail());
        }
        // Write completely to a sibling file before replacing the previous save.
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "qa-tracker-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                properties.storeToXML(output, "QA Tracker data - format version 1", "UTF-8");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
