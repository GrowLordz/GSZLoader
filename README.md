# GSZ Loader v2.0

PUBG Mod Loader dengan floating menu overlay. Support root dan non-root (Shizuku).

## Fitur

- Floating menu overlay (drag + expand/collapse animation)
- ESP toggle (on/off)
- Aimbot toggle (on/off)
- Shizuku support (non-root)
- Root support (auto-detect)
- Pak injection ke game folder
- Auto-detect game package (BGMI, PUBG Global, KR, VN, TW)

## Cara Pakai

### Non-Root (Shizuku)

1. Install Shizuku dari Play Store
2. Start Shizuku via wireless debugging atau adb
3. Buka GSZ Loader, grant Shizuku permission
4. Tap "COPY PAKS TO GAME"
5. Tap "LAUNCH GAME"
6. Tap "START FLOATING MENU"
7. Drag icon ke posisi suka, tap untuk expand menu
8. Toggle ESP / Aimbot

### Root

1. Buka GSZ Loader (auto-detect root)
2. Tap "COPY PAKS TO GAME"
3. Tap "LAUNCH GAME"
4. Tap "START FLOATING MENU"

## Cara Kerja Toggle

```
User tap toggle di floating menu
         |
ConfigManager.setEspEnabled(true)
         |
writeLuaConfig() -> generate grow_settings.txt
         |
ShizukuHelper.writeFile() -> copy ke game Paks folder
         |
Lua AutoSaveLoop() baca file tiap 3 detik
         |
_G.LexusConfig.EspVipPro = true
         |
ESP jalan di game
```

## Build

GitHub Actions auto-build on push to main. Download APK dari Artifacts.

## Struktur Config

Loader nulis `grow_settings.txt` ke:
```
/sdcard/Android/data/<pkg>/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/
```

Format sama persis dengan `SaveModSettings()` di lua mod:

```lua
return {
LexusConfig = {
  ["EspVipPro"] = true,
  ["AimTouchEnable"] = true,
  ...
},
CustomTextData = {}
}
```

## Catatan

- Toggle real-time kalau game udah jalan dan lua `AutoSaveLoop` aktif
- Kalau game belum jalan, config kebaca pas `LoadModSettings()` di awal
- Pak injection cuma perlu sekali, kecuali game update
- Root lebih stabil, shizuku kadang ke-kill di background
- Pastikan file .pak taruh di `app/src/main/assets/paks/` sebelum build
