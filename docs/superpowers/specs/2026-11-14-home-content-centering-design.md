# Home Content Centering Design

## Intent

Prevent fixed and suggested apps from overlapping the clock, calendar event, and weather content while using the free space below those widgets.

## Approach

Place `dateTimeLayout` and `homeAppsLayout` in one full-height vertical container in portrait and landscape. The widget block keeps wrap-content height; the app block receives the remaining height with weight `1` and centers its children inside that remainder. Screen time and bottom overlays remain siblings in the root `FrameLayout`.

## Constraints

- Preserve horizontal alignment and optional bottom alignment.
- Preserve widget visibility behavior and contextual suggestions.
- Do not add runtime measurement code or dependencies.
- Apply the same structure to portrait and landscape resources.

## Alternatives considered

- Dynamic Kotlin margins: rejected because asynchronous weather/agenda updates, font scaling, and rotation would require repeated measurement logic.
- Fixed larger top padding: rejected because it still cannot adapt to changing widget height or app count.
