package com.fish.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class ClickAccessibilityService extends AccessibilityService {
    private static ClickAccessibilityService current;
    private final Handler handler = new Handler(Looper.getMainLooper());

    static ClickAccessibilityService instance() {
        return current;
    }

    @Override
    protected void onServiceConnected() {
        current = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (current == this) {
            current = null;
        }
        super.onDestroy();
    }

    void tap(float x, float y, Runnable success, Runnable failure) {
        Path path = new Path();
        path.moveTo(Math.max(0f, x), Math.max(0f, y));
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0L, 45L);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                handler.post(success);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                handler.post(failure);
            }
        }, handler);
        if (!accepted) {
            handler.post(failure);
        }
    }
}
