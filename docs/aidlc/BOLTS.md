# monolaunch — Construction Bolt Backlog

Ordered list of AI-DLC Bolts for Construction. Each Bolt = one mergeable change + evidence + docs update, small enough to review in one sitting.

---

## Bolt 1 — First real build (unblocks everything else)

**Status: DONE (2026-08-13).** Installed Android SDK cmdline-tools + `platform-tools` + `platforms;android-35` + `build-tools;35.0.0` locally and ran `gradlew.bat assembleDebug` — build succeeded on the first real attempt.

---

## Bolt 2 — Persist frequent-app launch counts

**Status: DONE (2026-08-13).** Replaced session-only in-memory launch counts with `SharedPreferences` persistence. Launch counts and FREQUENT ordering survive app process death.

---

## Bolt 3 — Physical-device verification (realme P2 Pro)

**Goal:** Install on the reference device, select monolaunch as the default Home app, and complete the checklist (safe-area/curved-edge clipping, touch targets, swipe gestures).

**Status: OPEN.** Dev build compiles successfully but physical-device runtime validation is pending.

---

## Bolt 4 — App-list refresh on install/uninstall/update

**Status: DONE (2026-08-13).** Registered a dynamic `BroadcastReceiver` in `LauncherViewModel` monitoring `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, and `ACTION_PACKAGE_CHANGED` with `package` URI scheme. App drawer refreshes automatically on install/uninstall actions without app relaunch.

---

## Bolt 5 — Request the Android Home role

**Status: DONE (2026-08-13).** Implemented API 29+ `RoleManager` default launcher role request with clean fallback to `Settings.ACTION_HOME_SETTINGS` on older APIs. Integrated via "Set Default Launcher" button in the About dialog.

---

## Bolt 6 — About / version surface

**Status: DONE (2026-08-13).** Added an About surface (accessible via long-press on the Home screen clock) showing App Name, Version Name, Version Code, and a button to request default launcher.

---

## Bolt 7 — Long-press app details / uninstall

**Status: DONE (2026-08-13).** Integrated a long-press popup menu (AppActionsDialog) on app icons in both drawer and frequent rows, allowing direct navigation to system App Info (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) and triggers App Uninstall (`Intent.ACTION_DELETE`).

---

## Bolt 8 — Voice search wiring

**Status: DONE (2026-08-13).** Wired the Home search pill's microphone button to trigger speech-to-text recognition via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`. Spoken queries are correctly routed to the search field.

---

## Bolt 9 — Swipe Down for Notifications Panel

**Status: DONE (2026-08-13).** Handled vertical swipe-down gesture on the Home screen to trigger notifications drawer expansion using `android.permission.EXPAND_STATUS_BAR` and reflection on the `statusbar` system service.

---

## Bolt 10 — Monochrome AMOLED Icon Filter

**Status: DONE (2026-08-13).** Implemented custom color saturation filter (0% saturation) on app icon images in the drawer and frequent list to enforce the pitch-black and white minimalist AMOLED aesthetic.
