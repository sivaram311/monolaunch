# monolaunch — Construction Bolt Backlog

Ordered list of AI-DLC Bolts for Construction. Each Bolt = one mergeable
change + evidence + docs update, small enough to review in one sitting.

---

## Bolt 1 — First real build (unblocks everything else)

**Status: DONE (2026-08-13).** Installed Android SDK cmdline-tools +
`platform-tools` + `platforms;android-35` + `build-tools;35.0.0` locally
(`E:\Android\sdk`, `ANDROID_HOME` not set machine-wide — `local.properties`
points at it, gitignored) and ran `gradlew.bat assembleDebug` — **build
succeeded on the first real attempt**, no compile errors (the icon-name
and Compose BOM/AGP/Kotlin-compatibility risks flagged in INCEPTION.md
did not materialize). Output: `app/build/outputs/apk/debug/app-debug.apk`
(~16 MB). `aapt dump badging` confirms `package=buzz.delena.monolaunch`,
`versionCode=1`, `versionName=0.1.0`, `application-label=Monolaunch`.

**Not yet done:** install + run on a real or emulated device — this Bolt
only proves it *compiles and packages*, not that it *runs* correctly
(Home/Drawer transition, gesture handling, icon rendering are all
unverified at runtime). See Bolt 3.

**Acceptance (compile-time half): met.** Runtime acceptance
(`adb install -r`; app launches and shows the Home screen without
crashing) still open — tracked under Bolt 3.

---

## Bolt 2 — Persist frequent-app launch counts

**Goal:** Replace the session-only in-memory launch-count map with
`SharedPreferences` (or DataStore) persistence so the FREQUENT row
survives process death.

**Acceptance:** Launch counts and FREQUENT ordering survive a force-stop
+ relaunch; unit test for the persistence layer.

---

## Bolt 3 — Physical-device verification (realme P2 Pro)

**Goal:** Install on the reference device (or closest available), select
monolaunch as the default Home app, and complete a checklist mirroring
`my-realme-launcher/docs/OPS.md`: safe-area/curved-edge clipping at
360×780 logical viewport, touch target sizes, swipe-up open / swipe-down
close, search + launch, app install/uninstall refresh, reboot retains
selection.

**Acceptance:** Checklist recorded in `docs/OPS.md` (new file, same
pattern as the sibling launcher project); any layout issues found are
filed as follow-up Bolts, not silently patched without a note.

---

## Bolt 4 — App-list refresh on install/uninstall/update

**Goal:** Register a `PackageManager` / `BroadcastReceiver` for
`ACTION_PACKAGE_ADDED` / `_REMOVED` / `_REPLACED` (or the modern
`LauncherApps` callback API) so `LauncherViewModel.loadApps()` re-runs
automatically instead of only at process start.

**Acceptance:** Installing or uninstalling an app while monolaunch is
running (Home in background) updates the drawer without a manual
relaunch; receiver unregistered in `onCleared()` — no leak.

---

## Bolt 5 — Request the Android Home role

**Goal:** On API 29+, use `RoleManager` to request `ROLE_HOME` explicitly
(rather than relying solely on the HOME/DEFAULT intent-filter + system
chooser), matching `my-realme-launcher`'s approach.

**Acceptance:** Tapping a "Set as default launcher" affordance (or first
Home-button press) triggers the system role request flow where supported;
falls back cleanly pre-API 29.

---

## Bolt 6 — About / version surface

**Goal:** Add a minimal About surface (e.g. long-press the clock, matching
`my-realme-launcher`'s long-press pattern) showing app name + `versionName`
+ `versionCode`, satisfying this machine's CONSCIOUS rule 24 UI-surface
requirement (see INCEPTION.md "Machine convention adaptations" for why no
API surface applies here).

**Acceptance:** Name + version visible without dev tools; matches
`BuildConfig.VERSION_NAME` / `VERSION_CODE`.

---

## Bolt 7 — Long-press app details / uninstall

**Goal:** Long-press on an app in the drawer (or FREQUENT row) opens the
system App Info screen (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`),
matching standard launcher affordances and `my-realme-launcher`'s existing
behavior.

**Acceptance:** Long-press opens the correct app's details page; verified
on-device.

---

## Bolt 8 — Voice search wiring

**Goal:** The Home search pill's mic icon currently only renders (no
action). Wire it to `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (or a
documented deferral if no speech-recognition intent resolves on the
target device).

**Acceptance:** Tapping the mic either launches system speech recognition
and routes the result into the drawer search query, or — if deferred —
this Bolt records that decision explicitly rather than shipping a dead
button silently.

---

## Bolt 9 — Auth / sync decision checkpoint

**Goal:** Before any future feature that syncs data off-device (backup,
cross-device settings, etc.), revisit the CSS waiver recorded in
INCEPTION.md and either integrate CSS or re-document the waiver for that
specific feature.

**Acceptance:** Decision recorded under `docs/aidlc/`; no silent
no-auth assumption for a network-facing feature. Not expected to trigger
soon — monolaunch is offline-first by design.
