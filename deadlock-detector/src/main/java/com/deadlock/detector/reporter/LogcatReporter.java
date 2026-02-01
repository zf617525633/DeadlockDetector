package com.deadlock.detector.reporter;

import android.util.Log;

import com.deadlock.detector.model.DeadlockReport;
import com.deadlock.detector.model.DeadlockType;
import com.deadlock.detector.model.ThreadDetail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Logcat日志输出实现
 */
public class LogcatReporter implements DeadlockReporter {

    private static final String TAG = "DeadlockDetector";
    private static final String LINE = "══════════════════════════════════════════════════════════════════";
    private static final String SEPARATOR = "──────────────────────────────────────────────────────────────────";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    @Override
    public void report(DeadlockReport report) {
        StringBuilder sb = new StringBuilder();

        // 头部
        sb.append("\n╔").append(LINE).append("╗\n");
        sb.append("║                        ").append(getTitle(report.getType())).append("                        ║\n");
        sb.append("╠").append(LINE).append("╣\n");

        // 基本信息
        sb.append("║ Type: ").append(report.getType().getDescription()).append("\n");
        sb.append("║ Time: ").append(dateFormat.format(new Date(report.getTimestamp()))).append("\n");

        if (report.getMessage() != null && !report.getMessage().isEmpty()) {
            sb.append("║ Message: ").append(report.getMessage()).append("\n");
        }

        // 死锁线程详情
        List<ThreadDetail> threadDetails = report.getThreadDetails();
        for (int i = 0; i < threadDetails.size(); i++) {
            sb.append("╠").append(SEPARATOR).append("╣\n");
            appendThreadDetail(sb, threadDetails.get(i), report.getType());
        }

        // ANR时显示其他线程信息
        if (report.getType() == DeadlockType.ANR && !report.getOtherThreadDetails().isEmpty()) {
            sb.append("╠").append(SEPARATOR).append("╣\n");
            sb.append("║ Other Threads (top 5):\n");

            List<ThreadDetail> others = report.getOtherThreadDetails();
            int count = Math.min(5, others.size());
            for (int i = 0; i < count; i++) {
                ThreadDetail detail = others.get(i);
                sb.append("║   - ").append(detail.getThreadName())
                        .append(" (").append(detail.getThreadState()).append(")\n");
            }
        }

        // 尾部
        sb.append("╚").append(LINE).append("╝\n");

        Log.e(TAG, sb.toString());
    }

    private String getTitle(DeadlockType type) {
        switch (type) {
            case SYNCHRONIZED:
            case REENTRANT_LOCK:
                return "DEADLOCK DETECTED!";
            case ANR:
                return "ANR DETECTED!     ";
            default:
                return "ISSUE DETECTED!   ";
        }
    }

    private void appendThreadDetail(StringBuilder sb, ThreadDetail detail, DeadlockType type) {
        sb.append("║ Thread: \"").append(detail.getThreadName())
                .append("\" (id=").append(detail.getThreadId()).append(")\n");
        sb.append("║ State: ").append(detail.getThreadState()).append("\n");

        // 锁信息
        if (type == DeadlockType.SYNCHRONIZED) {
            if (detail.getLockName() != null) {
                sb.append("║ Waiting for: ").append(detail.getLockName());
                if (detail.getLockOwnerId() > 0) {
                    sb.append(" (held by ").append(detail.getLockOwnerName()).append(")");
                }
                sb.append("\n");
            }
        } else if (type == DeadlockType.REENTRANT_LOCK) {
            if (detail.getWaitingForLock() != null) {
                sb.append("║ Waiting for: ").append(detail.getWaitingForLock()).append("\n");
            }
            if (detail.getHeldLocks() != null && !detail.getHeldLocks().isEmpty()) {
                sb.append("║ Holding: ").append(String.join(", ", detail.getHeldLocks())).append("\n");
            }
        } else if (type == DeadlockType.ANR) {
            if (detail.getBlockTimeMs() > 0) {
                sb.append("║ Blocked for: ").append(detail.getBlockTimeMs()).append("ms\n");
            }
        }

        // 堆栈信息
        StackTraceElement[] stackTrace = detail.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            sb.append("║ Stack:\n");
            int maxLines = Math.min(15, stackTrace.length);
            for (int i = 0; i < maxLines; i++) {
                sb.append("║   at ").append(stackTrace[i].toString()).append("\n");
            }
            if (stackTrace.length > maxLines) {
                sb.append("║   ... ").append(stackTrace.length - maxLines).append(" more\n");
            }
        }
    }
}
