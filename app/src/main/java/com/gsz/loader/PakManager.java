package com.gsz.loader;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PakManager {

    private static final String PAK_DIR = "paks";

    public static void copyPaks(Context ctx, IShizukuShell shell, String gamePkg) throws Exception {
        // Step 1: extract paks dari assets ke app internal
        AssetManager am = ctx.getAssets();
        String[] files = am.list(PAK_DIR);
        if (files == null || files.length == 0) return;

        File internalDir = new File(ctx.getFilesDir(), "paks");
        if (!internalDir.exists()) internalDir.mkdirs();

        for (String f : files) {
            if (!f.endsWith(".pak")) continue;
            File out = new File(internalDir, f);
            if (out.exists()) continue;

            InputStream is = am.open(PAK_DIR + "/" + f);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.close();
            is.close();
        }

        // Step 2: copy ke game folder via shizuku
        String gamePakDir = "/sdcard/Android/data/" + gamePkg
            + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";

        // Buat direktori kalau belum ada
        shell.exec("mkdir -p " + gamePakDir);

        // Copy semua paks
        for (File f : internalDir.listFiles()) {
            if (f.getName().endsWith(".pak")) {
                shell.exec("cp " + f.getAbsolutePath() + " " + gamePakDir + f.getName());
                shell.exec("chmod 644 " + gamePakDir + f.getName());
            }
        }

        // Step 3: copy config juga
        File cfg = new File(ctx.getFilesDir(), "grow_settings.txt");
        if (cfg.exists()) {
            shell.exec("cp " + cfg.getAbsolutePath() + " " + gamePakDir + "grow_settings.txt");
            shell.exec("chmod 644 " + gamePakDir + "grow_settings.txt");
        }
    }
}
