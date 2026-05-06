# Static Analysis Baseline

Date: 2026-05-06

## Commands

| Command | Result |
| --- | --- |
| `.\gradlew.bat projects --console=plain` | PASS |
| `.\gradlew.bat tasks --console=plain` | PASS |
| `.\gradlew.bat :core:network:testDebugUnitTest --console=plain` | PASS after URL parser fix |
| `.\gradlew.bat lintDebug --console=plain` | PASS |
| `.\gradlew.bat test --console=plain` | PASS |
| `.\gradlew.bat :app:assembleDebug --console=plain` | PASS |

## Failed Or Retried Commands

- Initial sandboxed `projects`/`tasks` failed because Gradle could not create wrapper lock files under sandbox Gradle home.
- First elevated `projects` run timed out after 124 seconds; later rerun passed.
- First parallel `:app:assembleDebug` failed on `.gradle/configuration-cache` lock while `:core:network:testDebugUnitTest` was still running; serial rerun passed.
- First `:core:network:testDebugUnitTest` failed on Unicode host normalization; fixed in `UrlNormalizer`.

## Findings

- WARNING: `lintDebug` reports were generated for modules, but Gradle completed successfully with no blocking lint errors.
- TESTING_GAP: Before this pass, no targeted launch-gate, URL validation, or backup/restore tests were present.
- DOCUMENTATION_GAP: No root `AGENTS.md` or audit docs existed before this pass.
- INFO: No ktlint or detekt tasks appeared in `gradlew tasks`.
