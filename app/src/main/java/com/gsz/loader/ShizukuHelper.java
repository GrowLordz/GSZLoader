package com.gsz.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static Class<?> shizukuClass = null;
    private static boolean isRoot = false;
    private static Boolean rootChecked = null;

    static {
        try {
            shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Log.d(TAG, "Shizuku class loaded via reflection");
        } catch (Throwable t) {
            Log.e(TAG, "Shizuku class not found: " + t.getMessage());
        }
    }

    private ShizukuHelper() {
    }

    public static boolean isShizukuAvailable() {
        if (shizukuClass != null) return true;
        try {
            shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isRootAvailable() {
        if (rootChecked != null) return isRoot;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo root_test"});
            int exit = p.waitFor();
            isRoot = (exit == 0);
            rootChecked = true;
            Log.d(TAG, "Root check: " + isRoot);
        } catch (Exception e) {
            isRoot = false;
            rootChecked = true;
        }
        return isRoot;
    }

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
            if (shizukuClass == null) return;
            Method m = shizukuClass.getMethod("requestPermission", int.class);
            m.invoke(null, reqCode);
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
        }
    }

    private static Process shizukuNewProcess(String[] cmd) throws Exception {
        Method m = shizukuClass.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, cmd, null, null);
    }

    public static String exec(String command) {
        if (isRootAvailable()) return execRoot(command);
        return execShizuku(command);
    }

    private static String execRoot(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            InputStream is = p.getInputStream();
            InputStream es = p.getErrorStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ByteArrayOutputStream bes = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            while ((n = es.read(buf)) > 0) bes.write(buf, 0, n);
            is.close();
            es.close();
            p.waitFor();
            p.destroy();
            output.append(bos.toString(StandardCharsets.UTF_8));
            String err = bes.toString(StandardCharsets.UTF_8);
            if (!err.isEmpty()) output.append("ERR: ").append(err);
        } catch (Exception e) {
            output.append("EXCEPTION: ").append(e.getMessage());
        }
        return output.toString();
    }

    private static String execShizuku(String command) {
        StringBuilder output = new StringBuilder();
        try {
            if (!isBinderAlive() || !hasPermission()) return "ERROR: no shizuku permission";
            Process p = shizukuNewProcess(new String[]{"sh", "-c", command});
            InputStream is = p.getInputStream();
            InputStream es = p.getErrorStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ByteArrayOutputStream bes = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            while ((n = es.read(buf)) > 0) bes.write(buf, 0, n);
            is.close();
            es.close();
            p.waitFor();
            p.destroy();
            output.append(bos.toString(StandardCharsets.UTF_8));
            String err = bes.toString(StandardCharsets.UTF_8);
            if (!err.isEmpty()) output.append("ERR: ").append(err);
        } catch (Exception e) {
            output.append("EXCEPTION: ").append(e.getMessage());
        }
        return output.toString();
    }

    public static boolean writeFile(String absPath, String content) {
        if (isRootAvailable()) return writeFileRoot(absPath, content);
        return writeFileShizuku(absPath, content);
    }

    private static boolean writeFileRoot(String absPath, String content) {
        try {
            String parent = new File(absPath).getParent();
            if (parent != null) {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "mkdir -p '" + parent + "'"}).waitFor();
            }
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "cat > '" + absPath + "' && chmod 644 '" + absPath + "'"});
            OutputStream os = p.getOutputStream();
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Exception e) {
            Log.e(TAG, "writeFileRoot failed", e);
            return false;
        }
    }

    private static boolean writeFileShizuku(String absPath, String content) {
        if (!isBinderAlive() || !hasPermission()) {
            Log.e(TAG, "writeFileShizuku: no shizuku permission");
            return false;
        }
        try {
            String parent = new File(absPath).getParent();
            if (parent != null) {
                Process mk = shizukuNewProcess(new String[]{"sh", "-c", "mkdir -p '" + parent + "'"});
                mk.waitFor();
                mk.destroy();
            }
            Process p = shizukuNewProcess(new String[]{
                    "sh", "-c", "cat > '" + absPath + "' && chmod 644 '" + absPath + "'"
            });
            OutputStream os = p.getOutputStream();
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
            Log.e(TAG, "writeFileShizuku failed", t);
            return false;
        }
    }

    public static String readFile(String absPath) {
        if (isRootAvailable()) return readFileRoot(absPath);
        return readFileShizuku(absPath);
    }

    private static String readFileRoot(String absPath) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "cat '" + absPath + "' 2>/dev/null"});
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
        } catch (Exception e) {
            return null;
        }
    }

    private static String readFileShizuku(String absPath) {
        if (!isBinderAlive() || !hasPermission()) return null;
        try {
            Process p = shizukuNewProcess(
                    new String[]{"sh", "-c", "cat '" + absPath + "' 2>/dev/null"});
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

    public static boolean copyFile(String src, String dst) {
        if (isRootAvailable()) return copyFileRoot(src, dst);
        return copyFileShizuku(src, dst);
    }

    private static boolean copyFileRoot(String src, String dst) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "cp '" + src + "' '" + dst + "' && chmod 644 '" + dst + "'"});
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Exception e) {
            Log.e(TAG, "copyFileRoot failed", e);
            return false;
        }
    }

    private static boolean copyFileShizuku(String src, String dst) {
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            Process p = shizukuNewProcess(new String[]{
                    "sh", "-c", "cp '" + src + "' '" + dst + "' && chmod 644 '" + dst + "'"
            });
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
            Log.e(TAG, "copyFileShizuku failed", t);
            return false;
        }
    }

    public static boolean mkdir(String path) {
        if (isRootAvailable()) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "mkdir -p '" + path + "'"});
                int code = p.waitFor();
                p.destroy();
                return code == 0;
            } catch (Exception e) {
                return false;
            }
        }
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            Process p = shizukuNewProcess(new String[]{"sh", "-c", "mkdir -p '" + path + "'"});
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void killGame(String packageName) {
        exec("am force-stop " + packageName);
    }

    /**
     * FIXED: More robust game detection with multiple methods
     */
    public static String detectInstalledGame(Context ctx) {
        PackageManager pm = ctx.getPackageManager();

        // Method 1: Check known packages directly
        String[] knownPkgs = {
                "com.pubg.imobile",
                "com.tencent.ig",
                "com.tencent.igfit",
                "com.pubg.krmobile",
                "com.vng.pubgmobile",
                "com.rekoo.pubgm",
                "com.pubg.mobile",
                "com.tencent.iglite",
                "com.pubg.mobilelite",
                "com.tencent.tmgp.pubgmhd",
                "com.tencent.tmgp.pubgm",
                "com.garena.game.kgvn",
                "com.garena.game.pkgametw",
                "com.garena.game.pkgame"
        };

        for (String pkg : knownPkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                Log.d(TAG, "Game found (direct): " + pkg);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        // Method 2: Query all installed packages with PUBG-like names
        try {
            List<PackageInfo> allPackages = pm.getInstalledPackages(0);
            for (PackageInfo pi : allPackages) {
                String name = pi.packageName.toLowerCase();
                if (name.contains("pubg") || name.contains("tencent.ig") ||
                    name.contains("shadowtracker") || name.contains("battleground")) {
                    Log.d(TAG, "Game found (scan): " + pi.packageName);
                    return pi.packageName;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Package scan failed: " + e.getMessage());
        }

        // Method 3: Check launch intents
        try {
            Intent launchIntent = pm.getLaunchIntentForPackage("com.tencent.ig");
            if (launchIntent != null) return "com.tencent.ig";

            launchIntent = pm.getLaunchIntentForPackage("com.pubg.imobile");
            if (launchIntent != null) return "com.pubg.imobile";
        } catch (Exception ignored) {
        }

        Log.e(TAG, "No game package detected!");
        return null;
    }

    public static String getGamePakDir(String gamePkg) {
        return "/sdcard/Android/data/" + gamePkg
                + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
    }

    public static String getGameSaveDir(String gamePkg) {
        return "/sdcard/Android/data/" + gamePkg
                + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/";
    }
}
