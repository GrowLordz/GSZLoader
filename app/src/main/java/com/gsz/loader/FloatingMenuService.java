package com.gsz.loader;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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

    private static final int NOTIF_ID = 1;
    private static final String CHANNEL_ID = "gsz_menu";

    private WindowManager windowManager;
    private View overlayView;
    private View menuPanel;
    private ImageView btnToggle;
    private LinearLayout panelContent;
    private View espRow, aimbotRow;
    private View espIndicator, aimbotIndicator;
    private TextView espLabel, aimbotLabel;

    private boolean menuExpanded = false;
    private ConfigManager config;
    private Handler handler;

    private float initialX, initialY;
    private float touchX, touchY;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        config = new ConfigManager(this);
        handler = new Handler(Looper.getMainLooper());
        startForegroundWithNotification();
        createOverlay();
    }

    private void startForegroundWithNotification() {
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
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notif = builder
                .setContentTitle("GSZ Loader")
                .setContentText("Floating menu active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, notif);
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

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
        menuPanel = overlayView.findViewById(R.id.menuPanel);
        btnToggle = overlayView.findViewById(R.id.btnToggle);
        panelContent = overlayView.findViewById(R.id.panelContent);
        espRow = overlayView.findViewById(R.id.espRow);
        aimbotRow = overlayView.findViewById(R.id.aimbotRow);
        espIndicator = overlayView.findViewById(R.id.espIndicator);
        aimbotIndicator = overlayView.findViewById(R.id.aimbotIndicator);
        espLabel = overlayView.findViewById(R.id.espLabel);
        aimbotLabel = overlayView.findViewById(R.id.aimbotLabel);

        updateToggleUI(espIndicator, config.isEspEnabled());
        updateToggleUI(aimbotIndicator, config.isAimbotEnabled());

        btnToggle.setOnClickListener(v -> {
            if (menuExpanded) {
                collapseMenu();
            } else {
                expandMenu();
            }
        });

        espRow.setOnClickListener(v -> {
            boolean newState = !config.isEspEnabled();
            config.setEspEnabled(newState);
            animateToggle(espIndicator, newState);
            showToast("ESP: " + (newState ? "ON" : "OFF"));
        });

        aimbotRow.setOnClickListener(v -> {
            boolean newState = !config.isAimbotEnabled();
            config.setAimbotEnabled(newState);
            animateToggle(aimbotIndicator, newState);
            showToast("Aimbot: " + (newState ? "ON" : "OFF"));
        });

        overlayView.setOnTouchListener((v, event) -> {
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
                    windowManager.updateViewLayout(overlayView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        v.performClick();
                    }
                    return true;
            }
            return false;
        });

        windowManager.addView(overlayView, params);
    }

    private void expandMenu() {
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

        ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 0f, 45f);
        rotate.setDuration(280);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.start();

        ObjectAnimator scaleBtn = ObjectAnimator.ofFloat(btnToggle, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleBtnY = ObjectAnimator.ofFloat(btnToggle, "scaleY", 1f, 1.15f, 1f);
        AnimatorSet btnAnim = new AnimatorSet();
        btnAnim.playTogether(scaleBtn, scaleBtnY);
        btnAnim.setDuration(280);
        btnAnim.start();
    }

    private void collapseMenu() {
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
                panelContent.setVisibility(View.GONE);
            }
        });
        set.start();

        ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 45f, 0f);
        rotate.setDuration(280);
        rotate.start();
    }

    private void animateToggle(View indicator, boolean enabled) {
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
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
        }
    }
}
