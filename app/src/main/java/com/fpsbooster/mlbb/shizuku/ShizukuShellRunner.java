package com.fpsbooster.mlbb.shizuku;

import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

/**
 * Executes privileged shell commands via the Shizuku binder without root access.
 */
public final class ShizukuShellRunner {

    private static final String TARGET_PACKAGE = "com.mobile.legends";
    private static final String DEFAULT_TECNO_DEVICE_NAME = "TECNO CAMON 40 Pro 5G";

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

    public static String applyTranssionSpoofAnd165Unlock(String targetModel) {
        String safeModel = (targetModel == null || targetModel.trim().isEmpty())
                ? "ASUS_AI2401_A"
                : targetModel.replace("'", "");
        String cmd = String.join("\n",
                "cmd display set-user-preferred-display-mode 1080 2436 144.00002 0",
                "settings put system peak_refresh_rate 165.0",
                "settings put system min_refresh_rate 144.0",
                "settings put system user_refresh_rate 165",
                "settings put secure user_refresh_rate 165",
                "settings put global user_refresh_rate 165",
                "settings put system tran_refresh_mode 165",
                "settings put system tran_refresh_rate 165",
                "settings put system tran_screen_refresh_rate_mode 165",
                "settings put system tran_need_recovery_refresh_mode 165",
                "settings put system tran_need_recovery_refresh_rate 165",
                "settings put system last_tran_refresh_mode_in_refresh_setting 165",
                "settings put system tecno_refresh_rate 165",
                "settings put global tecno_display_refresh_rate 165",
                "settings put global tran_high_temperature_limit_refresh_rate 0",
                "settings put system tran_fps_stabilization 0",
                "settings put system tran_low_battery_mode 0",
                "settings put system tran_power_save_mode 0",
                "settings put global tran_smart_power_save 0",
                "settings put global transsion_game_acceleration 1",
                "settings put global transsion_game_mode 1",
                "settings put secure transsion_game_function_mode 2",
                "settings put global device_name '" + safeModel + "'",
                "settings put secure bluetooth_name '" + safeModel + "'",
                "cmd game set --mode 2 --fps 165 " + TARGET_PACKAGE,
                "cmd game set --downscale disable --fps 165 " + TARGET_PACKAGE,
                "cmd game mode performance " + TARGET_PACKAGE,
                "echo '--- Transsion Spoof & Bypass Verification ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "echo \"tran_refresh_mode=$(settings get system tran_refresh_mode)\"",
                "echo \"tran_need_recovery_refresh_rate=$(settings get system tran_need_recovery_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE,
                "cmd game list-configs " + TARGET_PACKAGE);
        return executeShell(cmd);
    }

    public static String apply165FpsCommands() {
        String cmd = String.join("\n",
                "settings put system peak_refresh_rate 165.0",
                "settings put system min_refresh_rate 165.0",
                "cmd game set --mode 2 --fps 165 " + TARGET_PACKAGE,
                "cmd game set --downscale disable --fps 165 " + TARGET_PACKAGE,
                "cmd game mode performance " + TARGET_PACKAGE,
                "echo '--- Verification ---'",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate)\"",
                "echo \"min_refresh_rate=$(settings get system min_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE,
                "cmd game list-configs " + TARGET_PACKAGE);
        return executeShell(cmd);
    }

    public static String apply144HzHardwarePeak() {
        String cmd = String.join("\n",
                "cmd display set-user-preferred-display-mode 1080 2436 144.00002 0",
                "settings put system peak_refresh_rate 144.0",
                "settings put system min_refresh_rate 144.0",
                "cmd game set --downscale disable --fps 144 " + TARGET_PACKAGE,
                "cmd game mode performance " + TARGET_PACKAGE,
                "echo '--- Verification ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate)\"",
                "echo \"min_refresh_rate=$(settings get system min_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE,
                "cmd game list-configs " + TARGET_PACKAGE);
        return executeShell(cmd);
    }

    public static String readCurrentConfig() {
        String cmd = String.join("\n",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate)\"",
                "echo \"min_refresh_rate=$(settings get system min_refresh_rate)\"",
                "echo \"tran_refresh_mode=$(settings get system tran_refresh_mode)\"",
                "echo \"tran_need_recovery_refresh_rate=$(settings get system tran_need_recovery_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE,
                "cmd game list-configs " + TARGET_PACKAGE);
        return executeShell(cmd);
    }

    public static String resetToDefault() {
        String cmd = String.join("\n",
                "cmd display clear-user-preferred-display-mode 0",
                "settings delete system peak_refresh_rate",
                "settings delete system min_refresh_rate",
                "settings put global device_name '" + DEFAULT_TECNO_DEVICE_NAME + "'",
                "settings put secure bluetooth_name '" + DEFAULT_TECNO_DEVICE_NAME + "'",
                "cmd game reset " + TARGET_PACKAGE,
                "cmd game mode standard " + TARGET_PACKAGE,
                "echo '--- After Reset ---'",
                "cmd display get-user-preferred-display-mode 0",
                "echo \"device_name=$(settings get global device_name)\"",
                "echo \"peak_refresh_rate=$(settings get system peak_refresh_rate)\"",
                "cmd game list-modes " + TARGET_PACKAGE);
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
            // Shizuku.newProcess is private in API 13.x; reflection keeps shell execution direct without a separate UserService.
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
