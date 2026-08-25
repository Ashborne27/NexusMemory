package com.nexusmemory.app;

import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

public class NexusMemoryVault {
    private static volatile NexusMemoryVault instance;
    private final ConcurrentHashMap<String, String> hotMemoryIndex;
    private final AtomicLong transactionCounter;

    private NexusMemoryVault() {
        // Allocation initiale dimensionnée pour exploiter massivement la RAM disponible
        this.hotMemoryIndex = new ConcurrentHashMap<>(16384, 0.75f, Runtime.getRuntime().availableProcessors() * 2);
        this.transactionCounter = new AtomicLong(0);
    }

    public static NexusMemoryVault getInstance() {
        if (instance == null) {
            synchronized (NexusMemoryVault.class) {
                if (instance == null) {
                    instance = new NexusMemoryVault();
                }
            }
        }
        return instance;
    }

    public void indexMemory(String key, String content) {
        if (key != null && content != null) {
            hotMemoryIndex.put(key, content);
            transactionCounter.incrementAndGet();
        }
    }

    public String retrieveMemory(String key) {
        return key != null ? hotMemoryIndex.get(key) : null;
    }

    public List<String> searchHotMemory(String query) {
        List<String> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return results;
        
        String lowerQuery = query.toLowerCase();
        for (ConcurrentHashMap.Entry<String, String> entry : hotMemoryIndex.entrySet()) {
            if (entry.getValue().toLowerCase().contains(lowerQuery)) {
                results.add(entry.getKey());
            }
        }
        return results;
    }

    public void purgeVault() {
        hotMemoryIndex.clear();
        transactionCounter.set(0);
    }

    public long getTransactionCount() {
        return transactionCounter.get();
    }
}
