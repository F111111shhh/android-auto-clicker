package com.fish.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends android.app.Activity {
    private EditText countInput;
    private EditText intervalInput;
    private EditText jitterInput;
    private EditText leftInput;
    private EditText topInput;
    private EditText rightInput;
    private EditText bottomInput;
    private EditText centerXInput;
    private EditText centerYInput;
    private EditText radiusInput;
    private CheckBox infiniteCheck;
    private CheckBox randomPointCheck;
    private CheckBox randomIntervalCheck;
    private RadioGroup regionGroup;
    private TextView statusText;
    private ClickConfig config;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatus(intent.getStringExtra(ClickController.EXTRA_MESSAGE));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = ClickConfig.load(this);
        setContentView(buildContent());
        fillForm(config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiverCompat();
        updateStatus(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(stateReceiver);
        saveFromForm();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(248, 250, 252));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("连点器");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        statusText = new TextView(this);
        statusText.setTextColor(Color.rgb(3, 105, 161));
        statusText.setTextSize(14);
        statusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        statusText.setBackgroundResource(com.fish.autoclicker.R.drawable.bg_status);
        root.addView(statusText, matchWrap());

        root.addView(section("点击次数"));
        infiniteCheck = checkBox("无限点击，手动停止");
        root.addView(infiniteCheck);
        countInput = input("固定次数，例如 100");
        root.addView(countInput, matchWrap());

        root.addView(section("点击频率"));
        intervalInput = input("点击间隔毫秒，例如 200");
        root.addView(intervalInput, matchWrap());
        randomIntervalCheck = checkBox("不按完全相同间隔点击");
        root.addView(randomIntervalCheck);
        jitterInput = input("时间浮动百分比，例如 30");
        root.addView(jitterInput, matchWrap());

        root.addView(section("点击范围"));
        regionGroup = new RadioGroup(this);
        regionGroup.setOrientation(RadioGroup.HORIZONTAL);
        android.widget.RadioButton rect = radio("矩形范围", 1001);
        android.widget.RadioButton circle = radio("中心点半径", 1002);
        regionGroup.addView(rect);
        regionGroup.addView(circle);
        root.addView(regionGroup);
        randomPointCheck = checkBox("在指定区域随机点击");
        root.addView(randomPointCheck);

        leftInput = input("矩形左 X");
        topInput = input("矩形上 Y");
        rightInput = input("矩形右 X");
        bottomInput = input("矩形下 Y");
        centerXInput = input("中心 X");
        centerYInput = input("中心 Y");
        radiusInput = input("半径");
        root.addView(twoColumns(leftInput, topInput));
        root.addView(twoColumns(rightInput, bottomInput));
        root.addView(twoColumns(centerXInput, centerYInput));
        root.addView(radiusInput, matchWrap());

        Button save = primaryButton("保存设置");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                Toast.makeText(MainActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                updateStatus("已保存设置");
            }
        });
        root.addView(save, matchWrapWithTop());

        Button chooseRegion = secondaryButton("打开悬浮窗并选择范围");
        chooseRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                ensureOverlayThenStart(true);
            }
        });
        root.addView(chooseRegion, matchWrapWithTop());

        Button startFloating = primaryButton("打开悬浮控制");
        startFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                ensureOverlayThenStart(false);
            }
        });
        root.addView(startFloating, matchWrapWithTop());

        Button accessibility = secondaryButton("开启辅助功能权限");
        accessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(accessibility, matchWrapWithTop());

        Button overlay = secondaryButton("开启悬浮窗权限");
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        });
        root.addView(overlay, matchWrapWithTop());

        return scroll;
    }

    private void fillForm(ClickConfig c) {
        infiniteCheck.setChecked(c.infinite);
        countInput.setText(String.valueOf(c.clickCount));
        intervalInput.setText(String.valueOf(c.intervalMs));
        randomPointCheck.setChecked(c.randomPoint);
        randomIntervalCheck.setChecked(c.randomInterval);
        jitterInput.setText(String.valueOf(c.intervalJitterPercent));
        regionGroup.check(ClickConfig.REGION_CIRCLE.equals(c.regionMode) ? 1002 : 1001);
        leftInput.setText(String.valueOf(Math.round(c.left)));
        topInput.setText(String.valueOf(Math.round(c.top)));
        rightInput.setText(String.valueOf(Math.round(c.right)));
        bottomInput.setText(String.valueOf(Math.round(c.bottom)));
        centerXInput.setText(String.valueOf(Math.round(c.centerX)));
        centerYInput.setText(String.valueOf(Math.round(c.centerY)));
        radiusInput.setText(String.valueOf(Math.round(c.radius)));
    }

    private void saveFromForm() {
        if (config == null) {
            config = ClickConfig.load(this);
        }
        config.infinite = infiniteCheck.isChecked();
        config.clickCount = readInt(countInput, 100);
        config.intervalMs = readInt(intervalInput, 200);
        config.randomPoint = randomPointCheck.isChecked();
        config.randomInterval = randomIntervalCheck.isChecked();
        config.intervalJitterPercent = readInt(jitterInput, 30);
        config.regionMode = regionGroup.getCheckedRadioButtonId() == 1002
                ? ClickConfig.REGION_CIRCLE
                : ClickConfig.REGION_RECT;
        config.left = readFloat(leftInput, 300f);
        config.top = readFloat(topInput, 600f);
        config.right = readFloat(rightInput, 700f);
        config.bottom = readFloat(bottomInput, 1000f);
        config.centerX = readFloat(centerXInput, 540f);
        config.centerY = readFloat(centerYInput, 900f);
        config.radius = readFloat(radiusInput, 120f);
        config.save(this);
    }

    private void ensureOverlayThenStart(boolean selectRegion) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }
        Intent intent = new Intent(this, FloatingControlService.class);
        intent.putExtra(FloatingControlService.EXTRA_SELECT_REGION, selectRegion);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        moveTaskToBack(true);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void updateStatus(String message) {
        ClickController controller = ClickController.get();
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(message)) {
            builder.append(message).append("  ");
        }
        builder.append("辅助功能：")
                .append(ClickAccessibilityService.instance() == null ? "未开启" : "已开启")
                .append("  悬浮窗：")
                .append(Settings.canDrawOverlays(this) ? "已允许" : "未允许");
        if (controller.isRunning()) {
            builder.append("  进度：")
                    .append(controller.completed())
                    .append("/")
                    .append(controller.total() == Integer.MAX_VALUE ? "无限" : controller.total())
                    .append(controller.isPaused() ? "，已暂停" : "，点击中");
        }
        if (config != null) {
            builder.append("\n").append(config.describeRegion());
        }
        statusText.setText(builder.toString());
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(17);
        view.setTextColor(Color.rgb(15, 23, 42));
        view.setPadding(0, dp(18), 0, dp(8));
        return view;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setTextSize(15);
        editText.setHint(hint);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setPadding(dp(10), 0, dp(10), 0);
        return editText;
    }

    private CheckBox checkBox(String text) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextSize(15);
        checkBox.setTextColor(Color.rgb(30, 41, 59));
        return checkBox;
    }

    private android.widget.RadioButton radio(String text, int id) {
        android.widget.RadioButton button = new android.widget.RadioButton(this);
        button.setId(id);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(Color.rgb(30, 41, 59));
        return button;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackgroundResource(com.fish.autoclicker.R.drawable.bg_button);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.rgb(15, 23, 42));
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackgroundResource(com.fish.autoclicker.R.drawable.bg_panel);
        return button;
    }

    private LinearLayout twoColumns(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1f);
        params.setMargins(0, 0, dp(8), 0);
        row.addView(left, params);
        LinearLayout.LayoutParams paramsRight = new LinearLayout.LayoutParams(0, dp(54), 1f);
        row.addView(right, paramsRight);
        return row;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWrapWithTop() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private int readInt(EditText editText, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(editText.getText().toString().trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float readFloat(EditText editText, float fallback) {
        try {
            return Float.parseFloat(editText.getText().toString().trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void registerReceiverCompat() {
        IntentFilter filter = new IntentFilter(ClickController.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
    }
}
