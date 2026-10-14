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
import android.os.IBinder;
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

import androidx.core.app.NotificationCompat;

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

    private boolean menuExpanded = false;
    private ConfigManager config;

    private float initialX, initialY;
    private float touchX, touchY;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        config = new ConfigManager(this);
        startForegroundWithNotification();
        createOverlay();
    }

    private void startForegroundWithNotification() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "GSZ Menu", NotificationManager.IMPORTANCE_MIN);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
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

        // Set initial toggle states
        updateToggleUI(espIndicator, config.isEspEnabled());
        updateToggleUI(aimbotIndicator, config.isAimbotEnabled());

        // Toggle button click = expand/collapse
        btnToggle.setOnClickListener(v -> {
            if (menuExpanded) {
                collapseMenu();
            } else {
                expandMenu();
            }
        });

        // ESP toggle
        espRow.setOnClickListener(v -> {
            boolean newState = !config.isEspEnabled();
            config.setEspEnabled(newState);
            animateToggle(espIndicator, newState);
        });

        // Aimbot toggle
        aimbotRow.setOnClickListener(v -> {
            boolean newState = !config.isAimbotEnabled();
            config.setAimbotEnabled(newState);
            animateToggle(aimbotIndicator, newState);
        });

        // Drag logic
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
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
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

        // Animate panel expand
        panelContent.setVisibility(View.VISIBLE);
        panelContent.setAlpha(0f);
        panelContent.setScaleX(0.7f);
        panelContent.setScaleY(0.7f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(panelContent, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(panelContent, "scaleX", 0.7f, 1f),
            ObjectAnimator.ofFloat(panelContent, "scaleY", 0.7f, 1f)
        );
        set.setDuration(300);
        set.setInterpolator(new OvershootInterpolator(1.2f));
        set.start();

        // Rotate toggle button
        ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 0f, 45f);
        rotate.setDuration(250);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.start();
    }

    private void collapseMenu() {
        menuExpanded = false;

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(panelContent, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(panelContent, "scaleX", 1f, 0.7f),
            ObjectAnimator.ofFloat(panelContent, "scaleY", 1f, 0.7f)
        );
        set.setDuration(200);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                panelContent.setVisibility(View.GONE);
            }
        });
        set.start();

        ObjectAnimator rotate = ObjectAnimator.ofFloat(btnToggle, "rotation", 45f, 0f);
        rotate.setDuration(250);
        rotate.start();
    }

    private void animateToggle(View indicator, boolean enabled) {
        // Bounce animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(indicator, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(indicator, "scaleY", 1f, 1.3f, 1f);
        AnimatorSet bounce = new AnimatorSet();
        bounce.playTogether(scaleX, scaleY);
        bounce.setDuration(300);
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
