# Design system

This contract contains reusable UI rules. Product branding, artwork, supported themes, orientation,
language and screen layouts require their own approved specification; no previous product policy
is inherited by this base.

## Tokens and layout

- Use Material color roles first. Use `ColorPrimitives` only for needs without a suitable role.
- Use `LocalAppTypography`, `AppSpacing`, `AppShapes` and `AppMotion`. Resolve actual values from
  the current theme source rather than duplicating palettes and typography tables here.
- Keep spacing, shape and motion tokens in `ui/theme/AppSpacing.kt`, `AppShapes.kt` and
  `AppMotion.kt` respectively, separate from `Theme.kt` theme composition.
- Keep component geometry in private named declarations in the owning UI file.
- Keep root `fontScale = 1f` and preserve device density.
- Pass `overscrollEffect = null` to supported scroll containers, lazy lists, grids and pagers.
- Give lazy lists bounded constraints. Do not apply `verticalScroll` to a `LazyColumn` or place
  an unbounded lazy list inside another vertical scrolling container. Put headers and footers
  inside the lazy list with `item` instead.
- Respect safe drawing, keyboard insets, accessibility semantics and minimum touch targets.
  System bar visibility is an explicit app decision; do not inherit immersive behavior.

## State and interaction

- Use immutable composable state and repository interfaces at the ViewModel boundary.
- Separate first-load state from refresh and paging. Confirm empty, retry, offline and failure UX
  before adding a flow; pull-to-refresh is not a universal requirement for every screen.
- Debounce clicks and claim in-flight operations synchronously. Guard conflicting actions,
  cancellation, dismissal and callbacks after owner destruction.
- Use one shell snackbar host and lifecycle-aware buffered effects. Avoid duplicate delivery
  after recomposition or navigation. Features keep their strings separate.
- Error copy precedence: joined `details[].message`, then backend `message`, then feature fallback,
  then generic fallback. Never show exception text or raw response bodies.
- Keep routes small serializable NavKeys registered in `AppRouteSerializersModule`; scope Hilt
  ViewModels to navigation entries. Preserve drafts and approved Back behavior.

## Media and motion

Bound decode memory, concurrent work, cache lifetime and player resources. Release resources on
owner disposal, propagate cancellation and reject results for replaced selections or old contexts.
Previews must not start network, video or decorative infinite playback. Reuse shared components
where they exist; do not describe a component as available without checking source.

## Verification

After UI changes run `python3 scripts/check_design_tokens.py`. Compile, lint and static checks are
allowed; runtime and automated test execution follow repository authorization rules. Offer a
Maestro draft for new flows. Documentation-only edits do not require UI token checks.

## Concrete token usage

```kotlin
// Inside a composable using the app's existing theme and typography definitions:
Text(
    text = title,
    style = LocalAppTypography.current.title,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(AppSpacing.space16),
)
```

Keep token declarations in `AppSpacing.kt`, `AppShapes.kt` and `AppMotion.kt` rather than mixing
them into `Theme.kt`. The current base exposes `AppShapes.medium`, `AppMotion.PressScale` and
`AppMotion.DebounceMillis`; consumers reuse these instead of hardcoding another radius, scale or
debounce delay. Component-specific sizes still belong in private named geometry declarations.

Reference shared component responsibilities, when the consuming app has them:

| Component | Responsibility |
| --- | --- |
| `AppButton` / `AppIconButton` | Consistent press feedback and debounced clicks |
| Shell `AppSnackbar` host | Shared messages, insets and one-shot delivery |
| `AppPullToRefreshBox` | Refresh interaction separate from initial load |
| `AppNetworkImage` | Loading/error slots and cache-aware rendering |
| `AppAvatarCropEditor` | Crop placement draft and gesture/rendering boundary |
| `AppAnimationVideo` | Muted playback, poster handoff and player disposal |

These are reuse references, not instructions to recreate old onboarding, chat, widget or branded
screens. Check current source before importing any component beyond those actually installed.
