package com.deadlock.detector.detector;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Lock包装器 - 用于追踪锁的获取和释放
 */
public class LockWrapper implements Lock {

    private final Lock delegate;
    private final String name;
    private final ReentrantLockDetector detector;

    public LockWrapper(Lock delegate, String name, ReentrantLockDetector detector) {
        this.delegate = delegate;
        this.name = name;
        this.detector = detector;
    }

    public String getName() {
        return name;
    }

    public Lock getDelegate() {
        return delegate;
    }

    @Override
    public void lock() {
        detector.onBeforeLock(this);
        try {
            delegate.lock();
            detector.onAfterLock(this);
        } catch (Exception e) {
            detector.onLockFailed(this);
            throw e;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        detector.onBeforeLock(this);
        try {
            delegate.lockInterruptibly();
            detector.onAfterLock(this);
        } catch (InterruptedException e) {
            detector.onLockFailed(this);
            throw e;
        }
    }

    @Override
    public boolean tryLock() {
        boolean acquired = delegate.tryLock();
        if (acquired) {
            detector.onAfterLock(this);
        }
        return acquired;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        detector.onBeforeLock(this);
        try {
            boolean acquired = delegate.tryLock(time, unit);
            if (acquired) {
                detector.onAfterLock(this);
            } else {
                detector.onLockFailed(this);
            }
            return acquired;
        } catch (InterruptedException e) {
            detector.onLockFailed(this);
            throw e;
        }
    }

    @Override
    public void unlock() {
        delegate.unlock();
        detector.onUnlock(this);
    }

    @Override
    public Condition newCondition() {
        return delegate.newCondition();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof LockWrapper) {
            return delegate.equals(((LockWrapper) obj).delegate);
        }
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return "LockWrapper{name='" + name + "', delegate=" + delegate + "}";
    }
}
