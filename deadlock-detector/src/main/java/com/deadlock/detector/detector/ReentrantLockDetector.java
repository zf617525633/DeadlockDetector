package com.deadlock.detector.detector;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock等显式锁死锁检测器
 * 通过包装Lock实现追踪锁的持有和等待关系
 */
public class ReentrantLockDetector implements IDeadlockDetector {

    private static final String TAG = "ReentrantLockDetector";

    // 记录每个线程持有的锁
    private final ConcurrentHashMap<Long, Set<LockWrapper>> threadHeldLocks;
    // 记录每个线程等待的锁
    private final ConcurrentHashMap<Long, LockWrapper> threadWaitingLock;
    // 所有被追踪的锁
    private final Set<LockWrapper> trackedLocks;

    private final DeadlockReporter reporter;
    private final DeadlockAnalyzer analyzer;

    public ReentrantLockDetector() {
        this.threadHeldLocks = new ConcurrentHashMap<>();
        this.threadWaitingLock = new ConcurrentHashMap<>();
        this.trackedLocks = ConcurrentHashMap.newKeySet();
        this.reporter = new LogcatReporter();
        this.analyzer = new DeadlockAnalyzer();
    }

    public ReentrantLockDetector(DeadlockReporter reporter) {
        this.threadHeldLocks = new ConcurrentHashMap<>();
        this.threadWaitingLock = new ConcurrentHashMap<>();
        this.trackedLocks = ConcurrentHashMap.newKeySet();
        this.reporter = reporter;
        this.analyzer = new DeadlockAnalyzer();
    }

    /**
     * 创建一个被追踪的ReentrantLock
     */
    public Lock createTrackedLock(String lockName) {
        return createTrackedLock(lockName, false);
    }

    /**
     * 创建一个被追踪的ReentrantLock
     *
     * @param lockName 锁名称
     * @param fair     是否公平锁
     */
    public Lock createTrackedLock(String lockName, boolean fair) {
        ReentrantLock lock = new ReentrantLock(fair);
        return wrap(lock, lockName);
    }

    /**
     * 包装现有的Lock进行追踪
     *
     * @param lock     原始锁
     * @param lockName 锁名称
     */
    public LockWrapper wrap(Lock lock, String lockName) {
        LockWrapper wrapper = new LockWrapper(lock, lockName, this);
        trackedLocks.add(wrapper);
        return wrapper;
    }

    /**
     * 内部回调方法 - 锁被获取前
     */
    void onBeforeLock(LockWrapper lock) {
        long threadId = Thread.currentThread().getId();
        threadWaitingLock.put(threadId, lock);
    }

    /**
     * 内部回调方法 - 锁被获取后
     */
    void onAfterLock(LockWrapper lock) {
        long threadId = Thread.currentThread().getId();
        threadWaitingLock.remove(threadId);
        threadHeldLocks.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet()).add(lock);
    }

    /**
     * 内部回调方法 - 锁获取失败
     */
    void onLockFailed(LockWrapper lock) {
        long threadId = Thread.currentThread().getId();
        threadWaitingLock.remove(threadId);
    }

    /**
     * 内部回调方法 - 锁被释放
     */
    void onUnlock(LockWrapper lock) {
        long threadId = Thread.currentThread().getId();
        Set<LockWrapper> heldLocks = threadHeldLocks.get(threadId);
        if (heldLocks != null) {
            heldLocks.remove(lock);
            if (heldLocks.isEmpty()) {
                threadHeldLocks.remove(threadId);
            }
        }
    }

    @Override
    public void detect() {
        // 构建等待图并检测环
        Map<Long, Long> waitForGraph = buildWaitForGraph();
        if (waitForGraph.isEmpty()) {
            return;
        }

        List<List<Long>> cycles = analyzer.detectCycles(waitForGraph);

        if (!cycles.isEmpty()) {
            DeadlockReport report = buildReport(cycles);
            reporter.report(report);
        }
    }

    private Map<Long, Long> buildWaitForGraph() {
        Map<Long, Long> graph = new HashMap<>();

        for (Map.Entry<Long, LockWrapper> entry : threadWaitingLock.entrySet()) {
            long waitingThreadId = entry.getKey();
            LockWrapper waitingForLock = entry.getValue();

            // 找到持有这个锁的线程
            for (Map.Entry<Long, Set<LockWrapper>> heldEntry : threadHeldLocks.entrySet()) {
                if (heldEntry.getValue().contains(waitingForLock)) {
                    graph.put(waitingThreadId, heldEntry.getKey());
                    break;
                }
            }
        }

        return graph;
    }

    private DeadlockReport buildReport(List<List<Long>> cycles) {
        DeadlockReport.Builder builder = new DeadlockReport.Builder()
                .type(DeadlockType.REENTRANT_LOCK)
                .timestamp(System.currentTimeMillis());

        // 收集所有死锁线程ID（去重）
        Set<Long> deadlockedThreadIds = ConcurrentHashMap.newKeySet();
        for (List<Long> cycle : cycles) {
            deadlockedThreadIds.addAll(cycle);
        }

        for (Long threadId : deadlockedThreadIds) {
            Thread thread = findThreadById(threadId);
            if (thread != null) {
                LockWrapper waitingLock = threadWaitingLock.get(threadId);
                Set<LockWrapper> heldLocks = threadHeldLocks.get(threadId);

                List<String> heldLockNames = new ArrayList<>();
                if (heldLocks != null) {
                    for (LockWrapper lock : heldLocks) {
                        heldLockNames.add(lock.getName());
                    }
                }

                ThreadDetail detail = new ThreadDetail.Builder()
                        .threadId(threadId)
                        .threadName(thread.getName())
                        .threadState(thread.getState().name())
                        .waitingForLock(waitingLock != null ? waitingLock.getName() : null)
                        .heldLocks(heldLockNames)
                        .stackTrace(thread.getStackTrace())
                        .build();
                builder.addThreadDetail(detail);
            }
        }

        return builder.build();
    }

    private Thread findThreadById(long threadId) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == threadId) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "ReentrantLockDetector";
    }

    @Override
    public void destroy() {
        threadHeldLocks.clear();
        threadWaitingLock.clear();
        trackedLocks.clear();
    }
}
