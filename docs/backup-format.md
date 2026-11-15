# Backup format

LinkNest writes format-v2 JSON envelopes containing kind `linknest.backup`, metadata, and base64-encoded GZIP snapshot data. SHA-256 covers the compressed bytes. This detects accidental corruption; it does not authenticate a file and is not encryption.

Both encoded input and expanded snapshot text are limited to 32 MiB of characters. External file reads stop at that limit before allocating the complete input. Legacy plain snapshot JSON remains readable. Database schema versions 1 through 6 are supported; newer versions are rejected.

Snapshots contain categories, websites, tags, associations, mappings, saved filters, integrity events, recent queries, optional preferences and legacy cache metadata. Device-local image files and SAF grants are not portable. Restores discard cache paths/custom image URIs and report the omission. The chosen local backup folder is preserved.

Import previews counts and requires explicit replacement confirmation. The previous snapshot is staged before replacement. Room table changes are transactional; preferences and search-index updates occur after commit and may produce warnings. The previous staged snapshot can be selected with Import last export for recovery; it is replaced by subsequent exports/restores. Store an external copy for durable retention.

Validation rejects invalid identities, duplicate names, missing relationships, invalid values and unsupported saved-filter enums before clearing tables. Tests cover serializer compatibility and bounded reads; Android tests cover migration association preservation and restore transaction rollback.
