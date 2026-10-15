package com.gsz.loader;

import android.os.Build;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


public class ShizukuShellService extends IShizukuShell.Stub {

    public ShizukuShellService() {}

    @Override
    public String exec(String command) throws RemoteException {
        StringBuilder output = new StringBuilder();
        try {
            Process process;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Try to call Shizuku.newProcess via reflection so compilation doesn't require the Shizuku API.
                    Class<?> shizukuClass = Class.forName("moe.shizuku.api.Shizuku");
                    java.lang.reflect.Method newProcessMethod =
                            shizukuClass.getMethod("newProcess", String[].class, String[].class, File.class);
                    Object procObj = newProcessMethod.invoke(null,
                            new Object[]{new String[]{"sh", "-c", command}, null, null});
                    process = (Process) procObj;
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    // Shizuku API not present at compile/runtime — fall back to normal exec
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
}
