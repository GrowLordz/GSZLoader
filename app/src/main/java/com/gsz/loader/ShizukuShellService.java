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
    private static Class<?> shizukuClass = null;

    static {
        String[] candidates = new String[] {
                "moe.shizuku.api.Shizuku",
                "moe.shizuku.Shizuku",
                "rikka.shizuku.Shizuku"
        };
        for (String c : candidates) {
            try {
                shizukuClass = Class.forName(c);
                break;
            } catch (Throwable ignored) {}
        }
    }

    private ShizukuHelper() {}

    public static boolean isBinderAlive() {
        if (shizukuClass == null) return false;
        for (String methodName : new String[]{"pingBinder", "isBinderAlive"}) {
            try {
                Method m = shizukuClass.getMethod(methodName);
                Object result = m.invoke(null);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean hasPermission() {
        if (shizukuClass == null || !isBinderAlive()) return false;
        try {
            Method m = shizukuClass.getMethod("checkSelfPermission");
            Object result = m.invoke(null);
            if (result instanceof Integer) {
                return ((Integer) result) == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void requestPermission(int reqCode) {
        if (shizukuClass == null) return;
        try {
            Method m = shizukuClass.getMethod("requestPermission", int.class);
            m.invoke(null, reqCode);
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
        }
    }

    private static Process shizukuNewProcess(String[] cmd) throws Exception {
        if (shizukuClass == null) {
            throw new IllegalStateException("Shizuku not available");
        }

        Method m = null;
        Class<?>[][] signatures = new Class<?>[][] {
                {String[].class, String[].class, String.class},
                {String[].class, String[].class, File.class},
                {String[].class, String[].class, String[].class}
        };

        for (Class<?>[] signature : signatures) {
            try {
                m = shizukuClass.getDeclaredMethod("newProcess", signature);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        if (m == null) {
            throw new NoSuchMethodException("newProcess");
        }

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
