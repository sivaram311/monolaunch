# monolaunch — AI-DLC Inception Charter

**Phase:** Inception (Day 1)
**Status:** Source generated, unbuilt — no Gradle/Android SDK run on this machine yet
**Next phase:** Construction (Bolt-sized diffs + evidence, starting with a real on-device build)

---

## Purpose

**monolaunch** is an ultra-minimalist Android home-screen launcher for AMOLED
displays (reference device: realme P2 Pro — `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`).
Pure pitch-black background, white-outline iconography, no wallpaper, no
blur — built for battery/power conservation on AMOLED panels and a
120Hz-smooth feel. Single-user, single-device, no backend, no account.

This is a **third, distinct direction** from two prior launcher efforts on
this machine, kept deliberately separate rather than merged:

- `E:\MyWorkspace\sandbox\my-realme-launcher` — first-generation XML-view
  launcher, shows the device wallpaper, explicitly superseded per its own
  README ("Product direction continues as ForgeCity").
- `E:\MyWorkspace\sandbox\forgecity-launcher` — a Compose isometric 3D
  "story" launcher, a different aesthetic and scope entirely.

monolaunch does not replace either; it is the AMOLED-minimalist spec the
user asked for, standalone.

---

## Scope for this Inception + first Construction pass

**In scope**

- Charter + ordered Bolt backlog (this doc + `BOLTS.md`)
- A modular Compose skeleton implementing the full first-cut UI: Home
  (analog clock, date, battery, 4 primary-app icons, search pill), App
  Drawer (FREQUENT row, alphabetical ALL APPS list, A-Z quick-scroll
  index), and the ViewModel driving Home ⇄ Drawer transitions
- HOME/DEFAULT intent-filter registration so the app is installable as a
  default launcher

**Explicitly out of scope for this pass**

- A real device/emulator build — this machine has no Android SDK or
  Gradle CLI installed (`ANDROID_HOME` unset, no `gradle` on `PATH`); the
  Gradle wrapper is present and the source is believed correct, but has
  **not been compiled or run**. First Construction Bolt is exactly this.
- Persisted frequent-app launch counts (session-only for now)
- Widgets, folders, icon packs, gestures beyond swipe-up, notification
  badges, backup/restore
- PREPROD / PROD promote — this machine's Q1/Q2 evidence-pack pipeline is
  written for web services; a local APK has no DEV/PREPROD/PROD drive
  topology or ports. See "Machine convention adaptations" below.

---

## Tech stack decision

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language | Kotlin | Required by spec |
| UI | Jetpack Compose + Material 3 | Required by spec; `androidx.compose.plugin.compose` Kotlin compiler plugin (Kotlin 2.2.20 requires it — the old `composeOptions.kotlinCompilerExtensionVersion` path is gone) |
| Min / target / compile SDK | 26 / 35 / 35 | Target matches spec (API 34/35); min 26 keeps modern Compose/adaptive-icon behavior without excluding the realme P2 Pro or similar recent devices |
| Build | AGP 8.11.1, Gradle 8.14, Kotlin 2.2.20 | Matches the proven-working versions already used by `my-realme-launcher` on this machine |
| Package / applicationId | `buzz.delena.monolaunch` | Matches this machine's `buzz.delena.*` convention for delena.buzz-associated apps |
| App enumeration | `PackageManager.queryIntentActivities` scoped to `ACTION_MAIN` / `CATEGORY_LAUNCHER` via `<queries>` | Deliberately **not** `QUERY_ALL_PACKAGES` — unnecessary for a launcher and restricted by Play policy (same reasoning `my-realme-launcher` already documented) |

This decision is **fixed** — do not re-decide during Construction.

---

## Architecture sketch

```
MainActivity (ComponentActivity)
  -> viewModels<LauncherViewModel>()
  -> setContent { MonolaunchTheme { LauncherApp(viewModel) } }
  -> back press: Drawer -> Home; Home -> moveTaskToBack (never finish())

LauncherApp (private composable, in MainActivity.kt)
  -> collects LauncherViewModel.uiState
  -> owns the swipe-up/down drag gesture + animateFloatAsState transition
  -> stacks HomeScreen + AppDrawerScreen, offsetting each by dragProgress

LauncherViewModel : AndroidViewModel
  -> LauncherUiState (screen, dragProgress, allApps, frequentApps,
     primaryApps, searchQuery, batteryPercent) as a single StateFlow
  -> loads installed apps off the main thread (Dispatchers.Default)
  -> resolves the 4 primary apps (phone/messages/camera/settings) via
     Intent resolution, not hardcoded packages
  -> registers/unregisters a battery BroadcastReceiver against
     Application context only (no Activity leak)
  -> launchApp() starts the target activity with FLAG_ACTIVITY_NEW_TASK

HomeScreen / AppDrawerScreen (stateless composables)
  -> pure functions of LauncherUiState + callbacks, no ViewModel reference
```

---

## Machine convention adaptations (documented, not silently skipped)

This machine's standing rules (`E:\MyAgent\workflow\CONSCIOUS.md`) are
written for networked web services. A local, offline Android launcher maps
onto them as follows — recorded explicitly per the "waiver must be
documented" principle (CONSCIOUS rule 5), not assumed:

| Rule | Standard meaning | monolaunch adaptation |
|------|-------------------|------------------------|
| Drive purposes (E/F/G/H) | DEV/PREPROD/PROD/RELEASES web deploys | N/A — no server process. Source lives on `E:` (DEV) only; a signed release APK/AAB is the eventual `H:`-equivalent artifact, not yet produced |
| Ports | 3000s/4000s/5000s reservation | N/A — no listening port, no `workflow/ports/REGISTRY.md` entry needed |
| Postgres schema-per-env | N/A | No database |
| CSS auth | Centralized Security System or documented waiver | **Waived** — single-device, no login, no user data leaves the device. Revisit only if monolaunch ever gains sync/cloud features |
| App name + version display (rule 24) | UI surface + `/api/version` | UI surface only: version shown via an About/Settings surface (Bolt 6). No API surface exists or makes sense for an offline launcher — `adb shell dumpsys package buzz.delena.monolaunch \| grep versionName` is the machine-readable equivalent |
| Q1/Q2 promote evidence pack | DEV→PREPROD→PROD GO/NO-GO | Replaced by: DEV Bolt review (this repo) → signed release build → physical-device checklist (mirrors `my-realme-launcher/docs/OPS.md`) → manual install. No automated promote pipeline for a sideloaded APK |

---

## Known risks / open questions

| Topic | Note |
|-------|------|
| Unbuilt | No Gradle/Android SDK on this machine as of Inception — first Construction Bolt must get a real `assembleDebug` (or equivalent, e.g. via Android Studio) before any of this is proven, not just believed-correct |
| Icon library coverage | `androidx.compose.material:material-icons-extended` is assumed to include `Sms`, `PhotoCamera`, `Mic`, `Add`, `Call`, `Settings` outlined variants — verify on first build |
| Compose BOM / AGP / Kotlin version compatibility | Versions chosen by matching the last proven-working sibling project (`my-realme-launcher`) plus one reasonably recent Compose BOM (`2024.10.01`); not independently re-verified against Google's compatibility map for this exact combination |
| Frequent-apps persistence | Session-only (in-memory `Map` in the ViewModel) — resets on process death; Bolt 2 |
| Touch handling during the Home⇄Drawer transition | Both screens are laid out simultaneously and offset via `graphicsLayer { translationY }`; hit-testing correctness while a drag is mid-flight has not been verified on a real device |
| Primary-app resolution | Uses `Intent` resolution (`ACTION_DIAL`, `smsto:`, `ACTION_IMAGE_CAPTURE`, `ACTION_SETTINGS`) rather than hardcoded packages, matching `my-realme-launcher`'s approach — should degrade gracefully (icon dims, tap no-ops) if a role has no handler, but untested |

---

## Initial Bolt backlog

See **[`BOLTS.md`](./BOLTS.md)** for the ordered Construction backlog.
