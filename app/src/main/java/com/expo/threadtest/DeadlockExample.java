package com.expo.threadtest;

/**
 * 死锁示例类 - 演示 synchronized 死锁
 *
 * 死锁场景：
 * - Thread-1: 先获取 lockA，再尝试获取 lockB
 * - Thread-2: 先获取 lockB，再尝试获取 lockA
 * - 两个线程互相等待对方释放锁，形成死锁
 */
public class DeadlockExample {

    // 两个锁对象
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    /**
     * 触发 synchronized 死锁
     */
    public void triggerSynchronizedDeadlock() {
        // 线程1：先获取lockA，再获取lockB
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lockA) {
                    System.out.println("Thread-1: 持有 lockA，等待 lockB...");

                    // 休眠一下，确保线程2有时间获取lockB
                    sleep(100);

                    synchronized (lockB) {
                        System.out.println("Thread-1: 获取到 lockB");
                    }
                }
            }
        }, "DeadlockThread-1");

        // 线程2：先获取lockB，再获取lockA
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lockB) {
                    System.out.println("Thread-2: 持有 lockB，等待 lockA...");

                    // 休眠一下，确保线程1有时间获取lockA
                    sleep(100);

                    synchronized (lockA) {
                        System.out.println("Thread-2: 获取到 lockA");
                    }
                }
            }
        }, "DeadlockThread-2");

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
