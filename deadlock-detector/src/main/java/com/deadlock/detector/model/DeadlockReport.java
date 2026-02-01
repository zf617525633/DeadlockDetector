package com.deadlock.detector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 死锁报告
 */
public class DeadlockReport {

    private final DeadlockType type;
    private final long timestamp;
    private final String message;
    private final List<ThreadDetail> threadDetails;
    private final List<ThreadDetail> otherThreadDetails;

    private DeadlockReport(Builder builder) {
        this.type = builder.type;
        this.timestamp = builder.timestamp;
        this.message = builder.message;
        this.threadDetails = Collections.unmodifiableList(builder.threadDetails);
        this.otherThreadDetails = Collections.unmodifiableList(builder.otherThreadDetails);
    }

    public DeadlockType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 获取死锁相关的线程详情
     */
    public List<ThreadDetail> getThreadDetails() {
        return threadDetails;
    }

    /**
     * 获取其他线程详情（用于ANR分析）
     */
    public List<ThreadDetail> getOtherThreadDetails() {
        return otherThreadDetails;
    }

    public static class Builder {
        private DeadlockType type;
        private long timestamp;
        private String message;
        private List<ThreadDetail> threadDetails = new ArrayList<>();
        private List<ThreadDetail> otherThreadDetails = new ArrayList<>();

        public Builder type(DeadlockType type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder addThreadDetail(ThreadDetail detail) {
            this.threadDetails.add(detail);
            return this;
        }

        public Builder addOtherThreadDetail(ThreadDetail detail) {
            this.otherThreadDetails.add(detail);
            return this;
        }

        public DeadlockReport build() {
            return new DeadlockReport(this);
        }
    }
}
