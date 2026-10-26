# CleanYourTV

A clean, dynamic, tvOS-style home screen for Android TV, plus an adb script that removes ads/tracking bloat.
Built around a **Xiaomi Mi Box 4 (Android TV 9, API 28)**.

## What you get
- **Launcher** (Kotlin + Jetpack Compose): clock, hero title, backdrop glow that follows the focused app's colour,
  *Favorites* (hold OK to pin/unpin), *Recently used*, *All apps*, Settings tile.
- **`tvctl`** scripts (`tvctl.sh`, `tvctl.ps1`): install the launcher, set it as Home, remove bloat, restore everything.
- **GitHub Actions** builds the APK on every push (Actions tab > latest run > Artifacts).

## Steps
1. Push this folder to the repo. Wait for the **Build APK** workflow, download `cleanyourtv-debug-apk`, unzip.
2. TV connected over adb, then:
   ```
   # Windows PowerShell
   .\tvctl.ps1 -Ip 192.168.1.34 -Mode install -Apk app-debug.apk
   .\tvctl.ps1 -Ip 192.168.1.34 -Mode safe
   # macOS / Linux / Git Bash
   ./tvctl.sh 192.168.1.34 install app-debug.apk
   ./tvctl.sh 192.168.1.34 safe
   ```
3. Press Home on the remote and check the launcher works. Only then:
   ```
   .\tvctl.ps1 -Ip 192.168.1.34 -Mode stock-launchers
   ```
4. Optional extras: edit `debloat/optional.txt`, then run mode `optional`.

## Undo
`tvctl restore-all` reinstalls every package listed in the three files. A factory reset also brings everything back.

## Notes
- Removals use `pm uninstall -k --user 0`: no root, system apps stay on disk.
- Never remove: `mitv.service`, `com.droidlogic*`, `com.google.android.gms/gsf`, `com.android.vending`, `com.android.systemui`, `com.android.tv.settings`, `com.google.android.katniss` (voice), `com.google.android.apps.mediashell` (Chromecast).
- Blur is skipped on purpose: `Modifier.blur` needs Android 12+, the Mi Box 4 is Android 9.
- Not yet compiled or tested on a real TV. If the first CI build fails, paste the log and it gets fixed.
