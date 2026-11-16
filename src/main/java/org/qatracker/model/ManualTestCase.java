package org.qatracker.model;

public final class ManualTestCase extends TestCase {
    private final String testerName;

    public ManualTestCase(int id, String title, String description, String preconditions,
                          String expectedResult, TestStatus status, String testerName) {
        super(id, title, description, preconditions, expectedResult, status);
        this.testerName = clean(testerName);
    }

    @Override
    public TestType getType() { return TestType.MANUAL; }

    @Override
    public String getDetailLabel() { return "Tester"; }

    @Override
    public String getDetail() { return testerName; }
}
