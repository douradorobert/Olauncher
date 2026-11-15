# Launcher Screenshot Gallery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce five privacy-safe screenshots of the real launcher running on `still_api37` and place them appropriately in the README and contextual-suggestions documentation.

**Architecture:** Build and install the debug APK in an isolated worktree, reset the authorized AVD, and stage deterministic demo state using visible UI plus narrowly scoped ADB commands. Treat visual approval as a hard gate: navigate through all five scenes in the visible emulator without capturing, wait for explicit user approval, then capture device-only PNGs and reference them from Markdown.

**Tech Stack:** Android/Gradle, Android Emulator `still_api37`, ADB, PNG, Markdown/HTML.

**Spec:** `docs/superpowers/specs/2026-11-15-launcher-screenshots-design.md`

## Global Constraints

- Use only the authorized `still_api37`; wiping its data is permitted.
- Do not alter product code merely to stage screenshots.
- Do not execute any screenshot command before explicit user approval of all five visible scenes.
- Any visual adjustment after approval requires renewed approval for the affected scenes.
- Never expose accounts, e-mail, real calendar/location data, SSID/BSSID, Bluetooth names, notification content, device identifiers, backup filenames, file pickers, or computer chrome.
- Use the current debug package `app.olauncher.debug` and preserve local untracked `AGENTS.md` plus unrelated work.
- Store final assets under `docs/images/` with the five stable filenames specified by the design.

## Review Focus

- A bare API 37 AVD may have no writable local calendar: use the visible calendar flow first and stop for user direction rather than adding accounts, helpers, root, or product code.
- Weather refresh may overwrite staged values: keep location unavailable, use a recent cached timestamp, and verify the visible text immediately before approval and capture.
- Navigation can mutate or dismiss staged UI: reopen each scene and visually verify it immediately before capture.
- Small-screen text or dialogs may be clipped: review every scene at the actual emulator dimensions and adjust only app settings/demo content, never the product layout for documentation.
- A screenshot may accidentally include sensitive system UI: run the privacy checklist on each staged scene and each final PNG.

---

### Task 1: Isolated capture environment and clean launcher

**Files:**
- No repository files modified.
- Create temporary staging data only under `/tmp/olauncher-screenshots/`.

**Interfaces:**
- Consumes: branch `develop`, AVD `still_api37`, package `app.olauncher.debug`.
- Produces: a visible, clean emulator running the current Olauncher build as the default home app.

- [ ] **Step 1: Create an isolated worktree using `superpowers:using-git-worktrees`**

Create branch `docs/launcher-screenshots` from current `develop`. Confirm `git status --short` in the main checkout contains only the preserved untracked `AGENTS.md` before creating the worktree.

- [ ] **Step 2: Build and verify the debug APK**

Run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Start the authorized AVD visibly with clean data**

Ensure no instance of `still_api37` is running, then start:

```bash
/home/robertd/Android/Sdk/emulator/emulator -avd still_api37 -wipe-data
```

Do not use `-no-window`. Wait for `adb wait-for-device` and `sys.boot_completed=1`.

- [ ] **Step 4: Install and select the launcher**

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, launch `app.olauncher.debug`, and complete the default-home selection through the visible Android UI. Set locale to pt-BR, use a plain neutral wallpaper, disable intrusive notifications, and keep status/navigation-bar treatment consistent.

- [ ] **Step 5: Verify the clean visual baseline**

Confirm the Olauncher home is visible in the emulator window, contains no account or personal data, and responds to navigation. Do not capture.

- [ ] **Step 6: Record environment state in the execution ledger**

Record emulator serial, display dimensions, density, Android API, APK SHA-256, and the absence of accounts/personal notifications without logging preference or calendar contents.

### Task 2: Stage and obtain approval for all five scenes

**Files:**
- No repository files modified.
- Temporary command snippets may live under `/tmp/olauncher-screenshots/` and must not contain copied personal data.

**Interfaces:**
- Consumes: clean visible emulator from Task 1.
- Produces: five privacy-checked scenes explicitly approved by the user, still without PNG captures.

- [ ] **Step 1: Stage neutral app and launcher settings**

Use the visible UI for ordinary settings. Select neutral installed applications for fixed apps and contextual candidates; enable contextual suggestions, set maximum home items to leave suggestion slots, set minimum score to `20`, enable weekday/weekend separation, and choose separate suggestion display if it improves clarity.

- [ ] **Step 2: Stage deterministic suggestion evidence**

Stop `app.olauncher.debug`, use `run-as app.olauncher.debug` only for narrowly scoped debug preferences, and seed either current-bucket JSON v3 history or an always-matching neutral rule for selected candidates. Restart the launcher and verify suggestions are visible. Never dump existing preferences to logs.

- [ ] **Step 3: Stage cached weather**

With the app stopped, set `WEATHER_ENABLED=true`, neutral `WEATHER_LOCATION_NAME`, plausible temperature/code, `WEATHER_MANUAL=true`, and a current `WEATHER_UPDATED_AT` in the app's own debug preferences. Keep location permission unavailable so refresh cannot replace the staged values. Restart and verify the widget.

- [ ] **Step 4: Stage a fictitious upcoming calendar event**

Grant only `READ_CALENDAR`. Through the visible calendar UI, create a neutral event within the launcher's three-hour window, such as “Consulta de demonstração” at “Centro”, using a local writable calendar. If no local writable calendar exists, stop and ask the user rather than adding an account, installing a helper, rooting the AVD, or changing product code.

- [ ] **Step 5: Prepare the five scenes**

Navigate, without taking screenshots, through:

1. Home with date/time, fictitious event, cached weather, and contextual suggestions.
2. Suggestions settings with enabled state, candidates, maximum, minimum score, and day-profile toggle visible.
3. A filled contextual-rule editor using neutral conditions.
4. The minimum-score information dialog, falling back to rule information only if score copy is illegible at the device scale.
5. Backup and restore section showing both app actions without opening a file picker.

- [ ] **Step 6: Run the privacy and visual checklist on every scene**

For each scene confirm no account/e-mail, real event/location, network/Bluetooth name, notification, identifier, filename/file picker, or computer chrome is visible. Confirm text is readable and no required control is clipped.

- [ ] **Step 7: Hard gate — request user approval**

Leave the emulator visibly showing the final reviewed scene and tell the user that all five scenes are prepared and no screenshot has been taken. Ask them to inspect the launcher and explicitly approve capture. **Stop execution here.** Do not run `screencap`, emulator screenshot shortcuts, screen recording, or host capture commands until approval arrives.

### Task 3: Capture and validate five PNG assets

**Files:**
- Create: `docs/images/home-contextual.png`
- Create: `docs/images/suggestion-settings.png`
- Create: `docs/images/contextual-rule.png`
- Create: `docs/images/scoring-info.png`
- Create: `docs/images/backup-restore.png`

**Interfaces:**
- Consumes: explicit user approval from Task 2 and the five approved emulator scenes.
- Produces: five device-only, privacy-safe, consistently sized PNG assets.

- [ ] **Step 1: Create the asset directory**

Use `mkdir -p docs/images`. Do not overwrite unrelated assets. If any target exists, inspect it and stop before replacement unless it belongs to this plan.

- [ ] **Step 2: Reopen and capture each approved scene**

For every target, reopen the corresponding scene, visually confirm it still matches the approved state, then run:

```bash
adb exec-out screencap -p > docs/images/<target>.png
```

If any visual adjustment is necessary, stop and request renewed approval before capturing that scene.

- [ ] **Step 3: Validate file format and dimensions**

Run `file docs/images/*.png` and an available image-dimension inspector. Expected: all five are valid PNGs with identical pixel dimensions and portrait orientation.

- [ ] **Step 4: Inspect every final image visually**

Use the image-viewing tool on each PNG. Verify legibility, consistent bars/framing, correct scene content, and the privacy checklist. Recapture only from the approved state; otherwise obtain renewed approval.

- [ ] **Step 5: Check repository scope**

Run `git status --short` and confirm the only new repository files are the five intended PNGs.

- [ ] **Step 6: Commit the screenshot assets**

```bash
git add docs/images/home-contextual.png docs/images/suggestion-settings.png docs/images/contextual-rule.png docs/images/scoring-info.png docs/images/backup-restore.png
git commit -m "docs: add launcher screenshots"
```

### Task 4: Integrate the gallery into permanent documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/contextual-suggestions.md`
- Review: `AGENTS.md`

**Interfaces:**
- Consumes: validated PNGs from Task 3.
- Produces: a compact README gallery and contextual images placed near the technical explanations they illustrate.

- [ ] **Step 1: Add the README gallery**

Add a `## Capturas de tela` section after the feature/differences overview. Use centered HTML images with stable relative paths, approximately `240–280` pixels wide, meaningful Portuguese alt text, and a layout that wraps acceptably on narrow GitHub pages. Include Home, suggestion settings, contextual rule, and backup/restore; avoid repeating every modal if it makes the README noisy.

- [ ] **Step 2: Add images to contextual documentation**

Place `home-contextual.png` near the pipeline overview, `suggestion-settings.png` near the threshold/profile sections, `contextual-rule.png` near contextual bonuses/rules, and `scoring-info.png` near the minimum-score explanation. Use captions or nearby prose explaining what each real screenshot demonstrates.

- [ ] **Step 3: Verify Markdown paths and accessibility text**

Use `rg` to confirm every intended filename is referenced, every `<img>` has non-empty `alt`, and no broken or absolute local paths were introduced.

- [ ] **Step 4: Review rendered scale and image contents**

Inspect README and documentation at source plus available rendered preview. Confirm the gallery remains readable without dominating the page and contextual images support the adjacent text.

- [ ] **Step 5: Run final repository verification**

Run:

```bash
git diff --check
./gradlew testDebugUnitTest assembleDebug
```

Expected: diff check clean and `BUILD SUCCESSFUL`. Review `git status --short` to ensure local `AGENTS.md` remains untracked and untouched.

- [ ] **Step 6: Commit documentation integration**

```bash
git add README.md docs/contextual-suggestions.md
git commit -m "docs: show launcher features in action"
```

- [ ] **Step 7: Request final review and integrate only by user choice**

Use `superpowers:requesting-code-review`, fix Critical/Important findings, rerun verification, then use `superpowers:finishing-a-development-branch` to offer merge, PR, or branch preservation.
