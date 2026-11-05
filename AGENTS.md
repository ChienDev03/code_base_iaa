# Base repository workflow
Read `.ai-kb/README.md`, then only the owning topic. User instructions govern execution.
Before any coding task, apply `.claude/skills/regression-prevention/SKILL.md` and select checklist IDs.
For feature implementation/review, dependencies, startup/async/navigation/list/image/media/service or
release changes, also apply `.claude/skills/performance-and-reliability/SKILL.md` before editing.
Canonical skills are under `.claude/skills`; preserve `.agents/skills` symlink aliases.

Single :app, feature-first UDF. Core never imports features except core/di/RepositoryModule.
Composable state is immutable; ViewModels depend on repository interfaces. UI <500 lines; VM <1000.
Public declarations have concise English KDoc; source/docs English. Feature strings stay separate.
Material roles first; ColorPrimitives only for unmapped needs. Use LocalAppTypography/AppSpacing/
AppShapes/AppMotion, private named geometry, overscrollEffect=null, debounced clicks/in-flight guards.
Keep root fontScale=1f and density unchanged. One shell snackbar, lifecycle-aware buffered effects.
Routes are small serializable NavKeys registered in AppRouteSerializersModule; Hilt per-entry stores.
Inject clocks/dispatchers. Propagate cancellation and reject stale context results. No personal data logs.
Confirm new first-load/refresh/paging/action/retry/offline UX before implementation. Preserve approved
behavior. Error precedence details messages -> backend message -> feature -> generic; no exception text.
Optional SDKs must be absent from disabled dependencies/manifests, not merely disabled at runtime.
No unapproved analytics events. Never read/copy secret files or source keys/Firebase configs.

Use rtk/rtk proxy when available; accurately report fallback. Use Gradle wrapper only.
Compile/build/lint/static checks allowed. Unit/instrumented/Maestro/benchmark require explicit confirmation.
Run `python3 scripts/check_design_tokens.py` after UI changes. Offer a Maestro draft for new flows.
Update owning contract + `.ai-kb/CHANGELOG.md` for significant changes; confirm that in final response.
No commit/publication/live status updates/messages without authorization. Report actual verification
and selected checklist IDs with pass/not run/N/A reasons. Do not claim unavailable runtime checks passed.
