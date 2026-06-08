package com.fish.autoclicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.Locale;
import java.util.Random;

final class ClickConfig {
    static final String PREFS = "click_config";

    static final String REGION_RECT = "rect";
    static final String REGION_CIRCLE = "circle";

    int clickCount;
    boolean infinite;
    int intervalMs;
    boolean randomPoint;
    boolean randomInterval;
    int intervalJitterPercent;
    String regionMode;
    float left;
    float top;
    float right;
    float bottom;
    float centerX;
    float centerY;
    float radius;

    static ClickConfig load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ClickConfig config = new ClickConfig();
        config.clickCount = prefs.getInt("clickCount", 100);
        config.infinite = prefs.getBoolean("infinite", false);
        config.intervalMs = prefs.getInt("intervalMs", 200);
        config.randomPoint = prefs.getBoolean("randomPoint", true);
        config.randomInterval = prefs.getBoolean("randomInterval", false);
        config.intervalJitterPercent = prefs.getInt("intervalJitterPercent", 30);
        config.regionMode = prefs.getString("regionMode", REGION_RECT);
        config.left = prefs.getFloat("left", 300f);
        config.top = prefs.getFloat("top", 600f);
        config.right = prefs.getFloat("right", 700f);
        config.bottom = prefs.getFloat("bottom", 1000f);
        config.centerX = prefs.getFloat("centerX", 540f);
        config.centerY = prefs.getFloat("centerY", 900f);
        config.radius = prefs.getFloat("radius", 120f);
        config.normalize();
        return config;
    }

    void save(Context context) {
        normalize();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt("clickCount", clickCount)
                .putBoolean("infinite", infinite)
                .putInt("intervalMs", intervalMs)
                .putBoolean("randomPoint", randomPoint)
                .putBoolean("randomInterval", randomInterval)
                .putInt("intervalJitterPercent", intervalJitterPercent)
                .putString("regionMode", regionMode)
                .putFloat("left", left)
                .putFloat("top", top)
                .putFloat("right", right)
                .putFloat("bottom", bottom)
                .putFloat("centerX", centerX)
                .putFloat("centerY", centerY)
                .putFloat("radius", radius)
                .apply();
    }

    PointF nextPoint(Random random) {
        normalize();
        if (REGION_CIRCLE.equals(regionMode)) {
            if (!randomPoint) {
                return new PointF(centerX, centerY);
            }
            double angle = random.nextDouble() * Math.PI * 2d;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            return new PointF(
                    centerX + (float) (Math.cos(angle) * distance),
                    centerY + (float) (Math.sin(angle) * distance)
            );
        }

        if (!randomPoint) {
            return new PointF((left + right) / 2f, (top + bottom) / 2f);
        }
        return new PointF(
                left + random.nextFloat() * Math.max(1f, right - left),
                top + random.nextFloat() * Math.max(1f, bottom - top)
        );
    }

    long nextDelayMs(Random random) {
        int base = Math.max(10, intervalMs);
        if (!randomInterval) {
            return base;
        }
        int jitter = Math.max(0, Math.min(90, intervalJitterPercent));
        int spread = Math.max(1, Math.round(base * jitter / 100f));
        int min = Math.max(10, base - spread);
        int max = Math.max(min, base + spread);
        return min + random.nextInt(max - min + 1);
    }

    int maxClicks() {
        return infinite ? Integer.MAX_VALUE : Math.max(1, clickCount);
    }

    String describeRegion() {
        normalize();
        if (REGION_CIRCLE.equals(regionMode)) {
            return String.format(Locale.CHINA, "圆形：中心 %.0f, %.0f，半径 %.0f", centerX, centerY, radius);
        }
        return String.format(Locale.CHINA, "矩形：%.0f, %.0f - %.0f, %.0f", left, top, right, bottom);
    }

    private void normalize() {
        intervalMs = Math.max(10, intervalMs);
        clickCount = Math.max(1, clickCount);
        intervalJitterPercent = Math.max(0, Math.min(90, intervalJitterPercent));
        if (!REGION_CIRCLE.equals(regionMode)) {
            regionMode = REGION_RECT;
        }
        float minX = Math.min(left, right);
        float maxX = Math.max(left, right);
        float minY = Math.min(top, bottom);
        float maxY = Math.max(top, bottom);
        left = minX;
        right = Math.max(minX + 1f, maxX);
        top = minY;
        bottom = Math.max(minY + 1f, maxY);
        radius = Math.max(1f, radius);
    }

    RectF rect() {
        normalize();
        return new RectF(left, top, right, bottom);
    }
}
