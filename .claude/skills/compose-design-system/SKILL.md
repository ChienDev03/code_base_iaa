---
name: compose-design-system
description: Rules for writing, editing, or reviewing any Compose UI in this Android app — screens, shared components, previews, theming. Use whenever a task touches a @Composable under com.lutech.pairpulse (core/ui, ui/theme, features/*/presentation), adds a screen or component, translates a Figma design to Compose, styles text/colors/spacing/radii, wires a click or press animation, shows a snackbar, or reviews UI code for token/Material-3 violations. Covers the Material-role-first color rule (MaterialTheme.colorScheme, else ColorPrimitives directly; no LocalAppColors), the AppSpacing/AppShapes/AppMotion/LocalAppTypography token set, file-private component and icon sizes (no AppSizes), pull-to-refresh, overscrollEffect = null on every scrollable, mandatory click debouncing, the core/ui component catalog, the forbidden Material 3 components, the pressScale + rememberDebouncedClick interaction contract, the app-scoped snackbar, and preview/test requirements.
---

# Compose design system (core/ui + ui/theme)

The shared design system lives in `app/src/main/java/com/lutech/pairpulse/core/ui` and `ui/theme`.
Feature code **consumes** it; it must never duplicate component geometry or inline raw
palette/dimension values. `.ai-kb/design-system.md` is the prose reference — read it when a
question here is unanswered, and update it when you change the system.

## Hard rules

1. **One token API per concern — there is no "either/or".** See "Which API do I use?" below.
   Never inline `Color(0xFF...)` or a raw `.sp`. Spacing, radius, color and type always come from
   tokens. Component sizes follow "Component sizes" below (file-private named vals).
2. **Never use Material 3 `Button`, `OutlinedTextField`, `TextField`, `AssistChip`,
   `FilterChip`, `InputChip`.** Their internal padding, min sizes and state layers cannot meet
   the Figma geometry. Use the `core/ui/components` catalog reference, or compose a new
   token-driven primitive from `Box`/`Row`, `BasicTextField`, `clickable`, and Material 3 ripple.
   Material 3 `Scaffold`, `Snackbar`, `Text`, `Icon`, `LazyColumn`, `MaterialTheme` are fine.
3. **Reuse before you create.** Check the catalog first. A new shared component belongs in
   `core/ui/components` only if two or more features need it; otherwise put it in
   `features/<name>/presentation/components/`.
4. **Split stateful and stateless.** `XScreen(...)` takes `hiltViewModel()`, collects state with
   `collectAsStateWithLifecycle()`, and consumes one-shot events. `XContent(state, callbacks, modifier)`
   is stateless, has no ViewModel reference, and is what previews and instrumented tests render.
5. **`hiltViewModel()`, never `viewModel()`** — Navigation 3 entries require the Hilt-aware factory.
6. **Every user-visible string comes from `stringResource(R.string.…)`**, defined in the module's
   `res/values/string_<module>.xml` with a `<module>_` key prefix (cross-module ones in
   `string_common.xml`). `app_name` is supplied per flavor via `resValue` — never add it to any strings file.
7. **UI files stay under 500 lines** — split large screens into private section composables in
   sibling files (`<Screen>Header.kt`, `<Screen>Sections.kt`).
8. **Every public composable gets a KDoc line** and a `@AppPreviews`-annotated private preview
   wrapped in `AppTheme { }` (the annotation is the default/large-font/compact-width matrix).
9. **Every scrollable passes `overscrollEffect = null` (IMPORTANT).** See "Scrolling" below.
10. **Every click is debounced.** See "Components, interaction and feedback" below.
11. **Data screens support pull-to-refresh.** See "Pull-to-refresh" below.
12. Runtime appearance is fixed light, English, portrait and fixed font scale. Retained dark
   scheme definitions do not imply a selectable theme. See `.ai-kb/features/settings.md`.

## Which API do I use?

Deterministic — do not improvise, and do not mix styles within a file.

### Typography — always `LocalAppTypography.current.*`

```kotlin
style = LocalAppTypography.current.headingH5     // ALWAYS this
style = MaterialTheme.typography.titleMedium     // NEVER — 0 usages in the codebase
style = AppHeadingH5                             // NEVER — don't import the raw TextStyle
```

Tokens: `headingH2/H4/H5/H6`, `bodyB2/B3/B4`, `labelL2/L3/L4` (Inter). The same styles are also
installed into `MaterialTheme.typography` so Material internals render correctly — that mapping
exists for Material's benefit, **not as a second API for you**.

### Color — Material role first, then `ColorPrimitives` directly

There is **no extended color object**. `LocalAppColors`, `AppExtendedColors`, `LightAppColors` and
`DarkAppColors` are deprecated and being removed — never use or extend them (`COLOR-EXTENDED`).

0. **Look up the scheme before choosing any color — mandatory.** Resolve the design color to its
   primitive (Figma variable / hex → `ColorPrimitives.<Name>` in `ui/theme/Color.kt`), then search
   `LightColorScheme` in `ui/theme/AppColors.kt` for that primitive. If a role holds it and the role's
   meaning fits the usage (container fill → `surface`/`background`, brand accent → `primary`, body
   text → `onSurface`, …), you **must** use the role — including tinted variants
   (`colorScheme.primary.copy(alpha = 0.28f)`, not `ColorPrimitives.Pink700.copy(…)`). Only when no
   role holds the value, or the color must stay identical in dark mode (text/icon over a photo, an
   illustration fill), fall through to step 2. The token check enforces this (`COLOR-PRIMITIVE-ROLE`);
   a deliberate mode-fixed primitive needs `// primitive-ok: <reason>` on the same or previous line.
1. **A Material role fits → `MaterialTheme.colorScheme.<role>`.** Roles are the only colors that
   switch between light and dark. Use a role only if it is mapped in **both** `LightColorScheme` and
   `DarkColorScheme` (`ui/theme/AppColors.kt`) and listed in `ALLOWED_ROLES` in the token check.
   Currently configured: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`,
   `background`, `onBackground`, `surface`, `onSurface`, `onSurfaceVariant`, `outline`.
   An unmapped role (`error`, `scrim`, `surfaceVariant`, `outlineVariant`, …) compiles but silently
   returns a Material *baseline* color (`colorScheme.error` = `#B3261E`, not brand `Red500`) — map it
   in both schemes and add it to `ALLOWED_ROLES` first (`COLOR-ROLE`).
2. **No role fits → `ColorPrimitives.<Ramp><Shade>` directly** in the UI code
   (`import com.lutech.pairpulse.ui.theme.ColorPrimitives`), e.g. `ColorPrimitives.Green500`.
   It is the same color in both modes — accepted by design.
3. **Must differ per mode but no role fits** → map the closest role in both schemes, or derive from a
   role (`colorScheme.onSurface.copy(alpha = 0.6f)`). Never branch on `isSystemInDarkTheme()` in UI code.
4. **Color missing** → add it to `ColorPrimitives` (`ui/theme/Color.kt`) only when it exists in the
   Figma `Color Primitives` collection. Raw `Color(0x…)` is forbidden outside `ui/theme/Color.kt` (`COLOR-HEX`).

Legacy token → role/primitive mapping, plus the roles still to configure:
`references/color-roles.md`.

### Everything else

| Concern | API | Notes |
| --- | --- | --- |
| Spacing (padding, gaps, `Arrangement.spacedBy`) | `AppSpacing.space02…space32` | 2/4/6/8/12/16/20/24/32dp. Compose them (`space08 + space02`) rather than inlining `10.dp` |
| Icon / component sizes | file-private `private val` in the UI file | See "Component sizes" below; tappables still need a 48dp minimum touch target. Never `AppSizes.*` |
| Corner radius | `AppShapes.radius04/08/12/16/24`, `AppShapes.pill` | Never `RoundedCornerShape(8.dp)` inline |
| Motion | `AppMotion.PressScale` (0.97), `PressScaleCompact` (0.94), `PressSpring`, `*TweenMillis` | |

Text-bearing surfaces use `heightIn(min = …)`, never a fixed `height(…)`, so font scaling works.

### Component sizes — file-private, never global

- Widget/icon/component dimensions (`size`, `width`, `height`, `heightIn`, `widthIn`,
  `requiredSize`, `defaultMinSize`) are declared as `private val <Component><Purpose> = NN.dp` at
  the top of the UI file that uses them. Never put the literal inside the modifier chain.
- **Icons included** — every icon size is a file-private val, not a shared token:

  ```kotlin
  private val SearchIconSize = 20.dp            // top of the file
  private val JourneyCardWidth = 173.dp

  Icon(painterResource(R.drawable.ic_search), null, Modifier.size(SearchIconSize))   // ALWAYS
  Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp))            // NEVER (SIZE-INLINE)
  Icon(painterResource(R.drawable.ic_search), null, Modifier.size(AppSizes.icon20))  // NEVER (SIZE-GLOBAL)
  ```

  Name by purpose (`ClearIconSize`, `NavIconSize`), PascalCase like other file-level vals. One val
  per distinct meaning — two icons that happen to be 20dp but mean different things get two vals.
- Do **not** put sizes in `ui/theme/Spacing.kt` or any shared token file. `AppSizes` is legacy
  (still in `Spacing.kt`, read by `AppTextField`) and is being removed — never reference or extend it.
  `AppSpacing` is only for padding and gaps.
- A size another file needs is a sign the UI should be a shared component: move the component (with
  its private size) into `core/ui/components`, or expose the size as a parameter with a default.
- Enforced by the token check (`SIZE-INLINE`, `SIZE-GLOBAL`).

### Scrolling — `overscrollEffect = null` everywhere (IMPORTANT)

```kotlin
LazyColumn(modifier = …, overscrollEffect = null) { … }          // LazyRow / Lazy*Grid / *Pager too
Modifier.verticalScroll(rememberScrollState(), overscrollEffect = null)   // horizontalScroll too
```

No exceptions — screens, previews, sheets, dialogs, shared components. Enforced by the token check
(`SCROLL-OVERSCROLL`).

### Pull-to-refresh

- Every screen that shows loaded data (repository- or network-backed) wraps its scrollable content in
  the shared `core/ui/components/refresh/AppPullToRefreshBox`. Screens with no loadable data (Login,
  static Settings, placeholders) are exempt.
- `isRefreshing` is separate from the first load: first load shows the skeleton; a refresh keeps the
  current content and shows only the indicator.
- The ViewModel's `onRefresh()` reloads without clearing existing data; a refresh failure keeps the old
  content and reports the error through the snackbar.

## Components, interaction and feedback

Consult [components and feedback](references/components-and-feedback.md) when selecting a component,
building a pressable surface or changing snackbar behavior. Reuse catalog components first.
Pressable modifier order is pressScale → clip → styling → bounded ripple/clickable → padding;
compact surfaces use `PressScaleCompact`. Raw actions wrap `rememberDebouncedClick`; catalog actions
already debounce. Never debounce `onValueChange`. Supply action semantics and disable infinite
preview animations with `LocalInspectionMode`.

One app-scoped `SnackbarController`/host owns unresolved `UiText` messages through buffered Channels.
Errors use backend-first `toUiText`; never render exception text. Identical pending/showing messages
are suppressed for the renderer job's lifetime. Different messages replace immediately (latest wins).
`afterCurrent = true` explicitly queues ordered feedback; a later default message cancels the active
and queued chain. Preserve these rules when changing feedback; the reference includes API examples.

## Before you finish

**Run the token check only after UI work** — a UI feature or a UI bug fix (files under
`features/*/presentation`, `core/ui`, `ui/theme`, `navigation`, or any file with `@Composable`).
Skip it for non-UI changes (data, DI, network, tests, docs, Gradle) and for trivial edits that don't
touch rendering, and say in the summary that it was skipped. The `PostToolUse` hook applies the same
filter automatically. It enforces typography API, unconfigured `colorScheme` roles, `LocalAppColors`
usage, raw hex, inline `RoundedCornerShape`, forbidden Material 3 components, `viewModel()`, inline
size literals, `AppSizes` usage, and missing `overscrollEffect = null`:

```bash
.claude/scripts/check-design-tokens.sh <files you edited>   # or with no args to scan everything
```

**Tests are manual.** Don't run them yourself: list the commands and ask the user whether to run
them (and, for a new UI flow, offer to draft a `.maestro/<feature>.yaml`):

```bash
./gradlew testDevDebugUnitTest
./gradlew connectedDevDebugAndroidTest   # needs a device/emulator; instrumented tests render XContent
```

Only **new** violations fail. Known legacy ones are listed in
`.claude/scripts/design-tokens-baseline.txt` — treat that file as a to-do list, not as permission.
Never add an entry to it to silence your own new code; fix the code instead. Regenerate it (with
`--update-baseline`) only after deliberately migrating a legacy file, so the entry disappears.

`ui/theme/Color.kt` (`ColorPrimitives`) is the only place raw hex belongs. If a needed color does not
exist, add it to `ColorPrimitives` (or map a Material role in both schemes — see "Color" above);
a missing spacing token goes to `AppSpacing`. Never inline it — then log the change in `.ai-kb/design-system.md` and `.ai-kb/CHANGELOG.md`.

Instrumented UI tests live in `app/src/androidTest/.../core/ui/components/` and
`features/*/presentation/`; they render the stateless `XContent`/component inside `AppTheme` with
`createComposeRule()` and assert on text or content description. Add one for any new shared component.
