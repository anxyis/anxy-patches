# anxy-patches

Custom Morphe patches for Android applications by [@anxyis](https://github.com/anxyis).

## Usage with Morphe Manager

To use these patches in **Morphe Manager**:
1. Open **Morphe Manager** on your Android device.
2. Navigate to **Settings** > **Sources**.
3. Add custom patch source:
   ```text
   anxyis/anxy-patches
   ```
4. Select your target application APK and apply the desired patches!

---

## Supported Applications & Patches

### 🎬 Alight Motion / After Motion Z+ (`com.alightcreative.motion`)
- **Target Versions**: `5.0.273.1028426`, `5.0.273`
- **Patches Included**:
  - `AMZ Popup Suppression (Complete Suite)`: Master composite patch suppressing all intrusive startup, update, wizard, and modded-by dialogs.
  - `Native Server 1 Startup Gate Bypass`: Neutralizes native `SERVER 1` startup gate in `libsatriyaid.so`.
  - `Updates Required Popup Suppression`: No-ops framework update prompts (`fq.ab`).
  - `New Project Wizard Suppression`: Neutralizes project creation wizard (`zzzb.vbd`, `zzzb.vwp`, `zzzb.uio`).
  - `Modded By Satriyaid Dialog Suppression`: Neutralizes modded-by dialog (`zzw.xyz`).
  - `Seed Default Preferences`: Injects initial configuration seeds into the APK container.

---

## Repository Structure

```text
anxy-patches/
├── .github/workflows/          # Automated semantic-release CI/CD
├── extensions/                 # Android companion runtime DEX extensions
│   └── alightmotion/           # NoPopupSeedProvider & PopupDismisser daemon
├── patches/
│   └── src/main/kotlin/
│       └── anxyis/morphe/patches/
│           ├── all/            # Universal patches across apps
│           ├── alightmotion/   # Alight Motion / After Motion patches
│           └── shared/         # Reusable bytecode & binary helpers
```

---

## Building Locally

### Prerequisites
- JDK 17 or 21
- Android SDK Build-Tools

### Build Command
```bash
./gradlew :patches:build
```

### Run Tests
```bash
./gradlew test
```

---

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.
