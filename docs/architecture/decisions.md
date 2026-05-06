# Architecture Decisions

## 2026-05-06: Dashboard is the start destination

Decision: Remove the blocking launch-gate route and start navigation at `DASHBOARD_ROUTE`.

Rationale: Health checks are maintenance work and should not block the primary app workflow. Background health checks remain scheduled through WorkManager when enabled.

Validation: `:app:assembleDebug`, `lintDebug`, and `test` passed.

## 2026-05-06: URL storage is more lenient than URL automation

Decision: Allow practical non-web schemes to be stored while keeping metadata fetch and health automation limited to safe HTTP(S) URLs. Unsafe schemes such as `file`, `javascript`, `data`, and `content` are rejected.

Rationale: Users may need to save app-specific links, but LinkNest should not fetch or probe arbitrary schemes. Normalized HTTP input still upgrades to HTTPS, private/local web targets remain blocked, and custom schemes return warnings.

Validation: `UrlNormalizerTest`, `:core:network:testDebugUnitTest`, `test`, and `:app:assembleDebug` passed.
