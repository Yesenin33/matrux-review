-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-repackageclasses 'ru.matrux.app.core'
-allowaccessmodification
-optimizationpasses 5

-keep class ru.matrux.app.MainActivity { *; }
-keep class ru.matrux.app.service.** { *; }
