package com.deadlock.detector.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 线程详情
 */
public class ThreadDetail {

    private final long threadId;
    private final String threadName;
    private final String threadState;
    private final String lockName;
    private final long lockOwnerId;
    private final String lockOwnerName;
    private final String waitingForLock;
    private final List<String> heldLocks;
    private final StackTraceElement[] stackTrace;
    private final long blockTimeMs;

    private ThreadDetail(Builder builder) {
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.threadState = builder.threadState;
        this.lockName = builder.lockName;
        this.lockOwnerId = builder.lockOwnerId;
        this.lockOwnerName = builder.lockOwnerName;
        this.waitingForLock = builder.waitingForLock;
        this.heldLocks = builder.heldLocks != null ? builder.heldLocks : Collections.emptyList();
        this.stackTrace = builder.stackTrace;
        this.blockTimeMs = builder.blockTimeMs;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getThreadState() {
        return threadState;
    }

    public String getLockName() {
        return lockName;
    }

    public long getLockOwnerId() {
        return lockOwnerId;
    }

    public String getLockOwnerName() {
        return lockOwnerName;
    }

    public String getWaitingForLock() {
        return waitingForLock;
    }

    public List<String> getHeldLocks() {
        return heldLocks;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public long getBlockTimeMs() {
        return blockTimeMs;
    }

    public String getStackTraceString() {
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    public static class Builder {
        private long threadId;
        private String threadName;
        private String threadState;
        private String lockName;
        private long lockOwnerId = -1;
        private String lockOwnerName;
        private String waitingForLock;
        private List<String> heldLocks;
        private StackTraceElement[] stackTrace;
        private long blockTimeMs;

        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder threadState(String threadState) {
            this.threadState = threadState;
            return this;
        }

        public Builder lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder lockOwnerId(long lockOwnerId) {
            this.lockOwnerId = lockOwnerId;
            return this;
        }

        public Builder lockOwnerName(String lockOwnerName) {
            this.lockOwnerName = lockOwnerName;
            return this;
        }

        public Builder waitingForLock(String waitingForLock) {
            this.waitingForLock = waitingForLock;
            return this;
        }

        public Builder heldLocks(List<String> heldLocks) {
            this.heldLocks = heldLocks;
            return this;
        }

        public Builder stackTrace(StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder blockTimeMs(long blockTimeMs) {
            this.blockTimeMs = blockTimeMs;
            return this;
        }

        public ThreadDetail build() {
            return new ThreadDetail(this);
        }
    }
}
