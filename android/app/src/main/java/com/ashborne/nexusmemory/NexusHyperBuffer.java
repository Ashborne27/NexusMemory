package com.ashborne.nexusmemory;

import android.content.Context;
import android.os.Process;

public class NexusHyperBuffer {
    public static void optimizeMemoryAndProcess(Context context) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            Runtime.getRuntime().gc();
        } catch (Exception e) {
            // Isolation des exceptions pour garantir la stabilité du thread principal
        }
    }
}
