package com.fpsbooster.mlbb.xposed;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;

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
 * Xposed / LSPatch entry point that scrubs Transsion/MT6878 properties, spoofs device identity,
 * and reports 165 Hz on Display.getRefreshRate() for scoped packages.
 */
public class DeviceSpoofHook implements IXposedHookLoadPackage {

    private static final String SELF_PACKAGE = "com.fpsbooster.mlbb";
    private static final String TARGET_PACKAGE = "com.mobile.legends";
    private static final String[] PROP_PARTITIONS = {
            "", "system.", "vendor.", "odm.", "product.", "bootimage.", "odm_dlkm.", "vendor_dlkm."
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if (SELF_PACKAGE.equals(lpparam.packageName)) {
            return;
        }
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            XposedBridge.log("[MLBBFpsBooster] Hooking scoped package: " + lpparam.packageName);
        }

        SpoofProfiles.Profile profile = loadSelectedProfile();
        Map<String, String> propertyOverrides = buildPropertyOverrides(profile);

        applyBuildSpoof(profile);
        hookProperties(lpparam.classLoader, propertyOverrides, profile);
        hookSettingsNames(profile);
        hookDisplayRefreshRate();

        XposedBridge.log("[MLBBFpsBooster] Transsion spoof active -> " + profile.model
                + " (" + profile.socModel + ") for " + lpparam.packageName);
    }

    private static SpoofProfiles.Profile loadSelectedProfile() {
        try {
            XSharedPreferences prefs = new XSharedPreferences(SELF_PACKAGE, SpoofProfiles.PREFS_NAME);
            prefs.makeWorldReadable();
            prefs.reload();
            String selected = prefs.getString(
                    SpoofProfiles.KEY_SELECTED_PROFILE,
                    SpoofProfiles.DEFAULT_PROFILE_KEY);
            return SpoofProfiles.getOrDefault(selected);
        } catch (Throwable t) {
            return SpoofProfiles.getOrDefault(SpoofProfiles.DEFAULT_PROFILE_KEY);
        }
    }

    private static void applyBuildSpoof(SpoofProfiles.Profile profile) {
        try {
            XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", profile.manufacturer);
            XposedHelpers.setStaticObjectField(Build.class, "BRAND", profile.brand);
            XposedHelpers.setStaticObjectField(Build.class, "PRODUCT", profile.product);
            XposedHelpers.setStaticObjectField(Build.class, "DEVICE", profile.device);
            XposedHelpers.setStaticObjectField(Build.class, "MODEL", profile.model);
            XposedHelpers.setStaticObjectField(Build.class, "BOARD", profile.board);
            XposedHelpers.setStaticObjectField(Build.class, "HARDWARE", profile.hardware);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.setStaticObjectField(Build.class, "SOC_MANUFACTURER", profile.socManufacturer);
                XposedHelpers.setStaticObjectField(Build.class, "SOC_MODEL", profile.socModel);
            }
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Failed to set Build fields: " + t.getMessage());
        }
    }

    private static Map<String, String> buildPropertyOverrides(SpoofProfiles.Profile p) {
        Map<String, String> map = new HashMap<>();
        for (String part : PROP_PARTITIONS) {
            map.put("ro.product." + part + "model", p.model);
            map.put("ro.product." + part + "brand", p.brand);
            map.put("ro.product." + part + "name", p.product);
            map.put("ro.product." + part + "device", p.device);
            map.put("ro.product." + part + "manufacturer", p.manufacturer);
        }
        map.put("ro.product.board", p.board);
        map.put("persist.sys.tran.device.name", p.marketName);
        map.put("ro.product.marketname", p.marketName);
        map.put("ro.com.google.clientidbase", "android-" + p.brand.toLowerCase(Locale.US));
        map.put("ro.board.platform", p.hardware);
        map.put("ro.boot.hardware", p.hardware);
        map.put("ro.hardware", p.hardware);
        map.put("ro.vendor.mediatek.platform", p.hardware);
        map.put("ro.soc.model", p.socModel);
        map.put("ro.vendor.soc.model", p.socModel);
        map.put("ro.vendor.soc.model.external_name", p.socModel);
        map.put("ro.vendor.soc.model.part_name", p.socModel);
        return map;
    }

    private static String resolvePropertyOverride(
            String key, Map<String, String> exactOverrides, SpoofProfiles.Profile p) {
        String exact = exactOverrides.get(key);
        if (exact != null) {
            return exact;
        }
        if (key.startsWith("ro.product.tr_")) {
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
            if (key.endsWith(".name")) {
                return p.product;
            }
        }
        return null;
    }

    private static void hookProperties(
            ClassLoader classLoader,
            Map<String, String> overrides,
            SpoofProfiles.Profile profile) {
        XC_MethodHook propHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                    String spoofed = resolvePropertyOverride(
                            (String) param.args[0], overrides, profile);
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

    private static void hookSettingsNames(SpoofProfiles.Profile profile) {
        XC_MethodHook settingsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length >= 2 && param.args[1] instanceof String) {
                    String name = (String) param.args[1];
                    if ("device_name".equals(name) || "bluetooth_name".equals(name)) {
                        param.setResult(profile.model);
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
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Settings name hook error: " + t.getMessage());
        }
    }

    private static void hookDisplayRefreshRate() {
        try {
            XposedHelpers.findAndHookMethod(
                    Display.class, "getRefreshRate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(165.0f);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[MLBBFpsBooster] Display.getRefreshRate hook error: " + t.getMessage());
        }
    }
}
