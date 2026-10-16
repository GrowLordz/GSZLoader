# GSZ Loader v4.0

PUBG Mod Loader dengan floating menu overlay. Support root dan non-root (Shizuku).

## Changelog v4.0

- Fixed: Shizuku binder detection dengan `addBinderReceivedListenerSticky`
- Fixed: tombol nggak bisa dipencet — touch handler return `false` biar click propagate
- Fixed: crash/FC — defensive coding + proper lifecycle
- Fixed: auto-retry Shizuku connection tiap 1 detik
- Added: close button (X) di floating menu
- Added: comprehensive logging untuk debug

## Fitur

- Floating menu overlay (drag + expand/collapse animation)
- ESP toggle (on/off)
- Aimbot toggle (on/off)
- Shizuku support (non-root)
- Root support (auto-detect)
- Pak injection ke game folder
- Auto-detect game package
- Close button untuk stop floating menu

## Cara Pakai

### Non-Root (Shizuku)

1. Install Shizuku dari Play Store
2. Start Shizuku via wireless debugging atau adb
3. Buka GSZ Loader — tunggu "Shizuku connected"
4. Tap "COPY PAKS TO GAME"
5. Tap "LAUNCH GAME"
6. Tap "START FLOATING MENU"
7. Drag icon, tap untuk expand, toggle ESP/Aimbot
8. Tap X untuk close floating menu

### Root

1. Buka GSZ Loader (auto-detect root)
2. Tap "COPY PAKS TO GAME"
3. Tap "LAUNCH GAME"
4. Tap "START FLOATING MENU"

## Debug

Jika masih ada masalah, jalankan ini dengan adb:

```bash
adb logcat -s MainActivity:D ShizukuHelper:D FloatingMenuService:D PakManager:D
```

## Build

GitHub Actions auto-build on push to main.
