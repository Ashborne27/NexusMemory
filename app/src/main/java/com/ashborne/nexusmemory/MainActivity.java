package com.ashborne.nexusmemory;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;

public class MainActivity extends Activity {
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Forçage absolu de l'accélération matérielle
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminComponent = new ComponentName(this, NexusDeviceAdminReceiver.class);

        // Interface native minimaliste et haute performance du conteneur
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#0A0A0A"));

        TextView title = new TextView(this);
        title.setText("NEXUSMEMORY SYSTEM");
        title.setTextColor(Color.parseColor("#00FF66"));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView status = new TextView(this);
        boolean isAdmin = devicePolicyManager.isAdminActive(deviceAdminComponent);
        status.setText(isAdmin ? "État : Conteneur Isolé & Actif" : "État : Prêt pour l'élévation Device Owner");
        status.setTextColor(Color.parseColor("#CCCCCC"));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 40, 0, 0);
        layout.addView(status);

        setContentView(layout);
    }
}
