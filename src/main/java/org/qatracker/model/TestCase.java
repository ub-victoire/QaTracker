package org.qatracker.model;

import java.util.Objects;

/** Shared state and rules for every kind of test case. */
public abstract class TestCase {
    private final int id;
    private final String title;
    private final String description;
    private final String preconditions;
    private final String expectedResult;
    private TestStatus status;

    protected TestCase(int id, String title, String description,
                       String preconditions, String expectedResult, TestStatus status) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive number.");
        }
        this.id = id;
        this.title = requireText(title, "Title");
        this.description = clean(description);
        this.preconditions = clean(preconditions);
        this.expectedResult = requireText(expectedResult, "Expected result");
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        return value.strip();
    }

    protected static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPreconditions() { return preconditions; }
    public String getExpectedResult() { return expectedResult; }
    public TestStatus getStatus() { return status; }

    public void updateStatus(TestStatus status) {
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public abstract TestType getType();
    public abstract String getDetailLabel();
    public abstract String getDetail();
}
