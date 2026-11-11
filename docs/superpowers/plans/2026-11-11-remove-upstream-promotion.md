# Remove Upstream Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove visible upstream promotion and outbound upstream links from the Android app while preserving functional remote services and documenting this repository as an independent Olauncher fork with clear credit.

**Architecture:** Delete promotional views and their event handlers at the UI boundary, then remove the now-unreachable dialog state and URL constants without changing wallpaper or other functional integrations. Treat repository attribution separately in the README so the app stays promotion-free while the fork remains transparent about its origin.

**Tech Stack:** Android XML resources, Kotlin, Android Gradle Plugin, JUnit, Markdown

**Spec:** `docs/superpowers/specs/2026-11-11-remove-upstream-promotion-design.md`

## Global Constraints

- Keep the product name `Olauncher` and package name `app.olauncher`.
- Preserve all pre-existing uncommitted changes, especially in `SettingsFragment.kt`, `strings.xml`, and contextual-suggestion files.
- Keep functional remote services, including the wallpaper feed and its default image URLs.
- Keep `LICENSE` unchanged and credit the original project and author in repository documentation.
- Do not present `douradorobert/Olauncher` as the official Olauncher distribution.
- Do not add dependencies or replace existing remote services.

## Review Focus

- A fresh install must not schedule review/share prompts after the wallpaper prompt; Task 2 pins the terminal user-state transition.
- Settings must contain no clickable upstream link or promotion while ordinary settings remain available; Task 1 validates removed IDs and retained functional IDs.
- Wallpaper updates must retain `URL_WALLPAPERS` and both default wallpaper URLs; Task 2 validates these constants remain.
- Removing XML views must not leave ViewBinding references or `when` branches that break compilation; Tasks 1 and 2 compile the debug sources.
- The fork README must credit upstream without directing installation to upstream binaries; Task 3 validates both properties.

---

### Task 1: Remove Promotional Settings UI

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/java/app/olauncher/ui/SettingsFragment.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml`

**Interfaces:**
- Consumes: Existing `FragmentSettingsBinding` generated from `fragment_settings.xml`.
- Produces: A Settings screen without `aboutOlauncher`, `moreFeatures`, `privacy`, `share`, `rate`, `github`, `twitter`, or `footer` views and without Kotlin references to those bindings/IDs.

- [ ] **Step 1: Record the failing static UI assertions**

Run:

```bash
rg -n 'android:id="@\+id/(aboutOlauncher|moreFeatures|privacy|share|rate|github|twitter|footer)"' app/src/main/res/layout/fragment_settings.xml
```

Expected before implementation: matches for all eight promotional IDs.

- [ ] **Step 2: Remove the promotional views from `fragment_settings.xml`**

Delete the top-level `aboutOlauncher` and `moreFeatures` TextViews, the promotional footer row (`privacy`, `share`, `rate`, `github`, `twitter`), and `footer`. Preserve `olauncherHiddenApps`, `appInfo`, backup/restore, and all functional preference controls.

- [ ] **Step 3: Remove corresponding click behavior from `SettingsFragment.kt`**

Delete the eight `onClick` branches and eight `setOnClickListener` registrations. Remove imports used only by `shareApp()` or `rateApp()` if they become unused. Do not modify contextual suggestions, weather, agenda, backup, or daily-wallpaper behavior.

- [ ] **Step 4: Remove base and pt-BR strings used only by deleted UI**

Remove `github`, `olauncher_pro`, `new_app_note_safe`, `new_app_minimal_todo_lists`, `x_twitter`, `privacy`, `share`, `rate`, `pro_message`, `about_olauncher`, and `more_features` only after confirming each has no remaining code/resource reference. Leave a string in place if it still supports a functional flow scheduled for Task 2.

- [ ] **Step 5: Verify Settings no longer exposes promotion and retains functional controls**

Run:

```bash
! rg -n 'android:id="@\+id/(aboutOlauncher|moreFeatures|privacy|share|rate|github|twitter|footer)"|binding\.(aboutOlauncher|moreFeatures|privacy|share|rate|github|twitter|footer)|R\.id\.(aboutOlauncher|moreFeatures|privacy|share|rate|github|twitter|footer)' app/src/main/res/layout/fragment_settings.xml app/src/main/java/app/olauncher/ui/SettingsFragment.kt
rg -n 'android:id="@\+id/(olauncherHiddenApps|appInfo|exportSettings|importSettings|dailyWallpaper)"' app/src/main/res/layout/fragment_settings.xml
./gradlew compileDebugKotlin
```

Expected: first command succeeds with no matches; second finds all five functional controls; Gradle reports `BUILD SUCCESSFUL`.

- [ ] **Step 6: Review and checkpoint only Task 1 changes**

Run `git diff --check` and inspect `git diff` for the four files. Because these files already contain user changes, do not stage or commit without explicit confirmation that the diff separates cleanly.

### Task 2: Remove Automatic Promotion Flows and Dead Upstream URLs

**Files:**
- Modify: `app/src/main/java/app/olauncher/MainActivity.kt`
- Modify: `app/src/main/java/app/olauncher/data/Constants.kt`
- Modify: `app/src/main/java/app/olauncher/data/Prefs.kt` only if obsolete promotion preferences can be removed without migration risk
- Modify: `app/src/main/java/app/olauncher/helper/Utils.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml`
- Test: `app/src/test/java/app/olauncher/data/PromotionReferenceTest.kt`

**Interfaces:**
- Consumes: `Constants.UserState`, `Prefs.userState`, and functional wallpaper constants.
- Produces: A message scheduler whose post-wallpaper state is terminal and no longer launches review/share/store flows; functional wallpaper URLs remain unchanged.

- [ ] **Step 1: Add a failing source-contract test**

Create `PromotionReferenceTest` that reads the repository sources and asserts:

- `MainActivity.kt` contains no `Dialog.REVIEW`, `Dialog.RATE`, `Dialog.SHARE`, or `Dialog.PRO_MESSAGE` branch/reference.
- `Constants.kt` contains no `URL_ABOUT_OLAUNCHER`, `URL_OLAUNCHER_PRIVACY`, `URL_OLAUNCHER_GITHUB`, `URL_OLAUNCHER_PLAY_STORE`, `URL_OLAUNCHER_PRO`, `URL_PLAY_STORE_DEV`, `URL_TWITTER_TANUJ`, `URL_NTS`, or `URL_PENTASTIC` declaration.
- `Constants.kt` still contains `URL_WALLPAPERS`, `URL_DEFAULT_DARK_WALLPAPER`, and `URL_DEFAULT_LIGHT_WALLPAPER`.
- `Utils.kt` contains no `URL_OLAUNCHER_PLAY_STORE` use.

- [ ] **Step 2: Run the contract test and confirm it fails**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.PromotionReferenceTest`

Expected: FAIL because promotional dialog cases and URL constants still exist.

- [ ] **Step 3: Remove promotional dialog scheduling from `MainActivity.kt`**

Delete review, rate, share, and Pro Launcher dialog handling. Make the wallpaper flow terminal after it is shown/enabled rather than advancing into a promotional state. Retain New Year, wallpaper, hidden-apps, keyboard, and Digital Wellbeing messages.

- [ ] **Step 4: Remove dead promotional URL constants and helpers**

Delete constants listed by the test only when no remaining reference exists. Remove or adapt `shareApp()` and `rateApp()` in `Utils.kt` after Task 1 and the scheduler no longer call them. Keep wallpaper, DuckDuckGo, default-image, and Digital Wellbeing URLs.

- [ ] **Step 5: Remove newly dead promotional strings and safe preference fields**

Delete base and pt-BR strings for automatic review/share/Pro prompts once unused. Keep serialized preference keys and enum values when deleting them would introduce backup or upgrade compatibility risk; unreachable compatibility fields are preferable to a migration in this task.

- [ ] **Step 6: Run focused and compile verification**

Run:

```bash
./gradlew testDebugUnitTest --tests app.olauncher.data.PromotionReferenceTest
./gradlew compileDebugKotlin
```

Expected: test PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 7: Review and checkpoint only Task 2 changes**

Run `git diff --check` and inspect the touched-file diff. Do not stage overlapping pre-existing user hunks without explicit confirmation.

### Task 3: Reframe Repository Documentation

**Files:**
- Modify: `README.md`
- Preserve: `LICENSE`

**Interfaces:**
- Consumes: Repository identity `douradorobert/Olauncher`, upstream identity `tanujnotes/Olauncher`, and GPLv3.
- Produces: A Portuguese README that describes an independent modified fork, credits upstream, and points repository activity to this fork.

- [ ] **Step 1: Record failing README assertions**

Run:

```bash
rg -n 'Other apps from team Olauncher|play.google.com/store/apps/details\?id=app\.prolauncher|latest APK.*tanujnotes/Olauncher/releases' README.md
```

Expected before implementation: matches identifying upstream distribution and cross-promotion.

- [ ] **Step 2: Rewrite `README.md` in Portuguese**

Include: project summary; explicit “fork independente e modificado” notice; current fork features/differences; local build instructions using `./gradlew assembleDebug`; repository/release links under `douradorobert/Olauncher`; GPLv3 license; and a credits section linking `https://github.com/tanujnotes/Olauncher` and `https://x.com/tanujnotes`, stating that the base and most original work belong to Tanuj and upstream contributors. Remove official store badges, upstream APK instructions, upstream About link, and the other-apps section.

- [ ] **Step 3: Verify attribution and absence of upstream installation promotion**

Run:

```bash
rg -n 'fork independente|tanujnotes/Olauncher|GPLv3|douradorobert/Olauncher' README.md
! rg -n 'Other apps from team Olauncher|app\.prolauncher|com\.makenotetoself|app\.pentastic|tanujnotes/Olauncher/releases' README.md
git diff --check
```

Expected: required fork/credit/license references match; promotional/install references have no matches; diff check exits successfully.

- [ ] **Step 4: Review and checkpoint Task 3**

Inspect `git diff -- README.md LICENSE` and confirm `LICENSE` has no diff. Stage/commit only if the user authorizes commits.

### Task 4: Whole-Change Verification

**Files:**
- Verify all modified files from Tasks 1–3

**Interfaces:**
- Consumes: Promotion-free app UI/flows and rewritten fork documentation.
- Produces: Evidence that the complete change builds, tests, retains functional services, and does not overwrite unrelated work.

- [ ] **Step 1: Run the unit test suite**

Run: `./gradlew testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 2: Build the debug APK**

Run: `./gradlew assembleDebug`

Expected: `BUILD SUCCESSFUL` and a debug APK under `app/build/outputs/apk/debug/`.

- [ ] **Step 3: Search for remaining visible upstream promotion**

Run:

```bash
rg -n 'URL_(ABOUT_OLAUNCHER|OLAUNCHER_PRIVACY|OLAUNCHER_GITHUB|OLAUNCHER_PLAY_STORE|OLAUNCHER_PRO|PLAY_STORE_DEV|TWITTER_TANUJ|NTS|PENTASTIC)|Pro Launcher|Note to Self|Pentastic|X/Twitter|leave a 5-star review|share Olauncher' app/src/main
```

Expected: no executable or base-resource matches. Matches in untranslated locale files are acceptable only when the resource key is unreachable and removing every stale translation would create disproportionate churn.

- [ ] **Step 4: Confirm functional remote services remain**

Run:

```bash
rg -n 'URL_WALLPAPERS|URL_DEFAULT_DARK_WALLPAPER|URL_DEFAULT_LIGHT_WALLPAPER|URL_DIGITAL_WELLBEING_LEARN_MORE' app/src/main/java
```

Expected: wallpaper constants and their consumers remain. The Digital Wellbeing help URL may remain because it documents a functional feature rather than promoting the upstream project.

- [ ] **Step 5: Audit the final diff against pre-existing work**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Expected: no whitespace errors; only intended promotion/documentation hunks plus the user's pre-existing contextual-suggestion work. Report verification results and any intentionally retained unreachable compatibility strings/fields.

