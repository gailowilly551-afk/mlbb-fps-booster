package com.fpsbooster.mlbb.xposed;

import android.os.Environment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared catalog of whitelisted 165Hz flagship gaming device profiles used by the UI, Shizuku engine,
 * ContentProvider bridge, and Xposed/LSPatch hook.
 */
public final class SpoofProfiles {

    public static final String PREFS_NAME = "spoof_prefs";
    public static final String KEY_SELECTED_PROFILE = "selected_profile";
    public static final String KEY_DOWNSCALE_FACTOR = "downscale_factor";
    public static final String DEFAULT_PROFILE_KEY = "ASUS_AI2401_A";

    public static final String GLOBAL_SETTING_PROFILE_KEY = "mlbb_spoof_profile";
    public static final String PROVIDER_AUTHORITY = "com.fpsbooster.mlbb.provider";

    /** Returns external/tmp config file paths readable by com.mobile.legends without special permissions. */
    public static String[] getBridgeFilePaths() {
        String extRoot;
        try {
            extRoot = Environment.getExternalStorageDirectory().getPath();
        } catch (Throwable t) {
            extRoot = "/storage/emulated/0";
        }
        return new String[]{
                extRoot + "/Android/data/com.mobile.legends/files/.mlbb_spoof_profile",
                extRoot + "/Android/media/com.mobile.legends/.mlbb_spoof_profile",
                "/storage/emulated/0/Android/data/com.mobile.legends/files/.mlbb_spoof_profile",
                "/storage/emulated/0/Android/media/com.mobile.legends/.mlbb_spoof_profile",
                "/data/local/tmp/mlbb_spoof_profile.txt"
        };
    }

    /** Immutable device identity values applied to Build, SystemProperties, GLES, and Settings. */
    public static final class Profile {
        public final String id;
        public final String displayName;
        public final String marketName;
        public final String manufacturer;
        public final String brand;
        public final String product;
        public final String device;
        public final String model;
        public final String board;
        public final String hardware;
        public final String socManufacturer;
        public final String socModel;
        public final String gpuVendor;
        public final String gpuRenderer;
        public final String fingerprint;
        public final float targetRefreshRate;

        public Profile(
                String id,
                String displayName,
                String marketName,
                String manufacturer,
                String brand,
                String product,
                String device,
                String model,
                String board,
                String hardware,
                String socManufacturer,
                String socModel,
                String gpuVendor,
                String gpuRenderer,
                String fingerprint,
                float targetRefreshRate) {
            this.id = id;
            this.displayName = displayName;
            this.marketName = marketName;
            this.manufacturer = manufacturer;
            this.brand = brand;
            this.product = product;
            this.device = device;
            this.model = model;
            this.board = board;
            this.hardware = hardware;
            this.socManufacturer = socManufacturer;
            this.socModel = socModel;
            this.gpuVendor = gpuVendor;
            this.gpuRenderer = gpuRenderer;
            this.fingerprint = fingerprint;
            this.targetRefreshRate = targetRefreshRate;
        }
    }

    private static final Map<String, Profile> PROFILES;

    static {
        Map<String, Profile> map = new LinkedHashMap<>();
        map.put("ASUS_AI2401_A", new Profile(
                "ASUS_AI2401_A",
                "ASUS_AI2401_A (ROG Phone 8 Pro — 165Hz / Adreno 750)",
                "ROG Phone 8 Pro",
                "ASUS",
                "asus",
                "WW_AI2401",
                "AI2401",
                "ASUS_AI2401_A",
                "pineapple",
                "qcom",
                "Qualcomm",
                "SM8650",
                "Qualcomm",
                "Adreno (TM) 750",
                "asus/WW_AI2401/AI2401:14/UKQ1.230804.001/34.1420.1420.407:user/release-keys",
                165.0f));
        map.put("ASUS_AI2501", new Profile(
                "ASUS_AI2501",
                "ASUS_AI2501 (ROG Phone 9 Pro — 165Hz / Adreno 830)",
                "ROG Phone 9 Pro",
                "ASUS",
                "asus",
                "WW_AI2501",
                "AI2501",
                "ASUS_AI2501",
                "sun",
                "qcom",
                "Qualcomm",
                "SM8750",
                "Qualcomm",
                "Adreno (TM) 830",
                "asus/WW_AI2501/AI2501:15/AQ3A.240812.002/35.1210.1210.258:user/release-keys",
                165.0f));
        map.put("NX769J", new Profile(
                "NX769J",
                "NX769J (RedMagic 9 Pro — 165Hz / Adreno 750)",
                "REDMAGIC 9 Pro",
                "nubia",
                "nubia",
                "NX769J",
                "NX769J",
                "NX769J",
                "pineapple",
                "qcom",
                "Qualcomm",
                "SM8650",
                "Qualcomm",
                "Adreno (TM) 750",
                "nubia/NX769J/NX769J:14/UKQ1.230804.001/20240515.172512:user/release-keys",
                165.0f));
        PROFILES = Collections.unmodifiableMap(map);
    }

    private SpoofProfiles() {
    }

    public static Map<String, Profile> all() {
        return PROFILES;
    }

    public static Profile getOrDefault(String id) {
        Profile profile = id != null ? PROFILES.get(id.trim()) : null;
        return profile != null ? profile : PROFILES.get(DEFAULT_PROFILE_KEY);
    }
}
