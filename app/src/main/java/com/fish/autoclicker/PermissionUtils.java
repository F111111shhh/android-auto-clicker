package com.fish.autoclicker;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

final class PermissionUtils {
    private PermissionUtils() {
    }

    static boolean isOverlayAllowed(Context context) {
        return Settings.canDrawOverlays(context);
    }

    static boolean isAccessibilityEnabled(Context context) {
        if (ClickAccessibilityService.instance() != null) {
            return true;
        }
        int enabled;
        try {
            enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException exception) {
            enabled = 0;
        }
        if (enabled != 1) {
            return false;
        }

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null) {
            return false;
        }
        ComponentName expected = new ComponentName(context, ClickAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName current = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(current)) {
                return true;
            }
        }
        return false;
    }
}
