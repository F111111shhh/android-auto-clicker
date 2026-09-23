package com.fish.autoclicker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AboutActivity extends android.app.Activity {
    private static final String GITHUB_URL = "https://github.com/F111111shhh/android-auto-clicker";
    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/F111111shhh/android-auto-clicker/releases/latest";

    private UiTheme theme;
    private Button updateButton;
    private TextView updateStatus;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = UiTheme.from(this);
        theme.styleSystemBars(this);
        setContentView(buildContent());
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.background);
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), statusTopPadding(), dp(20), dp(28));
        scroll.addView(root, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("关于软件");
        theme.title(title, 27);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button back = quietButton("返回");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(74), dp(48)));
        root.addView(header);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.app_icon);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(124), dp(124));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.topMargin = dp(26);
        root.addView(logo, logoParams);

        TextView name = new TextView(this);
        name.setText("连点器");
        name.setGravity(Gravity.CENTER);
        theme.title(name, 24);
        root.addView(name, topMargin(8));

        root.addView(versionCard(), topMargin(22));
        root.addView(featureCard(), topMargin(14));
        root.addView(developerCard(), topMargin(14));
        return scroll;
    }

    private View versionCard() {
        LinearLayout card = card(theme.surface);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText("当前版本");
        theme.title(label, 17);
        labels.addView(label);
        TextView version = new TextView(this);
        version.setText(getVersionName());
        theme.body(version, 13);
        version.setPadding(0, dp(5), 0, 0);
        labels.addView(version);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        updateButton = tonalButton("检查更新");
        updateButton.setOnClickListener(v -> checkForUpdates());
        row.addView(updateButton, new LinearLayout.LayoutParams(dp(112), dp(48)));
        card.addView(row);

        updateStatus = new TextView(this);
        updateStatus.setText("从 GitHub 检查最新版本");
        theme.body(updateStatus, 12);
        updateStatus.setPadding(0, dp(11), 0, 0);
        card.addView(updateStatus);
        return card;
    }

    private View featureCard() {
        LinearLayout card = sectionCard("功能简介");
        TextView body = new TextView(this);
        body.setText("点击计划：设置点击次数和间隔，也可以持续点击直到手动停止。\n点击位置：指定一个固定点，或在选定的矩形、圆形区域内随机点击。\n悬浮控制：随时开始、暂停、继续或停止；还可开启随机时间浮动。\n\n应用通过 Android 辅助功能服务执行你主动配置的点击，不需要 root，不读取目标应用内容。");
        theme.body(body, 14);
        body.setLineSpacing(dp(3), 1f);
        card.addView(body, topMargin(10));
        return card;
    }

    private View developerCard() {
        LinearLayout card = card(theme.surface);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView github = new ImageView(this);
        github.setImageResource(R.drawable.ic_github);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.rightMargin = dp(12);
        row.addView(github, iconParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("GitHub");
        theme.title(title, 16);
        labels.addView(title);
        TextView developer = new TextView(this);
        developer.setText("开发者：F111111shhh");
        theme.body(developer, 13);
        developer.setPadding(0, dp(4), 0, 0);
        labels.addView(developer);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button open = tonalButton("打开");
        open.setOnClickListener(v -> openUrl(GITHUB_URL));
        row.addView(open, new LinearLayout.LayoutParams(dp(82), dp(46)));
        row.setOnClickListener(v -> openUrl(GITHUB_URL));
        card.addView(row);
        return card;
    }

    private void checkForUpdates() {
        updateButton.setEnabled(false);
        updateButton.setText("检查中");
        updateStatus.setTextColor(theme.subtext);
        updateStatus.setText("正在连接 GitHub...");
        final String currentVersion = getVersionName();
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "LianDianQi/" + currentVersion);
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException("HTTP " + responseCode);
                }
                String json = readAll(connection.getInputStream());
                JSONObject release = new JSONObject(json);
                String tag = release.optString("tag_name", "");
                if (!cleanVersion(tag).matches("\\d+\\.\\d+\\.\\d+(?:[-+].*)?")) {
                    throw new IllegalStateException("Missing release version");
                }
                String url = release.optString("html_url", GITHUB_URL);
                String body = release.optString("body", "暂无更新说明");
                runOnUiThread(() -> handleRelease(currentVersion, tag, url, body));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    updateButton.setEnabled(true);
                    updateButton.setText("检查更新");
                    updateStatus.setTextColor(theme.danger);
                    updateStatus.setText("检查失败：无法获取 GitHub 版本，请检查网络后重试");
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void handleRelease(String currentVersion, String tag, String url, String body) {
        updateButton.setEnabled(true);
        updateButton.setText("检查更新");
        String latestVersion = cleanVersion(tag);
        if (compareVersions(latestVersion, currentVersion) > 0) {
            updateStatus.setText("发现新版本 " + latestVersion);
            showReleaseDialog(latestVersion, url, body);
        } else {
            updateStatus.setText("当前已是最新版本");
        }
    }

    private void showReleaseDialog(String version, String url, String body) {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = card(theme.surface);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));

        TextView title = new TextView(this);
        title.setText("发现新版本 " + version);
        theme.title(title, 20);
        panel.addView(title);

        TextView releaseBody = new TextView(this);
        releaseBody.setText(body == null || body.trim().isEmpty() ? "暂无更新说明" : body.trim());
        theme.body(releaseBody, 13);
        releaseBody.setLineSpacing(dp(2), 1f);
        ScrollView releaseScroll = new ScrollView(this);
        releaseScroll.addView(releaseBody);
        panel.addView(releaseScroll, topMargin(14, LinearLayout.LayoutParams.MATCH_PARENT, dp(220)));

        Button open = primaryButton("打开 GitHub 更新");
        open.setOnClickListener(v -> {
            openUrl(url);
            dialog.dismiss();
        });
        panel.addView(open, topMargin(14, LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        Button close = quietButton("稍后");
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close, topMargin(8, LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        dialog.setContentView(panel);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(40), dp(420)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private LinearLayout sectionCard(String title) {
        LinearLayout card = card(theme.surface);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        theme.title(titleView, 18);
        card.addView(titleView);
        return card;
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(theme.stroked(color, theme.outline, 24, this));
        return card;
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
        button.setBackground(theme.ripple(theme.rounded(theme.accentContainer, 20, this), theme.accent));
        return button;
    }

    private Button quietButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(theme.accentStrong);
        button.setBackground(theme.ripple(theme.rounded(theme.accentContainer, 20, this), theme.accent));
        return button;
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        return button;
    }

    private LinearLayout.LayoutParams topMargin(int topDp) {
        return topMargin(topDp, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int topDp, int width, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(topDp);
        return params;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException ignored) {
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "未知版本";
        }
    }

    private String cleanVersion(String version) {
        if (version == null) {
            return "0.0.0";
        }
        return version.trim().replaceFirst("^[vV]", "");
    }

    private int compareVersions(String left, String right) {
        String[] a = cleanVersion(left).split("\\.");
        String[] b = cleanVersion(right).split("\\.");
        for (int i = 0; i < 3; i++) {
            int av = i < a.length ? parseVersionPart(a[i]) : 0;
            int bv = i < b.length ? parseVersionPart(b[i]) : 0;
            if (av != bv) {
                return av < bv ? -1 : 1;
            }
        }
        boolean leftPreview = left.contains("-");
        boolean rightPreview = right.contains("-");
        if (leftPreview != rightPreview) {
            return leftPreview ? -1 : 1;
        }
        return 0;
    }

    private int parseVersionPart(String value) {
        String digits = value.replaceAll("[^0-9].*", "");
        try {
            return Integer.parseInt(digits.isEmpty() ? "0" : digits);
        } catch (NumberFormatException ignored) {
            return 0;
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
}
