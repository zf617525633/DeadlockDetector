package com.deadlock.detector.detector;

/**
 * 死锁检测器接口
 */
public interface IDeadlockDetector {

    /**
     * 执行检测
     */
    void detect();

    /**
     * 获取检测器名称
     */
    String getName();

    /**
     * 销毁检测器，释放资源
     */
    void destroy();
}
