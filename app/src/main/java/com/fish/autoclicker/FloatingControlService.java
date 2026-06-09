package com.fish.autoclicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingControlService extends Service {
    static final String EXTRA_SELECT_REGION = "selectRegion";

    private static final int NOTIFICATION_ID = 10;
    private static final String CHANNEL_ID = "auto_clicker_control";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams bubbleParams;
    private View panelView;
    private View bubbleView;
    private RegionOverlayView regionView;
    private TextView statusText;
    private Button pauseButton;
    private ClickConfig config;
    private UiTheme theme;
    private boolean collapsed;
    private int panelX;
    private int panelY;

    private final Runnable collapseRunnable = new Runnable() {
        @Override
        public void run() {
            collapseToBubble();
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatus(intent.getStringExtra(ClickController.EXTRA_MESSAGE));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        theme = UiTheme.from(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        config = ClickConfig.load(this);
        panelX = dp(20);
        panelY = dp(120);
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        registerReceiverCompat();
        showPanel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        config = ClickConfig.load(this);
        if (panelView == null && bubbleView == null) {
            showPanel();
        } else {
            applyOverlayAlpha();
        }
        if (intent != null && intent.getBooleanExtra(EXTRA_SELECT_REGION, false)) {
            expandPanel();
            showRegionOverlay();
        }
        updateStatus("悬浮控制已打开");
        scheduleCollapse();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(collapseRunnable);
        removeRegionOverlay();
        removePanel();
        removeBubble();
        unregisterReceiver(stateReceiver);
        super.onDestroy();
    }

    private void showPanel() {
        if (!Settings.canDrawOverlays(this) || panelView != null) {
            return;
        }
        removeBubble();
        collapsed = false;
        config = ClickConfig.load(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackground(theme.stroked(theme.surface, theme.outline, 22, this));
        root.setElevation(dp(10));

        statusText = new TextView(this);
        statusText.setTextColor(theme.text);
        statusText.setTextSize(13);
        statusText.setMaxWidth(dp(238));
        statusText.setLineSpacing(dp(2), 1f);
        root.addView(statusText);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, dp(9), 0, 0);
        Button start = primaryButton("开始");
        pauseButton = tonalButton("暂停");
        Button stop = dangerButton("停止");
        row1.addView(start, buttonParams());
        row1.addView(pauseButton, buttonParams());
        row1.addView(stop, buttonParams());
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(7), 0, 0);
        Button select = tonalButton(config.randomPoint ? "选区" : "定点");
        Button settings = tonalButton("设置");
        Button close = quietButton("关闭");
        row2.addView(select, buttonParams());
        row2.addView(settings, buttonParams());
        row2.addView(close, buttonParams());
        root.addView(row2);

        panelParams = overlayParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = panelX;
        panelParams.y = panelY;
        panelParams.alpha = overlayAlpha();

        root.setOnTouchListener(new DragTouchListener(panelParams, true));
        windowManager.addView(root, panelParams);
        panelView = root;

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInteraction();
                config = ClickConfig.load(FloatingControlService.this);
                if (!PermissionUtils.isAccessibilityEnabled(FloatingControlService.this)) {
                    Toast.makeText(FloatingControlService.this, "请先开启辅助功能服务", Toast.LENGTH_LONG).show();
                    openAccessibilitySettings();
                    return;
                }
                ClickController.get().start(FloatingControlService.this, config);
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInteraction();
                ClickController.get().pauseOrResume();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInteraction();
                ClickController.get().stop();
            }
        });
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInteraction();
                showRegionOverlay();
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInteraction();
                Intent intent = new Intent(FloatingControlService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });

        updateStatus("悬浮控制已打开");
        scheduleCollapse();
    }

    private void showBubble() {
        if (!Settings.canDrawOverlays(this) || bubbleView != null) {
            return;
        }
        TextView bubble = new TextView(this);
        bubble.setText("点");
        bubble.setTextSize(17);
        bubble.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setTextColor(theme.onAccent());
        bubble.setIncludeFontPadding(false);
        GradientDrawable bubbleShape = new GradientDrawable();
        bubbleShape.setShape(GradientDrawable.OVAL);
        bubbleShape.setColor(theme.accent);
        bubble.setBackground(theme.ripple(bubbleShape, theme.accentStrong));
        bubble.setElevation(dp(10));

        bubbleParams = overlayParams(dp(56), dp(56));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = Math.max(dp(8), panelX);
        bubbleParams.y = Math.max(dp(80), panelY);
        bubbleParams.alpha = Math.min(0.9f, overlayAlpha() + 0.08f);
        bubble.setOnTouchListener(new DragTouchListener(bubbleParams, false));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandPanel();
            }
        });
        windowManager.addView(bubble, bubbleParams);
        bubbleView = bubble;
    }

    private void collapseToBubble() {
        if (collapsed || regionView != null || panelView == null) {
            return;
        }
        if (panelParams != null) {
            panelX = panelParams.x;
            panelY = panelParams.y;
        }
        removePanel();
        collapsed = true;
        showBubble();
    }

    private void expandPanel() {
        removeBubble();
        collapsed = false;
        showPanel();
    }

    private void showRegionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            return;
        }
        removeRegionOverlay();
        config = ClickConfig.load(this);
        regionView = new RegionOverlayView(this, config);
        WindowManager.LayoutParams params = overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(regionView, params);
        updateStatus(config.randomPoint ? "拖动选择范围" : "点击选择固定点");
        handler.removeCallbacks(collapseRunnable);
    }

    private void removeRegionOverlay() {
        if (regionView != null) {
            try {
                windowManager.removeView(regionView);
            } catch (IllegalArgumentException ignored) {
            }
            regionView = null;
            scheduleCollapse();
        }
    }

    private void removePanel() {
        if (panelView != null) {
            try {
                windowManager.removeView(panelView);
            } catch (IllegalArgumentException ignored) {
            }
            panelView = null;
            statusText = null;
            pauseButton = null;
        }
    }

    private void removeBubble() {
        if (bubbleView != null) {
            try {
                windowManager.removeView(bubbleView);
            } catch (IllegalArgumentException ignored) {
            }
            bubbleView = null;
        }
    }

    private void markInteraction() {
        config = ClickConfig.load(this);
        applyOverlayAlpha();
        scheduleCollapse();
    }

    private void scheduleCollapse() {
        handler.removeCallbacks(collapseRunnable);
        if (!collapsed && panelView != null && regionView == null) {
            handler.postDelayed(collapseRunnable, Math.max(3, config.collapseDelaySeconds) * 1000L);
        }
    }

    private void applyOverlayAlpha() {
        float alpha = overlayAlpha();
        if (panelParams != null && panelView != null) {
            panelParams.alpha = alpha;
            windowManager.updateViewLayout(panelView, panelParams);
        }
        if (bubbleParams != null && bubbleView != null) {
            bubbleParams.alpha = Math.min(0.9f, alpha + 0.08f);
            windowManager.updateViewLayout(bubbleView, bubbleParams);
        }
    }

    private float overlayAlpha() {
        return Math.max(0.3f, Math.min(1f, config.overlayOpacityPercent / 100f));
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.onAccent());
        button.setBackground(theme.ripple(theme.rounded(theme.accent, 16, this), theme.accentStrong));
        return button;
    }

    private Button tonalButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.accentStrong);
        button.setBackground(theme.ripple(theme.rounded(theme.accentSoft, 16, this), theme.accent));
        return button;
    }

    private Button dangerButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(theme.ripple(theme.rounded(theme.danger, 16, this), theme.danger));
        return button;
    }

    private Button quietButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.text);
        button.setBackground(theme.ripple(theme.stroked(theme.surfaceHigh, theme.outline, 16, this), theme.accent));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(66), dp(40));
        params.setMargins(0, 0, dp(6), 0);
        return params;
    }

    private WindowManager.LayoutParams overlayParams(int width, int height) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        return new WindowManager.LayoutParams(
                width,
                height,
                type,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
        );
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "连点器悬浮控制",
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE
                        : 0
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("连点器")
                .setContentText("悬浮控制正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void updateStatus(String message) {
        if (statusText == null) {
            return;
        }
        ClickController controller = ClickController.get();
        String state;
        if (controller.isRunning()) {
            String total = controller.total() == Integer.MAX_VALUE ? "无限" : String.valueOf(controller.total());
            state = "进度 " + controller.completed() + "/" + total + (controller.isPaused() ? " 已暂停" : " 点击中");
        } else {
            state = message == null ? "待开始" : message;
        }
        statusText.setText(state + "\n" + ClickConfig.load(this).describeRegion());
        if (pauseButton != null) {
            pauseButton.setText(controller.isPaused() ? "继续" : "暂停");
        }
    }

    private void registerReceiverCompat() {
        IntentFilter filter = new IntentFilter(ClickController.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
    }

    private int dp(int value) {
        return UiTheme.dp(this, value);
    }

    private final class DragTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams params;
        private final boolean panel;
        private int startX;
        private int startY;
        private float touchX;
        private float touchY;
        private boolean moved;

        DragTouchListener(WindowManager.LayoutParams params, boolean panel) {
            this.params = params;
            this.panel = panel;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    markInteraction();
                    startX = params.x;
                    startY = params.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(event.getRawX() - touchX);
                    int dy = Math.round(event.getRawY() - touchY);
                    if (Math.abs(dx) + Math.abs(dy) > dp(4)) {
                        moved = true;
                    }
                    params.x = startX + dx;
                    params.y = startY + dy;
                    if (panel) {
                        panelX = params.x;
                        panelY = params.y;
                    } else {
                        panelX = params.x;
                        panelY = params.y;
                    }
                    windowManager.updateViewLayout(v, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        v.performClick();
                    }
                    scheduleCollapse();
                    return true;
                default:
                    return false;
            }
        }
    }

    private final class RegionOverlayView extends View {
        private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int[] screenLocation = new int[2];
        private final ClickConfig draft;
        private final boolean selectingRandomRange;
        private boolean circleMode;
        private float downX;
        private float downY;
        private boolean draggingExisting;
        private ButtonMode buttonMode = ButtonMode.NONE;

        RegionOverlayView(Context context, ClickConfig config) {
            super(context);
            this.draft = config;
            this.selectingRandomRange = config.randomPoint;
            this.circleMode = ClickConfig.REGION_CIRCLE.equals(config.regionMode);
            dimPaint.setColor(Color.argb(128, 15, 23, 42));
            shapePaint.setStyle(Paint.Style.STROKE);
            shapePaint.setStrokeWidth(dp(3));
            shapePaint.setColor(theme.accentSoft);
            handlePaint.setColor(theme.accent);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(dp(15));
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            setBackgroundColor(Color.TRANSPARENT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            captureScreenLocation();
            canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

            if (selectingRandomRange) {
                if (circleMode) {
                    float localCenterX = screenToLocalX(draft.centerX);
                    float localCenterY = screenToLocalY(draft.centerY);
                    canvas.drawCircle(localCenterX, localCenterY, draft.radius, shapePaint);
                    canvas.drawCircle(localCenterX, localCenterY, dp(6), handlePaint);
                    canvas.drawCircle(localCenterX + draft.radius, localCenterY, dp(7), handlePaint);
                } else {
                    rect.set(draft.rect());
                    rect.offset(-screenLocation[0], -screenLocation[1]);
                    canvas.drawRect(rect, shapePaint);
                    canvas.drawCircle(rect.left, rect.top, dp(7), handlePaint);
                    canvas.drawCircle(rect.right, rect.bottom, dp(7), handlePaint);
                }
            } else {
                float localFixedX = screenToLocalX(draft.fixedX);
                float localFixedY = screenToLocalY(draft.fixedY);
                canvas.drawCircle(localFixedX, localFixedY, dp(20), shapePaint);
                canvas.drawCircle(localFixedX, localFixedY, dp(7), handlePaint);
            }

            drawButton(canvas, 0, "保存");
            drawButton(canvas, 1, selectingRandomRange ? (circleMode ? "矩形" : "圆形") : "定点");
            drawButton(canvas, 2, "取消");
            textPaint.setColor(Color.WHITE);
            canvas.drawText(selectingRandomRange
                    ? "拖动屏幕选择范围，底部按钮可保存或切换形状"
                    : "点击或拖动选择固定点击点", dp(16), dp(34), textPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            captureScreenLocation();
            float localX = event.getX();
            float localY = event.getY();
            float screenX = localToScreenX(localX);
            float screenY = localToScreenY(localY);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = screenX;
                    downY = screenY;
                    buttonMode = hitButton(localX, localY);
                    if (buttonMode != ButtonMode.NONE) {
                        return true;
                    }
                    if (!selectingRandomRange) {
                        draft.fixedX = clamp(screenX, screenLeft(), screenRight());
                        draft.fixedY = clamp(screenY, screenTop(), screenBottom());
                        invalidate();
                        return true;
                    }
                    draggingExisting = hitExistingShape(screenX, screenY);
                    if (!draggingExisting) {
                        if (circleMode) {
                            draft.centerX = screenX;
                            draft.centerY = screenY;
                            draft.radius = dp(20);
                        } else {
                            draft.left = screenX;
                            draft.top = screenY;
                            draft.right = screenX + 1;
                            draft.bottom = screenY + 1;
                        }
                    }
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (buttonMode != ButtonMode.NONE) {
                        return true;
                    }
                    if (!selectingRandomRange) {
                        draft.fixedX = clamp(screenX, screenLeft(), screenRight());
                        draft.fixedY = clamp(screenY, screenTop(), screenBottom());
                        invalidate();
                        return true;
                    }
                    if (circleMode) {
                        if (draggingExisting) {
                            draft.centerX += screenX - downX;
                            draft.centerY += screenY - downY;
                            downX = screenX;
                            downY = screenY;
                        } else {
                            draft.radius = Math.max(dp(8), distance(draft.centerX, draft.centerY, screenX, screenY));
                        }
                    } else if (draggingExisting) {
                        float dx = screenX - downX;
                        float dy = screenY - downY;
                        draft.left += dx;
                        draft.right += dx;
                        draft.top += dy;
                        draft.bottom += dy;
                        downX = screenX;
                        downY = screenY;
                    } else {
                        draft.right = screenX;
                        draft.bottom = screenY;
                    }
                    clampToScreen();
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    ButtonMode upMode = hitButton(localX, localY);
                    if (buttonMode != ButtonMode.NONE && buttonMode == upMode) {
                        handleButton(upMode);
                    }
                    buttonMode = ButtonMode.NONE;
                    draggingExisting = false;
                    return true;
                default:
                    return true;
            }
        }

        private boolean hitExistingShape(float x, float y) {
            if (circleMode) {
                return distance(draft.centerX, draft.centerY, x, y) <= draft.radius;
            }
            RectF current = draft.rect();
            return current.contains(x, y);
        }

        private void clampToScreen() {
            float left = screenLeft();
            float top = screenTop();
            float right = screenRight();
            float bottom = screenBottom();
            float width = Math.max(1f, right - left);
            float height = Math.max(1f, bottom - top);
            if (circleMode) {
                float maxRadius = Math.max(1f, Math.min(width, height) / 2f);
                draft.radius = Math.max(1f, Math.min(draft.radius, maxRadius));
                draft.centerX = clamp(draft.centerX, left + draft.radius, right - draft.radius);
                draft.centerY = clamp(draft.centerY, top + draft.radius, bottom - draft.radius);
            } else {
                draft.left = clamp(draft.left, left, right);
                draft.right = clamp(draft.right, left, right);
                draft.top = clamp(draft.top, top, bottom);
                draft.bottom = clamp(draft.bottom, top, bottom);
            }
        }

        private void handleButton(ButtonMode mode) {
            if (mode == ButtonMode.SAVE) {
                if (selectingRandomRange) {
                    draft.regionMode = circleMode ? ClickConfig.REGION_CIRCLE : ClickConfig.REGION_RECT;
                }
                draft.save(FloatingControlService.this);
                updateStatus(selectingRandomRange ? "已保存范围" : "已保存固定点");
                removeRegionOverlay();
            } else if (mode == ButtonMode.TOGGLE && selectingRandomRange) {
                circleMode = !circleMode;
                if (circleMode) {
                    RectF r = draft.rect();
                    draft.centerX = r.centerX();
                    draft.centerY = r.centerY();
                    draft.radius = Math.max(dp(20), Math.min(r.width(), r.height()) / 2f);
                } else {
                    draft.left = draft.centerX - draft.radius;
                    draft.right = draft.centerX + draft.radius;
                    draft.top = draft.centerY - draft.radius;
                    draft.bottom = draft.centerY + draft.radius;
                }
                clampToScreen();
                invalidate();
            } else if (mode == ButtonMode.CANCEL) {
                removeRegionOverlay();
            }
        }

        private void drawButton(Canvas canvas, int index, String text) {
            float width = dp(86);
            float height = dp(42);
            float gap = dp(10);
            float total = width * 3 + gap * 2;
            float left = (getWidth() - total) / 2f + index * (width + gap);
            float top = getHeight() - dp(70);
            Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boolean quiet = index == 2 || (!selectingRandomRange && index == 1);
            buttonPaint.setColor(quiet ? UiTheme.withAlpha(Color.WHITE, 238) : theme.accent);
            RectF button = new RectF(left, top, left + width, top + height);
            canvas.drawRoundRect(button, dp(18), dp(18), buttonPaint);
            textPaint.setColor(quiet ? theme.text : theme.onAccent());
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float textX = left + (width - textPaint.measureText(text)) / 2f;
            float textY = top + (height - metrics.bottom + metrics.top) / 2f - metrics.top;
            canvas.drawText(text, textX, textY, textPaint);
        }

        private ButtonMode hitButton(float x, float y) {
            float width = dp(86);
            float height = dp(42);
            float gap = dp(10);
            float total = width * 3 + gap * 2;
            float top = getHeight() - dp(70);
            for (int i = 0; i < 3; i++) {
                float left = (getWidth() - total) / 2f + i * (width + gap);
                if (x >= left && x <= left + width && y >= top && y <= top + height) {
                    if (i == 0) {
                        return ButtonMode.SAVE;
                    }
                    if (i == 1) {
                        return ButtonMode.TOGGLE;
                    }
                    return ButtonMode.CANCEL;
                }
            }
            return ButtonMode.NONE;
        }

        private float distance(float ax, float ay, float bx, float by) {
            float dx = ax - bx;
            float dy = ay - by;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private void captureScreenLocation() {
            getLocationOnScreen(screenLocation);
        }

        private float localToScreenX(float localX) {
            return screenLocation[0] + localX;
        }

        private float localToScreenY(float localY) {
            return screenLocation[1] + localY;
        }

        private float screenToLocalX(float screenX) {
            return screenX - screenLocation[0];
        }

        private float screenToLocalY(float screenY) {
            return screenY - screenLocation[1];
        }

        private float screenLeft() {
            return screenLocation[0];
        }

        private float screenTop() {
            return screenLocation[1];
        }

        private float screenRight() {
            return screenLocation[0] + Math.max(1, getWidth());
        }

        private float screenBottom() {
            return screenLocation[1] + Math.max(1, getHeight());
        }
    }

    private enum ButtonMode {
        NONE,
        SAVE,
        TOGGLE,
        CANCEL
    }
}
