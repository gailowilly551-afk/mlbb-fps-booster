# MLBB 165 FPS Unlocker, Performance Booster & Deep Spoofer (`com.fpsbooster.mlbb`)

A 2-in-1 Android 10–16 (`minSdk 29`, `targetSdk 35`) utility combining a **Shizuku System-Level Real FPS & Performance Engine** (no root / no APK patch required) with a **Deep LSPatch / Xposed Device & GPU Spoofing Module** (`DeviceSpoofHook`).

---

## Kailangan pa ba i-download ang Latest LSPatch? (Do You Need LSPatch?)

Depende sa gusto mong gawin—**pwede itong gamitin nang WALANG LSPatch**, o **KASAMA ang LSPatch/LSPosed**:

| Mode | Kailangan ba ng LSPatch? | Ano ang Nagagawa? | Account Safety |
| :--- | :--- | :--- | :--- |
| **Mode 1: Shizuku Only (Safe Mode)** | **HINDI KAILANGAN** (Shizuku app lang) | Ni-la-lock ang totoong peak hardware Hz (`120Hz` / `144Hz` / `165Hz`), bina-bypass ang Transsion (HiOS) / OEM refresh rate & thermal caps, sine-set ang global `device_name`, ina-activate ang Android `GameManager` Performance Mode + `PowerHAL` CPU/GPU boost, at nag-AOT `speed` compile para walang frame drop sa clash. | **100% SAFE (Zero Ban Risk)** — Walang binabago sa MLBB APK. |
| **Mode 2: Shizuku + LSPatch (No-Root Deep Spoof)** | **OO** (Gamitin ang **LSPatch v0.6+** o **JingMatrix LSPatch** para sa Android 14–16) | Ini-inject ang `DeviceSpoofHook` sa loob ng MLBB para ma-spoof ang `Build.*`, `SystemProperties`, OpenGL GPU (`Adreno 750` / `Adreno 830`), at `Display.Mode` `165Hz`—para lumabas ang nakatagong **Ultra (120/144/165 FPS)** setting sa mga phones na hindi pa whitelisted ni Moonton. | **May Ban Risk sa Main Account** dahil napapalitan ang signature ng MLBB APK kapag naka-LSPatch. Gamitin muna sa smurf/dummy account. |
| **Mode 3: LSPosed + Shizuku (Rooted Devices)** | **HINDI** (LSPosed Manager ang gamit) | Systemless hook sa `com.mobile.legends` nang hindi binabago ang APK signature ng MLBB, kasabay ng Shizuku hardware Hz lock. | **Mas Safe kaysa LSPatch** (walang APK signature modification). |

---

## Paano Gamitin ang APK (Step-by-Step Guide)

### Paraan 1: Shizuku Only Mode (100% Legal / Safe sa Main Account — Walang LSPatch)
Gamitin ito kung meron ka nang **High / Super / Ultra Frame Rate** sa MLBB settings pero bumabagsak sa **60Hz / 90Hz** ang screen mo o nagla-lag sa 5v5 clash:

1. I-install ang **Shizuku** app at ang **[MLBB-165FPS-Booster-v1.0.0.apk](app/build/outputs/apk/debug/MLBB-165FPS-Booster-v1.0.0.apk)**.
2. Buksan ang **Shizuku** at i-start ito via **Wireless Debugging** (Developer Options $\rightarrow$ Wireless Debugging $\rightarrow$ Pair device with pairing code).
3. Buksan ang **MLBB 165FPS Booster** app:
   - Sa **Card 2**, i-tap ang **`REQUEST SHIZUKU PERMISSION`** at i-allow.
   - Sa **Card 3**, piliin ang gusto mong profile (`ASUS_AI2401_A`, `ASUS_AI2501`, o `NX769J`).
   - Sa **Card 2**, i-tap ang **`APPLY FULL SPOOF + REAL MAX FPS + BOOST (SHIZUKU)`** (o **`LOCK NATIVE HARDWARE PEAK`**).
   - I-tap ang **`AOT SPEED COMPILE + RAM & POWERHAL BOOST (ANTI-LAG)`** at hintayin matapos (`10–25 seconds`).
   - *(Optional)* Kung umiinit o kapos ang GPU sa clash, piliin ang **`90% Smooth`** o **`85% Max FPS`** sa Resolution Scaling row.
4. I-tap ang **`OPEN MOBILE LEGENDS`** at maglaro!

---

### Paraan 2: Full Deep Spoof via LSPatch (Para Lumabas ang Hidden Ultra 120/144/165 FPS Toggle, No Root)
Kung naka-lock lang sa **60 FPS (High)** o **90 FPS (Super)** ang MLBB settings mo at gusto mong palabasin ang **Ultra FPS**:

1. **I-download ang Latest LSPatch:**
   - Para sa **Android 10–13:** Official **LSPatch v0.6** (`manager-v0.6-398-release.apk`).
   - Para sa **Android 14 / 15 / 16:** Gamitin ang updated **LSPatch (JingMatrix / Vector fork v0.7+)** na may support sa Android 14–16 API 35.
2. **I-setup ang Shizuku + LSPatch:**
   - I-start ang **Shizuku** via Wireless Debugging at bigyan ng Shizuku permission ang **LSPatch** at **MLBB 165FPS Booster**.
3. **I-patch ang Mobile Legends sa LSPatch:**
   - Buksan ang **LSPatch** $\rightarrow$ pumunta sa **Manage** tab $\rightarrow$ i-tap ang **`+` (Add)** button.
   - Piliin ang **Select an installed app** $\rightarrow$ piliin ang **Mobile Legends: Bang Bang**.
   - Piliin ang **Patch Mode: `Local` (Recommended)** para pwede kang magpalit ng profile (`ROG Phone 8 Pro`, `ROG Phone 9 Pro`, `RedMagic 9 Pro`) kahit kailan nang hindi nagre-repatch!
   - *(Kung gusto mo ng standalone APK, piliin ang `Integrated` at i-embed ang `MLBB 165FPS Booster` module).*
   - I-tap ang **Start Patch** at pagkatapos ay i-tap ang **Install** (i-uninstall muna ang original MLBB kapag nag-prompt).
4. **I-attach ang Module sa LSPatch (`Local` Mode):**
   - Sa **LSPatch** $\rightarrow$ **Manage** $\rightarrow$ i-tap ang **Mobile Legends: Bang Bang** $\rightarrow$ **Module Scope** $\rightarrow$ i-check ang **`MLBB 165FPS Booster`**.
5. **I-activate ang Spoof at Real Hz Lock:**
   - Buksan ang **MLBB 165FPS Booster** app.
   - Piliin ang profile sa **Card 3**:
     - `ASUS_AI2401_A (ROG Phone 8 Pro — 165Hz / Adreno 750)`
     - `ASUS_AI2501 (ROG Phone 9 Pro — 165Hz / Adreno 830)`
     - `NX769J (RedMagic 9 Pro — 165Hz / Adreno 750)`
   - I-tap ang **`VERIFY & PREVIEW SPOOF PAYLOAD`** para makita ang active spoof values.
   - Sa **Card 2**, i-tap ang **`APPLY FULL SPOOF + REAL MAX FPS + BOOST (SHIZUKU)`** para i-lock ang physical screen mo sa max Hz at i-sync ang profile bridge.
   - Buksan ang **Mobile Legends** $\rightarrow$ pumunta sa **Settings $\rightarrow$ Frame Rate** at piliin ang **Ultra**!

> **Tandaan para sa Facebook / Google Login sa LSPatch:** Dahil iba ang signature ng patched APK, mag-login gamit ang **Moonton Account** (o mag-login sa Facebook via **Browser / Chrome** habang naka-disable/uninstall muna ang Facebook app).

---

### Paraan 3: Rooted Devices (LSPosed + Shizuku)
1. Buksan ang **LSPosed Manager** $\rightarrow$ **Modules** $\rightarrow$ i-enable ang **`MLBB 165FPS Booster`**.
2. I-check sa scope ang:
   - `Mobile Legends: Bang Bang` (`com.mobile.legends`)
   - `MLBB 165FPS Booster` (`com.fpsbooster.mlbb`)
3. Buksan ang **MLBB 165FPS Booster**, piliin ang profile sa **Card 3**, i-tap ang **`APPLY FULL SPOOF + REAL MAX FPS + BOOST (SHIZUKU)`** sa **Card 2**, at i-Force Stop + buksan ulit ang MLBB.

---

## Paliwanag sa Mga Buttons sa App

- **`APPLY FULL SPOOF + REAL MAX FPS + BOOST (SHIZUKU)`:** All-in-one button na nagsi-sync ng spoof profile bridge, nagla-lock ng exact native hardware peak Hz (`120Hz`/`144Hz`/`165Hz`), nagba-bypass ng Transsion/OEM thermal & refresh rate caps, at nagse-set ng Android `GameManager` Performance Mode + `PowerHAL` Fixed Performance Mode.
- **`LOCK NATIVE HARDWARE PEAK (<Hz> EXACT)`:** Direktang ni-la-lock ang physical panel sa pinakamataas na hardware `Display.Mode` na na-detect sa screen mo.
- **`AOT SPEED COMPILE + RAM & POWERHAL BOOST (ANTI-LAG)`:** Nagpapatakbo ng `cmd package compile -m speed -f com.mobile.legends` at `am kill-all` para walang JIT compilation stutter sa gitna ng 5v5 clash.
- **`100% Native` / `90% Smooth` / `85% Max FPS`:** Legal Android `GameManager` resolution scaling para bawasan ang GPU load at mapanatiling stable ang FPS sa mabibigat na teamfights.
- **`CHECK REAL FPS (GFXINFO) & SYSTEM CONFIG`:** Binabasa ang `dumpsys gfxinfo com.mobile.legends`, `cmd display`, at `cmd game` para makita ang totoong rendered frames at jank stats.
- **`VERIFY & PREVIEW SPOOF PAYLOAD`:** Ipinapakita sa console ang lahat ng `Build.*`, `SystemProperties`, `GL_VENDOR`, `GL_RENDERER`, at `Display.Mode` values na ipinapasa sa MLBB.
- **`RESET ALL TO DEFAULT`:** Ibinabalik sa default ang display refresh rate, `device_name`, thermal service, power mode, at game mode.

---

## Built-in 165Hz Flagship Profiles (`SpoofProfiles`)

- `ASUS_AI2401_A (ROG Phone 8 Pro — 165Hz / Adreno 750)`
- `ASUS_AI2501 (ROG Phone 9 Pro — 165Hz / Adreno 830)`
- `NX769J (RedMagic 9 Pro — 165Hz / Adreno 750)`

---

## Build Instructions

Requires **JDK 17** and **Gradle 8.6** (`compileSdk 35`, `minSdk 29`, `targetSdk 35`):

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 /opt/gradle-8.6/bin/gradle clean lintDebug assembleDebug
```
