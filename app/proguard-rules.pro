# ProGuard rules for LSPosed module
# Keep all module classes and hooker classes
-keep class com.keep.mining.** { *; }
-keep class com.fix.boost.** { *; }

# Keep annotations
-keepattributes *Annotation*

# Keep method names for hooking
-keepclassmembers class * {
    @io.github.libxposed.api.callback.BeforeInvocation *;
    @io.github.libxposed.api.callback.AfterInvocation *;
}

# Don't warn about libxposed API
-dontwarn io.github.libxposed.api.**