package com.stark.protocol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final int STARTUP_PERMISSIONS = 7401;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(5, 6, 8));
        getWindow().setStatusBarColor(android.graphics.Color.rgb(5, 6, 8));
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new StarkPermissionBridge(), "AndroidPermissions");
        }
    }

    private class StarkPermissionBridge {
        @JavascriptInterface
        public void requestNotifications() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, STARTUP_PERMISSIONS);
                } else {
                    sendNotificationResult(true);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STARTUP_PERMISSIONS) {
            boolean granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            sendNotificationResult(granted);
        }
    }

    private void sendNotificationResult(boolean granted) {
        if (getBridge() != null && getBridge().getWebView() != null) {
            String js = "window.__starkNotificationResult && window.__starkNotificationResult(" + (granted ? "true" : "false") + ");";
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(js, null));
        }
    }
}
