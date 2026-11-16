package com.fpsbooster.mlbb.shizuku;

import android.content.pm.PackageManager;

import com.fpsbooster.mlbb.xposed.SpoofProfiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import rikka.shizuku.Shizuku;

/**
 * Executes privileged shell commands via the Shizuku binder (UID 2000) without root access.
 */
public final class ShizukuShellRunner {

    public static final String TARGET_PACKAGE = "com.mobile.legends";
    private static final String DEFAULT_TECNO_DEVICE_NAME = "TECNO CAMON 40 Pro 5G";

    /** Represents the highest physical Display.Mode detected on the device hardware. */
    public static final class DisplayModeSpec {
        public final int modeId;
        public final int width;
        public final int height;
        public final float exactRefreshRate;
        public final int roundedHz;

        public DisplayModeSpec(int modeId, int width, int height, float exactRefreshRate) {
            this.modeId = modeId;
            this.width = width;
            this.height = height;
            this.exactRefreshRate = exactRefreshRate;
            this.roundedHz = Math.round(exactRefreshRate);
        }

        public static DisplayModeSpec fallback144() {
            return new DisplayModeSpec(1, 1080, 2436, 144.00002f);
        }
    }

    private ShizukuShellRunner() {
    }

    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasPermission() {
        if (!isBinderAlive()) {
            return false;
        }
        try {
            if (Shizuku.isPreV11()) {
                return false;
            }
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Immediately syncs the chosen SpoofProfile to Settings.Global and MLBB-readable external/tmp
     * bridge files so the LSPatch/Xposed hook inside com.mobile.legends always reads the live profile.
     */
    public static String syncProfileBridge(SpoofProfiles.Profile profile) {
        SpoofProfiles.Profile p = profile != null
                ? profile
                : SpoofProfiles.getOrDefault(SpoofProfiles.DEFAULT_PROFILE_KEY);
        String safeId = p.id.replace("'", "");
        String safeModel = p.model.replace("'", "");

        String cmd = String.join("\n",
                "settings put global " + SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY + " '" + safeId + "'",
                "settings put global device_name '" + safeModel + "'",
                "settings put secure bluetooth_name '" + safeModel + "'",
                "mkdir -p /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files 2>/dev/null || true",
                "echo '" + safeId + "' > /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files/.mlbb_spoof_profile 2>/dev/null || true",
                "chmod 644 /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files/.mlbb_spoof_profile 2>/dev/null || true",
                "mkdir -p /storage/emulated/0/Android/media/" + TARGET_PACKAGE + " 2>/dev/null || true",
                "echo '" + safeId + "' > /storage/emulated/0/Android/media/" + TARGET_PACKAGE + "/.mlbb_spoof_profile 2>/dev/null || true",
                "chmod 644 /storage/emulated/0/Android/media/" + TARGET_PACKAGE + "/.mlbb_spoof_profile 2>/dev/null || true",
                "echo '" + safeId + "' > /data/local/tmp/mlbb_spoof_profile.txt 2>/dev/null || true",
                "chmod 644 /data/local/tmp/mlbb_spoof_profile.txt 2>/dev/null || true",
                "echo \"Synced spoof profile bridge -> " + safeId + " (" + safeModel + ")\"");
        return executeShell(cmd);
    }

    /**
     * Full All-in-One Shizuku Spoof + Dynamic Native Display Lock + 165/Peak Game Overlay + PowerHAL Boost.
     */
    public static String applyFullSpoofAndUnlock(
            SpoofProfiles.Profile profile,
            DisplayModeSpec hwPeak,
            String downscaleRatio) {
        SpoofProfiles.Profile p = profile != null
                ? profile
                : SpoofProfiles.getOrDefault(SpoofProfiles.DEFAULT_PROFILE_KEY);
        DisplayModeSpec spec = hwPeak != null ? hwPeak : DisplayModeSpec.fallback144();

        String safeId = p.id.replace("'", "");
        String safeModel = p.model.replace("'", "");
        int nativeHz = spec.roundedHz >= 90 ? spec.roundedHz : 144;
        int targetGameFps = Math.max(nativeHz, Math.round(p.targetRefreshRate));
        String ds = (downscaleRatio == null || downscaleRatio.isEmpty() || "1.0".equals(downscaleRatio))
                ? "disable"
                : downscaleRatio;
        String overlayDs = "disable".equals(ds) ? "1.0" : ds;

        String exactModeCmd = String.format(Locale.US,
                "cmd display set-user-preferred-display-mode %d %d %s 0",
                spec.width, spec.height, Float.toString(spec.exactRefreshRate));

        String cmd = String.join("\n",
                "# 1. Sync Spoof Profile Bridge (Settings.Global + MLBB Data Dir + /data/local/tmp)",
                "settings put global " + SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY + " '" + safeId + "'",
                "settings put global device_name '" + safeModel + "'",
                "settings put secure bluetooth_name '" + safeModel + "'",
                "mkdir -p /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files 2>/dev/null || true",
                "echo '" + safeId + "' > /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files/.mlbb_spoof_profile 2>/dev/null || true",
                "chmod 644 /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files/.mlbb_spoof_profile 2>/dev/null || true",
                "mkdir -p /storage/emulated/0/Android/media/" + TARGET_PACKAGE + " 2>/dev/null || true",
                "echo '" + safeId + "' > /storage/emulated/0/Android/media/" + TARGET_PACKAGE + "/.mlbb_spoof_profile 2>/dev/null || true",
                "chmod 644 /storage/emulated/0/Android/media/" + TARGET_PACKAGE + "/.mlbb_spoof_profile 2>/dev/null || true",
                "echo '" + safeId + "' > /data/local/tmp/mlbb_spoof_profile.txt 2>/dev/null || true",
                "chmod 644 /data/local/tmp/mlbb_spoof_profile.txt 2>/dev/null || true",
                "",
                "# 2. Lock Exact Hardware Peak Display Mode (" + spec.width + "x" + spec.height + " @ " + spec.exactRefreshRate + " Hz)",
                exactModeCmd,
                "settings put system peak_refresh_rate " + nativeHz + ".0",
                "settings put system min_refresh_rate " + nativeHz + ".0",
                "settings put system user_refresh_rate " + nativeHz,
                "settings put secure user_refresh_rate " + nativeHz,
                "settings put global user_refresh_rate " + nativeHz,
                "",
                "# 3. Transsion (HiOS / XOS) & Multi-OEM Refresh Rate + Thermal Bypass (Valid Native Hz = " + nativeHz + ")",
                "settings put system tran_refresh_mode " + nativeHz,
                "settings put system tran_refresh_rate " + nativeHz,
                "settings put system tran_screen_refresh_rate_mode " + nativeHz,
                "settings put system tran_need_recovery_refresh_mode " + nativeHz,
                "settings put system tran_need_recovery_refresh_rate " + nativeHz,
                "settings put system last_tran_refresh_mode_in_refresh_setting " + nativeHz,
                "settings put system tecno_refresh_rate " + nativeHz,
                "settings put global tecno_display_refresh_rate " + nativeHz,
                "settings put global tran_high_temperature_limit_refresh_rate 0",
                "settings put system tran_fps_stabilization 0",
                "settings put system tran_low_battery_mode 0",
                "settings put system tran_power_save_mode 0",
                "settings put global tran_smart_power_save 0",
                "settings put global transsion_game_acceleration 1",
                "settings put global transsion_game_mode 1",
                "settings put secure transsion_game_function_mode 2",
                "settings put system miui_refresh_rate " + nativeHz,
                "settings put secure miui_refresh_rate " + nativeHz,
                "settings put secure oplus_customize_screen_refresh_rate " + nativeHz,
                "",
                "# 4. Android 12-16 GameManager Overlay + PowerHAL Fixed Performance + Thermal Override",
                "device_config put game_overlay " + TARGET_PACKAGE
                        + " mode=2,downscaleFactor=" + overlayDs + ",fps=" + targetGameFps + ",loadingBoost=999999999"
                        + ":mode=3,downscaleFactor=0.9,fps=" + nativeHz + " 2>/dev/null || true",
                "cmd game mode 2 " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game set --mode 2 --downscale " + ds + " --fps " + targetGameFps + " " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd power set-fixed-performance-mode-enabled true 2>/dev/null || true",
                "cmd power set-adaptive-power-saver-enabled false 2>/dev/null || true",
                "cmd power set-mode 0 2>/dev/null || true",
                "cmd thermalservice override-status 0 2>/dev/null || true",
                "",
                "# 5. SurfaceFlinger & HWUI Low-Latency Frame Pacing Properties",
                "setprop debug.sf.latch_unsignaled 1 2>/dev/null || true",
                "setprop debug.sf.disable_backpressure 1 2>/dev/null || true",
                "setprop debug.hwui.use_hint_manager true 2>/dev/null || true",
                "",
                "echo '--- Active Spoof & Display Verification ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "echo \"mlbb_spoof_profile=$(settings get global " + SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY + ")\"",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate) | min_refresh_rate=$(settings get system min_refresh_rate)\"",
                "echo \"tran_refresh_mode=$(settings get system tran_refresh_mode) | recovery=$(settings get system tran_need_recovery_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game list-configs " + TARGET_PACKAGE + " 2>/dev/null || true");
        return executeShell(cmd);
    }

    /**
     * Locks the exact native hardware peak refresh rate (e.g. 144Hz or 120Hz) and sets GameManager to match.
     */
    public static String applyNativeHardwarePeak(DisplayModeSpec hwPeak, String downscaleRatio) {
        DisplayModeSpec spec = hwPeak != null ? hwPeak : DisplayModeSpec.fallback144();
        int nativeHz = spec.roundedHz >= 90 ? spec.roundedHz : 144;
        String ds = (downscaleRatio == null || downscaleRatio.isEmpty() || "1.0".equals(downscaleRatio))
                ? "disable"
                : downscaleRatio;
        String overlayDs = "disable".equals(ds) ? "1.0" : ds;

        String exactModeCmd = String.format(Locale.US,
                "cmd display set-user-preferred-display-mode %d %d %s 0",
                spec.width, spec.height, Float.toString(spec.exactRefreshRate));

        String cmd = String.join("\n",
                exactModeCmd,
                "settings put system peak_refresh_rate " + nativeHz + ".0",
                "settings put system min_refresh_rate " + nativeHz + ".0",
                "settings put system user_refresh_rate " + nativeHz,
                "settings put secure user_refresh_rate " + nativeHz,
                "settings put system tran_refresh_mode " + nativeHz,
                "settings put system tran_need_recovery_refresh_rate " + nativeHz,
                "settings put global tran_high_temperature_limit_refresh_rate 0",
                "settings put system tran_fps_stabilization 0",
                "device_config put game_overlay " + TARGET_PACKAGE
                        + " mode=2,downscaleFactor=" + overlayDs + ",fps=" + nativeHz + ",loadingBoost=999999999 2>/dev/null || true",
                "cmd game mode 2 " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game set --mode 2 --downscale " + ds + " --fps " + nativeHz + " " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd power set-fixed-performance-mode-enabled true 2>/dev/null || true",
                "cmd thermalservice override-status 0 2>/dev/null || true",
                "setprop debug.sf.latch_unsignaled 1 2>/dev/null || true",
                "setprop debug.sf.disable_backpressure 1 2>/dev/null || true",
                "echo '--- Hardware Peak Lock Verification (" + nativeHz + "Hz) ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game list-configs " + TARGET_PACKAGE + " 2>/dev/null || true");
        return executeShell(cmd);
    }

    /**
     * Compiles MLBB DEX code with AOT 'speed' profile, kills background apps, and locks PowerHAL clocks
     * to eliminate 5v5 teamfight frame drops and JIT stutter.
     */
    public static String applyPerformanceBoostAndCompile() {
        String cmd = String.join("\n",
                "echo '[1/4] Locking PowerHAL Fixed Performance Mode & Thermal Override...'",
                "cmd power set-fixed-performance-mode-enabled true 2>/dev/null || true",
                "cmd power set-adaptive-power-saver-enabled false 2>/dev/null || true",
                "cmd power set-mode 0 2>/dev/null || true",
                "cmd thermalservice override-status 0 2>/dev/null || true",
                "setprop debug.sf.latch_unsignaled 1 2>/dev/null || true",
                "setprop debug.sf.disable_backpressure 1 2>/dev/null || true",
                "setprop debug.hwui.use_hint_manager true 2>/dev/null || true",
                "echo '[2/4] Trimming background cached processes (am kill-all)...'",
                "am kill-all 2>/dev/null || true",
                "echo '[3/4] Running AOT DEX Speed Compilation on " + TARGET_PACKAGE + " (eliminates JIT lag in clashes)...'",
                "cmd package compile -m speed -f " + TARGET_PACKAGE + " 2>/dev/null || true",
                "echo '[4/4] Done! MLBB is AOT-compiled and CPU/GPU clocks are locked for sustained FPS.'");
        return executeShell(cmd);
    }

    /**
     * Applies legal Android GameManager resolution scaling to reduce GPU load during heavy 5v5 fights.
     */
    public static String applyDownscalePreset(String downscaleRatio, int targetFps) {
        int fps = targetFps >= 90 ? targetFps : 144;
        String ds = (downscaleRatio == null || downscaleRatio.isEmpty() || "1.0".equals(downscaleRatio))
                ? "disable"
                : downscaleRatio;
        String overlayDs = "disable".equals(ds) ? "1.0" : ds;

        String cmd = String.join("\n",
                "device_config put game_overlay " + TARGET_PACKAGE
                        + " mode=2,downscaleFactor=" + overlayDs + ",fps=" + fps + ",loadingBoost=999999999 2>/dev/null || true",
                "cmd game mode 2 " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game set --mode 2 --downscale " + ds + " --fps " + fps + " " + TARGET_PACKAGE + " 2>/dev/null || true",
                "echo '--- Resolution Scaling Updated (downscale=" + ds + ", fps=" + fps + ") ---'",
                "cmd game list-configs " + TARGET_PACKAGE + " 2>/dev/null || true");
        return executeShell(cmd);
    }

    /**
     * Reads current display config, spoof settings, GameManager state, and real MLBB frame rendering stats.
     */
    public static String readCurrentConfigAndRealFps() {
        String cmd = String.join("\n",
                "echo '=== 1. DISPLAY & SPOOF STATE ==='",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "echo \"mlbb_spoof_profile=$(settings get global " + SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY + ")\"",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate) | min_refresh_rate=$(settings get system min_refresh_rate)\"",
                "echo \"tran_refresh_mode=$(settings get system tran_refresh_mode) | recovery=$(settings get system tran_need_recovery_refresh_rate)\"",
                "echo ''",
                "echo '=== 2. GAMEMANAGER INTERVENTION ==='",
                "cmd game list-modes " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game list-configs " + TARGET_PACKAGE + " 2>/dev/null || true",
                "echo ''",
                "echo '=== 3. MLBB REAL RENDERED FRAME STATS (gfxinfo) ==='",
                "dumpsys gfxinfo " + TARGET_PACKAGE + " 2>/dev/null | grep -E 'Total frames|Janky frames|50th|90th|95th|99th' | head -n 12 || echo 'MLBB has no active rendered frames yet (open MLBB to measure live frames).'");
        return executeShell(cmd);
    }

    public static String resetToDefault(String originalHardwareModel) {
        String fallbackModel = (originalHardwareModel == null || originalHardwareModel.trim().isEmpty())
                ? DEFAULT_TECNO_DEVICE_NAME
                : originalHardwareModel.replace("'", "");
        String cmd = String.join("\n",
                "REAL_NAME=$(getprop ro.product.marketname)",
                "[ -z \"$REAL_NAME\" ] && REAL_NAME=$(getprop ro.product.model)",
                "[ -z \"$REAL_NAME\" ] && REAL_NAME='" + fallbackModel + "'",
                "cmd display clear-user-preferred-display-mode 0",
                "settings delete system peak_refresh_rate",
                "settings delete system min_refresh_rate",
                "settings delete global " + SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY,
                "settings put global device_name \"$REAL_NAME\"",
                "settings put secure bluetooth_name \"$REAL_NAME\"",
                "rm -f /storage/emulated/0/Android/data/" + TARGET_PACKAGE + "/files/.mlbb_spoof_profile 2>/dev/null || true",
                "rm -f /storage/emulated/0/Android/media/" + TARGET_PACKAGE + "/.mlbb_spoof_profile 2>/dev/null || true",
                "rm -f /data/local/tmp/mlbb_spoof_profile.txt 2>/dev/null || true",
                "device_config delete game_overlay " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game reset " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd game mode standard " + TARGET_PACKAGE + " 2>/dev/null || true",
                "cmd power set-fixed-performance-mode-enabled false 2>/dev/null || true",
                "cmd thermalservice reset 2>/dev/null || true",
                "echo '--- Reset Complete ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "cmd game list-modes " + TARGET_PACKAGE + " 2>/dev/null || true");
        return executeShell(cmd);
    }

    /**
     * Runs a shell script via Shizuku.newProcess using reflection for Shizuku API 13.x compatibility.
     */
    public static String executeShell(String script) {
        if (!isBinderAlive()) {
            return "[ERROR] Shizuku service is not running. Start Shizuku via Wireless Debugging or ADB first.";
        }
        if (!hasPermission()) {
            return "[ERROR] Shizuku permission not granted. Tap 'Request Shizuku Permission' first.";
        }

        Process process = null;
        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod(
                    "newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);

            process = (Process) newProcessMethod.invoke(
                    null,
                    new String[]{"sh", "-c", script + " 2>&1"},
                    null,
                    null);

            if (process == null) {
                return "[ERROR] Shizuku returned a null process.";
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();
            output.append("[exit code: ").append(exitCode).append(']');
            return output.toString().trim();
        } catch (Throwable t) {
            return "[EXCEPTION] " + t.getClass().getSimpleName() + ": " + t.getMessage();
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
