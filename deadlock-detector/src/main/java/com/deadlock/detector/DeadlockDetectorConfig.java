package com.deadlock.detector;

/**
 * 死锁检测配置
 */
public final class DeadlockDetectorConfig {

    private final boolean detectSynchronized;
    private final boolean detectReentrantLock;
    private final boolean detectANR;
    private final long detectionIntervalMs;
    private final long initialDelayMs;
    private final long anrThresholdMs;

    private DeadlockDetectorConfig(Builder builder) {
        this.detectSynchronized = builder.detectSynchronized;
        this.detectReentrantLock = builder.detectReentrantLock;
        this.detectANR = builder.detectANR;
        this.detectionIntervalMs = builder.detectionIntervalMs;
        this.initialDelayMs = builder.initialDelayMs;
        this.anrThresholdMs = builder.anrThresholdMs;
    }

    /**
     * 获取默认配置
     */
    public static DeadlockDetectorConfig getDefault() {
        return new Builder().build();
    }

    public boolean isDetectSynchronized() {
        return detectSynchronized;
    }

    public boolean isDetectReentrantLock() {
        return detectReentrantLock;
    }

    public boolean isDetectANR() {
        return detectANR;
    }

    public long getDetectionIntervalMs() {
        return detectionIntervalMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getAnrThresholdMs() {
        return anrThresholdMs;
    }

    public static class Builder {
        private boolean detectSynchronized = true;
        private boolean detectReentrantLock = true;
        private boolean detectANR = true;
        private long detectionIntervalMs = 5000;  // 5秒检测一次
        private long initialDelayMs = 3000;       // 启动后3秒开始
        private long anrThresholdMs = 5000;       // ANR阈值5秒

        public Builder() {
        }

        /**
         * 是否检测synchronized死锁
         */
        public Builder detectSynchronized(boolean detect) {
            this.detectSynchronized = detect;
            return this;
        }

        /**
         * 是否检测ReentrantLock死锁
         */
        public Builder detectReentrantLock(boolean detect) {
            this.detectReentrantLock = detect;
            return this;
        }

        /**
         * 是否检测ANR
         */
        public Builder detectANR(boolean detect) {
            this.detectANR = detect;
            return this;
        }

        /**
         * 设置检测间隔（毫秒）
         */
        public Builder detectionInterval(long intervalMs) {
            this.detectionIntervalMs = intervalMs;
            return this;
        }

        /**
         * 设置初始延迟（毫秒）
         */
        public Builder initialDelay(long delayMs) {
            this.initialDelayMs = delayMs;
            return this;
        }

        /**
         * 设置ANR阈值（毫秒）
         */
        public Builder anrThreshold(long thresholdMs) {
            this.anrThresholdMs = thresholdMs;
            return this;
        }

        public DeadlockDetectorConfig build() {
            return new DeadlockDetectorConfig(this);
        }
    }
}
