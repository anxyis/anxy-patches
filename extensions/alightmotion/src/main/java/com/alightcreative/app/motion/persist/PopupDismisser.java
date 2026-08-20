package com.alightcreative.app.motion.persist;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class PopupDismisser {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static volatile boolean running = false;

    public static void onStart() {
        if (running) return;
        running = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    dismissPopups();
                } catch (Throwable ignored) {
                }
                if (running) {
                    handler.postDelayed(this, 300);
                }
            }
        });
    }

    private static void dismissPopups() {
        try {
            Class<?> wmgClass = Class.forName("android.view.WindowManagerGlobal");
            Method getInstanceMethod = wmgClass.getMethod("getInstance");
            Object wmgInstance = getInstanceMethod.invoke(null);

            Field mViewsField = wmgClass.getDeclaredField("mViews");
            mViewsField.setAccessible(true);
            Object viewsObj = mViewsField.get(wmgInstance);

            if (viewsObj instanceof ArrayList) {
                ArrayList<?> viewsList = (ArrayList<?>) viewsObj;
                for (int i = viewsList.size() - 1; i >= 0; i--) {
                    Object viewObj = viewsList.get(i);
                    if (viewObj instanceof View) {
                        View root = (View) viewObj;
                        if (containsMarker(root)) {
                            root.setVisibility(View.GONE);
                            try {
                                Method hideMethod = root.getClass().getMethod("setVisibility", int.class);
                                hideMethod.invoke(root, View.GONE);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean containsMarker(View view) {
        if (view == null) return false;

        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String text = cs.toString();
                if (text.contains("Satriyaid") ||
                    text.contains("Modded by") ||
                    text.contains("VISIT LINK") ||
                    text.contains("DONT SHOW AGAIN") ||
                    text.contains("DON'T SHOW AGAIN") ||
                    text.contains("Telegram") ||
                    text.contains("JOIN MY")) {
                    return true;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                if (containsMarker(vg.getChildAt(i))) {
                    return true;
                }
            }
        }

        return false;
    }
}
