package com.nexusmemory.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

public class NexusCoreEngine {
    private static volatile NexusCoreEngine instance;
    private final ThreadPoolExecutor threadPool;
    private final ConcurrentHashMap<String, Object> memoryCache;
    private final Handler mainHandler;

    private NexusCoreEngine() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;

        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
        this.threadPool.allowCoreThreadTimeOut(true);
        this.memoryCache = new ConcurrentHashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static NexusCoreEngine getInstance() {
        if (instance == null) {
            synchronized (NexusCoreEngine.class) {
                if (instance == null) {
                    instance = new NexusCoreEngine();
                }
            }
        }
        return instance;
    }

    public void executeAsync(Runnable task) {
        threadPool.execute(task);
    }

    public void runOnMainThread(Runnable action) {
        mainHandler.post(action);
    }

    public void putCache(String key, Object value) {
        if (key != null && value != null) {
            memoryCache.put(key, value);
        }
    }

    public Object getCache(String key) {
        return key != null ? memoryCache.get(key) : null;
    }

    public void clearCache() {
        memoryCache.clear();
    }
}
