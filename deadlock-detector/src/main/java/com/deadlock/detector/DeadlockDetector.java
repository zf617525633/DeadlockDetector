package com.deadlock.detector;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.deadlock.detector.detector.ANRDetector;
import com.deadlock.detector.detector.IDeadlockDetector;
import com.deadlock.detector.detector.LockWrapper;
import com.deadlock.detector.detector.ReentrantLockDetector;
import com.deadlock.detector.detector.SynchronizedLockDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁检测器 - 对外统一入口
 * <p>
 * 使用方式:
 * <pre>
 * // 在Application.onCreate()中初始化
 * DeadlockDetector.init(this);
 *
 * // 或使用自定义配置
 * DeadlockDetectorConfig config = new DeadlockDetectorConfig.Builder()
 *     .detectSynchronized(true)
 *     .detectReentrantLock(true)
 *     .detectANR(true)
 *     .detectionInterval(5000)
 *     .anrThreshold(5000)
 *     .build();
 * DeadlockDetector.init(this, config);
 * </pre>
 */
public final class DeadlockDetector {

    private static final String TAG = "DeadlockDetector";

    private static volatile DeadlockDetector sInstance;

    private final Application application;
    private final DeadlockDetectorConfig config;
    private final List<IDeadlockDetector> detectors;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning;

    // ReentrantLock检测器实例，用于创建被追踪的锁
    private ReentrantLockDetector reentrantLockDetector;

    /**
     * 一行代码初始化 - 使用默认配置
     *
     * @param application Application实例
     */
    public static void init(@NonNull Application application) {
        init(application, DeadlockDetectorConfig.getDefault());
    }

    /**
     * 自定义配置初始化
     *
     * @param application Application实例
     * @param config      配置
     */
    public static void init(@NonNull Application application, @NonNull DeadlockDetectorConfig config) {
        if (sInstance == null) {
            synchronized (DeadlockDetector.class) {
                if (sInstance == null) {
                    sInstance = new DeadlockDetector(application, config);
                    sInstance.start();
                    Log.i(TAG, "DeadlockDetector initialized");
                }
            }
        }
    }

    /**
     * 停止检测
     */
    public static void stop() {
        if (sInstance != null) {
            synchronized (DeadlockDetector.class) {
                if (sInstance != null) {
                    sInstance.stopInternal();
                    sInstance = null;
                    Log.i(TAG, "DeadlockDetector stopped");
                }
            }
        }
    }

    /**
     * 手动触发一次检测
     */
    public static void detectNow() {
        if (sInstance != null) {
            sInstance.performDetection();
        }
    }

    /**
     * 创建一个被追踪的ReentrantLock
     *
     * @param lockName 锁名称，用于日志输出
     * @return 被追踪的Lock
     */
    public static Lock createTrackedLock(@NonNull String lockName) {
        return createTrackedLock(lockName, false);
    }

    /**
     * 创建一个被追踪的ReentrantLock
     *
     * @param lockName 锁名称，用于日志输出
     * @param fair     是否公平锁
     * @return 被追踪的Lock
     */
    public static Lock createTrackedLock(@NonNull String lockName, boolean fair) {
        if (sInstance != null && sInstance.reentrantLockDetector != null) {
            return sInstance.reentrantLockDetector.createTrackedLock(lockName, fair);
        }
        // 如果检测器未初始化，返回普通的ReentrantLock
        Log.w(TAG, "DeadlockDetector not initialized, returning untracked lock");
        return new ReentrantLock(fair);
    }

    /**
     * 包装现有的Lock进行追踪
     *
     * @param lock     原始锁
     * @param lockName 锁名称，用于日志输出
     * @return 被追踪的Lock
     */
    public static Lock wrapLock(@NonNull Lock lock, @NonNull String lockName) {
        if (sInstance != null && sInstance.reentrantLockDetector != null) {
            return sInstance.reentrantLockDetector.wrap(lock, lockName);
        }
        // 如果检测器未初始化，返回原始锁
        Log.w(TAG, "DeadlockDetector not initialized, returning original lock");
        return lock;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return sInstance != null;
    }

    /**
     * 检查是否正在运行
     */
    public static boolean isRunning() {
        return sInstance != null && sInstance.isRunning;
    }

    private DeadlockDetector(Application application, DeadlockDetectorConfig config) {
        this.application = application;
        this.config = config;
        this.detectors = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DeadlockDetector-Scheduler");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });

        // 根据配置初始化检测器
        if (config.isDetectSynchronized()) {
            detectors.add(new SynchronizedLockDetector());
            Log.d(TAG, "SynchronizedLockDetector enabled");
        }

        if (config.isDetectReentrantLock()) {
            reentrantLockDetector = new ReentrantLockDetector();
            detectors.add(reentrantLockDetector);
            Log.d(TAG, "ReentrantLockDetector enabled");
        }

        if (config.isDetectANR()) {
            detectors.add(new ANRDetector(application, config.getAnrThresholdMs()));
            Log.d(TAG, "ANRDetector enabled with threshold: " + config.getAnrThresholdMs() + "ms");
        }
    }

    private void start() {
        if (isRunning) return;
        isRunning = true;

        // 定时检测
        scheduler.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        performDetection();
                    }
                },
                config.getInitialDelayMs(),
                config.getDetectionIntervalMs(),
                TimeUnit.MILLISECONDS
        );

        Log.d(TAG, "Detection started with interval: " + config.getDetectionIntervalMs() + "ms");
    }

    private void performDetection() {
        for (IDeadlockDetector detector : detectors) {
            try {
                detector.detect();
            } catch (Exception e) {
                Log.e(TAG, "Detection error in " + detector.getName(), e);
            }
        }
    }

    private void stopInternal() {
        isRunning = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        for (IDeadlockDetector detector : detectors) {
            try {
                detector.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying " + detector.getName(), e);
            }
        }
        detectors.clear();
        reentrantLockDetector = null;
    }
}
