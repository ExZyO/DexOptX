# DexOptX

**DexOptX** is a modern, open-source Android and Android TV application designed to execute Ahead-of-Time (AOT) bytecode compilation (`dexopt`) on selected applications using the **Shizuku API** without requiring root access.

Designed with an immersive **Liquid Glass** aesthetic inspired by modern audiophile music players, DexOptX provides a responsive experience across both touch devices (such as the Samsung Galaxy S24 Ultra) and 10-foot television interfaces (such as the Nvidia Shield Pro).

---

## Features

- **Shizuku Native Integration:** Connects directly to Shizuku's Binder API to execute `cmd package compile` commands with elevated shell privileges. No root or bootloader unlocking required.
- **Dual Optimized Interfaces:**
  - **Phone (Touch):** Fullscreen edge-to-edge layout with a floating frosted-glass action dock.
  - **Android TV (D-pad Remote):** Dedicated 16:9 two-column layout with high-contrast D-pad focus indicators and leanback launcher support.
- **Liquid Glass Aesthetic:** Frosted glass cards, subtle ambient mesh background gradients, and clean vector iconography.
- **Strict Zero-Emoji Policy:** Clean typographic status pills (`[ COMPILED: SPEED ]`, `[ COMPILED: PROFILE ]`, `[ UNCOMPILED ]`) and monochrome Material vector icons.
- **Zero Truncation:** Displays full app labels and complete package identifiers without abbreviations or cutoffs.
- **Search & Intelligent Filters:** Instant real-time search by name or package ID, with filters for *All Apps*, *User Apps*, *System Apps*, and *Recently Updated* (updated within the last 7 days).
- **Flexible Optimization Profiles:**
  - `speed-profile`: Balanced Ahead-Of-Time compilation using profile-guided methods (recommended).
  - `speed`: Maximum performance with full AOT compilation of all methods.
- **Live Real-time Terminal Stream:** Real-time progress bar and live shell output logs during the compilation process.

---

## Requirements

1. **Android Device:** Android 8.0 (Oreo) or higher (tested up to Android 16 / One UI 8.5).
2. **Android TV (Optional):** Android TV 9.0 or higher (e.g., Nvidia Shield TV, Chromecast with Google TV).
3. **Shizuku:** The [Shizuku](https://shizuku.rikka.app/) service must be installed and active (started via Wireless Debugging or ADB).

---

## Installation

### On Phones & Tablets
1. Download the latest `DexOptX-release.apk` from the [Releases](https://github.com/antigravity/DexOptX/releases) tab.
2. Install the APK and launch **DexOptX**.
3. When prompted, grant Shizuku permission.

### On Nvidia Shield TV & Android TV
1. Transfer the APK to your Android TV via *Send Files to TV*, USB drive, or ADB:
   ```bash
   adb connect <SHIELD_IP_ADDRESS>
   adb install -r DexOptX-release.apk
   ```
2. DexOptX will appear directly in your Android TV home screen apps row.
3. Use the Nvidia Shield remote D-pad to navigate, toggle selections with **OK / Center**, and trigger optimization with **Play** or the on-screen action button.

---

## Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK (Platform 34+, Build Tools 34.0.0+)

### Build Steps
```bash
git clone https://github.com/antigravity/DexOptX.git
cd DexOptX
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Privacy & Security

- **Zero Telemetry:** No tracking libraries, analytics SDKs, or network calls.
- **Local Execution:** All compilation and package inspection occurs strictly on-device through the local Shizuku service.
- **Non-Destructive:** Does not modify application data, clear user credentials, or trip safety protections like Samsung Knox.

---

## License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.
