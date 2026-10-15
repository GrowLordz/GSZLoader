package com.gsz.loader;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class ShizukuShellService extends Service {

    private final IShizukuShell.Stub binder = new IShizukuShell.Stub() {
        @Override
        public String exec(String command) throws RemoteException {
            StringBuilder output = new StringBuilder();
            try {
                Process process;

                // Try to use Shizuku's newProcess via reflection when available; fall back to Runtime.exec().
                try {
                    Class<?> shizukuClass = null;
                    String[] candidates = new String[]{
                            "moe.shizuku.api.Shizuku",
                            "moe.shizuku.Shizuku",
                            "rikka.shizuku.Shizuku"
                    };
                    for (String c : candidates) {
                        try {
                            shizukuClass = Class.forName(c);
                            break;
                        } catch (ClassNotFoundException ignored) {}
                    }

                    if (shizukuClass != null) {
                        java.lang.reflect.Method newProcessMethod = null;
                        try {
                            newProcessMethod = shizukuClass.getMethod("newProcess", String[].class, String[].class, File.class);
                        } catch (NoSuchMethodException ignored) {
                            try {
                                newProcessMethod = shizukuClass.getMethod("newProcess", String[].class, String[].class, String.class);
                            } catch (NoSuchMethodException ignored2) {
                                newProcessMethod = null;
                            }
                        }

                        if (newProcessMethod != null) {
                            Object procObj = newProcessMethod.invoke(null,
                                    new Object[]{new String[]{"sh", "-c", command}, null, null});
                            process = (Process) procObj;
                        } else {
                            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                        }
                    } else {
                        process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                    }
                } catch (Throwable t) {
                    process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errReader.readLine()) != null) {
                    output.append("ERR: ").append(line).append("\n");
                }
                process.waitFor();
            } catch (Exception e) {
                output.append("EXCEPTION: ").append(e.getMessage());
            }
            return output.toString();
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
