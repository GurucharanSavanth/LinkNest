# Claude Skills Registry

This file records desired agent skills for LinkNest work. It is a project-local registry, not proof that a global Claude skill or MCP server is installed.

## Skills

1. `kotlin-modernize` - migrate legacy Kotlin to idiomatic Kotlin while preserving module boundaries.
2. `compose-optimize` - reduce recomposition risk, stabilize state, and improve accessibility semantics.
3. `room-optimize` - review Room indices, transactions, query shape, migrations, and schema exports.
4. `backup-engineer` - harden backup/restore format, validation, rollback, and round-trip testing.
5. `android-performance` - inspect startup, WorkManager, memory, CPU, image loading, and battery behavior.
6. `ui-revamp` - implement Material 3 UI changes with compact, accessible Compose surfaces.
7. `security-harden` - review URL policy, encryption, sensitive data handling, and unsafe scheme behavior.
8. `test-generation` - add focused unit, integration, migration, and Compose UI tests.
9. `dead-code-elimination` - remove unused code/resources only after reference search and build validation.
10. `mcp-orchestrator` - coordinate available tools while reporting unavailable tools as blocked.

## Required Context

- `AGENTS.md`
- `CLAUDE.md`
- `docs/audit/module-graph.md`
- `docs/audit/code-audit.md`
- `docs/testing/validation-report.md`

## Validation

Use repo-supported commands only:

```powershell
.\gradlew.bat :core:network:testDebugUnitTest --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat lintDebug --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```
