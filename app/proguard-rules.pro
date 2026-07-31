# MPAndroidChart - Required as JitPack library lacks bundled consumer R8 rules
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**