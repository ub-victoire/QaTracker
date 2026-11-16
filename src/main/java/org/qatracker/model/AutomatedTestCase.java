package org.qatracker.model;

public final class AutomatedTestCase extends TestCase {
    private final String scriptPath;

    public AutomatedTestCase(int id, String title, String description, String preconditions,
                             String expectedResult, TestStatus status, String scriptPath) {
        super(id, title, description, preconditions, expectedResult, status);
        this.scriptPath = clean(scriptPath);
    }

    @Override
    public TestType getType() { return TestType.AUTOMATED; }

    @Override
    public String getDetailLabel() { return "Script"; }

    @Override
    public String getDetail() { return scriptPath; }
}
