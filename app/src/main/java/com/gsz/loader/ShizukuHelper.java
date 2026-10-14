package com.gsz.loader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public final class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static Object shizukuBinder = null;
    private static Class<?> shizukuClass = null;

    static {
        try {
            shizukuClass = Class.forName("rikka.shizuku.Shizuku");
        } catch (Throwable ignored) {}
    }

    private ShizukuHelper() {}

    public static boolean isBinderAlive() {
        try {
            if (shizukuClass == null) return false;
            Method m = shizukuClass.getMethod("pingBinder");
            return (boolean) m.invoke(null);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            if (!isBinderAlive()) return false;
            Method m = shizukuClass.getMethod("checkSelfPermission");
            int result = (int) m.invoke(null);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestPermission(int reqCode) {
        try {
            Method m = shizukuClass.getMethod("requestPermission", int.class);
            m.invoke(null, reqCode);
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
        }
    }

    private static Process shizukuNewProcess(String[] cmd) throws Exception {
        Method m = shizukuClass.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, cmd, null, null);
    }

    public static boolean writeFileAsShell(String absPath, String content) {
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            String parent = new File(absPath).getParent();
            if (parent != null) {
                Process mk = shizukuNewProcess(new String[]{"sh", "-c", "mkdir -p '" + parent + "'"});
                mk.waitFor();
                mk.destroy();
            }
            Process p = shizukuNewProcess(new String[]{"sh", "-c", "cat > '" + absPath + "' && chmod 644 '" + absPath + "'"});
            OutputStream os = p.getOutputStream();
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
            Log.e(TAG, "writeFileAsShell failed", t);
            return false;
        }
    }

    public static String readFileAsShell(String absPath) {
        if (!isBinderAlive() || !hasPermission()) return null;
        try {
            Process p = shizukuNewProcess(new String[]{"sh", "-c", "cat '" + absPath + "' 2>/dev/null"});
            InputStream is = p.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            p.waitFor();
            p.destroy();
            String out = bos.toString(StandardCharsets.UTF_8);
            return out.isEmpty() ? null : out;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean copyFileAsShell(String src, String dst) {
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            Process p = shizukuNewProcess(new String[]{"sh", "-c", "cp '" + src + "' '" + dst + "' && chmod 644 '" + dst + "'"});
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String detectInstalledGame(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        String[] pkgs = {
            "com.pubg.imobile", "com.tencent.ig", "com.tencent.igfit",
            "com.pubg.krmobile", "com.vng.pubgmobile", "com.rekoo.pubgm"
        };
        for (String pkg : pkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }
}
