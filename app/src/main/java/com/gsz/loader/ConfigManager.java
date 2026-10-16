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

    /**
     * Write grow_settings.txt in Lua format that the mod reads.
     * Format matches SaveModSettings() in growzsmd.lua
     */
    public void writeLuaConfig() {
        boolean esp = isEspEnabled();
        boolean aimbot = isAimbotEnabled();

        StringBuilder sb = new StringBuilder();
        sb.append("return {
");
        sb.append("LexusConfig = {
");
        sb.append("  [\"EspVipPro\"] = ").append(esp).append(",
");
        sb.append("  [\"EspLoai9\"] = ").append(esp).append(",
");
        sb.append("  [\"EspRadar\"] = ").append(esp).append(",
");
        sb.append("  [\"EspLoai5\"] = ").append(esp).append(",
");
        sb.append("  [\"EspLoai6\"] = ").append(esp).append(",
");
        sb.append("  [\"EspLoai7\"] = ").append(esp).append(",
");
        sb.append("  [\"EspLoai8\"] = ").append(esp).append(",
");
        sb.append("  [\"BoxESPEnable\"] = ").append(esp).append(",
");
        sb.append("  [\"EspItem_Master\"] = ").append(esp).append(",
");
        sb.append("  [\"EspVehicle\"] = ").append(esp).append(",
");
        sb.append("  [\"EspBomMaster\"] = ").append(esp).append(",
");
        sb.append("  [\"EspAimWarning\"] = ").append(esp).append(",
");
        sb.append("  [\"EspAntenna\"] = ").append(esp).append(",
");
        sb.append("  [\"EspOutline\"] = ").append(esp).append(",
");
        sb.append("  [\"AimTouchEnable\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AimTouchHipfire\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AimTouchScopeAll\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AimTouchScopeSniper\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AimTouchSG\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AimTouchMortar\"] = ").append(aimbot).append(",
");
        sb.append("  [\"CustomAimbot\"] = ").append(aimbot).append(",
");
        sb.append("  [\"CustomAimbotClose\"] = ").append(aimbot).append(",
");
        sb.append("  [\"AutoHead\"] = ").append(aimbot).append(",
");
        sb.append("},
");
        sb.append("CustomTextData = {}
");
        sb.append("}");

        String lua = sb.toString();

        // Write to internal storage first
        try {
            File f = new File(ctx.getFilesDir(), "grow_settings.txt");
            FileWriter fw = new FileWriter(f);
            fw.write(lua);
            fw.close();
            Log.d(TAG, "Config written to internal: " + f.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write internal config", e);
        }

        // Copy to game folder via shizuku/root
        String gamePkg = getGamePackage();
        if (!gamePkg.isEmpty()) {
            String gamePath = ShizukuHelper.getGamePakDir(gamePkg) + "grow_settings.txt";
            boolean ok = ShizukuHelper.writeFile(gamePath, lua);
            Log.d(TAG, "Config copied to game: " + gamePath + " = " + ok);
        }
    }

    /**
     * Read current config from game folder (if exists)
     */
    public String readGameConfig() {
        String gamePkg = getGamePackage();
        if (gamePkg.isEmpty()) return null;
        String path = ShizukuHelper.getGamePakDir(gamePkg) + "grow_settings.txt";
        return ShizukuHelper.readFile(path);
    }
}
