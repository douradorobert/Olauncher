# Configurable Contextual Scoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document contextual suggestion scoring and let the user configure its minimum score and optional weekday/weekend learning profiles.

**Architecture:** Keep contextual history JSON at version 3 and derive day profiles from the persisted local `epochDay` during scoring. Thread two ordinary preferences through the existing repository, ranking, home, and settings boundaries; use a dedicated ViewBinding dialog for the score stepper and information action.

**Tech Stack:** Kotlin, Android XML/ViewBinding, SharedPreferences, JUnit, Markdown, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-15-configurable-contextual-scoring-design.md`

## Global Constraints

- Preserve JSON v3 and all existing history; do not persist a redundant weekday/weekend field.
- Default behavior remains unchanged: threshold `20`, profile separation off.
- Threshold values are `0..200` in steps of `10` and apply after all bonuses.
- Weekdays are Monday through Friday; weekends are Saturday and Sunday in the device's local civil-day history.
- Update permanent user documentation in the same implementation, as required by `AGENTS.md`.

## Review Focus

- Modular weekday calculation must remain correct for negative epoch days and near week boundaries.
- Profile separation must filter scoring only, never recording, pruning, encoding, or migration.
- Toggle-off scoring must remain byte-for-byte persistence-compatible and numerically identical to current behavior.
- A threshold of `200` must still permit a matching explicit rule with zero history; malformed stored values must normalize safely.
- Configuration controls and their info dialog must be accessible and disabled with contextual suggestions.

---

### Task 1: Weekday/weekend scoring profiles

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt`
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageRepository.kt`
- Test: `app/src/test/java/app/olauncher/data/ContextualUsageHistoryTest.kt`
- Test: `app/src/test/java/app/olauncher/data/ContextualUsageRepositoryTest.kt`

**Interfaces:**
- Produces: `ContextualUsageHistory.isWeekend(epochDay: Long): Boolean`.
- Produces: optional `separateDayProfiles: Boolean = false` on `ContextualUsageHistory.score(...)` and `ContextualUsageRepository.updateSerializedHistory(...)`.

- [ ] Add failing tests proving epoch day `0` is a weekday, `2/3` are weekend, `4` is weekday, negative days wrap correctly, toggle-off combines all records, and toggle-on isolates weekday and weekend histories in both directions.
- [ ] Run `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryTest --tests app.olauncher.data.ContextualUsageRepositoryTest`; expect failures because profile-aware APIs do not exist.
- [ ] Implement `isWeekend` with safe modulo arithmetic and filter score entries only when `separateDayProfiles` is true; forward the flag through the repository without changing record, prune, codec, or serialized output.
- [ ] Add a repository assertion that calculating with either toggle value leaves valid serialized v3 history unchanged when no pruning is required.
- [ ] Re-run the focused tests and commit with `feat: separate contextual day profiles`.

### Task 2: Configurable final-score threshold

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualSuggestions.kt`
- Modify: `app/src/main/java/app/olauncher/data/Prefs.kt`
- Modify: `app/src/main/java/app/olauncher/ui/HomeFragment.kt`
- Test: `app/src/test/java/app/olauncher/data/ContextualSuggestionsTest.kt`
- Test: `app/src/test/java/app/olauncher/data/SettingsBackupCodecTest.kt`

**Interfaces:**
- Produces: `ContextualSuggestions.normalizedMinimumScore(configured: Int): Int`.
- Produces: `Prefs.contextualMinimumScore: Int`, default `20`, and `Prefs.separateContextualDayProfiles: Boolean`, default `false`.
- Consumes: Task 1 `separateDayProfiles` repository parameter.

- [ ] Add failing tests for normalization (`-1→0`, `15→20`, `205→200`), thresholds `0/20/200`, and application after bonuses, including an explicit rule scoring exactly `200` with no history.
- [ ] Run `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualSuggestionsTest`; expect failures for the missing normalization behavior.
- [ ] Normalize to the nearest step of ten after clamping to `0..200`; in a tie, round upward. Persist both preferences with the approved defaults.
- [ ] Pass the profile toggle into `contextualUsageScore` and pass `contextualMinimumScore` into `ContextualSuggestions.rank` from `HomeFragment`.
- [ ] Extend `SettingsBackupCodecTest` to prove both ordinary keys remain included while `CONTEXTUAL_HISTORY` remains excluded.
- [ ] Run the focused data tests and commit with `feat: configure contextual score threshold`.

### Task 3: Suggestions settings and explanation dialog

**Files:**
- Create: `app/src/main/res/layout/dialog_minimum_score.xml`
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/res/layout-land/fragment_settings.xml`
- Modify: `app/src/main/java/app/olauncher/ui/SettingsFragment.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/app/olauncher/ui/ContextualScoringSettingsStructureTest.kt`

**Interfaces:**
- Consumes: Task 2 preferences and normalization.
- Produces: `showMinimumScoreDialog()`, `showMinimumScoreInfo()`, and the weekday/weekend toggle in Suggestions settings.

- [ ] Create `ContextualScoringSettingsStructureTest` requiring both setting rows in portrait and landscape and a dialog containing a `48dp` `ic_info` action plus minus/current/plus controls.
- [ ] Run the structural test; expect failure because the rows and dialog do not exist.
- [ ] Build `dialog_minimum_score.xml` with the same header/info pattern as the rule editor and a `− / value / +` stepper. Keep edits pending until confirmation; clamp at `0/200`; cancel/back leaves the preference unchanged.
- [ ] Add localized base strings for labels, on/off values, accessible info action, and an explanation listing the historical formula, four bonuses, final-score threshold, and profile behavior.
- [ ] Add both rows to portrait and landscape settings, register click handlers, refresh displayed values, and include both rows in the contextual-suggestions enabled/disabled group.
- [ ] Run unit tests plus `./gradlew assembleDebug`; commit with `feat: expose contextual scoring controls`.

### Task 4: Permanent scoring documentation and final verification

**Files:**
- Modify: `README.md`
- Create: `docs/contextual-suggestions.md`
- Review: existing contextual specs under `docs/superpowers/specs/`

**Interfaces:**
- Consumes: the exact shipped constants and behavior from Tasks 1–3.
- Produces: user-facing overview and a permanent technical reference.

- [ ] Add a README subsection summarizing contextual suggestions, configurable threshold, optional weekday/weekend profiles, and linking to `docs/contextual-suggestions.md`.
- [ ] Document the candidate pipeline, 48 buckets, temporal weights, ten points per launch, seven-day half-life, thirty-day retention, final bonuses, threshold semantics, ranking/tie-breaking, profile toggle, persistence, backup behavior, and worked examples.
- [ ] Cross-check every documented number against production constants and tests; update superseded wording in existing versioned specs only if it claims current behavior rather than historical design intent.
- [ ] Run `./gradlew testDebugUnitTest assembleDebug`, `./gradlew lintDebug`, and `git diff --check`; treat unrelated baseline lint findings as reported debt, not as permission to ignore new diagnostics.
- [ ] Review the changed documentation against `AGENTS.md`, then commit with `docs: explain contextual suggestion scoring`.
