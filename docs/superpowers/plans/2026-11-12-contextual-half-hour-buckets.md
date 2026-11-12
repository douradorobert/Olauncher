# Contextual Half-Hour Buckets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record contextual app launches in 30-minute buckets and score nearby times with a circular three-hour weighting window while migrating existing history safely to version 3.

**Architecture:** Keep temporal aggregation and scoring in `ContextualUsageHistory`, change persisted evidence from integer counts to decimal launch mass, and let the codec own all v2/legacy migration. Thread minute-of-hour through the repository and preference boundary so Android callers capture hour and minute from one `Calendar`, while `ContextualSuggestions` continues consuming only an integer score.

**Tech Stack:** Kotlin, Android SharedPreferences, `org.json`, JUnit 4, Gradle

**Spec:** `docs/superpowers/specs/2026-11-12-contextual-half-hour-buckets-design.md`

## Global Constraints

- Use exactly 48 half-hour buckets numbered `0..47`.
- Use temporal weights `1.0, 0.75, 0.5, 0.375, 0.25, 0.125, 0.0` for distances `0, 1, 2, 3, 4, 5, >=6`.
- Bucket distance is circular across midnight.
- Keep ten points per exact recent launch, seven-day half-life, thirty-day retention, and the existing twenty-point suggestion threshold.
- Keep `ContextualSuggestions` independent of persistence and temporal-bucket details.
- Version 3 migration distributes each old four-hour count equally across its eight covered half-hour buckets and rewrites once.
- Preserve settings-backup compatibility for the `CONTEXTUAL_HISTORY` preference.
- Add no dependencies.

## Review Focus

- Minute values exactly at `00`, `29`, `30`, and `59` must select the correct bucket; Task 1 pins every boundary.
- A query around midnight must treat bucket `47` and bucket `0` as adjacent, not 47 buckets apart; Task 1 covers both directions.
- Temporal weighting and age decay must multiply before a single final rounding; Task 1 covers fractional sums.
- Version 2 and unversioned histories must conserve total launch mass while diluting time confidence; Task 2 checks all eight migrated entries and their sum.
- Android callers must not obtain hour and minute from different clock reads near a half-hour boundary; Task 3 captures one `Calendar` per operation.

---

### Task 1: Half-Hour History Model and Weighted Scoring

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt`
- Modify: `app/src/test/java/app/olauncher/data/ContextualUsageHistoryTest.kt`

**Interfaces:**
- Produces: `ContextualUsageEntry(appIdentity: String, timeBucket: Int, epochDay: Long, launchMass: Double)`.
- Produces: `timeBucket(hour: Int, minute: Int): Int`.
- Produces: `temporalWeight(sourceBucket: Int, targetBucket: Int): Double`.
- Produces: `record(entries, appIdentity, hour, minute, epochDay): List<ContextualUsageEntry>`.
- Produces: `score(entries, appIdentity, hour, minute, currentEpochDay): Int`.

- [ ] **Step 1: Replace bucket/model tests with failing half-hour tests**

Add tests asserting:

- `timeBucket(0, 0) == 0`, `timeBucket(0, 29) == 0`, `timeBucket(0, 30) == 1`, `timeBucket(10, 20) == 20`, and `timeBucket(23, 59) == 47`.
- Coercion maps negative hour/minute to bucket `0` and oversized values to bucket `47`.
- `temporalWeight(20, target)` returns exact values `1.0, 0.75, 0.5, 0.375, 0.25, 0.125, 0.0` at distances `0..6`.
- `temporalWeight(47, 0) == 0.75` and `temporalWeight(0, 47) == 0.75`.
- A single launch at 10h20 scores `10, 8, 5, 4, 3, 1, 0` when queried at bucket distances `0..6`.
- Two contributions of `2.5` points sum to `5` rather than rounding individually.
- Existing age, pruning, invalid-entry, blank-identity, and saturation behavior still holds with `launchMass`.

- [ ] **Step 2: Run history tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryTest`

Expected: FAIL because the entry model and APIs still use integer count and four-hour buckets.

- [ ] **Step 3: Implement the half-hour model and kernel**

In `ContextualUsageHistory.kt`:

- replace `count: Int` with `launchMass: Double`;
- implement `timeBucket(hour, minute)` as `safeHour * 2 + safeMinute / 30`;
- implement circular bucket distance and the exact weight table from Global Constraints;
- increment matching entries by `1.0`, saturating at `Int.MAX_VALUE.toDouble()`;
- validate finite positive mass and bucket range `0..47`;
- multiply launch mass, ten points, temporal weight, and age decay before summing and rounding once.

- [ ] **Step 4: Run focused history tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryTest`

Expected: PASS.

- [ ] **Step 5: Commit Task 1**

```bash
git add app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt app/src/test/java/app/olauncher/data/ContextualUsageHistoryTest.kt
git commit -m "feat: score contextual usage in half-hour buckets"
```

### Task 2: Version 3 Persistence and Four-Hour Migration

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageHistoryCodec.kt`
- Modify: `app/src/test/java/app/olauncher/data/ContextualUsageHistoryCodecTest.kt`

**Interfaces:**
- Consumes: Task 1 `ContextualUsageEntry.launchMass` and valid buckets `0..47`.
- Produces: v3 JSON shaped as `{"version":3,"entries":[{"app":...,"bucket":...,"day":...,"mass":...}]}`.
- Produces: `decode(serialized, migrationEpochDay)` accepting v3, v2, and unversioned legacy history.

- [ ] **Step 1: Write failing codec and migration tests**

Add tests asserting:

- v3 round-trip preserves decimal mass and stable app/bucket/day ordering without requiring rewrite;
- invalid v3 entries (blank app, bucket `48`, zero/negative/non-finite/non-numeric mass) are discarded while valid siblings remain;
- a v2 entry `bucket=2,count=8` becomes buckets `16..23`, each with mass `1.0`, preserving total mass `8.0` and requiring rewrite;
- a v2 entry with `count=2` becomes eight entries of mass `0.25`;
- unversioned `"spotify|user#3":4` becomes buckets `24..31`, each with mass `0.5`;
- malformed/unknown versions recover as empty rewriteable v3 history;
- encode-after-migration decodes again without another rewrite.

- [ ] **Step 2: Run codec tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryCodecTest`

Expected: FAIL because the codec still emits v2 integer counts.

- [ ] **Step 3: Implement v3 encoding, validation, and migration**

Bump `VERSION` to `3`. Decode v3 mass strictly from JSON `Number`, requiring a finite value greater than zero. For v2 and legacy input, validate old bucket `0..5` and positive integer count, then emit eight entries starting at `oldBucket * 8`, each with `launchMass = count / 8.0`. Mark every migration or discarded entry for rewrite.

- [ ] **Step 4: Run focused codec tests and verify GREEN**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryCodecTest`

Expected: PASS.

- [ ] **Step 5: Run all history tests**

Run: `./gradlew testDebugUnitTest --tests 'app.olauncher.data.ContextualUsageHistory*'`

Expected: PASS.

- [ ] **Step 6: Commit Task 2**

```bash
git add app/src/main/java/app/olauncher/data/ContextualUsageHistoryCodec.kt app/src/test/java/app/olauncher/data/ContextualUsageHistoryCodecTest.kt
git commit -m "feat: migrate contextual history to version three"
```

### Task 3: Minute-Aware Repository and Android Callers

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageRepository.kt`
- Modify: `app/src/main/java/app/olauncher/data/Prefs.kt`
- Modify: `app/src/main/java/app/olauncher/MainViewModel.kt`
- Modify: `app/src/main/java/app/olauncher/ui/HomeFragment.kt`
- Modify: `app/src/test/java/app/olauncher/data/ContextualUsageRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 1 `record`/`score` APIs and Task 2 codec.
- Produces: `updateSerializedHistory(serialized, appIdentity, hour, minute, nowMillis, timeZone, recordLaunch = false)`.
- Produces: `Prefs.recordContextualLaunch(appIdentity, hour, minute, nowMillis, timeZone)`.
- Produces: `Prefs.contextualUsageScore(appIdentity, hour, minute, nowMillis, timeZone): Int`.

- [ ] **Step 1: Write failing repository tests for minute selection and one-time migration**

Add tests asserting:

- recording at `10:29` creates bucket `20`, while recording at `10:30` creates bucket `21`;
- a v2 history read returns `changed=true` and v3 JSON, then the identical second read returns `changed=false`;
- an exact-time new launch scores ten points, its adjacent half-hour scores eight, and a three-hour-distant query scores zero;
- pruning still retains day 30 and removes day 31 using decimal mass;
- corrupt history rewrites to valid empty v3 JSON.

- [ ] **Step 2: Run repository tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageRepositoryTest`

Expected: FAIL because minute is not accepted or forwarded.

- [ ] **Step 3: Thread minute through repository and preferences**

Add `minute: Int` immediately after `hour: Int` in repository and `Prefs` APIs, and pass it to Task 1 record/score functions. Keep `nowMillis` and `timeZone` injection unchanged for deterministic tests.

- [ ] **Step 4: Capture one Calendar per Android operation**

In `MainViewModel`, capture one `Calendar.getInstance()` after a successful app launch and pass both `HOUR_OF_DAY` and `MINUTE` to `recordContextualLaunch`, including the fallback-user path. In `HomeFragment.refreshContextualSuggestions`, capture one calendar before candidate mapping and pass its hour/minute to every `contextualUsageScore` call.

- [ ] **Step 5: Run focused repository tests and compile Android callers**

Run:

```bash
./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageRepositoryTest
./gradlew compileDebugKotlin
```

Expected: repository tests PASS and compilation reports `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit Task 3**

```bash
git add app/src/main/java/app/olauncher/data/ContextualUsageRepository.kt app/src/main/java/app/olauncher/data/Prefs.kt app/src/main/java/app/olauncher/MainViewModel.kt app/src/main/java/app/olauncher/ui/HomeFragment.kt app/src/test/java/app/olauncher/data/ContextualUsageRepositoryTest.kt
git commit -m "feat: pass minute precision to contextual history"
```

### Task 4: Whole-Feature Verification

**Files:**
- Verify all files changed in Tasks 1–3
- Modify only if verification exposes a defect

**Interfaces:**
- Consumes: v3 persistence, weighted half-hour scoring, and minute-aware Android callers.
- Produces: Green unit suite and build evidence for the complete feature.

- [ ] **Step 1: Run all contextual tests**

Run: `./gradlew testDebugUnitTest --tests 'app.olauncher.data.Contextual*'`

Expected: PASS with no contextual test failures.

- [ ] **Step 2: Run the complete unit suite**

Run: `./gradlew testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` with zero test failures.

- [ ] **Step 3: Build the debug APK**

Run: `./gradlew assembleDebug`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Audit persistence and caller contracts**

Run:

```bash
rg -n 'VERSION = 3|"mass"|timeBucket\(hour: Int, minute: Int\)|temporalWeight' app/src/main/java/app/olauncher/data
rg -n 'Calendar\.MINUTE|contextualUsageScore\(|recordContextualLaunch\(' app/src/main/java/app/olauncher
! rg -n 'val count: Int|"count"' app/src/main/java/app/olauncher/data/ContextualUsageHistory*.kt
git diff --check
```

Expected: v3/mass/minute contracts are present, production history code has no v2 count field, and diff check is clean.

- [ ] **Step 5: Inspect branch state**

Run:

```bash
git status --short
git log --oneline develop..HEAD
```

Expected: no uncommitted implementation files; commits are limited to the approved design, plan, and Tasks 1–3.
