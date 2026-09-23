package com.fish.autoclicker;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;

import java.security.SecureRandom;
import java.util.Random;

final class ClickController {
    static final String ACTION_STATE_CHANGED = "com.fish.autoclicker.STATE_CHANGED";
    static final String EXTRA_RUNNING = "running";
    static final String EXTRA_PAUSED = "paused";
    static final String EXTRA_DONE = "done";
    static final String EXTRA_TOTAL = "total";
    static final String EXTRA_MESSAGE = "message";

    private static final ClickController INSTANCE = new ClickController();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new SecureRandom();
    private ClickConfig config;
    private Context appContext;
    private boolean running;
    private boolean paused;
    private int completed;
    private int runId;
    private boolean gesturePending;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            performNext();
        }
    };

    static ClickController get() {
        return INSTANCE;
    }

    void start(Context context, ClickConfig newConfig) {
        if (running) {
            if (paused) {
                paused = false;
                notifyState("继续点击");
                handler.removeCallbacks(tick);
                if (!gesturePending) {
                    handler.postDelayed(tick, Math.max(50, config.nextDelayMs(random)));
                }
            }
            return;
        }
        appContext = context.getApplicationContext();
        config = newConfig;
        config.save(appContext);
        runId++;
        completed = 0;
        gesturePending = false;
        running = true;
        paused = false;
        notifyState("开始点击");
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    void pauseOrResume() {
        if (!running) {
            notifyState("还没有开始");
            return;
        }
        paused = !paused;
        notifyState(paused ? "已暂停" : "继续点击");
        handler.removeCallbacks(tick);
        if (!paused && !gesturePending) {
            handler.postDelayed(tick, Math.max(50, config.nextDelayMs(random)));
        }
    }

    void stop() {
        stopWithMessage("已停止");
    }

    boolean isRunning() {
        return running;
    }

    boolean isPaused() {
        return paused;
    }

    int completed() {
        return completed;
    }

    int total() {
        return config == null ? 0 : config.maxClicks();
    }

    String activeRegionDescription() {
        return config == null ? "" : config.describeRegion();
    }

    private void performNext() {
        if (!running || paused || config == null || gesturePending) {
            return;
        }
        if (ClickAccessibilityService.instance() == null) {
            stopWithMessage("请先开启辅助功能服务");
            return;
        }
        if (completed >= config.maxClicks()) {
            stopWithMessage("点击完成");
            return;
        }

        PointF point = config.nextPoint(random);
        final int dispatchedRunId = runId;
        gesturePending = true;
        ClickAccessibilityService.instance().tap(point.x, point.y, new Runnable() {
            @Override
            public void run() {
                if (dispatchedRunId != runId) {
                    return;
                }
                gesturePending = false;
                completed++;
                if (completed >= config.maxClicks()) {
                    stopWithMessage("点击完成");
                } else if (running && !paused) {
                    notifyState("点击中");
                    handler.postDelayed(tick, config.nextDelayMs(random));
                } else if (paused) {
                    notifyState("已暂停");
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (dispatchedRunId != runId) {
                    return;
                }
                gesturePending = false;
                stopWithMessage("点击失败，请确认辅助功能权限");
            }
        });
    }

    private void stopWithMessage(String message) {
        handler.removeCallbacks(tick);
        runId++;
        gesturePending = false;
        running = false;
        paused = false;
        notifyState(message);
    }

    private void notifyState(String message) {
        if (appContext == null) {
            return;
        }
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.setPackage(appContext.getPackageName());
        intent.putExtra(EXTRA_RUNNING, running);
        intent.putExtra(EXTRA_PAUSED, paused);
        intent.putExtra(EXTRA_DONE, completed);
        intent.putExtra(EXTRA_TOTAL, total());
        intent.putExtra(EXTRA_MESSAGE, message);
        appContext.sendBroadcast(intent);
    }
}
