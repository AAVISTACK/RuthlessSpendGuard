# ─── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ─── Hilt ────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ─── Kotlin data classes (for Room entities and data classes used in DAOs) ───
-keepclassmembers class com.ruthless.spendguard.data.** { *; }
-keepclassmembers class com.ruthless.spendguard.viewmodel.** { *; }

# ─── Enums ───────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Keep Coroutines internals from being stripped ────────────────────────────
-keepclassmembers class kotlinx.coroutines.** { *; }

# ─── DataStore ───────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
