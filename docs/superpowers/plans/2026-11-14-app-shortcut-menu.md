# App Shortcut Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a unified long-press dialog that exposes up to four Android app shortcuts plus context-specific Olauncher actions for drawer, fixed, and suggested apps.

**Architecture:** Keep Android shortcut access in a focused helper and keep ordering/deduplication in a pure Kotlin selector. Build one reusable dialog renderer whose callers provide action rows, then wire drawer and home contexts to it while preserving profile-aware launch behavior.

**Tech Stack:** Kotlin, Android `LauncherApps`, AppCompat `AlertDialog`, View Binding, JUnit 4.

**Spec:** Approved conversation plan from 2026-11-14 (no repository spec file).

## Global Constraints

- Show at most four enabled shortcuts, ordered by publisher rank and label, deduplicated by shortcut ID.
- Query dynamic, manifest, and caller-pinned shortcuts on API 25+ only when shortcut-host permission exists.
- Keep administrative actions available when shortcut lookup fails or is unavailable.
- Use existing `OlDialog` styling and add no external dependency.
- Removing a suggestion requires confirmation, removes it from `contextualApps` and deletes its contextual rule.

## Review Focus

- API 24 and missing shortcut-host permission must not crash or hide administrative actions.
- Work/profile `UserHandle` must be used for both lookup and launch.
- Disabled and duplicate shortcut records must not appear.
- Empty/full home slots must choose first free or ask which visible slot to replace.
- Canceling contextual removal must leave preferences untouched.

---

### Task 1: Shortcut selection and Android gateway

**Files:**
- Create: `app/src/main/java/app/olauncher/helper/AppShortcuts.kt`
- Test: `app/src/test/java/app/olauncher/helper/AppShortcutSelectorTest.kt`

- [ ] Write failing unit tests for filtering, deduplication, rank ordering, label fallback, and the four-item limit.
- [ ] Run the focused test and confirm it fails because the selector is absent.
- [ ] Implement the pure selector and the guarded `LauncherApps` query/launch gateway.
- [ ] Run the focused test and full unit suite.

### Task 2: Menu model and dialog

**Files:**
- Create: `app/src/main/java/app/olauncher/helper/AppActionMenu.kt`
- Create: `app/src/main/res/layout/dialog_app_actions.xml`
- Modify: `app/src/main/java/app/olauncher/helper/Dialogs.kt`
- Test: `app/src/test/java/app/olauncher/helper/AppActionMenuTest.kt`

- [ ] Write failing tests for drawer, fixed, suggestion, and Android-pinned-shortcut action sets.
- [ ] Implement the source/action model and reusable `OlDialog` action renderer.
- [ ] Run focused tests and full unit suite.

### Task 3: Drawer and home integration

**Files:**
- Modify: `app/src/main/java/app/olauncher/ui/AppDrawerAdapter.kt`
- Modify: `app/src/main/java/app/olauncher/ui/AppDrawerFragment.kt`
- Modify: `app/src/main/java/app/olauncher/ui/HomeFragment.kt`
- Modify: `app/src/main/java/app/olauncher/data/Prefs.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml`
- Test: `app/src/test/java/app/olauncher/data/ContextualCandidateRemovalTest.kt`

- [ ] Write a failing test for candidate/rule removal behavior.
- [ ] Add the preference operation and wire confirmation from suggestions.
- [ ] Replace drawer inline long-press actions with the unified dialog and wire fixed/suggested home long presses.
- [ ] Implement first-free-slot pinning and visible-slot replacement selection.
- [ ] Run unit tests, lint, and debug build.

