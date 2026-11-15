# Configurable Home Maximum Design

## Intent

Restore global gestures in blank home-screen space by removing the app `ScrollView`, while allowing the total number of fixed and contextual apps shown on the home screen to be configured.

## Behavior

- Keep the existing fixed-app count (`homeAppsNum`) and eight fixed slots.
- Add a configurable total maximum from the fixed-app count through eight, defaulting to eight.
- Fixed apps always take priority. Contextual suggestions fill only the remaining slots.
- Raising the fixed-app count above the configured maximum raises the maximum to match.
- The app area does not scroll. Users can lower the maximum, text size, or spacing when content does not fit.

## Approach

Persist a `maximumHomeItems` preference and use it in the existing contextual-suggestion slot calculation. Expose it in the Suggestions settings section. Replace the weighted `ScrollView` with the weighted `homeAppsLayout` in both layout variants so blank space is again handled by the root gesture listener.

## Constraints

- Values remain in `0..8` and never below the fixed-app count.
- Preserve the existing fixed app storage and contextual ranking behavior.
- Keep portrait and landscape resources structurally consistent even though normal phone use is portrait-only.

