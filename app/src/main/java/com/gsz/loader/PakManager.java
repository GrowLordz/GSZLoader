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
            Log.d(TAG, "=== copyPaks started ===");
            Log.d(TAG, "Game package: " + gamePkg);

            ShizukuHelper.killGame(gamePkg);
            Thread.sleep(500);

            AssetManager am = ctx.getAssets();
            String[] files = am.list(PAK_DIR);
            
            if (files == null || files.length == 0) {
                Log.e(TAG, "No files in assets/" + PAK_DIR);
                if (callback != null) callback.onComplete(false, "No paks in assets");
                return;
            }

            Log.d(TAG, "Found " + files.length + " files in assets");

            int total = 0;
            for (String f : files) {
                if (f.endsWith(".pak") || f.endsWith(".lua")) {
                    total++;
                    Log.d(TAG, "Found pak: " + f);
                }
            }

            if (total == 0) {
                Log.e(TAG, "No .pak or .lua files found");
                if (callback != null) callback.onComplete(false, "No .pak or .lua files found");
                return;
            }

            File internalDir = new File(ctx.getFilesDir(), "paks");
            if (!internalDir.exists()) {
                boolean created = internalDir.mkdirs();
                Log.d(TAG, "Created internal dir: " + created);
            }

            int current = 0;
            for (String f : files) {
                if (!f.endsWith(".pak") && !f.endsWith(".lua")) continue;
                current++;

                File out = new File(internalDir, f);
                Log.d(TAG, "Extracting: " + f + " (" + current + "/" + total + ")");
                
                InputStream is = am.open(PAK_DIR + "/" + f);
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                int len;
                long totalBytes = 0;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                    totalBytes += len;
                }
                fos.close();
                is.close();

                Log.d(TAG, "Extracted: " + f + " (" + totalBytes + " bytes)");
                if (callback != null) callback.onProgress(f, current, total);
            }

            String gamePakDir = ShizukuHelper.getGamePakDir(gamePkg);
            Log.d(TAG, "Game pak dir: " + gamePakDir);

            boolean dirOk = ShizukuHelper.mkdir(gamePakDir);
            Log.d(TAG, "Created game dir: " + dirOk);

            String checkDir = ShizukuHelper.exec("ls -la " + gamePakDir);
            Log.d(TAG, "Dir contents: " + checkDir);

            current = 0;
            File[] pakFiles = internalDir.listFiles();
            if (pakFiles != null) {
                for (File f : pakFiles) {
                    if (!f.getName().endsWith(".pak") && !f.getName().endsWith(".lua")) continue;
                    current++;

                    String dst = gamePakDir + f.getName();
                    Log.d(TAG, "Copying: " + f.getName() + " -> " + dst);
                    
                    boolean ok = ShizukuHelper.copyFile(f.getAbsolutePath(), dst);
                    Log.d(TAG, "Copy result: " + ok);

                    String checkFile = ShizukuHelper.exec("ls -la " + dst);
                    Log.d(TAG, "File check: " + checkFile);

                    if (callback != null) callback.onProgress(f.getName(), current, pakFiles.length);
                }
            }

            File cfg = new File(ctx.getFilesDir(), "grow_settings.txt");
            if (cfg.exists()) {
                String cfgDst = gamePakDir + "grow_settings.txt";
                boolean cfgOk = ShizukuHelper.copyFile(cfg.getAbsolutePath(), cfgDst);
                Log.d(TAG, "Config copied: " + cfgOk);
            }

            String finalCheck = ShizukuHelper.exec("ls -la " + gamePakDir);
            Log.d(TAG, "Final dir contents:\n" + finalCheck);

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
