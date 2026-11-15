# Home Content Centering Design

## Intent

Prevent fixed and suggested apps from overlapping the clock, calendar event, and weather content while using the free space below those widgets.

## Approach

Place `dateTimeLayout` and a weighted app viewport in one full-height vertical container in portrait and landscape. The widget block keeps wrap-content height; the viewport receives the remaining height. A `fillViewport` scroll container expands `homeAppsLayout` to center short lists and permits scrolling when many apps do not fit. Screen time remains a later sibling in the root `FrameLayout`, preserving its drawing and touch priority; bottom overlays remain later siblings as well.

## Constraints

- Preserve horizontal alignment and optional bottom alignment.
- Preserve widget visibility behavior and contextual suggestions.
- Keep every configured app reachable on short screens and in landscape.
- Do not add runtime measurement code or dependencies.
- Apply the same structure to portrait and landscape resources.

## Alternatives considered

- Dynamic Kotlin margins: rejected because asynchronous weather/agenda updates, font scaling, and rotation would require repeated measurement logic.
- Fixed larger top padding: rejected because it still cannot adapt to changing widget height or app count.
