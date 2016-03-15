# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/wakim/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Source File Name and Line Number in Stacktrace
# http://stackoverflow.com/questions/3913338/how-to-debug-with-obfuscated-with-proguard-applications-on-android
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses

-keepattributes LocalVariableTable,LocalVariableTypeTable

# RxJava
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn rx.internal.util.**