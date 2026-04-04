package com.linknest.core.data.storage

import android.content.Context
import java.io.File

object LinkNestStorage {
    const val ICON_CACHE_DIR = "icon-cache"
    const val BACKUP_DIR = "backups"
    const val IMAGE_LOADER_CACHE_DIR = "image-loader"

    fun iconCacheDirectory(context: Context): File =
        File(context.filesDir, ICON_CACHE_DIR)

    fun backupDirectory(context: Context): File =
        File(context.filesDir, BACKUP_DIR)

    fun imageLoaderCacheDirectory(context: Context): File =
        File(context.cacheDir, IMAGE_LOADER_CACHE_DIR)
}
