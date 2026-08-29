package com.ashborne.nexusmemory;

import android.os.Bundle;
import android.webkit.WebSettings;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (this.bridge != null && this.bridge.getWebView() != null) {
            WebSettings settings = this.bridge.getWebView().getSettings();
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        }
        
        // Activation initiale du Moteur Hyper-Buffer 50Go RAM
        NexusHyperBuffer.optimizeMemoryAndProcess(this);
    }
}
