package com.gsz.loader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PakManager {

    private static final String PAK_DIR = "paks";
    private static final String TAG = "PakManager";

    public static void copyPaks(Context ctx, IShizukuShell shell, String gamePkg) throws Exception {
        AssetManager am = ctx.getAssets();
        String[] files = am.list(PAK_DIR);
        if (files == null || files.length == 0) return;

        File internalDir = new File(ctx.getFilesDir(), "paks");
        if (!internalDir.exists()) {
            boolean created = internalDir.mkdirs();
            if (!created && !internalDir.exists()) {
                throw new IllegalStateException("Could not create internal paks directory");
            }
        }

        for (String f : files) {
            if (f == null || !f.endsWith(".pak")) continue;
            File out = new File(internalDir, f);
            if (out.exists()) continue;

            try (InputStream is = am.open(PAK_DIR + "/" + f);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            }
        }

        String gamePakDir = "/sdcard/Android/data/" + gamePkg
            + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";

        shell.exec("mkdir -p " + gamePakDir);

        File[] localFiles = internalDir.listFiles();
        if (localFiles != null) {
            for (File f : localFiles) {
                if (f == null || !f.getName().endsWith(".pak")) continue;
                try {
                    shell.exec("cp " + f.getAbsolutePath() + " " + gamePakDir + f.getName());
                    shell.exec("chmod 644 " + gamePakDir + f.getName());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to copy pak: " + f.getName(), e);
                }
            }
        }

        File cfg = new File(ctx.getFilesDir(), "grow_settings.txt");
        if (cfg.exists()) {
            try {
                shell.exec("cp " + cfg.getAbsolutePath() + " " + gamePakDir + "grow_settings.txt");
                shell.exec("chmod 644 " + gamePakDir + "grow_settings.txt");
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy config", e);
            }
        }
    }
}
