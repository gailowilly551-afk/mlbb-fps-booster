# MLBB 165 FPS Unlocker & Transsion Spoofer (`com.fpsbooster.mlbb`)

A 2-in-1 Android 16 utility combining **Shizuku system-level display & game mode control** (no root required) with an **LSPatch / Xposed device spoofing module** tailored for Transsion (TECNO / Infinix / Itel HiOS) and gaming devices.

## Features

1. **Shizuku Engine (Safe Mode / System API — No APK Modification)**
   - Applies Android 16 Game Manager overrides (`cmd game set --mode 2 --fps 165 com.mobile.legends`, `--downscale disable`, `cmd game mode performance`).
   - Bypasses Transsion (HiOS) display recovery & thermal caps (`cmd display set-user-preferred-display-mode 1080 2436 144.00002 0`, `tran_refresh_mode`, `tran_need_recovery_refresh_rate`, `tran_high_temperature_limit_refresh_rate`, `tran_fps_stabilization`).
   - Applies system-level `device_name` / `bluetooth_name` spoof and `peak_refresh_rate` / `min_refresh_rate` overrides.
   - Includes a live Device & Display Monitor (`Display.Mode` list, `Live Hz`, `User Pref`, `device_name`) and Command Log Console.

2. **LSPatch / Xposed Transsion Spoofer (`DeviceSpoofHook`)**
   - Hooks `android.os.Build`, `android.os.SystemProperties`, `System.getProperty`, `Settings.Global/Secure` names, and `Display.getRefreshRate()` (`165.0f`).
   - Scrubs Transsion (`ro.product.tr_*`, `persist.sys.tran.device.name`, `ro.product.marketname`) and MediaTek (`MT6878`) identifiers.
   - Preset profiles:
     - `ASUS_AI2401_A` (ROG Phone 8 Pro — 165Hz)
     - `NX769J` (RedMagic 9 Pro — 144/165Hz)
     - `PJZ110` (OnePlus 13 — Snapdragon 8 Elite)
     - `Infinix X6871` (Infinix GT 20 Pro — Transsion 144Hz)

> **Warning:** Using LSPatch to modify/patch the `com.mobile.legends` APK changes its signature and carries a high account ban risk. For main accounts, use only the **Shizuku Engine** buttons.

## Build Requirements

- JDK 17 (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`)
- Android SDK (`compileSdk 35`, `minSdk 29`, `targetSdk 35`)
- Gradle 8.6

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 gradle assembleDebug
```
