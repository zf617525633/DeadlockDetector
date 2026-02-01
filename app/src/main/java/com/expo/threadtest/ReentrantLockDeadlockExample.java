package com.expo.threadtest;

import com.deadlock.detector.DeadlockDetector;

import java.util.concurrent.locks.Lock;

/**
 * ReentrantLock 死锁示例类
 *
 * 死锁场景：
 * - Thread-1: 先获取 lock1，再尝试获取 lock2
 * - Thread-2: 先获取 lock2，再尝试获取 lock1
 * - 两个线程互相等待对方释放锁，形成死锁
 */
public class ReentrantLockDeadlockExample {

    // 使用 DeadlockDetector 创建被追踪的锁
    private Lock lock1;
    private Lock lock2;

    public ReentrantLockDeadlockExample() {
        // 创建被追踪的 ReentrantLock
        lock1 = DeadlockDetector.createTrackedLock("Lock-1");
        lock2 = DeadlockDetector.createTrackedLock("Lock-2");
    }

    /**
     * 触发 ReentrantLock 死锁
     */
    public void triggerReentrantLockDeadlock() {
        // 线程1：先获取lock1，再获取lock2
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                lock1.lock();
                try {
                    System.out.println("ReentrantLock-Thread-1: 持有 lock1，等待 lock2...");

                    // 休眠一下，确保线程2有时间获取lock2
                    sleep(100);

                    lock2.lock();
                    try {
                        System.out.println("ReentrantLock-Thread-1: 获取到 lock2");
                    } finally {
                        lock2.unlock();
                    }
                } finally {
                    lock1.unlock();
                }
            }
        }, "ReentrantLockDeadlock-1");

        // 线程2：先获取lock2，再获取lock1
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                lock2.lock();
                try {
                    System.out.println("ReentrantLock-Thread-2: 持有 lock2，等待 lock1...");

                    // 休眠一下，确保线程1有时间获取lock1
                    sleep(100);

                    lock1.lock();
                    try {
                        System.out.println("ReentrantLock-Thread-2: 获取到 lock1");
                    } finally {
                        lock1.unlock();
                    }
                } finally {
                    lock2.unlock();
                }
            }
        }, "ReentrantLockDeadlock-2");

        // 启动两个线程
        thread1.start();
        thread2.start();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
