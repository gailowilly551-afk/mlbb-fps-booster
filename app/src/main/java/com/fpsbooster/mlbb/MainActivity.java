package com.fpsbooster.mlbb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fpsbooster.mlbb.shizuku.ShizukuShellRunner;
import com.fpsbooster.mlbb.shizuku.ShizukuShellRunner.DisplayModeSpec;
import com.fpsbooster.mlbb.xposed.DeviceSpoofHook;
import com.fpsbooster.mlbb.xposed.SpoofProfiles;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

/**
 * Main controller UI for MLBB 165 FPS Unlocker, Dynamic Display/PowerHAL Booster, and Deep Spoofer.
 */
@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {

    private static final int REQ_CODE_SHIZUKU = 1001;
    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    private static final String MLBB_PACKAGE = "com.mobile.legends";

    private static final int BG_DARK = Color.parseColor("#0B101B");
    private static final int CARD_BG = Color.parseColor("#141C2B");
    private static final int CARD_BORDER = Color.parseColor("#25324B");
    private static final int TEXT_PRIMARY = Color.parseColor("#F1F5F9");
    private static final int TEXT_MUTED = Color.parseColor("#94A3B8");
    private static final int ACCENT_GREEN = Color.parseColor("#00E676");
    private static final int ACCENT_CYAN = Color.parseColor("#38BDF8");
    private static final int ACCENT_AMBER = Color.parseColor("#F59E0B");
    private static final int ACCENT_PURPLE = Color.parseColor("#A855F7");
    private static final int ACCENT_RED = Color.parseColor("#EF4444");
    private static final int BTN_DARK = Color.parseColor("#1E293B");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String originalHardwareModel;
    private TextView deviceInfoView;
    private TextView shizukuStatusBadge;
    private TextView spoofStatusView;
    private TextView logConsoleView;
    private Button shizukuAuthBtn;
    private Button applyNativePeakBtn;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> mainHandler.post(this::refreshAllStatus);

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> mainHandler.post(this::refreshAllStatus);

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> mainHandler.post(() -> {
                refreshAllStatus();
                if (requestCode == REQ_CODE_SHIZUKU) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        appendLog("Shizuku permission GRANTED. Auto-syncing spoof profile bridge...");
                        SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(loadSavedProfileId());
                        runShizukuTask(
                                "Syncing profile bridge (" + p.model + ")...",
                                () -> ShizukuShellRunner.syncProfileBridge(p));
                    } else {
                        appendLog("Shizuku permission DENIED.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        originalHardwareModel = Build.BRAND + " " + Build.MODEL;
        setContentView(buildUi());

        // Ensure preferences file is readable by XSharedPreferences & SpoofConfigProvider on startup
        saveSelectedProfile(loadSavedProfileId(), false);

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        refreshAllStatus();
        appendLog("Ready. Select your target profile & tap 'APPLY FULL SPOOF + REAL MAX FPS + BOOST'.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllStatus();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_DARK);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, dp(42), pad, dp(36));

        TextView title = makeText("MLBB 165 FPS Unlocker & Deep Spoofer", 19, TEXT_PRIMARY, true);
        TextView subtitle = makeText(
                "Dynamic Hz Controller + PowerHAL Boost + Deep Device/GPU Spoofer",
                12, ACCENT_CYAN, false);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        root.addView(title);
        root.addView(subtitle);

        // Card 1: Device & Display Monitor
        LinearLayout card1 = createCard();
        card1.addView(makeSectionHeader("1. LIVE DEVICE, DISPLAY & SPOOF MONITOR", ACCENT_CYAN));
        deviceInfoView = makeText("Loading display modes...", 12, TEXT_PRIMARY, false);
        deviceInfoView.setTypeface(Typeface.MONOSPACE);
        deviceInfoView.setPadding(0, dp(6), 0, dp(8));
        card1.addView(deviceInfoView);

        Button refreshMonitorBtn = makeButton("REFRESH LIVE MONITOR", BTN_DARK, TEXT_PRIMARY);
        refreshMonitorBtn.setOnClickListener(v -> refreshAllStatus());
        card1.addView(refreshMonitorBtn);
        root.addView(card1);

        // Card 2: Shizuku Engine
        LinearLayout card2 = createCard();
        card2.addView(makeSectionHeader("2. SHIZUKU REAL FPS & PERFORMANCE ENGINE", ACCENT_GREEN));
        shizukuStatusBadge = makeText("Checking Shizuku...", 13, ACCENT_AMBER, true);
        shizukuStatusBadge.setPadding(0, dp(6), 0, dp(10));
        card2.addView(shizukuStatusBadge);

        shizukuAuthBtn = makeButton("REQUEST SHIZUKU PERMISSION", BTN_DARK, ACCENT_CYAN);
        shizukuAuthBtn.setOnClickListener(v -> handleShizukuAuth());
        card2.addView(shizukuAuthBtn);

        Button fullUnlockBtn = makeButton(
                "APPLY FULL SPOOF + REAL MAX FPS + BOOST (SHIZUKU)", ACCENT_GREEN, Color.BLACK);
        fullUnlockBtn.setOnClickListener(v -> {
            SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(loadSavedProfileId());
            DisplayModeSpec hwPeak = detectPeakDisplayModeSpec();
            String ds = loadSavedDownscale();
            runShizukuTask(
                    "Applying Full Spoof (" + p.model + ") + Peak Mode ("
                            + hwPeak.width + "x" + hwPeak.height + "@" + hwPeak.roundedHz
                            + "Hz) + GameOverlay " + Math.round(p.targetRefreshRate) + "FPS...",
                    () -> ShizukuShellRunner.applyFullSpoofAndUnlock(p, hwPeak, ds));
        });
        card2.addView(fullUnlockBtn);

        DisplayModeSpec detectedPeak = detectPeakDisplayModeSpec();
        applyNativePeakBtn = makeButton(
                "LOCK NATIVE HARDWARE PEAK (" + detectedPeak.roundedHz + "Hz EXACT)",
                ACCENT_CYAN, Color.BLACK);
        applyNativePeakBtn.setOnClickListener(v -> {
            DisplayModeSpec hwPeak = detectPeakDisplayModeSpec();
            String ds = loadSavedDownscale();
            runShizukuTask(
                    "Locking Native Hardware Peak (" + hwPeak.exactRefreshRate + " Hz)...",
                    () -> ShizukuShellRunner.applyNativeHardwarePeak(hwPeak, ds));
        });
        card2.addView(applyNativePeakBtn);

        Button aotBoostBtn = makeButton(
                "AOT SPEED COMPILE + RAM & POWERHAL BOOST (ANTI-LAG)", ACCENT_AMBER, Color.BLACK);
        aotBoostBtn.setOnClickListener(v -> runShizukuTask(
                "Running AOT DEX Speed Compile + PowerHAL Fixed Performance + RAM Trim (may take 10-25s)...",
                ShizukuShellRunner::applyPerformanceBoostAndCompile));
        card2.addView(aotBoostBtn);

        // Downscale selector row
        TextView scaleLabel = makeText(
                "GPU Load / Game Resolution Scaling (Teamfight FPS Stability):",
                11, TEXT_MUTED, true);
        scaleLabel.setPadding(0, dp(10), 0, dp(2));
        card2.addView(scaleLabel);

        LinearLayout scaleRow = new LinearLayout(this);
        scaleRow.setOrientation(LinearLayout.HORIZONTAL);
        scaleRow.addView(makeScaleBtn("100% Native", "1.0"));
        scaleRow.addView(makeScaleBtn("90% Smooth", "0.9"));
        scaleRow.addView(makeScaleBtn("85% Max FPS", "0.85"));
        card2.addView(scaleRow);

        Button checkCfgBtn = makeButton("CHECK REAL FPS (GFXINFO) & SYSTEM CONFIG", BTN_DARK, TEXT_PRIMARY);
        checkCfgBtn.setOnClickListener(v -> runShizukuTask(
                "Querying Display Mode, GameManager & MLBB Real Frame Stats...",
                ShizukuShellRunner::readCurrentConfigAndRealFps));
        card2.addView(checkCfgBtn);

        Button resetBtn = makeButton("RESET ALL TO DEFAULT", ACCENT_RED, Color.WHITE);
        resetBtn.setOnClickListener(v -> runShizukuTask(
                "Resetting display preference, thermal override, power mode & MLBB game mode...",
                () -> ShizukuShellRunner.resetToDefault(originalHardwareModel)));
        card2.addView(resetBtn);

        Button launchMlbbBtn = makeButton("OPEN MOBILE LEGENDS", BTN_DARK, ACCENT_GREEN);
        launchMlbbBtn.setOnClickListener(v -> launchPackage(MLBB_PACKAGE));
        card2.addView(launchMlbbBtn);

        root.addView(card2);

        // Card 3: LSPatch / Xposed Spoofer Engine
        LinearLayout card3 = createCard();
        card3.addView(makeSectionHeader("3. DEEP DEVICE & GPU SPOOFER (SHIZUKU + LSPATCH/XPOSED)", ACCENT_AMBER));
        spoofStatusView = makeText("", 12, TEXT_MUTED, false);
        spoofStatusView.setPadding(0, dp(6), 0, dp(8));
        card3.addView(spoofStatusView);

        RadioGroup profileGroup = new RadioGroup(this);
        String savedProfile = loadSavedProfileId();
        for (SpoofProfiles.Profile p : SpoofProfiles.all().values()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(p.displayName);
            rb.setTextColor(TEXT_PRIMARY);
            rb.setTextSize(12);
            rb.setTag(p.id);
            rb.setPadding(dp(4), dp(6), dp(4), dp(6));
            profileGroup.addView(rb);
            if (p.id.equals(savedProfile)) {
                rb.setChecked(true);
            }
        }
        profileGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof String) {
                String profileId = (String) checked.getTag();
                saveSelectedProfile(profileId, true);
                updateSpoofStatusText(profileId);
                updateDeviceMonitor();
            }
        });
        card3.addView(profileGroup);

        Button verifySpoofBtn = makeButton(
                "VERIFY & PREVIEW SPOOF PAYLOAD", ACCENT_PURPLE, Color.WHITE);
        verifySpoofBtn.setOnClickListener(v -> previewSpoofPayload());
        card3.addView(verifySpoofBtn);

        TextView lspatchNote = makeText(
                "How Deep Spoofing Works:\n"
                        + "1. Multi-Bridge Sync: Automatically syncs via ContentProvider, XSharedPreferences (0644), Settings.Global (mlbb_spoof_profile), and MLBB external files dir.\n"
                        + "2. Deep Hooks: Spoofs Build.*, SystemProperties.get/native_get, GLES20/30 GL_RENDERER & GL_VENDOR (Adreno 750 / Adreno 830), Display.Mode 165Hz, and Surface.setFrameRate().\n"
                        + "3. For Zero-Modify Safe Mode on unpatched MLBB: Use the Shizuku buttons in Card 2.",
                11, ACCENT_AMBER, false);
        lspatchNote.setPadding(0, dp(8), 0, 0);
        card3.addView(lspatchNote);
        root.addView(card3);

        // Card 4: Live Command Log Console
        LinearLayout card4 = createCard();
        card4.addView(makeSectionHeader("4. LIVE COMMAND & DIAGNOSTIC CONSOLE", ACCENT_CYAN));
        logConsoleView = makeText("", 11, ACCENT_GREEN, false);
        logConsoleView.setTypeface(Typeface.MONOSPACE);
        logConsoleView.setPadding(dp(8), dp(8), dp(8), dp(8));
        logConsoleView.setBackgroundColor(Color.parseColor("#060911"));
        card4.addView(logConsoleView);
        root.addView(card4);

        scroll.addView(root);
        return scroll;
    }

    private Button makeScaleBtn(String title, String ratio) {
        Button btn = makeButton(title, BTN_DARK, ACCENT_CYAN);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.topMargin = dp(6);
        lp.rightMargin = dp(4);
        btn.setLayoutParams(lp);
        btn.setTextSize(11);
        btn.setOnClickListener(v -> {
            saveDownscale(ratio);
            DisplayModeSpec spec = detectPeakDisplayModeSpec();
            SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(loadSavedProfileId());
            int targetFps = Math.max(spec.roundedHz, Math.round(p.targetRefreshRate));
            runShizukuTask(
                    "Setting GameManager downscale=" + ratio + " @ " + targetFps + " FPS...",
                    () -> ShizukuShellRunner.applyDownscalePreset(ratio, targetFps));
        });
        return btn;
    }

    private void refreshAllStatus() {
        updateDeviceMonitor();
        updateShizukuStatus();
        updateSpoofStatusText(loadSavedProfileId());
    }

    private DisplayModeSpec detectPeakDisplayModeSpec() {
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display == null) {
            return DisplayModeSpec.fallback144();
        }
        Display.Mode[] modes = display.getSupportedModes();
        if (modes == null || modes.length == 0) {
            Display.Mode current = display.getMode();
            if (current != null) {
                return new DisplayModeSpec(
                        current.getModeId(),
                        current.getPhysicalWidth(),
                        current.getPhysicalHeight(),
                        current.getRefreshRate());
            }
            return DisplayModeSpec.fallback144();
        }
        Display.Mode best = modes[0];
        for (Display.Mode m : modes) {
            if (m.getRefreshRate() > best.getRefreshRate()
                    || (Math.abs(m.getRefreshRate() - best.getRefreshRate()) < 0.01f
                    && m.getPhysicalWidth() >= best.getPhysicalWidth())) {
                best = m;
            }
        }
        return new DisplayModeSpec(
                best.getModeId(),
                best.getPhysicalWidth(),
                best.getPhysicalHeight(),
                best.getRefreshRate());
    }

    private void updateDeviceMonitor() {
        SpoofProfiles.Profile target = SpoofProfiles.getOrDefault(loadSavedProfileId());
        DisplayModeSpec peakSpec = detectPeakDisplayModeSpec();
        if (applyNativePeakBtn != null) {
            applyNativePeakBtn.setText(
                    "LOCK NATIVE HARDWARE PEAK (" + peakSpec.roundedHz + "Hz EXACT)");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Real HW     : ").append(Build.BRAND).append(' ').append(Build.MODEL).append('\n');
        sb.append("Spoof Target: ").append(target.model)
                .append(" (").append(target.socModel).append(")\n");
        sb.append("Spoof GPU   : ").append(target.gpuVendor)
                .append(" / ").append(target.gpuRenderer).append('\n');
        sb.append("Global Name : ").append(readGlobalSetting("device_name")).append('\n');
        sb.append("Global Sync : ").append(readGlobalSetting(SpoofProfiles.GLOBAL_SETTING_PROFILE_KEY)).append('\n');
        sb.append("Android SDK : ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display != null) {
            Display.Mode current = display.getMode();
            sb.append(String.format(Locale.US, "Live Display: %.2f Hz (%dx%d, id=%d)\n",
                    current.getRefreshRate(), current.getPhysicalWidth(),
                    current.getPhysicalHeight(), current.getModeId()));
            sb.append(String.format(Locale.US, "Peak HW Mode: id=%d (%dx%d @ %s Hz)\n",
                    peakSpec.modeId, peakSpec.width, peakSpec.height,
                    Float.toString(peakSpec.exactRefreshRate)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sb.append("User Pref   : ").append(readUserPreferredModeText(display, dm)).append('\n');
            }
            sb.append("Panel Modes :\n");
            for (Display.Mode m : display.getSupportedModes()) {
                sb.append(String.format(Locale.US, "  - id=%d: %dx%d @ %.2f Hz\n",
                        m.getModeId(), m.getPhysicalWidth(), m.getPhysicalHeight(), m.getRefreshRate()));
            }
        }
        sb.append("Downscale   : ").append(loadSavedDownscale()).append('\n');
        sb.append("MLBB Status : ").append(isPackageInstalled(MLBB_PACKAGE) ? "Installed" : "Not Found");
        deviceInfoView.setText(sb.toString().trim());
    }

    private String readGlobalSetting(String key) {
        try {
            String val = Settings.Global.getString(getContentResolver(), key);
            return (val != null && !val.isEmpty()) ? val : "Not Set";
        } catch (Throwable t) {
            return "Unavailable";
        }
    }

    private String readUserPreferredModeText(Display display, DisplayManager dm) {
        try {
            java.lang.reflect.Method m = Display.class.getMethod("getUserPreferredDisplayMode");
            Object res = m.invoke(display);
            if (res instanceof Display.Mode) {
                Display.Mode pref = (Display.Mode) res;
                return String.format(Locale.US, "id=%d (%.2f Hz)",
                        pref.getModeId(), pref.getRefreshRate());
            }
            if (dm != null) {
                java.lang.reflect.Method gm =
                        DisplayManager.class.getMethod("getGlobalUserPreferredDisplayMode");
                Object gres = gm.invoke(dm);
                if (gres instanceof Display.Mode) {
                    Display.Mode pref = (Display.Mode) gres;
                    return String.format(Locale.US, "id=%d (%.2f Hz)",
                            pref.getModeId(), pref.getRefreshRate());
                }
            }
            return "Dynamic (None)";
        } catch (Throwable t) {
            return "Use CHECK REAL FPS button";
        }
    }

    private void updateShizukuStatus() {
        if (!ShizukuShellRunner.isBinderAlive()) {
            shizukuStatusBadge.setText("Status: Shizuku Not Running");
            shizukuStatusBadge.setTextColor(ACCENT_RED);
            shizukuAuthBtn.setText(isPackageInstalled(SHIZUKU_PACKAGE)
                    ? "OPEN SHIZUKU APP"
                    : "SHIZUKU NOT INSTALLED");
        } else if (ShizukuShellRunner.hasPermission()) {
            shizukuStatusBadge.setText("Status: Connected & Authorized (UID 2000 Ready)");
            shizukuStatusBadge.setTextColor(ACCENT_GREEN);
            shizukuAuthBtn.setText("SHIZUKU AUTHORIZED (READY)");
        } else {
            shizukuStatusBadge.setText("Status: Connected (Permission Needed)");
            shizukuStatusBadge.setTextColor(ACCENT_AMBER);
            shizukuAuthBtn.setText("REQUEST SHIZUKU PERMISSION");
        }
    }

    private void updateSpoofStatusText(String profileId) {
        SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(profileId);
        boolean selfHooked = "true".equals(System.getProperty("mlbb.spoof.hook.active"));
        spoofStatusView.setText(
                "Active Profile : " + p.marketName + " [" + p.model + "]\n"
                        + "Spoof SoC/GPU  : " + p.socManufacturer + " " + p.socModel
                        + " / " + p.gpuRenderer + " (" + Math.round(p.targetRefreshRate) + "Hz)\n"
                        + "Config Bridge  : Provider Ready + Prefs (0644)"
                        + (selfHooked ? " + Xposed Self-Verified" : ""));
    }

    private void previewSpoofPayload() {
        SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(loadSavedProfileId());
        Map<String, String> overrides = DeviceSpoofHook.buildPropertyOverrides(p);
        DisplayModeSpec hwPeak = detectPeakDisplayModeSpec();

        StringBuilder sb = new StringBuilder();
        sb.append("=== SPOOF PAYLOAD VERIFICATION (").append(p.id).append(") ===\n");
        sb.append("Build.MANUFACTURER : ").append(p.manufacturer).append('\n');
        sb.append("Build.BRAND        : ").append(p.brand).append('\n');
        sb.append("Build.MODEL        : ").append(p.model).append('\n');
        sb.append("Build.DEVICE       : ").append(p.device).append('\n');
        sb.append("Build.PRODUCT      : ").append(p.product).append('\n');
        sb.append("Build.HARDWARE     : ").append(p.hardware).append('\n');
        sb.append("Build.SOC_MODEL    : ").append(p.socModel).append('\n');
        sb.append("Build.FINGERPRINT  : ").append(p.fingerprint).append('\n');
        sb.append("GLES GL_VENDOR     : ").append(p.gpuVendor).append('\n');
        sb.append("GLES GL_RENDERER   : ").append(p.gpuRenderer).append('\n');
        sb.append("ro.product.model   : ").append(overrides.get("ro.product.model")).append('\n');
        sb.append("ro.soc.model       : ").append(overrides.get("ro.soc.model")).append('\n');
        sb.append("ro.product.tr_model: ")
                .append(DeviceSpoofHook.resolvePropertyOverride("ro.product.tr_mi.model", overrides, p))
                .append('\n');
        sb.append("persist.sys.tran   : ").append(overrides.get("persist.sys.tran.device.name")).append('\n');
        sb.append("Target Spoof Hz    : ").append(p.targetRefreshRate).append(" Hz\n");
        sb.append("Mapped HW Mode ID  : id=").append(hwPeak.modeId)
                .append(" (").append(hwPeak.width).append('x').append(hwPeak.height)
                .append(" @ ").append(hwPeak.exactRefreshRate).append(" Hz)");
        appendLog(sb.toString());
    }

    private void handleShizukuAuth() {
        if (!ShizukuShellRunner.isBinderAlive()) {
            launchPackage(SHIZUKU_PACKAGE);
            return;
        }
        if (ShizukuShellRunner.hasPermission()) {
            appendLog("Shizuku permission is already granted.");
            return;
        }
        try {
            Shizuku.requestPermission(REQ_CODE_SHIZUKU);
        } catch (Throwable t) {
            appendLog("Failed to request Shizuku permission: " + t.getMessage());
        }
    }

    private void runShizukuTask(String startMsg, java.util.concurrent.Callable<String> task) {
        appendLog(startMsg);
        executor.execute(() -> {
            String result;
            try {
                result = task.call();
            } catch (Throwable t) {
                result = "[ERROR] " + t.getMessage();
            }
            String finalResult = result;
            mainHandler.post(() -> {
                appendLog(finalResult);
                updateDeviceMonitor();
            });
        });
    }

    private void launchPackage(String packageName) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            startActivity(launch);
        } else {
            appendLog("Package not launchable or not installed: " + packageName);
        }
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String loadSavedProfileId() {
        SharedPreferences prefs = getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(SpoofProfiles.KEY_SELECTED_PROFILE, SpoofProfiles.DEFAULT_PROFILE_KEY);
        return SpoofProfiles.getOrDefault(raw).id;
    }

    private String loadSavedDownscale() {
        SharedPreferences prefs = getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(SpoofProfiles.KEY_DOWNSCALE_FACTOR, "1.0");
    }

    @SuppressLint("ApplySharedPref")
    private void saveDownscale(String ratio) {
        getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SpoofProfiles.KEY_DOWNSCALE_FACTOR, ratio)
                .commit();
        makePrefsWorldReadable();
    }

    @SuppressLint("SetWorldReadable")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void makePrefsWorldReadable() {
        try {
            File dataDir = new File(getApplicationInfo().dataDir);
            dataDir.setExecutable(true, false);
            dataDir.setReadable(true, false);

            File prefsDir = new File(dataDir, "shared_prefs");
            if (prefsDir.exists()) {
                prefsDir.setExecutable(true, false);
                prefsDir.setReadable(true, false);
            }

            File prefsFile = new File(prefsDir, SpoofProfiles.PREFS_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressLint("ApplySharedPref")
    private void saveSelectedProfile(String profileId, boolean logAndSyncShizuku) {
        getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SpoofProfiles.KEY_SELECTED_PROFILE, profileId)
                .commit();
        makePrefsWorldReadable();

        if (logAndSyncShizuku) {
            SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(profileId);
            appendLog("Selected Spoof Target: " + p.model + " (" + p.socModel + " / " + p.gpuRenderer + ")");
            if (ShizukuShellRunner.isBinderAlive() && ShizukuShellRunner.hasPermission()) {
                runShizukuTask(
                        "Auto-syncing profile bridge via Shizuku...",
                        () -> ShizukuShellRunner.syncProfileBridge(p));
            }
        }
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String existing = logConsoleView.getText().toString();
        String line = "[" + time + "] " + message;
        String updated = existing.isEmpty() ? line : existing + "\n" + line;
        if (updated.length() > 8000) {
            updated = updated.substring(updated.length() - 8000);
        }
        logConsoleView.setText(updated);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), CARD_BORDER);
        card.setBackground(bg);
        int p = dp(14);
        card.setPadding(p, p, p, p);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView makeSectionHeader(String text, int color) {
        TextView tv = makeText(text, 12, color, true);
        tv.setPadding(0, 0, 0, dp(4));
        return tv;
    }

    private TextView makeText(String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        return tv;
    }

    private Button makeButton(String label, int bgColor, int textColor) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(textColor);
        btn.setTextSize(12);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
