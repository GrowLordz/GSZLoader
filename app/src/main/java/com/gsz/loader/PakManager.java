package com.gsz.loader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PakManager {

    private static final String TAG = "PakManager";
    private static final String PAK_DIR = "paks";

    public interface CopyCallback {
        void onProgress(String fileName, int current, int total);
        void onComplete(boolean success, String message);
    }

    public static void copyPaks(Context ctx, String gamePkg, CopyCallback callback) {
        try {
            AssetManager am = ctx.getAssets();
            String[] files = am.list(PAK_DIR);
            if (files == null || files.length == 0) {
                if (callback != null) callback.onComplete(false, "No paks in assets");
                return;
            }

            int total = 0;
            for (String f : files) {
                if (f.endsWith(".pak") || f.endsWith(".lua")) total++;
            }

            if (total == 0) {
                if (callback != null) callback.onComplete(false, "No .pak or .lua files found");
                return;
            }

            File internalDir = new File(ctx.getFilesDir(), "paks");
            if (!internalDir.exists()) internalDir.mkdirs();

            int current = 0;
            for (String f : files) {
                if (!f.endsWith(".pak") && !f.endsWith(".lua")) continue;
                current++;

                File out = new File(internalDir, f);
                if (out.exists() && out.length() > 0) {
                    if (callback != null) callback.onProgress(f, current, total);
                    continue;
                }

                InputStream is = am.open(PAK_DIR + "/" + f);
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fos.close();
                is.close();

                if (callback != null) callback.onProgress(f, current, total);
            }

            String gamePakDir = ShizukuHelper.getGamePakDir(gamePkg);
            ShizukuHelper.mkdir(gamePakDir);

            current = 0;
            File[] pakFiles = internalDir.listFiles();
            if (pakFiles != null) {
                for (File f : pakFiles) {
                    if (!f.getName().endsWith(".pak") && !f.getName().endsWith(".lua")) continue;
                    current++;
                    String dst = gamePakDir + f.getName();
                    ShizukuHelper.copyFile(f.getAbsolutePath(), dst);
                    if (callback != null) callback.onProgress(f.getName(), current, pakFiles.length);
                }
            }

            File cfg = new File(ctx.getFilesDir(), "grow_settings.txt");
            if (cfg.exists()) {
                ShizukuHelper.copyFile(cfg.getAbsolutePath(), gamePakDir + "grow_settings.txt");
            }

            if (callback != null) callback.onComplete(true, "Paks copied successfully");

        } catch (Exception e) {
            Log.e(TAG, "Copy paks failed", e);
            if (callback != null) callback.onComplete(false, "Error: " + e.getMessage());
        }
    }

    public static boolean arePaksInAssets(Context ctx) {
        try {
            AssetManager am = ctx.getAssets();
            String[] files = am.list(PAK_DIR);
            if (files == null) return false;
            for (String f : files) {
                if (f.endsWith(".pak") || f.endsWith(".lua")) return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Check assets failed", e);
        }
        return false;
    }
}
