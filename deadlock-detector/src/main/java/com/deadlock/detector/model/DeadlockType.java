package com.deadlock.detector.model;

/**
 * 死锁类型枚举
 */
public enum DeadlockType {
    /**
     * synchronized关键字导致的死锁
     */
    SYNCHRONIZED("Synchronized Lock Deadlock"),

    /**
     * ReentrantLock等显式锁导致的死锁
     */
    REENTRANT_LOCK("ReentrantLock Deadlock"),

    /**
     * 主线程阻塞(ANR)
     */
    ANR("Application Not Responding");

    private final String description;

    DeadlockType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
