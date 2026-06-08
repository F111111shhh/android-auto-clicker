package com.fish.autoclicker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PermissionActivity extends android.app.Activity {
    private UiTheme theme;
    private TextView accessibilityState;
    private TextView overlayState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = UiTheme.from(this);
        theme.styleSystemBars(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), statusTopPadding(), dp(20), dp(28));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("权限与系统设置");
        theme.title(title, 27);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button back = quietButton("返回");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(74), dp(42)));
        root.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("连点器只需要这两个权限：悬浮窗用于显示控制面板，辅助功能用于执行你设置的点击。");
        theme.body(subtitle, 14);
        subtitle.setLineSpacing(dp(2), 1f);
        subtitle.setPadding(0, dp(10), 0, 0);
        root.addView(subtitle);

        root.addView(permissionCard(
                "辅助功能服务",
                "启用“连点器点击服务”后，应用才能按你的参数发送点击手势。",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                },
                true
        ), topMargin(18));

        root.addView(permissionCard(
                "悬浮窗权限",
                "允许显示开始、暂停、停止和范围选择控件。",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())
                        );
                        startActivity(intent);
                    }
                },
                false
        ), topMargin(14));

        Button appSettings = quietButton("打开应用系统详情");
        appSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
        root.addView(appSettings, topMargin(16, LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        return scroll;
    }

    private View permissionCard(String title, String body, View.OnClickListener listener, boolean accessibility) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(theme.stroked(theme.surface, theme.outline, 24, this));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(theme.text);
        titleView.setTextSize(18);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView state = stateChip(false);
        if (accessibility) {
            accessibilityState = state;
        } else {
            overlayState = state;
        }
        titleRow.addView(state);
        card.addView(titleRow);

        TextView bodyView = new TextView(this);
        bodyView.setText(body);
        theme.body(bodyView, 14);
        bodyView.setLineSpacing(dp(2), 1f);
        bodyView.setPadding(0, dp(10), 0, dp(14));
        card.addView(bodyView);

        Button button = primaryButton(accessibility ? "去开启辅助功能" : "去开启悬浮窗");
        button.setOnClickListener(listener);
        card.addView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));
        return card;
    }

    private void refreshState() {
        setState(accessibilityState, PermissionUtils.isAccessibilityEnabled(this));
        setState(overlayState, PermissionUtils.isOverlayAllowed(this));
    }

    private TextView stateChip(boolean active) {
        TextView chip = new TextView(this);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        setState(chip, active);
        return chip;
    }

    private void setState(TextView chip, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setText(active ? "已开启" : "未开启");
        chip.setTextColor(active ? theme.success : theme.subtext);
        chip.setBackground(theme.stroked(active ? Color.WHITE : theme.surfaceHigh,
                active ? UiTheme.mix(theme.success, Color.WHITE, 0.35f) : theme.outline,
                18,
                this));
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.onAccent());
        button.setBackground(theme.ripple(theme.rounded(theme.accent, 18, this), theme.accentStrong));
        return button;
    }

    private Button quietButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.text);
        button.setBackground(theme.ripple(theme.stroked(theme.surface, theme.outline, 18, this), theme.accent));
        return button;
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private LinearLayout.LayoutParams topMargin(int topDp) {
        return topMargin(topDp, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int topDp, int width, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, dp(topDp), 0, 0);
        return params;
    }

    private int dp(int value) {
        return UiTheme.dp(this, value);
    }

    private int statusTopPadding() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int status = resourceId == 0 ? dp(22) : getResources().getDimensionPixelSize(resourceId);
        return status + dp(16);
    }
}
