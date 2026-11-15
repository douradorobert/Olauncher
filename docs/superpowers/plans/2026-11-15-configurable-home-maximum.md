# Configurable Home Maximum Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove home app scrolling and let the user configure the total number of fixed and suggested apps shown.

**Architecture:** Normalize the configured maximum with a pure contextual-suggestion helper, persist it in `Prefs`, and consume it when ranking suggestions. Restore the weighted app layout without a `ScrollView` and expose the value in Settings.

**Tech Stack:** Android XML, Kotlin, JUnit, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-15-configurable-home-maximum-design.md`

## Global Constraints

- Total maximum stays in `0..8`, defaults to eight, and never falls below the fixed count.
- Fixed apps take priority and retain the existing eight-slot storage.
- The app area must not scroll.

## Review Focus

- Invalid persisted maximum values are normalized.
- Increasing fixed apps raises the configured maximum.
- Disabling contextual suggestions leaves the new setting disabled consistently.
- Portrait and landscape layouts retain the same content-flow structure.
- Blank app-area space reaches the root gesture listener.

---

### Task 1: Maximum rule and persistence

- [ ] Add failing unit tests for maximum normalization and available suggestion slots.
- [ ] Run the focused tests and confirm the intended failures.
- [ ] Implement the pure normalization rule, preference, and fixed-count synchronization.
- [ ] Consume the configured maximum when choosing contextual suggestions.
- [ ] Run the focused tests and full unit suite.

### Task 2: Settings and non-scrollable layout

- [ ] Change the structural test to require a weighted, non-scrollable app layout.
- [ ] Run it and confirm failure against the current `ScrollView` layout.
- [ ] Remove the app `ScrollView` from both layout variants.
- [ ] Add the maximum-items row and popup behavior to Suggestions settings.
- [ ] Run unit tests, lint, resource compilation, and debug assembly.
