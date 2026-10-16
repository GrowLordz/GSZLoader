package com.gsz.loader;

import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_PERMISSION = 1001;
    private static final int REQ_OVERLAY = 1002;

    private TextView tvStatus, tvGamePkg, tvAccessMode;
    private Button btnStartMenu, btnCopyPaks, btnLaunchGame, btnKillGame, btnCheckAccess;
    private ProgressBar progressBar;
    private ConfigManager config;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = new ConfigManager(this);
        handler = new Handler(Looper.getMainLooper());

        initViews();
        checkAccess();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvGamePkg = findViewById(R.id.tvGamePkg);
        tvAccessMode = findViewById(R.id.tvAccessMode);
        btnStartMenu = findViewById(R.id.btnStartMenu);
        btnCopyPaks = findViewById(R.id.btnCopyPaks);
        btnLaunchGame = findViewById(R.id.btnLaunchGame);
        btnKillGame = findViewById(R.id.btnKillGame);
        btnCheckAccess = findViewById(R.id.btnCheckAccess);
        progressBar = findViewById(R.id.progressBar);

        btnCheckAccess.setOnClickListener(v -> checkAccess());

        btnCopyPaks.setOnClickListener(v -> showCopyDialog());

        btnLaunchGame.setOnClickListener(v -> {
            String pkg = config.getGamePackage();
            if (!pkg.isEmpty()) {
                GameLauncher.launchGame(this, pkg);
            } else {
                Toast.makeText(this, "No game detected!", Toast.LENGTH_LONG).show();
            }
        });

        btnKillGame.setOnClickListener(v -> {
            String pkg = config.getGamePackage();
            if (!pkg.isEmpty()) {
                GameLauncher.killGame(pkg);
                Toast.makeText(this, "Game killed", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartMenu.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else {
                startFloatingMenu();
            }
        });
    }

    private void checkAccess() {
        Log.d(TAG, "=== checkAccess started ===");

        // Detect game
        String gamePkg = ShizukuHelper.detectInstalledGame(this);
        Log.d(TAG, "Game detected: " + gamePkg);

        if (gamePkg != null) {
            config.setGamePackage(gamePkg);
            tvGamePkg.setText("Game: " + gamePkg);
            tvGamePkg.setTextColor(0xFF4CAF50);
        } else {
            tvGamePkg.setText("Game: Not found");
            tvGamePkg.setTextColor(0xFFF44336);
        }

        // Check root
        boolean hasRoot = ShizukuHelper.isRootAvailable();
        Log.d(TAG, "Root available: " + hasRoot);

        if (hasRoot) {
            tvAccessMode.setText("Access: ROOT");
            tvAccessMode.setTextColor(0xFF4CAF50);
            tvStatus.setText("Root access granted");
            tvStatus.setTextColor(0xFF4CAF50);
            return;
        }

        // Check Shizuku
        boolean shizukuAvail = ShizukuHelper.isShizukuAvailable();
        Log.d(TAG, "Shizuku available: " + shizukuAvail);

        if (!shizukuAvail) {
            tvAccessMode.setText("Access: NONE");
            tvAccessMode.setTextColor(0xFFF44336);
            tvStatus.setText("Shizuku not installed");
            tvStatus.setTextColor(0xFFF44336);
            showShizukuInstallDialog();
            return;
        }

        boolean binderAlive = ShizukuHelper.isBinderAlive();
        Log.d(TAG, "Shizuku binder alive: " + binderAlive);

        if (!binderAlive) {
            tvAccessMode.setText("Access: SHIZUKU (stopped)");
            tvAccessMode.setTextColor(0xFFFF9800);
            tvStatus.setText("Start Shizuku service first");
            tvStatus.setTextColor(0xFFFF9800);
            return;
        }

        boolean hasPerm = ShizukuHelper.hasPermission();
        Log.d(TAG, "Shizuku permission: " + hasPerm);

        if (hasPerm) {
            tvAccessMode.setText("Access: SHIZUKU");
            tvAccessMode.setTextColor(0xFF4CAF50);
            tvStatus.setText("Shizuku connected");
            tvStatus.setTextColor(0xFF4CAF50);
        } else {
            tvAccessMode.setText("Access: SHIZUKU (no perm)");
            tvAccessMode.setTextColor(0xFFFF9800);
            tvStatus.setText("Requesting permission...");
            tvStatus.setTextColor(0xFFFF9800);
            ShizukuHelper.requestPermission(REQ_PERMISSION);
        }

        Log.d(TAG, "=== checkAccess finished ===");
    }

    private void showShizukuInstallDialog() {
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
                    tvAccessMode.setText("Access: FORCED");
                    tvAccessMode.setTextColor(0xFFFF9800);
                    tvStatus.setText("Forced mode - may not work");
                    tvStatus.setTextColor(0xFFFF9800);
                })
                .show();
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
        progressBar.setVisibility(View.VISIBLE);
        btnCopyPaks.setEnabled(false);
        tvStatus.setText("Copying paks...");

        new Thread(() -> {
            PakManager.copyPaks(this, gamePkg, new PakManager.CopyCallback() {
                @Override
                public void onProgress(String fileName, int current, int total) {
                    handler.post(() -> {
                        tvStatus.setText("Copying: " + fileName + " (" + current + "/" + total + ")");
                    });
                }

                @Override
                public void onComplete(boolean success, String message) {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCopyPaks.setEnabled(true);

                        if (success) {
                            config.setPaksCopied(true);
                            tvStatus.setText("Paks copied!");
                            tvStatus.setTextColor(0xFF4CAF50);
                            Toast.makeText(MainActivity.this, "Success! Now launch game.", Toast.LENGTH_LONG).show();
                            config.writeLuaConfig();
                        } else {
                            tvStatus.setText("Copy failed: " + message);
                            tvStatus.setTextColor(0xFFF44336);
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }).start();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_OVERLAY);
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

            // Minimize main activity instead of finish
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
        checkAccess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}
