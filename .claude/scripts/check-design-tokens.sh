#!/usr/bin/env bash
# Design-system token check for the Compose code in this app.
# Rules are documented in .claude/skills/compose-design-system/SKILL.md.
#
# Usage:
#   .claude/scripts/check-design-tokens.sh                   # scan the whole main sourceSet
#   .claude/scripts/check-design-tokens.sh <file.kt> ...     # scan specific files (hook mode)
#   .claude/scripts/check-design-tokens.sh --update-baseline # accept current violations as known
#
# Only violations that are NOT in design-tokens-baseline.txt fail the run, so known
# legacy code does not cause alert fatigue.
# Exit 0 = no new violations, 1 = new violations (details on stderr).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Auto-detect the application namespace from app/build.gradle.kts so this script
# follows scripts/change_bundle.py renames without manual edits.
NAMESPACE="$(sed -n 's/^[[:space:]]*namespace[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -n1)"
if [ -z "${NAMESPACE:-}" ]; then
  echo "check-design-tokens: could not detect namespace in app/build.gradle.kts" >&2
  exit 2
fi
NAMESPACE_PATH="$(echo "$NAMESPACE" | tr '.' '/')"
SRC="$ROOT/app/src/main/java/$NAMESPACE_PATH"
THEME_DIR="$SRC/ui/theme"
BASELINE="$SCRIPT_DIR/design-tokens-baseline.txt"

# Roles mapped in BOTH LightColorScheme and DarkColorScheme (ui/theme/AppColors.kt).
# Any other colorScheme role silently resolves to a Material 3 baseline color.
# When a role is mapped in both schemes, add it here (see compose-design-system/references/color-roles.md).
ALLOWED_ROLES='primary|onPrimary|primaryContainer|onPrimaryContainer|background|onBackground|surface|onSurface|onSurfaceVariant|outline'

# Primitive -> roles map, derived from LightColorScheme in AppColors.kt (one "Primitive<TAB>role, role"
# line per primitive). A role value written as `LightAppColors.<token>` is resolved through the
# LightAppColors definition. Used by COLOR-PRIMITIVE-ROLE: a primitive that IS a role's value must be
# read through the role, so it follows dark mode.
APP_COLORS="$THEME_DIR/AppColors.kt"
ROLE_PRIMITIVES="$(awk '
  /^internal val LightAppColors = / { inExt = 1; next }
  /^internal val LightColorScheme/  { inScheme = 1; next }
  /^\)/ { inExt = 0; inScheme = 0 }
  inExt    && match($0, /[A-Za-z0-9]+ = P\.[A-Za-z0-9]+/) {
    split(substr($0, RSTART, RLENGTH), kv, / = P\./); ext[kv[1]] = kv[2]
  }
  inScheme && match($0, /[A-Za-z0-9]+ = (P|LightAppColors)\.[A-Za-z0-9]+/) {
    split(substr($0, RSTART, RLENGTH), kv, / = /); split(kv[2], src, /\./)
    prim = (src[1] == "P") ? src[2] : ext[src[2]]
    if (prim != "") roles[prim] = (prim in roles) ? roles[prim] ", " kv[1] : kv[1]
  }
  END { for (p in roles) print p "\t" roles[p] }
' "$APP_COLORS" 2>/dev/null)"

UPDATE_BASELINE=0
FILES=()
for arg in "$@"; do
  if [ "$arg" = "--update-baseline" ]; then
    UPDATE_BASELINE=1
  else
    FILES+=("$arg")
  fi
done
if [ "${#FILES[@]}" -eq 0 ]; then
  while IFS= read -r kt; do FILES+=("$kt"); done < <(find "$SRC" -name '*.kt' | sort)
fi

# Each finding is "relpath<TAB>RULE<TAB>line<TAB>message".
findings=()

add() { findings+=("$1"$'\t'"$2"$'\t'"$3"$'\t'"$4"); }

scan_rule() { # file rel rule pattern message  (pattern is an ERE for grep -nE)
  local f="$1" rel="$2" rule="$3" pattern="$4" msg="$5" hit
  while IFS= read -r hit; do
    [ -n "$hit" ] || continue
    add "$rel" "$rule" "${hit%%:*}" "$msg"
  done < <(grep -nE "$pattern" "$f" 2>/dev/null || true)
}

# scan_calls file call_regex require|forbid arg_regex
# Prints the start line of every call matching call_regex whose argument list (followed across
# lines to the balancing paren, with // comments stripped) does not contain (require) or does
# contain (forbid) arg_regex.
scan_calls() {
  awk -v call="$2" -v mode="$3" -v pat="$4" '
    function close_at(text,   i, c) {
      for (i = 1; i <= length(text); i++) {
        c = substr(text, i, 1)
        if (c == "(") depth++
        else if (c == ")") { depth--; if (depth == 0) return i }
      }
      return 0
    }
    function report(args) {
      if ((mode == "require" && args !~ pat) || (mode == "forbid" && args ~ pat)) print startLine
    }
    { line = $0; c = index(line, "//"); if (c) line = substr(line, 1, c - 1) }
    open {
      e = close_at(line)
      if (e) { buf = buf " " substr(line, 1, e); report(buf); open = 0 } else buf = buf " " line
      next
    }
    match(line, call) {
      rest = substr(line, RSTART + RLENGTH - 1)
      depth = 0; startLine = NR
      e = close_at(rest)
      if (e) report(substr(rest, 1, e)); else { buf = rest; open = 1 }
    }
  ' "$1"
}

for f in "${FILES[@]}"; do
  case "$f" in *.kt) ;; *) continue ;; esac
  case "$f" in /*) ;; *) f="$ROOT/$f" ;; esac
  [ -f "$f" ] || continue

  rel="${f#"$ROOT"/}"
  in_theme=0
  case "$f" in "$THEME_DIR"/*) in_theme=1 ;; esac

  if [ "$in_theme" -eq 0 ]; then
    # 1. Typography always comes from LocalAppTypography.
    scan_rule "$f" "$rel" TYPOGRAPHY 'MaterialTheme\.typography\.' \
      'use LocalAppTypography.current.<token>, not MaterialTheme.typography'

    # 2. Raw hex belongs only in ui/theme/Color.kt.
    scan_rule "$f" "$rel" COLOR-HEX 'Color\(0x' \
      'raw hex; use a MaterialTheme.colorScheme role or add the color to ColorPrimitives (ui/theme/Color.kt)'

    # 3. Corner radius must use AppShapes. A parameterized RoundedCornerShape(variable) is allowed.
    scan_rule "$f" "$rel" SHAPE 'RoundedCornerShape\((([0-9])|(AppSpacing\.))' \
      'use AppShapes.radius04/08/12/16/24 or AppShapes.pill, not an inline RoundedCornerShape'

    # 4. colorScheme roles that LightColorScheme does not configure.
    while IFS= read -r hit; do
      [ -n "$hit" ] || continue
      line="${hit%%:*}"
      role="$(printf '%s' "$hit" | sed -n 's/.*MaterialTheme\.colorScheme\.\([A-Za-z0-9]*\).*/\1/p')"
      if [ -n "$role" ] && ! printf '%s' "$role" | grep -qE "^($ALLOWED_ROLES)$"; then
        add "$rel" COLOR-ROLE "$line" \
          "colorScheme.$role is NOT mapped in both color schemes (returns a Material baseline color); map it in AppColors.kt + ALLOWED_ROLES, or use ColorPrimitives.<Name>"
      fi
    done < <(grep -n 'MaterialTheme\.colorScheme\.' "$f" 2>/dev/null || true)

    # 4b. The extended color object is deprecated: Material role first, else ColorPrimitives.
    scan_rule "$f" "$rel" COLOR-EXTENDED '(LocalAppColors|AppExtendedColors|LightAppColors|DarkAppColors)' \
      'extended colors are deprecated; use MaterialTheme.colorScheme.<role>, else ColorPrimitives.<Name>'

    # 4c. Material role first: a ColorPrimitives value that is already a configured role's light value
    #     must be read through the role. Opt out only for a color that must stay fixed in dark mode
    #     (e.g. text on a photo) with `// primitive-ok: <reason>` on the same or the previous line.
    while IFS= read -r hit; do
      [ -n "$hit" ] || continue
      line="${hit%%:*}"
      prev="$(sed -n "$((line > 1 ? line - 1 : 1))p" "$f")"
      printf '%s\n%s' "$prev" "${hit#*:}" | grep -qE '//[[:space:]]*primitive-ok:[[:space:]]*[^[:space:]]' && continue
      for prim in $(printf '%s' "${hit#*:}" | grep -oE 'ColorPrimitives\.[A-Za-z0-9]+' | sed 's/ColorPrimitives\.//' | sort -u); do
        roles="$(printf '%s\n' "$ROLE_PRIMITIVES" | awk -F'\t' -v p="$prim" '$1 == p { print $2 }')"
        [ -n "$roles" ] || continue
        add "$rel" COLOR-PRIMITIVE-ROLE "$line" \
          "ColorPrimitives.$prim is the value of colorScheme.{$roles}; use MaterialTheme.colorScheme.<role> (or .copy(alpha = …)), or mark a mode-fixed color with \`// primitive-ok: <reason>\`"
      done
    done < <(grep -n 'ColorPrimitives\.' "$f" 2>/dev/null || true)
  fi

  # 5. Material 3 components whose geometry cannot match the design tokens.
  scan_rule "$f" "$rel" MATERIAL3 'androidx\.compose\.material3\.(Button|OutlinedButton|TextButton|OutlinedTextField|TextField|AssistChip|FilterChip|InputChip|Card)$' \
    'forbidden Material 3 component; use the core/ui/components catalog'

  if [ "$in_theme" -eq 0 ]; then
    # 7. Component sizes are file-private named vals, never inline literals in a size modifier.
    #    Multi-line calls are followed to their closing paren.
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      add "$rel" SIZE-INLINE "$line" 'inline size literal; declare `private val <Component><Purpose> = NN.dp` at the top of this file'
    done < <(scan_calls "$f" '[.](size|width|height|heightIn|widthIn|requiredSize|defaultMinSize)[(]' forbid '[0-9]+([.][0-9]+)?[.]dp')

    # 7b. No shared size object: icon/component sizes are file-private vals.
    scan_rule "$f" "$rel" SIZE-GLOBAL 'AppSizes[.]' \
      'AppSizes is deprecated; declare `private val <Component><Purpose> = NN.dp` at the top of this file'

    # 8. Every scrollable passes exactly `overscrollEffect = null`.
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      add "$rel" SCROLL-OVERSCROLL "$line" 'scrollable without `overscrollEffect = null`'
    done < <(scan_calls "$f" '(^|[^A-Za-z])(LazyColumn|LazyRow|LazyVerticalGrid|LazyHorizontalGrid|LazyVerticalStaggeredGrid|LazyHorizontalStaggeredGrid|HorizontalPager|VerticalPager|verticalScroll|horizontalScroll)[(]' require 'overscrollEffect[[:space:]]*=[[:space:]]*null')
  fi

  # 6. Navigation 3 entries need the Hilt-aware factory.
  scan_rule "$f" "$rel" VIEWMODEL '= viewModel\(\)' \
    'use hiltViewModel(), not viewModel()'
done

if [ "$UPDATE_BASELINE" -eq 1 ]; then
  {
    echo "# Known design-system violations, accepted as legacy. Regenerate with:"
    echo "#   .claude/scripts/check-design-tokens.sh --update-baseline"
    echo "# Format: <repo-relative path><TAB><RULE>. Line numbers are deliberately not stored."
    printf '%s\n' "${findings[@]}" | cut -f1,2 | sort -u
  } > "$BASELINE"
  printf 'baseline updated: %s (%d entries)\n' "${BASELINE#"$ROOT"/}" "$(grep -cv '^#' "$BASELINE")"
  exit 0
fi

new=0
known=0
for finding in "${findings[@]:-}"; do
  [ -n "$finding" ] || continue
  IFS=$'\t' read -r rel rule line msg <<< "$finding"
  if [ -f "$BASELINE" ] && grep -qxF "$rel"$'\t'"$rule" "$BASELINE"; then
    known=$((known + 1))
    continue
  fi
  printf '%s:%s: %s — %s\n' "$rel" "$line" "$rule" "$msg" >&2
  new=$((new + 1))
done

if [ "$new" -gt 0 ]; then
  printf '\n%d NEW design-system violation(s)%s. Rules: .claude/skills/compose-design-system/SKILL.md\n' \
    "$new" "$([ "$known" -gt 0 ] && printf ' (%d known/baselined ignored)' "$known")" >&2
  exit 1
fi
[ "$known" -gt 0 ] && printf 'clean — %d known/baselined violation(s) ignored\n' "$known"
exit 0
