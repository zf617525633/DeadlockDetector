package com.deadlock.detector.detector;

import android.util.Log;

import com.deadlock.detector.analyzer.DeadlockAnalyzer;
import com.deadlock.detector.model.DeadlockReport;
import com.deadlock.detector.model.DeadlockType;
import com.deadlock.detector.model.ThreadDetail;
import com.deadlock.detector.reporter.DeadlockReporter;
import com.deadlock.detector.reporter.LogcatReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronized关键字死锁检测器
 *
 * 由于Android不支持java.lang.management包，采用以下方案检测死锁：
 * 1. 遍历所有线程，找出处于BLOCKED状态的线程
 * 2. 分析堆栈信息，识别等待的锁对象
 * 3. 构建等待图，使用DFS检测环
 */
public class SynchronizedLockDetector implements IDeadlockDetector {

    private static final String TAG = "SyncLockDetector";
    private final DeadlockReporter reporter;
    private final DeadlockAnalyzer analyzer;

    public SynchronizedLockDetector() {
        this.reporter = new LogcatReporter();
        this.analyzer = new DeadlockAnalyzer();
    }

    public SynchronizedLockDetector(DeadlockReporter reporter) {
        this.reporter = reporter;
        this.analyzer = new DeadlockAnalyzer();
    }

    @Override
    public void detect() {
        // 获取所有线程及其堆栈
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        // 找出所有BLOCKED状态的线程
        List<Thread> blockedThreads = new ArrayList<>();
        for (Thread thread : allStackTraces.keySet()) {
            if (thread.getState() == Thread.State.BLOCKED) {
                blockedThreads.add(thread);
            }
        }

        if (blockedThreads.isEmpty()) {
            return;
        }

        // 分析BLOCKED线程，尝试检测死锁
        // 如果有多个线程同时处于BLOCKED状态，可能存在死锁
        if (blockedThreads.size() >= 2) {
            // 检查是否存在循环等待
            Map<Long, Long> waitForGraph = buildWaitForGraph(blockedThreads, allStackTraces);
            List<List<Long>> cycles = analyzer.detectCycles(waitForGraph);

            if (!cycles.isEmpty()) {
                DeadlockReport report = buildReport(cycles, allStackTraces);
                reporter.report(report);
            }
        }

        // 即使没有检测到完整的死锁环，也报告长时间BLOCKED的线程
        // 这可以帮助发现潜在的死锁问题
        reportBlockedThreads(blockedThreads, allStackTraces);
    }

    /**
     * 构建等待图
     * 通过分析BLOCKED线程的堆栈，尝试确定它们在等待哪个线程持有的锁
     */
    private Map<Long, Long> buildWaitForGraph(List<Thread> blockedThreads,
                                               Map<Thread, StackTraceElement[]> allStackTraces) {
        Map<Long, Long> graph = new HashMap<>();

        for (Thread blockedThread : blockedThreads) {
            StackTraceElement[] stackTrace = allStackTraces.get(blockedThread);
            if (stackTrace == null || stackTrace.length == 0) {
                continue;
            }

            // 分析堆栈，找到等待进入的同步块
            String waitingForMethod = findWaitingMethod(stackTrace);
            if (waitingForMethod == null) {
                continue;
            }

            // 查找哪个线程可能持有这个锁
            for (Thread otherThread : allStackTraces.keySet()) {
                if (otherThread.getId() == blockedThread.getId()) {
                    continue;
                }

                StackTraceElement[] otherStack = allStackTraces.get(otherThread);
                if (otherStack == null) {
                    continue;
                }

                // 检查其他线程是否在执行相同的同步方法/块
                if (isHoldingLock(otherStack, waitingForMethod)) {
                    graph.put(blockedThread.getId(), otherThread.getId());
                    break;
                }
            }
        }

        return graph;
    }

    /**
     * 从堆栈中找到线程正在等待进入的方法
     */
    private String findWaitingMethod(StackTraceElement[] stackTrace) {
        if (stackTrace.length == 0) {
            return null;
        }
        // 返回堆栈顶部的方法作为等待点
        StackTraceElement top = stackTrace[0];
        return top.getClassName() + "." + top.getMethodName();
    }

    /**
     * 检查线程是否持有某个锁（在执行某个同步方法）
     */
    private boolean isHoldingLock(StackTraceElement[] stackTrace, String methodSignature) {
        for (StackTraceElement element : stackTrace) {
            String method = element.getClassName() + "." + element.getMethodName();
            if (method.equals(methodSignature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 报告BLOCKED状态的线程（可能的死锁）
     */
    private void reportBlockedThreads(List<Thread> blockedThreads,
                                       Map<Thread, StackTraceElement[]> allStackTraces) {
        // 只有当有多个BLOCKED线程时才报告，避免误报
        if (blockedThreads.size() < 2) {
            return;
        }

        Log.w(TAG, "Detected " + blockedThreads.size() + " blocked threads, possible deadlock:");
        for (Thread thread : blockedThreads) {
            StackTraceElement[] stack = allStackTraces.get(thread);
            Log.w(TAG, "  Thread: " + thread.getName() + " (id=" + thread.getId() + ")");
            if (stack != null && stack.length > 0) {
                Log.w(TAG, "    at " + stack[0]);
            }
        }
    }

    private DeadlockReport buildReport(List<List<Long>> cycles,
                                        Map<Thread, StackTraceElement[]> allStackTraces) {
        DeadlockReport.Builder builder = new DeadlockReport.Builder()
                .type(DeadlockType.SYNCHRONIZED)
                .timestamp(System.currentTimeMillis())
                .message("Detected " + cycles.size() + " deadlock cycle(s)");

        // 收集所有死锁线程ID
        Set<Long> deadlockedThreadIds = ConcurrentHashMap.newKeySet();
        for (List<Long> cycle : cycles) {
            deadlockedThreadIds.addAll(cycle);
        }

        // 构建线程详情
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            if (deadlockedThreadIds.contains(thread.getId())) {
                ThreadDetail detail = new ThreadDetail.Builder()
                        .threadId(thread.getId())
                        .threadName(thread.getName())
                        .threadState(thread.getState().name())
                        .stackTrace(entry.getValue())
                        .build();
                builder.addThreadDetail(detail);
            }
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return "SynchronizedLockDetector";
    }

    @Override
    public void destroy() {
        // 无需清理
    }
}
