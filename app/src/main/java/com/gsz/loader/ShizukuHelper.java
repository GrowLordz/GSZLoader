package com.gsz.loader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

public final class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static boolean isRoot = false;
    private static Boolean rootChecked = null;

    private ShizukuHelper() {}

    public static boolean isShizukuAvailable() {
        try {
            Shizuku.pingBinder();
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
        } catch (Exception e) {
            isRoot = false;
            rootChecked = true;
        }
        return isRoot;
    }

    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestPermission(int reqCode) {
        try {
            Shizuku.requestPermission(reqCode);
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
        }
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
            Process p = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
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
            return false;
        }
    }

    private static boolean writeFileShizuku(String absPath, String content) {
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            String parent = new File(absPath).getParent();
            if (parent != null) {
                Process mk = Shizuku.newProcess(new String[]{"sh", "-c", "mkdir -p '" + parent + "'"}, null, null);
                mk.waitFor();
                mk.destroy();
            }
            Process p = Shizuku.newProcess(new String[]{
                    "sh", "-c", "cat > '" + absPath + "' && chmod 644 '" + absPath + "'"
            }, null, null);
            OutputStream os = p.getOutputStream();
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
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
            Process p = Shizuku.newProcess(
                    new String[]{"sh", "-c", "cat '" + absPath + "' 2>/dev/null"}, null, null);
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
            return false;
        }
    }

    private static boolean copyFileShizuku(String src, String dst) {
        if (!isBinderAlive() || !hasPermission()) return false;
        try {
            Process p = Shizuku.newProcess(new String[]{
                    "sh", "-c", "cp '" + src + "' '" + dst + "' && chmod 644 '" + dst + "'"
            }, null, null);
            int code = p.waitFor();
            p.destroy();
            return code == 0;
        } catch (Throwable t) {
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
            Process p = Shizuku.newProcess(new String[]{"sh", "-c", "mkdir -p '" + path + "'"}, null, null);
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

    public static String detectInstalledGame(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        String[] pkgs = {
                "com.pubg.imobile", "com.tencent.ig", "com.tencent.igfit",
                "com.pubg.krmobile", "com.vng.pubgmobile", "com.rekoo.pubgm",
                "com.pubg.mobile", "com.tencent.iglite", "com.pubg.mobilelite",
                "com.tencent.tmgp.pubgmhd", "com.tencent.tmgp.pubgm"
        };
        for (String pkg : pkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    public static String getGamePakDir(String gamePkg) {
        String[] paths = {
            "/sdcard/Android/data/" + gamePkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/",
            "/storage/emulated/0/Android/data/" + gamePkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/",
            "/sdcard/Android/data/" + gamePkg + "/files/UE4Game/ShadowTrackerExtra/Saved/Paks/",
        };
        
        for (String path : paths) {
            String check = exec("test -d '" + path + "' && echo EXISTS || echo NOT_FOUND");
            if (check.contains("EXISTS")) {
                Log.d(TAG, "Found pak dir: " + path);
                return path;
            }
        }
        
        return paths[0];
    }

    public static String getGameSaveDir(String gamePkg) {
        return "/sdcard/Android/data/" + gamePkg
                + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/";
    }

    public static boolean fileExists(String path) {
        String result = exec("test -f '" + path + "' && echo EXISTS || echo NOT_FOUND");
        return result.contains("EXISTS");
    }

    public static String getFileSize(String path) {
        return exec("stat -c %s '" + path + "' 2>/dev/null || echo 0");
    }
}
