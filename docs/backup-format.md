# Backup Format

Current implementation supports plain JSON export and encrypted `.lnen` payloads. This document records the current format; it is not yet the hardened target format.

## Current Top-Level JSON

- `schemaVersion`: integer, current default is `2`.
- `exportedAt`: epoch milliseconds.
- `categories`: category rows.
- `websites`: website rows.
- `tags`: tag rows.
- `websiteTags`: website/tag cross references.
- `mappings`: domain/category mappings.
- `savedFilters`: optional saved filter rows.
- `events`: optional recent integrity events.

## Current Export Behavior

`BackupManager.export` serializes a `BackupSnapshot`, limits payload size to 8 MiB of JSON characters, optionally encrypts using `BackupCryptoManager`, then writes into LinkNest app backup storage with either `.json` or `.lnen` extension.

## Current Import Behavior

`BackupManager.parse` enforces the same 8 MiB size guard, decrypts if needed, checks required arrays, and parses JSON into `BackupSnapshot`. `OfflineFirstBackupRepository.importSnapshot` runs database writes inside a Room transaction, maps imported IDs to local IDs, skips duplicate websites, imports mappings/saved filters/events, then rebuilds AppSearch after the transaction.

## Gaps Before Hardening

- No magic/header or `formatVersion` envelope.
- No checksum verification.
- No app version metadata.
- No explicit compression metadata.
- No documented backward compatibility plan for older exports.
- No backup-specific round-trip tests found yet.
