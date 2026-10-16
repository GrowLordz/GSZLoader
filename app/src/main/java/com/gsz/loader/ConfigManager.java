package com.gsz.loader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;

public class ConfigManager {

    private static final String TAG = "ConfigManager";
    private static final String PREFS = "gsz_config";
    private static final String KEY_ESP = "esp_enabled";
    private static final String KEY_AIMBOT = "aimbot_enabled";
    private static final String KEY_GAME_PKG = "game_package";
    private static final String KEY_PAKS_COPIED = "paks_copied";

    private final SharedPreferences prefs;
    private final Context ctx;

    public ConfigManager(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setEspEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ESP, enabled).apply();
        writeLuaConfig();
    }

    public void setAimbotEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AIMBOT, enabled).apply();
        writeLuaConfig();
    }

    public boolean isEspEnabled() {
        return prefs.getBoolean(KEY_ESP, false);
    }

    public boolean isAimbotEnabled() {
        return prefs.getBoolean(KEY_AIMBOT, false);
    }

    public void setGamePackage(String pkg) {
        prefs.edit().putString(KEY_GAME_PKG, pkg).apply();
    }

    public String getGamePackage() {
        return prefs.getString(KEY_GAME_PKG, "");
    }

    public void setPaksCopied(boolean copied) {
        prefs.edit().putBoolean(KEY_PAKS_COPIED, copied).apply();
    }

    public boolean isPaksCopied() {
        return prefs.getBoolean(KEY_PAKS_COPIED, false);
    }

    public void writeLuaConfig() {
        boolean esp = isEspEnabled();
        boolean aimbot = isAimbotEnabled();

        StringBuilder sb = new StringBuilder();
        sb.append("return {\n");
        sb.append("LexusConfig = {\n");
        sb.append("  [\"EspVipPro\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspLoai9\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspRadar\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspLoai5\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspLoai6\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspLoai7\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspLoai8\"] = ").append(esp).append(",\n");
        sb.append("  [\"BoxESPEnable\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspItem_Master\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspVehicle\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspBomMaster\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspAimWarning\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspAntenna\"] = ").append(esp).append(",\n");
        sb.append("  [\"EspOutline\"] = ").append(esp).append(",\n");
        sb.append("  [\"AimTouchEnable\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AimTouchHipfire\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AimTouchScopeAll\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AimTouchScopeSniper\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AimTouchSG\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AimTouchMortar\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"CustomAimbot\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"CustomAimbotClose\"] = ").append(aimbot).append(",\n");
        sb.append("  [\"AutoHead\"] = ").append(aimbot).append(",\n");
        sb.append("},\n");
        sb.append("CustomTextData = {}\n");
        sb.append("}\n");

        String lua = sb.toString();

        try {
            File f = new File(ctx.getFilesDir(), "grow_settings.txt");
            FileWriter fw = new FileWriter(f);
            fw.write(lua);
            fw.close();
            Log.d(TAG, "Config written to internal");
        } catch (Exception e) {
            Log.e(TAG, "Failed to write internal config", e);
        }

        String gamePkg = getGamePackage();
        if (!gamePkg.isEmpty()) {
            String gamePath = ShizukuHelper.getGamePakDir(gamePkg) + "grow_settings.txt";
            boolean ok = ShizukuHelper.writeFile(gamePath, lua);
            Log.d(TAG, "Config copied to game: " + ok);
        }
    }

    public String readGameConfig() {
        String gamePkg = getGamePackage();
        if (gamePkg.isEmpty()) return null;
        String path = ShizukuHelper.getGamePakDir(gamePkg) + "grow_settings.txt";
        return ShizukuHelper.readFile(path);
    }
}
