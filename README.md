# DeadlockDetector

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

### Introduction

DeadlockDetector is an Android library for detecting thread deadlocks during development and debugging. It supports:

- **Synchronized Deadlock Detection** - Detects deadlocks caused by `synchronized` keyword
- **ReentrantLock Deadlock Detection** - Detects deadlocks caused by explicit locks like `ReentrantLock`
- **ANR Detection** - Detects main thread blocking (Application Not Responding)

### Quick Start

#### 1. Add Dependency

In your `app/build.gradle`:

```groovy
dependencies {
    implementation project(':deadlock-detector')
}
```

#### 2. Initialize

In your `Application.onCreate()`:

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // One-line initialization with default config
        DeadlockDetector.init(this);
    }
}
```

#### 3. Custom Configuration (Optional)

```java
DeadlockDetectorConfig config = new DeadlockDetectorConfig.Builder()
    .detectSynchronized(true)      // Enable synchronized deadlock detection
    .detectReentrantLock(true)     // Enable ReentrantLock deadlock detection
    .detectANR(true)               // Enable ANR detection
    .detectionInterval(5000)       // Detection interval: 5 seconds
    .anrThreshold(5000)            // ANR threshold: 5 seconds
    .build();

DeadlockDetector.init(this, config);
```

### API Reference

```java
// Initialize
DeadlockDetector.init(application);
DeadlockDetector.init(application, config);

// Stop detection
DeadlockDetector.stop();

// Trigger detection manually
DeadlockDetector.detectNow();

// Check status
DeadlockDetector.isInitialized();
DeadlockDetector.isRunning();

// Create tracked locks for ReentrantLock detection
Lock lock = DeadlockDetector.createTrackedLock("MyLock");
Lock trackedLock = DeadlockDetector.wrapLock(existingLock, "MyLock");
```

### Detection Principles

| Type | Implementation |
|------|----------------|
| Synchronized | Analyze BLOCKED threads' stack traces, build wait-for graph, detect cycles using DFS |
| ReentrantLock | Track lock holding/waiting via LockWrapper, build wait-for graph, detect cycles |
| ANR | Worker thread sends messages to main thread, detect response timeout |

### Log Output Example

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

### Project Structure

```
deadlock-detector/
└── src/main/java/com/deadlock/detector/
    ├── DeadlockDetector.java           # Main API entry
    ├── DeadlockDetectorConfig.java     # Configuration
    ├── analyzer/
    │   └── DeadlockAnalyzer.java       # Cycle detection (DFS)
    ├── detector/
    │   ├── IDeadlockDetector.java      # Detector interface
    │   ├── SynchronizedLockDetector.java
    │   ├── ReentrantLockDetector.java
    │   ├── ANRDetector.java
    │   └── LockWrapper.java
    ├── model/
    │   ├── DeadlockType.java
    │   ├── DeadlockReport.java
    │   └── ThreadDetail.java
    └── reporter/
        ├── DeadlockReporter.java
        └── LogcatReporter.java
```

### Notes

1. **Development Only** - This library is designed for development/debugging, not recommended for production
2. **ReentrantLock Tracking** - Only locks created via `createTrackedLock()` or `wrapLock()` can be detected
3. **Performance** - Periodic detection has some overhead, adjust interval via configuration

---

<a name="中文"></a>
## 中文

### 简介

DeadlockDetector 是一个用于 Android 开发调试阶段的线程死锁检测库，支持：

- **Synchronized 死锁检测** - 检测 `synchronized` 关键字导致的死锁
- **ReentrantLock 死锁检测** - 检测 `ReentrantLock` 等显式锁导致的死锁
- **ANR 检测** - 检测主线程阻塞（Application Not Responding）

### 快速开始

#### 1. 添加依赖

在 `app/build.gradle` 中添加：

```groovy
dependencies {
    implementation project(':deadlock-detector')
}
```

#### 2. 初始化

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

#### 3. 自定义配置（可选）

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

### API 说明

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

// 创建被追踪的锁（用于 ReentrantLock 检测）
Lock lock = DeadlockDetector.createTrackedLock("MyLock");
Lock trackedLock = DeadlockDetector.wrapLock(existingLock, "MyLock");
```

### 检测原理

| 类型 | 实现方案 |
|------|---------|
| Synchronized | 分析 BLOCKED 线程堆栈，构建等待图，DFS 检测环 |
| ReentrantLock | 通过 LockWrapper 追踪锁持有/等待关系，构建等待图，检测环 |
| ANR | Worker 线程向主线程发消息，检测响应超时 |

### 日志输出示例

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

### 项目结构

```
deadlock-detector/
└── src/main/java/com/deadlock/detector/
    ├── DeadlockDetector.java           # 统一入口 API
    ├── DeadlockDetectorConfig.java     # 配置类
    ├── analyzer/
    │   └── DeadlockAnalyzer.java       # 环检测算法 (DFS)
    ├── detector/
    │   ├── IDeadlockDetector.java      # 检测器接口
    │   ├── SynchronizedLockDetector.java
    │   ├── ReentrantLockDetector.java
    │   ├── ANRDetector.java
    │   └── LockWrapper.java
    ├── model/
    │   ├── DeadlockType.java
    │   ├── DeadlockReport.java
    │   └── ThreadDetail.java
    └── reporter/
        ├── DeadlockReporter.java
        └── LogcatReporter.java
```

### 注意事项

1. **仅用于开发调试** - 此库设计用于开发阶段，不建议在生产环境使用
2. **ReentrantLock 追踪** - 只有使用 `createTrackedLock()` 或 `wrapLock()` 创建的锁才能被检测
3. **性能开销** - 定时检测会有一定性能开销，可通过配置调整检测间隔

---

## License

Apache License 2.0
