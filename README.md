# MLBB 165 FPS Unlocker, Performance Booster & Deep Spoofer (`com.fpsbooster.mlbb`)

A 2-in-1 Android 10–16 (`minSdk 29`, `targetSdk 35`) utility combining a **Shizuku System-Level Real FPS & Performance Engine** (no root / no APK patch required) with a **Deep LSPatch / Xposed Device & GPU Spoofing Module** (`DeviceSpoofHook`).

## Key Features

### 1. Shizuku Real FPS & Performance Engine (`ShizukuShellRunner`)
- **Dynamic Hardware Peak Hz Lock:** Automatically detects the exact highest physical `Display.Mode` (`modeId`, `width`, `height`, and exact float `refreshRate`, e.g., `1080x2436 @ 144.00002 Hz` or `120.0 Hz`) via `DisplayManager` and applies `cmd display set-user-preferred-display-mode <w> <h> <exact_hz> 0`.
- **Smart Transsion (HiOS/XOS) & Multi-OEM Bypass:** Writes the true hardware maximum integer Hz (`144` / `120`) to `tran_refresh_mode`, `tran_need_recovery_refresh_rate`, `user_refresh_rate`, `peak_refresh_rate`, `min_refresh_rate`, `miui_refresh_rate`, and `oplus_customize_screen_refresh_rate` so OEM controllers never reject out-of-range values or reset back to 60Hz, while disabling `tran_high_temperature_limit_refresh_rate` and `tran_fps_stabilization`.
- **Android 12–16 GameManager Overlay + PowerHAL + Thermal Override:**
  - Configures `device_config put game_overlay com.mobile.legends mode=2,downscaleFactor=<ratio>,fps=<target>,loadingBoost=999999999` and `cmd game set --mode 2`.
  - Locks sustained CPU/GPU frequencies via `cmd power set-fixed-performance-mode-enabled true`.
  - Prevents thermal refresh-rate throttling via `cmd thermalservice override-status 0`.
  - Eliminates SurfaceFlinger VSync backpressure via `setprop debug.sf.latch_unsignaled 1`, `setprop debug.sf.disable_backpressure 1`, and `setprop debug.hwui.use_hint_manager true`.
- **AOT DEX Speed Compilation & Pre-Match RAM Trim:**
  - Runs `cmd package compile -m speed -f com.mobile.legends` and `am kill-all` to eliminate runtime JIT compilation stutters during 5v5 teamfights.
- **Real Rendered FPS Verification:** Queries `dumpsys gfxinfo com.mobile.legends` alongside `cmd display` and `cmd game` to inspect real rendered frame counts and jank percentiles.

### 2. Deep LSPatch / Xposed Device & GPU Spoofer (`DeviceSpoofHook`)
- **4-Layer Cross-Process Config Sync (Works on Android 11–16, LSPosed & Rootless LSPatch):**
  1. External/Tmp Bridge Files (`/storage/emulated/0/Android/data/com.mobile.legends/files/.mlbb_spoof_profile`, `/data/local/tmp/mlbb_spoof_profile.txt`)
  2. `Settings.Global` Bridge (`mlbb_spoof_profile` & `device_name`)
  3. Exported Read-Only `ContentProvider` (`SpoofConfigProvider` at `content://com.fpsbooster.mlbb.provider/config`)
  4. `XSharedPreferences` with `xposedsharedprefs` manifest metadata and `0644` permissions
- **Complete Identity & GPU Spoofing:**
  - Spoofs `android.os.Build.*` (`MANUFACTURER`, `BRAND`, `MODEL`, `DEVICE`, `PRODUCT`, `BOARD`, `HARDWARE`, `SOC_MANUFACTURER`, `SOC_MODEL`, `FINGERPRINT`, `SKU`, `ODM_SKU`).
  - Hooks `SystemProperties.get`, `SystemProperties.native_get`, and `System.getProperty` (scrubbing `ro.product.tr_*`, `persist.sys.tran.*`, and MediaTek props).
  - Hooks OpenGL ES `GLES10` / `GLES20` / `GLES30` `glGetString(GL_VENDOR)` and `glGetString(GL_RENDERER)` (`Adreno (TM) 750`, `Adreno (TM) 830`, `Mali-G610 MC6`) so Unity's `SystemInfo.graphicsDeviceName` matches the spoofed flagship device.
  - Hooks `Display.getRefreshRate()`, `Display.Mode.getRefreshRate()` (mapped to the real peak hardware `modeId`), `Surface.setFrameRate()` (preventing 60Hz down-throttling), and `Activity.onResume()` window `preferredDisplayModeId`.
- **Built-in 165Hz Flagship Profiles (`SpoofProfiles`):**
  - `ASUS_AI2401_A` (ROG Phone 8 Pro — 165Hz / Adreno 750)
  - `ASUS_AI2501` (ROG Phone 9 Pro — 165Hz / Adreno 830)
  - `NX769J` (RedMagic 9 Pro — 165Hz / Adreno 750)
