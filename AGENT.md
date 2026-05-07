# AGENT.md — Agent Swarm Configuration

## 1. AGENT ROLES & RESPONSIBILITIES

| Agent | Role | Focus |
|-------|------|-------|
| ARCHITECT | System design, module boundaries | Module graph, API contracts |
| REFACTORER | Code modernization, pattern migration | Kotlin idioms, Compose fixes |
| UI_ENGINEER | Compose optimization, theming | Dashboard revamp, minimalism |
| PERFORMANCE | Memory, battery, CPU profiling | Optimization passes |
| SECURITY | Encryption, validation | Backup security, URL validation |
| TESTER | Unit tests, integration | Test coverage |
| BACKUP_ENGINEER | Backup/restore logic | Export/import pipelines |
| DEBUGGER | Bug triage, root cause | Issue resolution |

## 2. COMMUNICATION PROTOCOL

- All agents read CLAUDE.md before any operation
- Cross-module changes require ARCHITECT approval
- UI changes require UI_ENGINEER + PERFORMANCE sign-off
- Backup changes require BACKUP_ENGINEER + SECURITY sign-off
- All changes must pass TESTER validation before merge

## 3. DECISION LOG

| Timestamp | Agent | Decision | Rationale |
|-----------|-------|----------|-----------|
| 2026-05-07 | (audit) | Health check gate already removed | NavHost starts at DASHBOARD_ROUTE directly |
| 2026-05-07 | (audit) | URL validation needs loosening | Current blocks non-HTTP/HTTPS — should accept any URL |
| 2026-05-07 | (audit) | Backup uses internal storage | Needs SAF file picker for user-selected location |
| 2026-05-07 | REFACTORER | URL validation leniency | Added normalizeHostLenient + updated UrlNormalizer + AddEditScreen |
| 2026-05-07 | UI_ENGINEER | Dashboard minimalism | Replaced GlassPanel + LinkNestGradientBackground with Box + Surface |

## 4. WORKFLOW RULES

- NEVER modify multiple modules in single commit
- ALWAYS run ./gradlew :module:test before commit
- ALWAYS update CLAUDE.md when module boundaries change
- ALWAYS add @Stable/@Immutable for Compose data classes
- NEVER use GlobalScope — use viewModelScope or lifecycleScope
- NEVER hardcode dp/sp — use MaterialTheme dimensions
- ALWAYS handle Result<T> failures with user-visible error states
- NEVER suppress lint/detekt without ARCHITECT approval

## 5. MCP TOOL BINDINGS

- filesystem: Full repo R/W
- git: Version control operations
- android-gradle: Build, test, lint, assemble via Gradle
- lint: Static analysis
- ktlint: Code style enforcement
- detekt: Complexity, code smell detection
- compose-inspector: Recomposition tracing
- memory-graph: Leak detection

## 6. QUALITY GATES

- lint: 0 warnings (or suppressed with justification)
- ktlint: 0 violations
- detekt: 0 high-severity issues
- tests: 100% pass rate
- coverage: > 70% overall
- build: ./gradlew :app:assembleRelease succeeds

## 7. CURRENT PATCHES IN PROGRESS

| Patch | Status | Assignee |
|-------|--------|----------|
| 2.1: Health check gate | DONE | — |
| 2.2: Backup/restore revamp | NEEDS SAF | BACKUP_ENGINEER |
| 2.3: UI/UX revamp | IN PROGRESS | UI_ENGINEER |
| 2.4: Performance | PENDING | PERFORMANCE |
| 2.5: URL validation | DONE | REFACTORER |