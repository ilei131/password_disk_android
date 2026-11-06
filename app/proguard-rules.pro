# ============================================================
# Password Disk - ProGuard / R8 规则
# 目标：release 包可混淆，同时保证反射相关框架正常运行
# ============================================================

# ---------- 通用 ----------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.**

# ---------- 应用入口 ----------
-keep class com.passworddisk.app.PasswordDiskApplication { *; }
-keep class com.passworddisk.app.ui.SplashActivity { *; }
-keep class com.passworddisk.app.ui.MainActivity { *; }

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ---------- Gson（数据类需保留字段名以正确反序列化） ----------
-keep class com.passworddisk.app.cloud.** { *; }
-keep class com.passworddisk.app.data.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn sun.misc.**

# ---------- Retrofit / OkHttp ----------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ---------- ZXing 扫码 ----------
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ---------- bcrypt / codec ----------
-keep class at.favre.lib.crypto.bcrypt.** { *; }
-dontwarn at.favre.lib.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.codec.**

# ---------- Kotlin 协程 ----------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------- 枚举 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
