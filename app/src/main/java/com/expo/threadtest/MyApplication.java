package com.expo.threadtest;

import android.app.Application;

import com.deadlock.detector.DeadlockDetector;
import com.deadlock.detector.DeadlockDetectorConfig;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化死锁检测器
        DeadlockDetectorConfig config = new DeadlockDetectorConfig.Builder()
                .detectSynchronized(true)
                .detectReentrantLock(true)
                .detectANR(true)
                .detectionInterval(3000)  // 3秒检测一次
                .anrThreshold(5000)       // ANR阈值5秒
                .build();

        DeadlockDetector.init(this, config);
    }
}
