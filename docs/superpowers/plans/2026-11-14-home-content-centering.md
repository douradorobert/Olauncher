# Home Content Centering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Center home apps inside the vertical space remaining below visible date, calendar, and weather widgets.

**Architecture:** Use Android's vertical `LinearLayout` weight distribution instead of runtime measurement. In both layout variants, a full-height content column owns the widget block and a weighted, fill-viewport scroll area that centers short lists and keeps long lists reachable.

**Tech Stack:** Android XML layouts, Kotlin/JUnit structural regression test, Gradle Android build.

**Spec:** `docs/superpowers/specs/2026-11-14-home-content-centering-design.md`

## Global Constraints

- Preserve horizontal and optional bottom alignment behavior.
- Keep screen-time and bottom overlay views outside the new content column.
- Add no dependency or Kotlin measurement logic.
- Update portrait and landscape consistently.

## Review Focus

- No widgets: apps still center across the usable screen.
- Agenda and weather visible: apps begin below the measured widget block.
- Eight apps: content uses lower free space instead of overlapping widgets.
- Bottom alignment: apps align to the bottom of their remaining area.
- Landscape: widget centering and horizontal app margins remain unchanged.

---

### Task 1: Weighted home content flow

**Files:**
- Modify: `app/src/main/res/layout/fragment_home.xml`
- Modify: `app/src/main/res/layout-land/fragment_home.xml`
- Test: `app/src/test/java/app/olauncher/ui/HomeLayoutStructureTest.kt`

- [ ] Write a failing structural test asserting a shared vertical parent and a weighted app area in both resources.
- [ ] Run the focused test and confirm it fails against the overlay layout.
- [ ] Wrap widgets and a scrollable app viewport in a full-height vertical content column; make the viewport `0dp` high with weight `1`, enable `fillViewport`, retain existing horizontal/bottom padding, and keep screen time above the column in z-order.
- [ ] Run the focused test, full unit suite, resource compilation, and debug assembly.
- [ ] Commit the verified change.
