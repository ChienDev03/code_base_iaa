# Components, interactions and feedback

## Component catalog (`core.ui.components`)

- Actions — `AppButton`, `AppIconButton`, `AppBackButton` (back arrow on `AppIconButton`, debounced, single TalkBack label; types: primary/secondary/outline/ghost/destructive; sizes small/medium/large; blocks clicks while loading but keeps enabled colors)
- Inputs — `AppTextField`, `AppPasswordField`, `AppSearchField`, `AppTextArea`, `AppOtpInput`
- Progress — `AppStepProgressBar`, `AppLinearProgressBar`, `AppIndeterminateProgressBar`, `AppLoadingIndicator`, `AppSkeleton`
- Selection — `AppChip`, `AppChipGroup`
- Media — `AppAnimationVideo` (`core.ui.components.video`; muted URI playback, optional looping, first-frame/completion/error callbacks; parent owns poster and aspect-correct bounds)
- Refresh — `AppPullToRefreshBox` (`core.ui.components.refresh`; brand-styled Material 3 `PullToRefreshBox`)
- Structure — `AppTopBar`, `AppDivider`, `AppIconContainer`, `AppBorderedCard`, `SectionHeader`
- State — empty/error views in `AppStateViews`
- Feedback — `AppSnackbar` / `AppSnackbarType`, `AppAlertDialog` (the only allowed app dialog API)

Modifiers (`core.ui.modifier`): `Modifier.pressScale(interactionSource, …)`, `Modifier.shimmer()`,
`rememberDebouncedClick(windowMillis = 500L) { … }`.
Previews (`core.ui.preview`): `@AppPreviews`.

## Interaction contract

Compose in this order on a pressable surface (`AppButton` is the reference implementation):

```kotlin
val interactionSource = remember { MutableInteractionSource() }
Row(
    modifier = Modifier
        .pressScale(interactionSource)                     // 1. scale (graphicsLayer, no layout change)
        .clip(AppShapes.radius16)                          // 2. clip
        .background(ColorPrimitives.Green100)                // 3. container styling
        .clickable(                                        // 4. bounded Material 3 ripple
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = rippleColor),   // androidx.compose.material3.ripple
            onClickLabel = actionLabel,
            onClick = rememberDebouncedClick(onClick = onClick),
        )
        .padding(AppSpacing.space16),
)
```

- Use `PressScaleCompact` for chips and icon buttons, `PressScale` for standard surfaces.
- **Debounce every action.** The `core/ui` catalog (`AppButton`, `AppIconButton`, `AppChip`,
  `SectionHeader` action, `AppTopBar` back, overlay actions, snackbar action) debounces internally —
  pass the plain callback. Any raw `Modifier.clickable { }`, `combinedClickable`, `IconButton`, or
  `onClick` given to a non-catalog API must wrap it in `rememberDebouncedClick(onClick = …)`.
  Text input (`onValueChange`) is never debounced this way.
- Give tappable rows an `onClickLabel` / `contentDescription` — Maestro flows (`.maestro/`, once
  added) select by those strings, so changing one breaks E2E.
- Disable infinite shimmer/progress animation under `LocalInspectionMode` (the existing helpers already do).

## Snackbar

There is exactly **one** snackbar host, in the app `Scaffold` inside `AppNavGraph`. Never create or
own a `SnackbarHostState` in a screen or destination.

Feature ViewModel injects the singleton `SnackbarController` and enqueues unresolved text:

```kotlin
snackbar.show(message = null) // generic fallback; resolve approved feature error resources in UI
// Never render exception text, SQL diagnostics, or stored personal data.
```

Both `SnackbarController` and `AppViewModel.events` use buffered `Channel`s (not
`MutableSharedFlow`) so a message survives route and configuration transitions.

Repeated messages with matching `UiText`, severity and action label are ignored while already
showing or waiting. The renderer's job lifetime determines suppression, not a fixed timeout;
duplicates do not restart or extend the snackbar. After completion or dismissal, it may show again.

By default a different message cancels the suspended `showSnackbar` call and immediately replaces the
snackbar currently on screen. Preserve this **latest-wins** behavior; never make the host await
every snackbar sequentially, which recreates a visible queue and delays newer feedback. The one
opt-in exception is `afterCurrent = true` (`SnackbarController.show/showSuccess`,
`UserMessage.afterCurrent`): that message waits for the snackbar on screen to finish, then shows.
Use it only when two toasts must both be read in order (e.g. a deferred welcome message after
an initial action toast). A later default message still wins over the whole chain: it cancels
the snackbar on screen *and* any queued `afterCurrent` ones (`SnackbarMessageQueue.showSnackbarsInOrder`).
