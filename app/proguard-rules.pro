# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room — keep entity field names used by TypeConverters and schema export
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# Keep all enums (Room TypeConverters use Enum.valueOf by name)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# Hilt — keep entry points and injected constructors
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-dontwarn dagger.hilt.**

# Kotlin & coroutines
-keep class kotlin.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlin.**
-dontwarn kotlinx.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# AppSearch
-keep class androidx.appsearch.** { *; }
-dontwarn androidx.appsearch.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-dontwarn androidx.work.**

# org.json (used in BackupManager)
-keep class org.json.** { *; }

# Javax inject
-dontwarn javax.inject.**

# Keep app model classes used in backup serialization
-keep class com.linknest.core.data.model.** { *; }
-keep class com.linknest.core.model.** { *; }

# Keep Hilt worker factory entries
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Prevent removing unused interface implementations
-keep interface com.linknest.core.data.repository.** { *; }
