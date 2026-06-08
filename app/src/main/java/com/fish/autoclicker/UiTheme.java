package com.fish.autoclicker;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

final class UiTheme {
    final int accent;
    final int accentStrong;
    final int accentSoft;
    final int accentContainer;
    final int background;
    final int surface;
    final int surfaceHigh;
    final int field;
    final int text;
    final int subtext;
    final int outline;
    final int success;
    final int danger;

    private UiTheme(int accent) {
        this.accent = accent;
        this.accentStrong = shiftValue(accent, 0.72f);
        this.accentSoft = mix(Color.WHITE, accent, 0.13f);
        this.accentContainer = mix(Color.WHITE, accent, 0.22f);
        this.background = mix(Color.rgb(248, 250, 252), accent, 0.05f);
        this.surface = mix(Color.WHITE, accent, 0.035f);
        this.surfaceHigh = mix(Color.WHITE, accent, 0.08f);
        this.field = mix(Color.WHITE, accent, 0.06f);
        this.text = Color.rgb(15, 23, 42);
        this.subtext = Color.rgb(86, 99, 118);
        this.outline = mix(Color.rgb(148, 163, 184), accent, 0.22f);
        this.success = Color.rgb(16, 128, 82);
        this.danger = Color.rgb(190, 52, 52);
    }

    static UiTheme from(Context context) {
        return new UiTheme(resolveAccent(context));
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    Drawable rounded(int color, float radiusDp, Context context) {
        return new SmoothDrawable(color, 0, 0, dp(context, Math.round(radiusDp)));
    }

    Drawable stroked(int color, int strokeColor, float radiusDp, Context context) {
        return new SmoothDrawable(color, strokeColor, dp(context, 1), dp(context, Math.round(radiusDp)));
    }

    Drawable ripple(Drawable content, int rippleColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(withAlpha(rippleColor, 40)), content, null);
        }
        return content;
    }

    void styleSystemBars(android.app.Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(background);
            activity.getWindow().setNavigationBarColor(background);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    void title(TextView view, float sizeSp) {
        view.setTextColor(text);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
    }

    void body(TextView view, float sizeSp) {
        view.setTextColor(subtext);
        view.setTextSize(sizeSp);
        view.setIncludeFontPadding(true);
    }

    int onAccent() {
        return isLight(accent) ? Color.rgb(15, 23, 42) : Color.WHITE;
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    static int mix(int base, int overlay, float amount) {
        float keep = 1f - amount;
        return Color.rgb(
                Math.round(Color.red(base) * keep + Color.red(overlay) * amount),
                Math.round(Color.green(base) * keep + Color.green(overlay) * amount),
                Math.round(Color.blue(base) * keep + Color.blue(overlay) * amount)
        );
    }

    private static final class SmoothDrawable extends Drawable {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final int strokeWidth;
        private final float radius;

        SmoothDrawable(int fillColor, int strokeColor, int strokeWidth, float radius) {
            this.strokeWidth = strokeWidth;
            this.radius = radius;
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(fillColor);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(strokeWidth);
            strokePaint.setColor(strokeColor);
        }

        @Override
        public void draw(Canvas canvas) {
            float inset = strokeWidth > 0 ? strokeWidth / 2f : 0f;
            rect.set(getBounds());
            rect.inset(inset, inset);
            buildContinuousRoundRect(rect, Math.min(radius, Math.min(rect.width(), rect.height()) / 2f), path);
            canvas.drawPath(path, fillPaint);
            if (strokeWidth > 0) {
                canvas.drawPath(path, strokePaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
            strokePaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            fillPaint.setColorFilter(colorFilter);
            strokePaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        private static void buildContinuousRoundRect(RectF r, float radius, Path out) {
            float smoothing = radius * 0.55f;
            out.reset();
            out.moveTo(r.left + radius, r.top);
            out.lineTo(r.right - radius, r.top);
            out.cubicTo(r.right - smoothing, r.top, r.right, r.top + smoothing, r.right, r.top + radius);
            out.lineTo(r.right, r.bottom - radius);
            out.cubicTo(r.right, r.bottom - smoothing, r.right - smoothing, r.bottom, r.right - radius, r.bottom);
            out.lineTo(r.left + radius, r.bottom);
            out.cubicTo(r.left + smoothing, r.bottom, r.left, r.bottom - smoothing, r.left, r.bottom - radius);
            out.lineTo(r.left, r.top + radius);
            out.cubicTo(r.left, r.top + smoothing, r.left + smoothing, r.top, r.left + radius, r.top);
            out.close();
        }
    }

    private static int resolveAccent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int resourceId = context.getResources().getIdentifier("system_accent1_600", "color", "android");
            if (resourceId != 0) {
                return normalize(context.getResources().getColor(resourceId, context.getTheme()));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperColors colors = WallpaperManager.getInstance(context)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null && colors.getPrimaryColor() != null) {
                    return normalize(colors.getPrimaryColor().toArgb());
                }
            } catch (RuntimeException ignored) {
            }
        }
        return Color.rgb(61, 112, 226);
    }

    private static int normalize(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0.34f, Math.min(0.68f, hsv[1]));
        hsv[2] = Math.max(0.38f, Math.min(0.68f, hsv[2]));
        return Color.HSVToColor(hsv);
    }

    private static int shiftValue(int color, float targetValue) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = targetValue;
        hsv[1] = Math.min(0.75f, hsv[1] + 0.05f);
        return Color.HSVToColor(hsv);
    }

    private static boolean isLight(int color) {
        double r = Color.red(color) / 255d;
        double g = Color.green(color) / 255d;
        double b = Color.blue(color) / 255d;
        double luminance = 0.2126d * r + 0.7152d * g + 0.0722d * b;
        return luminance > 0.58d;
    }
}
