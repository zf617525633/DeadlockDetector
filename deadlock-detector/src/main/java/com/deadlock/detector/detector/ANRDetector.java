package com.deadlock.detector.detector;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.deadlock.detector.model.DeadlockReport;
import com.deadlock.detector.model.DeadlockType;
import com.deadlock.detector.model.ThreadDetail;
import com.deadlock.detector.reporter.DeadlockReporter;
import com.deadlock.detector.reporter.LogcatReporter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ANR检测器 - 检测主线程阻塞
 */
public class ANRDetector implements IDeadlockDetector {

    private static final String TAG = "ANRDetector";

    private final Handler mainHandler;
    private final Handler workerHandler;
    private final HandlerThread workerThread;
    private final long thresholdMs;
    private final DeadlockReporter reporter;

    private final AtomicBoolean responseReceived;
    private final AtomicLong lastCheckTime;
    private final AtomicBoolean isMonitoring;

    // 防止重复报告同一次ANR
    private final AtomicBoolean anrReported;

    public ANRDetector(Application application, long thresholdMs) {
        this.thresholdMs = thresholdMs;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.workerThread = new HandlerThread("ANRDetector-Worker");
        this.workerThread.start();
        this.workerHandler = new Handler(workerThread.getLooper());
        this.reporter = new LogcatReporter();
        this.responseReceived = new AtomicBoolean(true);
        this.lastCheckTime = new AtomicLong(0);
        this.isMonitoring = new AtomicBoolean(false);
        this.anrReported = new AtomicBoolean(false);
    }

    public ANRDetector(Application application, long thresholdMs, DeadlockReporter reporter) {
        this.thresholdMs = thresholdMs;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.workerThread = new HandlerThread("ANRDetector-Worker");
        this.workerThread.start();
        this.workerHandler = new Handler(workerThread.getLooper());
        this.reporter = reporter;
        this.responseReceived = new AtomicBoolean(true);
        this.lastCheckTime = new AtomicLong(0);
        this.isMonitoring = new AtomicBoolean(false);
        this.anrReported = new AtomicBoolean(false);
    }

    @Override
    public void detect() {
        if (!isMonitoring.get()) {
            startMonitoring();
        }
    }

    private void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            scheduleCheck();
        }
    }

    private void scheduleCheck() {
        if (!isMonitoring.get()) return;

        workerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkMainThread();
            }
        }, thresholdMs);
    }

    private void checkMainThread() {
        if (!isMonitoring.get()) return;

        // 检查上一次的响应
        if (!responseReceived.get()) {
            // 主线程没有响应，可能发生ANR
            long blockTime = System.currentTimeMillis() - lastCheckTime.get();

            // 只报告一次，直到主线程恢复
            if (anrReported.compareAndSet(false, true)) {
                reportANR(blockTime);
            }
        } else {
            // 主线程已响应，重置ANR报告标志
            anrReported.set(false);
        }

        // 发送新的检测消息
        responseReceived.set(false);
        lastCheckTime.set(System.currentTimeMillis());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                responseReceived.set(true);
            }
        });

        // 安排下一次检测
        scheduleCheck();
    }

    private void reportANR(long blockTimeMs) {
        Thread mainThread = Looper.getMainLooper().getThread();

        DeadlockReport.Builder builder = new DeadlockReport.Builder()
                .type(DeadlockType.ANR)
                .timestamp(System.currentTimeMillis())
                .message("Main thread blocked for " + blockTimeMs + "ms (threshold: " + thresholdMs + "ms)");

        // 主线程详情
        ThreadDetail mainDetail = new ThreadDetail.Builder()
                .threadId(mainThread.getId())
                .threadName(mainThread.getName())
                .threadState(mainThread.getState().name())
                .stackTrace(mainThread.getStackTrace())
                .blockTimeMs(blockTimeMs)
                .build();

        builder.addThreadDetail(mainDetail);

        // 收集所有线程信息，帮助分析
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread t = entry.getKey();
            if (t.getId() != mainThread.getId()) {
                ThreadDetail otherDetail = new ThreadDetail.Builder()
                        .threadId(t.getId())
                        .threadName(t.getName())
                        .threadState(t.getState().name())
                        .stackTrace(entry.getValue())
                        .build();
                builder.addOtherThreadDetail(otherDetail);
            }
        }

        reporter.report(builder.build());
    }

    @Override
    public String getName() {
        return "ANRDetector";
    }

    @Override
    public void destroy() {
        isMonitoring.set(false);
        workerHandler.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
        workerThread.quitSafely();
    }
}
