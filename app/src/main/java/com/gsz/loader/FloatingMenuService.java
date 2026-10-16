package com.gsz.loader;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingMenuService extends Service {

    private static final String TAG = "FloatingMenuService";
    private static final int NOTIF_ID = 1;
    private static final String CHANNEL_ID = "gsz_menu";

    private WindowManager windowManager;
    private View overlayView;
    private View menuPanel;
    private ImageView btnToggle;
    private LinearLayout panelContent;
    private View espRow, aimbotRow;
    private View espIndicator, aimbotIndicator;

    private boolean menuExpanded = false;
    private ConfigManager config;
    private Handler handler;

    private float initialX, initialY;
    private float touchX, touchY;
    private boolean isDragging = false;

    private static boolean isRunning = false;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

        try {
            isRunning = true;
            config = new ConfigManager(this);
            handler = new Handler(Looper.getMainLooper());
            startForegroundWithNotification();
            createOverlay();
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed", e);
            stopSelf();
        }
    }

    private void startForegroundWithNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "GSZ Menu", NotificationManager.IMPORTANCE_MIN);
                nm.createNotificationChannel(channel);
            }

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            } else {
                builder = new NotificationCompat.Builder(this);
            }

            Notification notif = builder
                    .setContentTitle("GSZ Loader")
                    .setContentText("Floating menu active - tap to open")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, notif);
            }

            Log.d(TAG, "Foreground notification started");
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed", e);
        }
    }

    private void createOverlay() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            int type;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 50;
            params.y = 200;

            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_menu, null);
            if (overlayView == null) {
                Log.e(TAG, "Failed to inflate overlay_menu");
                stopSelf();
                return;
            }

            menuPanel = overlayView.findViewById(R.id.menuPanel);
            btnToggle = overlayView.findViewById(R.id.btnToggle);
            panelContent = overlayView.findViewById(R.id.panelContent);
            espRow = overlayView.findViewById(R.id.espRow);
            aimbotRow = overlayView.findViewById(R.id.aimbotRow);
            espIndicator = overlayView.findViewById(R.id.espIndicator);
            aimbotIndicator = overlayView.findViewById(R.id.aimbotIndicator);

            if (btnToggle == null || panelContent == null) {
                Log.e(TAG, "Required views not found in layout");
                stopSelf();
                return;
            }

            updateToggleUI(espIndicator, config.isEspEnabled());
            updateToggleUI(aimbotIndicator, config.isAimbotEnabled());

            btnToggle.setOnClickListener(v -> {
                if (menuExpanded) {
                    collapseMenu();
                } else {
                    expandMenu();
                }
            });

            if (espRow != null) {
                espRow.setOnClickListener(v -> {
                    boolean newState = !config.isEspEnabled();
                    config.setEspEnabled(newState);
                    animateToggle(espIndicator, newState);
                    showToast("ESP: " + (newState ? "ON" : "OFF"));
                });
            }

            if (aimbotRow != null) {
                aimbotRow.setOnClickListener(v -> {
                    boolean newState = !config.isAimbotEnabled();
                    config.setAimbotEnabled(newState);
                    animateToggle(aimbotIndicator, newState);
                    showToast("Aimbot: " + (newState ? "ON" : "OFF"));
                });
            }

            overlayView.setOnTouchListener((v, event) -> {
                try {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            touchX = event.getRawX();
                            touchY = event.getRawY();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - touchX;
                            float dy = event.getRawY() - touchY;
                            if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                                isDragging = true;
                            }
                            params.x = (int) (initialX + dx);
                            params.y = (int) (initialY + dy);
                            if (windowManager != null && overlayView != null) {
                                windowManager.updateViewLayout(overlayView, params);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (!isDragging) {
                                v.performClick();
                            }
                            return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Touch handling failed", e);
                }
                return false;
            });

            windowManager.addView(overlayView, params);
            Log.d(TAG, "Overlay view added successfully");

        } catch (Exception e) {
            Log.e(TAG, "createOverlay failed", e);
            stopSelf();
        }
    }

    private void expandMenu() {
        if (panelContent == null) return;

        menuExpanded = true;

        panelContent.setVisibility(View.VISIBLE);
        panelContent.setAlpha(0f);
        panelContent.setScaleX(0.6f);
        panelContent.setScaleY(0.6f);
        panelContent.setTranslationY(-30f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(panelContent, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(panelContent, "scaleX", 0.6f, 1f),
                ObjectAnimator.ofFloat(panelContent, "scaleY", 0.6f, 1f),
                ObjectAnimator.ofFloat(panelContent, "translationY", -30f, 0f)
        );
        set.setDuration(350);
        set.setInterpolator(new OvershootInterpolator(1.3f));
        set.start();

        if (btnToggle != null) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 0f, 45f);
            rotate.setDuration(280);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            rotate.start();
        }
    }

    private void collapseMenu() {
        if (panelContent == null) return;

        menuExpanded = false;

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(panelContent, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(panelContent, "scaleX", 1f, 0.6f),
                ObjectAnimator.ofFloat(panelContent, "scaleY", 1f, 0.6f),
                ObjectAnimator.ofFloat(panelContent, "translationY", 0f, -30f)
        );
        set.setDuration(250);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (panelContent != null) {
                    panelContent.setVisibility(View.GONE);
                }
            }
        });
        set.start();

        if (btnToggle != null) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 45f, 0f);
            rotate.setDuration(280);
            rotate.start();
        }
    }

    private void animateToggle(View indicator, boolean enabled) {
        if (indicator == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(indicator, "scaleX", 1f, 1.4f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(indicator, "scaleY", 1f, 1.4f, 1f);
        AnimatorSet bounce = new AnimatorSet();
        bounce.playTogether(scaleX, scaleY);
        bounce.setDuration(350);
        bounce.setInterpolator(new BounceInterpolator());
        bounce.start();

        updateToggleUI(indicator, enabled);
    }

    private void updateToggleUI(View indicator, boolean enabled) {
        if (indicator == null) return;

        if (enabled) {
            indicator.setBackgroundResource(R.drawable.bg_toggle_on);
        } else {
            indicator.setBackgroundResource(R.drawable.bg_toggle_off);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        isRunning = false;

        try {
            if (overlayView != null && windowManager != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing overlay", e);
        }

        windowManager = null;
    }
}
