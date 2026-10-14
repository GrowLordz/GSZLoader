package com.gsz.loader;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREFS = "gsz_config";
    private static final String KEY_ESP = "esp_enabled";
    private static final String KEY_AIMBOT = "aimbot_enabled";
    private static final String KEY_GAME_PKG = "game_package";

    private final SharedPreferences prefs;

    public ConfigManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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

    // Tulis config yang dibaca oleh lua (grow_settings.txt format)
    private void writeLuaConfig() {
        boolean esp = isEspEnabled();
        boolean aimbot = isAimbotEnabled();

        String lua = "return {\n"
                + "LexusConfig = {\n"
                + "  [\"EspVipPro\"] = " + esp + ",\n"
                + "  [\"EspLoai9\"] = " + esp + ",\n"
                + "  [\"EspRadar\"] = " + esp + ",\n"
                + "  [\"EspLoai5\"] = " + esp + ",\n"
                + "  [\"EspLoai6\"] = " + esp + ",\n"
                + "  [\"EspLoai7\"] = " + esp + ",\n"
                + "  [\"EspLoai8\"] = " + esp + ",\n"
                + "  [\"AimTouchEnable\"] = " + aimbot + ",\n"
                + "  [\"AimTouchHipfire\"] = " + aimbot + ",\n"
                + "  [\"AimTouchScopeAll\"] = " + aimbot + ",\n"
                + "  [\"AimTouchScopeSniper\"] = " + aimbot + ",\n"
                + "  [\"AimTouchSG\"] = " + aimbot + ",\n"
                + "},\n"
                + "CustomTextData = {}\n"
                + "}";

        // Simpan ke internal storage, nanti di-copy ke game folder via shizuku
        try {
            java.io.File f = new java.io.File(
                android.os.Environment.getExternalStorageDirectory(),
                "Android/data/" + getGamePackage() + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/grow_settings.txt"
            );
            // Fallback: tulis ke app internal dulu
            java.io.File internal = new java.io.File(
                "/data/data/com.gsz.loader/files/grow_settings.txt"
            );
            java.io.FileWriter fw = new java.io.FileWriter(internal);
            fw.write(lua);
            fw.close();
        } catch (Exception ignored) {}
    }
}
