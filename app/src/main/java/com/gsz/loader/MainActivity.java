package com.gsz.loader;

import android.app.Activity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int REQ_PERMISSION = 1001;
    private static final int REQ_OVERLAY = 1002;

    private TextView tvStatus, tvGamePkg, tvAccessMode;
    private Button btnStartMenu, btnCopyPaks, btnLaunchGame, btnKillGame, btnCheckAccess;
    private ProgressBar progressBar;
    private ConfigManager config;
    private Handler handler;
    private Shizuku.OnRequestPermissionResultListener permListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed", e);
            Toast.makeText(this, "Layout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        config = new ConfigManager(this);
        handler = new Handler(Looper.getMainLooper());

        initViews();

        try {
            permListener = (requestCode, grantResult) -> {
                if (requestCode == REQ_PERMISSION) {
                    runOnUiThread(() -> {
                        try {
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                tvAccessMode.setText("Access: SHIZUKU");
                                tvAccessMode.setTextColor(0xFF4CAF50);
                                tvStatus.setText("Shizuku connected");
                                tvStatus.setTextColor(0xFF4CAF50);
                                Toast.makeText(MainActivity.this, "Shizuku permission granted!", Toast.LENGTH_SHORT).show();
                            } else {
                                tvStatus.setText("Shizuku permission denied");
                                tvStatus.setTextColor(0xFFF44336);
                                showPermissionDeniedDialog();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Permission callback error", e);
                        }
                    });
                }
            };
            
            try {
                Shizuku.addRequestPermissionResultListener(permListener);
            } catch (Throwable t) {
                Log.e(TAG, "Failed to register Shizuku listener", t);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to create permission listener", t);
        }

        checkAccess();
    }

    private void initViews() {
        try {
            tvStatus = findViewById(R.id.tvStatus);
            tvGamePkg = findViewById(R.id.tvGamePkg);
            tvAccessMode = findViewById(R.id.tvAccessMode);
            btnStartMenu = findViewById(R.id.btnStartMenu);
            btnCopyPaks = findViewById(R.id.btnCopyPaks);
            btnLaunchGame = findViewById(R.id.btnLaunchGame);
            btnKillGame = findViewById(R.id.btnKillGame);
            btnCheckAccess = findViewById(R.id.btnCheckAccess);
            progressBar = findViewById(R.id.progressBar);

            if (btnCheckAccess != null) {
                btnCheckAccess.setOnClickListener(v -> checkAccess());
            }

            if (btnCopyPaks != null) {
                btnCopyPaks.setOnClickListener(v -> showCopyDialog());
            }

            if (btnLaunchGame != null) {
                btnLaunchGame.setOnClickListener(v -> {
                    String pkg = config.getGamePackage();
                    if (!pkg.isEmpty()) {
                        GameLauncher.launchGame(this, pkg);
                    } else {
                        Toast.makeText(this, "No game detected!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (btnKillGame != null) {
                btnKillGame.setOnClickListener(v -> {
                    String pkg = config.getGamePackage();
                    if (!pkg.isEmpty()) {
                        GameLauncher.killGame(pkg);
                        Toast.makeText(this, "Game killed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (btnStartMenu != null) {
                btnStartMenu.setOnClickListener(v -> {
                    if (!Settings.canDrawOverlays(this)) {
                        requestOverlayPermission();
                    } else {
                        startFloatingMenu();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "initViews failed", e);
        }
    }

    private void checkAccess() {
        Log.d(TAG, "=== checkAccess started ===");

        try {
            String gamePkg = null;
            try {
                gamePkg = ShizukuHelper.detectInstalledGame(this);
            } catch (Exception e) {
                Log.e(TAG, "Game detection failed", e);
            }
            
            Log.d(TAG, "Game detected: " + gamePkg);

            if (gamePkg != null) {
                config.setGamePackage(gamePkg);
                if (tvGamePkg != null) {
                    tvGamePkg.setText("Game: " + gamePkg);
                    tvGamePkg.setTextColor(0xFF4CAF50);
                }
            } else {
                if (tvGamePkg != null) {
                    tvGamePkg.setText("Game: Not found");
                    tvGamePkg.setTextColor(0xFFF44336);
                }
            }

            boolean hasRoot = false;
            try {
                hasRoot = ShizukuHelper.isRootAvailable();
            } catch (Exception e) {
                Log.e(TAG, "Root check failed", e);
            }
            
            Log.d(TAG, "Root available: " + hasRoot);

            if (hasRoot) {
                if (tvAccessMode != null) {
                    tvAccessMode.setText("Access: ROOT");
                    tvAccessMode.setTextColor(0xFF4CAF50);
                }
                if (tvStatus != null) {
                    tvStatus.setText("Root access granted");
                    tvStatus.setTextColor(0xFF4CAF50);
                }
                return;
            }

            boolean shizukuAvail = false;
            try {
                shizukuAvail = ShizukuHelper.isShizukuAvailable();
            } catch (Throwable t) {
                Log.e(TAG, "Shizuku availability check failed", t);
            }
            
            Log.d(TAG, "Shizuku available: " + shizukuAvail);

            if (!shizukuAvail) {
                if (tvAccessMode != null) {
                    tvAccessMode.setText("Access: NONE");
                    tvAccessMode.setTextColor(0xFFF44336);
                }
                if (tvStatus != null) {
                    tvStatus.setText("Shizuku not installed");
                    tvStatus.setTextColor(0xFFF44336);
                }
                showShizukuInstallDialog();
                return;
            }

            boolean binderAlive = false;
            try {
                binderAlive = ShizukuHelper.isBinderAlive();
            } catch (Throwable t) {
                Log.e(TAG, "Binder check failed", t);
            }
            
            Log.d(TAG, "Shizuku binder alive: " + binderAlive);

            if (!binderAlive) {
                if (tvAccessMode != null) {
                    tvAccessMode.setText("Access: SHIZUKU (stopped)");
                    tvAccessMode.setTextColor(0xFFFF9800);
                }
                if (tvStatus != null) {
                    tvStatus.setText("Start Shizuku service first");
                    tvStatus.setTextColor(0xFFFF9800);
                }
                return;
            }

            boolean hasPerm = false;
            try {
                hasPerm = ShizukuHelper.hasPermission();
            } catch (Throwable t) {
                Log.e(TAG, "Permission check failed", t);
            }
            
            Log.d(TAG, "Shizuku permission: " + hasPerm);

            if (hasPerm) {
                if (tvAccessMode != null) {
                    tvAccessMode.setText("Access: SHIZUKU");
                    tvAccessMode.setTextColor(0xFF4CAF50);
                }
                if (tvStatus != null) {
                    tvStatus.setText("Shizuku connected");
                    tvStatus.setTextColor(0xFF4CAF50);
                }
            } else {
                if (tvAccessMode != null) {
                    tvAccessMode.setText("Access: SHIZUKU (no perm)");
                    tvAccessMode.setTextColor(0xFFFF9800);
                }
                if (tvStatus != null) {
                    tvStatus.setText("Requesting permission...");
                    tvStatus.setTextColor(0xFFFF9800);
                }
                
                try {
                    ShizukuHelper.requestPermission(REQ_PERMISSION);
                } catch (Throwable t) {
                    Log.e(TAG, "Permission request failed", t);
                    showPermissionDeniedDialog();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "checkAccess failed", e);
            if (tvStatus != null) {
                tvStatus.setText("Error: " + e.getMessage());
                tvStatus.setTextColor(0xFFF44336);
            }
        }

        Log.d(TAG, "=== checkAccess finished ===");
    }

    private void showPermissionDeniedDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("GSZ Loader needs Shizuku permission to work.\n\n" +
                            "Please grant permission in Shizuku app:\n" +
                            "1. Open Shizuku\n" +
                            "2. Go to 'Authorized applications'\n" +
                            "3. Find GSZ Loader and enable it")
                    .setPositiveButton("OPEN SHIZUKU", (d, w) -> {
                        try {
                            Intent intent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                            if (intent != null) {
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Shizuku not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("RETRY", (d, w) -> checkAccess())
                    .setNeutralButton("FORCE CONTINUE", (d, w) -> {
                        if (tvAccessMode != null) {
                            tvAccessMode.setText("Access: FORCED");
                            tvAccessMode.setTextColor(0xFFFF9800);
                        }
                        if (tvStatus != null) {
                            tvStatus.setText("Forced mode - may not work");
                            tvStatus.setTextColor(0xFFFF9800);
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "showPermissionDeniedDialog failed", e);
        }
    }

    private void showShizukuInstallDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Shizuku Required")
                    .setMessage("This app needs Shizuku for non-root access.\n\n" +
                            "1. Install Shizuku from Play Store\n" +
                            "2. Start Shizuku via wireless debugging or adb\n" +
                            "3. Grant permission to this app\n\n" +
                            "Or use a rooted device.")
                    .setPositiveButton("INSTALL", (d, w) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=moe.shizuku.privileged.api")));
                        } catch (Exception e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")));
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .setNeutralButton("FORCE CONTINUE", (d, w) -> {
                        if (tvAccessMode != null) {
                            tvAccessMode.setText("Access: FORCED");
                            tvAccessMode.setTextColor(0xFFFF9800);
                        }
                        if (tvStatus != null) {
                            tvStatus.setText("Forced mode - may not work");
                            tvStatus.setTextColor(0xFFFF9800);
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "showShizukuInstallDialog failed", e);
        }
    }

    private void showCopyDialog() {
        String gamePkg = config.getGamePackage();
        if (gamePkg.isEmpty()) {
            Toast.makeText(this, "No game detected!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!PakManager.arePaksInAssets(this)) {
            Toast.makeText(this, "No paks found in app assets!", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Copy Paks")
                .setMessage("This will copy mod files to:\n" +
                        ShizukuHelper.getGamePakDir(gamePkg) + "\n\n" +
                        "Make sure game is NOT running.")
                .setPositiveButton("Copy", (d, w) -> doCopyPaks(gamePkg))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doCopyPaks(String gamePkg) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnCopyPaks != null) btnCopyPaks.setEnabled(false);
        if (tvStatus != null) {
            tvStatus.setText("Copying paks...");
            tvStatus.setTextColor(0xFFFF9800);
        }

        new Thread(() -> {
            PakManager.copyPaks(this, gamePkg, new PakManager.CopyCallback() {
                @Override
                public void onProgress(String fileName, int current, int total) {
                    handler.post(() -> {
                        if (tvStatus != null) {
                            tvStatus.setText("Copying: " + fileName + " (" + current + "/" + total + ")");
                        }
                    });
                }

                @Override
                public void onComplete(boolean success, String message) {
                    handler.post(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (btnCopyPaks != null) btnCopyPaks.setEnabled(true);

                        if (success) {
                            config.setPaksCopied(true);
                            if (tvStatus != null) {
                                tvStatus.setText("Paks copied!");
                                tvStatus.setTextColor(0xFF4CAF50);
                            }
                            Toast.makeText(MainActivity.this, "Success! Now launch game.", Toast.LENGTH_LONG).show();
                            config.writeLuaConfig();
                        } else {
                            if (tvStatus != null) {
                                tvStatus.setText("Copy failed: " + message);
                                tvStatus.setTextColor(0xFFF44336);
                            }
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }).start();
    }

    private void requestOverlayPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        } catch (Exception e) {
            Log.e(TAG, "requestOverlayPermission failed", e);
        }
    }

    private void startFloatingMenu() {
        try {
            Intent intent = new Intent(this, FloatingMenuService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Toast.makeText(this, "Floating menu started!", Toast.LENGTH_SHORT).show();
            moveTaskToBack(true);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start floating menu", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingMenu();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAccess();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (permListener != null) {
                Shizuku.removeRequestPermissionResultListener(permListener);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to remove listener", t);
        }
        Log.d(TAG, "MainActivity destroyed");
    }
}
