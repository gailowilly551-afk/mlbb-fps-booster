package com.fpsbooster.mlbb.xposed;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared catalog of whitelisted flagship device profiles used by both the UI and Xposed hook.
 */
public final class SpoofProfiles {

    public static final String PREFS_NAME = "spoof_prefs";
    public static final String KEY_SELECTED_PROFILE = "selected_profile";
    public static final String DEFAULT_PROFILE_KEY = "ASUS_AI2401_A";

    /** Immutable device identity values applied to Build, SystemProperties, and Settings. */
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
                String socModel) {
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
        }
    }

    private static final Map<String, Profile> PROFILES;

    static {
        Map<String, Profile> map = new LinkedHashMap<>();
        map.put("ASUS_AI2401_A", new Profile(
                "ASUS_AI2401_A",
                "ROG Phone 8 Pro (ASUS_AI2401_A - 165Hz)",
                "ROG Phone 8 Pro",
                "ASUS",
                "asus",
                "WW_AI2401",
                "AI2401",
                "ASUS_AI2401_A",
                "pineapple",
                "qcom",
                "Qualcomm",
                "SM8650"));
        map.put("NX769J", new Profile(
                "NX769J",
                "RedMagic 9 Pro (NX769J - Gaming 144/165Hz)",
                "REDMAGIC 9 Pro",
                "nubia",
                "nubia",
                "NX769J",
                "NX769J",
                "NX769J",
                "pineapple",
                "qcom",
                "Qualcomm",
                "SM8650"));
        map.put("PJZ110", new Profile(
                "PJZ110",
                "OnePlus 13 (PJZ110 - Snapdragon 8 Elite)",
                "OnePlus 13",
                "OnePlus",
                "OnePlus",
                "PJZ110",
                "OP5D3DL1",
                "PJZ110",
                "sun",
                "qcom",
                "Qualcomm",
                "SM8750"));
        map.put("Infinix X6871", new Profile(
                "Infinix X6871",
                "Infinix GT 20 Pro (Infinix X6871 - Transsion 144Hz)",
                "Infinix GT 20 Pro",
                "INFINIX",
                "Infinix",
                "X6871-OP",
                "Infinix-X6871",
                "Infinix X6871",
                "Infinix-X6871",
                "mt6896",
                "MediaTek",
                "MT6896"));
        PROFILES = Collections.unmodifiableMap(map);
    }

    private SpoofProfiles() {
    }

    public static Map<String, Profile> all() {
        return PROFILES;
    }

    public static Profile getOrDefault(String id) {
        Profile profile = id != null ? PROFILES.get(id) : null;
        return profile != null ? profile : PROFILES.get(DEFAULT_PROFILE_KEY);
    }
}
