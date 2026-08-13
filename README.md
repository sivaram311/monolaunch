# Monolaunch

An ultra-minimalist Android home-screen launcher for AMOLED displays
(reference device: realme P2 Pro). Pure pitch-black background, white
outline iconography, no wallpaper, no blur — built for AMOLED power
conservation and a smooth 120Hz feel.

See [`docs/aidlc/INCEPTION.md`](docs/aidlc/INCEPTION.md) for the full
charter (scope, tech-stack decisions, architecture, known risks) and
[`docs/aidlc/BOLTS.md`](docs/aidlc/BOLTS.md) for the ordered Construction
backlog — start there before making changes.

## Status

**Unbuilt.** Source was generated against the versions already proven on
this machine (AGP 8.11.1, Kotlin 2.2.20, Gradle 8.14 — see
`my-realme-launcher` for precedent), but this machine has no Android SDK
or Gradle CLI installed, so nothing here has been compiled or run yet.
Bolt 1 in `docs/aidlc/BOLTS.md` is exactly that first real build.

## Stack

Kotlin, Jetpack Compose + Material 3, min SDK 26 / target & compile SDK 35.

## Project layout

```
app/src/main/java/buzz/delena/monolaunch/
  MainActivity.kt              # activity, back-press handling, Home<->Drawer orchestration
  model/AppInfo.kt             # AppInfo / PrimaryApps data classes
  viewmodel/LauncherViewModel.kt
  ui/HomeScreen.kt
  ui/AppDrawerScreen.kt
  ui/components/AnalogClock.kt # Canvas-drawn clock
  ui/theme/                    # Color.kt, Type.kt, Theme.kt
```

## Build

Requires Android Studio (or Android SDK Platform 35 + `ANDROID_HOME` set
for CLI use) and JDK 17+.

```powershell
.\gradlew.bat assembleDebug
```

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Open **Monolaunch** and accept the home-app prompt, or set it manually
under **Settings → Apps → Default apps → Home app**.

## Relationship to other launcher projects on this machine

This is a **third, separate** launcher effort, kept intentionally distinct:

- `E:\MyWorkspace\sandbox\my-realme-launcher` — first-generation XML-view
  launcher (shows wallpaper); its own README says product direction moved
  to ForgeCity.
- `E:\MyWorkspace\sandbox\forgecity-launcher` — Compose isometric 3D
  "story" launcher; a different aesthetic and scope.

Monolaunch is the AMOLED-minimalist spec, standalone — see
`docs/aidlc/INCEPTION.md` for the full reasoning.

## Release boundary

DEV-only. No port, database, CSS auth client, or PREPROD/PROD deploy — a
local launcher APK has no such topology (documented in
`docs/aidlc/INCEPTION.md` under "Machine convention adaptations"). A
signed release build and physical-device evidence are still required
before real-world use as a daily-driver launcher.
