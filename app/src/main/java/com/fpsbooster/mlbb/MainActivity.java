package com.fpsbooster.mlbb;

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
import com.fpsbooster.mlbb.xposed.SpoofProfiles;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

/**
 * Main UI for configuring Transsion bypass, Shizuku 165 FPS commands, and LSPatch spoof profiles.
 */
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
    private static final int ACCENT_RED = Color.parseColor("#EF4444");
    private static final int BTN_DARK = Color.parseColor("#1E293B");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView deviceInfoView;
    private TextView shizukuStatusBadge;
    private TextView spoofStatusView;
    private TextView logConsoleView;
    private Button shizukuAuthBtn;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> mainHandler.post(this::refreshAllStatus);

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> mainHandler.post(this::refreshAllStatus);

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> mainHandler.post(() -> {
                refreshAllStatus();
                if (requestCode == REQ_CODE_SHIZUKU) {
                    appendLog(grantResult == PackageManager.PERMISSION_GRANTED
                            ? "Shizuku permission GRANTED."
                            : "Shizuku permission DENIED.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        refreshAllStatus();
        appendLog("Ready. Tap 'BYPASS & SPOOF TRANSSION (SHIZUKU)' to lock 144Hz & apply spoofed device_name.");
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
        root.setPadding(pad, dp(36), pad, dp(28));

        TextView title = makeText("MLBB 165 FPS Unlocker & Transsion Spoofer", 19, TEXT_PRIMARY, true);
        TextView subtitle = makeText(
                "Android 16 Shizuku Controller + LSPatch Transsion Hook",
                12, ACCENT_CYAN, false);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        root.addView(title);
        root.addView(subtitle);

        // Card 1: Device & Display Monitor
        LinearLayout card1 = createCard();
        card1.addView(makeSectionHeader("1. DEVICE & DISPLAY MONITOR", ACCENT_CYAN));
        deviceInfoView = makeText("Loading display modes...", 13, TEXT_PRIMARY, false);
        deviceInfoView.setTypeface(Typeface.MONOSPACE);
        deviceInfoView.setPadding(0, dp(6), 0, dp(8));
        card1.addView(deviceInfoView);

        Button refreshMonitorBtn = makeButton("REFRESH MONITOR", BTN_DARK, TEXT_PRIMARY);
        refreshMonitorBtn.setOnClickListener(v -> refreshAllStatus());
        card1.addView(refreshMonitorBtn);
        root.addView(card1);

        // Card 2: Shizuku Engine
        LinearLayout card2 = createCard();
        card2.addView(makeSectionHeader("2. SHIZUKU ENGINE (SAFE / SYSTEM API)", ACCENT_GREEN));
        shizukuStatusBadge = makeText("Checking Shizuku...", 13, ACCENT_AMBER, true);
        shizukuStatusBadge.setPadding(0, dp(6), 0, dp(10));
        card2.addView(shizukuStatusBadge);

        Button bypassTranssionBtn = makeButton(
                "BYPASS & SPOOF TRANSSION (SHIZUKU)", ACCENT_AMBER, Color.BLACK);
        bypassTranssionBtn.setOnClickListener(v -> {
            SpoofProfiles.Profile p = SpoofProfiles.getOrDefault(loadSavedProfileId());
            runShizukuTask(
                    "Running Transsion bypass + spoofing device_name to " + p.model + "...",
                    () -> ShizukuShellRunner.applyTranssionSpoofAnd165Unlock(p.model));
        });
        card2.addView(bypassTranssionBtn);

        shizukuAuthBtn = makeButton("REQUEST SHIZUKU PERMISSION", BTN_DARK, ACCENT_CYAN);
        shizukuAuthBtn.setOnClickListener(v -> handleShizukuAuth());
        card2.addView(shizukuAuthBtn);

        Button apply165Btn = makeButton("APPLY 165 FPS COMMANDS (SHIZUKU)", ACCENT_GREEN, Color.BLACK);
        apply165Btn.setOnClickListener(v -> runShizukuTask(
                "Applying 165 FPS + Performance Mode...",
                ShizukuShellRunner::apply165FpsCommands));
        card2.addView(apply165Btn);

        Button apply144Btn = makeButton("LOCK 144Hz NATIVE HARDWARE PEAK", ACCENT_CYAN, Color.BLACK);
        apply144Btn.setOnClickListener(v -> runShizukuTask(
                "Locking 144Hz Native Hardware Peak...",
                ShizukuShellRunner::apply144HzHardwarePeak));
        card2.addView(apply144Btn);

        Button checkCfgBtn = makeButton("CHECK CURRENT FPS CONFIG", BTN_DARK, TEXT_PRIMARY);
        checkCfgBtn.setOnClickListener(v -> runShizukuTask(
                "Querying system settings & cmd game...",
                ShizukuShellRunner::readCurrentConfig));
        card2.addView(checkCfgBtn);

        Button resetBtn = makeButton("RESET TO DEFAULT", ACCENT_RED, Color.WHITE);
        resetBtn.setOnClickListener(v -> runShizukuTask(
                "Resetting display preference, device_name & MLBB game mode...",
                ShizukuShellRunner::resetToDefault));
        card2.addView(resetBtn);

        Button launchMlbbBtn = makeButton("OPEN MOBILE LEGENDS", BTN_DARK, ACCENT_GREEN);
        launchMlbbBtn.setOnClickListener(v -> launchPackage(MLBB_PACKAGE));
        card2.addView(launchMlbbBtn);

        root.addView(card2);

        // Card 3: LSPatch / Xposed Spoofer Engine
        LinearLayout card3 = createCard();
        card3.addView(makeSectionHeader("3. LSPATCH / XPOSED TRANSSION SPOOFER", ACCENT_AMBER));
        spoofStatusView = makeText("", 12, TEXT_MUTED, false);
        spoofStatusView.setPadding(0, dp(6), 0, dp(8));
        card3.addView(spoofStatusView);

        RadioGroup profileGroup = new RadioGroup(this);
        String savedProfile = loadSavedProfileId();
        for (SpoofProfiles.Profile p : SpoofProfiles.all().values()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(p.displayName);
            rb.setTextColor(TEXT_PRIMARY);
            rb.setTextSize(13);
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
                saveSelectedProfile(profileId);
                updateSpoofStatusText(profileId);
                appendLog("Saved spoof target: " + profileId);
            }
        });
        card3.addView(profileGroup);

        TextView lspatchNote = makeText(
                "Full Transsion Spoof (via LSPatch):\n"
                        + "- Pinapalitan ang Build.*, ro.product.tr_*, persist.sys.tran.device.name, MT6878 props, Settings device_name, at Display.getRefreshRate()=165Hz.\n"
                        + "- Piliin ang 'Infinix X6871' kung gusto ng official Transsion 144Hz profile o 'ASUS_AI2401_A' para sa 165Hz.\n"
                        + "BABALA: High Ban Risk ang pag-patch ng MLBB APK. Sa main account, gamitin lang ang Shizuku Bypass sa taas.",
                12, ACCENT_AMBER, false);
        lspatchNote.setPadding(0, dp(8), 0, 0);
        card3.addView(lspatchNote);
        root.addView(card3);

        // Card 4: Live Command Log Console
        LinearLayout card4 = createCard();
        card4.addView(makeSectionHeader("4. LIVE COMMAND LOG CONSOLE", ACCENT_CYAN));
        logConsoleView = makeText("", 11, ACCENT_GREEN, false);
        logConsoleView.setTypeface(Typeface.MONOSPACE);
        logConsoleView.setPadding(dp(8), dp(8), dp(8), dp(8));
        logConsoleView.setBackgroundColor(Color.parseColor("#060911"));
        card4.addView(logConsoleView);
        root.addView(card4);

        scroll.addView(root);
        return scroll;
    }

    private void refreshAllStatus() {
        updateDeviceMonitor();
        updateShizukuStatus();
        updateSpoofStatusText(loadSavedProfileId());
    }

    private void updateDeviceMonitor() {
        StringBuilder sb = new StringBuilder();
        sb.append("Model       : ").append(Build.BRAND).append(' ').append(Build.MODEL).append('\n');
        sb.append("Device Name : ").append(readGlobalDeviceName()).append('\n');
        sb.append("Android     : ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");

        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display != null) {
            Display.Mode current = display.getMode();
            sb.append(String.format(Locale.US, "Live Hz     : %.2f Hz (%dx%d)\n",
                    current.getRefreshRate(), current.getPhysicalWidth(), current.getPhysicalHeight()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sb.append("User Pref   : ").append(readUserPreferredModeText(display, dm)).append('\n');
            }
            sb.append("Panel Modes :\n");
            for (Display.Mode m : display.getSupportedModes()) {
                sb.append(String.format(Locale.US, "  - id=%d: %dx%d @ %.2f Hz\n",
                        m.getModeId(), m.getPhysicalWidth(), m.getPhysicalHeight(), m.getRefreshRate()));
            }
        }
        sb.append("MLBB        : ").append(isPackageInstalled(MLBB_PACKAGE) ? "Installed" : "Not Found");
        deviceInfoView.setText(sb.toString().trim());
    }

    private String readGlobalDeviceName() {
        try {
            String name = Settings.Global.getString(getContentResolver(), "device_name");
            return name != null ? name : "Unknown";
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
            return "None (-1)";
        } catch (Throwable t) {
            return "Use CHECK CONFIG button";
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
            shizukuStatusBadge.setText("Status: Connected & Authorized");
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
        spoofStatusView.setText(
                "Module Status : Ready for LSPatch / Xposed Scope (com.mobile.legends)\n"
                        + "Active Target : " + p.model + " (" + p.manufacturer + " / " + p.socModel + ")");
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
        return prefs.getString(SpoofProfiles.KEY_SELECTED_PROFILE, SpoofProfiles.DEFAULT_PROFILE_KEY);
    }

    @SuppressWarnings("deprecation")
    private void saveSelectedProfile(String profileId) {
        getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SpoofProfiles.KEY_SELECTED_PROFILE, profileId)
                .apply();
        try {
            getSharedPreferences(SpoofProfiles.PREFS_NAME, MODE_WORLD_READABLE)
                    .edit()
                    .putString(SpoofProfiles.KEY_SELECTED_PROFILE, profileId)
                    .apply();
        } catch (SecurityException ignored) {
        }
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String existing = logConsoleView.getText().toString();
        String line = "[" + time + "] " + message;
        String updated = existing.isEmpty() ? line : existing + "\n" + line;
        if (updated.length() > 6000) {
            updated = updated.substring(updated.length() - 6000);
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
        btn.setTextSize(13);
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
