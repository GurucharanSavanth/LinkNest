# Module Graph

Generated from `settings.gradle.kts`, `build.gradle.kts`, and `.\gradlew.bat projects --console=plain` on 2026-05-06.

## Included Modules

- `:app`
- `:core:common`
- `:core:model`
- `:core:database`
- `:core:datastore`
- `:core:network`
- `:core:designsystem`
- `:core:data`
- `:core:action`
- `:feature:dashboard`
- `:feature:addedit`
- `:feature:settings`
- `:feature:search`

## Project Dependencies

- `:app` -> `:core:action`, `:core:data`, `:core:model`, `:core:designsystem`, all feature modules.
- `:core:action` -> `:core:common`, `:core:model`, `:core:data`, `:core:network`.
- `:core:data` -> `:core:common`, `:core:model`, `:core:database`, `:core:datastore`, `:core:network`.
- `:core:database` -> `:core:model`.
- `:core:datastore` -> `:core:common`, `:core:model`.
- `:core:network` -> `:core:common`, `:core:model`.
- `:feature:addedit` -> `:core:action`, `:core:data`, `:core:model`, `:core:network`, `:core:designsystem`.
- `:feature:dashboard` -> `:core:action`, `:core:data`, `:core:model`, `:core:designsystem`.
- `:feature:search` -> `:core:action`, `:core:data`, `:core:model`, `:core:designsystem`.
- `:feature:settings` -> `:core:action`, `:core:data`, `:core:model`, `:core:designsystem`.

## Notes

`settings.gradle.kts` already had local uncommitted toolchain-resolver changes before this pass. No module additions were made in this patch.
