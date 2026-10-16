package com.gsz.loader;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GameLauncher {

    private static final String TAG = "GameLauncher";

    public static void launchGame(Context ctx, String packageName) {
        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                Log.d(TAG, "Game launched: " + packageName);
            } else {
                Log.e(TAG, "Launch intent null for: " + packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch failed", e);
        }
    }

    public static void killGame(String packageName) {
        Log.d(TAG, "Killing game: " + packageName);
        ShizukuHelper.killGame(packageName);
    }

    public static boolean isGameRunning(Context ctx, String packageName) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"pidof", packageName});
            int exit = p.waitFor();
            p.destroy();
            return exit == 0;
        } catch (Exception e) {
            // pidof not available, try alternative
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"ps"});
                java.io.InputStream is = p.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                is.close();
                p.waitFor();
                p.destroy();
                String output = bos.toString();
                return output.contains(packageName);
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
