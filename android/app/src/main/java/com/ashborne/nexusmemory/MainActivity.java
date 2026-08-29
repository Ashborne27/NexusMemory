package com.ashborne.nexusmemory;

import android.os.Bundle;
import android.view.View;
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
            // Optimisation stricte du rendu pour préserver le CPU et éviter la surchauffe
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Suspension active des scripts WebView en arrière-plan pour geler la consommation CPU
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().evaluateJavascript("if(window.pauseTelemetry) { window.pauseTelemetry(); }", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reprise du flux de télémétrie au retour au premier plan
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().evaluateJavascript("if(window.resumeTelemetry) { window.resumeTelemetry(); }", null);
        }
    }
}
