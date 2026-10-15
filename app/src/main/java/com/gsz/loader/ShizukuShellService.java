package com.gsz.loader;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Adapter Service that exposes IShizukuShell AIDL via a proper android.app.Service.
 * The previous implementation was a Binder implementation but did not extend Service,
 * which caused Lint (Instantiatable) to fail for release builds. This Service returns
 * an IShizukuShell.Stub binder that delegates to the existing exec implementation.
 */
public class ShizukuShellService extends Service {

    private final IShizukuShell.Stub binder = new IShizukuShell.Stub() {
        @Override
        public String exec(String command) throws RemoteException {
            StringBuilder output = new StringBuilder();
            try {
                Process process;

                // Try to use Shizuku's newProcess via reflection when available; fall back to Runtime.exec().
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        Class<?> shizukuClass = null;
                        // Try several known Shizuku class names to be robust across versions
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
                                // try alternate signature used in other versions
                                newProcessMethod = shizukuClass.getMethod("newProcess", String[].class, String[].class, String.class);
                            }

                            Object procObj = newProcessMethod.invoke(null,
                                    new Object[]{new String[]{"sh", "-c", command}, null, null});
                            process = (Process) procObj;
                        } else {
                            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                        }
                    } catch (Throwable t) {
                        // If anything goes wrong with reflection/use of Shizuku, fall back to Runtime.exec
                        process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                    }
                } else {
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
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
};
