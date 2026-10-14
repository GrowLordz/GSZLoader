package com.gsz.loader;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION = 1001;
    private static final int REQ_OVERLAY = 1002;

    private TextView tvStatus;
    private Button btnStartMenu, btnCopyPaks, btnLaunchGame, btnCheckShizuku;
    private boolean shizukuBound = false;
    private IShizukuShell shellService;

    private final ServiceConnection shizukuConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            shellService = IShizukuShell.Stub.asInterface(service);
            shizukuBound = true;
            runOnUiThread(() -> {
                tvStatus.setText("Shizuku: CONNECTED");
                tvStatus.setTextColor(0xFF4CAF50);
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            shizukuBound = false;
            shellService = null;
            runOnUiThread(() -> {
                tvStatus.setText("Shizuku: DISCONNECTED");
                tvStatus.setTextColor(0xFFF44336);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnStartMenu = findViewById(R.id.btnStartMenu);
        btnCopyPaks = findViewById(R.id.btnCopyPaks);
        btnLaunchGame = findViewById(R.id.btnLaunchGame);
        btnCheckShizuku = findViewById(R.id.btnCheckShizuku);

        checkShizukuPermission();

        btnCheckShizuku.setOnClickListener(v -> {
            if (ShizukuHelper.isBinderAlive()) {
                bindShizuku();
            } else {
                tvStatus.setText("Shizuku: NOT RUNNING");
                tvStatus.setTextColor(0xFFFF9800);
                Toast.makeText(this, "Start Shizuku first!", Toast.LENGTH_LONG).show();
            }
        });

        btnCopyPaks.setOnClickListener(v -> copyPaksViaShizuku());

        btnLaunchGame.setOnClickListener(v -> {
            String pkg = GameLauncher.detectInstalledGame(this);
            if (pkg != null) {
                GameLauncher.launchGame(this, pkg);
            } else {
                Toast.makeText(this, "No PUBG game installed!", Toast.LENGTH_LONG).show();
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

    private void checkShizukuPermission() {
        if (ShizukuHelper.isBinderAlive()) {
            if (ShizukuHelper.hasPermission() == PackageManager.PERMISSION_GRANTED) {
                bindShizuku();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                tvStatus.setText("Shizuku: NEED PERMISSION");
            } else {
                ShizukuHelper.requestPermission(REQ_PERMISSION);
            }
        } else {
            tvStatus.setText("Shizuku: NOT RUNNING");
        }
    }

    private void bindShizuku() {
        Intent intent = new Intent(this, ShizukuShellService.class);
        intent.setAction("moe.shizuku.intent.action.USER_SERVICE");
        Shizuku.bindUserService(intent, shizukuConn);
    }

    private void copyPaksViaShizuku() {
        if (!shizukuBound || shellService == null) {
            Toast.makeText(this, "Shizuku not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                String pkg = GameLauncher.detectInstalledGame(MainActivity.this);
                if (pkg == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Game not found!", Toast.LENGTH_SHORT).show());
                    return;
                }
                PakManager.copyPaks(MainActivity.this, shellService, pkg);
                runOnUiThread(() -> Toast.makeText(this, "Paks copied!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_OVERLAY);
    }

    private void startFloatingMenu() {
        Intent intent = new Intent(this, FloatingMenuService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Floating menu started!", Toast.LENGTH_SHORT).show();
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
            bindShizuku();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shizukuBound) {
            try {
                Shizuku.unbindUserService(shizukuConn);
            } catch (Exception ignored) {}
        }
    }
}
