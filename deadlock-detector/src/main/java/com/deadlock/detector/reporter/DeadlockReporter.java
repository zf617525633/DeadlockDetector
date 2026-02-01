package com.deadlock.detector.reporter;

import com.deadlock.detector.model.DeadlockReport;

/**
 * 死锁报告输出接口
 */
public interface DeadlockReporter {

    /**
     * 输出死锁报告
     *
     * @param report 死锁报告
     */
    void report(DeadlockReport report);
}
