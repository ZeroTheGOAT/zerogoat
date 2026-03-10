# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep our agent action types (used via reflection in JSON parsing)
-keep class com.zerogoat.zero.agent.ActionType { *; }
-keep class com.zerogoat.zero.agent.ActionType$* { *; }
-keep class com.zerogoat.zero.agent.AgentAction { *; }

# Keep accessibility service
-keep class com.zerogoat.zero.accessibility.ZeroAccessibilityService { *; }

# Keep channel services
-keep class com.zerogoat.zero.channels.WhatsAppChannel { *; }
-keep class com.zerogoat.zero.ui.overlay.FloatingBubbleService { *; }
