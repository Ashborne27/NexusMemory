package com.nexusmemory.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NexusTelemetry")
public class NexusTelemetryBridge extends Plugin {

    @PluginMethod
    public void getSystemMetrics(PluginCall call) {
        JSObject ret = new JSObject();
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            ret.put("maxMemoryMB", maxMemory / (1024 * 1024));
            ret.put("usedMemoryMB", usedMemory / (1024 * 1024));
            ret.put("freeMemoryMB", freeMemory / (1024 * 1024));
            ret.put("vaultTransactions", NexusMemoryVault.getInstance().getTransactionCount());
            ret.put("daemonStatus", "SOUVERAIN_ACTIF");
            
            call.resolve(ret);
        } catch (Throwable e) {
            call.reject(e.getMessage() != null ? e.getMessage() : "Telemetry extraction failed");
        }
    }
}
