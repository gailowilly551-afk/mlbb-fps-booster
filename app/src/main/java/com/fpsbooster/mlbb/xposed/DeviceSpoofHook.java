package com.fpsbooster.mlbb.xposed;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Deep Xposed / LSPatch spoofing & frame-pacing engine for Mobile Legends: Bang Bang.
 *
 * <ul>
 *   <li>Multi-stage profile loader: Bridge files -> Settings.Global -> SpoofConfigProvider -> XSharedPreferences
 *   <li>Full android.os.Build & SystemProperties (including native_get & Transsion/MTK scrub)
 *   <li>OpenGL ES GL_RENDERER & GL_VENDOR spoofing (GLES10/20/30 + GLImpl)
 *   <li>Display.getRefreshRate() & Display.Mode.getRefreshRate() mapped to real peak hardware Mode ID
 *   <li>Window preferredDisplayModeId & Surface.setFrameRate() unthrottling
 * </ul>
 */
public class DeviceSpoofHook implements IXposedHookLoadPackage {

    private static final String SELF_PACKAGE = "com.fpsbooster.mlbb";
    private static final String TARGET_PACKAGE = "com.mobile.legends";

    private static final int GL_VENDOR_ENUM = 0x1F00;
    private static final int GL_RENDERER_ENUM = 0x1F01;

    private static final String[] PROP_PARTITIONS = {
            "", "system.", "vendor.", "odm.", "product.", "bootimage.",
            "odm_dlkm.", "vendor_dlkm.", "system_ext."
    };

    private static volatile SpoofProfiles.Profile activeProfile =
            SpoofProfiles.getOrDefault(SpoofProfiles.DEFAULT_PROFILE_KEY);
    private static volatile Map<String, String> activePropertyOverrides =
            buildPropertyOverrides(activeProfile);
    private static volatile int peakHardwareModeId = -1;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if (SELF_PACKAGE.equals(lpparam.packageName)) {
            try {
                System.setProperty("mlbb.spoof.hook.active", "true");
            } catch (Throwable ignored) {
            }
            return;
        }
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            XposedBridge.log("[MLBBFpsBooster] Hooking scoped package: " + lpparam.packageName);
        }

        // Stage 1: Load initial profile from bridge files or XSharedPreferences before Context attach.
        updateActiveProfile(loadEarlyProfile());

        // Stage 2: Hook Application.attach(Context) to refresh profile via Settings.Global & ContentProvider.
        hookApplicationAttach();

        // Apply Build, SystemProperties, Settings, OpenGL GPU, Display Mode, and Surface Frame Pacing hooks.
        applyBuildSpoof(activeProfile);
        hookProperties(lpparam.classLoader);
        hookSettingsNames();
        hookOpenGlRenderer(lpparam.classLoader);
        hookDisplayAndFramePacing();

        XposedBridge.log("[MLBBFpsBooster] Deep spoof initialized -> " + activeProfile.model
                + " (" + activeProfile.socModel + " / " + activeProfile.gpuRenderer
                + " @ " + activeProfile.targetRefreshRate + "Hz) for " + lpparam.packageName);
    }

    private static synchronized void updateActiveProfile(SpoofProfiles.Profile profile) {
        if (profile == null) {
            return;
        }
        activeProfile = profile;
        activePropertyOverrides = buildPropertyOverrides(profile);
        applyBuildSpoof(profile);
    }

    private static SpoofProfiles.Profile loadEarlyProfile() {
        for (String path : SpoofProfiles.getBridgeFilePaths()) {
            try {
                File f = new File(path);
                if (f.exists() && f.canRead()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line = br.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            return SpoofProfiles.getOrDefault(line.trim());
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            XSharedPreferences prefs = new XSharedPreferences(SELF_PACKAGE, SpoofProfiles.PREFS_NAME);
            prefs.makeWorldReadable();
            prefs.reload();
            String selected = prefs.getString(SpoofProfiles.KEY_SELECTED_PROFILE, null);
            if (selected != null && !selected.trim().isEmpty()) {
                return SpoofProfiles.getOrDefault(selected.trim());
            }
        } catch (Throwable ignored) {
        }

        return SpoofProfiles.getOrDefault(SpoofProfiles.DEFAULT_PROFILE_KEY);
    }

    private static SpoofProfiles.Profile loadProfileFromContext(Context context) {
        if (context == null) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            try {
                String globalVal = Settings.Global.getString(
                        resolver, SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY);
                if (globalVal != null && !globalVal.trim().isEmpty()) {
                    return SpoofProfiles.getOrDefault(globalVal.trim());
                }
            } catch (Throwable ignored) {
            }

            Uri uri = Uri.parse("content://" + SpoofProfiles.PROVIDER_AUTHORITY + "/config");
            try {
                Bundle b = resolver.call(
                        uri, SpoofConfigProvider.METHOD_GET_PROFILE, null, null);
                if (b != null) {
                    String val = b.getString(SpoofProfiles.KEY_SELECTED_PROFILE);
                    if (val != null && !val.trim().isEmpty()) {
                        return SpoofProfiles.getOrDefault(val.trim());
                    }
                }
            } catch (Throwable ignored) {
            }

            try (Cursor c = resolver.query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    String val = c.getString(0);
                    if (val != null && !val.trim().isEmpty()) {
                        return SpoofProfiles.getOrDefault(val.trim());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void hookApplicationAttach() {
        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args != null && param.args.length > 0
                                    && param.args[0] instanceof Context) {
                                Context ctx = (Context) param.args[0];
                                SpoofProfiles.Profile resolved = loadProfileFromContext(ctx);
                                if (resolved != null) {
                                    updateActiveProfile(resolved);
                                    XposedBridge.log("[MLBBFpsBooster] Context profile synced -> "
                                            + resolved.model + " (" + resolved.gpuRenderer + ")");
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Application.attach hook error: " + t.getMessage());
        }
    }

    public static void applyBuildSpoof(SpoofProfiles.Profile profile) {
        try {
            XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", profile.manufacturer);
            XposedHelpers.setStaticObjectField(Build.class, "BRAND", profile.brand);
            XposedHelpers.setStaticObjectField(Build.class, "PRODUCT", profile.product);
            XposedHelpers.setStaticObjectField(Build.class, "DEVICE", profile.device);
            XposedHelpers.setStaticObjectField(Build.class, "MODEL", profile.model);
            XposedHelpers.setStaticObjectField(Build.class, "BOARD", profile.board);
            XposedHelpers.setStaticObjectField(Build.class, "HARDWARE", profile.hardware);
            XposedHelpers.setStaticObjectField(Build.class, "FINGERPRINT", profile.fingerprint);
            XposedHelpers.setStaticObjectField(Build.class, "DISPLAY", profile.model + "-user 15");
            XposedHelpers.setStaticObjectField(Build.class, "TAGS", "release-keys");
            XposedHelpers.setStaticObjectField(Build.class, "TYPE", "user");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.setStaticObjectField(Build.class, "SOC_MANUFACTURER", profile.socManufacturer);
                XposedHelpers.setStaticObjectField(Build.class, "SOC_MODEL", profile.socModel);
                XposedHelpers.setStaticObjectField(Build.class, "SKU", profile.model);
                XposedHelpers.setStaticObjectField(Build.class, "ODM_SKU", profile.model);
            }
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Failed to set Build fields: " + t.getMessage());
        }
    }

    public static Map<String, String> buildPropertyOverrides(SpoofProfiles.Profile p) {
        Map<String, String> map = new HashMap<>();
        for (String part : PROP_PARTITIONS) {
            map.put("ro.product." + part + "model", p.model);
            map.put("ro.product." + part + "brand", p.brand);
            map.put("ro.product." + part + "name", p.product);
            map.put("ro.product." + part + "device", p.device);
            map.put("ro.product." + part + "manufacturer", p.manufacturer);
            map.put("ro." + part + "build.fingerprint", p.fingerprint);
        }
        map.put("ro.build.fingerprint", p.fingerprint);
        map.put("ro.build.product", p.device);
        map.put("ro.build.device", p.device);
        map.put("ro.product.board", p.board);
        map.put("persist.sys.tran.device.name", p.marketName);
        map.put("persist.sys.device_name", p.marketName);
        map.put("ro.product.marketname", p.marketName);
        map.put("ro.vendor.product.marketname", p.marketName);
        map.put("ro.com.google.clientidbase", "android-" + p.brand.toLowerCase(Locale.US));
        map.put("ro.board.platform", p.hardware);
        map.put("ro.boot.hardware", p.hardware);
        map.put("ro.hardware", p.hardware);
        map.put("ro.hardware.chipname", p.socModel);
        map.put("ro.vendor.mediatek.platform", p.hardware);
        map.put("ro.mediatek.platform", p.hardware);
        map.put("ro.soc.manufacturer", p.socManufacturer);
        map.put("ro.soc.model", p.socModel);
        map.put("ro.vendor.soc.model", p.socModel);
        map.put("ro.vendor.soc.model.external_name", p.socModel);
        map.put("ro.vendor.soc.model.part_name", p.socModel);
        map.put("ro.surface_flinger.use_content_detection_for_refresh_rate", "false");
        map.put("ro.surface_flinger.set_idle_timer_ms", "0");
        map.put("ro.surface_flinger.set_touch_timer_ms", "0");
        return map;
    }

    public static String resolvePropertyOverride(
            String key, Map<String, String> exactOverrides, SpoofProfiles.Profile p) {
        if (key == null) {
            return null;
        }
        String exact = exactOverrides.get(key);
        if (exact != null) {
            return exact;
        }
        if (key.startsWith("ro.product.tr_") || key.startsWith("ro.tr_") || key.startsWith("ro.tran.")) {
            if (key.endsWith(".model")) {
                return p.model;
            }
            if (key.endsWith(".brand")) {
                return p.brand;
            }
            if (key.endsWith(".manufacturer")) {
                return p.manufacturer;
            }
            if (key.endsWith(".device")) {
                return p.device;
            }
            if (key.endsWith(".name") || key.endsWith(".marketname")) {
                return p.marketName;
            }
        }
        return null;
    }

    private static void hookProperties(ClassLoader classLoader) {
        XC_MethodHook propHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                    String spoofed = resolvePropertyOverride(
                            (String) param.args[0], activePropertyOverrides, activeProfile);
                    if (spoofed != null) {
                        param.setResult(spoofed);
                    }
                }
            }
        };

        try {
            Class<?> sysProps = XposedHelpers.findClass(
                    "android.os.SystemProperties", classLoader);
            XposedHelpers.findAndHookMethod(sysProps, "get", String.class, propHook);
            XposedHelpers.findAndHookMethod(sysProps, "get", String.class, String.class, propHook);
            try {
                XposedHelpers.findAndHookMethod(sysProps, "native_get", String.class, propHook);
                XposedHelpers.findAndHookMethod(
                        sysProps, "native_get", String.class, String.class, propHook);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] SystemProperties hook error: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class, propHook);
            XposedHelpers.findAndHookMethod(
                    System.class, "getProperty", String.class, String.class, propHook);
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] System.getProperty hook error: " + t.getMessage());
        }
    }

    private static void hookSettingsNames() {
        XC_MethodHook settingsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length >= 2 && param.args[1] instanceof String) {
                    String name = (String) param.args[1];
                    if ("device_name".equals(name) || "bluetooth_name".equals(name)) {
                        param.setResult(activeProfile.model);
                    } else if ("peak_refresh_rate".equals(name)
                            || "min_refresh_rate".equals(name)
                            || "user_refresh_rate".equals(name)) {
                        param.setResult(String.valueOf((int) activeProfile.targetRefreshRate));
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                    Settings.Global.class, "getString",
                    ContentResolver.class, String.class, settingsHook);
            XposedHelpers.findAndHookMethod(
                    Settings.Secure.class, "getString",
                    ContentResolver.class, String.class, settingsHook);
            XposedHelpers.findAndHookMethod(
                    Settings.System.class, "getString",
                    ContentResolver.class, String.class, settingsHook);
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Settings name hook error: " + t.getMessage());
        }
    }

    private static void hookOpenGlRenderer(ClassLoader classLoader) {
        XC_MethodHook glStringHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof Integer) {
                    int name = (Integer) param.args[0];
                    if (name == GL_VENDOR_ENUM) {
                        param.setResult(activeProfile.gpuVendor);
                    } else if (name == GL_RENDERER_ENUM) {
                        param.setResult(activeProfile.gpuRenderer);
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(GLES10.class, "glGetString", int.class, glStringHook);
        } catch (Throwable ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(GLES20.class, "glGetString", int.class, glStringHook);
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] GLES20.glGetString hook error: " + t.getMessage());
        }
        try {
            XposedHelpers.findAndHookMethod(GLES30.class, "glGetString", int.class, glStringHook);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> glImpl = XposedHelpers.findClass(
                    "com.google.android.gles_jni.GLImpl", classLoader);
            XposedHelpers.findAndHookMethod(glImpl, "glGetString", int.class, glStringHook);
        } catch (Throwable ignored) {
        }
    }

    private static float readRawModeRefreshRateField(Display.Mode mode) {
        try {
            return XposedHelpers.getFloatField(mode, "mRefreshRate");
        } catch (Throwable t) {
            return 60.0f;
        }
    }

    @SuppressWarnings("deprecation")
    private static Display getWindowDisplay(Activity activity, Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Display d = activity.getDisplay();
                if (d != null) {
                    return d;
                }
            } catch (Throwable ignored) {
            }
        }
        WindowManager wm = window.getWindowManager();
        return wm != null ? wm.getDefaultDisplay() : null;
    }

    private static void hookDisplayAndFramePacing() {
        // 1. Hook Display.getSupportedModes() to record the true peak hardware mode ID
        try {
            XposedHelpers.findAndHookMethod(
                    Display.class, "getSupportedModes", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof Display.Mode[])) {
                                return;
                            }
                            Display.Mode[] modes = (Display.Mode[]) result;
                            int bestId = -1;
                            float bestRawHz = -1.0f;
                            for (Display.Mode m : modes) {
                                float rawHz = readRawModeRefreshRateField(m);
                                if (rawHz > bestRawHz) {
                                    bestRawHz = rawHz;
                                    bestId = m.getModeId();
                                }
                            }
                            if (bestId != -1) {
                                peakHardwareModeId = bestId;
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }

        // 2. Hook Display.getRefreshRate()
        try {
            XposedHelpers.findAndHookMethod(
                    Display.class, "getRefreshRate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(activeProfile.targetRefreshRate);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Display.getRefreshRate hook error: " + t.getMessage());
        }

        // 3. Hook Display.Mode.getRefreshRate() so the peak hardware Mode ID reports targetRefreshRate (165Hz)
        try {
            XposedHelpers.findAndHookMethod(
                    Display.Mode.class, "getRefreshRate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Display.Mode)) {
                                return;
                            }
                            Object origResult = param.getResult();
                            float rawHz = origResult instanceof Float
                                    ? (Float) origResult
                                    : readRawModeRefreshRateField((Display.Mode) param.thisObject);
                            int modeId = ((Display.Mode) param.thisObject).getModeId();
                            if ((peakHardwareModeId != -1 && modeId == peakHardwareModeId)
                                    || rawHz >= 119.0f) {
                                param.setResult(Math.max(rawHz, activeProfile.targetRefreshRate));
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Display.Mode.getRefreshRate hook error: " + t.getMessage());
        }

        // 4. Hook Surface.setFrameRate() on Android 11+ (API 30+) to prevent 60Hz down-throttling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XC_MethodHook surfaceFrameRateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length >= 1
                            && param.args[0] instanceof Float) {
                        float requested = (Float) param.args[0];
                        if (requested > 0.0f && requested < activeProfile.targetRefreshRate) {
                            param.args[0] = activeProfile.targetRefreshRate;
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(
                        Surface.class, "setFrameRate",
                        float.class, int.class, surfaceFrameRateHook);
            } catch (Throwable ignored) {
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    XposedHelpers.findAndHookMethod(
                            Surface.class, "setFrameRate",
                            float.class, int.class, int.class, surfaceFrameRateHook);
                } catch (Throwable ignored) {
                }
            }
        }

        // 5. Hook Activity.onResume() to lock Window preferredRefreshRate & preferredDisplayModeId to peak hardware mode
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class, "onResume", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Activity)) {
                                return;
                            }
                            Activity activity = (Activity) param.thisObject;
                            Window window = activity.getWindow();
                            if (window == null) {
                                return;
                            }
                            Display display = getWindowDisplay(activity, window);
                            if (display == null) {
                                return;
                            }
                            Display.Mode[] modes = display.getSupportedModes();
                            Display.Mode bestPhysicalMode = null;
                            float bestRawHz = -1.0f;
                            for (Display.Mode m : modes) {
                                float rawHz = readRawModeRefreshRateField(m);
                                if (bestPhysicalMode == null || rawHz > bestRawHz) {
                                    bestRawHz = rawHz;
                                    bestPhysicalMode = m;
                                }
                            }
                            WindowManager.LayoutParams lp = window.getAttributes();
                            if (bestPhysicalMode != null) {
                                lp.preferredDisplayModeId = bestPhysicalMode.getModeId();
                            }
                            lp.preferredRefreshRate = activeProfile.targetRefreshRate;
                            window.setAttributes(lp);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }
}
