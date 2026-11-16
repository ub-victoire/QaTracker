package org.qatracker.model;

/** An immutable report value: records provide accessors such as total(). */
public record ExecutionReport(int total, int manual, int automated,
                              int passed, int failed, int blocked, int notRun) {
    public double passRate() {
        return total == 0 ? 0.0 : 100.0 * passed / total;
    }
}
