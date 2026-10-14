package com.gsz.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public class GameLauncher {

    private static final String[] GAME_PACKAGES = {
        "com.pubg.imobile",      // BGMI India
        "com.tencent.ig",        // PUBG Global
        "com.tencent.igfit",     // PUBG Global (fit)
        "com.pubg.krmobile",     // PUBG KR
        "com.vng.pubgmobile",    // PUBG VN
        "com.rekoo.pubgm"        // PUBG TW
    };

    public static String detectInstalledGame(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : GAME_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    public static void launchGame(Context ctx, String packageName) {
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    }

    public static void killGame(IShizukuShell shell, String packageName) {
        try {
            shell.exec("am force-stop " + packageName);
        } catch (Exception ignored) {}
    }
}
