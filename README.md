# GSZ Loader v3.0

PUBG Mod Loader dengan floating menu overlay. Support root dan non-root (Shizuku).

## Changelog v3.0

- Fixed: package detection (14+ package names + full scan fallback)
- Fixed: floating menu crash on android 12+ (FOREGROUND_SERVICE_SPECIAL_USE)
- Fixed: force close on re-open (proper service lifecycle)
- Fixed: overlay not appearing (better error handling + logging)
- Added: FORCE CONTINUE button in Shizuku dialog
- Added: QUERY_ALL_PACKAGES permission for better game detection
- Added: comprehensive logging for debugging

## Fitur

- Floating menu overlay (drag + expand/collapse animation)
- ESP toggle (on/off)
- Aimbot toggle (on/off)
- Shizuku support (non-root)
- Root support (auto-detect)
- Pak injection ke game folder
- Auto-detect game package

## Cara Pakai

### Non-Root (Shizuku)

1. Install Shizuku dari Play Store
2. Start Shizuku via wireless debugging atau adb
3. Buka GSZ Loader, grant Shizuku permission
4. Tap "COPY PAKS TO GAME"
5. Tap "LAUNCH GAME"
6. Tap "START FLOATING MENU"
7. Drag icon, tap untuk expand, toggle ESP/Aimbot

### Root

1. Buka GSZ Loader (auto-detect root)
2. Tap "COPY PAKS TO GAME"
3. Tap "LAUNCH GAME"
4. Tap "START FLOATING MENU"

## Debug

Jika masih ada masalah, jalankan ini dengan adb:

```bash
adb logcat -s MainActivity:D ShizukuHelper:D FloatingMenuService:D
```

Lihat log untuk diagnosis.

## Build

GitHub Actions auto-build on push to main.
