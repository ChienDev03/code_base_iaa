# Color roles — Material first, `ColorPrimitives` for the rest

Target color model (decided 2026-10-02). `AppExtendedColors`, `LightAppColors`, `DarkAppColors`
and `LocalAppColors` are being removed; the code migration is a separate phase. Until it lands,
legacy files still use `LocalAppColors` (the token check reports them as `COLOR-EXTENDED`) and only
the roles marked **configured** below resolve to brand colors.

## The two layers that remain

| Layer | File | Read from UI as |
| --- | --- | --- |
| Material roles (light + dark) | `ui/theme/AppColors.kt` → `LightColorScheme` / `DarkColorScheme` | `MaterialTheme.colorScheme.<role>` |
| Raw palette (one value, both modes) | `ui/theme/Color.kt` → `ColorPrimitives` | `ColorPrimitives.<Ramp><Shade>` |

Decision order for every color:

0. Resolve the design color to its primitive, then search `LightColorScheme` for it. A role holding
   that primitive wins whenever its meaning fits — `COLOR-PRIMITIVE-ROLE` flags `ColorPrimitives.<X>`
   when `<X>` is a configured role's light value (map derived from `AppColors.kt` at check time, so it
   grows as phase-2 roles are added). Mode-fixed exceptions carry `// primitive-ok: <reason>`.
1. A Material role means the same thing → `MaterialTheme.colorScheme.<role>`. This is the **only**
   way a color changes between light and dark.
2. No role fits → `ColorPrimitives.<Name>` directly in the UI code. It is the **same color in both
   modes** — accepted by design.
3. A color must differ between modes but no role fits → map it to the closest role (add the role to
   both schemes) or derive it from a role (`colorScheme.onSurface.copy(alpha = 0.6f)`). Never add an
   extended token object, never branch on `isSystemInDarkTheme()` in UI code.
4. Color missing from the palette → add it to `ColorPrimitives` (only if it exists in Figma
   `Color Primitives`). Raw `Color(0x…)` stays forbidden outside `ui/theme/Color.kt`.

A role is only usable after it is mapped in **both** schemes and listed in `ALLOWED_ROLES` in
`.claude/scripts/check-design-tokens.sh`; an unmapped role silently returns a Material baseline color.

## Migration map (legacy token → target)

Status: **configured** = mapped today; **phase 2** = role must be added to both schemes during the
migration (values below are the ones to use).

| Legacy `LocalAppColors` token | Light / Dark | Target | Status |
| --- | --- | --- | --- |
| `backgroundBasePrimary` | `White1000` / `Gray900` | `background` / `surface` | configured |
| `textBaseSecondary` | `Gray800` / `Gray200` | `onSurfaceVariant` | configured |
| `borderOnSurfaceSecondary` | `Gray200` / `Gray500` | `outline` | configured |
| `backgroundBrandSecondary` | `Pink50` / `Pink950` | `primaryContainer` | configured |
| `iconInvert` | `White1000` / `Gray900` | `onPrimary` | configured |
| `borderOnSurfacePrimary` | `Gray100` / `Gray600` | `outlineVariant` | phase 2 |
| `backgroundOnSurfacePrimary` | `Slate50` / `Gray700` | `surfaceContainerLow` | phase 2 |
| `bottomBarBackground` | `White1000` / `Gray800` | `surfaceContainer` | phase 2 |
| `backgroundBaseQuaternary` | `Slate100` / `Gray600` | `surfaceContainerHighest` (+ `surfaceVariant`, already read by `AppLoadingSkeleton`) | phase 2 |
| `backgroundBrandTertiary` | `Orange50` / `Gray800` | `tertiaryContainer` | phase 2 |
| `textFuncError` | `Red500` / `Red400` | `error` | phase 2 |
| `backgroundFuncError` | `Red50` / `Red900` | `errorContainer` | phase 2 |
| `overlayScrim` | `Black500` / `Black500` | `scrim` (already read by `AppLoadingIndicator`) | phase 2 |
| `textBaseSecondaryAlpha` | `Black600` / `White600` | `onSurface.copy(alpha = 0.6f)` — confirm with design | phase 2 |
| `textBaseTertiary` | `Black400` / `White400` | `onSurface.copy(alpha = 0.4f)` — confirm with design | phase 2 |
| `backgroundOnSurfaceOverlay` | `Black50` / `White50` | `onSurface.copy(alpha = 0.05f)` — confirm with design | phase 2 |
| `textOnImage`, `iconBaseTertiary`, `journeyPhotoOverlay`, `photoEditorBackground` | same both modes | `ColorPrimitives.*` (`White1000`, `Gray300`, `JourneyPhotoOverlay`, `Gray900`) | phase 2 |
| `topic*Background` / `topic*Content` | same both modes | `ColorPrimitives.Topic*` / ramp value | phase 2 |
| Warning / success (`backgroundFuncWarning`, `textFuncWarning`, `iconFuncWarning`, `textFuncSuccess`) | differ | `ColorPrimitives` light value (`Yellow50`, `Yellow700`, `Yellow500`, `Green700`) — dark variant dropped | phase 2 |
| Accent ramps (`green*`, `blue*`, `purple*`, `pink*`, `emerald*`, `yellow50`, `red50`, `orange50`) | differ | `ColorPrimitives` light value — dark variant dropped | phase 2 |
| `slate600`, `slate400`, `neutral700`, `neutral300` | differ | **Review each usage:** text/icon on a surface → a role (`onSurfaceVariant`, `outline`); decoration → `ColorPrimitives` | phase 2 |

Dark-mode consequence to verify in phase 2: every "dark variant dropped" row renders its light value
on the dark surface. Check contrast for anything used as text or icon tint.
