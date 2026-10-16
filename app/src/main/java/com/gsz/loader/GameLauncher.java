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
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch failed", e);
        }
    }

    public static void killGame(String packageName) {
        Log.d(TAG, "Killing game: " + packageName);
        ShizukuHelper.killGame(packageName);
    }
}
