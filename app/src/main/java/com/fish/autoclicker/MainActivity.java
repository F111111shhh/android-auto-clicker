package com.fish.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends android.app.Activity {
    private static final int MODE_RECT_ID = 1001;
    private static final int MODE_CIRCLE_ID = 1002;

    private UiTheme theme;
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
    private EditText fixedXInput;
    private EditText fixedYInput;
    private EditText opacityInput;
    private EditText collapseDelayInput;
    private CompoundButton infiniteSwitch;
    private CompoundButton randomPointSwitch;
    private CompoundButton randomIntervalSwitch;
    private TextView statusTitle;
    private TextView statusBody;
    private TextView accessibilityChip;
    private TextView overlayChip;
    private TextView rectSegment;
    private TextView circleSegment;
    private LinearLayout rectFields;
    private LinearLayout circleFields;
    private LinearLayout randomRangeFields;
    private LinearLayout fixedPointFields;
    private View countBlock;
    private View jitterBlock;
    private ScrollView scrollView;
    private LinearLayout contentRoot;
    private int baseRootBottomPadding;
    private int keyboardInsetBottom;
    private int windowInsetsKeyboardBottom;
    private int layoutKeyboardBottom;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    private ClickConfig config;
    private String regionMode = ClickConfig.REGION_RECT;
    private boolean receiverRegistered;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatus(intent.getStringExtra(ClickController.EXTRA_MESSAGE));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        theme = UiTheme.from(this);
        theme.styleSystemBars(this);
        config = ClickConfig.load(this);
        regionMode = config.regionMode;
        setContentView(buildContent());
        fillForm(config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiverCompat();
        config = ClickConfig.load(this);
        fillForm(config);
        updateStatus(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver);
            receiverRegistered = false;
        }
        saveFromForm();
    }

    @Override
    protected void onDestroy() {
        if (scrollView != null && keyboardLayoutListener != null) {
            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
            keyboardLayoutListener = null;
        }
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scrollView = scroll;
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(theme.background);

        LinearLayout root = new LinearLayout(this);
        contentRoot = root;
        baseRootBottomPadding = dp(30);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), statusTopPadding(), dp(20), baseRootBottomPadding);
        scroll.addView(root, matchWrap());
        installKeyboardAvoidance(scroll);

        root.addView(header());
        root.addView(statusCard(), topMargin(16));
        root.addView(planCard(), topMargin(16));
        root.addView(rhythmCard(), topMargin(14));
        root.addView(regionCard(), topMargin(14));
        root.addView(floatingCard(), topMargin(14));
        root.addView(actionPanel(), topMargin(18));
        return scroll;
    }

    private View header() {
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("连点器");
        theme.title(title, 32);
        titles.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("自动点击 · 悬浮控制");
        theme.body(subtitle, 13);
        subtitle.setPadding(0, dp(6), 0, 0);
        titles.addView(subtitle);
        return titles;
    }

    private View statusCard() {
        LinearLayout card = card(theme.accentContainer);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        statusTitle = new TextView(this);
        statusTitle.setText("待开始");
        statusTitle.setTextColor(theme.accentStrong);
        statusTitle.setTextSize(17);
        statusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(statusTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        accessibilityChip = chip("辅助功能", false);
        overlayChip = chip("悬浮窗", false);
        chips.addView(accessibilityChip);
        LinearLayout.LayoutParams chipGap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chipGap.setMargins(dp(8), 0, 0, 0);
        chips.addView(overlayChip, chipGap);
        top.addView(chips);
        card.addView(top);

        statusBody = new TextView(this);
        statusBody.setTextColor(theme.text);
        statusBody.setTextSize(14);
        statusBody.setLineSpacing(dp(2), 1f);
        statusBody.setPadding(0, dp(12), 0, 0);
        card.addView(statusBody);
        return card;
    }

    private View planCard() {
        LinearLayout card = sectionCard("点击计划", "设置总次数，或保持运行直到手动停止。");
        infiniteSwitch = switchRow(card, "无限点击", "开启后忽略固定次数");
        infiniteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshEnabledStates();
            }
        });
        countInput = numberInput("固定次数", "100");
        countBlock = inputBlock(countInput);
        card.addView(countBlock);
        return card;
    }

    private View rhythmCard() {
        LinearLayout card = sectionCard("点击节奏", "控制点击间隔和时间浮动。");
        intervalInput = numberInput("点击间隔", "200 ms");
        jitterInput = numberInput("随机浮动", "30%");
        card.addView(inputBlock(intervalInput));
        randomIntervalSwitch = switchRow(card, "不等间隔", "开启后随机浮动才会生效");
        randomIntervalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshEnabledStates();
            }
        });
        jitterBlock = inputBlock(jitterInput);
        card.addView(jitterBlock);
        return card;
    }

    private View regionCard() {
        LinearLayout card = sectionCard("点击范围", "随机点击会严格落在所选范围内；关闭随机后使用固定点击点。");

        randomPointSwitch = switchRow(card, "区域内随机点击", "开启后使用下方矩形或圆形范围");
        randomPointSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshEnabledStates();
            }
        });

        randomRangeFields = new LinearLayout(this);
        randomRangeFields.setOrientation(LinearLayout.VERTICAL);
        randomRangeFields.addView(regionSegments());

        leftInput = numberInput("左 X", "300");
        topInput = numberInput("上 Y", "600");
        rightInput = numberInput("右 X", "700");
        bottomInput = numberInput("下 Y", "1000");
        centerXInput = numberInput("中心 X", "540");
        centerYInput = numberInput("中心 Y", "900");
        radiusInput = numberInput("半径", "120");

        rectFields = new LinearLayout(this);
        rectFields.setOrientation(LinearLayout.VERTICAL);
        rectFields.addView(twoColumnInputs(leftInput, topInput));
        rectFields.addView(twoColumnInputs(rightInput, bottomInput));
        randomRangeFields.addView(rectFields);

        circleFields = new LinearLayout(this);
        circleFields.setOrientation(LinearLayout.VERTICAL);
        circleFields.addView(twoColumnInputs(centerXInput, centerYInput));
        circleFields.addView(inputBlock(radiusInput));
        randomRangeFields.addView(circleFields);
        card.addView(randomRangeFields);

        fixedPointFields = new LinearLayout(this);
        fixedPointFields.setOrientation(LinearLayout.VERTICAL);
        fixedXInput = numberInput("固定点 X", "540");
        fixedYInput = numberInput("固定点 Y", "900");
        fixedPointFields.addView(twoColumnInputs(fixedXInput, fixedYInput));
        TextView hint = new TextView(this);
        hint.setText("关闭区域随机点击后，每次都会点击这个固定点。");
        theme.body(hint, 12);
        hint.setPadding(dp(2), dp(8), 0, 0);
        fixedPointFields.addView(hint);
        card.addView(fixedPointFields);

        Button selectRegion = tonalButton("打开悬浮窗并选择范围/点击点");
        selectRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                ensureOverlayThenStart(true);
            }
        });
        card.addView(selectRegion, topMargin(12, LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        return card;
    }

    private View regionSegments() {
        LinearLayout segmentRow = new LinearLayout(this);
        segmentRow.setOrientation(LinearLayout.HORIZONTAL);
        segmentRow.setPadding(0, dp(12), 0, 0);
        rectSegment = segment("矩形范围", MODE_RECT_ID);
        circleSegment = segment("中心点半径", MODE_CIRCLE_ID);
        segmentRow.addView(rectSegment, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        circleParams.setMargins(dp(8), 0, 0, 0);
        segmentRow.addView(circleSegment, circleParams);
        return segmentRow;
    }

    private View floatingCard() {
        LinearLayout card = sectionCard("悬浮窗", "设置悬浮面板透明度和自动收起时间。");
        opacityInput = numberInput("透明度", "88%");
        collapseDelayInput = numberInput("无操作后收起", "6 秒");
        card.addView(twoColumnInputs(opacityInput, collapseDelayInput));
        return card;
    }

    private View actionPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        Button openFloating = primaryButton("打开悬浮控制");
        openFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                ensureOverlayThenStart(false);
            }
        });
        panel.addView(openFloating, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        Button save = tonalButton("保存设置");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromForm();
                Toast.makeText(MainActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                updateStatus("已保存设置");
            }
        });
        panel.addView(save, topMargin(10, LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        Button permissions = quietButton("权限与系统设置");
        permissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PermissionActivity.class));
            }
        });
        panel.addView(permissions, topMargin(10, LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        return panel;
    }

    private void fillForm(ClickConfig c) {
        if (c == null || countInput == null) {
            return;
        }
        regionMode = c.regionMode;
        infiniteSwitch.setChecked(c.infinite);
        countInput.setText(String.valueOf(c.clickCount));
        intervalInput.setText(String.valueOf(c.intervalMs));
        randomPointSwitch.setChecked(c.randomPoint);
        randomIntervalSwitch.setChecked(c.randomInterval);
        jitterInput.setText(String.valueOf(c.intervalJitterPercent));
        leftInput.setText(String.valueOf(Math.round(c.left)));
        topInput.setText(String.valueOf(Math.round(c.top)));
        rightInput.setText(String.valueOf(Math.round(c.right)));
        bottomInput.setText(String.valueOf(Math.round(c.bottom)));
        centerXInput.setText(String.valueOf(Math.round(c.centerX)));
        centerYInput.setText(String.valueOf(Math.round(c.centerY)));
        radiusInput.setText(String.valueOf(Math.round(c.radius)));
        fixedXInput.setText(String.valueOf(Math.round(c.fixedX)));
        fixedYInput.setText(String.valueOf(Math.round(c.fixedY)));
        opacityInput.setText(String.valueOf(c.overlayOpacityPercent));
        collapseDelayInput.setText(String.valueOf(c.collapseDelaySeconds));
        refreshRegionMode();
        refreshEnabledStates();
    }

    private void saveFromForm() {
        if (config == null) {
            config = ClickConfig.load(this);
        }
        config.infinite = infiniteSwitch.isChecked();
        config.clickCount = readInt(countInput, 100);
        config.intervalMs = readInt(intervalInput, 200);
        config.randomPoint = randomPointSwitch.isChecked();
        config.randomInterval = randomIntervalSwitch.isChecked();
        config.intervalJitterPercent = readInt(jitterInput, 30);
        config.regionMode = regionMode;
        config.left = readFloat(leftInput, 300f);
        config.top = readFloat(topInput, 600f);
        config.right = readFloat(rightInput, 700f);
        config.bottom = readFloat(bottomInput, 1000f);
        config.centerX = readFloat(centerXInput, 540f);
        config.centerY = readFloat(centerYInput, 900f);
        config.radius = readFloat(radiusInput, 120f);
        config.fixedX = readFloat(fixedXInput, 540f);
        config.fixedY = readFloat(fixedYInput, 900f);
        config.overlayOpacityPercent = readInt(opacityInput, 88);
        config.collapseDelaySeconds = readInt(collapseDelayInput, 6);
        config.save(this);
    }

    private void ensureOverlayThenStart(boolean selectRegion) {
        if (!PermissionUtils.isOverlayAllowed(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, PermissionActivity.class));
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

    private void updateStatus(String message) {
        ClickController controller = ClickController.get();
        boolean accessibility = PermissionUtils.isAccessibilityEnabled(this);
        boolean overlay = PermissionUtils.isOverlayAllowed(this);

        setChip(accessibilityChip, "辅助功能", accessibility);
        setChip(overlayChip, "悬浮窗", overlay);

        String title = TextUtils.isEmpty(message) ? "待开始" : message;
        if (controller.isRunning()) {
            title = controller.isPaused() ? "已暂停" : "点击中";
        }
        statusTitle.setText(title);

        StringBuilder builder = new StringBuilder();
        if (controller.isRunning()) {
            builder.append("进度 ")
                    .append(controller.completed())
                    .append("/")
                    .append(controller.total() == Integer.MAX_VALUE ? "无限" : controller.total())
                    .append("\n");
        }
        if (config != null) {
            builder.append(config.describeRegion());
        }
        statusBody.setText(builder.toString());
    }

    private LinearLayout sectionCard(String title, String subtitle) {
        LinearLayout card = card(theme.surface);
        TextView titleView = new TextView(this);
        theme.title(titleView, 18);
        titleView.setText(title);
        card.addView(titleView);

        TextView subtitleView = new TextView(this);
        theme.body(subtitleView, 13);
        subtitleView.setText(subtitle);
        subtitleView.setPadding(0, dp(7), 0, 0);
        card.addView(subtitleView);
        return card;
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(theme.stroked(color, theme.outline, 24, this));
        return card;
    }

    private CompoundButton switchRow(LinearLayout parent, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(2));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(theme.text);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        theme.body(subtitleView, 12);
        subtitleView.setPadding(0, dp(4), 0, 0);
        textBox.addView(titleView);
        textBox.addView(subtitleView);
        row.addView(textBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch control = new Switch(this);
        tintSwitch(control);
        row.addView(control);
        parent.addView(row);
        return control;
    }

    private EditText numberInput(String label, String hint) {
        EditText editText = new EditText(this);
        editText.setTag(label);
        editText.setSingleLine(true);
        editText.setTextSize(18);
        editText.setTextColor(theme.text);
        editText.setHint(hint);
        editText.setHintTextColor(theme.subtext);
        editText.setSelectAllOnFocus(true);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setPadding(dp(14), 0, dp(14), 0);
        editText.setBackground(theme.stroked(theme.field, theme.outline, 16, this));
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ensureFocusedInputVisible(v);
                }
            }
        });
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureFocusedInputVisible(v);
            }
        });
        return editText;
    }

    private void ensureFocusedInputVisible(final View view) {
        if (scrollView == null) {
            return;
        }
        scrollInputIntoView(view, 120);
        scrollInputIntoView(view, 320);
        scrollInputIntoView(view, 560);
    }

    private void scrollInputIntoView(final View view, int delayMs) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scrollView == null) {
                    return;
                }
                setLayoutKeyboardInset(estimateKeyboardInset());
                Rect rect = new Rect();
                view.getDrawingRect(rect);
                scrollView.offsetDescendantRectToMyCoords(view, rect);
                int bottomClearance = keyboardInsetBottom > 0
                        ? keyboardInsetBottom + dp(36)
                        : dp(120);
                int target = Math.max(0, rect.bottom - scrollView.getHeight() + bottomClearance);
                int current = scrollView.getScrollY();
                if (rect.top - current < dp(18)) {
                    target = Math.max(0, rect.top - dp(18));
                    scrollView.smoothScrollTo(0, target);
                } else if (target > current) {
                    scrollView.smoothScrollTo(0, target);
                }
            }
        }, delayMs);
    }

    private void installKeyboardAvoidance(final ScrollView scroll) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    setWindowInsetsKeyboardInset(keyboardInsetFromInsets(insets));
                    return insets;
                }
            });
            scroll.requestApplyInsets();
        }
        keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setLayoutKeyboardInset(estimateKeyboardInset());
            }
        };
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private int keyboardInsetFromInsets(WindowInsets insets) {
        if (insets == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            int navigation = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            return Math.max(0, ime - navigation);
        }
        return 0;
    }

    private int estimateKeyboardInset() {
        if (scrollView == null) {
            return 0;
        }
        Rect visible = new Rect();
        View root = scrollView.getRootView();
        root.getWindowVisibleDisplayFrame(visible);
        int hidden = Math.max(0, root.getHeight() - visible.bottom);
        return hidden > dp(140) ? hidden : 0;
    }

    private void setWindowInsetsKeyboardInset(int inset) {
        windowInsetsKeyboardBottom = Math.max(0, inset);
        applyKeyboardInset();
    }

    private void setLayoutKeyboardInset(int inset) {
        layoutKeyboardBottom = Math.max(0, inset);
        applyKeyboardInset();
    }

    private void applyKeyboardInset() {
        int normalized = Math.max(windowInsetsKeyboardBottom, layoutKeyboardBottom);
        if (Math.abs(normalized - keyboardInsetBottom) < dp(6)) {
            return;
        }
        keyboardInsetBottom = normalized;
        updateContentBottomPadding();
        View focused = getCurrentFocus();
        if (keyboardInsetBottom > 0 && focused instanceof EditText) {
            ensureFocusedInputVisible(focused);
        }
    }

    private void updateContentBottomPadding() {
        if (contentRoot == null) {
            return;
        }
        int dynamicBottom = keyboardInsetBottom > 0
                ? keyboardInsetBottom + dp(28)
                : baseRootBottomPadding;
        contentRoot.setPadding(
                contentRoot.getPaddingLeft(),
                contentRoot.getPaddingTop(),
                contentRoot.getPaddingRight(),
                dynamicBottom
        );
    }

    private View inputBlock(EditText input) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(12), 0, 0);

        TextView label = new TextView(this);
        label.setText(String.valueOf(input.getTag()));
        label.setTextColor(theme.subtext);
        label.setTextSize(12);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(dp(2), 0, 0, dp(6));
        block.addView(label);
        block.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        ));
        return block;
    }

    private View twoColumnInputs(EditText left, EditText right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(inputBlock(left), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMargins(dp(10), 0, 0, 0);
        row.addView(inputBlock(right), rightParams);
        return row;
    }

    private TextView segment(String text, int id) {
        TextView view = new TextView(this);
        view.setId(id);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regionMode = v.getId() == MODE_CIRCLE_ID ? ClickConfig.REGION_CIRCLE : ClickConfig.REGION_RECT;
                refreshRegionMode();
            }
        });
        return view;
    }

    private void refreshRegionMode() {
        boolean circle = ClickConfig.REGION_CIRCLE.equals(regionMode);
        styleSegment(rectSegment, !circle);
        styleSegment(circleSegment, circle);
        rectFields.setVisibility(circle ? View.GONE : View.VISIBLE);
        circleFields.setVisibility(circle ? View.VISIBLE : View.GONE);
    }

    private void refreshEnabledStates() {
        if (countBlock != null) {
            setGroupEnabled(countBlock, !infiniteSwitch.isChecked());
        }
        if (jitterBlock != null) {
            setGroupEnabled(jitterBlock, randomIntervalSwitch.isChecked());
        }
        boolean randomPoint = randomPointSwitch.isChecked();
        if (randomRangeFields != null) {
            setGroupEnabled(randomRangeFields, randomPoint);
            randomRangeFields.setVisibility(randomPoint ? View.VISIBLE : View.GONE);
        }
        if (fixedPointFields != null) {
            setGroupEnabled(fixedPointFields, !randomPoint);
            fixedPointFields.setVisibility(randomPoint ? View.GONE : View.VISIBLE);
        }
    }

    private void setGroupEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.42f);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setGroupEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private void styleSegment(TextView view, boolean selected) {
        if (view == null) {
            return;
        }
        int bg = selected ? theme.accent : theme.surfaceHigh;
        int text = selected ? theme.onAccent() : theme.text;
        view.setTextColor(text);
        view.setBackground(theme.ripple(theme.stroked(bg, selected ? theme.accent : theme.outline, 18, this), theme.accent));
    }

    private TextView chip(String text, boolean active) {
        TextView chip = new TextView(this);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        setChip(chip, text, active);
        return chip;
    }

    private void setChip(TextView chip, String label, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setText(label + (active ? " 已开" : " 未开"));
        chip.setTextColor(active ? theme.success : theme.subtext);
        chip.setBackground(theme.stroked(active ? Color.WHITE : theme.surfaceHigh,
                active ? UiTheme.mix(theme.success, Color.WHITE, 0.35f) : theme.outline,
                18,
                this));
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.onAccent());
        button.setBackground(theme.ripple(theme.rounded(theme.accent, 20, this), theme.accentStrong));
        return button;
    }

    private Button tonalButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.accentStrong);
        button.setBackground(theme.ripple(theme.rounded(theme.accentSoft, 18, this), theme.accent));
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
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private void tintSwitch(Switch control) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        control.setThumbTintList(new android.content.res.ColorStateList(
                states,
                new int[]{theme.accent, Color.WHITE}
        ));
        control.setTrackTintList(new android.content.res.ColorStateList(
                states,
                new int[]{theme.accentSoft, theme.outline}
        ));
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams topMargin(int topDp) {
        return topMargin(topDp, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int topDp, int width, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, dp(topDp), 0, 0);
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
        return UiTheme.dp(this, value);
    }

    private int statusTopPadding() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int status = resourceId == 0 ? dp(22) : getResources().getDimensionPixelSize(resourceId);
        return status + dp(16);
    }

    private void registerReceiverCompat() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(ClickController.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        receiverRegistered = true;
    }
}
