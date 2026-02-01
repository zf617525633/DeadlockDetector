# Android 线程死锁检测库 - DeadlockDetector

## 简介

DeadlockDetector 是一个用于 Android 开发调试阶段的线程死锁检测库，支持检测：
- synchronized 关键字导致的死锁
- ReentrantLock 等显式锁导致的死锁
- 主线程 ANR（Application Not Responding）

## 快速开始

### 1. 添加依赖

在 `app/build.gradle` 中添加：

```groovy
dependencies {
    implementation project(':deadlock-detector')
}
```

### 2. 初始化

在 `Application.onCreate()` 中初始化：

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 一行代码初始化（使用默认配置）
        DeadlockDetector.init(this);
    }
}
```

### 3. 自定义配置（可选）

```java
DeadlockDetectorConfig config = new DeadlockDetectorConfig.Builder()
    .detectSynchronized(true)      // 检测 synchronized 死锁
    .detectReentrantLock(true)     // 检测 ReentrantLock 死锁
    .detectANR(true)               // 检测 ANR
    .detectionInterval(5000)       // 检测间隔 5 秒
    .anrThreshold(5000)            // ANR 阈值 5 秒
    .build();

DeadlockDetector.init(this, config);
```

## API 说明

### 基本 API

```java
// 初始化
DeadlockDetector.init(application);
DeadlockDetector.init(application, config);

// 停止检测
DeadlockDetector.stop();

// 手动触发检测
DeadlockDetector.detectNow();

// 检查状态
DeadlockDetector.isInitialized();
DeadlockDetector.isRunning();
```

### 追踪 ReentrantLock

要检测 ReentrantLock 死锁，需要使用被追踪的锁：

```java
// 方式1: 创建被追踪的 ReentrantLock
Lock lock = DeadlockDetector.createTrackedLock("MyLock");

// 方式2: 包装现有 Lock
Lock originalLock = new ReentrantLock();
Lock trackedLock = DeadlockDetector.wrapLock(originalLock, "MyLock");

// 正常使用
lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock();
}
```

## 日志输出示例

检测到死锁时，会在 Logcat 中输出详细信息：

```
╔══════════════════════════════════════════════════════════════════╗
║                        DEADLOCK DETECTED!                        ║
╠══════════════════════════════════════════════════════════════════╣
║ Type: Synchronized Lock Deadlock
║ Time: 2024-01-15 10:30:45.123
╠──────────────────────────────────────────────────────────────────╣
║ Thread: "Thread-1" (id=12)
║ State: BLOCKED
║ Stack:
║   at com.example.MyClass.methodA(MyClass.java:25)
║   at com.example.MyClass.run(MyClass.java:10)
╠──────────────────────────────────────────────────────────────────╣
║ Thread: "Thread-2" (id=13)
║ State: BLOCKED
║ Stack:
║   at com.example.MyClass.methodB(MyClass.java:35)
║   at com.example.MyClass.run(MyClass.java:15)
╚══════════════════════════════════════════════════════════════════╝
```

## 架构说明

```
com.deadlock.detector/
├── DeadlockDetector.java          # 对外统一 API 入口
├── DeadlockDetectorConfig.java    # 配置类
├── detector/
│   ├── IDeadlockDetector.java     # 检测器接口
│   ├── SynchronizedLockDetector   # synchronized 死锁检测
│   ├── ReentrantLockDetector      # 显式锁死锁检测
│   ├── ANRDetector                # ANR 检测
│   └── LockWrapper                # 锁包装器
├── analyzer/
│   └── DeadlockAnalyzer           # 等待图环检测算法
├── reporter/
│   ├── DeadlockReporter           # 报告接口
│   └── LogcatReporter             # Logcat 输出实现
└── model/
    ├── DeadlockReport             # 死锁报告
    ├── ThreadDetail               # 线程详情
    └── DeadlockType               # 死锁类型枚举
```

## 检测原理

### Synchronized 死锁检测
- 遍历所有线程，找出处于 BLOCKED 状态的线程
- 分析堆栈信息，构建等待图
- 使用 DFS 算法检测环（死锁）

### ReentrantLock 死锁检测
- 通过 LockWrapper 包装锁，追踪锁的持有和等待关系
- 构建等待图（Wait-for Graph）
- 使用 DFS 算法检测环

### ANR 检测
- Worker 线程定时向主线程发送消息
- 检测主线程响应时间
- 超过阈值则报告 ANR，并收集所有线程堆栈

## 注意事项

1. **仅用于开发调试**：此库设计用于开发阶段，不建议在生产环境使用
2. **ReentrantLock 追踪**：只有使用 `createTrackedLock()` 或 `wrapLock()` 创建的锁才能被检测
3. **性能开销**：定时检测会有一定性能开销，可通过配置调整检测间隔
